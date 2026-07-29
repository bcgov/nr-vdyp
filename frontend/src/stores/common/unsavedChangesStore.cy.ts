/// <reference types="cypress" />

import { setActivePinia, createPinia } from 'pinia'
import { useUnsavedChangesStore } from '@/stores/common/unsavedChangesStore'

describe('Unsaved Changes Store Unit Tests', () => {
  let unsavedChangesStore: ReturnType<typeof useUnsavedChangesStore>

  beforeEach(() => {
    setActivePinia(createPinia())
    unsavedChangesStore = useUnsavedChangesStore()
  })

  it('should return false when no checker is registered', async () => {
    const result = await unsavedChangesStore.hasUnsavedChanges()
    expect(result).to.equal(false)
  })

  it('should return the registered checker result', async () => {
    unsavedChangesStore.registerChecker(async () => true)
    const result = await unsavedChangesStore.hasUnsavedChanges()
    expect(result).to.equal(true)
  })

  it('should return false again after unregistering', async () => {
    unsavedChangesStore.registerChecker(async () => true)
    unsavedChangesStore.unregisterChecker()
    const result = await unsavedChangesStore.hasUnsavedChanges()
    expect(result).to.equal(false)
  })
})
