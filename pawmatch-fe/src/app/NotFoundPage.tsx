import { Link } from 'react-router'

import { Button } from '@/components/ui/button'
import { copy } from '@/copy/uk'

export function NotFoundPage() {
  return (
    <section className="flex flex-col items-start gap-4">
      <h1 className="text-2xl font-semibold">{copy.notFound.title}</h1>
      <Button asChild variant="outline">
        <Link to="/">{copy.notFound.backHome}</Link>
      </Button>
    </section>
  )
}
