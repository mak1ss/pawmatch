import { Button } from '@/components/ui/button'
import { copy } from '@/copy/uk'

/** Placeholder landing page; the questionnaire route arrives with the questionnaire feature. */
export function LandingPage() {
  return (
    <section className="flex max-w-2xl flex-col items-start gap-4">
      <h1 className="text-3xl font-semibold tracking-tight">{copy.landing.title}</h1>
      <p className="text-muted-foreground">{copy.landing.description}</p>
      <Button disabled>{copy.landing.start}</Button>
    </section>
  )
}
