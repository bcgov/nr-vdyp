import { mount } from 'cypress/vue'
import { createVuetify } from 'vuetify'
import { createPinia, setActivePinia } from 'pinia'
import 'vuetify/styles'
import SiteIndicesTable from './SiteIndicesTable.vue'
import { useModelParameterStore } from '@/stores/projection/modelParameterStore'
import { CONSTANTS, DEFAULTS } from '@/constants'
import type { SiteIndexSpeciesRow } from '@/interfaces/interfaces'

const vuetify = createVuetify()

const makeRow = (speciesCode: string, overrides: Partial<SiteIndexSpeciesRow> = {}): SiteIndexSpeciesRow => ({
  speciesCode,
  computedValue: CONSTANTS.COMPUTED_VALUE.BHA_SITE_INDEX,
  ageType: CONSTANTS.AGE_TYPE.TOTAL,
  age: DEFAULTS.DEFAULT_VALUES.SPZ_AGE,
  height: DEFAULTS.DEFAULT_VALUES.SPZ_HEIGHT,
  bhaSiteIndex: DEFAULTS.DEFAULT_VALUES.BHA50_SITE_INDEX,
  ...overrides,
})

const mountTable = (
  isConfirmEnabled: boolean,
  setup?: (store: ReturnType<typeof useModelParameterStore>) => void,
) => {
  const pinia = createPinia()
  setActivePinia(pinia)
  const store = useModelParameterStore()
  if (setup) setup(store)
  mount(SiteIndicesTable, {
    global: { plugins: [pinia, vuetify] },
    props: { isConfirmEnabled },
  })
  return { store }
}

