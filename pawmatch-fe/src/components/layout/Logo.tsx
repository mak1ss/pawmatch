import { Link } from 'react-router'

import { paths } from '@/app/routePaths'
import logoMark from '@/assets/brand/logo-mark.svg'
import { copy } from '@/copy/uk'

/** Brand mark and name, linking home. */
export function Logo() {
  return (
    <Link to={paths.home} className="inline-flex items-center gap-2.5 rounded-lg text-[22px] font-bold text-foreground">
      <img src={logoMark} alt="" width={36} height={36} />
      {copy.app.name}
    </Link>
  )
}
