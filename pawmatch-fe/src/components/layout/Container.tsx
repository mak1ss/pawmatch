import type { ComponentProps } from 'react'

import { cn } from '@/lib/utils'

/** Page content column: 1200px wide on desktop (the Figma layout grid), with side padding on smaller screens. */
export function Container({ className, ...props }: ComponentProps<'div'>) {
  return <div className={cn('mx-auto w-full max-w-[1200px] px-4 sm:px-6 xl:px-0', className)} {...props} />
}
