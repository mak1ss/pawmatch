import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'

import { API_BASE_URL } from '@/api/client'
import type { RecommendationRun } from '@/api/recommendations'
import { copy } from '@/copy/uk'
import { readGuestSessionId, saveGuestSessionId } from '@/features/questionnaire/guestSession'
import { recommendationRun } from '@/mocks/fixtures/recommendations'
import { MOCK_SESSION_ID } from '@/mocks/handlers'
import { server } from '@/mocks/server'
import { renderRoute } from '@/test/render'

const t = copy.results
const recommendationsUrl = `${API_BASE_URL}/api/v1/sessions/:sessionId/recommendations`
const notFound = () =>
  HttpResponse.json(
    { status: 404, title: 'Not Found' },
    { status: 404, headers: { 'Content-Type': 'application/problem+json' } },
  )

function runWith(count: number, notice: string): RecommendationRun {
  return { ...recommendationRun, recommendations: recommendationRun.recommendations.slice(0, count), notice }
}

describe('results', () => {
  beforeEach(() => saveGuestSessionId(MOCK_SESSION_ID))
  afterEach(() => sessionStorage.clear())

  it('asks a visitor without a session to take the questionnaire', async () => {
    sessionStorage.clear()
    renderRoute('/results')

    expect(await screen.findByRole('heading', { name: t.noSession.title })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: t.noSession.start })).toHaveAttribute('href', '/questionnaire')
  })

  it('shows the loading screen, then the ranked breeds with the model texts as-is', async () => {
    renderRoute('/results')

    expect(await screen.findByRole('heading', { name: t.loading.title })).toBeInTheDocument()
    expect(await screen.findByRole('heading', { level: 1, name: t.title })).toBeInTheDocument()

    expect(screen.getByText(t.subtitle(5))).toHaveTextContent('Ми знайшли 5 порід')
    expect(screen.getByText(recommendationRun.score_meaning)).toBeInTheDocument()
    const cards = screen.getAllByRole('article')
    expect(cards).toHaveLength(5)

    const first = within(cards[0]!)
    expect(first.getByRole('heading', { name: 'Кавалер-кінг-чарльз-спанієль' })).toBeInTheDocument()
    expect(first.getByText(t.card.rank(1))).toBeInTheDocument()
    expect(first.getByText(t.card.indexLabel(98))).toBeInTheDocument()
    expect(first.getByText('Малий')).toBeInTheDocument()
    expect(first.getByText('Помірна активність')).toBeInTheDocument()
    expect(first.getByText('Невелике линяння')).toBeInTheDocument()
    expect(first.getByText('Активність породи — 3/5, бажаний рівень — 3/5.')).toBeInTheDocument()
    expect(first.getByText('Вокалізація: оцінка породи — 3/5, ваша допустима межа — 2/5.')).toBeInTheDocument()
    expect(first.getByRole('link', { name: t.card.detailsLabel('Кавалер-кінг-чарльз-спанієль') })).toHaveAttribute(
      'href',
      '/breeds/cavalier-king-charles-spaniel',
    )
    // Three or more breeds: no "fewer" notice.
    expect(screen.queryByRole('heading', { name: t.fewer.title })).not.toBeInTheDocument()
  })

  it('reuses the stored run after a reload instead of running the model again', async () => {
    let posts = 0
    server.use(
      http.get(`${recommendationsUrl}/latest`, () => HttpResponse.json(recommendationRun)),
      http.post(recommendationsUrl, () => {
        posts++
        return HttpResponse.json(recommendationRun, { status: 201 })
      }),
    )
    renderRoute('/results')

    expect(await screen.findAllByRole('article')).toHaveLength(5)
    expect(posts).toBe(0)
  })

  it('shows the backend notice when fewer than three breeds qualify', async () => {
    const notice = 'Знайдено лише 2 порід вище порога; слабкі варіанти не додавалися.'
    server.use(http.post(recommendationsUrl, () => HttpResponse.json(runWith(2, notice), { status: 201 })))
    renderRoute('/results')

    expect(await screen.findByRole('heading', { name: t.fewer.title })).toBeInTheDocument()
    expect(screen.getByText(notice)).toBeInTheDocument()
    expect(screen.getAllByRole('article')).toHaveLength(2)
  })

  it('shows the empty state, with the notice and a way to change answers, when no breed qualifies', async () => {
    const notice = 'Знайдено лише 0 порід вище порога; слабкі варіанти не додавалися.'
    server.use(http.post(recommendationsUrl, () => HttpResponse.json(runWith(0, notice), { status: 201 })))
    renderRoute('/results')

    expect(await screen.findByRole('heading', { name: t.empty.title })).toBeInTheDocument()
    expect(screen.getByText(notice)).toBeInTheDocument()
    expect(screen.getByText(recommendationRun.score_meaning)).toBeInTheDocument()
    expect(screen.queryByRole('article')).not.toBeInTheDocument()
    for (const link of screen.getAllByRole('link', { name: t.changeAnswers })) {
      expect(link).toHaveAttribute('href', '/questionnaire/review')
    }
  })

  it('offers to start again when the session has expired, and forgets it', async () => {
    server.use(http.post(recommendationsUrl, notFound))
    renderRoute('/results')

    expect(await screen.findByRole('heading', { name: t.expired.title })).toBeInTheDocument()
    expect(readGuestSessionId()).toBeNull()
  })

  it('offers a retry when the request fails', async () => {
    server.use(http.post(recommendationsUrl, () => HttpResponse.json({ status: 500 }, { status: 500 })))
    renderRoute('/results')

    expect(await screen.findByRole('alert')).toHaveTextContent(t.error)
    expect(screen.getByRole('button', { name: t.retry })).toBeInTheDocument()
  })

  it('collects breeds for comparison in the bottom panel', async () => {
    const u = userEvent.setup()
    renderRoute('/results')
    await screen.findAllByRole('article')

    await u.click(screen.getByRole('checkbox', { name: `${t.card.addToCompare}: Ши-тцу` }))
    const panel = screen.getByRole('region', { name: t.compare.selected })
    expect(panel).toHaveTextContent(t.compare.hint)

    await u.click(screen.getByRole('checkbox', { name: `${t.card.addToCompare}: Боксер` }))
    expect(within(panel).getByRole('link', { name: t.compare.compare(2) })).toHaveAttribute(
      'href',
      '/compare?breeds=shih-tzu,boxer',
    )

    await u.click(within(panel).getByRole('button', { name: t.compare.remove('Ши-тцу') }))
    expect(screen.getByRole('checkbox', { name: `${t.card.addToCompare}: Ши-тцу` })).not.toBeChecked()
  })

  it('thanks the user for feedback', async () => {
    const u = userEvent.setup()
    renderRoute('/results')
    await screen.findAllByRole('article')

    await u.click(screen.getByRole('button', { name: t.feedback.useful }))
    expect(screen.getByRole('status')).toHaveTextContent(t.feedback.thanks)
  })
})
