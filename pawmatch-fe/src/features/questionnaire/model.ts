import type { AnswersPayload, ContractField, QuestionnaireContract } from '@/api/questionnaire'
import type { FieldError } from '@/api/types'

/**
 * Questionnaire structure, form values and their mapping to the API payload.
 * The order and grouping of questions is frontend-owned (Figma screens 03–07); allowed values and ranges come from the
 * backend contract.
 */

export type AnswerKey = Exclude<keyof AnswersPayload, 'allowed_sizes' | 'hard_limits'>

/** Optional constraints, offered as a checkbox under their question. */
export type StrictKey = 'allowed_sizes' | 'shedding_level' | 'coat_grooming_frequency' | 'barking_level'

export type QuestionKind = 'choice' | 'scale' | 'yes-no' | 'slider' | 'size'

export interface Question {
  field: AnswerKey
  kind: QuestionKind
  /** Upper end of the slider in the UI; the contract may allow more (e.g. 1440 minutes). */
  sliderMax?: number
  unit?: 'hours' | 'minutes'
  strict?: StrictKey
}

export type StepKey = 'housing' | 'lifestyle' | 'family' | 'experience' | 'preferences'

export interface Step {
  key: StepKey
  questions: Question[]
}

export const steps: Step[] = [
  {
    key: 'housing',
    questions: [
      { field: 'housing_type', kind: 'choice' },
      { field: 'alone_hours_daily', kind: 'slider', sliderMax: 24, unit: 'hours' },
    ],
  },
  {
    key: 'lifestyle',
    questions: [
      { field: 'activity_level', kind: 'scale' },
      { field: 'walk_minutes_daily', kind: 'slider', sliderMax: 180, unit: 'minutes' },
    ],
  },
  {
    key: 'family',
    questions: [
      { field: 'has_young_children', kind: 'yes-no' },
      { field: 'has_other_dogs', kind: 'yes-no' },
      { field: 'has_cats', kind: 'yes-no' },
    ],
  },
  {
    key: 'experience',
    questions: [
      { field: 'owner_experience', kind: 'choice' },
      { field: 'training_minutes_daily', kind: 'slider', sliderMax: 60, unit: 'minutes' },
      { field: 'mental_stimulation_capacity', kind: 'scale' },
    ],
  },
  {
    key: 'preferences',
    questions: [
      { field: 'preferred_size', kind: 'size', strict: 'allowed_sizes' },
      { field: 'shedding_tolerance', kind: 'scale', strict: 'shedding_level' },
      { field: 'grooming_capacity', kind: 'scale', strict: 'coat_grooming_frequency' },
      { field: 'barking_tolerance', kind: 'scale', strict: 'barking_level' },
    ],
  },
]

export const TOTAL_STEPS = steps.length

/** Question number shown in the UI (1–14), in step order. */
export function questionNumber(field: AnswerKey): number {
  return steps.flatMap((step) => step.questions).findIndex((question) => question.field === field) + 1
}

/** Form state. `null` means "not answered yet"; sliders always have a value. */
export interface QuestionnaireValues {
  housing_type: string | null
  alone_hours_daily: number
  activity_level: number | null
  walk_minutes_daily: number
  has_young_children: boolean | null
  has_other_dogs: boolean | null
  has_cats: boolean | null
  owner_experience: string | null
  training_minutes_daily: number
  mental_stimulation_capacity: number | null
  preferred_size: string | null
  shedding_tolerance: number | null
  grooming_capacity: number | null
  barking_tolerance: number | null
  strict: Record<StrictKey, boolean>
}

/** Slider starting values follow the mockups; every choice question starts unanswered. */
export const initialValues: QuestionnaireValues = {
  housing_type: null,
  alone_hours_daily: 6,
  activity_level: null,
  walk_minutes_daily: 45,
  has_young_children: null,
  has_other_dogs: null,
  has_cats: null,
  owner_experience: null,
  training_minutes_daily: 20,
  mental_stimulation_capacity: null,
  preferred_size: null,
  shedding_tolerance: null,
  grooming_capacity: null,
  barking_tolerance: null,
  strict: { allowed_sizes: false, shedding_level: false, coat_grooming_frequency: false, barking_level: false },
}

