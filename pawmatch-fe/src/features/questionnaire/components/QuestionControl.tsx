import { useController, useFormContext } from 'react-hook-form'

import { copy } from '@/copy/uk'
import { AmountSlider, ValueBadge } from '@/features/questionnaire/components/AmountSlider'
import { OptionCards } from '@/features/questionnaire/components/OptionCards'
import { QuestionField } from '@/features/questionnaire/components/QuestionField'
import { errorId, questionLabelId } from '@/features/questionnaire/questionIds'
import { StrictCheckbox } from '@/features/questionnaire/components/StrictCheckbox'
import { formatAmount, optionLabel } from '@/features/questionnaire/format'
import type { Question, QuestionnaireValues } from '@/features/questionnaire/model'
import { contractField, optionCodes, sliderRange } from '@/features/questionnaire/model'
import { useContract } from '@/features/questionnaire/contractContext'

const t = copy.questionnaire

const gridByCount: Record<number, string> = {
  2: 'grid-cols-2 sm:max-w-[372px]',
  3: 'sm:grid-cols-3',
  5: 'grid-cols-2 sm:grid-cols-3 lg:grid-cols-5',
  6: 'grid-cols-2 sm:grid-cols-3 lg:grid-cols-6',
}

/** Renders one question with the control its kind needs, wired to the questionnaire form. */
export function QuestionControl({ question }: { question: Question }) {
  const contract = useContract()
  const { control, clearErrors, setValue, watch } = useFormContext<QuestionnaireValues>()
  const { field, fieldState } = useController({ control, name: question.field })
  const labelId = questionLabelId(question.field)
  const error = fieldState.error?.message

  if (question.kind === 'slider') {
    const { min, max } = sliderRange(contract, question)
    const contractMax = contractField(contract, question.field)?.max ?? max
    const value = field.value as number
    return (
      <QuestionField field={question.field} aside={<ValueBadge>{formatAmount(value, question.unit)}</ValueBadge>}>
        <AmountSlider
          labelledBy={labelId}
          value={value}
          min={min}
          max={max}
          onChange={field.onChange}
          valueText={formatAmount(value, question.unit)}
          minLabel={formatAmount(min, question.unit)}
          // "180+ хв" when the contract allows more than the slider shows.
          maxLabel={max < contractMax ? formatAmount(max, question.unit).replace(' ', '+ ') : formatAmount(max, question.unit)}
        />
      </QuestionField>
    )
  }

  const options =
    question.kind === 'yes-no'
      ? [
          { value: 'true', label: t.yes },
          { value: 'false', label: t.no },
        ]
      : optionCodes(contract, question.field).map((code) => ({ value: code, label: optionLabel(question.field, code) }))

  const parse = (raw: string): string | number | boolean =>
    question.kind === 'yes-no' ? raw === 'true' : question.kind === 'scale' ? Number(raw) : raw

  const strict = question.strict
  const strictChecked = watch(`strict.${strict ?? 'allowed_sizes'}`)
  const sizeIsAny = question.kind === 'size' && field.value === 'any'

  return (
    <QuestionField field={question.field} error={error}>
      <OptionCards
        name={question.field}
        labelledBy={labelId}
        options={options}
        value={field.value === null ? null : String(field.value)}
        onChange={(raw) => {
          const next = parse(raw)
          field.onChange(next)
          clearErrors(question.field)
          if (question.kind === 'size' && next === 'any') {
            setValue('strict.allowed_sizes', false)
          }
        }}
        invalid={Boolean(error)}
        describedBy={error ? errorId(question.field) : undefined}
        showDot={question.kind !== 'size'}
        className={gridByCount[options.length]}
      />
      {strict && (
        <StrictCheckbox
          id={`${question.field}-strict`}
          label={strict === 'allowed_sizes' ? t.constraints.allowed_sizes : t.constraints.hardLimit}
          checked={strictChecked}
          disabled={sizeIsAny}
          onChange={(checked) => setValue(`strict.${strict}`, checked)}
        />
      )}
    </QuestionField>
  )
}
