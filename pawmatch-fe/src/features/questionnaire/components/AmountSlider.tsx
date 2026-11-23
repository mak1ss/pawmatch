import { Slider } from '@/components/ui/slider'

/**
 * Amount slider (Figma "Повзунок"): 8px track, 28px thumb, min/max labels underneath. The value badge is rendered by
 * the question header.
 */
export function AmountSlider({
  labelledBy,
  value,
  min,
  max,
  onChange,
  valueText,
  minLabel,
  maxLabel,
}: {
  labelledBy: string
  value: number
  min: number
  max: number
  onChange: (value: number) => void
  valueText: string
  minLabel: string
  maxLabel: string
}) {
  return (
    <div className="pt-1.5">
      <Slider
        value={[value]}
        min={min}
        max={max}
        step={1}
        onValueChange={(next) => onChange(next[0] ?? min)}
        thumbLabelledBy={labelledBy}
        thumbValueText={valueText}
        className="py-2.5 [&_[data-slot=slider-thumb]]:size-7 [&_[data-slot=slider-thumb]]:border-[3px] [&_[data-slot=slider-thumb]]:border-primary [&_[data-slot=slider-track]]:h-2"
      />
      <div className="mt-3 flex justify-between text-sm font-bold text-muted-foreground">
        <span>{minLabel}</span>
        <span>{maxLabel}</span>
      </div>
    </div>
  )
}

/** Peach pill showing the slider's current value ("6 год"). */
export function ValueBadge({ children }: { children: string }) {
  return (
    <span className="inline-flex h-8 shrink-0 items-center rounded-full bg-accent px-3.5 text-base font-bold text-accent-foreground">
      {children}
    </span>
  )
}
