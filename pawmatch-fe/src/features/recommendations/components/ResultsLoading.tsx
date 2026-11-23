import { useEffect, useState } from 'react'

import reasonYes from '@/assets/results/reason-yes.svg'
import { cn } from '@/lib/utils'
import { copy } from '@/copy/uk'

const t = copy.results.loading
const STEP_MS = 900

/**
 * "Підбираємо породи…" (Figma "10 Завантаження"). The steps advance on a timer while the request runs: they show what
 * happens, not real progress, so the last step stays active until the results arrive.
 */
export function ResultsLoading() {
  const [active, setActive] = useState(1)

  useEffect(() => {
    if (active >= t.steps.length - 1) return
    const timer = window.setTimeout(() => setActive((step) => step + 1), STEP_MS)
    return () => window.clearTimeout(timer)
  }, [active])

  return (
    <div className="mx-auto mt-6 flex max-w-[560px] flex-col items-center rounded-3xl border border-border bg-card px-6 py-12 text-center sm:px-20">
      <div
        aria-hidden
        className="size-24 rounded-full border-8 border-muted border-t-primary border-r-primary motion-safe:animate-spin"
      />
      <div role="status">
        <h1 className="mt-8 text-[28px] font-bold sm:text-[30px]">{t.title}</h1>
        <p className="mt-2 text-[17px] text-muted-foreground">{t.subtitle}</p>
      </div>

      <ol className="mt-9 flex w-full flex-col gap-6 text-left">
        {t.steps.map((label, index) => {
          const state = index < active ? 'done' : index === active ? 'active' : 'pending'
          return (
            <li key={label} className="flex items-center gap-3.5">
              {state === 'done' ? (
                <img src={reasonYes} alt="" width={22} height={22} />
              ) : (
                <span
                  aria-hidden
                  className={cn(
                    'flex size-[22px] shrink-0 items-center justify-center rounded-full',
                    state === 'active' ? 'bg-accent' : 'border-[1.5px] border-border',
                  )}
                >
                  {state === 'active' && <span className="size-2 rounded-full bg-primary" />}
                </span>
              )}
              <span className={cn('text-[17px] font-bold', state === 'pending' && 'text-muted-foreground')}>{label}</span>
            </li>
          )
        })}
      </ol>
    </div>
  )
}
