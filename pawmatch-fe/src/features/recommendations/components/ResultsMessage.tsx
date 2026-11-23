import type { ReactNode } from 'react'

/** A centred card for states without results: no session, expired session, request error, no matching breeds. */
export function ResultsMessage({
  title,
  children,
  actions,
  role,
}: {
  title: string
  children: ReactNode
  actions: ReactNode
  role?: 'alert'
}) {
  return (
    <div
      role={role}
      className="mx-auto mt-6 flex max-w-[640px] flex-col items-center rounded-3xl border border-border bg-card px-6 py-12 text-center sm:px-16"
    >
      <img src="/pawmatch_logo.svg" alt="" width={64} height={64} />
      <h2 className="mt-6 text-[26px] leading-tight font-bold">{title}</h2>
      <div className="mt-3 flex flex-col gap-2 text-[17px] leading-relaxed text-muted-foreground">{children}</div>
      <div className="mt-8 flex flex-wrap justify-center gap-3">{actions}</div>
    </div>
  )
}
