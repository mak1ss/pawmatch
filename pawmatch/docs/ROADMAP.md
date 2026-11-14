# PawMatch — MVP Backend Roadmap

> Scope: **backend only, MVP only.** Everything here is from the PawMatch story map (CardBoard) and the
> Team_1_544 Drive folder: Product Vision Board, System-first vs Model-first, ML Canvas, the Sprint 3 dataset and
> baseline-model reports, and the `pawmatch.py` / `breeds.csv` package.

---

## 1. What the MVP is

A visitor answers a short lifestyle questionnaire **without registering** and gets **3–5 dog breeds**. Each breed
has a **compatibility index from 0 to 100** and a short explanation. The visitor can then open breed cards and compare breeds.

| Decision (source) | Backend consequence |
|---|---|
| System-first approach (System-first vs Model-first) | The matching engine is a component behind an interface, so we can swap the baseline for an ML model later without changing the architecture. |
| MVP = versioned weighted-rules baseline, no training (ML Canvas, board #9643227) | We ship the baseline as the production engine. Ridge regression is post-MVP (#9643317). |
| Guest mode is enough for MVP (board #9643225, ML Canvas) | No auth, accounts, profiles, favourites or history in the MVP. |
| 3–5 breeds, threshold ≥ 60, sorted by index (#9643228, #9643300) | `limit=5`, `min_score=60`. If fewer than 3 breeds pass, return only the ones that pass, with a notice. Never pad the list with weak matches. |
| "Index, not probability" (#9643299) | Every response carries `score_meaning`. |
| Fallback to baseline if ML unavailable (#9643298) | Build a `ScoringEngine` seam and a fallback metric now. In the MVP the baseline is the only engine. |
| 12–15 required questions, field-level errors (#9643296) | Server-side validation returns HTTP 422 with a list of field errors. |
| Change answers → recompute (#9643297) | Answers are versioned per guest session. Each change creates a new recommendation run. |
| Optional useful / not-useful feedback (#9643301) | Anonymous feedback is stored per recommendation run. It feeds Precision@3 / NDCG@5 later. |
| Catalogue = 30 hand-checked AKC breeds with source and date (ML Canvas, dataset report) | Breed table is seeded from `breeds.csv` v1.0.0. Unknown values stay `NULL`, never `0`. |
| p95 API ≤ 1 s at 10 concurrent requests, CPU only (ML Canvas) | Engine runs in-memory. We add a load test to the definition of done. |
| Monitoring: daily p95 / errors / fallback; weekly completion, usefulness, empty-result share (ML Canvas) | Actuator + Micrometer, plus SQL-backed product metrics. Max owns the service side; Ihor owns model quality. |

**Explicitly out of MVP** (board marks these "Майбутній реліз" or "потребує узгодження"):
registration, login and logout; saved answers; history; favourites; profile; admin panel and admin auth;
catalogue CRUD UI; system-status UI; access management; shelter dogs; premium features and care advice;
the Ridge/ML model.

---

## 2. Key technical decisions (please confirm)

1. **Inference integration is deferred.** For now the `ScoringEngine` is a **stub** that returns the real response shape,
   so the rest of the system can be built. The integration options are below; the current recommendation is a Java port.
   **Port the baseline to Java (recommended) instead of running a Python sidecar.**
   - `pawmatch.py` is about 150 lines of pure arithmetic and validation with no dependencies.
   - Porting it keeps one deployable, removes a network hop, and makes the p95 target trivial.
   - We prove the port is correct with **parity tests** against Ihor's `example_result.json`, the 60 synthetic
     scenarios (`evaluation.json`) and `test_pawmatch.py`.
   - The Python service only makes sense once the Ridge model exists, and then it sits behind the
     same `ScoringEngine` interface with the Java baseline as fallback.
2. **PostgreSQL + JPA + Liquibase. Drop the MongoDB starter** from `build.gradle`. The data is small and relational
   (the ML Canvas lists `breeds, questionnaire_sessions, answers, recommendations, feedback, expert_labels`), and
   running two databases is unnecessary MVP overhead. Store answers and result payloads as `jsonb`.
3. **Guest session = an unguessable UUID** returned on the first submit. Whoever holds the ID can resubmit answers and
   leave feedback. No PII is collected.
4. **Hexagonal architecture.** Each feature module has `domain`, `application` (ports and services) and `adapter`
   (`in/web`, `out/persistence`, ...) layers. Every feature is checked against the boundary rules in `../CLAUDE.md`.
5. **API style:** REST under `/api/v1`, OpenAPI via springdoc, errors as RFC 9457 `ProblemDetail`, Ukrainian
   explanation texts, as in the baseline.

---

## 3. Milestones

Each milestone ends with something the frontend can call.

### M0 — Foundation (≈ 2–3 days)
- [ ] Remove `spring-boot-starter-mongodb` and its test starter. Add PostgreSQL driver, Testcontainers, springdoc,
      Actuator, Micrometer Prometheus registry, validation.
- [ ] `docker-compose.yml` with Postgres. `application.yaml` with profiles `local`, `test`, `prod`.
- [ ] Package layout: `catalog`, `questionnaire`, `matching`, `recommendation`, `feedback`, `common`
      (errors, config).
- [ ] Global exception handler → `ProblemDetail`. 422 carries `fieldErrors: [{field, message}]`.
- [ ] CORS config for the frontend origin. CI: `./gradlew build` with tests on every push.
- **Done when:** the app boots against Postgres, `/actuator/health` is UP and `/swagger-ui` loads.

### M1 — Breed catalogue (≈ 2–3 days)
*Board: Переглянути породи → Відкрити картку породи, Переглянути характеристики.*
- [x] Liquibase creates a `breed` table designed for the app, not mirroring the dataset CSV:
      - `id uuid` PK (`gen_random_uuid()`), plus `slug` `UNIQUE` as the business key;
      - en/uk names, AKC group, size, weight, height, life expectancy, coat;
      - 14 ratings as `integer NOT NULL CHECK 1–5`;
      - nullable `alone_tolerance_hours`, `exercise_minutes_per_day`, `novice_suitability` and `good_with_cats`
        (empty in v1.0.0);
      - `source_url`.

      Dataset provenance (retrieval date, page hash, review status) stays with the dataset, not in the app.
      `catalogue_dataset` holds the loaded dataset version; `breed_image` holds photo metadata and attribution.
- [x] Seeded from a trimmed `db/changelog/data/breeds-v1.0.0.csv` via Liquibase `loadData`. Database CHECK and UNIQUE
      constraints validate it, so a bad seed fails the migration. The in-app mapping also validates every row.
- [x] `GET /api/v1/breeds` returns a list with the fields needed for cards.
- [x] `GET /api/v1/breeds/{slug}` returns full characteristics, the source URL and photo attribution.
      Unknown values are sent as explicit `null`.
- [x] `GET /api/v1/breeds/{slug}/image` returns the breed photo:
      - the binary image with the correct `Content-Type` (`image/webp` or `image/jpeg`);
      - `Cache-Control` and `ETag` headers, so a repeat request gets `304 Not Modified`;
      - 404 `ProblemDetail` for an unknown breed or a breed without an image.
      The list and detail responses include `image_url`, pointing at this endpoint, or `null` when there's no image.
      The image bytes are read through an outbound port, `BreedImageStorage`. The MVP adapter reads bundled files
      (`catalog/images/<slug>.webp|jpg|png`); the port lets us switch to object storage or a CDN later.
      Attribution (source, author, licence) lives in `breed_image` and is returned in the detail response.
      **The photos themselves are still pending** (open question 7).
- [x] In-memory catalogue cache, loaded once on first access, used by the matching engine.
- **Done when:** the frontend can render the catalogue and breed cards from the API, **including breed photos**.
  The DB-backed parts (migration, seed, constraints) are verified in CI only until local Podman networking is fixed.

### M2 — Questionnaire contract and validation (≈ 2 days)
*Board: Пройти анкету (columns 4–10), #9643296.*
- [ ] `GET /api/v1/questionnaire` returns the question definitions: key, Ukrainian label, type, options and ranges, required flag.
      The frontend renders from this, so the 14 fields aren't hard-coded in two places.
- [ ] Profile DTO with the 14 required fields: `activity_level`, `shedding_tolerance`, `grooming_capacity`,
      `mental_stimulation_capacity`, `barking_tolerance` (1–5); `preferred_size` (5 sizes or `any`);
      `has_young_children`, `has_other_dogs`, `has_cats` (bool); `housing_type`; `walk_minutes_daily`,
      `training_minutes_daily` (0–1440); `alone_hours_daily` (0–24); `owner_experience`.
      Optional fields: `allowed_sizes` (hard filter) and `hard_limits` (max scores).
- [ ] The validator mirrors `validate_profile`: strict types (no 0/1 for booleans), reject unknown fields, and collect all
      errors before throwing.
- **Done when:** a missing, extra or out-of-range answer returns 422 with every offending field listed.

### M3 — Matching engine and recommendations (≈ 4–5 days) ← critical path
*Board: Отримати рекомендації (columns 11–14), #9643227, #9643228, #9643298, #9643299, #9643300.*
- [ ] `ScoringEngine` interface with `assess(profile, breed)`, `recommend(profile, limit, minScore)` and `compare(profile, ids)`.
- [ ] `StubScoringEngine` returns a deterministic response in the real shape (all fields present). It unblocks the API and the frontend.
- [ ] *(Deferred: integration approach not decided.)* `BaselineScoringEngine` is a Java port of `pawmatch-baseline-1.0.0`:
  - weights: activity 3; shedding, grooming and mental stimulation 2; barking and size 1; young children and other dogs 1.5;
  - activity uses `1 − |breed − wanted| / 4`; capacity criteria use `1 − max(0, need − limit) / 4`; size uses category distance;
  - exclusions: `allowed_sizes`, `hard_limits`, and the children / other-dogs < 3/5 rule;
  - `I = round(100 · Σ(w·s) / Σw, 1)`, keep breeds ≥ 60, sort by (−index, breed_id), top 5;
  - output: factors, reasons, warnings, `unassessed_criteria`, `excluded`, `notice`, `score_meaning`,
    `model_version`, `dataset_version`.
- [ ] **Parity test suite** feeds the same inputs through Java and compares with the Python outputs (example profile plus 60 synthetic
      scenarios), and ports the 13 `test_pawmatch.py` cases. Ask Ihor for the scenario inputs and outputs as JSON.
- [ ] Persistence:
      - `questionnaire_session` (uuid, created_at, updated_at);
      - `questionnaire_answers` (session_id, version, answers jsonb);
      - `recommendation_run` (uuid, session_id, answers_version, model_version, dataset_version, min_score, result jsonb,
        latency_ms, engine_used, fallback_used);
      - `recommendation_item` (run_id, rank, breed_id, compatibility_index), which keeps the metrics queries simple.
- [ ] `POST /api/v1/sessions` with answers creates a session and runs the recommendation. It returns `sessionId` and the run.
- [ ] `PUT /api/v1/sessions/{id}/answers` stores a new answers version and returns a new run (recompute).
- [ ] `GET /api/v1/sessions/{id}/recommendations/latest` returns the result again on page reload.
- [ ] Fallback seam: an `EngineRouter` tries the primary engine and falls back to the baseline on exception or timeout,
      increments a `pawmatch.recommendation.fallback` counter and sets `fallback_used`. In the MVP the primary engine is the baseline,
      behind config flag `pawmatch.engine.primary=baseline`.
- **Done when:** the end-to-end guest flow works on the stub engine (questionnaire → response in the real shape) and the
  result is persisted. Once the real engine is integrated: the parity tests are green.

### M4 — Compare breeds (≈ 1–2 days)
*Board: Порівняти породи (columns 18–20).*
- [ ] `POST /api/v1/sessions/{id}/compare` with `{breedIds: [2..5]}` scores each breed against the latest answers.
      Excluded breeds show their reason and no index, as in Python `compare`.
- [ ] Validation: 2–5 unique breed IDs that exist in the catalogue.
- **Done when:** the frontend can show a side-by-side comparison with per-factor match.

### M5 — Feedback (≈ 1 day)
*Board: #9643301.*
- [ ] `feedback` table (run_id, useful bool, optional short comment, created_at), with one feedback per run (upsert).
- [ ] `POST /api/v1/recommendations/{runId}/feedback` with `{useful: true|false, comment?}`.
- **Done when:** feedback is stored and linked to the exact run, model version and breeds shown.

### M6 — Observability and NFRs (≈ 2–3 days)
*ML Canvas "Monitoring" and "Making predictions"; board #9643308 (minimal service-side part).*
- [ ] Micrometer timer `pawmatch.recommendation.latency` with p95/p99, error counter, fallback counter, and
      Actuator `health`, `metrics` and `prometheus`.
- [ ] Structured logs with run ID, engine, model version and latency. No answers in the logs.
- [ ] Product-metrics SQL views:
      - questionnaire completion rate. This needs a "started" event, so either `POST /sessions` on the first screen or
        a lightweight `POST /api/v1/events/started`. **Decide with the frontend team.**
      - usefulness = share of `useful=true` feedback;
      - empty-result share and share of results with fewer than 3 breeds.
- [ ] Load test (k6 or Gatling): 10 concurrent users on `POST /sessions` → p95 ≤ 1 s. Commit the script and the result.
- **Done when:** p95, errors and fallbacks are visible daily and the weekly metrics come from a single query.

### M7 — MVP release hardening (≈ 2 days)
- [ ] Dockerfile, a `prod` profile using env-based DB credentials, and Liquibase run on startup.
- [ ] Input-size limits and basic rate limiting on write endpoints, because the endpoints are anonymous.
- [ ] OpenAPI spec published for the frontend. README covering run, test, the seed catalogue and how to bump `dataset_version`.
- [ ] Smoke test of the full guest journey against the deployed environment.
- **Done when:** a tagged `v0.1.0` is deployed and the frontend runs the whole MVP journey against it.

**Rough total: about 3–4 weeks for one backend developer.** M1 and M2 can run in parallel; M3 is the critical path.

---

## 4. Stretch / needs team agreement (not committed MVP)

The board labels these "Запропонована вимога: потребує узгодження з командою". They matter for the ML path, so
we design the schema to allow them but don't build any UI:

- **CSV catalogue import with validation** (#9643303). Cheap, because the M1 validator already exists. We could expose it as an
  internal endpoint protected by a static admin token.
- **Expert compatibility labels** (#9643307): an `expert_label` table (profile snapshot, breed_id, label 0 / 0.5 / 1,
  labeller, created_at) plus a CSV import. **This is a prerequisite for Ridge and for Precision@3 / NDCG@5**, so it is
  worth pulling in if the ML timeline depends on it.
- **Quality dashboard** (#9643308, the Precision@3 and NDCG@5 part). This only becomes meaningful once expert labels exist.

---

## 5. Open questions for the team

1. **Java port vs Python service.** Does Ihor agree to a Java port with parity tests as the MVP engine, keeping the Python model
   for the post-MVP Ridge service?
2. **Questionnaire vs model fields.** The board lists "Вказати розмір житла" (housing size). The model has no such field and
   only has `housing_type`. Also, `housing_type`, walk minutes, alone hours, experience, training minutes and cats are collected
   but **not scored**; they only appear in `unassessed_criteria`. Is that acceptable UX for the MVP, or should we drop or merge
   questions to stay in the 12–15 range with fewer "unscored" items?
3. **MongoDB.** It is in the initial `build.gradle`. Was there a reason, or can we drop it?
4. **"Started" event for completion rate.** Is it a frontend event or a backend endpoint?
5. **Frontend stack, origin and hosting target.** These are needed for CORS and the M7 deploy.
6. **Admin and expert labelling.** Is any of §4 in MVP scope, given that the ML step depends on labels?
7. **Breed image source and licence.** `breeds.csv` has no image column, and AKC page photos are copyrighted, so we can't
   reuse them. Who provides the 30 photos, and under which licence? Wikimedia Commons (CC BY / CC BY-SA, with attribution)
   or team-owned photos are the safe options. This blocks filling in the image endpoint's data, not building the endpoint.
