/** App paths in one place, so links and routes can't drift. Screens not built yet still get their final path. */
export const paths = {
  home: '/',
  howItWorks: '/#how-it-works',
  questionnaire: '/questionnaire',
  /** Step numbers are 1-based, as shown to the user. */
  questionnaireStep: (step: number) => `/questionnaire/step/${step}`,
  questionnaireReview: '/questionnaire/review',
  results: '/results',
  compare: '/compare',
  login: '/login',
} as const
