import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { logout as keycloakLogout } from '@/services/keycloak'
import type { User } from '@/interfaces/interfaces'
import { saveAuthSession, loadAuthSession, clearAuthSession } from '@/utils/authSession'

export const useAuthStore = defineStore('authStore', () => {
  const authenticated = ref<boolean>(false)
  const user = ref<User | null>(null)

  const setUser = (newUser: User) => {
    user.value = newUser
    authenticated.value = true
    saveAuthSession(newUser)
  }

  const clearUser = () => {
    user.value = null
    authenticated.value = false
    clearAuthSession()
  }

  const loadUserFromStorage = () => {
    const storedUser = loadAuthSession()
    if (storedUser) {
      user.value = storedUser
      authenticated.value = true
    }
  }

  const parseToken = (
    token: string | undefined,
  ): Record<string, any> | null => {
    if (!token) return null

    try {
      const tokenParts = token.split('.')

      if (tokenParts.length !== 3) {
        console.error('Invalid token format')
        return null
      }

      const tokenParsed = JSON.parse(atob(tokenParts[1]))
      return tokenParsed
    } catch (error) {
      console.error('Failed to parse token:', error)
      return null
    }
  }

  const getParsedAccessToken = () => {
    const parsed = parseToken(user.value?.accessToken)
    if (!parsed) return null

    return {
      aud: parsed.aud ?? null,
      azp: parsed.azp ?? null,
      client_roles: parsed.client_roles ?? [],
      display_name: parsed.display_name ?? null,
      email: parsed.email ?? null,
      email_verified: parsed.email_verified ?? null,
      exp: parsed.exp ?? null,
      family_name: parsed.family_name ?? null,
      given_name: parsed.given_name ?? null,
      iat: parsed.iat ?? null,
      identity_provider: parsed.identity_provider ?? null,
      idir_user_guid: parsed.idir_user_guid ?? null,
      idir_username: parsed.idir_username ?? null,
      iss: parsed.iss ?? null,
      jti: parsed.jti ?? null,
      name: parsed.name ?? null,
      nonce: parsed.nonce ?? null,
      preferred_username: parsed.preferred_username ?? null,
      scope: parsed.scope ?? null,
      session_state: parsed.session_state ?? null,
      sid: parsed.sid ?? null,
      sub: parsed.sub ?? null,
      typ: parsed.typ ?? null,
      user_principal_name: parsed.user_principal_name ?? null,
    }
  }

  const getParsedRefreshToken = () => {
    const parsed = parseToken(user.value?.refToken)
    if (!parsed) return null

    return {
      aud: parsed.aud ?? null,
      azp: parsed.azp ?? [],
      exp: parsed.exp ?? null,
      iat: parsed.iat ?? null,
      iss: parsed.iss ?? null,
      jti: parsed.jti ?? null,
      nonce: parsed.nonce ?? null,
      scope: parsed.scope ?? null,
      sid: parsed.sid ?? null,
      sub: parsed.sub ?? null,
      typ: parsed.typ ?? null,
    }
  }

  const getParsedIdToken = () => {
    const parsed = parseToken(user.value?.idToken)
    if (!parsed) return null

    return {
      at_hash: parsed.at_hash ?? null,
      aud: parsed.aud ?? null,
      azp: parsed.azp ?? null,
      client_roles: parsed.client_roles ?? [],
      display_name: parsed.display_name ?? null,
      email: parsed.email ?? null,
      email_verified: parsed.email_verified ?? null,
      exp: parsed.exp ?? null,
      family_name: parsed.family_name ?? null,
      given_name: parsed.given_name ?? null,
      iat: parsed.iat ?? null,
      identity_provider: parsed.identity_provider ?? null,
      idir_user_guid: parsed.idir_user_guid ?? null,
      idir_username: parsed.idir_username ?? null,
      iss: parsed.iss ?? null,
      jti: parsed.jti ?? null,
      name: parsed.name ?? null,
      nonce: parsed.nonce ?? null,
      preferred_username: parsed.preferred_username ?? null,
      session_state: parsed.session_state ?? null,
      sid: parsed.sid ?? null,
      sub: parsed.sub ?? null,
      typ: parsed.typ ?? null,
      user_principal_name: parsed.user_principal_name ?? null,
    }
  }

  const getAllRoles = () => {
    const parsedToken = getParsedIdToken()
    if (parsedToken && Array.isArray(parsedToken.client_roles)) {
      return parsedToken.client_roles
    }
    return []
  }

  const hasRole = (role: string) => {
    const roles = getAllRoles()
    return roles.includes(role)
  }

  const logout = (userLogout = keycloakLogout) => {
    clearUser()
    userLogout()
  }

  const getAuthenticated = computed(() => authenticated.value)
  const getUser = computed(() => user.value)

  return {
    authenticated,
    user,
    getAuthenticated,
    getUser,
    setUser,
    clearUser,
    loadUserFromStorage,
    getParsedAccessToken,
    getParsedRefreshToken,
    getParsedIdToken,
    getAllRoles,
    hasRole,
    logout,
  }
})
