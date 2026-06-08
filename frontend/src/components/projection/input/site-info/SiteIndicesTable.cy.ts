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
  it('renders subtitle and all column headers', () => {
    mountTable(false)
    cy.contains('.text-subtitle-2', 'Site Indices').should('exist')
    cy.get('thead th').eq(0).should('contain.text', 'Site Species')
    cy.get('thead th').eq(1).should('contain.text', 'Computed Value')
    cy.get('thead th').eq(2).should('contain.text', 'Age')
    cy.get('thead th').eq(3).should('contain.text', 'Height in Meters')
    cy.get('thead th').eq(4).should('contain.text', 'BHA Site Index')
  })

  it('renders one row per store entry with correct species code', () => {
    mountTable(false, (store) => {
      store.siteIndexRows = [makeRow('FD'), makeRow('PL'), makeRow('S')]
    })
    cy.get('tbody tr').should('have.length', 3)
    cy.get('tbody tr').eq(0).find('td').eq(0).should('contain.text', 'FD')
    cy.get('tbody tr').eq(1).find('td').eq(0).should('contain.text', 'PL')
  })

  it('disables all inputs when isConfirmEnabled is false', () => {
    mountTable(false, (store) => {
      store.siteIndexRows = [makeRow('FD')]
    })
    cy.get('.v-select.v-input--disabled').should('have.length', 1)
    cy.get('.v-radio-group.v-input--disabled').should('have.length', 1)
    cy.get('tbody tr').eq(0).find('.site-spin-field input').each(($input) => {
      cy.wrap($input).should('be.disabled')
    })
  })

  it('disables select, radio, age, and height when Supplied; BHA remains enabled', () => {
    mountTable(true, (store) => {
      store.siteIndexRows = [makeRow('FD')]
      store.siteSpeciesValues = CONSTANTS.SITE_SPECIES_VALUES.SUPPLIED
    })
    cy.get('.v-select.v-input--disabled').should('have.length', 1)
    cy.get('.v-radio-group.v-input--disabled').should('have.length', 1)
    cy.get('tbody tr').eq(0).find('.site-spin-field').eq(0).find('input').should('be.disabled')
    cy.get('tbody tr').eq(0).find('.site-spin-field').eq(1).find('input').should('be.disabled')
    cy.get('tbody tr').eq(0).find('.site-spin-field').eq(2).find('input').should('not.be.disabled')
  })

  it('enables primary row inputs when Computed + isConfirmEnabled', () => {
    mountTable(true, (store) => {
      store.siteIndexRows = [makeRow('FD')]
      store.siteSpeciesValues = CONSTANTS.SITE_SPECIES_VALUES.COMPUTED
    })
    cy.get('.v-select').first().should('not.have.class', 'v-input--disabled')
    cy.get('.v-radio-group').first().should('not.have.class', 'v-input--disabled')
  })

  it('disables all non-primary row inputs with N/A placeholders when Computed + Volume', () => {
    mountTable(true, (store) => {
      store.siteIndexRows = [makeRow('FD'), makeRow('PL', { age: null, height: null, bhaSiteIndex: null })]
      store.siteSpeciesValues = CONSTANTS.SITE_SPECIES_VALUES.COMPUTED
      store.derivedBy = CONSTANTS.DERIVED_BY.VOLUME
    })
    cy.get('.v-select').eq(1).should('have.class', 'v-input--disabled')
    cy.get('tbody tr').eq(1).find('.site-spin-field input').each(($input) => {
      cy.wrap($input).should('be.disabled')
      cy.wrap($input).should('have.attr', 'placeholder', CONSTANTS.SPECIAL_INDICATORS.NA)
    })
  })

  it('shows Calc. placeholder for the field matching the selected computedValue', () => {
    mountTable(true, (store) => {
      store.siteIndexRows = [
        makeRow('FD', { age: null, computedValue: CONSTANTS.COMPUTED_VALUE.TOTAL_AGE }),
        makeRow('PL', { height: null, computedValue: CONSTANTS.COMPUTED_VALUE.HEIGHT }),
        makeRow('S', { bhaSiteIndex: null, computedValue: CONSTANTS.COMPUTED_VALUE.BHA_SITE_INDEX }),
      ]
      store.siteSpeciesValues = CONSTANTS.SITE_SPECIES_VALUES.COMPUTED
    })
    cy.get('tbody tr').eq(0).find('.site-spin-field').eq(0).find('input')
      .should('have.attr', 'placeholder', CONSTANTS.SPECIAL_INDICATORS.CALC)
    cy.get('tbody tr').eq(1).find('.site-spin-field').eq(1).find('input')
      .should('have.attr', 'placeholder', CONSTANTS.SPECIAL_INDICATORS.CALC)
    cy.get('tbody tr').eq(2).find('.site-spin-field').eq(2).find('input')
      .should('have.attr', 'placeholder', CONSTANTS.SPECIAL_INDICATORS.CALC)
  })

  it('shows N/A placeholder for age and height when Supplied', () => {
    mountTable(true, (store) => {
      store.siteIndexRows = [makeRow('FD', { age: null, height: null })]
      store.siteSpeciesValues = CONSTANTS.SITE_SPECIES_VALUES.SUPPLIED
    })
    cy.get('tbody tr').eq(0).find('.site-spin-field').eq(0).find('input')
      .should('have.attr', 'placeholder', CONSTANTS.SPECIAL_INDICATORS.NA)
    cy.get('tbody tr').eq(0).find('.site-spin-field').eq(1).find('input')
      .should('have.attr', 'placeholder', CONSTANTS.SPECIAL_INDICATORS.NA)
  })

  it('resets age and height to N/A when switching to Supplied', () => {
    mountTable(true, (store) => {
      store.siteIndexRows = [makeRow('FD')]
      store.siteSpeciesValues = CONSTANTS.SITE_SPECIES_VALUES.COMPUTED
    })
    cy.then(() => {
      useModelParameterStore().siteSpeciesValues = CONSTANTS.SITE_SPECIES_VALUES.SUPPLIED
    })
    cy.get('tbody tr').eq(0).find('.site-spin-field').eq(0).find('input')
      .should('have.attr', 'placeholder', CONSTANTS.SPECIAL_INDICATORS.NA)
    cy.get('tbody tr').eq(0).find('.site-spin-field').eq(1).find('input')
      .should('have.attr', 'placeholder', CONSTANTS.SPECIAL_INDICATORS.NA)
  })
})
