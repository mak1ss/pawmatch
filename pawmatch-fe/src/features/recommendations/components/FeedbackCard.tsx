import { useState } from 'react'

import { Button } from '@/components/ui/button'
import { copy } from '@/copy/uk'

const t = copy.results.feedback

/**
 * "Чи корисні ці рекомендації?" (Figma "Відгук"). Optional. The feedback endpoint (backend M5) doesn't exist yet,
 * so the answer is only acknowledged here; send it with POST /recommendations/{run_id}/feedback once it lands.
 */
export function FeedbackCard() {
  const [answered, setAnswered] = useState(false)
  return (
    <section
      aria-labelledby="feedback-title"
      className="flex flex-col gap-4 rounded-2xl border border-border bg-card px-6 py-6 sm:flex-row sm:items-center sm:justify-between sm:px-[30px]"
    >
      <div>
        <h2 id="feedback-title" className="text-xl font-bold">
          {t.title}
        </h2>
        <p className="mt-1 text-[15px] text-muted-foreground">{t.text}</p>
      </div>
      {answered ? (
        <p role="status" className="text-base font-bold text-primary">
          {t.thanks}
        </p>
      ) : (
        <div className="flex gap-3">
          <Button variant="outline" className="h-12 px-6 text-base font-bold" onClick={() => setAnswered(true)}>
            {t.useful}
          </Button>
          <Button variant="outline" className="h-12 px-6 text-base font-bold" onClick={() => setAnswered(true)}>
            {t.notUseful}
          </Button>
        </div>
      )}
    </section>
  )
}
