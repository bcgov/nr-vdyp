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

/**
 * Loads the projection session context saved before navigation.
 * Returns null if no session exists or if parsing fails.
 */
export const loadProjectionSession = (): ProjectionSessionCtx | null => {
  try {
    const raw = sessionStorage.getItem(PROJ_CTX_KEY)
    if (!raw) return null
    return JSON.parse(atob(raw)) as ProjectionSessionCtx
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
