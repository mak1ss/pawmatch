# CLAUDE.md

Guidance for Claude Code when working in this repository.

> **More context:** see [`../docs/ROADMAP.md`](../docs/ROADMAP.md) for milestones, decisions and open questions.
> For product and ML background, analyze the team's Google Drive folder **Team_1_544** (Vision Board, ML Canvas,
> System-first vs Model-first, datasets, baseline model) and the **PawMatch** board on CardBoard
> (cardboardit, map_id `237149`, a user story map).

## Product

**PawMatch** is a web app that recommends dog breeds that fit a person's lifestyle, so people stop choosing by looks or
popularity. The target users are future owners, first-time owners and experienced owners getting another dog.

The user answers a lifestyle questionnaire and gets **3–5 breeds**. Each breed has a **compatibility index from 0 to 100**,
with reasons and warnings. The user can then open breed cards and compare breeds.

The index is **not a probability** of success. Every recommendation response must say so (`score_meaning`).

## MVP scope (backend)

The current work. Do not build anything outside this list without being asked.

- **Guest mode only**: no registration, login or profiles. A guest session is identified by an unguessable UUID.
- **Breed catalogue**: 30 hand-checked AKC breeds from `breeds.csv` (dataset v1.0.0), each with a source URL and retrieval date.
  Unknown values are `NULL`, never `0`.
- **Questionnaire**: 14 required fields, validated server-side. Invalid input returns HTTP 422 with a list of field errors.
- **Recommendations**:
  - threshold ≥ 60, at most 5 breeds, sorted by index;
  - if fewer than 3 breeds pass, return only those plus a notice, and never pad with weak matches;
  - when the user changes answers, create a new answers version and a new recommendation run.
- **Compare**: 2–5 breeds scored against the session's latest answers. Excluded breeds show the reason and no index.
- **Feedback**: anonymous useful / not useful, one per recommendation run.
- **Monitoring**: p95 latency, error count, fallback count. API target: p95 ≤ 1 s at 10 concurrent requests.

**Out of MVP:** accounts, saved history, favourites, admin panel or auth, catalogue CRUD UI, shelter dogs, premium features,
and the trained ML model (Ridge regression). CSV import and expert labels are stretch items that need team agreement.

## Architecture

**System-first**: the web system is designed first, and the ML component is a replaceable module behind an interface.

```
com.pawmatch
├── catalog         breeds: entity, repository, read API, catalogue loader and validator
├── questionnaire   question definitions, profile DTO, validation (field errors → 422)
├── inference       ScoringEngine interface + engine router (primary → fallback)
├── recommendation  sessions, answer versions, recommendation runs/items, compare
├── feedback        feedback on recommendation runs
└── common          error handling (ProblemDetail), config, CORS
```

### Inference module (stub for now)

- All matching goes through the `ScoringEngine` interface: `recommend(profile, limit, minScore)` and `compare(profile, breedIds)`.
- **For now the only implementation is a stub.** How we integrate the real baseline is not decided yet. The options are a Java
  port, or a Python service wrapping `pawmatch.py`. Do not implement the scoring algorithm until asked.
- The stub must return the real response shape, so the rest of the system and the frontend can be built against it:
  - `model_version`, `model_type`, `score_meaning`, `min_score`;
  - `recommendations[]`, where each item has `breed_id`, names, `compatibility_index`, `factors[]`, `reasons[]`, `warnings[]` and source;
  - `notice`, `unassessed_criteria[]`, `excluded[]`.
- An engine router will try the primary engine and fall back to a secondary one, recording `engine_used` and `fallback_used`
  and a `pawmatch.recommendation.fallback` metric.

### Reference: the team's baseline (`pawmatch-baseline-1.0.0`, owned by Ihor)

This is a weighted-rules model over 8 catalogue scores: energy, shedding, coat grooming, mental stimulation, barking, size,
good with young children, good with other dogs.

- **Exclusions** run before scoring: `allowed_sizes`, `hard_limits`, and breed scores below 3/5 for children or other dogs
  when those apply to the user.
