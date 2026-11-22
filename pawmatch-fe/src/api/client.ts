import type { FieldError, ProblemDetail } from '@/api/types'

export const API_BASE_URL: string = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

/** A non-2xx response from the backend, carrying its RFC 9457 problem body when there is one. */
export class ApiError extends Error {
  readonly status: number
  readonly problem: ProblemDetail | null

  constructor(status: number, problem: ProblemDetail | null) {
    super(problem?.detail ?? problem?.title ?? `Request failed with status ${status}`)
    this.name = 'ApiError'
    this.status = status
    this.problem = problem
  }

  /** Validation errors of a 422 response; empty for any other error. */
  get fieldErrors(): FieldError[] {
    return this.problem?.field_errors ?? []
  }
}

/**
 * Calls the PawMatch API. Paths are relative to the API root, e.g. `/api/v1/breeds`.
 * Resolves with the parsed JSON body (or `undefined` for empty responses); rejects with {@link ApiError}.
 */
export async function apiFetch<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers)
  headers.set('Accept', 'application/json, application/problem+json')
  if (init.body !== undefined && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  const response = await fetch(`${API_BASE_URL}${path}`, { ...init, headers })
  const body = await readJson(response)

  if (!response.ok) {
    throw new ApiError(response.status, isProblem(body) ? body : null)
  }
  return body as T
}

async function readJson(response: Response): Promise<unknown> {
  const text = await response.text()
  if (!text) {
    return undefined
  }
  try {
    return JSON.parse(text) as unknown
  } catch {
    return undefined
  }
}

function isProblem(body: unknown): body is ProblemDetail {
  return typeof body === 'object' && body !== null && 'status' in body
}
