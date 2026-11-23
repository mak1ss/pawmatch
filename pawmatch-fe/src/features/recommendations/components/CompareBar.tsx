import { XIcon } from 'lucide-react'
import { Link } from 'react-router'

import type { RecommendedBreed } from '@/api/recommendations'
import { paths } from '@/app/routePaths'
import { Container } from '@/components/layout/Container'
import { Button } from '@/components/ui/button'
import { copy } from '@/copy/uk'
import { BreedPhoto } from '@/features/recommendations/components/BreedPhoto'
import { MIN_COMPARE } from '@/features/recommendations/model'

const t = copy.results.compare

/** Bottom panel with the breeds picked for comparison (Figma "Панель порівняння"). Shown once a breed is picked. */
export function CompareBar({ breeds, onRemove }: { breeds: RecommendedBreed[]; onRemove: (slug: string) => void }) {
  const ready = breeds.length >= MIN_COMPARE
  return (
    <section aria-label={t.selected} className="fixed inset-x-0 bottom-0 z-10 border-t border-border bg-card">
      <Container className="flex min-h-[84px] items-center gap-4 py-3">
        <p className="hidden shrink-0 text-base font-bold lg:block">{t.selected}</p>
        <ul className="flex min-w-0 flex-1 gap-2 overflow-x-auto py-1">
          {breeds.map((breed) => (
            <li
              key={breed.breed_id}
              className="flex h-[42px] shrink-0 items-center gap-2 rounded-full border border-border bg-card pr-2 pl-1.5"
            >
              <BreedPhoto breed={breed} compact className="size-7 shrink-0 rounded-full" />
              <span className="text-sm font-bold whitespace-nowrap">{breed.breed_name}</span>
              <button
                type="button"
                onClick={() => onRemove(breed.breed_id)}
                aria-label={t.remove(breed.breed_name)}
                className="flex size-6 items-center justify-center rounded-full text-muted-foreground hover:bg-accent"
              >
                <XIcon className="size-3.5" strokeWidth={2.5} />
              </button>
            </li>
          ))}
        </ul>
        {ready ? (
          <Button asChild size="form" className="shrink-0">
            <Link to={paths.compareBreeds(breeds.map((breed) => breed.breed_id))}>{t.compare(breeds.length)}</Link>
          </Button>
        ) : (
          <p className="shrink-0 text-sm font-bold text-muted-foreground">{t.hint}</p>
        )}
      </Container>
    </section>
  )
}
