/**
 * Ukrainian UX copy. The frontend owns all wording; the backend only sends keys and codes.
 * Questionnaire copy (by field key, option code and error code) will be added per feature.
 */
export const copy = {
  app: {
    name: 'PawMatch',
    tagline: 'Підбір породи собаки під ваш спосіб життя',
  },
  landing: {
    title: 'Знайдіть собаку, яка пасуватиме вашому способу життя',
    description:
      'Дайте відповіді на кілька запитань про житло, активність і досвід — і ми запропонуємо 3–5 порід з поясненням, чому вони вам підходять.',
    start: 'Почати підбір',
  },
  notFound: {
    title: 'Сторінку не знайдено',
    backHome: 'На головну',
  },
} as const
