import { queryOptions, useMutation, useQuery } from '@tanstack/react-query'

import { ApiError, apiFetch } from '@/api/client'

/**
 * Questionnaire endpoints. Types mirror the backend contract (GET /api/v1/questionnaire) until they're generated
 * from the OpenAPI spec (`npm run generate:api`).
 */

export type ContractFieldType = 'integer' | 'number' | 'boolean' | 'choice' | 'multi_choice' | 'limits'

/** One answer the backend accepts: key, value shape, range and allowed codes. */
export interface ContractField {
  key: string
  type: ContractFieldType
  required: boolean
  min?: number
  max?: number
  options?: string[]
}

export interface QuestionnaireContract {
  version: string
  fields: ContractField[]
}

/** Request body of POST /sessions and PUT /sessions/{id}/answers (snake_case, exactly as the backend expects). */
export interface AnswersPayload {
  activity_level: number
  shedding_tolerance: number
  grooming_capacity: number
  mental_stimulation_capacity: number
  barking_tolerance: number
  preferred_size: string
  has_young_children: boolean
  has_other_dogs: boolean
  has_cats: boolean
  housing_type: string
  walk_minutes_daily: number
  alone_hours_daily: number
  owner_experience: string
  training_minutes_daily: number
  allowed_sizes?: string[]
  hard_limits?: Record<string, number>
}

export interface SubmittedAnswers {
  session_id: string
  answers_version: number
  questionnaire_version: string
  submitted_at: string
}

export const questionnaireKeys = {
  contract: ['questionnaire', 'contract'] as const,
}

/** The contract changes only with a backend release, so it's fetched once per visit. */
export function useQuestionnaireContract() {
  return useQuery({
    queryKey: questionnaireKeys.contract,
    queryFn: () => apiFetch<QuestionnaireContract>('/api/v1/questionnaire'),
    staleTime: Infinity,
  })
}

/**
 * Saves the answers. With a session id it replaces that session's answers (PUT /sessions/{id}/answers), so changing
 * answers keeps the session; without one, or when that session has expired (404), it starts a new session
 * (POST /sessions). Rejects with ApiError; 422 carries field errors.
 */
export function useSubmitAnswers() {
  return useMutation({
    mutationFn: async ({ answers, sessionId }: { answers: AnswersPayload; sessionId: string | null }) => {
      const body = JSON.stringify(answers)
      if (sessionId) {
        try {
          const path = `/api/v1/sessions/${encodeURIComponent(sessionId)}/answers`
          return await apiFetch<SubmittedAnswers>(path, { method: 'PUT', body })
        } catch (error) {
          if (!(error instanceof ApiError && error.status === 404)) throw error
        }
      }
      return apiFetch<SubmittedAnswers>('/api/v1/sessions', { method: 'POST', body })
    },
  })
}

/** GET /sessions/{id}/answers: the session's latest answers, in the same shape they were submitted. */
export interface SessionAnswers extends SubmittedAnswers {
  answers: AnswersPayload
}

export const sessionAnswersKey = (sessionId: string) => ['questionnaire', 'answers', sessionId] as const

/** The latest answers of a guest session, to restore the form after a page reload. A 404 means it expired. */
export function sessionAnswersQuery(sessionId: string) {
  return queryOptions({
    queryKey: sessionAnswersKey(sessionId),
    staleTime: Infinity,
    queryFn: () => apiFetch<SessionAnswers>(`/api/v1/sessions/${encodeURIComponent(sessionId)}/answers`),
  })
}
