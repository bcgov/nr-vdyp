import { mount } from 'cypress/vue'
import { createVuetify } from 'vuetify'
import 'vuetify/styles'
import ReportConfiguration from './ReportConfiguration.vue'
import { CONSTANTS } from '@/constants'

describe('ReportConfiguration.vue', () => {
  const vuetify = createVuetify()

  beforeEach(() => {
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
    startingAge: 10,
    finishingAge: 100,
    ageIncrement: 5,
    volumeReported: [CONSTANTS.VOLUME_REPORTED.WHOLE_STEM],
    includeInReport: [CONSTANTS.INCLUDE_IN_REPORT.COMPUTED_MAI],
    projectionType: CONSTANTS.PROJECTION_TYPE.VOLUME,
    reportTitle: 'Test Report',
    isDisabled: false,
  }

  it('renders correctly with initial props', () => {
    mount(ReportConfiguration, {
      props,
      global: {
        plugins: [vuetify],
      },
    })

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

    cy.get('input[type="string"]').should('have.value', props.reportTitle)
  })

  it('emits events when input values are changed', () => {
    const onUpdateSpy = cy.spy().as('updateSpy')

    mount(ReportConfiguration, {
      props: {
        ...props,
        'onUpdate:startingAge': onUpdateSpy,
        'onUpdate:finishingAge': onUpdateSpy,
        'onUpdate:ageIncrement': onUpdateSpy,
        'onUpdate:volumeReported': onUpdateSpy,
        'onUpdate:includeInReport': onUpdateSpy,
        'onUpdate:projectionType': onUpdateSpy,
        'onUpdate:reportTitle': onUpdateSpy,
      },
      global: {
        plugins: [vuetify],
      },
    })

    // Update "Starting Age"
    cy.get('input[id="startingAge"]').as('startingAgeInput')
    cy.get('@startingAgeInput').clear()
    cy.get('@startingAgeInput').type('15')
    cy.get('@updateSpy').should('have.been.calledWith', 15)

    // Update "Finishing Age"
    cy.get('input[id="startingAge"]').as('finishingAgeInput')
    cy.get('@finishingAgeInput').clear()
    cy.get('@finishingAgeInput').type('120')
    cy.get('@updateSpy').should('have.been.calledWith', 120)

    // Update "Age Increment"
    cy.get('input[id="startingAge"]').as('ageIncrement')
    cy.get('@ageIncrement').clear()
    cy.get('@ageIncrement').type('10')
    cy.get('@updateSpy').should('have.been.calledWith', 10)

    // Update "Report Title"
    cy.get('input[id="reportTitle"]').as('reportTitleInput')
    cy.get('@reportTitleInput').clear()
    cy.get('@reportTitleInput').type('Updated Report Title')
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

    cy.get('.v-select').click() // Select opens the dropdown
    cy.get('.v-list-item')
      .contains(CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS) // Match the dropdown item by text
      .click({ force: true }) // Force click in case the item is hidden
    cy.get('.v-select input').should(
      'have.value',
      CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS,
    )

    // Under "Volumes Reported", make sure the "Close Utilization" checkbox is checked
    cy.contains('.v-input', CONSTANTS.VOLUME_REPORTED.CLOSE_UTIL)
      .find('input[type="checkbox"]')
      .should('be.checked')

    // Verify that the other checkboxes in "Volumes Reported" are reset
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

    // Ensure that all checkboxes in "Volumes Reported" are Disabled
    cy.get('[data-testid="volume-reported"] input[type="checkbox"]').should(
      'be.disabled',
    )

    // Make sure the "Computed MAI" checkbox under "Include in Report" is Disabled
    cy.contains('.v-input', CONSTANTS.INCLUDE_IN_REPORT.COMPUTED_MAI)
      .find('input[type="checkbox"]')
      .should('be.disabled')
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

    // Make sure the "Culmination Values" checkbox under "Include in Report" is enabled
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

    // Set "Starting Age" to 11 (keep Finishing Age at 300)
    cy.get('input[id="startingAge"]').clear()
    cy.get('input[id="startingAge"]').type('11')

    // Make sure the "Culmination Values" checkbox under "Include in Report" is disabled
    cy.contains('.v-input', CONSTANTS.INCLUDE_IN_REPORT.CULMINATION_VALUES)
      .find('input[type="checkbox"]')
      .should('be.disabled')

    cy.get('input[id="startingAge"]').clear()
    cy.get('input[id="startingAge"]').type('10')
    cy.get('input[id="finishingAge"]').clear()
    cy.get('input[id="finishingAge"]').type('299')

    // Make sure the "ulmination Values" checkbox under "Include in Report" is disabled
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

    // Verify that all inputs are disabled
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

    // Verify that all inputs are enabled
    cy.get('.v-field__input input').should('not.be.disabled')
    cy.get('.v-select input').should('not.be.disabled')
    // cy.get('.v-checkbox input[type="checkbox"]').should('not.be.disabled')
  })
})
