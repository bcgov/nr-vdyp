import { mount } from 'cypress/vue'
import { createVuetify } from 'vuetify'
import 'vuetify/styles'
import ReportConfiguration from './ReportConfiguration.vue'
import { CONSTANTS } from '@/constants'
import { useAppStore } from '@/stores/appStore'
import { createPinia, setActivePinia } from 'pinia'

describe('ReportConfiguration.vue', () => {
  const vuetify = createVuetify()

  beforeEach(() => {
    setActivePinia(createPinia())
    const appStore = useAppStore()
    appStore.modelSelection = CONSTANTS.MODEL_SELECTION.INPUT_MODEL_PARAMETERS

    cy.document().then((doc) => {
      const style = doc.createElement('style')
      style.innerHTML = `
        body {
          background-color: rgb(240, 240, 240) !important;
        }
      `
      doc.head.appendChild(style)
    })
  })

  const props = {
    selectedAgeYearRange: CONSTANTS.AGE_YEAR_RANGE.AGE,
    startingAge: 10,
    finishingAge: 300,
    ageIncrement: 5,
    startYear: 2020,
    endYear: 2030,
    yearIncrement: 2,
    volumeReported: [CONSTANTS.VOLUME_REPORTED.WHOLE_STEM],
    includeInReport: [CONSTANTS.INCLUDE_IN_REPORT.COMPUTED_MAI],
    projectionType: CONSTANTS.PROJECTION_TYPE.VOLUME,
    reportTitle: 'Test Report',
    isDisabled: false,
  }

  it('renders correctly with initial props for AGE range', () => {
    mount(ReportConfiguration, {
      props,
      global: {
        plugins: [vuetify],
      },
    })

    cy.get('.v-radio-group', { timeout: 6000 })
      .find('.v-radio', { timeout: 6000 })
      .contains('Age', { matchCase: false })
      .parent()
      .find('input[type="radio"]')
      .should('be.checked')

    cy.get('input[id="startingAge"]').should(
      'have.value',
      props.startingAge.toString(),
    )
    cy.get('input[id="finishingAge"]').should(
      'have.value',
      props.finishingAge.toString(),
    )
    cy.get('input[id="ageIncrement"]').should(
      'have.value',
      props.ageIncrement.toString(),
    )

    cy.get('input[id="startYear"]').should('not.exist')
    cy.get('input[id="endYear"]').should('not.exist')
    cy.get('input[id="yearIncrement"]').should('not.exist')

    for (const value of props.volumeReported) {
      cy.contains('.v-input', value)
        .find('input[type="checkbox"]')
        .should('be.checked')
    }

    for (const value of props.includeInReport) {
      cy.contains('.v-input', value)
        .find('input[type="checkbox"]')
        .should('be.checked')
    }

    cy.get('.v-select').find('input').should('have.value', props.projectionType)

    cy.get('input[id="reportTitle"]').should('have.value', props.reportTitle)
  })

  it('renders correctly with initial props for YEAR range', () => {
    mount(ReportConfiguration, {
      props: {
        ...props,
        selectedAgeYearRange: CONSTANTS.AGE_YEAR_RANGE.YEAR,
      },
      global: {
        plugins: [vuetify],
      },
    })

    cy.get('.v-radio-group', { timeout: 6000 })
      .find('.v-radio', { timeout: 6000 })
      .contains('Year', { matchCase: false })
      .parent()
      .find('input[type="radio"]')
      .should('be.checked')

    cy.get('input[id="startYear"]').should(
      'have.value',
      props.startYear.toString(),
    )
    cy.get('input[id="endYear"]').should('have.value', props.endYear.toString())
    cy.get('input[id="yearIncrement"]').should(
      'have.value',
      props.yearIncrement.toString(),
    )

    cy.get('input[id="startingAge"]').should('not.exist')
    cy.get('input[id="finishingAge"]').should('not.exist')
    cy.get('input[id="ageIncrement"]').should('not.exist')

    for (const value of props.volumeReported) {
      cy.contains('.v-input', value)
        .find('input[type="checkbox"]')
        .should('be.checked')
    }

    for (const value of props.includeInReport) {
      cy.contains('.v-input', value)
        .find('input[type="checkbox"]')
        .should('be.checked')
    }

    cy.get('.v-select').find('input').should('have.value', props.projectionType)

    cy.get('input[id="reportTitle"]').should('have.value', props.reportTitle)
  })

  it('emits events when input values are changed for AGE range', () => {
    const onUpdateSpy = cy.spy().as('updateSpy')

    mount(ReportConfiguration, {
      props: {
        ...props,
        'onUpdate:selectedAgeYearRange': onUpdateSpy,
        'onUpdate:startingAge': onUpdateSpy,
        'onUpdate:finishingAge': onUpdateSpy,
        'onUpdate:ageIncrement': onUpdateSpy,
        'onUpdate:startYear': onUpdateSpy,
        'onUpdate:endYear': onUpdateSpy,
        'onUpdate:yearIncrement': onUpdateSpy,
        'onUpdate:volumeReported': onUpdateSpy,
        'onUpdate:includeInReport': onUpdateSpy,
        'onUpdate:projectionType': onUpdateSpy,
        'onUpdate:reportTitle': onUpdateSpy,
      },
      global: {
        plugins: [vuetify],
      },
    })

    cy.get('input[id="startingAge"]').clear()
    cy.get('input[id="startingAge"]').type('15')
    cy.get('@updateSpy').should('have.been.calledWith', 15)

    cy.get('input[id="finishingAge"]').clear()
    cy.get('input[id="finishingAge"]').type('120')
    cy.get('@updateSpy').should('have.been.calledWith', 120)

    cy.get('input[id="ageIncrement"]').clear()
    cy.get('input[id="ageIncrement"]').type('10')
    cy.get('@updateSpy').should('have.been.calledWith', 10)

    cy.get('.v-select').click()
    cy.get('.v-list-item')
      .contains(CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS)
      .click({ force: true })
    cy.get('@updateSpy').should(
      'have.been.calledWith',
      CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS,
    )

    cy.get('input[id="reportTitle"]').clear()
    cy.get('input[id="reportTitle"]').type('Updated Report Title')
    cy.get('@updateSpy').should('have.been.calledWith', 'Updated Report Title')

    cy.get('.v-radio-group')
      .find('.v-radio')
      .contains('Year', { matchCase: false })
      .parent()
      .find('input')
      .click()
    cy.get('@updateSpy').should(
      'have.been.calledWith',
      CONSTANTS.AGE_YEAR_RANGE.YEAR,
    )
  })

  it('emits events when input values are changed for YEAR range', () => {
    const onUpdateSpy = cy.spy().as('updateSpy')

    mount(ReportConfiguration, {
      props: {
        ...props,
        selectedAgeYearRange: CONSTANTS.AGE_YEAR_RANGE.YEAR,
        'onUpdate:selectedAgeYearRange': onUpdateSpy,
        'onUpdate:startingAge': onUpdateSpy,
        'onUpdate:finishingAge': onUpdateSpy,
        'onUpdate:ageIncrement': onUpdateSpy,
        'onUpdate:startYear': onUpdateSpy,
        'onUpdate:endYear': onUpdateSpy,
        'onUpdate:yearIncrement': onUpdateSpy,
        'onUpdate:volumeReported': onUpdateSpy,
        'onUpdate:includeInReport': onUpdateSpy,
        'onUpdate:projectionType': onUpdateSpy,
        'onUpdate:reportTitle': onUpdateSpy,
      },
      global: {
        plugins: [vuetify],
      },
    })

    cy.get('.v-radio-group')
      .find('.v-radio')
      .contains('Age', { matchCase: false })
      .parent()
      .find('input')
      .click()
    cy.get('@updateSpy').should(
      'have.been.calledWith',
      CONSTANTS.AGE_YEAR_RANGE.AGE,
    )

    cy.get('input[id="startingAge"]').clear()
    cy.get('input[id="startingAge"]').type('1')
    cy.get('@updateSpy').should('have.been.calledWith', 1)

    cy.get('input[id="finishingAge"]').clear()
    cy.get('input[id="finishingAge"]').type('200')
    cy.get('@updateSpy').should('have.been.calledWith', 200)

    cy.get('input[id="ageIncrement"]').clear()
    cy.get('input[id="ageIncrement"]').type('3')
    cy.get('@updateSpy').should('have.been.calledWith', 3)

    cy.get('.v-select').click()
    cy.get('.v-list-item')
      .contains(CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS)
      .click({ force: true })
    cy.get('@updateSpy').should(
      'have.been.calledWith',
      CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS,
    )

    cy.get('input[id="reportTitle"]').clear()
    cy.get('input[id="reportTitle"]').type('Updated Report Title')
    cy.get('@updateSpy').should('have.been.calledWith', 'Updated Report Title')
  })

  it('forces Close Utilization checkbox checked when Projection Type is CFS_BIOMASS', () => {
    mount(ReportConfiguration, {
      props: {
        ...props,
      },
      global: {
        plugins: [vuetify],
      },
    })

    cy.get('.v-select').click()
    cy.get('.v-list-item')
      .contains(CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS)
      .click({ force: true })
    cy.get('.v-select input').should(
      'have.value',
      CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS,
    )

    cy.contains('.v-input', CONSTANTS.VOLUME_REPORTED.CLOSE_UTIL)
      .find('input[type="checkbox"]')
      .should('be.checked')

    cy.get('[data-testid="volume-reported"] input[type="checkbox"]').each(
      ($el) => {
        cy.wrap($el)
          .closest('.v-input')
          .invoke('text')
          .then((parentText) => {
            if (!parentText.includes(CONSTANTS.VOLUME_REPORTED.CLOSE_UTIL)) {
              cy.wrap($el).should('not.be.checked')
            }
          })
      },
    )

    cy.get('[data-testid="volume-reported"] input[type="checkbox"]').should(
      'be.disabled',
    )
  })

  it('enables Culmination Values checkbox when Starting Age <= 10 and Finishing Age >= 300', () => {
    mount(ReportConfiguration, {
      props: {
        ...props,
      },
      global: {
        plugins: [vuetify],
      },
    })

    cy.get('input[id="startingAge"]').clear()
    cy.get('input[id="startingAge"]').type('10')

    cy.get('input[id="finishingAge"]').clear()
    cy.get('input[id="finishingAge"]').type('300')

    cy.contains('.v-input', CONSTANTS.INCLUDE_IN_REPORT.CULMINATION_VALUES)
      .find('input[type="checkbox"]')
      .should('not.be.disabled')
  })

  it('disables Culmination Values checkbox when Starting Age > 10 or Finishing Age < 300', () => {
    mount(ReportConfiguration, {
      props: {
        ...props,
      },
      global: {
        plugins: [vuetify],
      },
    })

    cy.get('input[id="startingAge"]').clear()
    cy.get('input[id="startingAge"]').type('11')

    cy.contains('.v-input', CONSTANTS.INCLUDE_IN_REPORT.CULMINATION_VALUES)
      .find('input[type="checkbox"]')
      .should('be.disabled')

    cy.get('input[id="startingAge"]').clear()
    cy.get('input[id="startingAge"]').type('10')
    cy.get('input[id="finishingAge"]').clear()
    cy.get('input[id="finishingAge"]').type('299')

    cy.contains('.v-input', CONSTANTS.INCLUDE_IN_REPORT.CULMINATION_VALUES)
      .find('input[type="checkbox"]')
      .should('be.disabled')
  })

  it('disables all inputs when "isDisabled" is true', () => {
    mount(ReportConfiguration, {
      props: {
        ...props,
        isDisabled: true,
      },
      global: {
        plugins: [vuetify],
      },
    })

    cy.get('.v-field__input input').should('be.disabled')
    cy.get('.v-select input').should('be.disabled')
    cy.get('.v-checkbox input[type="checkbox"]').should('be.disabled')
  })

  it('enables all inputs when "isDisabled" is false', () => {
    mount(ReportConfiguration, {
      props: {
        ...props,
      },
      global: {
        plugins: [vuetify],
      },
    })

    cy.get('.v-field__input input').should('not.be.disabled')
    cy.get('.v-select input').should('not.be.disabled')
    cy.get('.v-checkbox input[type="checkbox"]').should('not.be.disabled')
  })
})
