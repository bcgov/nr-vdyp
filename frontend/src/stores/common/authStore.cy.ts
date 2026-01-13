/// <reference types="cypress" />

import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '@/stores/common/authStore'

describe('Auth Store Unit Tests', () => {
  let authStore: ReturnType<typeof useAuthStore>

  beforeEach(() => {
    setActivePinia(createPinia())
    authStore = useAuthStore()
  })

  it('should set user and store in sessionStorage', () => {
    const user = {
      accessToken: 'access_token',
      refToken: 'refresh_token',
      idToken: 'id_token',
    }

    authStore.setUser(user)

    expect(authStore.user).to.deep.equal(user)
    expect(authStore.authenticated).to.be.true

    cy.wrap(sessionStorage.getItem('authUser')).should(
      'equal',
      JSON.stringify(user),
    )
  })

  it('should clear user and remove from sessionStorage', () => {
    authStore.clearUser()

    expect(authStore.user).to.be.null
    expect(authStore.authenticated).to.be.false

    cy.wrap(sessionStorage.getItem('authUser')).should('be.null')
  })

  it('should load user from sessionStorage', () => {
    const user = {
      accessToken: 'access_token',
      refToken: 'refresh_token',
      idToken: 'id_token',
    }
    sessionStorage.setItem('authUser', JSON.stringify(user))

    authStore.loadUserFromStorage()

    expect(authStore.user).to.deep.equal(user)
    expect(authStore.authenticated).to.be.true
  })

  it('should not load user if sessionStorage has invalid data', () => {
    sessionStorage.setItem('authUser', 'invalid json')

    cy.spy(console, 'error').as('consoleError')

    authStore.loadUserFromStorage()

    expect(authStore.user).to.be.null
    expect(authStore.authenticated).to.be.false
    cy.get('@consoleError').should(
      'have.been.calledWithMatch',
      'Failed to parse user from sessionStorage',
    )
  })

  it('should parse a valid ID token', () => {
    const mockToken = JSON.stringify({
      name: 'John Doe',
      exp: 123456,
      client_roles: ['admin'],
    })
    const base64Token = btoa(mockToken)
    const user = {
      accessToken: '',
      refToken: '',
      idToken: `mock.${base64Token}.token`,
    }
    authStore.setUser(user)

    const parsed = authStore.getParsedIdToken()

    expect(parsed?.name).to.equal('John Doe')
    expect(parsed?.exp).to.equal(123456)
    expect(parsed?.client_roles).to.include('admin')
  })

  it('should parse a valid Access token', () => {
    const mockToken = JSON.stringify({
      iss: 'https://example.com',
      sub: 'user123',
      exp: 123456,
    })
    const base64Token = btoa(mockToken)
    const user = {
      accessToken: `mock.${base64Token}.token`,
      refToken: '',
      idToken: '',
    }
    authStore.setUser(user)

    const parsed = authStore.getParsedAccessToken()

    expect(parsed?.iss).to.equal('https://example.com')
    expect(parsed?.sub).to.equal('user123')
    expect(parsed?.exp).to.equal(123456)
  })

  it('should parse a valid Refresh token', () => {
    const mockToken = JSON.stringify({
      iss: 'https://example.com',
      sub: 'user123',
      exp: 123456,
    })
    const base64Token = btoa(mockToken)
    const user = {
      accessToken: '',
      refToken: `mock.${base64Token}.token`,
      idToken: '',
    }
    authStore.setUser(user)

    const parsed = authStore.getParsedRefreshToken()

    expect(parsed?.iss).to.equal('https://example.com')
    expect(parsed?.sub).to.equal('user123')
    expect(parsed?.exp).to.equal(123456)
  })

  it('should return an empty roles array when ID token is not set', () => {
    authStore.user = null
    const roles = authStore.getAllRoles()
    expect(roles).to.be.an('array').that.is.empty
  })

  it('should verify user has a specific role', () => {
    // Set user with a valid ID token containing roles
    const mockToken = JSON.stringify({
      client_roles: ['admin', 'user'],
    })
    const base64Token = btoa(mockToken)
    authStore.setUser({
      accessToken: '',
      refToken: '',
      idToken: `mock.${base64Token}.token`,
    })

    const hasAdminRole = authStore.hasRole('admin')
    const hasGuestRole = authStore.hasRole('guest')

    expect(hasAdminRole).to.be.true
    expect(hasGuestRole).to.be.false
  })

  it('should logout the user', () => {
    const logoutStub = cy.stub().as('logoutStub').resolves()
    authStore.logout(logoutStub)

    expect(authStore.user).to.be.null
    expect(authStore.authenticated).to.be.false

    cy.get('@logoutStub').should('have.been.calledOnce')
  })

  it('should handle invalid tokens (Access, Refresh, ID)', () => {
    const malformedTokens = [
      { type: 'access', token: 'invalid.token' },
      { type: 'refresh', token: 'invalid.token' },
      { type: 'id', token: 'invalid.token' },
    ]

    cy.spy(console, 'error').as('consoleError')

    malformedTokens.forEach(({ type, token }) => {
      const user = {
        accessToken: type === 'access' ? token : '',
        refToken: type === 'refresh' ? token : '',
        idToken: type === 'id' ? token : '',
      }
      authStore.setUser(user)

      let parsed
      if (type === 'access') {
        parsed = authStore.getParsedAccessToken()
      } else if (type === 'refresh') {
        parsed = authStore.getParsedRefreshToken()
      } else {
        parsed = authStore.getParsedIdToken()
      }

      expect(parsed).to.be.null
    })

    cy.get('@consoleError')
      .should('have.been.calledWithMatch', 'Invalid token format')
      .and('have.callCount', 3) // Expect 3 calls for 3 tokens
  })

  it('should handle malformed tokens with varying formats', () => {
    const malformedTokens = [
      'token.without.dots',
      'token.with.one.dot',
      'token.with.three.dots.but.invalid.base64',
    ]

    cy.spy(console, 'error').as('consoleError')

    malformedTokens.forEach((token) => {
      const user = {
        accessToken: token,
        refToken: token,
        idToken: token,
      }
      authStore.setUser(user)
      const parsedAccess = authStore.getParsedAccessToken()
      const parsedRefresh = authStore.getParsedRefreshToken()
      const parsedId = authStore.getParsedIdToken()
      expect(parsedAccess).to.be.null
      expect(parsedRefresh).to.be.null
      expect(parsedId).to.be.null
    })

    cy.get('@consoleError')
      .should('have.been.calledWithMatch', 'Invalid token format')
      .and('have.callCount', 9) // Expect 9 calls (3 tokens x 3 types)
  })

  it('should not load user if sessionStorage is empty', () => {
    sessionStorage.removeItem('authUser')
    authStore.loadUserFromStorage()

    expect(authStore.user).to.be.null
    expect(authStore.authenticated).to.be.false
  })

  it('should update user correctly', () => {
    const initialUser = {
      accessToken: 'initial_access',
      refToken: 'initial_refresh',
      idToken: 'initial_id',
    }
    authStore.setUser(initialUser)

    const updatedUser = {
      accessToken: 'updated_access',
      refToken: 'updated_refresh',
      idToken: 'updated_id',
    }
    authStore.setUser(updatedUser)

    expect(authStore.user).to.deep.equal(updatedUser)
    expect(authStore.authenticated).to.be.true
    cy.wrap(sessionStorage.getItem('authUser')).should(
      'equal',
      JSON.stringify(updatedUser),
    )
  })

  it('should correctly reflect computed properties', () => {
    expect(authStore.getAuthenticated).to.be.false
    expect(authStore.getUser).to.be.null

    const user = {
      accessToken: 'access_token',
      refToken: 'refresh_token',
      idToken: 'id_token',
    }
    authStore.setUser(user)

    expect(authStore.getAuthenticated).to.be.true
    expect(authStore.getUser).to.deep.equal(user)
  })
})
