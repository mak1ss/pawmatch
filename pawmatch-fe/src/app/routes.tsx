import type { RouteObject } from 'react-router'

import { Layout } from '@/app/Layout'
import { NotFoundPage } from '@/app/NotFoundPage'
import { LandingPage } from '@/features/home/LandingPage'
import { AnswersFormLayout } from '@/features/questionnaire/AnswersFormLayout'
import { QuestionnaireLayout } from '@/features/questionnaire/QuestionnaireLayout'
import { QuestionnaireReviewPage } from '@/features/questionnaire/QuestionnaireReviewPage'
import { QuestionnaireStartPage } from '@/features/questionnaire/QuestionnaireStartPage'
import { QuestionnaireStepPage } from '@/features/questionnaire/QuestionnaireStepPage'
import { ResultsPage } from '@/features/recommendations/ResultsPage'

/** Route tree, shared by the browser router and by tests (memory router). */
export const routes: RouteObject[] = [
  {
    element: <Layout />,
    children: [
      { index: true, element: <LandingPage /> },
      {
        // The answers form spans the questionnaire and the results ("change answers" goes back with them filled in).
        element: <AnswersFormLayout />,
        children: [
          {
            path: 'questionnaire',
            element: <QuestionnaireLayout />,
            children: [
              { index: true, element: <QuestionnaireStartPage /> },
              { path: 'step/:step', element: <QuestionnaireStepPage /> },
              { path: 'review', element: <QuestionnaireReviewPage /> },
            ],
          },
          { path: 'results', element: <ResultsPage /> },
        ],
      },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
]
