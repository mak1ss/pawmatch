import type { RecommendedBreed } from '@/api/recommendations'
import { copy } from '@/copy/uk'
import { cn } from '@/lib/utils'

/** The breed photo, or a peach placeholder while the catalogue has no photo for it (`image_url` is null). */
export function BreedPhoto({
  breed,
  className,
  compact = false,
}: {
  breed: RecommendedBreed
  className?: string
  /** Thumbnail size: the placeholder shows only the paw. */
  compact?: boolean
}) {
  if (breed.image_url) {
    return (
      <img
        src={breed.image_url}
        alt={compact ? '' : copy.results.card.photoAlt(breed.breed_name)}
        className={cn('object-cover', className)}
      />
    )
  }
  return (
    <div className={cn('flex flex-col items-center justify-center gap-2 bg-accent', className)}>
      <img src="/pawmatch_logo.svg" alt="" width={compact ? 18 : 48} height={compact ? 18 : 48} />
      {!compact && <span className="text-sm font-bold text-accent-foreground">{copy.results.card.noPhoto}</span>}
    </div>
  )
}
