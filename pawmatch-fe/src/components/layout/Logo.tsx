import { Link } from 'react-router'

import { paths } from '@/app/routePaths'
import { copy } from '@/copy/uk'

/** Brand mark (paw, served from public/) and name, linking home. Used in the header and the footer. */
export function Logo() {
  return (
    <Link to={paths.home} className="inline-flex items-center gap-2.5 rounded-lg text-[22px] font-bold text-foreground">
      {/* Decorative: the visible name next to it is the link's accessible name. */}
      <img src="/pawmatch_logo.svg" alt="" width={36} height={36} />
      {copy.app.name}
    </Link>
  )
}
