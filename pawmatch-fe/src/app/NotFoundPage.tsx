import { Link } from 'react-router'

import { paths } from '@/app/routePaths'
import { Container } from '@/components/layout/Container'
import { Button } from '@/components/ui/button'
import { copy } from '@/copy/uk'

export function NotFoundPage() {
  return (
    <Container className="flex flex-col items-start gap-6 py-16">
      <h1 className="text-[40px] leading-tight font-bold">{copy.notFound.title}</h1>
      <Button asChild variant="outline" size="xl">
        <Link to={paths.home}>{copy.notFound.backHome}</Link>
      </Button>
    </Container>
  )
}