- **Per-criterion match** is 0–1:
  - activity: `1 − abs(b − p) / 4`;
  - capacity criteria: `1 − max(0, b − p) / 4`;
  - size: category distance.
- **Weights**: activity 3; shedding, grooming and mental stimulation 2; barking and size 1; children and other dogs 1.5.
- **Index**: `round(100 · Σ(w·s) / Σw, 1)`.
- These answers are collected but not scored, and are reported as `unassessed_criteria`: walk minutes, alone hours, training minutes,
  owner experience, housing type, cats.

### Questionnaire fields (14, all required)

| Field | Values |
|---|---|
| `activity_level`, `shedding_tolerance`, `grooming_capacity`, `mental_stimulation_capacity`, `barking_tolerance` | int 1–5 |
| `preferred_size` | `XSmall` / `Small` / `Medium` / `Large` / `XLarge` / `any` (soft preference) |
| `has_young_children`, `has_other_dogs`, `has_cats` | bool (strict: no 0/1 or strings) |
| `housing_type` | `apartment` / `house` / `other` |
| `walk_minutes_daily`, `training_minutes_daily` | 0–1440 |
| `alone_hours_daily` | 0–24 |
| `owner_experience` | `none` / `some` / `experienced` |

There are also two optional fields:
- `allowed_sizes`: a non-empty list of unique sizes, applied as a hard filter;
- `hard_limits`: maximum allowed values from 1–5 for `energy_level`, `shedding_level`, `coat_grooming_frequency`,
  `mental_stimulation_needs` and `barking_level`.

Unknown fields are rejected. All errors are collected before responding.

### Data model (PostgreSQL, Liquibase)

- `breed`
- `questionnaire_session`
- `questionnaire_answers` (versioned, `jsonb`)
- `recommendation_run` (`result` as `jsonb`, plus model and dataset version, latency, `engine_used`, `fallback_used`)
- `recommendation_item` (rank, breed, index)
- `feedback`
- Later: `expert_label` (0 / 0.5 / 1), needed to train the ML model and to measure Precision@3 / NDCG@5.

## Technologies

- **Java 25**, **Spring Boot 4.1.2**, Gradle (wrapper), Lombok
- Spring Web MVC, Spring Data JPA, **Liquibase** migrations
- **PostgreSQL** is the database (MongoDB was removed in M0).
- Testcontainers, springdoc-openapi, Actuator + Micrometer (Prometheus), docker compose, GitHub Actions CI

## Commands (Windows: use `gradlew.bat` or `../gradlew` in Git Bash)

```
./gradlew build                     # compile + all tests (needs a container runtime for Testcontainers)
./gradlew build -PskipDockerTests   # skip tests tagged @Tag("docker"); use this locally
./gradlew bootRun                   # run locally (profile "local", expects Postgres from compose.yaml)
docker compose up -d                # start local Postgres (or: podman compose up -d)
```

- Tests that need a database use `@Import(TestcontainersConfiguration.class)` and **must** be tagged `@Tag("docker")`.
- Locally, the Podman VM currently can't reach Docker Hub (`no route to host`), so run local builds with
  `-PskipDockerTests`. CI (`.github/workflows/ci.yml` at the repo root) runs the full suite.
- Local URLs: Swagger UI `/swagger-ui`, OpenAPI `/v3/api-docs`, health `/actuator/health`.

## Conventions

- REST under `/api/v1`. JSON fields use `snake_case`, to match the model contract.
  Errors are RFC 9457 `ProblemDetail`; validation errors add `field_errors: [{field, message}]`.
- User-facing texts (explanations, warnings, validation messages) are in **Ukrainian**. Code, comments and docs are in English.
- All schema changes go through Liquibase changesets. Never use `ddl-auto` in non-test profiles.
- Never log questionnaire answers or other user input. Log run ID, engine, model version and latency.
- Every recommendation result stores `model_version` and `dataset_version`, so it can be reproduced.
- Keep the engine behind the `ScoringEngine` interface. Nothing outside `inference` should know which engine is running.

## Team

- **Maksym**: backend and model integration (service side, monitoring).
- **Ihor**: ML (baseline, datasets, model quality).
- **Anna**: product (vision board, story map).
