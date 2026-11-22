# PawMatch — MVP Backend Roadmap

> Scope: **backend only, MVP only.** Everything here is from the PawMatch story map (CardBoard) and the
> Team_1_544 Drive folder: Product Vision Board, System-first vs Model-first, ML Canvas, the Sprint 3 dataset and
> baseline-model reports, and the `pawmatch.py` / `breeds.csv` package.
>
> **Scope update (2026-11-15, team decision):** registration, login, a basic profile, saved results and questionnaire
> history are **in the MVP**. This overrides the "Майбутній реліз" labels on the board's registered-user cards.

---

## 1. What the MVP is

The MVP supports **two scenarios**:

- **Guest:** answers a short lifestyle questionnaire **without registering** and gets **3–5 dog breeds**. Each breed has a
  **compatibility index from 0 to 100** and a short explanation. The guest can open breed cards and compare breeds.
  **A guest's results are not saved**: there is no history to come back to.
- **Registered user:** everything a guest can do, plus a **profile**, **saved results** (recommendations and favourite breeds)
  and the **history of completed questionnaires**.

> Original (team decision, Ukrainian): «У нас передбачено два сценарії: гість може пройти анкету й отримати рекомендації без
> реєстрації, але його результати не зберігаються. Зареєстрований користувач додатково матиме профіль, можливість зберігати
> результати та переглядати історію заповнення анкет. Тобто авторизація, реєстрація та базовий функціонал профілю також
> входять у поточний обсяг MVP.»

