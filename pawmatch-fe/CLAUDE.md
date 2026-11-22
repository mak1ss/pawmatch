# CLAUDE.md

Guidance for Claude Code when working on the PawMatch frontend.

> **More context:** the backend lives in [`../pawmatch`](../pawmatch). Read its `CLAUDE.md`, `docs/ROADMAP.md`,
> `docs/frontend-questionnaire.md` (field contract, error codes, draft Ukrainian copy) and
> `docs/examples/questionnaire-sessions/` (example payloads). For product background, analyze the team's Google Drive
> folder **Team_1_544** (Vision Board, ML Canvas, datasets, baseline model) and the **PawMatch** board on CardBoard
> (cardboardit, map_id `237149`, a user story map with the guest and registered-user journeys).

## Product

**PawMatch** recommends dog breeds that fit a person's lifestyle, so people stop choosing by looks or popularity.
The user answers a lifestyle questionnaire and gets **3–5 breeds**, each with a **compatibility index from 0 to 100**,
reasons and warnings. They can open breed cards and compare breeds.

**The index is not a probability** of success. Every results screen must say so, using the `score_meaning` text the
backend sends.

## MVP scope

Two scenarios (team decision 2026-11-15):

- **Guest:** questionnaire and recommendations without registering. Results are **not saved** as history: the guest
  session lives only for the visit.
- **Registered user:** everything a guest can do, plus registration, login and logout, a basic profile, saved
  results, favourite (saved) breeds and questionnaire history.

Screens, following the story map's journey:

| Screen | Board | Notes |
|---|---|---|
| Landing | Ознайомитися з PawMatch | What the service does; start the questionnaire |
| Questionnaire | Пройти анкету | 14 required answers plus 2 optional constraints; client-side checks from the contract; show every 422 field error at once |
| Results | Отримати рекомендації | 3–5 breeds sorted by index; "index, not probability" notice; if fewer than 3, show the backend `notice`; reasons and warnings per breed; optional useful / not-useful feedback; "change answers" recomputes |
| Breed catalogue and card | Переглянути породи | All characteristics; unknown values arrive as `null` and are shown as "unknown", never as 0; source link (AKC) |
| Compare | Порівняти породи | 2–5 breeds side by side; an excluded breed shows its reason and no index |
| Account | Реєстрація / вхід | Register, log in, log out |
| Profile, history, saved breeds | Керувати профілем / результатами | Registered users only |

**Out of MVP:** admin panel, shelter dogs, premium features.

## Backend API

