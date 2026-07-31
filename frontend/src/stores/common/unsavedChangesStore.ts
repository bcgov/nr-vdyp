import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * Lets a page register an async "do I have unsaved changes" check while it is mounted,
 * so global chrome (e.g. the user menu) can query it without depending on route name
 * or reaching into page-specific stores that may hold stale state after the page unmounts.
 */
export const useUnsavedChangesStore = defineStore('unsavedChanges', () => {
  const checker = ref<(() => Promise<boolean>) | null>(null)

  const registerChecker = (fn: () => Promise<boolean>) => {
    checker.value = fn
  }

  const unregisterChecker = () => {
    checker.value = null
  }

  const hasUnsavedChanges = async (): Promise<boolean> => {
    return checker.value ? await checker.value() : false
  }

  return {
    registerChecker,
    unregisterChecker,
    hasUnsavedChanges,
  }
})
