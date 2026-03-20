import { CONSTANTS } from '@/constants'
import type { User } from '@/interfaces/interfaces'

/**
 * SessionStorage utilities for persisting authenticated user context.
 */

const AUTH_SESSION_KEY = btoa(CONSTANTS.AUTH_SESSION.KEY)

/**
 * Saves the authenticated user to sessionStorage with base64 encoding.
 */
export const saveAuthSession = (user: User): void => {
  sessionStorage.setItem(AUTH_SESSION_KEY, btoa(JSON.stringify(user)))
}

/**
 * Type guard that validates the parsed user object has all required token fields.
 * Returns false if any field is missing or not a non-empty string.
 * This prevents manipulated sessionStorage values from reaching the store.
 */
const isValidUser = (parsed: unknown): parsed is User => {
  if (!parsed || typeof parsed !== 'object') return false
  const u = parsed as Record<string, unknown>
  return (
    typeof u.accessToken === 'string' && u.accessToken.trim().length > 0 &&
    typeof u.refToken === 'string' && u.refToken.trim().length > 0 &&
    typeof u.idToken === 'string' && u.idToken.trim().length > 0
  )
}

/**
 * Loads and validates the authenticated user from sessionStorage.
 * Returns null if no session exists, decoding fails, or any field fails validation.
 */
export const loadAuthSession = (): User | null => {
  try {
    const raw = sessionStorage.getItem(AUTH_SESSION_KEY)
    if (!raw) return null
    const parsed: unknown = JSON.parse(atob(raw))
    return isValidUser(parsed) ? parsed : null
  } catch {
    return null
  }
}

/**
 * Clears the authenticated user session from sessionStorage.
 */
export const clearAuthSession = (): void => {
  sessionStorage.removeItem(AUTH_SESSION_KEY)
}
