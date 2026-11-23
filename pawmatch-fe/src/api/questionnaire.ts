import { useMutation, useQuery } from '@tanstack/react-query'

import { apiFetch } from '@/api/client'

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

/** Starts a guest session with the given answers (POST /sessions). Rejects with ApiError; 422 carries field errors. */
export function useSubmitAnswers() {
  return useMutation({
    mutationFn: (answers: AnswersPayload) =>
      apiFetch<SubmittedAnswers>('/api/v1/sessions', { method: 'POST', body: JSON.stringify(answers) }),
  })
}
