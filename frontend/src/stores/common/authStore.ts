import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { logout as keycloakLogout } from '@/services/keycloak'

interface User {
  accessToken: string
  refToken: string
  idToken: string
}

export const useAuthStore = defineStore('authStore', () => {
  const authenticated = ref<boolean>(false)
  const user = ref<User | null>(null)

  const setUser = (newUser: User) => {
    user.value = newUser
    authenticated.value = true
    sessionStorage.setItem('authUser', JSON.stringify(newUser))
  }

  const clearUser = () => {
    user.value = null
    authenticated.value = false
    sessionStorage.removeItem('authUser')
  }

  const loadUserFromStorage = () => {
    const storedUser = sessionStorage.getItem('authUser')
    if (storedUser) {
      try {
        const parsedUser = JSON.parse(storedUser)
        if (
          parsedUser &&
          typeof parsedUser === 'object' &&
          parsedUser.accessToken &&
          parsedUser.refToken &&
          parsedUser.idToken
        ) {
          user.value = parsedUser
          authenticated.value = true
        } else {
          console.warn('Invalid user data in sessionStorage')
          clearUser()
        }
      } catch (error) {
        console.error('Failed to parse user from sessionStorage:', error)
        clearUser()
      }
    }
  }

  const parseIdToken = () => {
    if (!user.value || !user.value.idToken) return null

    try {
      const idTokenParts = user.value.idToken.split('.')

      if (idTokenParts.length !== 3) {
        console.error('Invalid ID Token format')
        return null
      }

      const idTokenParsed = JSON.parse(atob(idTokenParts[1]))

      return {
        auth_time: idTokenParsed.auth_time ?? null,
        client_roles: idTokenParsed.client_roles ?? [],
        display_name: idTokenParsed.display_name ?? null,
        email: idTokenParsed.email ?? null,
        exp: idTokenParsed.exp ?? null,
        family_name: idTokenParsed.family_name ?? null,
        given_name: idTokenParsed.given_name ?? null,
        idir_username: idTokenParsed.idir_username ?? null,
        name: idTokenParsed.name ?? null,
        preferred_username: idTokenParsed.preferred_username ?? null,
        user_principal_name: idTokenParsed.user_principal_name ?? null,
      }
    } catch (error) {
      console.error('Failed to parse ID Token:', error)
      return null
    }
  }

  const getAllRoles = () => {
    const parsedToken = parseIdToken()
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
    parseIdToken,
    getAllRoles,
    hasRole,
    logout,
  }
})
