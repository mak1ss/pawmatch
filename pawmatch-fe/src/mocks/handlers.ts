import { http, HttpResponse } from 'msw'

import { API_BASE_URL } from '@/api/client'
import { questionnaireContract } from '@/mocks/fixtures/questionnaire'

const api = (path: string) => `${API_BASE_URL}/api/v1${path}`

/** A fixed UUIDv7-shaped id, like the backend generates. */
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
]
