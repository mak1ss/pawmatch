import { screen, within } from '@testing-library/react'

import { copy } from '@/copy/uk'
import { renderRoute } from '@/test/render'

describe('routes', () => {
  it('renders the landing page at /', () => {
    renderRoute('/')

    expect(screen.getByRole('heading', { level: 1, name: copy.landing.title })).toBeInTheDocument()
    expect(screen.getByRole('heading', { level: 2, name: copy.landing.steps.title })).toBeInTheDocument()
    expect(screen.getByRole('heading', { level: 2, name: copy.landing.cta.title })).toBeInTheDocument()
  })

  it('shows the header and footer on every page, with the logo linking home', () => {
    renderRoute('/')

    const header = screen.getByRole('banner')
    const footer = screen.getByRole('contentinfo')
    expect(within(header).getByRole('link', { name: copy.app.name })).toHaveAttribute('href', '/')
    expect(within(footer).getByRole('link', { name: copy.app.name })).toHaveAttribute('href', '/')
    expect(within(header).getByRole('link', { name: copy.nav.login })).toHaveAttribute('href', '/login')
  })

  it('starts the questionnaire from the hero and the call to action', () => {
    renderRoute('/')

    const main = screen.getByRole('main')
    expect(within(main).getByRole('link', { name: copy.landing.start })).toHaveAttribute('href', '/questionnaire')
    expect(within(main).getByRole('link', { name: copy.landing.cta.button })).toHaveAttribute('href', '/questionnaire')
  })

  it('renders the not-found page for unknown paths', () => {
    renderRoute('/no-such-page')

    expect(screen.getByRole('heading', { name: copy.notFound.title })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: copy.notFound.backHome })).toHaveAttribute('href', '/')
  })
})
