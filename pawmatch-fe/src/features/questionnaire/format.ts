import type { FieldError } from '@/api/types'
import { copy } from '@/copy/uk'
import type { AnswerKey, Question, QuestionnaireValues } from '@/features/questionnaire/model'

const t = copy.questionnaire

/** Label of a choice/scale option, by field key and option code. */
export function optionLabel(field: AnswerKey, code: string): string {
  return t.options[field]?.[code] ?? code
}

/** "6 год", "45 хв". */
export function formatAmount(value: number, unit: Question['unit']): string {
  return `${value} ${unit === 'hours' ? t.units.hours : t.units.minutes}`
}

/** Human-readable answer for the review screen. */
export function formatAnswer(question: Question, values: QuestionnaireValues): string {
  const value = values[question.field]
  if (value === null) return '—'
  if (typeof value === 'boolean') return value ? t.yes : t.no
  if (question.kind === 'slider') return t.perDay(formatAmount(value as number, question.unit))
  return optionLabel(question.field, String(value))
}

/** Our wording for a backend field error; falls back to the backend's Ukrainian message. */
export function errorMessage(error: FieldError): string {
  const templates: Record<string, string | undefined> = t.errors
  const template = templates[error.code]
  if (!template) return error.message
  return template
    .replace('{min}', String(error.params?.min ?? ''))
    .replace('{max}', String(error.params?.max ?? ''))
}
