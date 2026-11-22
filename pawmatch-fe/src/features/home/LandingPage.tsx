import { useEffect } from 'react'
import { Link, useLocation } from 'react-router'

import { paths } from '@/app/routePaths'
import ctaDecor from '@/assets/landing/cta-decor.svg'
import exampleBreedThumb from '@/assets/landing/example-breed-thumb.jpg'
import heroPhoto from '@/assets/landing/hero-photo.jpg'
import stepCompare from '@/assets/landing/step-compare.svg'
import stepQuestionnaire from '@/assets/landing/step-questionnaire.svg'
import stepRecommendations from '@/assets/landing/step-recommendations.svg'
import { Container } from '@/components/layout/Container'
import { Button } from '@/components/ui/button'
import { copy } from '@/copy/uk'

const HOW_IT_WORKS_ID = 'how-it-works'
const stepIcons = [stepQuestionnaire, stepRecommendations, stepCompare] as const

/** Home page (Figma "01 Головна"): hero, how it works, what we consider, call to action. */
export function LandingPage() {
  useScrollToHash()

  return (
    <>
      <Hero />
      <HowItWorks />
      <Factors />
      <CallToAction />
    </>
  )
}

function Hero() {
  const t = copy.landing
  return (
    <Container className="grid items-start gap-12 pt-12 pb-20 lg:grid-cols-[minmax(0,1fr)_580px] lg:gap-10">
      <div className="lg:pt-6">
        <p className="inline-flex h-9 items-center rounded-full bg-accent px-3.5 text-[15px] font-bold text-accent-foreground">
          {t.tag}
        </p>
        <h1 className="mt-3 max-w-[480px] text-[40px] leading-[1.15] font-bold sm:text-5xl lg:text-[56px]">{t.title}</h1>
        <p className="mt-6 max-w-[470px] text-[19px] leading-[1.55] text-muted-foreground">{t.description}</p>
        <div className="mt-6 flex flex-wrap gap-3.5">
          <Button asChild size="xl">
            <Link to={paths.questionnaire}>{t.start}</Link>
          </Button>
          <Button asChild variant="outline" size="xl">
            <Link to={paths.howItWorks}>{t.howItWorks}</Link>
          </Button>
        </div>
        <dl className="mt-10 grid max-w-[540px] grid-cols-3 gap-4">
          {t.stats.map((stat) => (
            <div key={stat.label} className="flex flex-col-reverse gap-1">
              <dt className="text-[15px] font-bold text-muted-foreground">{stat.label}</dt>
              <dd className="text-[30px] leading-tight font-bold">{stat.value}</dd>
            </div>
          ))}
        </dl>
      </div>
      <div className="relative pb-6 lg:pb-0">
        <img
          src={heroPhoto}
          alt={t.heroPhotoAlt}
          width={1160}
          height={880}
          className="aspect-[580/470] w-full rounded-3xl object-cover"
        />
        <ExampleResultCard />
      </div>
    </Container>
  )
}

/** The illustrative result card overlapping the hero photo. */
function ExampleResultCard() {
  const example = copy.landing.example
  return (
    <div className="absolute bottom-0 left-4 flex w-[330px] max-w-[calc(100%-2rem)] items-center gap-4 rounded-[20px] bg-card px-5 py-6 lg:-bottom-3.5 lg:-left-[60px]">
      <img
        src={exampleBreedThumb}
        alt={example.breedName}
        width={64}
        height={64}
        className="size-16 shrink-0 rounded-full object-cover"
      />
      <div className="min-w-0 flex-1">
        <p className="truncate text-sm font-bold">{example.breedName}</p>
        <p className="mt-0.5 flex items-baseline gap-2">
          <span className="text-[13px] font-bold text-muted-foreground">{example.indexLabel}</span>
          <span className="text-lg font-bold text-primary">{example.index}</span>
        </p>
        <div aria-hidden className="mt-1.5 h-2 rounded-full bg-muted">
          <div className="h-full rounded-full bg-primary" style={{ width: `${example.index}%` }} />
        </div>
      </div>
    </div>
  )
}

function HowItWorks() {
  const t = copy.landing.steps
  return (
    <Container className="scroll-mt-6 pb-20" id={HOW_IT_WORKS_ID}>
      <SectionHeading title={t.title} subtitle={t.subtitle} />
      <ol className="mt-8 grid gap-[22px] md:grid-cols-3">
        {t.items.map((step, index) => (
          <li key={step.label} className="rounded-2xl border border-border bg-card p-8">
            <div className="flex items-center gap-5">
              <img src={stepIcons[index]} alt="" width={60} height={60} className="shrink-0" />
              <div>
                <p className="text-sm font-bold text-primary">{step.label}</p>
                <h3 className="text-[21px] leading-snug font-bold lg:whitespace-nowrap">{step.title}</h3>
              </div>
            </div>
            <p className="mt-7 text-base leading-[1.55] text-muted-foreground">{step.text}</p>
          </li>
        ))}
      </ol>
    </Container>
  )
}

function Factors() {
  const t = copy.landing.factors
  return (
    <Container className="pb-20">
      <SectionHeading title={t.title} subtitle={t.subtitle} />
      <ul className="mt-6 flex max-w-[1080px] flex-wrap gap-3">
        {t.items.map((factor) => (
          <li
            key={factor}
            className="inline-flex h-[42px] items-center rounded-full bg-accent px-3.5 text-base font-bold text-accent-foreground"
          >
            {factor}
          </li>
        ))}
      </ul>
    </Container>
  )
}

function CallToAction() {
  const t = copy.landing.cta
  return (
    <Container className="pb-20">
      <section className="relative overflow-hidden rounded-3xl bg-primary px-6 py-12 sm:px-16 sm:py-[62px]">
        <img
          src={ctaDecor}
          alt=""
          width={340}
          height={390}
          className="pointer-events-none absolute -top-[60px] right-0 max-w-none"
        />
        <div className="relative">
          <h2 className="text-[32px] leading-tight font-bold text-primary-foreground sm:text-[42px]">{t.title}</h2>
          <p className="mt-3 text-[19px] font-bold text-primary-foreground/90">{t.text}</p>
          <Button asChild variant="secondary" size="xl" className="mt-8 text-primary">
            <Link to={paths.questionnaire}>{t.button}</Link>
          </Button>
        </div>
      </section>
    </Container>
  )
}

function SectionHeading({ title, subtitle }: { title: string; subtitle: string }) {
  return (
    <>
      <h2 className="text-[32px] leading-tight font-bold sm:text-[40px]">{title}</h2>
      <p className="mt-1 text-lg text-muted-foreground">{subtitle}</p>
    </>
  )
}

/** Links like "/#how-it-works" navigate client-side, which doesn't scroll by itself. */
function useScrollToHash() {
  const { hash } = useLocation()
  useEffect(() => {
    if (hash) {
      // scrollIntoView is missing in jsdom (tests), hence the optional call.
      document.getElementById(decodeURIComponent(hash.slice(1)))?.scrollIntoView?.({ behavior: 'smooth' })
    }
  }, [hash])
}
