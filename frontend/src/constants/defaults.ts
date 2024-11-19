import * as CONSTANTS from '@/constants/constants'

export const DEFAULT_VALUES = Object.freeze({
  DERIVED_BY: CONSTANTS.DERIVED_BY.VOLUME,
  BEC_ZONE: 'IDF',
  SITE_SPECIES_VALUES: CONSTANTS.SITE_SPECIES_VALUES.COMPUTED,
  AGE_TYPE: CONSTANTS.AGE_TYPE.TOTAL,
  PERCENT_STOCKABLE_AREA: 55,
  AGE: 60,
  HEIGHT: '17.00',
  BHA50_SITE_INDEX: '16.30',
  FLOATING: CONSTANTS.FLOATING.SITEINDEX,
  BASAL_AREA: '10.0000',
  TPH: '1000.00',
  MINIMUM_DBH_LIMIT: CONSTANTS.MINIMUM_DBH_LIMITS.CM7_5,
  CURRENT_DIAMETER: '11.3',
  PERCENT_CROWN_CLOSURE: 0,
  COMPUTED_VALUES: CONSTANTS.COMPUTED_VALUES.USE,
  LOREY_HEIGHT: '14.47',
  WHOLE_STEM_VOL75: '61.1',
  BASAL_AREA125: '4.7545',
  WHOLE_STEM_VOL125: '34.8',
  CU_VOL: '23.6',
  CU_NET_DECAY_VOL: '22.6',
  CU_NET_DECAY_WASTE_VOL: '22.2',
  STARTING_AGE: 0,
  FINISHING_AGE: 250,
  AGE_INCREMENT: 25,
  VOLUME_REPORTED: [CONSTANTS.VOLUME_REPORTED.WHOLE_STEM],
  PROJECTION_TYPE: CONSTANTS.PROJECTION_TYPE.VOLUME,
  REPORT_TITLE: 'A Sample Report Title',
})