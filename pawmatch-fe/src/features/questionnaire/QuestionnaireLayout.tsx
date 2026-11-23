import { Outlet } from 'react-router'

import { useQuestionnaireContract } from '@/api/questionnaire'
import { Container } from '@/components/layout/Container'
import { Button } from '@/components/ui/button'
import { copy } from '@/copy/uk'
import { ContractContext } from '@/features/questionnaire/contractContext'

const t = copy.questionnaire

/** Parent route of all questionnaire screens: loads the contract once. The answers form lives in AnswersFormLayout. */
export function QuestionnaireLayout() {
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
    <ContractContext value={contract.data}>
      <Outlet />
    </ContractContext>
  )
}
