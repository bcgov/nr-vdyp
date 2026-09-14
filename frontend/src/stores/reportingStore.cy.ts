/// <reference types="cypress" />

import { assert } from 'chai'
import { setActivePinia, createPinia } from 'pinia'
import { useReportingStore } from '@/stores/reportingStore'

describe('Reporting Store Unit Tests', () => {
  let reportingStore: ReturnType<typeof useReportingStore>

  beforeEach(() => {
    setActivePinia(createPinia())
    reportingStore = useReportingStore()
  })

  it('should initialize with default state', () => {
    assert.isFalse(reportingStore.modelParamReportingTabsEnabled)
    assert.isFalse(reportingStore.fileUploadReportingTabsEnabled)
  })

  it('should enable modelParamReportingTabs', () => {
    reportingStore.modelParamEnableTabs()
    assert.isTrue(reportingStore.modelParamReportingTabsEnabled)
  })

  it('should disable modelParamReportingTabs', () => {
    reportingStore.modelParamEnableTabs() // First enable
    reportingStore.modelParamDisableTabs()
    assert.isFalse(reportingStore.modelParamReportingTabsEnabled)
  })

  it('should enable fileUploadReportingTabs', () => {
    reportingStore.fileUploadEnableTabs()
    assert.isTrue(reportingStore.fileUploadReportingTabsEnabled)
  })

  it('should disable fileUploadReportingTabs', () => {
    reportingStore.fileUploadEnableTabs() // First enable
    reportingStore.fileUploadDisableTabs()
    assert.isFalse(reportingStore.fileUploadReportingTabsEnabled)
  })
})
