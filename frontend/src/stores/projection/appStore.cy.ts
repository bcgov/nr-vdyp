/// <reference types="cypress" />

import { setActivePinia, createPinia } from 'pinia'
import { useAppStore } from '@/stores/projection/appStore'
import { DEFAULTS } from '@/constants'
import { PROJECTION_VIEW_MODE, PROJECTION_STATUS } from '@/constants/constants'

describe('App Store Unit Tests', () => {
  let appStore: ReturnType<typeof useAppStore>

  beforeEach(() => {
    setActivePinia(createPinia())
    appStore = useAppStore()
  })

  it('should initialize with correct default state', () => {
    expect(appStore.modelSelection).to.equal(DEFAULTS.DEFAULT_VALUES.METHOD_SELECTION)
    expect(appStore.viewMode).to.equal(PROJECTION_VIEW_MODE.CREATE)
    expect(appStore.currentProjectionGUID).to.be.null
    expect(appStore.currentProjectionStatus).to.equal(PROJECTION_STATUS.DRAFT)
    expect(appStore.isSavingProjection).to.be.false
    expect(appStore.duplicatedFromInfo).to.be.null
  })

  describe('isReadOnly', () => {
    it('should be true when viewMode is VIEW', () => {
      appStore.setViewMode(PROJECTION_VIEW_MODE.VIEW)
      expect(appStore.isReadOnly).to.be.true
    })

    it('should be false when viewMode is EDIT', () => {
      appStore.setViewMode(PROJECTION_VIEW_MODE.EDIT)
      expect(appStore.isReadOnly).to.be.false
    })
  })

  describe('isDraft', () => {
    it('should be true when status is DRAFT and false otherwise', () => {
      expect(appStore.isDraft).to.be.true
      appStore.setCurrentProjectionStatus(PROJECTION_STATUS.READY)
      expect(appStore.isDraft).to.be.false
    })
  })

  describe('Setters', () => {
    it('setModelSelection should update modelSelection', () => {
      appStore.setModelSelection('Manual Input')
      expect(appStore.modelSelection).to.equal('Manual Input')
    })

    it('setViewMode should update viewMode', () => {
      appStore.setViewMode(PROJECTION_VIEW_MODE.EDIT)
      expect(appStore.viewMode).to.equal(PROJECTION_VIEW_MODE.EDIT)
      appStore.setViewMode(PROJECTION_VIEW_MODE.VIEW)
      expect(appStore.viewMode).to.equal(PROJECTION_VIEW_MODE.VIEW)
    })

    it('setCurrentProjectionGUID should set and clear GUID', () => {
      appStore.setCurrentProjectionGUID('abc-123-def')
      expect(appStore.currentProjectionGUID).to.equal('abc-123-def')
      appStore.setCurrentProjectionGUID(null)
      expect(appStore.currentProjectionGUID).to.be.null
    })

    it('setCurrentProjectionStatus should update status', () => {
      appStore.setCurrentProjectionStatus(PROJECTION_STATUS.RUNNING)
      expect(appStore.currentProjectionStatus).to.equal(PROJECTION_STATUS.RUNNING)
    })

    it('setDuplicatedFromInfo should set and clear info', () => {
      const info = { originalName: 'My Projection', duplicatedAt: '2024-01-15T10:00:00' }
      appStore.setDuplicatedFromInfo(info)
      expect(appStore.duplicatedFromInfo).to.deep.equal(info)
      appStore.setDuplicatedFromInfo(null)
      expect(appStore.duplicatedFromInfo).to.be.null
    })
  })

  describe('resetForNewProjection', () => {
    it('should reset all fields to initial state', () => {
      appStore.setViewMode(PROJECTION_VIEW_MODE.VIEW)
      appStore.setCurrentProjectionGUID('guid-xyz')
      appStore.setCurrentProjectionStatus(PROJECTION_STATUS.FAILED)
      appStore.isSavingProjection = true
      appStore.setDuplicatedFromInfo({ originalName: 'My Projection', duplicatedAt: '2024-01-15T10:00:00' })

      appStore.resetForNewProjection()

      expect(appStore.viewMode).to.equal(PROJECTION_VIEW_MODE.CREATE)
      expect(appStore.currentProjectionGUID).to.be.null
      expect(appStore.currentProjectionStatus).to.equal(PROJECTION_STATUS.DRAFT)
      expect(appStore.isSavingProjection).to.be.false
      expect(appStore.duplicatedFromInfo).to.be.null
    })
  })
})
