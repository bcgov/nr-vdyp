/// <reference types="cypress" />

import { saveAuthSession, loadAuthSession, clearAuthSession } from '@/utils/authSession'
import { CONSTANTS } from '@/constants'

const AUTH_SESSION_KEY = btoa(CONSTANTS.AUTH_SESSION.KEY)

const VALID_USER = {
  accessToken: 'header.payload.signature',
  refToken: 'header.payload.signature',
  idToken: 'header.payload.signature',
}

describe('authSession Unit Tests', () => {
  beforeEach(() => {
    sessionStorage.clear()
  })

  describe('saveAuthSession', () => {
    it('should save user to sessionStorage with base64-encoded key and value', () => {
      saveAuthSession(VALID_USER)

      const raw = sessionStorage.getItem(AUTH_SESSION_KEY) ?? ''
      expect(raw).to.not.equal('')
      expect(JSON.parse(atob(raw))).to.deep.equal(VALID_USER)
    })

    it('should overwrite previous session when called again', () => {
      saveAuthSession(VALID_USER)
      const updated = { ...VALID_USER, accessToken: 'new.token.value' }
      saveAuthSession(updated)

      const raw = sessionStorage.getItem(AUTH_SESSION_KEY) ?? ''
      expect(JSON.parse(atob(raw))).to.deep.equal(updated)
    })
  })

  describe('loadAuthSession', () => {
    it('should return null when sessionStorage is empty', () => {
      expect(loadAuthSession()).to.be.null
    })

    it('should load and return a valid user', () => {
      saveAuthSession(VALID_USER)
      expect(loadAuthSession()).to.deep.equal(VALID_USER)
    })

    it('should return null when sessionStorage contains invalid base64', () => {
      sessionStorage.setItem(AUTH_SESSION_KEY, '!!!not-base64!!!')
      expect(loadAuthSession()).to.be.null
    })

    it('should return null when a required token field is missing', () => {
      const incomplete = { accessToken: 'a.b.c', refToken: 'a.b.c' } // idToken missing
      sessionStorage.setItem(AUTH_SESSION_KEY, btoa(JSON.stringify(incomplete)))
      expect(loadAuthSession()).to.be.null
    })

    it('should return null when a token field is an empty string', () => {
      const tampered = { ...VALID_USER, accessToken: '   ' }
      sessionStorage.setItem(AUTH_SESSION_KEY, btoa(JSON.stringify(tampered)))
      expect(loadAuthSession()).to.be.null
    })
  })

  describe('clearAuthSession', () => {
    it('should remove user session from sessionStorage', () => {
      saveAuthSession(VALID_USER)
      clearAuthSession()
      expect(sessionStorage.getItem(AUTH_SESSION_KEY)).to.be.null
    })

    it('should cause loadAuthSession to return null after clearing', () => {
      saveAuthSession(VALID_USER)
      clearAuthSession()
      expect(loadAuthSession()).to.be.null
    })
  })
})
