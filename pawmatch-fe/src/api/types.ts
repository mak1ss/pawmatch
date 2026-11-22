/**
 * Hand-written API types shared by all endpoints. Endpoint-specific types will come from the OpenAPI schema
 * generated with `npm run generate:api` (src/api/schema.d.ts).
 */

/** One invalid field of a 422 response. Word the message from `code` and `params`; `message` is a Ukrainian fallback. */
export interface FieldError {
  field: string
  code: string
  message: string
  params?: {
    min?: number
    max?: number
    allowed?: string[]
  }
}

/** RFC 9457 problem body returned for every error. */
export interface ProblemDetail {
  type?: string
  title?: string
  status: number
  detail?: string
  instance?: string
  field_errors?: FieldError[]
  resource?: string
  id?: string
}
