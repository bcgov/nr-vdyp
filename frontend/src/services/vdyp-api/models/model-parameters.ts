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
  // SPP1
  compute: string | null  // new field added by VDYP-1076
  ageYears: string | null
  speciesAge: string | null
  speciesHeight: string | null
  bha50SiteIndex: string | null
  // SPP2
  compute2: string | null
  ageYears2: string | null
  age2: string | null
  height2: string | null
  si2: string | null
  // SPP3
  compute3: string | null
  ageYears3: string | null
  age3: string | null
  height3: string | null
  si3: string | null
  // SPP4
  compute4: string | null
  ageYears4: string | null
  age4: string | null
  height4: string | null
  si4: string | null
  // SPP5
  compute5: string | null
  ageYears5: string | null
  age5: string | null
  height5: string | null
  si5: string | null
  // SPP6
  compute6: string | null
  ageYears6: string | null
  age6: string | null
  height6: string | null
  si6: string | null
  stockable: number | null
  cc: number | null
  BA: number | null
  TPH: number | null
  minDBHLimit: string | null
  currentDiameter: string | null
}
