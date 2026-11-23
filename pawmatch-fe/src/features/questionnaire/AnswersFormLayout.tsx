import { useQueryClient } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { FormProvider, useForm } from 'react-hook-form'
import { Outlet } from 'react-router'

import { ApiError } from '@/api/client'
import { sessionAnswersQuery } from '@/api/questionnaire'
import { clearGuestSessionId, readGuestSessionId } from '@/features/questionnaire/guestSession'
import type { QuestionnaireValues } from '@/features/questionnaire/model'
import { fromPayload, initialValues } from '@/features/questionnaire/model'
import { RestoringAnswersContext } from '@/features/questionnaire/restoringAnswers'

/**
 * Keeps the questionnaire answers in one form for the questionnaire and the results screens, so "change answers" on
 * the results reopens the review with the answers filled in. After a page reload, the form is restored from the guest
 * session's stored answers. Answers live in memory only and are never logged.
 */
export function AnswersFormLayout() {
  const form = useForm<QuestionnaireValues>({ defaultValues: initialValues })
  const queryClient = useQueryClient()
  const [sessionId] = useState(readGuestSessionId)
  const [restoring, setRestoring] = useState(sessionId !== null)

  useEffect(() => {
    if (sessionId === null) return
    let active = true
    queryClient
      .fetchQuery(sessionAnswersQuery(sessionId))
      .then(
        (stored) => {
          // Never overwrite answers the visitor has already started changing.
          if (active && !form.formState.isDirty) form.reset(fromPayload(stored.answers))
        },
        (error: unknown) => {
          // Without stored answers the questionnaire simply starts empty; a 404 means the session is gone.
          if (error instanceof ApiError && error.status === 404) clearGuestSessionId()
        },
      )
      .finally(() => {
        if (active) setRestoring(false)
      })
    return () => {
      active = false
    }
  }, [sessionId, queryClient, form])

  return (
    <FormProvider {...form}>
      <RestoringAnswersContext value={restoring}>
        <Outlet />
      </RestoringAnswersContext>
    </FormProvider>
  )
}
