import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'

import { API_BASE_URL } from '@/api/client'
import { copy } from '@/copy/uk'
import { readGuestSessionId, saveGuestSessionId } from '@/features/questionnaire/guestSession'
import { recommendationRun } from '@/mocks/fixtures/recommendations'
import { MOCK_SESSION_ID } from '@/mocks/handlers'
import { server } from '@/mocks/server'
import { renderRoute } from '@/test/render'

const t = copy.questionnaire
const user = () => userEvent.setup()

async function choose(u: ReturnType<typeof userEvent.setup>, question: string, option: string) {
  const group = await screen.findByRole('radiogroup', { name: new RegExp(question) })
  await u.click(within(group).getByRole('radio', { name: option }))
}

async function next(u: ReturnType<typeof userEvent.setup>) {
  await u.click(screen.getByRole('button', { name: t.next }))
}

/** Answers every step with the backend's example profile and lands on the review screen. */
async function answerEverything(u: ReturnType<typeof userEvent.setup>) {
  renderRoute('/questionnaire/step/1')
  await choose(u, t.questions.housing_type, 'Квартира')
  await next(u)
  await choose(u, t.questions.activity_level, 'Помірна')
  await next(u)
  await choose(u, t.questions.has_young_children, 'Так')
  await choose(u, t.questions.has_other_dogs, 'Ні')
  await choose(u, t.questions.has_cats, 'Ні')
  await next(u)
  await choose(u, t.questions.owner_experience, 'Немає')
  await choose(u, t.questions.mental_stimulation_capacity, 'Помірно')
  await next(u)
  await choose(u, t.questions.preferred_size, 'Малий')
  await choose(u, t.questions.shedding_tolerance, 'Невелике')
  await choose(u, t.questions.grooming_capacity, 'Трохи')
  await choose(u, t.questions.barking_tolerance, 'Невеликий')
  await u.click(screen.getAllByRole('checkbox', { name: t.constraints.hardLimit })[0]!)
  await u.click(screen.getByRole('button', { name: t.toReview }))
  await screen.findByRole('heading', { level: 1, name: t.review.title })
}

