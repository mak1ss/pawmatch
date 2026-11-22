import type { RouteObject } from 'react-router'

import { Layout } from '@/app/Layout'
import { NotFoundPage } from '@/app/NotFoundPage'
import { LandingPage } from '@/features/home/LandingPage'

/** Route tree, shared by the browser router and by tests (memory router). */
export const routes: RouteObject[] = [
  {
    element: <Layout />,
    children: [
      { index: true, element: <LandingPage /> },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
]
