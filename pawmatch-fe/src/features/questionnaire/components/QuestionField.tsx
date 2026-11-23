import type { ReactNode } from 'react'

import { copy } from '@/copy/uk'
import type { AnswerKey } from '@/features/questionnaire/model'
import { questionNumber } from '@/features/questionnaire/model'
import { errorId, questionId, questionLabelId } from '@/features/questionnaire/questionIds'

/**
 * A question with its number and label (e.g. "1. Де ви живете?"), the control, and an error line (Figma screen 08).
 * `aside` sits at the right end of the label row (slider value badge).
 */
export function QuestionField({
  field,
  error,
  aside,
  children,
}: {
  field: AnswerKey
  error?: string
  aside?: ReactNode
  children: ReactNode
}) {
  return (
    <section id={questionId(field)} aria-labelledby={questionLabelId(field)} className="scroll-mt-6">
      <div className="flex items-center justify-between gap-4">
        <h3 id={questionLabelId(field)} className="text-lg font-bold">
          {questionNumber(field)}. {copy.questionnaire.questions[field]}
        </h3>
        {aside}
      </div>
      <div className="mt-3.5">{children}</div>
      {error && <FieldErrorText id={errorId(field)}>{error}</FieldErrorText>}
    </section>
  )
}

export function FieldErrorText({ id, children }: { id?: string; children: ReactNode }) {
  return (
    <p id={id} role="alert" className="mt-2.5 flex items-center gap-2 text-[15px] font-bold text-destructive">
      <span
        aria-hidden
        className="flex size-4 shrink-0 items-center justify-center rounded-full bg-destructive text-[11px] text-white"
      >
        !
      </span>
      {children}
    </p>
  )
}
