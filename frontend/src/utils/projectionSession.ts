import { CONSTANTS } from '@/constants'

/**
 * SessionStorage utilities for persisting projection navigation context.
 */

const PROJ_CTX_KEY = btoa(CONSTANTS.PROJECTION_SESSION_CTX.KEY)

export type ProjectionSessionCtx =
  | { type: typeof CONSTANTS.PROJECTION_SESSION_CTX.NEW_TYPE; ms: string }                // new (unsaved) projection
  | { type: typeof CONSTANTS.PROJECTION_SESSION_CTX.EXISTING_TYPE; g: string; m: string } // existing projection (view/edit)

/**
 * Saves session context before navigating to an existing projection's detail page.
 * Enables the detail page to re-fetch projection data on browser refresh.
 */
export const saveExistingProjectionSession = (projectionGUID: string, viewMode: string): void => {
  const ctx: ProjectionSessionCtx = { type: CONSTANTS.PROJECTION_SESSION_CTX.EXISTING_TYPE, g: projectionGUID, m: viewMode }
  sessionStorage.setItem(PROJ_CTX_KEY, btoa(JSON.stringify(ctx)))
}

/**
 * Saves session context before navigating to a new projection's detail page.
 * Stores model selection so the correct empty form can be restored on refresh.
 */
export const saveNewProjectionSession = (modelSelection: string): void => {
  const ctx: ProjectionSessionCtx = { type: CONSTANTS.PROJECTION_SESSION_CTX.NEW_TYPE, ms: modelSelection }
  sessionStorage.setItem(PROJ_CTX_KEY, btoa(JSON.stringify(ctx)))
}

// Allowlists used to validate sessionStorage values against manipulation.
// 'create' is intentionally excluded from VALID_EXISTING_VIEW_MODES -
// existing projections can never be in create mode.
const VALID_MODEL_SELECTIONS = new Set<string>(Object.values(CONSTANTS.MODEL_SELECTION))
const VALID_EXISTING_VIEW_MODES = new Set<string>([
  CONSTANTS.PROJECTION_VIEW_MODE.VIEW,
  CONSTANTS.PROJECTION_VIEW_MODE.EDIT,
])

/**
 * Type guard that validates all fields of a parsed session context object.
 * Returns false if any field is missing, the wrong type, or not in the allowlist.
 * This prevents manipulated sessionStorage values from reaching the store.
 */
const isValidCtx = (parsed: unknown): parsed is ProjectionSessionCtx => {
  if (!parsed || typeof parsed !== 'object') return false
  const c = parsed as Record<string, unknown>

  if (c.type === CONSTANTS.PROJECTION_SESSION_CTX.NEW_TYPE) {
    return typeof c.ms === 'string' && VALID_MODEL_SELECTIONS.has(c.ms)
  }

  if (c.type === CONSTANTS.PROJECTION_SESSION_CTX.EXISTING_TYPE) {
    return typeof c.g === 'string' && c.g.trim().length > 0 &&
           typeof c.m === 'string' && VALID_EXISTING_VIEW_MODES.has(c.m)
  }

  return false
}

/**
 * Loads and validates the projection session context saved before navigation.
 * Returns null if no session exists, parsing fails, or any field fails validation.
 */
export const loadProjectionSession = (): ProjectionSessionCtx | null => {
  try {
    const raw = sessionStorage.getItem(PROJ_CTX_KEY)
    if (!raw) return null
    const parsed: unknown = JSON.parse(atob(raw))
    return isValidCtx(parsed) ? parsed : null
  } catch {
    return null
  }
}

/**
 * Clears the projection session context.
 * Should be called when navigating away from the detail page.
 */
export const clearProjectionSession = (): void => {
  sessionStorage.removeItem(PROJ_CTX_KEY)
}
