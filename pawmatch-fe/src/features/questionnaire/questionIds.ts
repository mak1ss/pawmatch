import type { AnswerKey } from '@/features/questionnaire/model'

/** DOM ids for a question, so labels, error messages and scrolling can target it. */
export const questionId = (field: AnswerKey) => `question-${field}`
export const questionLabelId = (field: AnswerKey) => `question-${field}-label`
export const errorId = (field: AnswerKey) => `question-${field}-error`
