import { Link, Outlet } from 'react-router'

import { copy } from '@/copy/uk'

export function Layout() {
  return (
    <div className="flex min-h-svh flex-col">
      <header className="border-b">
        <div className="mx-auto flex max-w-5xl items-center px-4 py-3">
          <Link to="/" className="font-semibold">
            {copy.app.name}
          </Link>
        </div>
      </header>
      <main className="mx-auto w-full max-w-5xl flex-1 px-4 py-8">
        <Outlet />
      </main>
    </div>
  )
}
