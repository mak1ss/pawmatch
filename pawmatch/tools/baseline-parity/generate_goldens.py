"""Generate golden data for the Java baseline port from the Python reference implementation.

Run from the project root:  python tools/baseline-parity/generate_goldens.py

1. Builds the reference's catalogue format from our seed CSV (the same data the application loads).
2. Self-check: the example profile must reproduce the ML team's example_result.json exactly.
3. Runs the reference on edge cases, seeded random profiles and compare cases, and writes goldens.json.

Agreed differences from the raw reference output are applied here, so the goldens describe the Java contract:
- per-breed 'source_retrieved_at_utc' and 'dataset_version' are dropped (provenance stays with the dataset;
  the dataset version is stored per recommendation run instead);
- 'excluded' is sorted by breed_id (catalogue order must not matter).
"""
from __future__ import annotations

import copy
import csv
import importlib.util
import json
import random
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
REFERENCE = Path(__file__).resolve().parent / 'reference' / 'pawmatch.py'
SEED_CSV = ROOT / 'src/main/resources/db/changelog/data/breeds-v1.0.0.csv'
GOLDEN_DIR = ROOT / 'src/test/resources/inference/baseline'
EXAMPLE_RESULT = GOLDEN_DIR / 'example_result.json'
OUTPUT = GOLDEN_DIR / 'goldens.json'

SEED = 20261122
RANDOM_PROFILES = 150
COMPARE_CASES = 30

SIZES = ['XSmall', 'Small', 'Medium', 'Large', 'XLarge']
HARD_LIMITS = ['energy_level', 'shedding_level', 'coat_grooming_frequency', 'mental_stimulation_needs',
               'barking_level']
SCORE_COLUMNS = HARD_LIMITS + ['trainability_level', 'good_with_young_children', 'good_with_other_dogs']

EXAMPLE_PROFILE = {
    'activity_level': 3, 'shedding_tolerance': 2, 'grooming_capacity': 2, 'mental_stimulation_capacity': 3,
    'barking_tolerance': 2, 'preferred_size': 'Small', 'has_young_children': True, 'has_other_dogs': False,
    'has_cats': False, 'housing_type': 'apartment', 'walk_minutes_daily': 60, 'alone_hours_daily': 4,
    'owner_experience': 'none', 'training_minutes_daily': 20,
}


def load_reference():
    spec = importlib.util.spec_from_file_location('pawmatch_reference', REFERENCE)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def write_reference_catalogue(target: Path) -> list[str]:
    """Our seed CSV -> the reference's column names. Returns the breed ids in seed order."""
    with SEED_CSV.open(encoding='utf-8', newline='') as source:
        rows = list(csv.DictReader(source))
    columns = ['breed_id', 'breed_name_en', 'breed_name_uk', 'size_source', 'source_url', 'dataset_version',
               'retrieved_at_utc'] + SCORE_COLUMNS
    with target.open('w', encoding='utf-8', newline='') as out:
        writer = csv.DictWriter(out, fieldnames=columns)
        writer.writeheader()
        for row in rows:
            record = {'breed_id': row['slug'], 'breed_name_en': row['name_en'], 'breed_name_uk': row['name_uk'],
                      'size_source': row['size'], 'source_url': row['source_url'], 'dataset_version': '1.0.0',
                      'retrieved_at_utc': 'not-tracked'}
            record.update({column: row[column] for column in SCORE_COLUMNS})
            writer.writerow(record)
    return [row['slug'] for row in rows]


def to_contract(result: dict) -> dict:
    """Apply the agreed differences to a reference result (recommend or compare)."""
    result = copy.deepcopy(result)
    for key in ('recommendations', 'breeds'):
        for breed in result.get(key, []):
            breed.pop('source_retrieved_at_utc', None)
            breed.pop('dataset_version', None)
    if 'excluded' in result:
        result['excluded'].sort(key=lambda item: item['breed_id'])
    return result


def edge_cases() -> list[tuple[str, dict]]:
    def profile(**changes):
        p = dict(EXAMPLE_PROFILE)
        p.update(changes)
        return p

    return [
        ('example-profile', profile()),
        ('impossible-energy-limit-returns-empty', profile(hard_limits={'energy_level': 1})),
        ('single-result-not-padded', profile(allowed_sizes=['Small'],
                                             hard_limits={'energy_level': 3, 'coat_grooming_frequency': 1,
                                                          'barking_level': 1})),
        ('any-size-skips-size-factor', profile(preferred_size='any')),
        ('children-and-other-dogs', profile(has_other_dogs=True)),
        ('cats-reported-unassessed', profile(has_cats=True)),
        ('loosest-capacities', profile(activity_level=5, shedding_tolerance=5, grooming_capacity=5,
                                       mental_stimulation_capacity=5, barking_tolerance=5, preferred_size='any',
                                       has_young_children=False)),
        ('strictest-capacities', profile(activity_level=1, shedding_tolerance=1, grooming_capacity=1,
                                         mental_stimulation_capacity=1, barking_tolerance=1,
                                         preferred_size='XSmall')),
        ('xlarge-only', profile(allowed_sizes=['XLarge'], preferred_size='XLarge', has_young_children=False)),
        ('all-hard-limits-three', profile(hard_limits={key: 3 for key in HARD_LIMITS})),
        ('no-children-no-dogs-large', profile(has_young_children=False, preferred_size='Large', activity_level=5)),
        ('decimal-times', profile(walk_minutes_daily=45.5, alone_hours_daily=7.5, training_minutes_daily=12.5)),
    ]


