/// <reference types="cypress" />

import {
  saveExistingProjectionSession,
  saveNewProjectionSession,
  loadProjectionSession,
  clearProjectionSession,
} from '@/utils/projectionSession'
import { CONSTANTS } from '@/constants'

const PROJ_CTX_KEY = btoa(CONSTANTS.PROJECTION_SESSION_CTX.KEY)

describe('projectionSession Unit Tests', () => {
  beforeEach(() => {
    sessionStorage.clear()
  })

  describe('saveExistingProjectionSession', () => {
    it('should save existing projection context to sessionStorage', () => {
      saveExistingProjectionSession('guid-123', CONSTANTS.PROJECTION_VIEW_MODE.VIEW)

      const raw = sessionStorage.getItem(PROJ_CTX_KEY)
      expect(raw).to.not.be.null

      const ctx = JSON.parse(atob(raw!))
      expect(ctx).to.deep.equal({ type: CONSTANTS.PROJECTION_SESSION_CTX.EXISTING_TYPE, g: 'guid-123', m: CONSTANTS.PROJECTION_VIEW_MODE.VIEW })
    })

    it('should overwrite previous session when called again', () => {
      saveExistingProjectionSession('guid-aaa', CONSTANTS.PROJECTION_VIEW_MODE.VIEW)
      saveExistingProjectionSession('guid-bbb', CONSTANTS.PROJECTION_VIEW_MODE.EDIT)

      const raw = sessionStorage.getItem(PROJ_CTX_KEY)
      const ctx = JSON.parse(atob(raw!))
      expect(ctx).to.deep.equal({ type: CONSTANTS.PROJECTION_SESSION_CTX.EXISTING_TYPE, g: 'guid-bbb', m: CONSTANTS.PROJECTION_VIEW_MODE.EDIT })
    })

    it('should store value as base64-encoded JSON', () => {
      saveExistingProjectionSession('test-guid', CONSTANTS.PROJECTION_VIEW_MODE.EDIT)

      const raw = sessionStorage.getItem(PROJ_CTX_KEY)
      // raw value should be valid base64
      expect(() => atob(raw!)).to.not.throw()
      // decoded value should be valid JSON
      expect(() => JSON.parse(atob(raw!))).to.not.throw()
    })
  })

  describe('saveNewProjectionSession', () => {
    it('should save new projection context to sessionStorage', () => {
      saveNewProjectionSession(CONSTANTS.MODEL_SELECTION.FILE_UPLOAD)

      const raw = sessionStorage.getItem(PROJ_CTX_KEY)
      expect(raw).to.not.be.null

      const ctx = JSON.parse(atob(raw!))
      expect(ctx).to.deep.equal({ type: CONSTANTS.PROJECTION_SESSION_CTX.NEW_TYPE, ms: CONSTANTS.MODEL_SELECTION.FILE_UPLOAD })
    })

    it('should overwrite existing session when called after saveExistingProjectionSession', () => {
      saveExistingProjectionSession('guid-123', CONSTANTS.PROJECTION_VIEW_MODE.VIEW)
      saveNewProjectionSession(CONSTANTS.MODEL_SELECTION.FILE_UPLOAD)

      const raw = sessionStorage.getItem(PROJ_CTX_KEY)
      const ctx = JSON.parse(atob(raw!))
      expect(ctx).to.deep.equal({ type: CONSTANTS.PROJECTION_SESSION_CTX.NEW_TYPE, ms: CONSTANTS.MODEL_SELECTION.FILE_UPLOAD })
    })

    it('should store value as base64-encoded JSON', () => {
      saveNewProjectionSession(CONSTANTS.MODEL_SELECTION.INPUT_MODEL_PARAMETERS)

      const raw = sessionStorage.getItem(PROJ_CTX_KEY)
      expect(() => atob(raw!)).to.not.throw()
      expect(() => JSON.parse(atob(raw!))).to.not.throw()
    })
  })

  describe('loadProjectionSession', () => {
    it('should return null when sessionStorage is empty', () => {
      expect(loadProjectionSession()).to.be.null
    })

    it('should load and return existing projection context', () => {
      saveExistingProjectionSession('guid-abc', CONSTANTS.PROJECTION_VIEW_MODE.VIEW)

      const ctx = loadProjectionSession()
      expect(ctx).to.deep.equal({ type: CONSTANTS.PROJECTION_SESSION_CTX.EXISTING_TYPE, g: 'guid-abc', m: CONSTANTS.PROJECTION_VIEW_MODE.VIEW })
    })

    it('should load and return new projection context', () => {
      saveNewProjectionSession(CONSTANTS.MODEL_SELECTION.FILE_UPLOAD)

      const ctx = loadProjectionSession()
      expect(ctx).to.deep.equal({ type: CONSTANTS.PROJECTION_SESSION_CTX.NEW_TYPE, ms: CONSTANTS.MODEL_SELECTION.FILE_UPLOAD })
    })

    it('should return null when sessionStorage contains invalid base64', () => {
      sessionStorage.setItem(PROJ_CTX_KEY, '!!!not-base64!!!')

      expect(loadProjectionSession()).to.be.null
    })

    it('should return null when sessionStorage contains invalid JSON after decoding', () => {
      sessionStorage.setItem(PROJ_CTX_KEY, btoa('not-valid-json'))

      expect(loadProjectionSession()).to.be.null
    })

    it('should return null when new projection context has an invalid model selection', () => {
      const tampered = { type: CONSTANTS.PROJECTION_SESSION_CTX.NEW_TYPE, ms: 'INVALID_MODEL' }
      sessionStorage.setItem(PROJ_CTX_KEY, btoa(JSON.stringify(tampered)))

      expect(loadProjectionSession()).to.be.null
    })

    it('should return null when existing projection context has an invalid view mode', () => {
      // 'create' is intentionally excluded from valid existing view modes
      const tampered = { type: CONSTANTS.PROJECTION_SESSION_CTX.EXISTING_TYPE, g: 'guid-xyz', m: CONSTANTS.PROJECTION_VIEW_MODE.CREATE }
      sessionStorage.setItem(PROJ_CTX_KEY, btoa(JSON.stringify(tampered)))

      expect(loadProjectionSession()).to.be.null
    })

    it('should return null when existing projection context has an empty GUID', () => {
      const tampered = { type: CONSTANTS.PROJECTION_SESSION_CTX.EXISTING_TYPE, g: '   ', m: CONSTANTS.PROJECTION_VIEW_MODE.VIEW }
      sessionStorage.setItem(PROJ_CTX_KEY, btoa(JSON.stringify(tampered)))

      expect(loadProjectionSession()).to.be.null
    })

    it('should return null when type field is unknown', () => {
      const tampered = { type: 'unknown-type', g: 'guid-xyz', m: CONSTANTS.PROJECTION_VIEW_MODE.VIEW }
      sessionStorage.setItem(PROJ_CTX_KEY, btoa(JSON.stringify(tampered)))

      expect(loadProjectionSession()).to.be.null
    })

    it('should return null when type field is missing', () => {
      const tampered = { g: 'guid-xyz', m: CONSTANTS.PROJECTION_VIEW_MODE.VIEW }
      sessionStorage.setItem(PROJ_CTX_KEY, btoa(JSON.stringify(tampered)))

      expect(loadProjectionSession()).to.be.null
    })
  })

  describe('clearProjectionSession', () => {
    it('should remove projection context from sessionStorage', () => {
      saveExistingProjectionSession('guid-123', CONSTANTS.PROJECTION_VIEW_MODE.VIEW)
      expect(sessionStorage.getItem(PROJ_CTX_KEY)).to.not.be.null

      clearProjectionSession()
      expect(sessionStorage.getItem(PROJ_CTX_KEY)).to.be.null
    })

    it('should be a no-op when sessionStorage is already empty', () => {
      expect(() => clearProjectionSession()).to.not.throw()
      expect(sessionStorage.getItem(PROJ_CTX_KEY)).to.be.null
    })

    it('should cause loadProjectionSession to return null after clearing', () => {
      saveNewProjectionSession(CONSTANTS.MODEL_SELECTION.FILE_UPLOAD)
      clearProjectionSession()

      expect(loadProjectionSession()).to.be.null
    })
  })
})
