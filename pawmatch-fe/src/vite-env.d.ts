/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** Backend origin, e.g. http://localhost:8080. */
  readonly VITE_API_BASE_URL?: string
  /** "true" serves API calls from MSW mocks instead of the backend (development only). */
  readonly VITE_API_MOCKING?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
