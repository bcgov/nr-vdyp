import { defineStore } from 'pinia'
import { logout as keycloakLogout } from '@/services/keycloak'

interface User {
  accessToken: string
  refToken: string
  idToken: string
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    authenticated: false,
    user: null as User | null,
  }),
  actions: {
    setUser(user: User) {
      this.user = user
      this.authenticated = true
      sessionStorage.setItem('authUser', JSON.stringify(user))
    },
    clearUser() {
      this.user = null
      this.authenticated = false
      sessionStorage.removeItem('authUser')
    },
    loadUserFromStorage() {
      const user = sessionStorage.getItem('authUser')
      if (user) {
        try {
          const parsedUser = JSON.parse(user)
          if (
            parsedUser &&
            typeof parsedUser === 'object' &&
            parsedUser.accessToken &&
            parsedUser.refToken &&
            parsedUser.idToken
          ) {
            this.user = parsedUser
            this.authenticated = true
          } else {
            console.warn('Invalid user data in sessionStorage')
            this.clearUser()
          }
        } catch (error) {
          console.error('Failed to parse user from sessionStorage:', error)
          this.clearUser()
        }
      }
    },
    parseIdToken() {
      if (!this.user || !this.user.idToken) return null

      try {
        const idTokenParts = this.user.idToken.split('.')

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
    },
    getAllRoles(): string[] {
      const parsedToken = this.parseIdToken()
      if (parsedToken && Array.isArray(parsedToken.client_roles)) {
        return parsedToken.client_roles
      }

      return []
    },
    hasRole(role: string): boolean {
      const roles = this.getAllRoles()
      return roles.includes(role)
    },
    async logout() {
      this.clearUser()
      keycloakLogout()
    },
  },
})
