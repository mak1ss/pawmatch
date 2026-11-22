import { http, HttpResponse } from 'msw'

import { API_BASE_URL, apiFetch, ApiError } from '@/api/client'
import { server } from '@/mocks/server'

const url = (path: string) => `${API_BASE_URL}${path}`

describe('apiFetch', () => {
  it('returns the parsed JSON body', async () => {
    const contract = await apiFetch<{ version: string; fields: unknown[] }>('/api/v1/questionnaire')

    expect(contract.version).toBe('1.0.0')
    expect(contract.fields).toHaveLength(16)
  })

  it('exposes field errors of a 422 problem response', async () => {
    server.use(
      http.post(url('/api/v1/sessions'), () =>
        HttpResponse.json(
          {
            title: 'Validation Failed',
            status: 422,
            field_errors: [
              { field: 'activity_level', code: 'invalid_integer', message: '…', params: { min: 1, max: 5 } },
            ],
          },
          { status: 422, headers: { 'Content-Type': 'application/problem+json' } },
        ),
      ),
    )

    const error = await apiFetch('/api/v1/sessions', { method: 'POST', body: '{}' }).catch((e: unknown) => e)

    expect(error).toBeInstanceOf(ApiError)
    expect((error as ApiError).status).toBe(422)
    expect((error as ApiError).fieldErrors).toEqual([
      { field: 'activity_level', code: 'invalid_integer', message: '…', params: { min: 1, max: 5 } },
    ])
  })

  it('rejects non-problem error bodies with an empty problem', async () => {
    server.use(http.get(url('/api/v1/breeds'), () => new HttpResponse('Bad Gateway', { status: 502 })))

    const error = await apiFetch('/api/v1/breeds').catch((e: unknown) => e)

    expect(error).toBeInstanceOf(ApiError)
    expect((error as ApiError).status).toBe(502)
    expect((error as ApiError).problem).toBeNull()
    expect((error as ApiError).fieldErrors).toEqual([])
  })

  it('sends JSON content type when there is a body', async () => {
    let contentType: string | null = null
    server.use(
      http.post(url('/api/v1/sessions'), ({ request }) => {
        contentType = request.headers.get('Content-Type')
        return HttpResponse.json({ session_id: 'x' }, { status: 201 })
      }),
    )

    await apiFetch('/api/v1/sessions', { method: 'POST', body: JSON.stringify({}) })

    expect(contentType).toBe('application/json')
  })
})
