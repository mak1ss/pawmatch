import { createContext, use } from 'react'

import type { QuestionnaireContract } from '@/api/questionnaire'

/** Provided by QuestionnaireLayout once the contract has loaded. */
export const ContractContext = createContext<QuestionnaireContract | null>(null)

/** The questionnaire contract, available to every screen under the questionnaire route. */
export function useContract(): QuestionnaireContract {
  const contract = use(ContractContext)
  if (!contract) throw new Error('useContract must be used inside QuestionnaireLayout')
  return contract
}
