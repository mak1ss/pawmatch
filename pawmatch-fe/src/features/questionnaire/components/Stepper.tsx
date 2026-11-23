import doneCheck from '@/assets/questionnaire/step-done-check.svg'
import { copy } from '@/copy/uk'
import { steps } from '@/features/questionnaire/model'
import { cn } from '@/lib/utils'

const t = copy.questionnaire

/**
 * Five-step progress bar (Figma "Прогрес анкети"). `current` is the 0-based active step; 'review' marks every step
 * as done.
 */
export function Stepper({ current }: { current: number | 'review' }) {
  return (
    <nav aria-label={t.progressLabel} className="mx-auto max-w-[960px]">
      <ol className="grid grid-cols-5">
        {steps.map((step, index) => {
          const done = current === 'review' || index < current
          const active = index === current
          const isLast = index === steps.length - 1
          return (
            <li
              key={step.key}
              aria-current={active ? 'step' : undefined}
              className="relative flex flex-col items-center gap-2.5 text-center"
            >
              {!isLast && (
                <span
                  aria-hidden
                  className={cn(
                    'absolute top-[16.5px] right-[calc(-50%+24px)] left-[calc(50%+24px)] h-[3px] rounded-full',
                    done ? 'bg-primary' : 'bg-border',
                  )}
                />
              )}
              <span
                className={cn(
                  'flex size-9 items-center justify-center rounded-full text-[15px] font-bold',
                  done || active ? 'bg-primary text-primary-foreground' : 'border-2 border-border bg-card text-muted-foreground',
                )}
              >
                {done ? <img src={doneCheck} alt="" width={15.4} height={11.4} /> : index + 1}
              </span>
              <span
                className={cn(
                  'hidden text-[15px] font-bold sm:block',
                  done || active ? 'text-foreground' : 'text-muted-foreground',
                )}
              >
                {t.steps[step.key].title}
              </span>
            </li>
          )
        })}
      </ol>
    </nav>
  )
}
