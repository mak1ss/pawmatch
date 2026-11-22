/**
 * Ukrainian UX copy. The frontend owns all wording; the backend only sends keys and codes.
 * Landing, header and footer copy follows the Figma guest mockups. Questionnaire copy (by field key, option code and
 * error code) will be added per feature.
 */
export const copy = {
  app: {
    name: 'PawMatch',
    tagline: 'Підбір породи собаки під ваш спосіб життя',
  },
  nav: {
    label: 'Головне меню',
    howItWorks: 'Як це працює',
    findBreed: 'Підібрати породу',
    compare: 'Порівняння',
    login: 'Увійти',
  },
  landing: {
    tag: 'Підбір породи за 3 хвилини',
    title: 'Знайдіть собаку, яка підходить саме вам',
    description:
      'Пройдіть коротку анкету про ваш спосіб життя, і PawMatch підбере породи з індексом сумісності та поясненням, чому вони вам підходять.',
    start: 'Підібрати породу',
    howItWorks: 'Як це працює',
    stats: [
      { value: '14', label: 'запитань' },
      { value: '30', label: 'порід у каталозі' },
      { value: 'до 5', label: 'рекомендацій' },
    ],
    heroPhotoAlt: 'Усміхнена жінка обіймає собаку',
    example: {
      breedName: 'Кавалер-кінг-чарльз-спанієль',
      indexLabel: 'Індекс сумісності',
      index: 98,
    },
    steps: {
      title: 'Як це працює',
      subtitle: 'Три кроки від анкети до вашої породи',
      items: [
        {
          label: 'Крок 1',
          title: 'Пройдіть анкету',
          text: '14 простих запитань про житло, ритм життя, сім’ю та побажання до собаки.',
        },
        {
          label: 'Крок 2',
          title: 'Отримайте рекомендації',
          text: 'Модель порівняє ваші відповіді з 30 породами і покаже найкращі збіги.',
        },
        {
          label: 'Крок 3',
          title: 'Порівняйте породи',
          text: 'Подивіться, чому порода підходить, на що зважити, і порівняйте кілька порід поруч.',
        },
      ],
    },
    factors: {
      title: 'Що ми враховуємо',
      subtitle: 'Підбір спирається на 14 параметрів вашого життя',
      items: [
        'Тип житла',
        'Час наодинці',
        'Активність',
        'Прогулянки',
        'Маленькі діти',
        'Інші собаки',
        'Коти',
        'Досвід',
        'Навчання',
        'Розумові заняття',
        'Розмір',
        'Линяння',
        'Догляд за шерстю',
        'Гавкіт',
      ],
    },
    cta: {
      title: 'Готові знайти свою породу?',
      text: 'Анкета займає близько трьох хвилин, реєстрація не потрібна.',
      button: 'Почати підбір',
    },
  },
  footer: {
    copyright: '© 2026 PawMatch',
    about: 'Про проєкт',
    howItWorks: 'Як це працює',
    contacts: 'Контакти',
  },
  notFound: {
    title: 'Сторінку не знайдено',
    backHome: 'На головну',
  },
} as const
