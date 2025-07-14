/// <reference types="cypress" />

import { setActivePinia, createPinia } from 'pinia'
import { useAppStore } from '@/stores/appStore'
import { DEFAULTS } from '@/constants'
import * as CONSTANTS from '@/constants/constants'

describe('App Store Unit Tests', () => {
  let appStore: ReturnType<typeof useAppStore>

  beforeEach(() => {
    setActivePinia(createPinia())
    appStore = useAppStore()
  })

  it('should initialize with default model selection', () => {
    expect(appStore.modelSelection).to.equal(
      DEFAULTS.DEFAULT_VALUES.MODEL_SELECTION,
    )
  })

  it('should get the correct model selection', () => {
    expect(appStore.getModelSelection).to.equal(appStore.modelSelection)
  })

  it('should set the model selection correctly', () => {
    const newSelection = CONSTANTS.MODEL_SELECTION.INPUT_MODEL_PARAMETERS
    appStore.setModelSelection(newSelection)
    expect(appStore.modelSelection).to.equal(newSelection)
    expect(appStore.getModelSelection).to.equal(newSelection)
  })
})
