// Mapping species code and species names
export const SPECIES_MAP = {
  AC: 'Poplar',
  AT: 'Aspen',
  B: 'True Fir',
  BA: 'Amabilis Fir',
  BG: 'Grand Fir',
  BL: 'Alpine Fir',
  CW: 'Western Red Cedar',
  DR: 'Red Alder',
  E: 'Birch',
  EA: 'Alaska Paper Birch',
  EP: 'Common Paper Birch',
  FD: 'Douglas Fir',
  H: 'Hemlock',
  HM: 'Mountain Hemlock',
  HW: 'Western Hemlock',
  L: 'Larch',
  LA: 'Alpine Larch',
  LT: 'Tamarack',
  LW: 'Western Larch',
  MB: 'Bigleaf Maple',
  PA: 'Whitebark Pine',
  PF: 'Limber Pine',
  PJ: 'Jack Pine',
  PL: 'Lodgepole Pine',
  PW: 'Western White Pine',
  PY: 'Ponderosa (Yellow) Pine',
  S: 'Spruce',
  SB: 'Black Spruce',
  SE: 'Engelmann Spruce',
  SS: 'Sitka Spruce',
  SW: 'White Spruce',
  YC: 'Yellow Cedar',
}

// Mapping Site Species(Key) and Species Group
export const SPECIES_GROUP_MAP: { [key: string]: string } = {
  AC: 'AC',
  AT: 'AT',
  B: 'B',
  BA: 'B',
  BG: 'B',
  BL: 'B',
  CW: 'C',
  DR: 'D',
  E: 'E',
  EA: 'E',
  EP: 'E',
  FD: 'F',
  H: 'H',
  HM: 'H',
  HW: 'H',
  L: 'L',
  LA: 'L',
  LT: 'L',
  LW: 'L',
  MB: 'MB',
  PA: 'PA',
  PF: 'PA',
  PJ: 'PL',
  PL: 'PL',
  PW: 'PW',
  PY: 'PY',
  S: 'S',
  SB: 'S',
  SE: 'S',
  SS: 'S',
  SW: 'S',
  YC: 'Y',
}

export const CONIFEROUS_SPECIES = new Set([
  'B',
  'BA',
  'BG',
  'BL',
  'CW',
  'FD',
  'H',
  'HM',
  'HW',
  'L',
  'LA',
  'LT',
  'LW',
  'PA',
  'PF',
  'PJ',
  'PL',
  'PW',
  'PY',
  'S',
  'SB',
  'SE',
  'SS',
  'SW',
  'YC',
])

export const BROADLEAF_SPECIES = new Set([
  'AC',
  'AT',
  'DR',
  'E',
  'EA',
  'EP',
  'MB',
])

export const INVENTORY_CODES = {
  FIP: 'F',
  VRI: 'V',
} as const

export const BCLCS_LEVEL1_THRESHOLD = 5 as const
export const BCLCS_LEVEL1_VEG = 'V' as const // Vegetated
export const BCLCS_LEVEL1_NON_VEG = 'N' as const // Non-Vegetated

export const BCLCS_LEVEL2_THRESHOLD = 10 as const
export const BCLCS_LEVEL2_TREED = 'T' as const // Treed
export const BCLCS_LEVEL2_NON_TREED = 'N' as const // Non-treed

export const BCLCS_LEVEL3_DEFAULT = 'U' as const // Upland
export const BCLCS_LEVEL3_ALPINE = 'A' as const // Alpine
export const BCLCS_LEVEL3_BECZONE_AT = 'AT' as const

export const BCLCS_LEVEL4_TC = 'TC' as const // Treed - Coniferous
export const BCLCS_LEVEL4_TB = 'TB' as const // Treed - Broadleaf
export const BCLCS_LEVEL4_TM = 'TM' as const // Treed - Mixed

export const BCLCS_LEVEL5_DE = 'DE' as const // Dense
export const BCLCS_LEVEL5_OP = 'OP' as const // Open
export const BCLCS_LEVEL5_SP = 'SP' as const // Sparse

export const UNKNOWN_CD = 'UNK' as const
export const YIELD_FACTOR_CD = '1' as const
export const FOREST_COVER_RANK_CD = '1' as const
export const LAYER_LEVEL_CD = '1' as const
export const VDYP7_LAYER_CD = 'P' as const
export const MAP_ID = '000A000' as const
