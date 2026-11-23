import { Link } from 'react-router'

import { paths } from '@/app/routePaths'
import checkBadge from '@/assets/questionnaire/check-badge.svg'
import startPhoto from '@/assets/questionnaire/start-photo.jpg'
import { Container } from '@/components/layout/Container'
import { Button } from '@/components/ui/button'
import { copy } from '@/copy/uk'
import { steps } from '@/features/questionnaire/model'

const t = copy.questionnaire

/** Questionnaire intro (Figma "02 Початок анкети"): the five blocks, what to expect, and the start button. */
export function QuestionnaireStartPage() {
  return (
    <Container className="grid items-start gap-12 pt-8 pb-20 lg:grid-cols-[minmax(0,561px)_600px] lg:justify-between">
      <div>
        <Link to={paths.home} className="rounded-md text-[15px] font-bold text-primary hover:underline">
          {t.start.backHome}
        </Link>
        <h1 className="mt-6 text-[36px] leading-tight font-bold sm:text-[46px]">{t.start.title}</h1>
        <p className="mt-3 max-w-[540px] text-lg leading-[1.6] text-muted-foreground">
          {t.start.text[0]} <br className="hidden sm:block" />
          {t.start.text[1]}
        </p>

        <ol className="mt-8 flex flex-col gap-2">
          {steps.map((step, index) => (
            <li
              key={step.key}
              className="flex h-[55px] items-center gap-3.5 rounded-2xl border border-border bg-card pr-4 pl-3.5"
            >
              <span className="flex size-8 shrink-0 items-center justify-center rounded-full bg-accent text-[15px] font-bold text-primary">
                {index + 1}
              </span>
              <span className="flex-1 text-[17px] font-bold">{t.steps[step.key].title}</span>
              <span className="text-[15px] font-bold text-muted-foreground">
                {t.questionCount(step.questions.length)}
              </span>
            </li>
          ))}
        </ol>

        <ul className="mt-9 flex flex-wrap gap-x-6 gap-y-3">
          {t.start.details.map((detail) => (
            <li key={detail} className="flex items-center gap-2 text-[15px] font-bold">
              <img src={checkBadge} alt="" width={20} height={20} />
              {detail}
            </li>
          ))}
        </ul>

        <Button asChild size="xl" className="mt-8 w-full sm:w-[240px]">
          <Link to={paths.questionnaireStep(1)}>{t.start.start}</Link>
        </Button>
      </div>

      <img
        src={startPhoto}
        alt={t.start.photoAlt}
        width={600}
        height={648}
        className="hidden aspect-[600/648] w-full rounded-3xl object-cover lg:mt-4 lg:block"
      />
    </Container>
  )
}
