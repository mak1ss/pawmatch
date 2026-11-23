import { Link } from 'react-router'

import { paths } from '@/app/routePaths'
import { Container } from '@/components/layout/Container'
import { Logo } from '@/components/layout/Logo'
import { copy } from '@/copy/uk'

/**
 * White bottom bar (Figma "Підвал"). "Про проєкт" and "Контакти" have no pages yet, so they're shown as plain text
 * rather than links that go nowhere.
 */
export function SiteFooter() {
  return (
    <footer className="border-t border-border bg-card">
      <Container className="flex flex-col gap-4 py-8 text-[15px] sm:h-[120px] sm:flex-row sm:items-center sm:justify-between sm:py-0">
        <div className="flex items-center gap-8">
          <Logo />
          <p className="text-muted-foreground">{copy.footer.copyright}</p>
        </div>
        <ul className="flex flex-wrap items-center gap-x-8 gap-y-2 font-bold text-muted-foreground">
          <li>{copy.footer.about}</li>
          <li>
            <Link to={paths.howItWorks} className="rounded-md transition-colors hover:text-primary">
              {copy.footer.howItWorks}
            </Link>
          </li>
          <li>{copy.footer.contacts}</li>
        </ul>
      </Container>
    </footer>
  )
}
