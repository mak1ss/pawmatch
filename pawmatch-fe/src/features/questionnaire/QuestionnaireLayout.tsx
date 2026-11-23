import { FormProvider, useForm } from 'react-hook-form'
import { Outlet } from 'react-router'

import { useQuestionnaireContract } from '@/api/questionnaire'
import { Container } from '@/components/layout/Container'
import { Button } from '@/components/ui/button'
import { copy } from '@/copy/uk'
import { ContractContext } from '@/features/questionnaire/contractContext'
import type { QuestionnaireValues } from '@/features/questionnaire/model'
import { initialValues } from '@/features/questionnaire/model'

const t = copy.questionnaire

/**
 * Parent route of all questionnaire screens: loads the contract once and keeps the answers in one form, so they
 * survive moving between steps. Answers live in memory only and are never logged.
 */
export function QuestionnaireLayout() {
  const form = useForm<QuestionnaireValues>({ defaultValues: initialValues })
  const contract = useQuestionnaireContract()

  if (contract.isPending) {
    return (
      <Container className="py-16">
        <p role="status" className="text-lg text-muted-foreground">
          {t.loading}
        </p>
      </Container>
    )
  }

  if (contract.isError) {
    return (
      <Container className="flex flex-col items-start gap-6 py-16">
        <p role="alert" className="text-lg font-bold">
          {t.loadError}
        </p>
        <Button variant="outline" size="form" onClick={() => void contract.refetch()}>
          {t.retry}
        </Button>
      </Container>
    )
  }

  return (
    <FormProvider {...form}>
      <ContractContext value={contract.data}>
        <Outlet />
      </ContractContext>
    </FormProvider>
  )
}