| Decision (source) | Backend consequence |
|---|---|
| System-first approach (System-first vs Model-first) | The matching engine is a component behind an interface, so we can swap the baseline for an ML model later without changing the architecture. |
| MVP = versioned weighted-rules baseline, no training (ML Canvas, board #9643227) | We ship the baseline as the production engine. Ridge regression is post-MVP (#9643317). |
| Two scenarios, guest and registered (team decision 2026-11-15; board "ЗАРЕЄСТРОВАНИЙ КОРИСТУВАЧ" columns 22–35) | Every guest endpoint keeps working without auth. Registration, login, logout, profile, saved results, favourite breeds and questionnaire history are added for authenticated users (M7–M9). The same questionnaire and recommendation endpoints serve both: with a valid token the run is linked to the user. |
| Guest results are not saved (team decision) | Guest runs are kept only as **anonymous technical records** (needed for recompute within the visit, feedback and metrics) and are never retrievable as history. See open question 8. |
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
admin panel and admin auth; catalogue CRUD UI; system-status UI; access management; shelter dogs; premium features
and care advice; the Ridge/ML model.

---

## 2. Key technical decisions (please confirm)

1. **Decided (M3): the baseline is ported to Java** (`inference.baseline.BaselineEngine`) instead of running a Python
   sidecar, with parity proven against the Python reference (see M3). The reasoning:
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
   leave feedback. No PII is collected for guests. A questionnaire session has a nullable `user_id`: `NULL` for guests,
   set when an authenticated user submits. Only user-owned sessions appear in history.
6. **Authentication (recommended, please confirm):**
   - Spring Security with email + password; passwords hashed with Argon2id or BCrypt.
   - **Stateless JWT access token** (short-lived, about 15 min) sent as `Authorization: Bearer`.
   - **Refresh token** (about 30 days) stored **hashed** in the DB, rotated on every refresh and revoked on logout.
   - This works across origins for the SPA without session cookies or CSRF handling.
   - Guest endpoints stay `permitAll`. `/api/v1/me/**` requires a valid token. Questionnaire and recommendation endpoints
     accept an *optional* token.
   - Accounts live in a new hexagonal module, `account`. Security filters are web adapters.
4. **Hexagonal architecture.** Each feature module has `domain`, `application` (ports and services) and `adapter`
   (`in/web`, `out/persistence`, ...) layers. Every feature is checked against the boundary rules in `../CLAUDE.md`.
5. **API style:** REST under `/api/v1`, OpenAPI via springdoc, errors as RFC 9457 `ProblemDetail`, Ukrainian
   explanation texts, as in the baseline.

---

## 3. Milestones

Each milestone ends with something the frontend can call.

### M0 — Foundation (≈ 2–3 days)
- [x] Remove `spring-boot-starter-mongodb` and its test starter. Add PostgreSQL driver, Testcontainers, springdoc,
      Actuator, Micrometer Prometheus registry, validation.
- [x] `docker-compose.yml` with Postgres. `application.yaml` with profiles `local`, `test`, `prod`.
- [x] Package layout: `catalog`, `questionnaire`, `matching`, `recommendation`, `feedback`, `common`
      (errors, config).
- [x] Global exception handler → `ProblemDetail`. 422 carries `fieldErrors: [{field, message}]`.
- [x] CORS config for the frontend origin. CI: `./gradlew build` with tests on every push.
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

### M2 — Questionnaire: contract, validation and answer submission (≈ 3 days)
*Board: Пройти анкету (columns 4–10), #9643296, Змінити відповіді #9643297, Надіслати відповіді #9642240/#9643226.*

The `questionnaire` module owns sessions and answers. Other modules read answers only through its inbound port.
- [x] `GET /api/v1/questionnaire` returns the **machine-readable contract**: `{ version, fields: [{ key, type, required,
      min?, max?, options? }] }`. The frontend builds the form from it, so keys, ranges and codes aren't hand-copied.
      **No wording on the backend:** question texts, option labels, sections, order and units are frontend copy,
      mapped by key and code (see `docs/frontend-questionnaire.md`).
- [x] Validation errors carry a stable `code` and `params` (`min`, `max`, `allowed`) next to the Ukrainian fallback
      `message`, so the frontend can word errors itself.
- [x] Profile DTO with the 14 required fields: `activity_level`, `shedding_tolerance`, `grooming_capacity`,
      `mental_stimulation_capacity`, `barking_tolerance` (1–5); `preferred_size` (5 sizes or `any`);
      `has_young_children`, `has_other_dogs`, `has_cats` (bool); `housing_type`; `walk_minutes_daily`,
      `training_minutes_daily` (0–1440); `alone_hours_daily` (0–24); `owner_experience`.
      Optional fields: `allowed_sizes` (hard filter) and `hard_limits` (max scores).
- [x] The validator mirrors `validate_profile`: strict types (no 0/1 for booleans), reject unknown fields, and collect all
      errors before throwing.
- [x] Persistence:
      - `questionnaire_session` (`id` UUIDv7, `created_at`, `updated_at`; `user_id` is added in M8);
      - `questionnaire_answers` (`session_id` FK, `version`, `questionnaire_version`, `answers` jsonb, `created_at`;
        `UNIQUE (session_id, version)`).
- [x] `POST /api/v1/sessions` with answers: validate, create the session, store answers version 1, and return
      `201 { session_id, answers_version }` with a `Location` header. Invalid input gets 422.
- [x] `PUT /api/v1/sessions/{id}/answers`: validate and store the next answers version, returning `200 { session_id, answers_version }`.
      An unknown session gets 404.
- [x] `GET /api/v1/sessions/{id}/answers` returns the latest answers, so the frontend can prefill "change answers"
      (#9643297). The `POST` `Location` header points here.
- [x] Inbound port `GetLatestAnswersUseCase` (session id → latest answers and version). `recommendation` (M3) and compare
      (M4) read answers through it, never through the questionnaire's repository.
- **Done when:** the frontend can submit and resubmit answers. Invalid input returns 422 with every offending field
  listed, and valid input is stored as a new answers version.

### M3 — Matching engine and recommendations (≈ 3–4 days) ← critical path
*Board: Отримати рекомендації (columns 11–14), #9643227, #9643228, #9643298, #9643299, #9643300.*
- [x] `ScoringEngine` port (owned by `recommendation`) with `recommend(profile, breeds, limit, minScore)` and
      `compare(profile, breeds)`. Engines are pure: the service passes `ScoringProfile` + `BreedFeatures`.
- [x] ~~`StubScoringEngine`~~: skipped; the real baseline was ported instead.
- [x] `BaselineEngine` is a Java port of `pawmatch-baseline-1.0.0`:
  - weights: activity 3; shedding, grooming and mental stimulation 2; barking and size 1; young children and other dogs 1.5;
  - activity uses `1 − |breed − wanted| / 4`; capacity criteria use `1 − max(0, need − limit) / 4`; size uses category distance;
  - exclusions: `allowed_sizes`, `hard_limits`, and the children / other-dogs < 3/5 rule;
  - `I = round(100 · Σ(w·s) / Σw, 1)`, keep breeds ≥ 60, sort by (−index, breed_id), top 5;
  - output: factors, reasons, warnings, `unassessed_criteria`, `excluded`, `notice`, `score_meaning`,
    `model_version`, `dataset_version`.
- [x] **Parity test suite:**
      - the ML team's `example_result.json` reproduced exactly;
      - **192 golden cases** (12 edge cases, 150 seeded random profiles, 30 compare cases) generated by running the Python
        reference on our seed catalogue (`tools/baseline-parity`), compared field by field;
      - the 10 behavioural `test_pawmatch.py` cases ported (validation cases live in `AnswersParserTest`);
      - invariants over 2,000 random profiles.

      Ihor's 60 synthetic scenarios are no longer needed: their inputs weren't in the package, and the generated
      goldens cover more cases.
- [x] Persistence:
      - `recommendation_run` (`id` UUIDv7, `session_id` FK, `answers_version`, `model_version`, `dataset_version`,
        `min_score`, `result` jsonb, `latency_ms`, `engine_used`, `fallback_used`, `created_at`);
      - `recommendation_item` (`run_id`, `rank`, `breed_id` FK, `compatibility_index`), which keeps the metrics queries simple.
      - plus a composite FK `(session_id, answers_version)` → `questionnaire_answers` and a `dataset_version` FK.
- [x] `POST /api/v1/sessions/{id}/recommendations` reads the latest answers through the questionnaire's
      `GetLatestAnswersUseCase`, runs the engine, stores a run and returns it (201).
      - After the answers change (M2 `PUT`), the frontend calls it again to recompute.
      - Calling it again without changes simply creates another run on the same answers version.
      - An unknown session gets 404.
- [x] `GET /api/v1/sessions/{id}/recommendations/latest` returns the result again on page reload.
- [x] Flow for the frontend: `POST /sessions` (M2) → `POST /sessions/{id}/recommendations`; after editing answers,
      `PUT /sessions/{id}/answers` → `POST /sessions/{id}/recommendations`. That's two calls by design: the questionnaire
      module must not depend on `recommendation`.
- [x] Fallback seam: an `EngineRouter` tries the primary engine and falls back to a configured engine on an exception,
      increments a `pawmatch.recommendation.fallback` counter and sets `fallback_used`. In the MVP the primary engine is the baseline,
      behind `pawmatch.inference.primary=baseline` (`pawmatch.inference.fallback` empty in the MVP).
- **Done when:** the end-to-end guest flow works on the real baseline (questionnaire → recommendations) and the
  result is persisted, and the parity tests are green. Recommended breeds include `image_url`.

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
      - questionnaire completion rate. This needs a "started" event. `POST /sessions` only happens on submit, so use either
        a lightweight `POST /api/v1/events/started` or frontend analytics. **Decide with the frontend team.**
      - usefulness = share of `useful=true` feedback;
      - empty-result share and share of results with fewer than 3 breeds.
- [ ] Load test (k6 or Gatling): 10 concurrent users running submit → recommend (`POST /sessions`, then
      `POST /sessions/{id}/recommendations`) → p95 ≤ 1 s per request. Commit the script and the result.
- **Done when:** p95, errors and fallbacks are visible daily and the weekly metrics come from a single query.

### M7 — Accounts and authentication (≈ 3–4 days)
*Board: РЕЄСТРАЦІЯ / ВХІД: Створити обліковий запис #9643238/#9643239, Увійти в систему #9643240/#9643241,
Вийти з облікового запису #9643242/#9643243.*
- [ ] New module `account` (domain `UserAccount`, ports, service, adapters), checked against the boundary rules.
- [ ] Schema:
      - `user_account` (`id` UUIDv7, `email` with a unique index on `lower(email)`, `password_hash`, `display_name`,
        `created_at`, `updated_at`);
      - `refresh_token` (`id`, `user_id` FK, `token_hash` unique, `expires_at`, `revoked_at`, `created_at`).
- [ ] `POST /api/v1/auth/register` (email, password, display name):
      - 201 with tokens;
      - 409 if the email is taken;
      - 422 with field errors for a bad email or a weak password (minimum 8 characters).
- [ ] `POST /api/v1/auth/login` returns access and refresh tokens. A failed login gets a generic 401 that doesn't say
      whether the email exists.
- [ ] `POST /api/v1/auth/refresh` rotates the refresh token. `POST /api/v1/auth/logout` revokes it.
- [ ] Spring Security configuration:
      - guest endpoints `permitAll`;
      - `/api/v1/me/**` authenticated;
      - optional authentication on questionnaire and recommendation endpoints;
      - 401/403 returned as `ProblemDetail`;
      - CORS updated for the `Authorization` header.
- [ ] Rate limiting on login and register. Passwords and tokens never logged.
- [ ] Add `account` to the ArchUnit module list and to CLAUDE.md.
- **Done when:** a user can register, log in, refresh and log out, and every guest endpoint still works without a token.

### M8 — Profile, saved results and questionnaire history (≈ 3 days)
*Board:*
- *КЕРУВАТИ ПРОФІЛЕМ: Переглянути профіль #9643266/#9643261, Редагувати профіль #9643262/#9643265,
  Переглянути історію анкет #9643264/#9643263;*
- *ПРОЙТИ АНКЕТУ (registered): Зберегти відповіді #9643247/#9643248;*
- *ОТРИМАТИ РЕКОМЕНДАЦІЇ (registered): Переглянути попередні рекомендації #9643253/#9643254.*
- [ ] `questionnaire_session.user_id` (nullable FK → `user_account`). With a valid token, the M2 endpoints `POST /sessions` and
      `PUT /sessions/{id}/answers` link the session to the user, so answers and runs are saved. Without a token they stay guest.
- [ ] `GET /api/v1/me` returns the profile (email, display name, member since). `PATCH /api/v1/me` edits the display name.
      Email and password changes are out of scope unless the team asks (open question 9).
- [ ] `GET /api/v1/me/questionnaires` returns the history: date, short answers summary and top breeds of the latest run,
      newest first, paginated.
- [ ] `GET /api/v1/me/questionnaires/{id}` returns the full answers and the recommendation runs of that questionnaire.
      Another user's id gets 404, never 403, so ids don't leak.
- [ ] `GET /api/v1/me/questionnaires/latest/answers` lets the frontend prefill the questionnaire for a returning user.
- **Done when:** a logged-in user completes the questionnaire, sees it in their history after logging in again, and can
  reopen any past result.

### M9 — Saved (favourite) breeds (≈ 1–2 days)
*Board: КЕРУВАТИ РЕЗУЛЬТАТАМИ: Зберегти породу #9643255/#9643256, Переглянути вибрані породи #9643257/#9643258,
Видалити породу з вибраного #9643259/#9643260.*
- [ ] New module `shortlist`. It reads breeds only through `catalog`'s inbound ports (rule 6).
- [ ] `saved_breed` (`id` UUIDv7, `user_id` FK → `user_account`, `breed_id` FK → `breed`, `saved_at`),
      `uq_saved_breed_user_breed UNIQUE (user_id, breed_id)`, index on `(user_id, saved_at DESC)`.
- [ ] Endpoints:
      - `PUT /api/v1/me/saved-breeds/{slug}` is idempotent: 201 the first time, 200 after;
      - `GET /api/v1/me/saved-breeds` returns breed summaries plus `saved_at`, newest first;
      - `DELETE /api/v1/me/saved-breeds/{slug}` returns 204, also if the breed wasn't saved.

      An unknown slug gets 404.
- **Done when:** a logged-in user can save, list and remove breeds, and the list survives logging out and back in.

### M10 — MVP release hardening (≈ 2 days)
- [ ] Dockerfile, a `prod` profile using env-based DB credentials and JWT signing key, and Liquibase run on startup.
- [ ] Input-size limits and basic rate limiting on write endpoints, especially the anonymous ones.
- [ ] OpenAPI spec published for the frontend, including the bearer-auth scheme. README covering run, test, the seed
      catalogue and how to bump `dataset_version`.
- [ ] Smoke test of both journeys (guest, and registered: register → questionnaire → save breed → history) against the
      deployed environment.
- **Done when:** a tagged `v0.1.0` is deployed and the frontend runs both MVP journeys against it.

**Rough total: about 5–6 weeks for one backend developer** (it was 3–4 weeks before accounts joined the MVP).
M1 and M2 can run in parallel; M3 needs M1 and M2 and is the critical path. M7 can start any time after M0 and runs in parallel with M3–M6.
M8 needs M3 and M7, and M9 needs M1 and M7.

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

1. ~~**Java port vs Python service.**~~ Resolved in M3: Java port with parity tests. Ihor should still know that
   baseline changes now need the goldens regenerated (`tools/baseline-parity`), and the Python model remains the
   path for the post-MVP Ridge service.
2. **Questionnaire vs model fields.** The board lists "Вказати розмір житла" (housing size). The model has no such field and
   only has `housing_type`. Also, `housing_type`, walk minutes, alone hours, experience, training minutes and cats are collected
   but **not scored**; they only appear in `unassessed_criteria`. Is that acceptable UX for the MVP, or should we drop or merge
   questions to stay in the 12–15 range with fewer "unscored" items?
3. **MongoDB.** It is in the initial `build.gradle`. Was there a reason, or can we drop it?
4. **"Started" event for completion rate.** Is it a frontend event or a backend endpoint?
5. **Frontend stack, origin and hosting target.** These are needed for CORS and the M10 deploy.
6. **Admin and expert labelling.** Is any of §4 in MVP scope, given that the ML step depends on labels?
7. **Breed image source and licence.** `breeds.csv` has no image column, and AKC page photos are copyrighted, so we can't
   reuse them. Who provides the 30 photos, and under which licence? Wikimedia Commons (CC BY / CC BY-SA, with attribution)
   or team-owned photos are the safe options. This blocks filling in the image endpoint's data, not building the endpoint.
8. **What "guest results are not saved" means for the backend.** The guest still needs their run kept during the visit:
   to recompute after changing answers, to compare breeds and to leave feedback. The usefulness metric and Precision@3
   also need runs and feedback. Proposal:
   - store guest runs as anonymous technical records with no link to a person;
   - never show them as history;
   - delete them after a retention period (e.g. 30 days).

   Is that acceptable, or must guest data not be stored at all? In that case feedback and metrics lose guest data.
9. **Account scope details.** Are any of these needed for the MVP?
   - email verification;
   - password reset by email (needs an email provider);
   - changing email or password;
   - social login (e.g. Google);
   - account deletion (GDPR-style).

   My assumption: none for the MVP, except account deletion if the team wants it.
10. **Guest → registered.** If a guest completes the questionnaire and then registers, should that result be attached to
    the new account? That would be one "claim session" call at registration.
11. **Saved breeds details.** Is there a limit on the number of saved breeds? Should a saved breed remember the
    compatibility index from the run it was saved from (`recommendation_run_id`)? Should "compare" offer the saved list?
12. **"Save results" wording.** Does it mean the automatic saving of a logged-in user's questionnaires and runs (M8), an
    explicit "save this result" action, or both? The roadmap assumes automatic saving plus favourite breeds (M9).
