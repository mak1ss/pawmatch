import { useQueryClient } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { useFormContext } from 'react-hook-form'
import { Link, Navigate, useNavigate } from 'react-router'

import { ApiError } from '@/api/client'
import { sessionAnswersQuery, useSubmitAnswers } from '@/api/questionnaire'
import { recommendationKeys } from '@/api/recommendations'
import type { FieldError } from '@/api/types'
import { paths } from '@/app/routePaths'
import { Container } from '@/components/layout/Container'
import { Button } from '@/components/ui/button'
import { copy } from '@/copy/uk'
import { FieldErrorText } from '@/features/questionnaire/components/QuestionField'
import { Stepper } from '@/features/questionnaire/components/Stepper'
import { errorMessage, formatAnswer } from '@/features/questionnaire/format'
import { readGuestSessionId, saveGuestSessionId } from '@/features/questionnaire/guestSession'
import type { QuestionnaireValues, Step } from '@/features/questionnaire/model'
import {
  firstIncompleteStep,
  questionForError,
  stepIndexOf,
  steps,
  TOTAL_STEPS,
  toPayload,
} from '@/features/questionnaire/model'

const t = copy.questionnaire

/**
 * Review before submitting (Figma "09 Перевірка відповідей"). Saves the answers (new guest session, or the existing one's
 * answers replaced) and opens the results, which compute recommendations for them.
 */
export function QuestionnaireReviewPage() {
  const navigate = useNavigate()
  const { getValues, setError } = useFormContext<QuestionnaireValues>()
  const submit = useSubmitAnswers()
  const queryClient = useQueryClient()
  const [serverErrors, setServerErrors] = useState<FieldError[]>([])

  useEffect(() => {
    window.scrollTo?.(0, 0)
  }, [])

  const values = getValues()
  const incomplete = firstIncompleteStep(values)
  if (incomplete !== null) {
    return <Navigate to={paths.questionnaireStep(incomplete + 1)} replace />
  }

  const onSubmit = () => {
    setServerErrors([])
    const answers = toPayload(values)
    submit.mutate({ answers, sessionId: readGuestSessionId() }, {
      onSuccess: (session) => {
        saveGuestSessionId(session.session_id)
        // A later restore (after a reload) must see these answers, not older cached ones.
        queryClient.setQueryData(sessionAnswersQuery(session.session_id).queryKey, { ...session, answers })
        // Results computed from older answers must not be shown from the cache.
        queryClient.removeQueries({ queryKey: recommendationKeys.session(session.session_id) })
        navigate(paths.results, { state: { answersVersion: session.answers_version } })
      },
      onError: (error) => {
        if (error instanceof ApiError && error.status === 422) {
          setServerErrors(error.fieldErrors)
          error.fieldErrors.forEach((fieldError) => {
            const question = questionForError(fieldError)
            if (question) setError(question, { type: 'server', message: errorMessage(fieldError) })
          })
        }
      },
    })
  }

  const genericError = submit.isError && serverErrors.length === 0
  const [left, right] = [steps.slice(0, 3), steps.slice(3)]

  return (
    <Container className="pt-10 pb-20">
      <Stepper current="review" />
      <h1 className="mt-8 text-[36px] leading-tight font-bold sm:text-[46px]">{t.review.title}</h1>
      <p className="mt-1 text-lg text-muted-foreground">{t.review.subtitle}</p>

      {serverErrors.length > 0 && <ServerErrors errors={serverErrors} />}
      {genericError && (
        <div className="mt-6">
          <FieldErrorText>{t.review.genericError}</FieldErrorText>
        </div>
      )}

      <div className="mt-8 grid gap-6 lg:grid-cols-2">
        {[left, right].map((column, columnIndex) => (
          <div key={columnIndex} className="flex flex-col gap-5">
            {column.map((step) => (
              <ReviewBlock key={step.key} step={step} values={values} />
            ))}
          </div>
        ))}
      </div>

      <div className="mt-10 flex justify-between gap-4">
        <Button asChild variant="outline" size="xl" className="min-w-[150px]">
          <Link to={paths.questionnaireStep(TOTAL_STEPS)}>{t.back}</Link>
        </Button>
        <Button size="xl" className="min-w-[290px]" onClick={onSubmit} disabled={submit.isPending}>
          {submit.isPending ? t.review.submitting : t.review.submit}
        </Button>
      </div>
    </Container>
  )
}

function ReviewBlock({ step, values }: { step: Step; values: QuestionnaireValues }) {
  const index = steps.indexOf(step)
  const title = t.steps[step.key].title
  return (
    <section aria-labelledby={`review-${step.key}`} className="rounded-[20px] border border-border bg-card px-7 pt-[22px] pb-1.5">
      <div className="-ml-1 flex items-center gap-3">
        <span className="flex size-8 shrink-0 items-center justify-center rounded-full bg-accent text-[15px] font-bold text-primary">
          {index + 1}
        </span>
        <h2 id={`review-${step.key}`} className="flex-1 text-[19px] font-bold">
          {title}
        </h2>
        <Link
          to={paths.questionnaireStep(index + 1)}
          aria-label={t.review.changeLabel(title)}
          className="rounded-md text-[15px] font-bold text-primary hover:underline"
        >
          {t.review.change}
        </Link>
      </div>
      <dl className="mt-[18px]">
        {step.questions.map((question) => (
          <div key={question.field} className="flex h-11 items-center justify-between gap-4 border-t border-border text-base font-bold">
            <dt className="text-muted-foreground">{t.review.labels[question.field]}</dt>
            <dd className="text-right">{formatAnswer(question, values)}</dd>
          </div>
        ))}
      </dl>
    </section>
  )
}

/** 422 from the backend: every field error with a link to the step that holds the question. */
function ServerErrors({ errors }: { errors: FieldError[] }) {
  return (
    <div role="alert" className="mt-6 rounded-2xl border-[1.5px] border-destructive bg-card p-5">
      <p className="font-bold text-destructive">{t.review.serverErrorsTitle}</p>
      <p className="mt-1 text-muted-foreground">{t.review.serverErrorsText}</p>
      <ul className="mt-3 flex flex-col gap-1.5">
        {errors.map((error) => {
          const question = questionForError(error)
          return (
            <li key={`${error.field}-${error.code}`} className="font-bold">
              {question ? (
                <Link to={paths.questionnaireStep(stepIndexOf(question) + 1)} className="text-primary hover:underline">
                  {t.review.labels[question]}
                </Link>
              ) : (
                <span>{error.field}</span>
              )}
              : {errorMessage(error)}
            </li>
          )
        })}
      </ul>
    </div>
  )
}