def random_profile(rng: random.Random) -> dict:
    p = {
        'activity_level': rng.randint(1, 5),
        'shedding_tolerance': rng.randint(1, 5),
        'grooming_capacity': rng.randint(1, 5),
        'mental_stimulation_capacity': rng.randint(1, 5),
        'barking_tolerance': rng.randint(1, 5),
        'preferred_size': rng.choice(SIZES + ['any']),
        'has_young_children': rng.random() < 0.4,
        'has_other_dogs': rng.random() < 0.35,
        'has_cats': rng.random() < 0.25,
        'housing_type': rng.choice(['apartment', 'house', 'other']),
        'walk_minutes_daily': rng.choice([rng.randint(0, 240), round(rng.uniform(0, 240), 1)]),
        'alone_hours_daily': rng.choice([rng.randint(0, 12), round(rng.uniform(0, 12), 1)]),
        'owner_experience': rng.choice(['none', 'some', 'experienced']),
        'training_minutes_daily': rng.choice([rng.randint(0, 90), round(rng.uniform(0, 90), 1)]),
    }
    if rng.random() < 0.3:
        chosen = set(rng.sample(SIZES, rng.randint(1, 4)))
        p['allowed_sizes'] = [size for size in SIZES if size in chosen]
    if rng.random() < 0.3:
        chosen = set(rng.sample(HARD_LIMITS, rng.randint(1, 3)))
        p['hard_limits'] = {key: rng.randint(2, 5) for key in HARD_LIMITS if key in chosen}
    return p


def to_json_lines(golden: dict) -> str:
    """Valid JSON with one compact case per line: small file, readable diffs."""
    header = {key: value for key, value in golden.items() if key != 'cases'}
    lines = [json.dumps(case, ensure_ascii=False, separators=(',', ':')) for case in golden['cases']]
    head = json.dumps(header, ensure_ascii=False)[:-1]
    return head + ',\n"cases":[\n' + ',\n'.join(lines) + '\n]}\n'


def main() -> int:
    reference = load_reference()
    with tempfile.TemporaryDirectory() as tmp:
        catalogue = Path(tmp) / 'breeds.csv'
        breed_ids = write_reference_catalogue(catalogue)
        model = reference.PawMatch(catalogue)

        expected_example = to_contract(json.loads(EXAMPLE_RESULT.read_text(encoding='utf-8')))
        actual_example = to_contract(model.recommend(dict(EXAMPLE_PROFILE)))
        if actual_example != expected_example:
            print('Self-check FAILED: the reference on our seed catalogue does not reproduce example_result.json',
                  file=sys.stderr)
            return 1
        print('Self-check passed: example_result.json reproduced.')

        rng = random.Random(SEED)
        cases = []
        for name, profile in edge_cases():
            cases.append({'name': name, 'kind': 'recommend', 'profile': profile,
                          'expected': to_contract(model.recommend(copy.deepcopy(profile)))})
        profiles = [random_profile(rng) for _ in range(RANDOM_PROFILES)]
        for index, profile in enumerate(profiles, 1):
            cases.append({'name': f'random-{index:03d}', 'kind': 'recommend', 'profile': profile,
                          'expected': to_contract(model.recommend(copy.deepcopy(profile)))})
        for index in range(1, COMPARE_CASES + 1):
            profile = profiles[index - 1]
            ids = rng.sample(breed_ids, rng.randint(2, 5))
            cases.append({'name': f'compare-{index:03d}', 'kind': 'compare', 'profile': profile, 'breed_ids': ids,
                          'expected': to_contract(model.compare(copy.deepcopy(profile), ids))})

    golden = {
        'generated_by': 'tools/baseline-parity/generate_goldens.py',
        'reference_model_version': reference.VERSION,
        'catalogue': SEED_CSV.relative_to(ROOT).as_posix(),
        'seed': SEED,
        'cases': cases,
    }
    OUTPUT.write_text(to_json_lines(golden), encoding='utf-8')
    recommend = [c for c in cases if c['kind'] == 'recommend']
    print(f'Wrote {len(cases)} cases to {OUTPUT.relative_to(ROOT)}: {len(recommend)} recommend, '
          f'{len(cases) - len(recommend)} compare; '
          f'{sum(1 for c in recommend if not c["expected"]["recommendations"])} empty, '
          f'{sum(1 for c in recommend if 0 < len(c["expected"]["recommendations"]) < 3)} with fewer than 3.')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
