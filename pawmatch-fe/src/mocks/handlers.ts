import { http, HttpResponse } from 'msw'

import { API_BASE_URL } from '@/api/client'
import { questionnaireContract } from '@/mocks/fixtures/questionnaire'
import { recommendationRun } from '@/mocks/fixtures/recommendations'

const api = (path: string) => `${API_BASE_URL}/api/v1${path}`

/** A fixed UUIDv7-shaped id, like the backend generates. */
/** The backend's example profile (docs/examples/questionnaire-sessions/01-submit-required-only.json). */
export const exampleAnswers = {
  activity_level: 3,
  shedding_tolerance: 2,
  grooming_capacity: 2,
  mental_stimulation_capacity: 3,
  barking_tolerance: 2,
  preferred_size: 'Small',
  has_young_children: true,
  has_other_dogs: false,
  has_cats: false,
  housing_type: 'apartment',
  walk_minutes_daily: 45,
  alone_hours_daily: 6,
  owner_experience: 'none',
  training_minutes_daily: 20,
}

export const MOCK_SESSION_ID = '0190f2a0-0000-7000-8000-000000000001'

/**
 * Mock handlers shaped exactly like the backend contract. Used by tests and, with VITE_API_MOCKING=true, in
 * development for endpoints the backend doesn't have yet. Add a handler per endpoint as features need it.
 */
export const handlers = [
  http.get(api('/questionnaire'), () => HttpResponse.json(questionnaireContract)),
  // POST /sessions: 201 with the stored answers' metadata (validation is the backend's job; tests override for 422).
  http.post(api('/sessions'), () =>
    HttpResponse.json(
      {
        session_id: MOCK_SESSION_ID,
        answers_version: 1,
        questionnaire_version: questionnaireContract.version,
        submitted_at: '2026-11-23T10:00:00Z',
      },
      { status: 201, headers: { Location: api(`/sessions/${MOCK_SESSION_ID}/answers`) } },
    ),
  ),
  // GET answers: the backend's example profile, as if submitted earlier in the visit.
  http.get(api('/sessions/:sessionId/answers'), ({ params }) =>
    HttpResponse.json({
      session_id: params.sessionId,
      answers_version: 1,
      questionnaire_version: questionnaireContract.version,
      submitted_at: '2026-11-23T10:00:00Z',
      answers: exampleAnswers,
    }),
  ),
  http.put(api('/sessions/:sessionId/answers'), ({ params }) =>
    HttpResponse.json({
      session_id: params.sessionId,
      answers_version: 2,
      questionnaire_version: questionnaireContract.version,
      submitted_at: '2026-11-23T10:05:00Z',
    }),
  ),
  // Recommendations: the real baseline output for the example profile, whatever the answers.
  http.post(api('/sessions/:sessionId/recommendations'), ({ params }) =>
    HttpResponse.json({ ...recommendationRun, session_id: params.sessionId }, { status: 201 }),
  ),
  // No stored run yet: the results screen then runs the model (POST above).
  http.get(api('/sessions/:sessionId/recommendations/latest'), () =>
    HttpResponse.json(
      { type: 'about:blank', title: 'Not Found', status: 404, detail: 'No recommendation run yet' },
      { status: 404, headers: { 'Content-Type': 'application/problem+json' } },
    ),
  ),
]
