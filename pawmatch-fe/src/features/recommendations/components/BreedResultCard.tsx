import { Link } from 'react-router'

import type { RecommendedBreed } from '@/api/recommendations'
import { paths } from '@/app/routePaths'
import reasonWarning from '@/assets/results/reason-warning.svg'
import reasonYes from '@/assets/results/reason-yes.svg'
import { Button } from '@/components/ui/button'
import { Checkbox } from '@/components/ui/checkbox'
import { copy } from '@/copy/uk'
import { BreedPhoto } from '@/features/recommendations/components/BreedPhoto'
import { breedTags, displayIndex } from '@/features/recommendations/model'

const t = copy.results.card

/**
 * One recommended breed (Figma "Картка" on screen 11): rank, photo, tags, the model's reasons and warnings (as-is),
 * the compatibility index, a link to the breed card, and the compare checkbox.
 */
export function BreedResultCard({
  breed,
  rank,
  compared,
  compareDisabled,
  onCompareChange,
}: {
  breed: RecommendedBreed
  rank: number
  compared: boolean
  compareDisabled: boolean
  onCompareChange: (compared: boolean) => void
}) {
  const index = displayIndex(breed)
  const titleId = `breed-${breed.breed_id}`
  const compareId = `compare-${breed.breed_id}`

  return (
    <article
      aria-labelledby={titleId}
      className="grid gap-6 rounded-2xl border border-border bg-card p-4 sm:p-6 lg:grid-cols-[220px_minmax(0,1fr)_257px]"
    >
      <div className="relative self-start">
        <BreedPhoto breed={breed} className="aspect-[220/200] w-full rounded-[16px]" />
        <span
          className="absolute top-2 left-2 flex size-8 items-center justify-center rounded-full bg-primary text-[15px] font-bold text-primary-foreground"
        >
          <span aria-hidden>{rank}</span>
          <span className="sr-only">{t.rank(rank)}</span>
        </span>
      </div>

      <div className="min-w-0">
        <h2 id={titleId} className="text-[24px] leading-tight font-bold sm:text-[26px]">
          {breed.breed_name}
        </h2>
        <ul className="mt-3 flex flex-wrap gap-2">
          {breedTags(breed).map((tag) => (
            <li key={tag} className="rounded-full bg-accent px-3 py-1 text-sm font-bold text-accent-foreground">
              {tag}
            </li>
          ))}
        </ul>

        <div className="mt-5 grid gap-5 sm:grid-cols-2">
          <TextList title={t.whyFits} icon={reasonYes} items={breed.reasons} />
          <TextList title={t.considerations} icon={reasonWarning} items={breed.warnings} />
        </div>

        <a
          href={breed.source_url}
          target="_blank"
          rel="noreferrer"
          className="mt-4 inline-block rounded-md text-sm font-bold text-muted-foreground underline-offset-2 hover:underline"
        >
          {t.source}
        </a>
      </div>

      <div className="flex flex-col border-t border-border pt-5 lg:border-t-0 lg:border-l lg:pt-0 lg:pl-5">
        <p className="text-sm font-bold text-muted-foreground">{t.index}</p>
        <p className="mt-1 flex items-baseline gap-2">
          <span className="sr-only">{t.indexLabel(index)}</span>
          <span aria-hidden className="text-[52px] leading-none font-bold">
            {index}
          </span>
          <span aria-hidden className="text-lg font-bold text-muted-foreground">
            {t.outOf}
          </span>
        </p>
        <div aria-hidden className="mt-4 h-2 overflow-hidden rounded-full bg-muted">
          <div className="h-full rounded-full bg-primary" style={{ width: `${Math.min(100, Math.max(0, index))}%` }} />
        </div>

        <Button asChild className="mt-4 h-[46px] w-full text-base font-bold">
          <Link to={paths.breed(breed.breed_id)} aria-label={t.detailsLabel(breed.breed_name)}>
            {t.details}
          </Link>
        </Button>

        <div className="mt-4 flex items-center gap-2.5">
          <Checkbox
            id={compareId}
            checked={compared}
            disabled={compareDisabled && !compared}
            onCheckedChange={(next) => onCompareChange(next === true)}
            className="size-[22px] rounded-[6px] border-[1.5px] border-border bg-card [&_svg]:size-4"
          />
          <label
            htmlFor={compareId}
            className="cursor-pointer text-[15px] font-bold peer-disabled:cursor-not-allowed peer-disabled:opacity-50"
          >
            {t.addToCompare}
            <span className="sr-only">: {breed.breed_name}</span>
          </label>
        </div>
      </div>
    </article>
  )
}

function TextList({ title, icon, items }: { title: string; icon: string; items: string[] }) {
  if (items.length === 0) return null
  return (
    <div>
      <h3 className="text-[15px] font-bold">{title}</h3>
      <ul className="mt-2.5 flex flex-col gap-2">
        {items.map((item) => (
          <li key={item} className="flex gap-2 text-[15px] leading-snug">
            <img src={icon} alt="" width={20} height={20} className="mt-px size-5 shrink-0 self-start" />
            {item}
          </li>
        ))}
      </ul>
    </div>
  )
}
