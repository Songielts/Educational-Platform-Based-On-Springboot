declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}

declare module 'vue-router' {
  export interface RouteRecordRaw {
    path: string
    name?: string
    component?: any
    redirect?: string | { name: string } | ((to: any) => string | { name: string })
    children?: RouteRecordRaw[]
    meta?: Record<string, any>
    alias?: string | string[]
  }
  
  export interface Router {
    currentRoute: RouteLocationNormalizedLoaded
    push: (to: string | { name?: string; path?: string; params?: Record<string, any>; query?: Record<string, any> }) => Promise<any>
    replace: (to: string | { name?: string; path?: string; params?: Record<string, any>; query?: Record<string, any> }) => Promise<any>
    go: (delta: number) => void
    back: () => void
    forward: () => void
    beforeEach: (guard: NavigationGuard) => void
    afterEach: (guard: (to: RouteLocationNormalizedLoaded, from: RouteLocationNormalizedLoaded) => void) => void
  }
  
  export interface RouteLocationNormalizedLoaded {
    path: string
    name?: string
    params: Record<string, string>
    query: Record<string, string>
    meta?: Record<string, any>
    fullPath: string
    hash: string
  }
  
  export type NavigationGuard = (to: RouteLocationNormalizedLoaded, from: RouteLocationNormalizedLoaded, next: (to?: string | false | void) => void) => void | Promise<void>
  
  export function createRouter(options: {
    history: any
    routes: RouteRecordRaw[]
  }): Router
  
  export function createWebHistory(base?: string): any
  export function createWebHashHistory(base?: string): any
  export function createMemoryHistory(base?: string): any
  
  export function useRoute(): RouteLocationNormalizedLoaded
  export function useRouter(): Router
  
  export function onBeforeRouteLeave(guard: NavigationGuard): void
  export function onBeforeRouteUpdate(guard: NavigationGuard): void
}
