declare module 'pinia' {
  import type { App } from 'vue'
  
  export interface Pinia {
    install: (app: App) => void
  }
  
  export function createPinia(): Pinia
  
  export function defineStore<Id extends string, SS>(
    id: Id,
    setup: () => SS
  ): () => SS
  
  export function defineStore<Id extends string, S, G, A>(
    id: Id,
    options: {
      state: () => S
      getters?: G
      actions?: A
    }
  ): () => S & G & A
}

declare module 'js-cookie' {
  interface CookiesStatic {
    get(name: string): string | undefined
    set(name: string, value: string, options?: { expires?: number | Date; path?: string; domain?: string; secure?: boolean }): void
    remove(name: string, options?: { path?: string; domain?: string }): void
  }
  
  const Cookies: CookiesStatic
  export default Cookies
}
