import { useQuery } from '@tanstack/react-query'

import { ApiError, apiFetch } from '@/api/client'

/**
 * Recommendation endpoints (backend M3). Types mirror RecommendationRunResponse until they're generated from the
 * OpenAPI spec. Texts (reasons, warnings, notice, score_meaning, unassessed_criteria) come from the model in Ukrainian
 * and are shown as-is.
 */

export interface RecommendationFactor {
  criterion: string
  label: string
  /** Breed rating (1–5) or size code. */
  breed_value: number | string
  /** The user's answer: rating, size code or `true`. */
  profile_value: number | string | boolean
  match: number
  weight: number
  explanation: string
}

export interface RecommendedBreed {
  /** The breed slug. */
  breed_id: string
  breed_name: string
  breed_name_en: string
  compatibility_index: number
  factors: RecommendationFactor[]
  reasons: string[]
  warnings: string[]
  source_url: string
  image_url: string | null
}

export interface Exclusion {
  breed_id: string
  reasons: string[]
}

export interface RecommendationRun {
  run_id: string
  session_id: string
  answers_version: number
  created_at: string
  model_version: string
  model_type: string
  score_meaning: string
  min_score: number
  dataset_version: string
  catalogue_breeds: number
  eligible_breeds: number
  above_threshold: number
  /** 0–5 breeds, best first; never padded with weak matches. */
  recommendations: RecommendedBreed[]
  notice: string
  unassessed_criteria: string[]
  excluded: Exclusion[]
}

export const recommendationKeys = {
  session: (sessionId: string) => ['recommendations', sessionId] as const,
}

const path = (sessionId: string) => `/api/v1/sessions/${encodeURIComponent(sessionId)}/recommendations`

/**
 * Recommendations for the session's current answers. Reuses the latest stored run when it was computed from answers
 * at least as new as `answersVersion` (e.g. after a page reload); otherwise runs the model again. A 404 from the
 * POST means the session expired or never existed.
 */
export function useRecommendations(sessionId: string | null, answersVersion: number) {
  return useQuery({
    queryKey: recommendationKeys.session(sessionId ?? ''),
    enabled: sessionId !== null,
    staleTime: Infinity,
    queryFn: async () => {
      const id = sessionId!
      try {
        const latest = await apiFetch<RecommendationRun>(`${path(id)}/latest`)
        if (latest.answers_version >= answersVersion) return latest
      } catch (error) {
        // 404 here also covers "no run yet"; the POST below tells that apart from a missing session.
        if (!(error instanceof ApiError && error.status === 404)) throw error
      }
      return apiFetch<RecommendationRun>(path(id), { method: 'POST' })
    },
  })
}
