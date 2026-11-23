import type { RecommendedBreed } from '@/api/recommendations'
import { copy } from '@/copy/uk'

/** The compare screen takes 2–5 breeds. */
export const MIN_COMPARE = 2
export const MAX_COMPARE = 5

/** The index arrives as a decimal; the UI shows whole points. */
export function displayIndex(breed: RecommendedBreed): number {
  return Math.round(breed.compatibility_index)
}

function factorValue(breed: RecommendedBreed, criterion: string): number | string | undefined {
  return breed.factors.find((factor) => factor.criterion === criterion)?.breed_value
}

/**
 * Characteristic tags under the breed name (Figma: "Малий", "Помірна активність", "Невелике линяння"), read from the
 * model's factors. A characteristic the model didn't report gets no tag.
 */
export function breedTags(breed: RecommendedBreed): string[] {
  const sizes: Record<string, string> = copy.questionnaire.options.preferred_size ?? {}
  const size = factorValue(breed, 'size')
  const activity = factorValue(breed, 'activity')
  const shedding = factorValue(breed, 'shedding')
  return [
    typeof size === 'string' ? sizes[size] : undefined,
    typeof activity === 'number' ? copy.results.tags.activity[activity] : undefined,
    typeof shedding === 'number' ? copy.results.tags.shedding[shedding] : undefined,
  ].filter((tag): tag is string => tag !== undefined)
}
