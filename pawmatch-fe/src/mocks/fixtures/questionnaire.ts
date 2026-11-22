/**
 * Mirrors GET /api/v1/questionnaire of the backend (contract version 1.0.0). Keep in sync with the backend's
 * QuestionnaireContract; once API types are generated, this fixture should satisfy them.
 */
const SIZES = ['XSmall', 'Small', 'Medium', 'Large', 'XLarge']
const scale = (key: string) => ({ key, type: 'integer', required: true, min: 1, max: 5 })
const number = (key: string, max: number) => ({ key, type: 'number', required: true, min: 0, max })
const bool = (key: string) => ({ key, type: 'boolean', required: true })
const choice = (key: string, options: string[]) => ({ key, type: 'choice', required: true, options })

export const questionnaireContract = {
  version: '1.0.0',
  fields: [
    scale('activity_level'),
    scale('shedding_tolerance'),
    scale('grooming_capacity'),
    scale('mental_stimulation_capacity'),
    scale('barking_tolerance'),
    choice('preferred_size', [...SIZES, 'any']),
    bool('has_young_children'),
    bool('has_other_dogs'),
    bool('has_cats'),
    choice('housing_type', ['apartment', 'house', 'other']),
    number('walk_minutes_daily', 1440),
    number('alone_hours_daily', 24),
    choice('owner_experience', ['none', 'some', 'experienced']),
    number('training_minutes_daily', 1440),
    { key: 'allowed_sizes', type: 'multi_choice', required: false, options: SIZES },
    {
      key: 'hard_limits',
      type: 'limits',
      required: false,
      min: 1,
      max: 5,
      options: [
        'energy_level',
        'shedding_level',
        'coat_grooming_frequency',
        'mental_stimulation_needs',
        'barking_level',
      ],
    },
  ],
}
