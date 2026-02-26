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

  describe('Initial State', () => {
    it('should initialize modelSelection with the default value', () => {
      expect(appStore.modelSelection).to.equal(DEFAULTS.DEFAULT_VALUES.MODEL_SELECTION)
    })

    it('should initialize viewMode as CREATE', () => {
      expect(appStore.viewMode).to.equal(PROJECTION_VIEW_MODE.CREATE)
    })

    it('should initialize currentProjectionGUID as null', () => {
      expect(appStore.currentProjectionGUID).to.be.null
    })

    it('should initialize currentProjectionStatus as DRAFT', () => {
      expect(appStore.currentProjectionStatus).to.equal(PROJECTION_STATUS.DRAFT)
    })

    it('should initialize isSavingProjection as false', () => {
      expect(appStore.isSavingProjection).to.be.false
    })

    it('should initialize duplicatedFromInfo as null', () => {
      expect(appStore.duplicatedFromInfo).to.be.null
    })
  })

  describe('Getters', () => {
    it('getModelSelection should return current modelSelection', () => {
      expect(appStore.getModelSelection).to.equal(DEFAULTS.DEFAULT_VALUES.MODEL_SELECTION)
    })

    it('getViewMode should return current viewMode', () => {
      expect(appStore.getViewMode).to.equal(PROJECTION_VIEW_MODE.CREATE)
    })

    it('getCurrentProjectionGUID should return current GUID', () => {
      expect(appStore.getCurrentProjectionGUID).to.be.null
    })

    it('getCurrentProjectionStatus should return current status', () => {
      expect(appStore.getCurrentProjectionStatus).to.equal(PROJECTION_STATUS.DRAFT)
    })

    it('isReadOnly should be false when viewMode is not VIEW', () => {
      expect(appStore.isReadOnly).to.be.false
    })

    it('isReadOnly should be true when viewMode is VIEW', () => {
      appStore.setViewMode(PROJECTION_VIEW_MODE.VIEW)
      expect(appStore.isReadOnly).to.be.true
    })

    it('isReadOnly should be false when viewMode is EDIT', () => {
      appStore.setViewMode(PROJECTION_VIEW_MODE.EDIT)
      expect(appStore.isReadOnly).to.be.false
    })

    it('isDraft should be true when status is DRAFT', () => {
      expect(appStore.isDraft).to.be.true
    })

    it('isDraft should be false when status is not DRAFT', () => {
      appStore.setCurrentProjectionStatus(PROJECTION_STATUS.READY)
      expect(appStore.isDraft).to.be.false
    })
  })

  describe('setModelSelection', () => {
    it('should update modelSelection', () => {
      appStore.setModelSelection('Manual Input')
      expect(appStore.modelSelection).to.equal('Manual Input')
    })

    it('getModelSelection getter should reflect updated value', () => {
      appStore.setModelSelection('File Upload')
      expect(appStore.getModelSelection).to.equal('File Upload')
    })
  })

  describe('setViewMode', () => {
    it('should set viewMode to EDIT', () => {
      appStore.setViewMode(PROJECTION_VIEW_MODE.EDIT)
      expect(appStore.viewMode).to.equal(PROJECTION_VIEW_MODE.EDIT)
    })

    it('should set viewMode to VIEW', () => {
      appStore.setViewMode(PROJECTION_VIEW_MODE.VIEW)
      expect(appStore.viewMode).to.equal(PROJECTION_VIEW_MODE.VIEW)
    })

    it('should set viewMode to CREATE', () => {
      appStore.setViewMode(PROJECTION_VIEW_MODE.EDIT)
      appStore.setViewMode(PROJECTION_VIEW_MODE.CREATE)
      expect(appStore.viewMode).to.equal(PROJECTION_VIEW_MODE.CREATE)
    })
  })

  describe('setCurrentProjectionGUID', () => {
    it('should set GUID to a string value', () => {
      const testGuid = 'abc-123-def'
      appStore.setCurrentProjectionGUID(testGuid)
      expect(appStore.currentProjectionGUID).to.equal(testGuid)
    })

    it('should set GUID back to null', () => {
      appStore.setCurrentProjectionGUID('some-guid')
      appStore.setCurrentProjectionGUID(null)
      expect(appStore.currentProjectionGUID).to.be.null
    })

    it('getCurrentProjectionGUID getter should reflect updated value', () => {
      const testGuid = 'test-guid-456'
      appStore.setCurrentProjectionGUID(testGuid)
      expect(appStore.getCurrentProjectionGUID).to.equal(testGuid)
    })
  })

  describe('setCurrentProjectionStatus', () => {
    it('should set status to READY', () => {
      appStore.setCurrentProjectionStatus(PROJECTION_STATUS.READY)
      expect(appStore.currentProjectionStatus).to.equal(PROJECTION_STATUS.READY)
    })

    it('should set status to RUNNING', () => {
      appStore.setCurrentProjectionStatus(PROJECTION_STATUS.RUNNING)
      expect(appStore.currentProjectionStatus).to.equal(PROJECTION_STATUS.RUNNING)
    })

    it('should set status to FAILED', () => {
      appStore.setCurrentProjectionStatus(PROJECTION_STATUS.FAILED)
      expect(appStore.currentProjectionStatus).to.equal(PROJECTION_STATUS.FAILED)
    })

    it('getCurrentProjectionStatus getter should reflect updated value', () => {
      appStore.setCurrentProjectionStatus(PROJECTION_STATUS.RUNNING)
      expect(appStore.getCurrentProjectionStatus).to.equal(PROJECTION_STATUS.RUNNING)
    })
  })

  describe('setDuplicatedFromInfo', () => {
    it('should set duplicatedFromInfo with an object', () => {
      const info = { originalName: 'My Projection', duplicatedAt: '2024-01-15T10:00:00' }
      appStore.setDuplicatedFromInfo(info)
      expect(appStore.duplicatedFromInfo).to.deep.equal(info)
    })

    it('should clear duplicatedFromInfo when set to null', () => {
      appStore.setDuplicatedFromInfo({ originalName: 'My Projection', duplicatedAt: '2024-01-15T10:00:00' })
      appStore.setDuplicatedFromInfo(null)
      expect(appStore.duplicatedFromInfo).to.be.null
    })

    it('should overwrite existing duplicatedFromInfo', () => {
      appStore.setDuplicatedFromInfo({ originalName: 'First', duplicatedAt: '2024-01-01T00:00:00' })
      const newInfo = { originalName: 'Second', duplicatedAt: '2024-06-01T00:00:00' }
      appStore.setDuplicatedFromInfo(newInfo)
      expect(appStore.duplicatedFromInfo).to.deep.equal(newInfo)
    })
  })

  describe('resetForNewProjection', () => {
    it('should reset viewMode to CREATE', () => {
      appStore.setViewMode(PROJECTION_VIEW_MODE.EDIT)
      appStore.resetForNewProjection()
      expect(appStore.viewMode).to.equal(PROJECTION_VIEW_MODE.CREATE)
    })

    it('should reset currentProjectionGUID to null', () => {
      appStore.setCurrentProjectionGUID('some-guid')
      appStore.resetForNewProjection()
      expect(appStore.currentProjectionGUID).to.be.null
    })

    it('should reset currentProjectionStatus to DRAFT', () => {
      appStore.setCurrentProjectionStatus(PROJECTION_STATUS.READY)
      appStore.resetForNewProjection()
      expect(appStore.currentProjectionStatus).to.equal(PROJECTION_STATUS.DRAFT)
    })

    it('should reset isSavingProjection to false', () => {
      appStore.isSavingProjection = true
      appStore.resetForNewProjection()
      expect(appStore.isSavingProjection).to.be.false
    })

    it('should reset duplicatedFromInfo to null', () => {
      appStore.setDuplicatedFromInfo({ originalName: 'My Projection', duplicatedAt: '2024-01-15T10:00:00' })
      appStore.resetForNewProjection()
      expect(appStore.duplicatedFromInfo).to.be.null
    })

    it('should reset all fields together', () => {
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
