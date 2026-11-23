/**
 * Ukrainian questionnaire copy (Figma screens 02–09), keyed by contract field key, option code and error code.
 * The backend owns keys, ranges and codes; all wording here is the frontend's.
 */
export const questionnaireCopy = {
  start: {
    backHome: '← На головну',
    title: 'Анкета підбору породи',
    text: ['Відповідайте чесно: що точніше відповіді, то точніший підбір.', 'Усе можна змінити перед відправленням.'],
    details: ['14 запитань', 'Близько 3 хвилин', 'Без реєстрації'],
    start: 'Почати анкету',
    photoAlt: 'Чоловік лежить на підлозі й дивиться на кучерявого пса',
  },
  steps: {
    housing: { title: 'Умови проживання', subtitle: 'Розкажіть, де житиме собака' },
    lifestyle: { title: 'Спосіб життя', subtitle: 'Наскільки активною має бути собака і скільки часу на прогулянки' },
    family: { title: 'Сімейні умови', subtitle: 'Хто ще живе з вами' },
    experience: { title: 'Досвід', subtitle: 'Ваш досвід і час на заняття з собакою' },
    preferences: { title: 'Побажання', subtitle: 'Які риси собаки для вас важливі' },
  },
  stepOf: (step: number, total: number) => `Крок ${step} з ${total}`,
  questionCount: (count: number) => `${count} ${pluralQuestions(count)}`,
  progressLabel: 'Прогрес анкети',
  back: 'Назад',
  next: 'Далі',
  toReview: 'Перевірити відповіді',
  questions: {
    housing_type: 'Де ви живете?',
    alone_hours_daily: 'Скільки годин на день собака буде сама?',
    activity_level: 'Наскільки активною має бути собака?',
    walk_minutes_daily: 'Скільки хвилин на день готові гуляти з собакою?',
    has_young_children: 'Чи є в сім’ї маленькі діти?',
    has_other_dogs: 'Чи є вдома інші собаки?',
    has_cats: 'Чи є вдома коти?',
    owner_experience: 'Який у вас досвід із собаками?',
    training_minutes_daily: 'Скільки хвилин на день готові займатися навчанням?',
    mental_stimulation_capacity: 'Наскільки готові до розумових ігор і занять?',
    preferred_size: 'Якого розміру собаку хочете?',
    shedding_tolerance: 'Яке линяння для вас прийнятне?',
    grooming_capacity: 'Скільки часу готові приділяти догляду за шерстю?',
    barking_tolerance: 'Який рівень гавкоту для вас прийнятний?',
  },
  /** Option labels by field key and option code (integer scales use "1".."5"). */
  options: {
    housing_type: { apartment: 'Квартира', house: 'Будинок', other: 'Інше' },
    activity_level: { 1: 'Дуже спокійна', 2: 'Спокійна', 3: 'Помірна', 4: 'Активна', 5: 'Дуже активна' },
    owner_experience: { none: 'Немає', some: 'Трохи є', experienced: 'Досвідчений власник' },
    mental_stimulation_capacity: { 1: 'Мінімально', 2: 'Трохи', 3: 'Помірно', 4: 'Багато', 5: 'Дуже багато' },
    preferred_size: {
      XSmall: 'Дуже малий',
      Small: 'Малий',
      Medium: 'Середній',
      Large: 'Великий',
      XLarge: 'Дуже великий',
      any: 'Будь-який',
    },
    shedding_tolerance: { 1: 'Майже немає', 2: 'Невелике', 3: 'Помірне', 4: 'Помітне', 5: 'Сильне' },
    grooming_capacity: { 1: 'Мінімально', 2: 'Трохи', 3: 'Помірно', 4: 'Багато', 5: 'Дуже багато' },
    barking_tolerance: { 1: 'Мінімальний', 2: 'Невеликий', 3: 'Помірний', 4: 'Частий', 5: 'Будь-який' },
  } as Record<string, Record<string, string>>,
  yes: 'Так',
  no: 'Ні',
  units: { hours: 'год', minutes: 'хв' },
  perDay: (value: string) => `${value} на день`,
  constraints: {
    allowed_sizes: 'Розглядати лише цей розмір',
    hardLimit: 'Обов’язкова умова',
  },
  review: {
    title: 'Перевірте відповіді',
    subtitle: 'Якщо щось не так, натисніть Змінити біля потрібного блоку',
    change: 'Змінити',
    changeLabel: (section: string) => `Змінити: ${section}`,
    submit: 'Отримати рекомендації',
    submitting: 'Надсилаємо…',
    labels: {
      housing_type: 'Тип житла',
      alone_hours_daily: 'Собака сама вдома',
      activity_level: 'Активність',
      walk_minutes_daily: 'Прогулянки',
      has_young_children: 'Маленькі діти',
      has_other_dogs: 'Інші собаки',
      has_cats: 'Коти',
      owner_experience: 'Досвід із собаками',
      training_minutes_daily: 'Навчання',
      mental_stimulation_capacity: 'Розумові заняття',
      preferred_size: 'Розмір',
      shedding_tolerance: 'Линяння',
      grooming_capacity: 'Догляд за шерстю',
      barking_tolerance: 'Гавкіт',
    },
    serverErrorsTitle: 'Не вдалося надіслати анкету',
    serverErrorsText: 'Перевірте відповіді нижче:',
    genericError: 'Не вдалося надіслати відповіді. Спробуйте ще раз.',
  },
  loading: 'Завантажуємо анкету…',
  loadError: 'Не вдалося завантажити анкету.',
  retry: 'Спробувати ще раз',
  errors: {
    /** Client-side: a choice question has no answer. */
    choose: 'Оберіть варіант відповіді',
    /** Server-side, by FieldError code; `{min}`, `{max}` come from params. */
    required: 'Оберіть варіант відповіді',
    invalid_integer: 'Оберіть значення від {min} до {max}',
    invalid_number: 'Вкажіть значення від {min} до {max}',
    invalid_boolean: 'Оберіть «Так» або «Ні»',
    invalid_choice: 'Оберіть один із запропонованих варіантів',
    invalid_list: 'Оберіть один із запропонованих розмірів',
    duplicate_value: 'Значення не повинні повторюватися',
    invalid_object: 'Некоректна відповідь',
    unsupported_key: 'Непідтримувана умова',
    unknown_field: 'Невідоме поле',
  },
}

function pluralQuestions(count: number): string {
  const lastTwo = count % 100
  const last = count % 10
  if (lastTwo >= 11 && lastTwo <= 14) return 'запитань'
  if (last === 1) return 'запитання'
  if (last >= 2 && last <= 4) return 'запитання'
  return 'запитань'
}
