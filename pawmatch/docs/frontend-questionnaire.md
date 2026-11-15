# Questionnaire: frontend handoff

The backend owns the questionnaire **contract**: field keys, types, ranges and allowed codes. The frontend owns
all **copy and layout**: question texts, option labels, scale end labels, units, sections, order and help texts.
Map your copy by field key and option code; never hardcode ranges or codes, read them from the contract.

## Endpoints

| Call | Purpose |
|---|---|
| `GET /api/v1/questionnaire` | The contract: `{ version, fields: [{ key, type, required, min?, max?, options? }] }` |
| `POST /api/v1/sessions` | Submit answers. `201` with `{ session_id, answers_version, questionnaire_version, submitted_at }` and `Location` |
| `PUT /api/v1/sessions/{session_id}/answers` | Change answers. `200` with the next `answers_version` |
| `GET /api/v1/sessions/{session_id}/answers` | Latest answers, to prefill the form |

The request body is a flat JSON object with the field keys below. Send JSON types exactly: `true`/`false` (not `1` or
`"true"`), integers as integers (not `"3"` or `3.0`).

## Fields (contract version 1.0.0)

| Key | Type | Values | Required |
|---|---|---|---|
| `activity_level` | integer | 1–5 | yes |
| `shedding_tolerance` | integer | 1–5 (5 = heavy shedding is fine) | yes |
| `grooming_capacity` | integer | 1–5 | yes |
| `mental_stimulation_capacity` | integer | 1–5 | yes |
| `barking_tolerance` | integer | 1–5 | yes |
| `preferred_size` | choice | `XSmall`, `Small`, `Medium`, `Large`, `XLarge`, `any` | yes |
| `has_young_children` | boolean | | yes |
| `has_other_dogs` | boolean | | yes |
| `has_cats` | boolean | | yes |
| `housing_type` | choice | `apartment`, `house`, `other` | yes |
| `walk_minutes_daily` | number | 0–1440, decimals allowed | yes |
| `alone_hours_daily` | number | 0–24, decimals allowed | yes |
| `owner_experience` | choice | `none`, `some`, `experienced` | yes |
| `training_minutes_daily` | number | 0–1440, decimals allowed | yes |
| `allowed_sizes` | multi_choice | non-empty, distinct values from `XSmall`, `Small`, `Medium`, `Large`, `XLarge` (hard filter) | no |
| `hard_limits` | limits | object; keys from `energy_level`, `shedding_level`, `coat_grooming_frequency`, `mental_stimulation_needs`, `barking_level`; values 1–5 (max allowed breed rating) | no |

**Copy the frontend needs:** a question text for each of the 16 keys, a label for every option code above, end labels
for the five 1–5 scales, units for the three number fields, and section titles and order. A suggested Ukrainian
starting point is in the appendix below. It's a draft for product review, not a backend contract.

## Validation errors (HTTP 422)

```json
{
  "status": 422,
  "title": "Validation Failed",
  "field_errors": [
    { "field": "activity_level", "code": "invalid_integer", "message": "Потрібне ціле число від 1 до 5.",
      "params": { "min": 1, "max": 5 } },
    { "field": "has_cats", "code": "required", "message": "Обов’язкове поле." }
  ]
}
```

All errors are returned at once, sorted by field. `message` is a Ukrainian fallback; prefer your own wording by `code`.

| `code` | Meaning | `params` |
|---|---|---|
| `required` | Required field missing | |
| `unknown_field` | Field isn't part of the questionnaire | |
| `invalid_integer` | Not an integer, or out of range | `min`, `max` |
| `invalid_number` | Not a number, or out of range | `min`, `max` |
| `invalid_boolean` | Not `true` / `false` | |
| `invalid_choice` | Not one of the allowed codes | `allowed` |
| `invalid_list` | Not a non-empty list of allowed codes | `allowed` |
| `duplicate_value` | A list contains the same value twice | |
| `invalid_object` | Not a JSON object | |
| `unsupported_key` | Object key isn't allowed (e.g. `hard_limits.size`) | `allowed` |

Nested fields use dot notation, e.g. `hard_limits.energy_level`.

## Appendix: suggested Ukrainian copy (draft, for product review)

| Section | Key | Question | Options / scale ends / unit |
|---|---|---|---|
| Житло | `housing_type` | Де ви живете? | `apartment` Квартира · `house` Приватний будинок · `other` Інше |
| Спосіб життя | `activity_level` | Який рівень активності собаки вам підходить? | 1 Спокійна … 5 Дуже активна |
| Спосіб життя | `walk_minutes_daily` | Скільки хвилин на день ви готові гуляти з собакою? | хв |
| Спосіб життя | `alone_hours_daily` | Скільки годин на день собака буде сама вдома? | год |
| Сім’я та інші тварини | `has_young_children` | Чи є у вашій родині малі діти? | |
| Сім’я та інші тварини | `has_other_dogs` | Чи є у вас інші собаки? | |
| Сім’я та інші тварини | `has_cats` | Чи є у вас коти? | |
| Досвід | `owner_experience` | Який у вас досвід утримання собак? | `none` Немає досвіду · `some` Певний досвід · `experienced` Досвідчений власник |
| Побажання до собаки | `preferred_size` | Якого розміру собаку ви хочете? | `XSmall` Дуже маленька · `Small` Маленька · `Medium` Середня · `Large` Велика · `XLarge` Дуже велика · `any` Будь-який розмір |
| Побажання до собаки | `shedding_tolerance` | Наскільки ви готові до линяння? | 1 Лише мінімальне … 5 Сильне линяння — не проблема |
| Побажання до собаки | `grooming_capacity` | Скільки уваги ви готові приділяти догляду за шерстю? | 1 Мінімум … 5 Регулярний ретельний догляд |
| Побажання до собаки | `mental_stimulation_capacity` | Наскільки ви готові до розумових занять із собакою? | 1 Мінімально … 5 Щодня й багато |
| Побажання до собаки | `barking_tolerance` | Наскільки для вас прийнятний гавкіт? | 1 Потрібна тиха собака … 5 Гавкіт не заважає |
| Побажання до собаки | `training_minutes_daily` | Скільки хвилин на день ви готові приділяти дресируванню? | хв |
| Додаткові обмеження (необов’язково) | `allowed_sizes` | Які розміри собаки для вас допустимі? | same size labels as `preferred_size`, without `any` |
| Додаткові обмеження (необов’язково) | `hard_limits` | Максимально допустимі рівні характеристик породи | `energy_level` Енергійність · `shedding_level` Линяння · `coat_grooming_frequency` Догляд за шерстю · `mental_stimulation_needs` Потреба в розумових заняттях · `barking_level` Гавкіт |
