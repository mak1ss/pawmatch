# Questionnaire session payload examples

Request bodies for `POST /api/v1/sessions` and `PUT /api/v1/sessions/{session_id}/answers`. Paste them into Swagger UI
(`/swagger-ui`) or Postman. For the `PUT`, use the `session_id` returned by a successful `POST`.

## Happy paths

| File | Endpoint | Result | Shows |
|---|---|---|---|
| `01-submit-required-only.json` | `POST /sessions` | `201`, `answers_version: 1` | All 14 required answers (the baseline team's example profile) |
| `02-submit-with-constraints.json` | `POST /sessions` | `201`, `answers_version: 1` | Optional `allowed_sizes` and `hard_limits`; a decimal `alone_hours_daily` |
| `03-update-changed-answers.json` | `PUT /sessions/{id}/answers` | `200`, `answers_version: 2` | Changed answers stored as the next version; `preferred_size: "any"` |

## Validation (HTTP 422)

Every invalid field is reported at once, sorted by field. Each error has a `code`, optional `params`
(`min`, `max`, `allowed`) and a Ukrainian fallback `message`.

| File | `field_errors` (field → code) | Shows |
|---|---|---|
| `10-invalid-empty-object.json` | all 14 fields → `required` | Missing required answers |
| `11-invalid-wrong-types.json` | `activity_level` → `invalid_integer` (`"3"`), `has_cats` → `invalid_boolean` (`1`), `has_other_dogs` → `invalid_boolean` (`"false"`), `shedding_tolerance` → `invalid_integer` (`3.0`), `walk_minutes_daily` → `invalid_number` (`"60"`) | Strict JSON types: no coercion |
| `12-invalid-out-of-range.json` | `activity_level` → `invalid_integer` (0), `alone_hours_daily` → `invalid_number` (25), `barking_tolerance` → `invalid_integer` (6), `training_minutes_daily` → `invalid_number` (−5) | Range checks |
| `13-invalid-unknown-values.json` | `dog_name` → `unknown_field`, `housing_type` → `invalid_choice`, `owner_experience` → `invalid_choice`, `preferred_size` → `invalid_choice` (`"small"`: codes are case-sensitive) | Unknown fields and codes |
| `14-invalid-constraints.json` | `allowed_sizes` → `duplicate_value`, `hard_limits.energy_level` → `invalid_integer` (7), `hard_limits.size` → `unsupported_key` | Optional constraints, nested field names |
| `15-invalid-constraint-shapes.json` | `allowed_sizes` → `invalid_list` (empty), `hard_limits` → `invalid_object` (array) | Wrong shapes for the constraints |
| `16-invalid-partial-answers.json` | 11 missing fields → `required`, `has_cats` → `invalid_boolean` (`"no"`) | Missing and invalid fields reported together |

## Malformed request (HTTP 400)

| File | Result | Shows |
|---|---|---|
| `17-malformed-array-body.json` | `400`, no `field_errors` | The body must be a JSON object |

## Example 422 response

```json
{
  "title": "Validation Failed",
  "status": 422,
  "detail": "Запит містить некоректні дані.",
  "field_errors": [
    { "field": "allowed_sizes", "code": "duplicate_value", "message": "Розміри не повинні повторюватися." },
    { "field": "hard_limits.energy_level", "code": "invalid_integer", "message": "Потрібне ціле число від 1 до 5.",
      "params": { "min": 1, "max": 5 } },
    { "field": "hard_limits.size", "code": "unsupported_key", "message": "Непідтримуваний критерій.",
      "params": { "allowed": ["energy_level", "shedding_level", "coat_grooming_frequency",
                              "mental_stimulation_needs", "barking_level"] } }
  ]
}
```

For the field contract and the full list of error codes, see `docs/frontend-questionnaire.md`.
