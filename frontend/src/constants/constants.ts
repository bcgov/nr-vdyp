export const KEYCLOAK = Object.freeze({
  PKCE_METHOD: 'S256',
  ONLOAD: 'check-sso',
  CHECK_LOGIN_IFRAME: false,
  // SILENT_CHECK_SSO_REDIRECT_PAGE: '/silent-check-sso.html',
  IDP_AZUR_IDIR: 'azureidir', // Identity Provider: IDIR with MFA
  MAX_SESSION_DURATION: 8 * 60 * 60 * 1000, // 8 hours (in milliseconds)
  UPDATE_TOKEN_MIN_VALIDITY: 5, // if -1, forcibly refreshed (in minutes)
  ENABLE_LOGGING: false,
})

export const SORT_ORDER = Object.freeze({
  ASC: 'ASC',
  DESC: 'DESC',
})

export const DERIVED_BY = Object.freeze({
  VOLUME: 'Volume',
  BASAL_AREA: 'Basal Area',
})

export const SITE_SPECIES_VALUES = Object.freeze({
  COMPUTED: 'Computed',
  SUPPLIED: 'Supplied',
})

export const FLOATING = Object.freeze({
  AGE: 'Age',
  HEIGHT: 'Height',
  SITEINDEX: 'SiteIndex',
})

export const COMPUTED_VALUES = Object.freeze({
  USE: 'Use Computed Values',
  MODIFY: 'Modify Computed Values',
})
