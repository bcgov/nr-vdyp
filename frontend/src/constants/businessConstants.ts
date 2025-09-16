import { UtilizationClassSetEnum } from '@/services/vdyp-api/models/utilization-class-set-enum'

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

// Mapping Species Group to Default Utilization Level for CFS Biomass
export const CFS_BIOMASS_SPECIES_GROUP_UTILIZATION_MAP: {
  [key: string]: UtilizationClassSetEnum
} = {
  AC: UtilizationClassSetEnum._125,
  AT: UtilizationClassSetEnum._125,
  B: UtilizationClassSetEnum._175,
  C: UtilizationClassSetEnum._175,
  D: UtilizationClassSetEnum._125,
  E: UtilizationClassSetEnum._125,
  F: UtilizationClassSetEnum._175,
  H: UtilizationClassSetEnum._175,
  L: UtilizationClassSetEnum._125,
  MB: UtilizationClassSetEnum._125,
  PA: UtilizationClassSetEnum._125,
  PL: UtilizationClassSetEnum._125,
  PW: UtilizationClassSetEnum._125,
  PY: UtilizationClassSetEnum._125,
  S: UtilizationClassSetEnum._175,
  Y: UtilizationClassSetEnum._175,
}

// All 16 Species Groups
export const SPECIES_GROUPS = [
  'AC',
  'AT',
  'B',
  'C',
  'D',
  'E',
  'F',
  'H',
  'L',
  'MB',
  'PA',
  'PL',
  'PW',
  'PY',
  'S',
  'Y',
] as const

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

export const SECONDARY_SPECIES_AGE = 50 as const
export const SECONDARY_SPECIES_HEIGHT = '15.00' as const

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

// BA_LIMIT_COEFFICIENTS stores the basal area limit coefficients for different species (AC, AT, B, etc.).
// Each species has separate coefficient values for coastal and interior regions, represented by 'coeff1' and 'coeff2'.
// These coefficients are used in an exponential equation to calculate the maximum allowable basal area based on species and location.
// A value of -999 indicates that no valid limit exists for the specific region and species combination. (from vdyp.ini)
export const BA_LIMIT_COEFFICIENTS = {
  AC: {
    coastal: { coeff1: 107.240519, coeff2: -14.377881 },
    interior: { coeff1: 118.629456, coeff2: -19.159803 },
  },
  AT: {
    coastal: { coeff1: -999, coeff2: -999 },
    interior: { coeff1: 98.298267, coeff2: -15.823783 },
  },
  B: {
    coastal: { coeff1: 134.265995, coeff2: -10.723979 },
    interior: { coeff1: 103.717551, coeff2: -12.032769 },
  },
  C: {
    coastal: { coeff1: 199.94291, coeff2: -14.931348 },
    interior: { coeff1: 393.75934, coeff2: -35.40266 },
  },
  D: {
    coastal: { coeff1: 107.240519, coeff2: -14.377881 },
    interior: { coeff1: -999, coeff2: -999 },
  },
  E: {
    coastal: { coeff1: 107.240519, coeff2: -14.377881 },
    interior: { coeff1: 118.629456, coeff2: -19.159803 },
  },
  F: {
    coastal: { coeff1: 213.706529, coeff2: -28.643038 },
    interior: { coeff1: 132.594246, coeff2: -20.216383 },
  },
  H: {
    coastal: { coeff1: 144.825311, coeff2: -13.110869 },
    interior: { coeff1: 122.420409, coeff2: -10.923619 },
  },
  L: {
    coastal: { coeff1: -999, coeff2: -999 },
    interior: { coeff1: 119.642742, coeff2: -21.246736 },
  },
  MB: {
    coastal: { coeff1: 107.240519, coeff2: -14.377881 },
    interior: { coeff1: -999, coeff2: -999 },
  },
  PL: {
    coastal: { coeff1: 185.048127, coeff2: -19.900699 },
    interior: { coeff1: 95.118542, coeff2: -12.154888 },
  },
  PW: {
    coastal: { coeff1: -999, coeff2: -999 },
    interior: { coeff1: 158.465684, coeff2: -26.781112 },
  },
  PY: {
    coastal: { coeff1: -999, coeff2: -999 },
    interior: { coeff1: 71.943238, coeff2: -14.264704 },
  },
  S: {
    coastal: { coeff1: 177.814415, coeff2: -13.714547 },
    interior: { coeff1: 96.84127, coeff2: -12.60781 },
  },
}

// BA_EQUATION_CONSTANTS defines the constant values used in the basal area limit equation.
// These constants (const1, const2, and const3) are applied to calculate the final basal area limit based on species-specific coefficients. (from vdyp.ini)
export const BA_EQUATION_CONSTANTS = {
  const1: 5,
  const2: 1.3,
  const3: -1,
}

