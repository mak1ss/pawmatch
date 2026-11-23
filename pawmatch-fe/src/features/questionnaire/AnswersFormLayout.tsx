import { FormProvider, useForm } from 'react-hook-form'
import { Outlet } from 'react-router'

import type { QuestionnaireValues } from '@/features/questionnaire/model'
import { initialValues } from '@/features/questionnaire/model'

/**
 * Keeps the questionnaire answers in one form for the questionnaire and the results screens, so "change answers" on
 * the results reopens the review with the answers filled in. Answers live in memory only and are never logged.
 */
export function AnswersFormLayout() {
  const form = useForm<QuestionnaireValues>({ defaultValues: initialValues })
  return (
    <FormProvider {...form}>
      <Outlet />
    </FormProvider>
  )
}
