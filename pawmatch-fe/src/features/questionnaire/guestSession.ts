/**
 * The guest session id lives only for the current visit (sessionStorage), never in long-lived storage.
 * Storage can be unavailable (private mode, blocked site data), so every access is guarded.
 */
const KEY = 'pawmatch.session_id'

export function saveGuestSessionId(sessionId: string): void {
  try {
    sessionStorage.setItem(KEY, sessionId)
  } catch {
    // Without storage the session still works for this page; a reload just starts over.
  }
}

export function readGuestSessionId(): string | null {
  try {
    return sessionStorage.getItem(KEY)
  } catch {
    return null
  }
}

/** Forgets a session the backend no longer knows (404), so the next submit starts a new one. */
export function clearGuestSessionId(): void {
  try {
    sessionStorage.removeItem(KEY)
  } catch {
    // Nothing stored when storage is unavailable.
  }
}