export const TPH_LIMIT_COEFFICIENTS = {
  AC: {
    coastal: {
      P10: { a0: 7.5, b0: 0.184064, b1: 0.005592 },
      P90: { a0: 7.5, b0: 0.96373, b1: 0.00453 },
    },
    interior: {
      P10: { a0: 7.5, b0: -0.084114, b1: 0.016436 },
      P90: { a0: 7.5, b0: 0.58714, b1: 0.022826 },
    },
  },
  AT: {
    interior: {
      P10: { a0: 7.5, b0: 0.00544, b1: 0.010618 },
      P90: { a0: 7.5, b0: 0.660157, b1: 0.011754 },
    },
  },
  B: {
    coastal: {
      P10: { a0: 7.5, b0: 0.229925, b1: 0.005735 },
      P90: { a0: 7.5, b0: 1.226133, b1: -0.002427 },
    },
    interior: {
      P10: { a0: 7.5, b0: 0.184201, b1: 0.006065 },
      P90: { a0: 7.5, b0: 1.059981, b1: -0.000686 },
    },
  },
  C: {
    coastal: {
      P10: { a0: 7.5, b0: 0.387454, b1: 0.002709 },
      P90: { a0: 7.5, b0: 1.45061, b1: -0.000679 },
    },
    interior: {
      P10: { a0: 7.5, b0: 0.103056, b1: 0.012318 },
      P90: { a0: 7.5, b0: 0.2699, b1: 0.042869 },
    },
  },
  D: {
    coastal: {
      P10: { a0: 7.5, b0: 0.184064, b1: 0.005592 },
      P90: { a0: 7.5, b0: 0.96373, b1: 0.00453 },
    },
  },
  E: {
    coastal: {
      P10: { a0: 7.5, b0: 0.184064, b1: 0.005592 },
      P90: { a0: 7.5, b0: 0.96373, b1: 0.00453 },
    },
    interior: {
      P10: { a0: 7.5, b0: -0.084114, b1: 0.016436 },
      P90: { a0: 7.5, b0: 0.58714, b1: 0.022826 },
    },
  },
  F: {
    coastal: {
      P10: { a0: 7.5, b0: 0.116002, b1: 0.006594 },
      P90: { a0: 7.5, b0: 0.68269, b1: 0.008622 },
    },
    interior: {
      P10: { a0: 7.5, b0: 0.123477, b1: 0.005786 },
      P90: { a0: 7.5, b0: 1.193114, b1: -0.006459 },
    },
  },
  H: {
    coastal: {
      P10: { a0: 7.5, b0: 0.126113, b1: 0.007561 },
      P90: { a0: 7.5, b0: 1.207655, b1: -0.001023 },
    },
    interior: {
      P10: { a0: 7.5, b0: 0.014342, b1: 0.012198 },
      P90: { a0: 7.5, b0: 0.79931, b1: 0.013942 },
    },
  },
  L: {
    interior: {
      P10: { a0: 7.5, b0: 0.06893, b1: 0.005579 },
      P90: { a0: 7.5, b0: 0.31423, b1: 0.015952 },
    },
  },
  MB: {
    coastal: {
      P10: { a0: 7.5, b0: 0.184064, b1: 0.005592 },
      P90: { a0: 7.5, b0: 0.96373, b1: 0.00453 },
    },
  },
  PL: {
    coastal: {
      P10: { a0: 7.5, b0: -0.083294, b1: 0.014145 },
      P90: { a0: 7.5, b0: 0.938361, b1: -0.003504 },
    },
    interior: {
      P10: { a0: 7.5, b0: -0.083294, b1: 0.014145 },
      P90: { a0: 7.5, b0: 0.938361, b1: -0.003504 },
    },
  },
  PW: {
    interior: {
      P10: { a0: 7.5, b0: 0.031801, b1: 0.007887 },
      P90: { a0: 7.5, b0: 0.909946, b1: -0.005477 },
    },
  },
  PY: {
    interior: {
      P10: { a0: 7.5, b0: 0.267422, b1: 0.009514 },
      P90: { a0: 7.5, b0: 1.922409, b1: -0.008496 },
    },
  },
  S: {
    coastal: {
      P10: { a0: 7.5, b0: 0.16879, b1: 0.008936 },
      P90: { a0: 7.5, b0: 0.8714, b1: 0.011812 },
    },
    interior: {
      P10: { a0: 7.5, b0: 0.124051, b1: 0.007309 },
      P90: { a0: 7.5, b0: 0.910138, b1: 0.002576 },
    },
  },
}

// Mapping bec zone code and coastal or not
export const BEC_ZONE_COASTAL_MAP: Record<string, boolean> = {
  AT: false,
  BG: false,
  BWBS: false,
  CDF: true,
  CWH: true,
  ESSF: false,
  ICH: false,
  IDF: false,
  MH: true,
  MS: false,
  PP: false,
  SBPS: false,
  SBS: false,
  SWB: false,
}

export const TPH_EQUATION_CONSTANTS = {
  const1: 5.0,
  const2: 1.3,
  const3: 0.00007854,
}
