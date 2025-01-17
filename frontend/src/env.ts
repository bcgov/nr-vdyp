declare global {
  interface Window {
    config: any
  }
}

export const env: Record<string, any> = { ...import.meta.env, ...window.config }
