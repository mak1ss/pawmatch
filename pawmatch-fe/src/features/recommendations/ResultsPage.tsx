import { useEffect, useState } from 'react'
import { Link, useLocation } from 'react-router'

import { ApiError } from '@/api/client'
import type { RecommendationRun } from '@/api/recommendations'
import { useRecommendations } from '@/api/recommendations'
import { paths } from '@/app/routePaths'
import { Container } from '@/components/layout/Container'
import { Button } from '@/components/ui/button'
import { copy } from '@/copy/uk'
import { clearGuestSessionId, readGuestSessionId } from '@/features/questionnaire/guestSession'
import { BreedResultCard } from '@/features/recommendations/components/BreedResultCard'
import { CompareBar } from '@/features/recommendations/components/CompareBar'
import { FeedbackCard } from '@/features/recommendations/components/FeedbackCard'
import { ResultsLoading } from '@/features/recommendations/components/ResultsLoading'
import { ResultsMessage } from '@/features/recommendations/components/ResultsMessage'
import { MAX_COMPARE } from '@/features/recommendations/model'

const t = copy.results

/** Fewer breeds than this get the backend's notice (Figma 12); none at all is the empty state (Figma 13). */
const FULL_RESULT = 3

/**
 * Recommendations for the guest session (Figma 10–13): loading, the ranked breeds, fewer than three, or none.
 * The review screen passes the answers version it saved, so a stored run for older answers is never shown.
 */
export function ResultsPage() {
  const location = useLocation()
  const answersVersion = (location.state as { answersVersion?: number } | null)?.answersVersion ?? 0
  const [sessionId] = useState(readGuestSessionId)
  const recommendations = useRecommendations(sessionId, answersVersion)
  const expired = recommendations.error instanceof ApiError && recommendations.error.status === 404

  useEffect(() => {
    window.scrollTo?.(0, 0)
  }, [])

  useEffect(() => {
    if (expired) clearGuestSessionId()
  }, [expired])

  if (sessionId === null || expired) {
    const text = sessionId === null ? t.noSession : t.expired
    return (
      <Container className="pt-10 pb-20">
        <ResultsMessage
          title={text.title}
          actions={
            <Button asChild size="xl">
              <Link to={paths.questionnaire}>{t.noSession.start}</Link>
            </Button>
          }
        >
          <p>{text.text}</p>
        </ResultsMessage>
      </Container>
    )
  }

  if (recommendations.isPending) {
    return (
      <Container className="pt-10 pb-20">
        <ResultsLoading />
      </Container>
    )
  }

  if (recommendations.isError) {
    return (
      <Container className="pt-10 pb-20">
        <ResultsMessage
          role="alert"
          title={t.title}
          actions={
            <Button variant="outline" size="form" onClick={() => void recommendations.refetch()}>
              {t.retry}
            </Button>
          }
        >
          <p>{t.error}</p>
        </ResultsMessage>
      </Container>
    )
  }

  // Keyed by run, so a new run starts with nothing picked for comparison.
  return <Results key={recommendations.data.run_id} run={recommendations.data} />
}

function Results({ run }: { run: RecommendationRun }) {
  const [compared, setCompared] = useState<string[]>([])
  const breeds = run.recommendations
  const comparedBreeds = breeds.filter((breed) => compared.includes(breed.breed_id))

  const setComparedBreed = (slug: string, on: boolean) =>
    setCompared((current) => (on ? [...current, slug] : current.filter((s) => s !== slug)))

  return (
    <Container className={comparedBreeds.length > 0 ? 'pt-10 pb-40' : 'pt-10 pb-20'}>
      <header className="flex flex-col gap-6 lg:flex-row lg:items-start lg:justify-between">
        <div>
          <h1 className="text-[34px] leading-tight font-bold sm:text-[40px]">{t.title}</h1>
          {breeds.length > 0 && <p className="mt-2 text-[17px] text-muted-foreground">{t.subtitle(breeds.length)}</p>}
          {/* The index is not a probability: every results screen says so, in the backend's words. */}
          <p className="mt-2 text-[15px] font-bold text-accent-foreground">{run.score_meaning}</p>
        </div>
        <div className="flex flex-wrap gap-3">
          {breeds.length > 0 && (
            <Button asChild variant="outline-primary" size="form">
              <Link to={paths.login}>{t.saveResults}</Link>
            </Button>
          )}
          <Button asChild variant="outline" size="form">
            <Link to={paths.questionnaireReview}>{t.changeAnswers}</Link>
          </Button>
        </div>
      </header>

      {breeds.length === 0 ? (
        <ResultsMessage
          title={t.empty.title}
          actions={
            <Button asChild size="xl">
              <Link to={paths.questionnaireReview}>{t.changeAnswers}</Link>
            </Button>
          }
        >
          <p>{run.notice}</p>
          <p>{t.empty.hint}</p>
        </ResultsMessage>
      ) : (
        <>
          {breeds.length < FULL_RESULT && (
            <section aria-labelledby="fewer-title" className="mt-8 rounded-2xl bg-accent px-6 py-5">
              <h2 id="fewer-title" className="text-lg font-bold">
                {t.fewer.title}
              </h2>
              <p className="mt-1 text-[15px] font-bold text-accent-foreground">{run.notice}</p>
              <p className="mt-1 text-[15px]">{t.fewer.hint}</p>
            </section>
          )}

          <ol className="mt-8 flex flex-col gap-5">
            {breeds.map((breed, index) => (
              <li key={breed.breed_id}>
                <BreedResultCard
                  breed={breed}
                  rank={index + 1}
                  compared={compared.includes(breed.breed_id)}
                  compareDisabled={compared.length >= MAX_COMPARE}
                  onCompareChange={(on) => setComparedBreed(breed.breed_id, on)}
                />
              </li>
            ))}
          </ol>

          <div className="mt-5 flex flex-col gap-5">
            <FeedbackCard />
            {run.unassessed_criteria.length > 0 && (
              <details className="rounded-2xl border border-border bg-card px-6 py-5 sm:px-[30px]">
                <summary className="cursor-pointer text-base font-bold">{t.unassessedTitle}</summary>
                <ul className="mt-3 flex list-disc flex-col gap-1.5 pl-5 text-[15px] text-muted-foreground">
                  {run.unassessed_criteria.map((criterion) => (
                    <li key={criterion}>{criterion}</li>
                  ))}
                </ul>
              </details>
            )}
          </div>
        </>
      )}

      {comparedBreeds.length > 0 && (
        <CompareBar breeds={comparedBreeds} onRemove={(slug) => setComparedBreed(slug, false)} />
      )}
    </Container>
  )
}
