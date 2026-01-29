export interface ModelSpecies {
  code: string
  percent: number | null
}

export interface ModelParameters {
  species: ModelSpecies[]
  derivedBy: string | null
  becZone: string | null
  ecoZone: string | null
  siteIndex: string | null
  siteSpecies: string | null
  ageYears: string | null
  speciesAge: number | null
  speciesHeight: number | null
  bha50SiteIndex: number | null
  stockable: number | null
  cc: number | null
  BA: number | null
  TPH: number | null
  minDBHLimit: string | null
  currentDiameter: string | null
}
