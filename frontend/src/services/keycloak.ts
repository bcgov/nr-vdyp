import Keycloak from 'keycloak-js'
import { useAuthStore } from '@/stores/common/authStore'
import type { KeycloakInitOptions } from 'keycloak-js'
import { KEYCLOAK } from '@/constants/constants'
import { Util } from '@/utils/util'
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
      if (!validateAccessToken(keycloakInstance.token)) {
        logErrorAndLogout(
          AUTH_ERR.AUTH_001,
          'Token validation failed (Error: AUTH_001).',
        )
        return undefined
      }

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

      // TODO - need to set up periodic token refresh?

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

const validateAccessToken = (accessToken: string): boolean => {
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

export const initializeKeycloakAndAuth = async (): Promise<boolean> => {
  try {
    if (!keycloakInstance) {
      keycloakInstance = createKeycloakInstance()
    }

    const authStore = useAuthStore()

    // not initialized, the token not be refreshed
    if (!keycloakInstance.clientId) {
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

// If the token expires within minValidity seconds (minValidity is optional, if not specified 5 is used) the token is refreshed.
// If -1 is passed as the minValidity, the token will be forcibly refreshed.
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
 * checks if the access token is valid, refreshes it if not, and stores it afterwards.
 * @returns null if valid, or null if the token has been refreshed. If the token fails to refresh or it is not available, return an error message.
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

    const currentTime = Date.now()
    const authTimeInUnixTime = getAuthTimeInUnixTime(
      authStore.user!.accessToken,
    )
    const sessionDuration = currentTime - authTimeInUnixTime

    // force session logout when the maximum session time is exceeded
    if (sessionDuration > KEYCLOAK.MAX_SESSION_DURATION) {
      logErrorAndLogout(
        AUTH_ERR.AUTH_031,
        `Forced out due to maximum session timeout (Error: AUTH_031) => session-duration:${sessionDuration} > max:${KEYCLOAK.MAX_SESSION_DURATION}`,
      )
      return
    }

    console.log(
      `----------------current time: ${new Date(currentTime).toString()}`,
    )
    console.log(
      `access token expired date(1): ${getTokenExpirationDate(authStore.user!.accessToken)}`,
    )
    console.log(
      `access token expired?: ${keycloakInstance.isTokenExpired(KEYCLOAK.IS_TOKEN_EXP_MIN_VALIDITY)}`,
    )

    if (keycloakInstance.isTokenExpired(KEYCLOAK.IS_TOKEN_EXP_MIN_VALIDITY)) {
      const refreshed = await refreshToken(KEYCLOAK.UPDATE_TOKEN_MIN_VALIDITY)
      console.log(`access token refreshed? ${refreshed}`)
    }

    console.log(
      `access token expired date(2): ${getTokenExpirationDate(authStore.user!.accessToken)}`,
    )
  } catch (err) {
    logErrorAndLogout(
      AUTH_ERR.AUTH_032,
      `Error during token validation (Error: AUTH_032): ${err}`,
    )
    return
  }
}

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

const getAuthTimeInUnixTime = (accessToken: string): number => {
  try {
    const accessTokenParsed = JSON.parse(atob(accessToken.split('.')[1]))
    console.log(
      `auth_time: ${accessTokenParsed.auth_time} (${Util.formatUnixTimestampToDate(accessTokenParsed.auth_time)})`,
    )
    return accessTokenParsed.auth_time * 1000 // convert to milliseconds
  } catch (error) {
    console.error('Failed to parse auth_time from ID Token:', error)
    return Date.now() // the current time on failure
  }
}

const getTokenExpirationDate = (token: string): Date | null => {
  try {
    const tokenParsed = JSON.parse(atob(token.split('.')[1]))
    if (!tokenParsed.exp) {
      return null
    }

    const expirationDate = Util.formatUnixTimestampToDate(tokenParsed.exp)
    return expirationDate
  } catch (error) {
    console.error('Failed to parse token expiration:', error)
    return null
  }
}

// TODO - need to set up periodic token refresh?
