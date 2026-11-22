import { screen } from '@testing-library/react'

import { copy } from '@/copy/uk'
import { renderRoute } from '@/test/render'

describe('routes', () => {
  it('renders the landing page at /', () => {
    renderRoute('/')

    expect(screen.getByRole('heading', { level: 1, name: copy.landing.title })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: copy.app.name })).toHaveAttribute('href', '/')
  })

  it('renders the not-found page for unknown paths', () => {
    renderRoute('/no-such-page')

    expect(screen.getByRole('heading', { name: copy.notFound.title })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: copy.notFound.backHome })).toHaveAttribute('href', '/')
  })
})
