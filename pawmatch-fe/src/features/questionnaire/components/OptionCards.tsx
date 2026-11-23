import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group'
import { cn } from '@/lib/utils'

export interface OptionCard {
  value: string
  label: string
}

/**
 * Single-choice answers as cards (Figma "Варіант: …"): a radio dot plus label, or a centred label without the dot
 * (`showDot={false}`, used for sizes). Built on the Radix radio group, so arrow keys and screen readers work.
 */
export function OptionCards({
  name,
  labelledBy,
  options,
  value,
  onChange,
  invalid,
  describedBy,
  showDot = true,
  className,
}: {
  name: string
  labelledBy: string
  options: OptionCard[]
  value: string | null
  onChange: (value: string) => void
  invalid?: boolean
  describedBy?: string
  showDot?: boolean
  className?: string
}) {
  const compact = options.length > 3
  return (
    <RadioGroup
      name={name}
      value={value ?? ''}
      onValueChange={onChange}
      aria-labelledby={labelledBy}
      aria-describedby={describedBy}
      aria-invalid={invalid || undefined}
      className={cn('grid gap-3', className)}
    >
      {options.map((option) => {
        const id = `${name}-${option.value}`
        return (
          <label
            key={option.value}
            htmlFor={id}
            className={cn(
              // Five-option rows keep only ~10px right padding, as in the mockup, so labels like "Дуже спокійна" fit.
              'flex h-14 cursor-pointer items-center gap-2 rounded-lg border-[1.5px] border-border bg-card font-bold transition-colors',
              showDot && 'pl-4',
              showDot ? (compact ? 'pr-2.5' : 'pr-4') : 'px-2.5',
              'hover:border-primary/60 has-[:focus-visible]:ring-3 has-[:focus-visible]:ring-ring/50',
              'has-data-[state=checked]:border-2 has-data-[state=checked]:border-primary has-data-[state=checked]:bg-accent',
              compact ? 'text-[15px]' : 'text-base',
              !showDot && 'justify-center has-data-[state=checked]:text-accent-foreground',
              invalid && 'border-destructive',
            )}
          >
            {showDot ? (
              <RadioGroupItem
                id={id}
                value={option.value}
                className="size-5 border-2 border-border bg-card data-checked:border-primary data-checked:bg-card [&_[data-slot=radio-group-indicator]>span]:size-2.5 [&_[data-slot=radio-group-indicator]>span]:bg-primary"
              />
            ) : (
              // Visually hidden but still focusable; the wrapper keeps it out of the card's layout.
              <span className="sr-only">
                <RadioGroupItem id={id} value={option.value} />
              </span>
            )}
            <span className="truncate">{option.label}</span>
          </label>
        )
      })}
    </RadioGroup>
  )
}