describe('<SiteIndicesTable />', () => {
  describe('Table structure', () => {
    it('renders the "Site Indices" subtitle', () => {
      mountTable(false)
      cy.contains('.text-subtitle-2', 'Site Indices').should('exist')
    })

    it('renders all column headers', () => {
      mountTable(false)
      cy.get('thead th').eq(0).should('contain.text', 'Site Species')
      cy.get('thead th').eq(1).should('contain.text', 'Computed Value')
      cy.get('thead th').eq(2).should('contain.text', 'Age')
      cy.get('thead th').eq(3).should('contain.text', 'Height in Meters')
      cy.get('thead th').eq(4).should('contain.text', 'BHA Site Index')
    })

    it('renders no rows when siteIndexRows is empty', () => {
      mountTable(false)
      cy.get('tbody tr').should('have.length', 0)
    })

    it('renders one row per entry in siteIndexRows', () => {
      mountTable(false, (store) => {
        store.siteIndexRows = [makeRow('FD'), makeRow('PL'), makeRow('S')]
      })
      cy.get('tbody tr').should('have.length', 3)
    })

    it('displays the species code in the first cell of each row', () => {
      mountTable(false, (store) => {
        store.siteIndexRows = [makeRow('FD'), makeRow('PL')]
      })
      cy.get('tbody tr').eq(0).find('td').eq(0).should('contain.text', 'FD')
      cy.get('tbody tr').eq(1).find('td').eq(0).should('contain.text', 'PL')
    })
  })

  describe('Computed Value select - disabled states', () => {
    it('is disabled for all rows when isConfirmEnabled is false', () => {
      mountTable(false, (store) => {
        store.siteIndexRows = [makeRow('FD')]
      })
      cy.get('.v-select.v-input--disabled').should('have.length', 1)
    })

    it('is disabled when siteSpeciesValues is Supplied', () => {
      mountTable(true, (store) => {
        store.siteIndexRows = [makeRow('FD')]
        store.siteSpeciesValues = CONSTANTS.SITE_SPECIES_VALUES.SUPPLIED
      })
      cy.get('.v-select.v-input--disabled').should('have.length', 1)
    })

    it('is enabled for the primary row when Computed + isConfirmEnabled', () => {
      mountTable(true, (store) => {
        store.siteIndexRows = [makeRow('FD')]
        store.siteSpeciesValues = CONSTANTS.SITE_SPECIES_VALUES.COMPUTED
      })
      cy.get('.v-select').first().should('not.have.class', 'v-input--disabled')
    })

    it('is disabled for non-primary rows when Computed + Volume + isConfirmEnabled', () => {
      mountTable(true, (store) => {
        store.siteIndexRows = [makeRow('FD'), makeRow('PL')]
        store.siteSpeciesValues = CONSTANTS.SITE_SPECIES_VALUES.COMPUTED
        store.derivedBy = CONSTANTS.DERIVED_BY.VOLUME
      })
      cy.get('.v-select').eq(1).should('have.class', 'v-input--disabled')
    })
  })

  describe('Age radio group - disabled states', () => {
    it('is disabled when isConfirmEnabled is false', () => {
      mountTable(false, (store) => {
        store.siteIndexRows = [makeRow('FD')]
      })
      cy.get('.v-radio-group.v-input--disabled').should('have.length', 1)
    })

    it('is disabled when siteSpeciesValues is Supplied', () => {
      mountTable(true, (store) => {
        store.siteIndexRows = [makeRow('FD')]
        store.siteSpeciesValues = CONSTANTS.SITE_SPECIES_VALUES.SUPPLIED
      })
      cy.get('.v-radio-group.v-input--disabled').should('have.length', 1)
    })

    it('is enabled when Computed + isConfirmEnabled', () => {
      mountTable(true, (store) => {
        store.siteIndexRows = [makeRow('FD')]
        store.siteSpeciesValues = CONSTANTS.SITE_SPECIES_VALUES.COMPUTED
      })
      cy.get('.v-radio-group').first().should('not.have.class', 'v-input--disabled')
    })
  })

  describe('Placeholders (N/A and Calc.)', () => {
    it('shows N/A placeholder for Age when Supplied', () => {
      mountTable(true, (store) => {
        store.siteIndexRows = [makeRow('FD', { age: null })]
        store.siteSpeciesValues = CONSTANTS.SITE_SPECIES_VALUES.SUPPLIED
      })
      cy.get('tbody tr').eq(0).find('.site-spin-field').eq(0).find('input')
        .should('have.attr', 'placeholder', CONSTANTS.SPECIAL_INDICATORS.NA)
    })

    it('shows Calc. placeholder for Age when computedValue is Total Age', () => {
      mountTable(true, (store) => {
        store.siteIndexRows = [makeRow('FD', { age: null, computedValue: CONSTANTS.COMPUTED_VALUE.TOTAL_AGE })]
        store.siteSpeciesValues = CONSTANTS.SITE_SPECIES_VALUES.COMPUTED
      })
      cy.get('tbody tr').eq(0).find('.site-spin-field').eq(0).find('input')
        .should('have.attr', 'placeholder', CONSTANTS.SPECIAL_INDICATORS.CALC)
    })

    it('shows Calc. placeholder for Height when computedValue is Height in Meters', () => {
      mountTable(true, (store) => {
        store.siteIndexRows = [makeRow('FD', { height: null, computedValue: CONSTANTS.COMPUTED_VALUE.HEIGHT })]
        store.siteSpeciesValues = CONSTANTS.SITE_SPECIES_VALUES.COMPUTED
      })
      cy.get('tbody tr').eq(0).find('.site-spin-field').eq(1).find('input')
        .should('have.attr', 'placeholder', CONSTANTS.SPECIAL_INDICATORS.CALC)
    })

    it('shows Calc. placeholder for BHA Site Index when computedValue is BHA Site Index', () => {
      mountTable(true, (store) => {
        store.siteIndexRows = [makeRow('FD', { bhaSiteIndex: null, computedValue: CONSTANTS.COMPUTED_VALUE.BHA_SITE_INDEX })]
        store.siteSpeciesValues = CONSTANTS.SITE_SPECIES_VALUES.COMPUTED
      })
      cy.get('tbody tr').eq(0).find('.site-spin-field').eq(2).find('input')
        .should('have.attr', 'placeholder', CONSTANTS.SPECIAL_INDICATORS.CALC)
    })

    it('shows N/A for all spin fields on non-primary rows when Computed + Volume', () => {
      mountTable(true, (store) => {
        store.siteIndexRows = [makeRow('FD'), makeRow('PL', { age: null, height: null, bhaSiteIndex: null })]
        store.siteSpeciesValues = CONSTANTS.SITE_SPECIES_VALUES.COMPUTED
        store.derivedBy = CONSTANTS.DERIVED_BY.VOLUME
      })
      cy.get('tbody tr').eq(1).find('.site-spin-field input').each(($input) => {
        cy.wrap($input).should('have.attr', 'placeholder', CONSTANTS.SPECIAL_INDICATORS.NA)
      })
    })
  })

  describe('Watch: siteSpeciesValues -> Supplied resets rows', () => {
    it('clears age and height when switching to Supplied (DOM: Age input becomes N/A placeholder)', () => {
      mountTable(true, (store) => {
        store.siteIndexRows = [makeRow('FD')]
        store.siteSpeciesValues = CONSTANTS.SITE_SPECIES_VALUES.COMPUTED
      })
      cy.then(() => {
        useModelParameterStore().siteSpeciesValues = CONSTANTS.SITE_SPECIES_VALUES.SUPPLIED
      })
      // Cypress retries until Vue flushes the watcher
      cy.get('tbody tr').eq(0).find('.site-spin-field').eq(0).find('input')
        .should('have.attr', 'placeholder', CONSTANTS.SPECIAL_INDICATORS.NA)
      cy.get('tbody tr').eq(0).find('.site-spin-field').eq(1).find('input')
        .should('have.attr', 'placeholder', CONSTANTS.SPECIAL_INDICATORS.NA)
    })
  })
})
