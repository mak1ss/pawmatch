import { useEffect } from 'react'
import { useFormContext } from 'react-hook-form'
import { Navigate, useNavigate, useParams } from 'react-router'

import { paths } from '@/app/routePaths'
import { Container } from '@/components/layout/Container'
import { Button } from '@/components/ui/button'
import { copy } from '@/copy/uk'
import { QuestionControl } from '@/features/questionnaire/components/QuestionControl'
import { questionId } from '@/features/questionnaire/questionIds'
import { Stepper } from '@/features/questionnaire/components/Stepper'
import type { QuestionnaireValues } from '@/features/questionnaire/model'
import { firstIncompleteStep, steps, TOTAL_STEPS, unansweredFields } from '@/features/questionnaire/model'

const t = copy.questionnaire

/** One questionnaire step (Figma 03–07), including the "no answer chosen" state (Figma 08). */
export function QuestionnaireStepPage() {
  const { step: stepParam } = useParams()
  const navigate = useNavigate()
  const { getValues, setError } = useFormContext<QuestionnaireValues>()

  const stepNumber = Number(stepParam)
  const index = stepNumber - 1
  const step = steps[index]

  useEffect(() => {
    window.scrollTo?.(0, 0)
  }, [index])

  if (!step) {
    return <Navigate to={paths.questionnaireStep(1)} replace />
  }
  // Steps are answered in order: jumping ahead lands on the first unanswered step.
  const incomplete = firstIncompleteStep(getValues())
  if (incomplete !== null && incomplete < index) {
    return <Navigate to={paths.questionnaireStep(incomplete + 1)} replace />
  }

  const isLast = stepNumber === TOTAL_STEPS

  const goNext = () => {
    const missing = unansweredFields(step, getValues())
    if (missing.length > 0) {
      missing.forEach((field) => setError(field, { type: 'required', message: t.errors.choose }))
      document.getElementById(questionId(missing[0]!))?.scrollIntoView?.({ behavior: 'smooth' })
      return
    }
    navigate(isLast ? paths.questionnaireReview : paths.questionnaireStep(stepNumber + 1))
  }

  const goBack = () => navigate(index === 0 ? paths.questionnaire : paths.questionnaireStep(stepNumber - 1))

  const stepCopy = t.steps[step.key]

  return (
    <Container className="pt-10 pb-20">
      <Stepper current={index} />
      <form
        noValidate
        aria-labelledby="step-title"
        onSubmit={(event) => {
          event.preventDefault()
          goNext()
        }}
        className="mx-auto mt-[30px] max-w-[960px] rounded-[28px] border border-border bg-card p-6 sm:p-12"
      >
        <p className="text-[15px] font-bold text-primary">{t.stepOf(stepNumber, TOTAL_STEPS)}</p>
        <h1 id="step-title" className="mt-2 text-[28px] leading-tight font-bold sm:text-[34px]">
          {stepCopy.title}
        </h1>
        <p className="mt-1.5 text-[17px] text-muted-foreground">{stepCopy.subtitle}</p>

        <div className="mt-10 flex flex-col gap-10">
          {step.questions.map((question) => (
            <QuestionControl key={question.field} question={question} />
          ))}
        </div>

        <div className="mt-12 flex justify-between gap-4 border-t border-border pt-7">
          <Button type="button" variant="outline" size="form" onClick={goBack}>
            {t.back}
          </Button>
          <Button type="submit" size="form">
            {isLast ? t.toReview : t.next}
          </Button>
        </div>
      </form>
    </Container>
  )
}
