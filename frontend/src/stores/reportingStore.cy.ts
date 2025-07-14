/// <reference types="cypress" />

import { setActivePinia, createPinia } from 'pinia'
import { useReportingStore } from '@/stores/reportingStore'

describe('Reporting Store Unit Tests', () => {
  let reportingStore: ReturnType<typeof useReportingStore>

  beforeEach(() => {
    setActivePinia(createPinia())
    reportingStore = useReportingStore()
  })

  it('should initialize with default state', () => {
    expect(reportingStore.modelParamReportingTabsEnabled).to.be.false
    expect(reportingStore.fileUploadReportingTabsEnabled).to.be.false
  })

  it('should enable modelParamReportingTabs', () => {
    reportingStore.modelParamEnableTabs()
    expect(reportingStore.modelParamReportingTabsEnabled).to.be.true
  })

  it('should disable modelParamReportingTabs', () => {
    reportingStore.modelParamEnableTabs() // First enable
    reportingStore.modelParamDisableTabs()
    expect(reportingStore.modelParamReportingTabsEnabled).to.be.false
  })

  it('should enable fileUploadReportingTabs', () => {
    reportingStore.fileUploadEnableTabs()
    expect(reportingStore.fileUploadReportingTabsEnabled).to.be.true
  })

  it('should disable fileUploadReportingTabs', () => {
    reportingStore.fileUploadEnableTabs() // First enable
    reportingStore.fileUploadDisableTabs()
    expect(reportingStore.fileUploadReportingTabsEnabled).to.be.false
  })
})
