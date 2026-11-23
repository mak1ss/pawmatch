import type { RecommendationRun } from '@/api/recommendations'

/**
 * Real baseline output (pawmatch-baseline-1.0.0) for the backend's example profile, copied from the backend's
 * resources/openapi/recommendation.yaml example. session_id is the mock session.
 */
export const recommendationRun: RecommendationRun = {
  "run_id": "0193a1f0-5b2c-7d4e-8f00-000000000001",
  "session_id": "0190f2a0-0000-7000-8000-000000000001",
  "answers_version": 1,
  "created_at": "2026-11-22T10:00:00Z",
  "model_version": "pawmatch-baseline-1.0.0",
  "model_type": "weighted_rule_based_baseline",
  "score_meaning": "Індекс відповідності за оціненими критеріями, не ймовірність успіху.",
  "min_score": 60,
  "dataset_version": "1.0.0",
  "catalogue_breeds": 30,
  "eligible_breeds": 29,
  "above_threshold": 29,
  "recommendations": [
    {
      "breed_id": "cavalier-king-charles-spaniel",
      "breed_name": "Кавалер-кінг-чарльз-спанієль",
      "breed_name_en": "Cavalier King Charles Spaniel",
      "compatibility_index": 98,
      "factors": [
        {
          "criterion": "activity",
          "label": "Активність",
          "breed_value": 3,
          "profile_value": 3,
          "match": 1,
          "weight": 3,
          "explanation": "Активність породи — 3/5, бажаний рівень — 3/5."
        },
        {
          "criterion": "shedding",
          "label": "Линяння",
          "breed_value": 2,
          "profile_value": 2,
          "match": 1,
          "weight": 2,
          "explanation": "Линяння: оцінка породи — 2/5, ваша допустима межа — 2/5."
        },
        {
          "criterion": "grooming",
          "label": "Догляд за шерстю",
          "breed_value": 2,
          "profile_value": 2,
          "match": 1,
          "weight": 2,
          "explanation": "Догляд за шерстю: оцінка породи — 2/5, ваша допустима межа — 2/5."
        },
        {
          "criterion": "mental_stimulation",
          "label": "Розумові заняття",
          "breed_value": 3,
          "profile_value": 3,
          "match": 1,
          "weight": 2,
          "explanation": "Розумові заняття: оцінка породи — 3/5, ваша допустима межа — 3/5."
        },
        {
          "criterion": "barking",
          "label": "Вокалізація",
          "breed_value": 3,
          "profile_value": 2,
          "match": 0.75,
          "weight": 1,
          "explanation": "Вокалізація: оцінка породи — 3/5, ваша допустима межа — 2/5."
        },
        {
          "criterion": "size",
          "label": "Розмір",
          "breed_value": "Small",
          "profile_value": "Small",
          "match": 1,
          "weight": 1,
          "explanation": "Розмір за AKC — Small; бажаний — Small."
        },
        {
          "criterion": "young_children",
          "label": "Взаємодія з малими дітьми",
          "breed_value": 5,
          "profile_value": true,
          "match": 1,
          "weight": 1.5,
          "explanation": "Взаємодія з малими дітьми: породна оцінка 5/5; потрібна оцінка конкретної собаки та нагляд."
        }
      ],
      "reasons": [
        "Активність породи — 3/5, бажаний рівень — 3/5.",
        "Линяння: оцінка породи — 2/5, ваша допустима межа — 2/5.",
        "Догляд за шерстю: оцінка породи — 2/5, ваша допустима межа — 2/5.",
        "Розумові заняття: оцінка породи — 3/5, ваша допустима межа — 3/5.",
        "Розмір за AKC — Small; бажаний — Small.",
        "Взаємодія з малими дітьми: породна оцінка 5/5; потрібна оцінка конкретної собаки та нагляд."
      ],
      "warnings": [
        "Вокалізація: оцінка породи — 3/5, ваша допустима межа — 2/5.",
        "Породні оцінки описують тенденції; поведінка конкретної собаки може відрізнятися.",
        "Навіть висока породна оцінка не замінює нагляду дорослого при взаємодії з дітьми."
      ],
      "source_url": "https://www.akc.org/dog-breeds/cavalier-king-charles-spaniel/",
      "image_url": null
    },
    {
      "breed_id": "french-bulldog",
      "breed_name": "Французький бульдог",
      "breed_name_en": "French Bulldog",
      "compatibility_index": 96,
      "factors": [
        {
          "criterion": "activity",
          "label": "Активність",
          "breed_value": 3,
          "profile_value": 3,
          "match": 1,
          "weight": 3,
          "explanation": "Активність породи — 3/5, бажаний рівень — 3/5."
        },
        {
          "criterion": "shedding",
          "label": "Линяння",
          "breed_value": 3,
          "profile_value": 2,
          "match": 0.75,
          "weight": 2,
          "explanation": "Линяння: оцінка породи — 3/5, ваша допустима межа — 2/5."
        },
        {
          "criterion": "grooming",
          "label": "Догляд за шерстю",
          "breed_value": 1,
          "profile_value": 2,
          "match": 1,
          "weight": 2,
          "explanation": "Догляд за шерстю: оцінка породи — 1/5, ваша допустима межа — 2/5."
        },
        {
          "criterion": "mental_stimulation",
          "label": "Розумові заняття",
          "breed_value": 3,
          "profile_value": 3,
          "match": 1,
          "weight": 2,
          "explanation": "Розумові заняття: оцінка породи — 3/5, ваша допустима межа — 3/5."
        },
        {
          "criterion": "barking",
          "label": "Вокалізація",
          "breed_value": 1,
          "profile_value": 2,
          "match": 1,
          "weight": 1,
          "explanation": "Вокалізація: оцінка породи — 1/5, ваша допустима межа — 2/5."
        },
        {
          "criterion": "size",
          "label": "Розмір",
          "breed_value": "Small",
          "profile_value": "Small",
          "match": 1,
          "weight": 1,
          "explanation": "Розмір за AKC — Small; бажаний — Small."
        },
        {
          "criterion": "young_children",
          "label": "Взаємодія з малими дітьми",
          "breed_value": 5,
          "profile_value": true,
          "match": 1,
          "weight": 1.5,
          "explanation": "Взаємодія з малими дітьми: породна оцінка 5/5; потрібна оцінка конкретної собаки та нагляд."
        }
      ],
      "reasons": [
        "Активність породи — 3/5, бажаний рівень — 3/5.",
        "Догляд за шерстю: оцінка породи — 1/5, ваша допустима межа — 2/5.",
        "Розумові заняття: оцінка породи — 3/5, ваша допустима межа — 3/5.",
        "Вокалізація: оцінка породи — 1/5, ваша допустима межа — 2/5.",
        "Розмір за AKC — Small; бажаний — Small.",
        "Взаємодія з малими дітьми: породна оцінка 5/5; потрібна оцінка конкретної собаки та нагляд."
      ],
      "warnings": [
        "Линяння: оцінка породи — 3/5, ваша допустима межа — 2/5.",
        "Породні оцінки описують тенденції; поведінка конкретної собаки може відрізнятися.",
        "Навіть висока породна оцінка не замінює нагляду дорослого при взаємодії з дітьми."
      ],
      "source_url": "https://www.akc.org/dog-breeds/french-bulldog/",
      "image_url": null
    },
    {
      "breed_id": "shih-tzu",
      "breed_name": "Ши-тцу",
      "breed_name_en": "Shih Tzu",
      "compatibility_index": 90,
      "factors": [
        {
          "criterion": "activity",
          "label": "Активність",
          "breed_value": 3,
          "profile_value": 3,
          "match": 1,
          "weight": 3,
          "explanation": "Активність породи — 3/5, бажаний рівень — 3/5."
        },
        {
          "criterion": "shedding",
          "label": "Линяння",
          "breed_value": 1,
          "profile_value": 2,
          "match": 1,
          "weight": 2,
          "explanation": "Линяння: оцінка породи — 1/5, ваша допустима межа — 2/5."
        },
        {
          "criterion": "grooming",
          "label": "Догляд за шерстю",
          "breed_value": 4,
          "profile_value": 2,
          "match": 0.5,
          "weight": 2,
          "explanation": "Догляд за шерстю: оцінка породи — 4/5, ваша допустима межа — 2/5."
        },
        {
          "criterion": "mental_stimulation",
          "label": "Розумові заняття",
          "breed_value": 3,
          "profile_value": 3,
          "match": 1,
          "weight": 2,
          "explanation": "Розумові заняття: оцінка породи — 3/5, ваша допустима межа — 3/5."
        },
        {
          "criterion": "barking",
          "label": "Вокалізація",
          "breed_value": 3,
          "profile_value": 2,
          "match": 0.75,
          "weight": 1,
          "explanation": "Вокалізація: оцінка породи — 3/5, ваша допустима межа — 2/5."
        },
        {
          "criterion": "size",
          "label": "Розмір",
          "breed_value": "Small",
          "profile_value": "Small",
          "match": 1,
          "weight": 1,
          "explanation": "Розмір за AKC — Small; бажаний — Small."
        },
        {
          "criterion": "young_children",
          "label": "Взаємодія з малими дітьми",
          "breed_value": 5,
          "profile_value": true,
          "match": 1,
          "weight": 1.5,
          "explanation": "Взаємодія з малими дітьми: породна оцінка 5/5; потрібна оцінка конкретної собаки та нагляд."
        }
      ],
      "reasons": [
        "Активність породи — 3/5, бажаний рівень — 3/5.",
        "Линяння: оцінка породи — 1/5, ваша допустима межа — 2/5.",
        "Розумові заняття: оцінка породи — 3/5, ваша допустима межа — 3/5.",
        "Розмір за AKC — Small; бажаний — Small.",
        "Взаємодія з малими дітьми: породна оцінка 5/5; потрібна оцінка конкретної собаки та нагляд."
      ],
      "warnings": [
        "Догляд за шерстю: оцінка породи — 4/5, ваша допустима межа — 2/5.",
        "Вокалізація: оцінка породи — 3/5, ваша допустима межа — 2/5.",
        "Породні оцінки описують тенденції; поведінка конкретної собаки може відрізнятися.",
        "Навіть висока породна оцінка не замінює нагляду дорослого при взаємодії з дітьми."
      ],
      "source_url": "https://www.akc.org/dog-breeds/shih-tzu/",
      "image_url": null
    },
    {
      "breed_id": "boxer",
      "breed_name": "Боксер",
      "breed_name_en": "Boxer",
      "compatibility_index": 84,
      "factors": [
        {
          "criterion": "activity",
          "label": "Активність",
          "breed_value": 4,
          "profile_value": 3,
          "match": 0.75,
          "weight": 3,
          "explanation": "Активність породи — 4/5, бажаний рівень — 3/5."
        },
        {
          "criterion": "shedding",
          "label": "Линяння",
          "breed_value": 2,
          "profile_value": 2,
          "match": 1,
          "weight": 2,
          "explanation": "Линяння: оцінка породи — 2/5, ваша допустима межа — 2/5."
        },
        {
          "criterion": "grooming",
          "label": "Догляд за шерстю",
          "breed_value": 2,
          "profile_value": 2,
          "match": 1,
          "weight": 2,
          "explanation": "Догляд за шерстю: оцінка породи — 2/5, ваша допустима межа — 2/5."
        },
        {
          "criterion": "mental_stimulation",
          "label": "Розумові заняття",
          "breed_value": 4,
          "profile_value": 3,
          "match": 0.75,
          "weight": 2,
          "explanation": "Розумові заняття: оцінка породи — 4/5, ваша допустима межа — 3/5."
        },
        {
          "criterion": "barking",
          "label": "Вокалізація",
          "breed_value": 3,
          "profile_value": 2,
          "match": 0.75,
          "weight": 1,
          "explanation": "Вокалізація: оцінка породи — 3/5, ваша допустима межа — 2/5."
        },
        {
          "criterion": "size",
          "label": "Розмір",
          "breed_value": "Large",
          "profile_value": "Small",
          "match": 0.5,
          "weight": 1,
          "explanation": "Розмір за AKC — Large; бажаний — Small."
        },
        {
          "criterion": "young_children",
          "label": "Взаємодія з малими дітьми",
          "breed_value": 5,
          "profile_value": true,
          "match": 1,
          "weight": 1.5,
          "explanation": "Взаємодія з малими дітьми: породна оцінка 5/5; потрібна оцінка конкретної собаки та нагляд."
        }
      ],
      "reasons": [
        "Линяння: оцінка породи — 2/5, ваша допустима межа — 2/5.",
        "Догляд за шерстю: оцінка породи — 2/5, ваша допустима межа — 2/5.",
        "Взаємодія з малими дітьми: породна оцінка 5/5; потрібна оцінка конкретної собаки та нагляд."
      ],
      "warnings": [
        "Розмір за AKC — Large; бажаний — Small.",
        "Активність породи — 4/5, бажаний рівень — 3/5.",
        "Вокалізація: оцінка породи — 3/5, ваша допустима межа — 2/5.",
        "Розумові заняття: оцінка породи — 4/5, ваша допустима межа — 3/5.",
        "Породні оцінки описують тенденції; поведінка конкретної собаки може відрізнятися.",
        "Навіть висока породна оцінка не замінює нагляду дорослого при взаємодії з дітьми."
      ],
      "source_url": "https://www.akc.org/dog-breeds/boxer/",
      "image_url": null
    },
    {
      "breed_id": "bulldog",
      "breed_name": "Бульдог",
      "breed_name_en": "Bulldog",
      "compatibility_index": 84,
      "factors": [
        {
          "criterion": "activity",
          "label": "Активність",
          "breed_value": 3,
          "profile_value": 3,
          "match": 1,
          "weight": 3,
          "explanation": "Активність породи — 3/5, бажаний рівень — 3/5."
        },
        {
          "criterion": "shedding",
          "label": "Линяння",
          "breed_value": 3,
          "profile_value": 2,
          "match": 0.75,
          "weight": 2,
          "explanation": "Линяння: оцінка породи — 3/5, ваша допустима межа — 2/5."
        },
        {
          "criterion": "grooming",
          "label": "Догляд за шерстю",
          "breed_value": 3,
          "profile_value": 2,
          "match": 0.75,
          "weight": 2,
          "explanation": "Догляд за шерстю: оцінка породи — 3/5, ваша допустима межа — 2/5."
        },
        {
          "criterion": "mental_stimulation",
          "label": "Розумові заняття",
          "breed_value": 3,
          "profile_value": 3,
          "match": 1,
          "weight": 2,
          "explanation": "Розумові заняття: оцінка породи — 3/5, ваша допустима межа — 3/5."
        },
        {
          "criterion": "barking",
          "label": "Вокалізація",
          "breed_value": 2,
          "profile_value": 2,
          "match": 1,
          "weight": 1,
          "explanation": "Вокалізація: оцінка породи — 2/5, ваша допустима межа — 2/5."
        },
        {
          "criterion": "size",
          "label": "Розмір",
          "breed_value": "Medium",
          "profile_value": "Small",
          "match": 0.75,
          "weight": 1,
          "explanation": "Розмір за AKC — Medium; бажаний — Small."
        },
        {
          "criterion": "young_children",
          "label": "Взаємодія з малими дітьми",
          "breed_value": 3,
          "profile_value": true,
          "match": 0.5,
          "weight": 1.5,
          "explanation": "Взаємодія з малими дітьми: породна оцінка 3/5; потрібна оцінка конкретної собаки та нагляд."
        }
      ],
      "reasons": [
        "Активність породи — 3/5, бажаний рівень — 3/5.",
        "Розумові заняття: оцінка породи — 3/5, ваша допустима межа — 3/5.",
        "Вокалізація: оцінка породи — 2/5, ваша допустима межа — 2/5."
      ],
      "warnings": [
        "Взаємодія з малими дітьми: породна оцінка 3/5; потрібна оцінка конкретної собаки та нагляд.",
        "Догляд за шерстю: оцінка породи — 3/5, ваша допустима межа — 2/5.",
        "Линяння: оцінка породи — 3/5, ваша допустима межа — 2/5.",
        "Розмір за AKC — Medium; бажаний — Small.",
        "Породні оцінки описують тенденції; поведінка конкретної собаки може відрізнятися.",
        "Навіть висока породна оцінка не замінює нагляду дорослого при взаємодії з дітьми."
      ],
      "source_url": "https://www.akc.org/dog-breeds/bulldog/",
      "image_url": null
    }
  ],
  "notice": "Порівняйте потреби рекомендованих порід перед вибором.",
  "unassessed_criteria": [
    "Час прогулянок: немає перевірених породних норм у хвилинах.",
    "Години самотності: немає перевірених породних меж.",
    "Час навчання та досвід власника: не підміняються оцінкою trainability.",
    "Тип житла: придатність не виводиться лише з розміру породи."
  ],
  "excluded": [
    {
      "breed_id": "chihuahua",
      "reasons": [
        "Оцінка взаємодії з малими дітьми нижча за поріг прототипу 3/5."
      ]
    }
  ]
}
