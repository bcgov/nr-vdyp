/// <reference types="cypress" />

import {
  saveExistingProjectionSession,
  saveNewProjectionSession,
  loadProjectionSession,
  clearProjectionSession,
} from '@/utils/projectionSession'
import { CONSTANTS } from '@/constants'

const PROJ_CTX_KEY = btoa('vdyp-projection-ctx')

describe('projectionSession Unit Tests', () => {
  beforeEach(() => {
    sessionStorage.clear()
  })

  describe('saveExistingProjectionSession', () => {
    it('should save existing projection context to sessionStorage', () => {
      saveExistingProjectionSession('guid-123', 'view')

      const raw = sessionStorage.getItem(PROJ_CTX_KEY)
      expect(raw).to.not.be.null

      const ctx = JSON.parse(atob(raw!))
      expect(ctx).to.deep.equal({ type: CONSTANTS.PROJECTION_SESSION_CTX.EXISTING_TYPE, g: 'guid-123', m: 'view' })
    })

    it('should overwrite previous session when called again', () => {
      saveExistingProjectionSession('guid-aaa', 'view')
      saveExistingProjectionSession('guid-bbb', 'edit')

      const raw = sessionStorage.getItem(PROJ_CTX_KEY)
      const ctx = JSON.parse(atob(raw!))
      expect(ctx).to.deep.equal({ type: CONSTANTS.PROJECTION_SESSION_CTX.EXISTING_TYPE, g: 'guid-bbb', m: 'edit' })
    })

    it('should store value as base64-encoded JSON', () => {
      saveExistingProjectionSession('test-guid', 'edit')

      const raw = sessionStorage.getItem(PROJ_CTX_KEY)
      // raw value should be valid base64
      expect(() => atob(raw!)).to.not.throw()
      // decoded value should be valid JSON
      expect(() => JSON.parse(atob(raw!))).to.not.throw()
    })
  })

  describe('saveNewProjectionSession', () => {
    it('should save new projection context to sessionStorage', () => {
      saveNewProjectionSession('VDYP8')

      const raw = sessionStorage.getItem(PROJ_CTX_KEY)
      expect(raw).to.not.be.null

      const ctx = JSON.parse(atob(raw!))
      expect(ctx).to.deep.equal({ type: CONSTANTS.PROJECTION_SESSION_CTX.NEW_TYPE, ms: 'VDYP8' })
    })

    it('should overwrite existing session when called after saveExistingProjectionSession', () => {
      saveExistingProjectionSession('guid-123', 'view')
      saveNewProjectionSession('VDYP8')

      const raw = sessionStorage.getItem(PROJ_CTX_KEY)
      const ctx = JSON.parse(atob(raw!))
      expect(ctx).to.deep.equal({ type: CONSTANTS.PROJECTION_SESSION_CTX.NEW_TYPE, ms: 'VDYP8' })
    })

    it('should store value as base64-encoded JSON', () => {
      saveNewProjectionSession('FORWARD')

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
      saveExistingProjectionSession('guid-abc', 'view')

      const ctx = loadProjectionSession()
      expect(ctx).to.deep.equal({ type: CONSTANTS.PROJECTION_SESSION_CTX.EXISTING_TYPE, g: 'guid-abc', m: 'view' })
    })

    it('should load and return new projection context', () => {
      saveNewProjectionSession('VDYP8')

      const ctx = loadProjectionSession()
      expect(ctx).to.deep.equal({ type: CONSTANTS.PROJECTION_SESSION_CTX.NEW_TYPE, ms: 'VDYP8' })
    })

    it('should return null when sessionStorage contains invalid base64', () => {
      sessionStorage.setItem(PROJ_CTX_KEY, '!!!not-base64!!!')

      expect(loadProjectionSession()).to.be.null
    })

    it('should return null when sessionStorage contains invalid JSON after decoding', () => {
      sessionStorage.setItem(PROJ_CTX_KEY, btoa('not-valid-json'))

      expect(loadProjectionSession()).to.be.null
    })
  })

  describe('clearProjectionSession', () => {
    it('should remove projection context from sessionStorage', () => {
      saveExistingProjectionSession('guid-123', 'view')
      expect(sessionStorage.getItem(PROJ_CTX_KEY)).to.not.be.null

      clearProjectionSession()
      expect(sessionStorage.getItem(PROJ_CTX_KEY)).to.be.null
    })

    it('should be a no-op when sessionStorage is already empty', () => {
      expect(() => clearProjectionSession()).to.not.throw()
      expect(sessionStorage.getItem(PROJ_CTX_KEY)).to.be.null
    })

    it('should cause loadProjectionSession to return null after clearing', () => {
      saveNewProjectionSession('VDYP8')
      clearProjectionSession()

      expect(loadProjectionSession()).to.be.null
    })
  })
})
