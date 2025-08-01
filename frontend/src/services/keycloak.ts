import Keycloak from 'keycloak-js'
import { useAuthStore } from '@/stores/common/authStore'
import type { KeycloakInitOptions } from 'keycloak-js'
import { KEYCLOAK } from '@/constants/constants'
import * as messageHandler from '@/utils/messageHandler'
import { env } from '@/env'
import { AUTH_ERR } from '@/constants/message'
import { useNotificationStore } from '@/stores/common/notificationStore'
import { getActivePinia } from 'pinia'

let keycloakInstance: Keycloak | null = null

const ssoAuthServerUrl = env.VITE_SSO_AUTH_SERVER_URL
const ssoClientId = env.VITE_SSO_CLIENT_ID
const ssoRealm = env.VITE_SSO_REALM
const ssoRedirectUrl = env.VITE_SSO_REDIRECT_URI

/**
 * Creates or returns the singleton Keycloak instance.
 * @returns {Keycloak} The Keycloak instance.
 */
const createKeycloakInstance = (): Keycloak => {
  if (!keycloakInstance) {
    keycloakInstance = new Keycloak({
      url: `${ssoAuthServerUrl}` as string,
      realm: `${ssoRealm}` as string,
      clientId: `${ssoClientId}` as string,
    })
  }
  return keycloakInstance
}

const initOptions: KeycloakInitOptions = {
  pkceMethod: KEYCLOAK.PKCE_METHOD,
  checkLoginIframe: KEYCLOAK.CHECK_LOGIN_IFRAME,
  onLoad: KEYCLOAK.ONLOAD,
  enableLogging: KEYCLOAK.ENABLE_LOGGING,
}

const loginOptions = {
  redirectUri: ssoRedirectUrl as string,
}

/**
 * Initializes Keycloak, loads stored tokens if available, validates them, and handles login if needed.
 * @returns {Promise<Keycloak | undefined>} The initialized Keycloak instance or undefined on failure.
 */
export const initializeKeycloak = async (): Promise<Keycloak | undefined> => {
  const pinia = getActivePinia()
  const notificationStore = pinia ? useNotificationStore(pinia) : null

  if (!pinia) {
    console.warn('Pinia is not active. Message will only be logged.')
  }

  try {
    keycloakInstance = createKeycloakInstance()

    const authStore = useAuthStore()

    // to avoid making a KeyCloak API on every request
    authStore.loadUserFromStorage()
    if (
      authStore.authenticated &&
      authStore.user &&
      authStore.user.accessToken &&
      authStore.user.refToken &&
      authStore.user.idToken
    ) {
      keycloakInstance.token = authStore.user.accessToken
      keycloakInstance.refreshToken = authStore.user.refToken
      keycloakInstance.idToken = authStore.user.idToken
      keycloakInstance.authenticated = true

      // Perform token validation
      if (!validateAccessToken(keycloakInstance.token ?? '')) {
        logErrorAndLogout(
          AUTH_ERR.AUTH_001,
          'Token validation failed (Error: AUTH_001).',
        )
        return undefined
      }

      // Perform token validation and refresh if expired
      await handleTokenValidation()

      return keycloakInstance
    }

    const auth = await keycloakInstance.init(initOptions)
    console.info(`SSO initialization complete : ${auth}`)
    if (
      auth &&
      keycloakInstance.token &&
      keycloakInstance.refreshToken &&
      keycloakInstance.idToken
    ) {
      console.info('Ready to parsed token payload')
      const tokenParsed = JSON.parse(atob(keycloakInstance.token.split('.')[1]))

      // do validate the IDP in the JWT
      if (tokenParsed.identity_provider !== KEYCLOAK.IDP_AZUR_IDIR) {
        logErrorAndLogout(
          AUTH_ERR.AUTH_002,
          'Authentication failed: Invalid identity provider. (Error: AUTH_002).',
        )
        return undefined
      }

      // Perform token validation
      if (!validateAccessToken(keycloakInstance.token)) {
        logErrorAndLogout(
          AUTH_ERR.AUTH_003,
          'Token validation failed. (Error: AUTH_003).',
        )
        return undefined
      }

      authStore.setUser({
        accessToken: keycloakInstance.token,
        refToken: keycloakInstance.refreshToken,
        idToken: keycloakInstance.idToken,
      })

      // Perform token validation and refresh if expired
      await handleTokenValidation()

      return keycloakInstance
    } else {
      keycloakInstance.login(loginOptions)
    }
  } catch (err) {
    if (notificationStore) {
      notificationStore.showErrorMessage(AUTH_ERR.AUTH_004)
    }
    console.error('Keycloak initialization failed (Error: AUTH_004):', err)
    keycloakInstance = null // Reset the instance on failure
    throw err
  }
}

/**
 * Validates the access token by checking issuer and subject.
 * @param {string} accessToken - The access token to validate.
 * @returns {boolean} True if valid, false otherwise.
 */
const validateAccessToken = (accessToken: string): boolean => {
  if (!accessToken) {
    console.error('Access token is missing.')
    return false
  }

  try {
    const tokenParsed = JSON.parse(atob(accessToken.split('.')[1]))

    // Validate issuer
    if (tokenParsed.iss !== `${ssoAuthServerUrl}/realms/${ssoRealm}`) {
      console.error('Invalid token issuer.')
      return false
    }

    // validate subject
    if (!tokenParsed.sub) {
      console.error('Token subject is missing.')
      return false
    }

    return true
  } catch (error) {
    console.error('Failed to validate token:', error)
    return false
  }
}

