import { Checkbox } from '@/components/ui/checkbox'

/** "Обов’язкова умова" / "Розглядати лише цей розмір" checkbox under a question (Figma "Чекбокс"). */
export function StrictCheckbox({
  id,
  label,
  checked,
  disabled,
  onChange,
}: {
  id: string
  label: string
  checked: boolean
  disabled?: boolean
  onChange: (checked: boolean) => void
}) {
  return (
    <div className="mt-4 flex items-center gap-2.5">
      <Checkbox
        id={id}
        checked={checked}
        disabled={disabled}
        onCheckedChange={(next) => onChange(next === true)}
        className="size-[22px] rounded-[6px] border-[1.5px] border-border bg-card [&_svg]:size-4"
      />
      <label htmlFor={id} className="cursor-pointer text-[15px] font-bold peer-disabled:cursor-not-allowed peer-disabled:opacity-50">
        {label}
      </label>
    </div>
  )
}