describe('questionnaire', () => {
  afterEach(() => sessionStorage.clear())

  it('shows the five blocks and starts at step 1', async () => {
    renderRoute('/questionnaire')

    expect(await screen.findByRole('heading', { level: 1, name: t.start.title })).toBeInTheDocument()
    expect(screen.getByText(t.steps.preferences.title)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: t.start.start })).toHaveAttribute('href', '/questionnaire/step/1')
  })

  it('blocks "Далі" until every choice is answered, then clears the error', async () => {
    const u = user()
    renderRoute('/questionnaire/step/1')

    await screen.findByRole('button', { name: t.next })
    await next(u)
    expect(screen.getByRole('alert')).toHaveTextContent(t.errors.choose)
    expect(screen.getByRole('heading', { level: 1, name: t.steps.housing.title })).toBeInTheDocument()

    await choose(u, t.questions.housing_type, 'Квартира')
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
    await next(u)
    expect(await screen.findByRole('heading', { level: 1, name: t.steps.lifestyle.title })).toBeInTheDocument()
  })

  it('reads slider bounds from the contract and shows the "+" when capped', async () => {
    renderRoute('/questionnaire/step/1')

    const slider = await screen.findByRole('slider', { name: new RegExp(t.questions.alone_hours_daily) })
    expect(slider).toHaveAttribute('aria-valuemin', '0')
    expect(slider).toHaveAttribute('aria-valuemax', '24')
    expect(slider).toHaveAttribute('aria-valuenow', '6')
    expect(screen.getByText('24 год')).toBeInTheDocument()
  })

  it('sends the user to the first unanswered step when jumping ahead', async () => {
    renderRoute('/questionnaire/step/4')

    expect(await screen.findByRole('heading', { level: 1, name: t.steps.housing.title })).toBeInTheDocument()
  })

  it('reviews every answer and submits the exact payload, keeping the session id for the visit', async () => {
    const u = user()
    let body: unknown
    server.use(
      http.post(`${API_BASE_URL}/api/v1/sessions`, async ({ request }) => {
        body = await request.json()
        return HttpResponse.json(
          { session_id: MOCK_SESSION_ID, answers_version: 1, questionnaire_version: '1.0.0', submitted_at: 'x' },
          { status: 201 },
        )
      }),
    )
    await answerEverything(u)

    expect(screen.getByText('6 год на день')).toBeInTheDocument()
    expect(screen.getByText('Квартира')).toBeInTheDocument()
    await u.click(screen.getByRole('button', { name: t.review.submit }))

    expect(await screen.findByRole('heading', { level: 1, name: copy.results.title })).toBeInTheDocument()
    expect(body).toEqual({
      activity_level: 3,
      shedding_tolerance: 2,
      grooming_capacity: 2,
      mental_stimulation_capacity: 3,
      barking_tolerance: 2,
      preferred_size: 'Small',
      has_young_children: true,
      has_other_dogs: false,
      has_cats: false,
      housing_type: 'apartment',
      walk_minutes_daily: 45,
      alone_hours_daily: 6,
      owner_experience: 'none',
      training_minutes_daily: 20,
      hard_limits: { shedding_level: 2 },
    })
    expect(readGuestSessionId()).toBe(MOCK_SESSION_ID)
  })

  it('changing answers from the results updates the same session and recomputes', async () => {
    const u = user()
    let put: { sessionId: unknown; body: unknown } | undefined
    let posts = 0
    server.use(
      http.put(`${API_BASE_URL}/api/v1/sessions/:sessionId/answers`, async ({ params, request }) => {
        put = { sessionId: params.sessionId, body: await request.json() }
        return HttpResponse.json({ session_id: MOCK_SESSION_ID, answers_version: 2, questionnaire_version: '1.0.0', submitted_at: 'x' })
      }),
      http.post(`${API_BASE_URL}/api/v1/sessions/:sessionId/recommendations`, () => {
        posts++
        return HttpResponse.json({ ...recommendationRun, answers_version: posts }, { status: 201 })
      }),
    )
    await answerEverything(u)
    await u.click(screen.getByRole('button', { name: t.review.submit }))
    await screen.findByRole('heading', { level: 1, name: copy.results.title })

    await u.click(screen.getAllByRole('link', { name: copy.results.changeAnswers })[0]!)
    // The review still has the answers.
    expect(await screen.findByText('Квартира')).toBeInTheDocument()
    await u.click(screen.getByRole('button', { name: t.review.submit }))

    expect(await screen.findByRole('heading', { level: 1, name: copy.results.title })).toBeInTheDocument()
    expect(put?.sessionId).toBe(MOCK_SESSION_ID)
    expect(put?.body).toMatchObject({ housing_type: 'apartment' })
    expect(posts).toBe(2)
  })

  it('restores the answers of the current session after a page reload', async () => {
    saveGuestSessionId(MOCK_SESSION_ID)
    renderRoute('/questionnaire/review')

    expect(await screen.findByRole('heading', { level: 1, name: t.review.title })).toBeInTheDocument()
    expect(screen.getByText('Квартира')).toBeInTheDocument()
    expect(screen.getByText('6 год на день')).toBeInTheDocument()
  })

  it('starts empty and forgets the session when it has expired', async () => {
    saveGuestSessionId(MOCK_SESSION_ID)
    server.use(
      http.get(`${API_BASE_URL}/api/v1/sessions/:sessionId/answers`, () =>
        HttpResponse.json({ status: 404, title: 'Not Found' }, { status: 404 }),
      ),
    )
    renderRoute('/questionnaire/review')

    expect(await screen.findByRole('heading', { level: 1, name: t.steps.housing.title })).toBeInTheDocument()
    expect(readGuestSessionId()).toBeNull()
  })

  it('shows every 422 field error in our wording, linked to its step', async () => {
    const u = user()
    server.use(
      http.post(`${API_BASE_URL}/api/v1/sessions`, () =>
        HttpResponse.json(
          {
            status: 422,
            title: 'Validation Failed',
            field_errors: [
              { field: 'activity_level', code: 'invalid_integer', message: 'fallback', params: { min: 1, max: 5 } },
            ],
          },
          { status: 422, headers: { 'Content-Type': 'application/problem+json' } },
        ),
      ),
    )
    await answerEverything(u)

    await u.click(screen.getByRole('button', { name: t.review.submit }))

    const alert = await screen.findByRole('alert')
    expect(alert).toHaveTextContent(t.review.serverErrorsTitle)
    expect(alert).toHaveTextContent('Оберіть значення від 1 до 5')
    expect(within(alert).getByRole('link', { name: t.review.labels.activity_level })).toHaveAttribute(
      'href',
      '/questionnaire/step/2',
    )
    expect(readGuestSessionId()).toBeNull()
  })
})
