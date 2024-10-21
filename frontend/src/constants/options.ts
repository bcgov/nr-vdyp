export const derivedByOptions = [
  { label: 'Volume', value: 'Volume' },
  { label: 'Basal Area', value: 'Basal Area' },
]

export const speciesMap = {
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

export const becZoneOptions = [
  { label: 'AT - Alpine Tundra', value: '1' },
  { label: 'BG - Bunch Grass', value: '2' },
  { label: 'BWBS - Boreal White and Black Spruce', value: '3' },
  { label: 'CDF - Coastal Douglas Fir', value: '4' },
  { label: 'CWH - Coastal Western Hemlock', value: '5' },
  { label: 'ESSF - Engelmann Spruce', value: '6' },
  { label: 'ICH - Interior Cedar Hemlock', value: '7' },
  { label: 'IDF - Interior Douglas Fir', value: '8' },
  { label: 'MH - Mountain Hemlock', value: '9' },
  { label: 'MS - Montane Spruce', value: '10' },
  { label: 'PP - Ponderosa Pine', value: '11' },
  { label: 'SBPS - Sub-Boreal Pine-Spruce', value: '12' },
  { label: 'SBS - Sub-Boreal Spruce', value: '13' },
  { label: 'SWB - Spruce-Willow-Birch', value: '14' },
]

export const siteSpeciesValuesOptions = [
  { label: 'Computed', value: 'Computed' },
  { label: 'Supplied', value: 'Supplied' },
]

export const ageTypeOptions = [
  { label: 'Total', value: 'Total' },
  { label: 'Breast', value: 'Breast' },
]

export const ecoZoneOptions = [
  { label: 'Boreal Cordillera', value: '1' },
  { label: 'Boreal Plains', value: '2' },
  { label: 'Montane Cordillera', value: '3' },
  { label: 'Pacific Maritime', value: '4' },
  { label: 'Taiga Plains', value: '5' },
]

export const siteIndexCurveMap = {
  AC: 'Huang, Titus, and Lakusta (1994ac)',
  AT: 'Nigh, Krestov, and Klinka 2002',
  B: 'Chen and Klinka (2000ac)',
  BA: 'Nigh (2009)',
  BG: 'Nigh (2009)',
  BL: 'Chen and Klinka (2000ac)',
  CW: 'Nigh (2000)',
  DR: 'Nigh and Courtin (1998)',
  E: 'Nigh (2009)',
  EA: 'Nigh (2009)',
  EP: 'Nigh (2009)',
  FD: 'Thrower and Goudie (1992ac)',
  H: 'Nigh (1998)',
  HM: 'Means, Campbell, Johnson (1988ac)',
  HW: 'Nigh (1998)',
  L: 'Brisco, Klinka, Nigh 2002',
  LA: 'Brisco, Klinka, Nigh 2002',
  LT: 'Brisco, Klinka, Nigh 2002',
  LW: 'Brisco, Klinka, Nigh 2002',
  MB: 'Nigh and Courtin (1998)',
  PA: 'Thrower (1994)',
  PF: 'Thrower (1994)',
  PJ: 'Huang (1997ac)',
  PL: 'Thrower (1994)',
  PW: 'Curtis, Diaz, and Clendenen (1990ac)',
  PY: 'Nigh (2002)',
  S: 'Goudie (1984ac) (natural)',
  SB: 'Nigh, Krestov, and Klinka 2002',
  SE: 'Nigh (2015)',
  SS: 'Nigh (1997)',
  SW: 'Goudie (1984ac) (natural)',
  YC: 'Nigh (2000)',
}

export const floatingOptions = [
  { label: 'Float', value: 'Age' },
  { label: 'Float', value: 'Height' },
  { label: 'Float', value: 'SiteIndex' },
]

export const minimumDBHLimitsOptions = [
  { label: '4.0 cm+', value: '4.0 cm+' },
  { label: '7.5 cm+', value: '7.5 cm+' },
  { label: '12.5 cm+', value: '12.5 cm+' },
  { label: '17.5 cm+', value: '17.5 cm+' },
  { label: '22.5 cm+', value: '22.5 cm+' },
]

export const additionalStandAttributesOptions = [
  {
    label:
      'Use Computed Values (These additional Stand attributes require that a Stand Age and Basal Area be supplied on the Site Index and the Density pages)',
    value: 'Use Computed Values',
  },
  { label: 'Modify Computed Values', value: 'Modify Computed Values' },
]

export const volumeReportedOptions = [
  { label: 'Whole Stem', value: 'Whole Stem' },
  { label: 'Close Utilization', value: 'Close Utilization' },
  { label: 'Net Decay', value: 'Net Decay' },
  { label: 'Net Decay and Waste', value: 'Net Decay and Waste' },
  {
    label: 'Net Decay, Waste and Breakage',
    value: 'Net Decay, Waste and Breakage',
  },
]

export const includeInReportOptions = [
  { label: 'Computed MAI', value: 'Computed MAI' },
  { label: 'Species Composition', value: 'Species Composition' },
  { label: 'Culmination Values', value: 'Culmination Values' },
]

export const projectionTypeOptions = [
  { label: 'Volume', value: 'Volume' },
  { label: 'CFS Biomass', value: 'CFS Biomass' },
]
