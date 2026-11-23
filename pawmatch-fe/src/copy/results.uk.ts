/**
 * Ukrainian UX copy of the results screens (Figma 10–13). Texts the model generates (reasons, warnings, notice,
 * score_meaning, unassessed criteria) are not here: they come from the backend and are shown as-is.
 */

const breedsPlural = new Intl.PluralRules('uk')

/** "1 породу", "3 породи", "5 порід": accusative after "знайшли". */
function breedsAccusative(count: number): string {
  const form = breedsPlural.select(count)
  const word = form === 'one' ? 'породу' : form === 'few' ? 'породи' : 'порід'
  return `${count} ${word}`
}

export const resultsCopy = {
  loading: {
    title: 'Підбираємо породи…',
    subtitle: 'Це займе кілька секунд',
    steps: ['Перевіряємо відповіді', 'Рахуємо сумісність з породами каталогу', 'Готуємо пояснення'],
  },

  title: 'Ваші рекомендації',
  subtitle: (count: number) => `Ми знайшли ${breedsAccusative(count)}, які найкраще підходять до ваших відповідей`,
  saveResults: 'Зберегти результати',
  changeAnswers: 'Змінити відповіді',

  card: {
    photoAlt: (name: string) => `Фото: ${name}`,
    noPhoto: 'Фото незабаром',
    rank: (place: number) => `Місце ${place}`,
    whyFits: 'Чому підходить',
    considerations: 'На що зважити',
    index: 'Індекс сумісності',
    outOf: '/ 100',
    indexLabel: (index: number) => `Індекс сумісності: ${index} зі 100`,
    details: 'Детальніше',
    detailsLabel: (name: string) => `Детальніше про породу ${name}`,
    addToCompare: 'Додати до порівняння',
    source: 'Джерело даних: AKC',
  },

  /** Characteristic tags under the breed name, by breed rating (1–5). Size tags use the questionnaire size labels. */
  tags: {
    activity: {
      1: 'Дуже спокійна',
      2: 'Спокійна',
      3: 'Помірна активність',
      4: 'Висока активність',
      5: 'Дуже висока активність',
    } as Record<number, string>,
    shedding: {
      1: 'Майже не линяє',
      2: 'Невелике линяння',
      3: 'Помірне линяння',
      4: 'Помітне линяння',
      5: 'Сильне линяння',
    } as Record<number, string>,
  },

  fewer: {
    title: 'Знайшли менше порід, ніж зазвичай',
    hint: 'Слабші варіанти ми не додаємо. Спробуйте пом’якшити обов’язкові умови або змінити відповіді.',
  },

  empty: {
    title: 'Не знайшли порід, які вам підходять',
    hint: 'Жодна порода з каталогу не пройшла ваші умови. Спробуйте пом’якшити обов’язкові умови, наприклад розмір чи линяння.',
  },

  unassessedTitle: 'Що ми поки не враховуємо',

  feedback: {
    title: 'Чи корисні ці рекомендації?',
    text: 'Ваш відгук допоможе зробити підбір точнішим',
    useful: 'Так, корисно',
    notUseful: 'Не дуже',
    thanks: 'Дякуємо за відгук!',
  },

  compare: {
    selected: 'Обрано для порівняння',
    remove: (name: string) => `Прибрати ${name} з порівняння`,
    compare: (count: number) => `Порівняти (${count})`,
    hint: 'Оберіть від 2 до 5 порід',
  },

  noSession: {
    title: 'Спершу пройдіть анкету',
    text: 'Рекомендації з’являться, щойно ви відповісте на запитання.',
    start: 'Пройти анкету',
  },
  expired: {
    title: 'Сесію не знайдено',
    text: 'Схоже, сесія завершилася. Пройдіть анкету ще раз, щоб отримати рекомендації.',
  },
  error: 'Не вдалося отримати рекомендації. Перевірте з’єднання і спробуйте ще раз.',
  retry: 'Спробувати ще раз',
}
