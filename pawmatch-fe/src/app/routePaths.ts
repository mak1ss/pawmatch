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
  /** Compare screen preselected with these breeds (slugs). */
  compareBreeds: (slugs: string[]) => `/compare?breeds=${slugs.map(encodeURIComponent).join(',')}`,
  breed: (slug: string) => `/breeds/${encodeURIComponent(slug)}`,
  login: '/login',
} as const
