"""PawMatch baseline: ordinal breed matching, constraints and Ukrainian explanations.

Python 3.10+, standard library. Scores are indices, never success probabilities.
"""
from __future__ import annotations
import argparse
import csv
import json
import math
from pathlib import Path

VERSION = 'pawmatch-baseline-1.0.0'
SIZES = ['XSmall', 'Small', 'Medium', 'Large', 'XLarge']
WEIGHTS = {'activity': 3.0, 'shedding': 2.0, 'grooming': 2.0,
           'mental_stimulation': 2.0, 'barking': 1.0, 'size': 1.0,
           'young_children': 1.5, 'other_dogs': 1.5}
REQUIRED = {
    'activity_level', 'shedding_tolerance', 'grooming_capacity',
    'mental_stimulation_capacity', 'barking_tolerance', 'preferred_size',
    'has_young_children', 'has_other_dogs', 'has_cats', 'housing_type',
    'walk_minutes_daily', 'alone_hours_daily', 'owner_experience',
    'training_minutes_daily',
}
ORDINAL_INPUTS = {'activity_level', 'shedding_tolerance', 'grooming_capacity',
                  'mental_stimulation_capacity', 'barking_tolerance'}
HARD_LIMIT_FIELDS = {'energy_level', 'shedding_level', 'coat_grooming_frequency',
                     'mental_stimulation_needs', 'barking_level'}
BREED_SCORES = HARD_LIMIT_FIELDS | {'trainability_level', 'good_with_young_children',
                                    'good_with_other_dogs'}


class ProfileError(ValueError):
    def __init__(self, errors):
        self.errors = errors
        super().__init__('Некоректна анкета: ' + '; '.join(errors))


def validate_profile(profile):
    if not isinstance(profile, dict):
        raise ProfileError(['Очікується JSON-об’єкт анкети.'])
    errors = []
    for key in sorted(REQUIRED - profile.keys()):
        errors.append(f'{key}: обов’язкове поле.')
    for key in sorted(profile.keys() - REQUIRED - {'allowed_sizes', 'hard_limits'}):
        errors.append(f'{key}: невідоме поле.')
    for key in sorted(ORDINAL_INPUTS & profile.keys()):
        if type(profile[key]) is not int or not 1 <= profile[key] <= 5:
            errors.append(f'{key}: потрібне ціле число 1–5.')
    for key in ['has_young_children', 'has_other_dogs', 'has_cats']:
        if key in profile and type(profile[key]) is not bool:
            errors.append(f'{key}: потрібне true або false.')
    enums = {'preferred_size': SIZES + ['any'],
             'housing_type': ['apartment', 'house', 'other'],
             'owner_experience': ['none', 'some', 'experienced']}
    for key, choices in enums.items():
        if key in profile and (not isinstance(profile[key], str) or profile[key] not in choices):
            errors.append(f'{key}: допустимі значення {choices}.')
    for key, maximum in [('walk_minutes_daily', 1440), ('training_minutes_daily', 1440),
                         ('alone_hours_daily', 24)]:
        if key in profile:
            value = profile[key]
            if type(value) not in (int, float) or not math.isfinite(value) or not 0 <= value <= maximum:
                errors.append(f'{key}: потрібне скінченне число від 0 до {maximum}.')
    if 'allowed_sizes' in profile:
        allowed = profile['allowed_sizes']
        if not isinstance(allowed, list) or not allowed or any(not isinstance(x, str) or x not in SIZES for x in allowed):
            errors.append('allowed_sizes: непорожній список категорій розміру.')
        elif len(set(allowed)) != len(allowed):
            errors.append('allowed_sizes: розміри не повинні повторюватися.')
    if 'hard_limits' in profile:
        limits = profile['hard_limits']
        if not isinstance(limits, dict):
            errors.append('hard_limits: потрібен об’єкт максимальних оцінок.')
        else:
            for key, value in limits.items():
                if key not in HARD_LIMIT_FIELDS:
                    errors.append(f'hard_limits.{key}: непідтримуваний критерій.')
                elif type(value) is not int or not 1 <= value <= 5:
                    errors.append(f'hard_limits.{key}: потрібне ціле число 1–5.')
    if errors:
        raise ProfileError(errors)
    return profile


