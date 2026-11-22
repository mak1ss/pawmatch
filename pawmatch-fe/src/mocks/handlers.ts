import { http, HttpResponse } from 'msw'

import { API_BASE_URL } from '@/api/client'
import { questionnaireContract } from '@/mocks/fixtures/questionnaire'

const api = (path: string) => `${API_BASE_URL}/api/v1${path}`

/**
 * Mock handlers shaped exactly like the backend contract. Used by tests and, with VITE_API_MOCKING=true, in
 * development for endpoints the backend doesn't have yet. Add a handler per endpoint as features need it.
 */
export const handlers = [http.get(api('/questionnaire'), () => HttpResponse.json(questionnaireContract))]
