import { createContext, useContext } from 'react'

/** True while the answers of an earlier guest session are being loaded into the form (after a page reload). */
export const RestoringAnswersContext = createContext(false)

export function useRestoringAnswers(): boolean {
  return useContext(RestoringAnswersContext)
}
