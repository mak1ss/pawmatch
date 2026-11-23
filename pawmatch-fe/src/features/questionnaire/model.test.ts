import type { QuestionnaireContract } from '@/api/questionnaire'
import type { QuestionnaireValues } from '@/features/questionnaire/model'
import {
  firstIncompleteStep,
  initialValues,
  optionCodes,
  questionForError,
  questionNumber,
  sliderRange,
  steps,
  toPayload,
} from '@/features/questionnaire/model'
import { questionnaireContract } from '@/mocks/fixtures/questionnaire'

const contract = questionnaireContract as QuestionnaireContract

/** Every question answered, mirroring the backend's example profile. */
const answered: QuestionnaireValues = {
  ...initialValues,
  housing_type: 'apartment',
  alone_hours_daily: 4,
  activity_level: 3,
  walk_minutes_daily: 60,
  has_young_children: true,
  has_other_dogs: false,
  has_cats: false,
  owner_experience: 'none',
  training_minutes_daily: 20,
  mental_stimulation_capacity: 3,
  preferred_size: 'Small',
  shedding_tolerance: 2,
  grooming_capacity: 2,
  barking_tolerance: 2,
}

describe('questionnaire model', () => {
  it('covers exactly the 14 required contract fields, numbered 1–14 in step order', () => {
    const fields = steps.flatMap((step) => step.questions.map((question) => question.field))
    const required = contract.fields.filter((field) => field.required).map((field) => field.key)

    expect([...fields].sort()).toEqual([...required].sort())
    expect(questionNumber('housing_type')).toBe(1)
    expect(questionNumber('barking_tolerance')).toBe(14)
  })

  it('takes option codes from the contract: listed options for choices, min..max for scales', () => {
    expect(optionCodes(contract, 'housing_type')).toEqual(['apartment', 'house', 'other'])
    expect(optionCodes(contract, 'preferred_size')).toEqual(['XSmall', 'Small', 'Medium', 'Large', 'XLarge', 'any'])
    expect(optionCodes(contract, 'activity_level')).toEqual(['1', '2', '3', '4', '5'])
  })

  it('caps slider ranges at the UI maximum but never exceeds the contract', () => {
    const walk = steps[1]!.questions[1]!
    const alone = steps[0]!.questions[1]!

    expect(sliderRange(contract, walk)).toEqual({ min: 0, max: 180 })
    expect(sliderRange(contract, alone)).toEqual({ min: 0, max: 24 })
  })

  it('finds the first step with an unanswered question', () => {
    expect(firstIncompleteStep(initialValues)).toBe(0)
    expect(firstIncompleteStep({ ...answered, has_cats: null })).toBe(2)
    expect(firstIncompleteStep(answered)).toBeNull()
  })

  it('builds the exact backend payload without optional constraints by default', () => {
    expect(toPayload(answered)).toEqual({
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
      walk_minutes_daily: 60,
      alone_hours_daily: 4,
      owner_experience: 'none',
      training_minutes_daily: 20,
    })
  })

  it('turns the checkboxes into allowed_sizes and hard_limits', () => {
    const payload = toPayload({
      ...answered,
      strict: { allowed_sizes: true, shedding_level: true, coat_grooming_frequency: false, barking_level: true },
    })

    expect(payload.allowed_sizes).toEqual(['Small'])
    expect(payload.hard_limits).toEqual({ shedding_level: 2, barking_level: 2 })
  })

  it('never sends allowed_sizes for "any" size', () => {
    const payload = toPayload({ ...answered, preferred_size: 'any', strict: { ...answered.strict, allowed_sizes: true } })

    expect(payload.allowed_sizes).toBeUndefined()
  })

  it('maps backend field errors to their question, including constraint fields', () => {
    const error = (field: string) => ({ field, code: 'invalid_integer', message: '' })

    expect(questionForError(error('activity_level'))).toBe('activity_level')
    expect(questionForError(error('allowed_sizes'))).toBe('preferred_size')
    expect(questionForError(error('hard_limits.barking_level'))).toBe('barking_tolerance')
    expect(questionForError(error('dog_name'))).toBeNull()
  })
})
