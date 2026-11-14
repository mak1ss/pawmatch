# CLAUDE.md

Guidance for Claude Code when working in this repository.

> **More context:** see [`docs/ROADMAP.md`](docs/ROADMAP.md) for milestones, decisions and open questions.
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
- **Breed catalogue**: 30 hand-checked AKC breeds (dataset v1.0.0), each with a source URL. Unknown values are `NULL`,
  never `0`, and the API sends them as explicit `null`.
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

**Hexagonal architecture (ports and adapters).** The code is split into feature modules, and each module is split into layers:

```
com.pawmatch
├── catalog         breeds: catalogue, breed cards, breed images, catalogue loader and validator
├── questionnaire   question definitions, profile, validation (field errors → 422)
├── recommendation  sessions, answer versions, recommendation runs/items, compare
├── feedback        feedback on recommendation runs
├── inference       adapters implementing the ScoringEngine port (stub now; baseline / ML later) + engine router
└── common          shared kernel (plain-Java exceptions, FieldError) + cross-cutting web config (ProblemDetail, CORS, OpenAPI)
```

Inside every feature module:

```
<feature>
├── domain                  entities, value objects, domain rules: plain Java, no framework imports
├── application
│   ├── port/in             use-case interfaces (driving ports), e.g. GetBreedUseCase
│   ├── port/out            interfaces the core needs from outside (driven ports), e.g. BreedRepository, BreedImageStorage
│   └── service             use-case implementations; depend only on domain + ports
└── adapter
    ├── in/web              REST controllers + request/response DTOs; call port/in only
    └── out/<tech>          implementations of port/out: persistence (JPA), storage, external services
```

**Boundary rules. Every feature-related change must be checked against these before it's done:**

1. `domain` depends on nothing outside itself, except `common` shared-kernel types. No Spring, JPA, Jackson or Lombok
   annotations that change behaviour. Plain records, classes and enums only.
2. `application` depends only on `domain`, its own ports and the shared kernel. Spring is allowed only for wiring
   (`@Service`, `@Transactional`). No web, JPA or HTTP types.
3. Adapters depend inward, on `application` ports and `domain`, never the other way round. Nothing in `domain` or `application`
   imports from `adapter`.
4. JPA entities and Spring Data repositories live only in `adapter/out/persistence`. They are mapped to domain objects there
   and never returned from a port.
5. Web DTOs live only in `adapter/in/web`. Controllers never return domain objects or JPA entities directly; they map to DTOs.
   Controllers call `port/in` interfaces, never services or repositories.
6. **Cross-feature access goes through the other feature's `port/in` only.** For example, `recommendation` reads breeds through
   `catalog`'s use-case port. It never touches another feature's repositories, entities or adapters.
7. The scoring engine is a driven port, `ScoringEngine`, owned by `recommendation.application.port.out`. Implementations
   live in `inference`. Nothing outside `inference` knows which engine is running.
8. Infrastructure details (file paths, URLs, table names) come from configuration in adapters, never from domain or application code.

When adding or reviewing a feature:
- state which layer and port each new class belongs to;
- check every new import against the rules above;
- flag any violation instead of working around it.

### Inference module (stub for now)

- All matching goes through the `ScoringEngine` port: `recommend(profile, limit, minScore)` and `compare(profile, breedIds)`.
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

**Design principles:**
- The schema is designed for the app. It never mirrors an input file 1:1, and dataset provenance stays with the dataset.
- Every table has a **`uuid` surrogate primary key**.
  - Reference data seeded by migrations (e.g. `breed`) uses `DEFAULT gen_random_uuid()`.
  - High-insert tables (sessions, runs, items, feedback) use **application-generated UUIDv7**
    (`@UuidGenerator(style = VERSION_7)`), so primary-key inserts stay ordered.
- Business keys get their own `UNIQUE` column. For breeds that's `slug`: it's used in URLs, by the scoring model and
  in anything that leaves the database. **UUIDs are internal and differ per environment**, so never hardcode or export them.
- Domain rules are also enforced as DB constraints (`NOT NULL`, `CHECK`, `UNIQUE`, FKs), named `ck_`/`uq_`/`fk_`/`pk_<table>_...`.
- **Catalogue data changes only through Liquibase changesets.** A new dataset version updates rows by slug and inserts
  new breeds. Never delete and re-insert, because other tables reference breed ids. Bump `catalogue_dataset.version`.

**Tables:**
- `breed`: `id`, `slug` (unique), names, group, size, weight, height, life expectancy, coat, 14 ratings (1–5),
  nullable unknowns (`alone_tolerance_hours`, `exercise_minutes_per_day`, `novice_suitability`, `good_with_cats`),
  `source_url`
- `catalogue_dataset` (version of the loaded breed dataset)
- `breed_image` (one per breed: `storage_key`, `content_type`, attribution `source_url`/`author`/`license`); bytes are
  bundled in `src/main/resources/catalog/images`
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

## Commands (Windows: use `gradlew.bat` or `gradlew` in Git Bash)

```
./gradlew build                     # compile + all tests (needs a container runtime for Testcontainers)
./gradlew build -PskipDockerTests   # skip tests tagged @Tag("docker") (no container runtime)
./gradlew bootRun                   # run locally (profile "local", expects Postgres from compose.yaml)
docker compose up -d                # start local Postgres on localhost:5433 (or: podman compose up -d)
```

- Tests that need a database use `@Import(TestcontainersConfiguration.class)` and **must** be tagged `@Tag("docker")`.
- CI (`.github/workflows/ci.yml` at the repo root) runs the full suite.
- Local Postgres listens on host port **5433** (5432 is commonly taken). Override with `PAWMATCH_DB_PORT` (used by both
  `compose.yaml` and the `local` profile), or set `PAWMATCH_DB_URL` directly.
- Local URLs: Swagger UI `/swagger-ui`, OpenAPI `/v3/api-docs`, health `/actuator/health`.

## Conventions

- REST under `/api/v1`. JSON fields use `snake_case`, to match the model contract.
  Errors are RFC 9457 `ProblemDetail`; validation errors add `field_errors: [{field, message}]`.
- User-facing texts (explanations, warnings, validation messages) are in **Ukrainian**. Code, comments and docs are in English.
- All schema changes go through Liquibase changesets. Never use `ddl-auto` in non-test profiles.
- Never log questionnaire answers or other user input. Log run ID, engine, model version and latency.
- Every recommendation result stores `model_version` and `dataset_version`, so it can be reproduced.
- Follow the hexagonal boundary rules in **Architecture**. Every feature change is checked against them.

## Team

- **Maksym**: backend and model integration (service side, monitoring).
- **Ihor**: ML (baseline, datasets, model quality).
- **Anna**: product (vision board, story map).
