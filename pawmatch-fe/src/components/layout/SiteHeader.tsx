import { Link } from 'react-router'

import { paths } from '@/app/routePaths'
import { Container } from '@/components/layout/Container'
import { Logo } from '@/components/layout/Logo'
import { Button } from '@/components/ui/button'
import { copy } from '@/copy/uk'

const navLinks = [
  { to: paths.howItWorks, label: copy.nav.howItWorks },
  { to: paths.questionnaire, label: copy.nav.findBreed },
  { to: paths.compare, label: copy.nav.compare },
] as const

/** White top bar: logo, main navigation and the log-in button (Figma "Шапка"). */
export function SiteHeader() {
  return (
    <header className="border-b border-border bg-card">
      <Container className="flex h-20 items-center justify-between gap-6">
        <Logo />
        <div className="flex items-center gap-8">
          <nav aria-label={copy.nav.label} className="hidden md:block">
            <ul className="flex items-center gap-8 text-base font-bold">
              {navLinks.map((link) => (
                <li key={link.to}>
                  <Link to={link.to} className="rounded-md text-foreground transition-colors hover:text-primary">
                    {link.label}
                  </Link>
                </li>
              ))}
            </ul>
          </nav>
          <Button asChild variant="outline-primary" size="nav">
            <Link to={paths.login}>{copy.nav.login}</Link>
          </Button>
        </div>
      </Container>
    </header>
  )
}