- Base URL locally: `http://localhost:8080`, all endpoints under `/api/v1`. Swagger UI: `/swagger-ui`, spec:
  `/v3/api-docs`. CORS already allows `http://localhost:5173` (Vite's default port).
- JSON is **snake_case**. Errors are RFC 9457 `ProblemDetail`. A 422 adds
  `field_errors: [{ field, code, message, params? }]`: map by `field` and `code`, word the message yourself, and use
  `message` (Ukrainian) only as a fallback. Nested fields use dot notation (`hard_limits.energy_level`).

| Area | Endpoints | Backend status |
|---|---|---|
| Breeds | `GET /breeds`, `GET /breeds/{slug}`, `GET /breeds/{slug}/image` | ✅ built (no photos loaded yet: `image_url` is `null`) |
| Questionnaire | `GET /questionnaire` (contract), `POST /sessions`, `PUT /sessions/{id}/answers`, `GET /sessions/{id}/answers` | ✅ built |
| Recommendations | `POST /sessions/{id}/recommendations`, `GET /sessions/{id}/recommendations/latest` | ⏳ M3, in progress |
| Compare | `POST /sessions/{id}/compare` | ❌ M4 |
| Feedback | `POST /recommendations/{run_id}/feedback` | ❌ M5 |
| Auth, profile, history, saved breeds | `/auth/*`, `/me`, `/me/questionnaires`, `/me/saved-breeds` | ❌ M7–M9 |

Check the backend roadmap for the current status before building against an endpoint. For endpoints that don't exist
yet, build against MSW mocks shaped exactly like the documented contract, and switch to the real API when it lands.

Flow for a guest: `POST /sessions` (answers) → `POST /sessions/{id}/recommendations`. To change answers:
`PUT /sessions/{id}/answers` → `POST /sessions/{id}/recommendations` again.

## Ownership of contracts and copy

- **The backend owns contracts and codes:** field keys, types, ranges, option codes and error codes. Read them from
  `GET /api/v1/questionnaire`; never hardcode ranges or allowed values.
- **The frontend owns all UX copy:** question texts, option labels, scale end labels, units, sections, ordering, help
  texts and error wording. Keep it in one place (`src/copy/`), keyed by field key, option code and error code. The
  draft Ukrainian copy in the backend's `docs/frontend-questionnaire.md` is the starting point, pending product review.
- **Texts the backend generates are shown as-is:** model reasons, warnings, `notice`, `score_meaning`,
  `unassessed_criteria`. They're already in Ukrainian.
- The UI language is **Ukrainian**. Code, comments and docs are in English.

## Design

**Figma:** file "PawMatch (Copy)", key `hveMQoIPFQwJWekuIBF3i8` (the original file isn't shared with our Figma account).
Implement from the high-fidelity mockups: `69:1945` "1 Мокапи Гість (MVP)" (guest screens 01–15, e.g. `69:780`
Головна). Registered-user screens exist only as wireframes (З-01..З-06); admin and future screens are out of MVP.

- **Tokens live in `src/index.css`** (shadcn variables, set from the mockups): cream background `#FFF7EF`, white
  cards, orange primary `#E9692C`, peach accent `#FDE8D7` with text `#B4501C`, border `#F0E2D4`, text `#2B1E14` /
  muted `#8B786A`, radius base 14px (cards `rounded-2xl` ≈ 24px, banners and photos `rounded-3xl` ≈ 32px). Font:
  **Nunito** (`@fontsource-variable/nunito`, with Cyrillic). Use the tokens, never raw hex values in components.
- **Layout:** 1200px content column (`components/layout/Container`), white header and footer
  (`SiteHeader`, `SiteFooter`). Buttons use the shadcn `Button` with the PawMatch additions: size `xl` (56px CTA),
  size `nav` (45px header), variant `outline-primary`.
- The mockups are desktop-only (1440px) and drawn with absolute positions: translate them into responsive layout.
  Static images and icons from Figma go to `src/assets/`; never reference Figma's temporary asset URLs.
- Mobile-first and accessible: semantic HTML, labelled inputs, keyboard navigation, sufficient contrast.

## Stack (installed)

- **TypeScript 5.9** (strict, `noUncheckedIndexedAccess`), **Vite 8**, **React 19**. TypeScript is pinned to `~5.9`:
  typescript-eslint supports up to 6.0 and openapi-typescript declares 5.x, so don't upgrade to 6/7 until they do.
- **React Router 8** for routing. Routes live in `src/app/routes.tsx`, shared by the app and tests.
- **TanStack Query 5** for server state; no extra global store unless a real need appears. Client errors (4xx) are
  never retried.
- **Tailwind CSS 4 + shadcn/ui 4** (style `radix-nova`, PawMatch colour tokens, Lucide icons, Nunito font). Add components
  with `npx shadcn@latest add <name>`; they land in `src/components/ui/`. shadcn 4 uses the `cn` package for class
  merging (`@/lib/utils` re-exports it).
- **React Hook Form** for the questionnaire; client-side checks derived from the contract
- **openapi-typescript** generates API types from the backend's `/v3/api-docs` into `src/api/schema.d.ts`
  (`npm run generate:api`, with the backend running). Until then, shared types are hand-written in `src/api/types.ts`.
- **Vitest 5 + Testing Library** (jsdom) for tests, **MSW 3** for API mocks. In MSW 3 the option for unmatched
  requests is `onUnhandledFrame` (not `onUnhandledRequest`).
- ESLint with typescript-eslint

## Structure

```
src/
├── app/            router, providers (QueryClient), layout, routes
├── api/            fetch client, generated OpenAPI types, query/mutation hooks per endpoint
├── features/
│   ├── questionnaire/
│   ├── recommendations/
│   ├── breeds/
│   ├── compare/
│   ├── account/    (M7+)
│   └── profile/    (M8+: history, saved breeds)
├── components/ui/  shadcn/ui components
├── copy/           Ukrainian UX copy keyed by field key, option code and error code
├── mocks/          MSW handlers and fixtures shaped like the backend contract (browser worker + node server)
└── test/           test setup and helpers (renderRoute)
```

- `src/api/client.ts`: `apiFetch` (base URL from `VITE_API_BASE_URL`) and `ApiError` with `status`, `problem` and
  `fieldErrors`.
- Mocks in development: set `VITE_API_MOCKING=true` in `.env.local` (see `.env.example`). In tests, MSW always runs
  and any unmocked request fails the test.

## Conventions

- Use the generated API types as they are (snake_case) in `src/api/`. Components use the hooks, never `fetch` directly.
- The guest `session_id` is kept for the current visit only (sessionStorage), never in long-lived storage.
  Authentication tokens: follow the backend's M7 decision (access token in memory, refresh token handling per the
  backend design). Never put tokens in localStorage.
- Never log questionnaire answers or tokens to the console or analytics.
- Show loading, empty and error states for every server call. A 404 on a session means it expired or never existed:
  offer to start the questionnaire again.

## Commands

```
npm install
npm run dev            # http://localhost:5173
npm run build          # type-check + production bundle
npm run typecheck
npm run lint
npm test               # Vitest, once
npm run test:watch
npm run generate:api   # API types from the running backend
```

Running the backend locally (Podman works on this machine):

```
cd ../pawmatch
podman compose up -d        # Postgres on localhost:5433
./gradlew bootRun           # API on http://localhost:8080
```

## Git

The git repository root is one level up (`../`). The frontend lives in `pawmatch-fe/`, the backend in `pawmatch/`.
Branches are named `feature/<topic>`. Never commit `node_modules`, `dist` or `.idea`.