export function contractField(contract: QuestionnaireContract, key: string): ContractField | undefined {
  return contract.fields.find((field) => field.key === key)
}

/** Allowed values of a choice or scale question, in contract order. */
export function optionCodes(contract: QuestionnaireContract, field: AnswerKey): string[] {
  const spec = contractField(contract, field)
  if (!spec) return []
  if (spec.type === 'integer' && spec.min !== undefined && spec.max !== undefined) {
    return Array.from({ length: spec.max - spec.min + 1 }, (_, i) => String(spec.min! + i))
  }
  return spec.options ?? []
}

/** Slider bounds: the contract's range, capped at the UI maximum. */
export function sliderRange(contract: QuestionnaireContract, question: Question): { min: number; max: number } {
  const spec = contractField(contract, question.field)
  const min = spec?.min ?? 0
  const max = Math.min(spec?.max ?? question.sliderMax ?? 100, question.sliderMax ?? Infinity)
  return { min, max }
}

/** Field keys of a step's unanswered questions, i.e. its client-side validation errors. */
export function unansweredFields(step: Step, values: QuestionnaireValues): AnswerKey[] {
  return step.questions
    .filter((question) => question.kind !== 'slider' && values[question.field] === null)
    .map((question) => question.field)
}

/** Index of the first step with unanswered questions, or null when everything is answered. */
export function firstIncompleteStep(values: QuestionnaireValues): number | null {
  const index = steps.findIndex((step) => unansweredFields(step, values).length > 0)
  return index === -1 ? null : index
}

/** Builds the request body. Call only when every question is answered (see firstIncompleteStep). */
export function toPayload(values: QuestionnaireValues): AnswersPayload {
  const answered = <T>(value: T | null, field: string): T => {
    if (value === null) throw new Error(`Unanswered question: ${field}`)
    return value
  }
  const payload: AnswersPayload = {
    activity_level: answered(values.activity_level, 'activity_level'),
    shedding_tolerance: answered(values.shedding_tolerance, 'shedding_tolerance'),
    grooming_capacity: answered(values.grooming_capacity, 'grooming_capacity'),
    mental_stimulation_capacity: answered(values.mental_stimulation_capacity, 'mental_stimulation_capacity'),
    barking_tolerance: answered(values.barking_tolerance, 'barking_tolerance'),
    preferred_size: answered(values.preferred_size, 'preferred_size'),
    has_young_children: answered(values.has_young_children, 'has_young_children'),
    has_other_dogs: answered(values.has_other_dogs, 'has_other_dogs'),
    has_cats: answered(values.has_cats, 'has_cats'),
    housing_type: answered(values.housing_type, 'housing_type'),
    walk_minutes_daily: values.walk_minutes_daily,
    alone_hours_daily: values.alone_hours_daily,
    owner_experience: answered(values.owner_experience, 'owner_experience'),
    training_minutes_daily: values.training_minutes_daily,
  }
  if (values.strict.allowed_sizes && payload.preferred_size !== 'any') {
    payload.allowed_sizes = [payload.preferred_size]
  }
  const hardLimits: Record<string, number> = {}
  if (values.strict.shedding_level) hardLimits.shedding_level = payload.shedding_tolerance
  if (values.strict.coat_grooming_frequency) hardLimits.coat_grooming_frequency = payload.grooming_capacity
  if (values.strict.barking_level) hardLimits.barking_level = payload.barking_tolerance
  if (Object.keys(hardLimits).length > 0) {
    payload.hard_limits = hardLimits
  }
  return payload
}

/** Maps a backend field error to the question it belongs to (constraints map to their question). */
export function questionForError(error: FieldError): AnswerKey | null {
  const strictToField: Record<string, AnswerKey> = {
    allowed_sizes: 'preferred_size',
    'hard_limits.shedding_level': 'shedding_tolerance',
    'hard_limits.coat_grooming_frequency': 'grooming_capacity',
    'hard_limits.barking_level': 'barking_tolerance',
  }
  const all = steps.flatMap((step) => step.questions.map((question) => question.field))
  if (all.includes(error.field as AnswerKey)) return error.field as AnswerKey
  return strictToField[error.field] ?? null
}

export function stepIndexOf(field: AnswerKey): number {
  return steps.findIndex((step) => step.questions.some((question) => question.field === field))
}