class PawMatch:
    def __init__(self, breeds_path=None):
        path = Path(breeds_path) if breeds_path is not None else Path(__file__).with_name('breeds.csv')
        with path.open(encoding='utf-8-sig', newline='') as source:
            rows = list(csv.DictReader(source))
        if not rows:
            raise ValueError('Каталог порід порожній.')
        self.breeds = {}
        required = {'breed_id', 'breed_name_en', 'breed_name_uk', 'size_source', 'source_url',
                    'dataset_version', 'retrieved_at_utc'} | BREED_SCORES
        for number, row in enumerate(rows, 2):
            if not required <= row.keys():
                raise ValueError(f'Рядок {number}: відсутні обов’язкові колонки каталогу.')
            if not row['breed_id'] or row['breed_id'] in self.breeds:
                raise ValueError(f'Рядок {number}: порожній або повторний breed_id.')
            if row['size_source'] not in SIZES:
                raise ValueError(f'Рядок {number}: невідомий розмір.')
            for key in BREED_SCORES:
                if row[key] not in ['1', '2', '3', '4', '5']:
                    raise ValueError(f'Рядок {number}: {key} має бути цілим 1–5.')
                row[key] = int(row[key])
            self.breeds[row['breed_id']] = row

    def _assess(self, profile, breed):
        excluded = []
        if 'allowed_sizes' in profile and breed['size_source'] not in profile['allowed_sizes']:
            excluded.append('Розмір поза явно дозволеним списком.')
        for key, maximum in profile.get('hard_limits', {}).items():
            if breed[key] > maximum:
                excluded.append(f'{key}: оцінка {breed[key]} перевищує жорстку межу {maximum}.')
        # Conservative, configurable-in-code prototype policy, not a guarantee of safety.
        if profile['has_young_children'] and breed['good_with_young_children'] < 3:
            excluded.append('Оцінка взаємодії з малими дітьми нижча за поріг прототипу 3/5.')
        if profile['has_other_dogs'] and breed['good_with_other_dogs'] < 3:
            excluded.append('Оцінка взаємодії з іншими собаками нижча за поріг прототипу 3/5.')

        factors = []
        def add(key, label, match, observed, requested, explanation):
            factors.append({'criterion': key, 'label': label, 'breed_value': observed,
                            'profile_value': requested, 'match': round(match, 4),
                            'weight': WEIGHTS[key], 'explanation': explanation})

        energy = breed['energy_level']; activity = profile['activity_level']
        add('activity', 'Активність', 1 - abs(energy - activity) / 4, energy, activity,
            f'Активність породи — {energy}/5, бажаний рівень — {activity}/5.')
        for criterion, label, breed_key, profile_key in [
            ('shedding', 'Линяння', 'shedding_level', 'shedding_tolerance'),
            ('grooming', 'Догляд за шерстю', 'coat_grooming_frequency', 'grooming_capacity'),
            ('mental_stimulation', 'Розумові заняття', 'mental_stimulation_needs', 'mental_stimulation_capacity'),
            ('barking', 'Вокалізація', 'barking_level', 'barking_tolerance'),
        ]:
            observed = breed[breed_key]; capacity = profile[profile_key]
            add(criterion, label, 1 - max(0, observed - capacity) / 4, observed, capacity,
                f'{label}: оцінка породи — {observed}/5, ваша допустима межа — {capacity}/5.')
        if profile['preferred_size'] != 'any':
            size = breed['size_source']; wanted = profile['preferred_size']
            add('size', 'Розмір', 1 - abs(SIZES.index(size) - SIZES.index(wanted)) / 4,
                size, wanted, f'Розмір за AKC — {size}; бажаний — {wanted}.')
        for enabled, criterion, label, field in [
            ('has_young_children', 'young_children', 'Взаємодія з малими дітьми', 'good_with_young_children'),
            ('has_other_dogs', 'other_dogs', 'Взаємодія з іншими собаками', 'good_with_other_dogs'),
        ]:
            if profile[enabled]:
                observed = breed[field]
                add(criterion, label, (observed - 1) / 4, observed, True,
                    f'{label}: породна оцінка {observed}/5; потрібна оцінка конкретної собаки та нагляд.')
        total_weight = sum(f['weight'] for f in factors)
        # Use unrounded values (all matches are exact quarter increments for this baseline).
        score = round(100 * sum(f['weight'] * f['match'] for f in factors) / total_weight, 1)
        warnings = [f['explanation'] for f in sorted(factors, key=lambda x: (x['match'], x['criterion'])) if f['match'] < 1]
        warnings.append('Породні оцінки описують тенденції; поведінка конкретної собаки може відрізнятися.')
        if profile['has_young_children']:
            warnings.append('Навіть висока породна оцінка не замінює нагляду дорослого при взаємодії з дітьми.')
        if profile['has_cats']:
            warnings.append('Взаємодію з котами не оцінено: у каталозі немає підтверджених даних.')
        return {
            'breed_id': breed['breed_id'], 'breed_name': breed['breed_name_uk'],
            'breed_name_en': breed['breed_name_en'], 'eligible': not excluded,
            'exclusion_reasons': excluded, 'compatibility_index': score if not excluded else None,
            'factors': factors, 'reasons': [f['explanation'] for f in factors if f['match'] == 1],
            'warnings': warnings, 'source_url': breed['source_url'],
            'dataset_version': breed['dataset_version'],
            'source_retrieved_at_utc': breed['retrieved_at_utc'],
        }

    def recommend(self, profile, limit=5, min_score=60):
        validate_profile(profile)
        if type(limit) is not int or not 1 <= limit <= 5:
            raise ValueError('limit має бути цілим числом 1–5.')
        if type(min_score) not in (int, float) or not math.isfinite(min_score) or not 0 <= min_score <= 100:
            raise ValueError('min_score має бути числом 0–100.')
        rows = [self._assess(profile, breed) for breed in self.breeds.values()]
        eligible = [r for r in rows if r['eligible']]
        above = sorted((r for r in eligible if r['compatibility_index'] >= min_score),
                       key=lambda r: (-r['compatibility_index'], r['breed_id']))
        results = above[:limit]
        unassessed = ['Час прогулянок: немає перевірених породних норм у хвилинах.',
                      'Години самотності: немає перевірених породних меж.',
                      'Час навчання та досвід власника: не підміняються оцінкою trainability.',
                      'Тип житла: придатність не виводиться лише з розміру породи.']
        if profile['has_cats']:
            unassessed.append('Взаємодія з котами: підтверджені породні дані відсутні.')
        return {
            'model_version': VERSION, 'model_type': 'weighted_rule_based_baseline',
            'score_meaning': 'Індекс відповідності за оціненими критеріями, не ймовірність успіху.',
            'min_score': min_score, 'catalogue_breeds': len(rows),
            'eligible_breeds': len(eligible), 'above_threshold': len(above),
            'recommendations': results,
            'notice': (f'Знайдено лише {len(results)} порід вище порога; слабкі варіанти не додавалися.'
                       if len(results) < 3 else 'Порівняйте потреби рекомендованих порід перед вибором.'),
            'unassessed_criteria': unassessed,
            'excluded': [{'breed_id': r['breed_id'], 'reasons': r['exclusion_reasons']} for r in rows if not r['eligible']],
        }

    def compare(self, profile, breed_ids):
        validate_profile(profile)
        if not isinstance(breed_ids, list) or not 2 <= len(breed_ids) <= 5:
            raise ValueError('Для порівняння потрібно 2–5 breed_id.')
        if any(not isinstance(x, str) for x in breed_ids):
            raise ValueError('breed_id мають бути рядками.')
        if len(set(breed_ids)) != len(breed_ids) or any(x not in self.breeds for x in breed_ids):
            raise ValueError('breed_id мають бути відомими та не повторюватися.')
        return {'model_version': VERSION,
                'breeds': [self._assess(profile, self.breeds[x]) for x in breed_ids]}


def main():
    parser = argparse.ArgumentParser(description='Підбір порід PawMatch за анкетою JSON.')
    parser.add_argument('--profile', type=Path, required=True)
    parser.add_argument('--breeds', type=Path, default=Path(__file__).with_name('breeds.csv'))
    parser.add_argument('--output', type=Path)
    parser.add_argument('--compare', help='2–5 breed_id через кому')
    args = parser.parse_args()
    try:
        profile = json.loads(args.profile.read_text(encoding='utf-8-sig'))
        model = PawMatch(args.breeds)
        result = model.compare(profile, args.compare.split(',')) if args.compare else model.recommend(profile)
        status = 0
    except (ProfileError, ValueError, OSError, json.JSONDecodeError) as error:
        result = {'error': str(error), 'field_errors': getattr(error, 'errors', [])}
        status = 2
    payload = json.dumps(result, ensure_ascii=False, indent=2)
    if args.output:
        args.output.write_text(payload + '\n', encoding='utf-8')
    else:
        print(payload)
    return status


if __name__ == '__main__':
    raise SystemExit(main())
