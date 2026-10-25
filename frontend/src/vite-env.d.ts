/// <reference types="vite/client" />

declare global {
  interface ImportMetaEnv {
    readonly VITE_APP_BASE_API: string
    readonly VITE_APP_TITLE: string
    readonly BASE_URL: string
    readonly MODE: string
    readonly DEV: boolean
    readonly PROD: boolean
    readonly SSR: boolean
  }

  interface ImportMeta {
    readonly env: ImportMetaEnv
    readonly url: string
    readonly glob: <T>(pattern: string) => Record<string, () => Promise<T>>
    readonly globEager: <T>(pattern: string) => Record<string, T>
    readonly hot?: {
      accept: () => void
      dispose: (callback: () => void) => void
    }
  }
}

export {}