/**
 * Initializes Keycloak and sets up authentication from stored tokens if available.
 * @returns {Promise<boolean>} True if initialization succeeds, false otherwise.
 */
export const initializeKeycloakAndAuth = async (): Promise<boolean> => {
  try {
    if (!keycloakInstance) {
      keycloakInstance = createKeycloakInstance()
    }

    const authStore = useAuthStore()

    // not initialized, the token not be refreshed
    if (keycloakInstance.clientId === undefined || !keycloakInstance.clientId) {
      if (!authStore || !authStore.user) {
        logErrorAndLogout(
          AUTH_ERR.AUTH_010,
          'Auth load failed. (Error: AUTH_010).',
        )
        return false
      }

      const { accessToken, refToken, idToken } = authStore.user

      if (!accessToken || !refToken || !idToken) {
        logErrorAndLogout(
          AUTH_ERR.AUTH_010,
          'Auth load failed. (Error: AUTH_010).',
        )
        return false
      }

      const auth = await keycloakInstance.init({
        token: authStore.user.accessToken,
        refreshToken: authStore.user.refToken,
        idToken: authStore.user.idToken,
      })

      if (!auth || !keycloakInstance.token) {
        logErrorAndLogout(
          AUTH_ERR.AUTH_011,
          'Auth load failed. (Error: AUTH_011).',
        )
        return false
      }
    }
    return true
  } catch (err) {
    logErrorAndLogout(
      AUTH_ERR.AUTH_012,
      `Error initializing Keycloak and Auth (Error: AUTH_012): ${err}`,
    )
    return false
  }
}

/**
 * Logs an error message and forces logout.
 * @param {string} message - The error message to be logged.
 * @param {string} [optionalMessage] - Optional detail message for console output.
 */
const logErrorAndLogout = (
  message: string,
  optionalMessage?: string | null,
): void => {
  messageHandler.logErrorMessage(message, optionalMessage)
  logout()
}

/**
 * Refreshes the token if it expires within the specified minValidity seconds.
 * If the token expires within minValidity seconds (minValidity is optional, if not specified 5 is used) the token is refreshed.
 * If -1 is passed as the minValidity, the token will be forcibly refreshed.
 * @param {number} [minValidity] - Minimum validity in seconds; -1 forces refresh.
 * @returns {Promise<boolean>} True if refreshed, false if valid or failed.
 */
export const refreshToken = async (minValidity?: number): Promise<boolean> => {
  try {
    const initialized = await initializeKeycloakAndAuth()
    if (!initialized || !keycloakInstance) {
      logErrorAndLogout(
        AUTH_ERR.AUTH_020,
        'Keycloak initialization failed during refresh token (Error: AUTH_020)',
      )
      return false
    }

    const authStore = useAuthStore()

    const refreshed = await keycloakInstance.updateToken(minValidity)

    if (refreshed) {
      console.log('Token was refreshed successfully')
      authStore.setUser({
        accessToken: keycloakInstance.token!,
        refToken: keycloakInstance.refreshToken!,
        idToken: keycloakInstance.idToken!,
      })
      return true
    } else {
      console.log('Token is still valid, no refresh needed')
      return false
    }
  } catch (err) {
    logErrorAndLogout(
      AUTH_ERR.AUTH_021,
      `Failed to refresh the token, or the session has expired (Error: AUTH_021): ${err}`,
    )
    return false
  }
}

/**
 * Validates the access token, refreshes if expired, and updates storage.
 * @returns {Promise<void>} Resolves on success, logs error on failure.
 */
export const handleTokenValidation = async (): Promise<void> => {
  try {
    const initialized = await initializeKeycloakAndAuth()
    if (!initialized || !keycloakInstance) {
      logErrorAndLogout(
        AUTH_ERR.AUTH_030,
        'Keycloak initialization failed during token validation (Error: AUTH_030)',
      )
      return
    }

    const authStore = useAuthStore()
    const parsedAccessToken = authStore.getParsedAccessToken()
    const currentTime = Math.floor(Date.now() / 1000)

    if (parsedAccessToken?.exp && parsedAccessToken.exp < currentTime) {
      await refreshToken(KEYCLOAK.UPDATE_TOKEN_MIN_VALIDITY)
    }
  } catch (err) {
    logErrorAndLogout(
      AUTH_ERR.AUTH_032,
      `Error during token validation (Error: AUTH_032): ${err}`,
    )
    return
  }
}

/**
 * Clears user data and redirects to logout URL.
 */
export const logout = (): void => {
  const authStore = useAuthStore()
  authStore.clearUser()

  window.location.href = `https://logon7.gov.bc.ca/clp-cgi/logoff.cgi?retnow=1&returl=${encodeURIComponent(
    `${ssoAuthServerUrl}/realms/${ssoRealm}/protocol/openid-connect/logout?post_logout_redirect_uri=` +
      ssoRedirectUrl +
      '&client_id=' +
      ssoClientId,
  )}`
}
