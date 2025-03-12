import { mount } from 'cypress/vue'
import { createVuetify } from 'vuetify'
import 'vuetify/styles'
import SpeciesGroupsDisplay from './SpeciesGroupsDisplay.vue'

const vuetify = createVuetify()

describe('SpeciesGroupsDisplay.vue', () => {
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

  const propsWithGroups = {
    speciesGroups: [
      { group: 'PL', percent: '30.0', siteSpecies: 'PL' },
      { group: 'AC', percent: '30.0', siteSpecies: 'AC' },
      { group: 'H', percent: '30.0', siteSpecies: 'H' },
      { group: 'S', percent: '10.0', siteSpecies: 'S' },
    ],
  }

  it('renders correctly with species groups', () => {
    mount(SpeciesGroupsDisplay, {
      props: propsWithGroups,
      global: {
        plugins: [vuetify],
      },
    })

    // Check if the correct number of group rows are rendered
    cy.get('[data-testid="species-group-row"]').should(
      'have.length',
      propsWithGroups.speciesGroups.length,
    )

    // Verify the first group's columns
    cy.get('[data-testid="species-group-row"]')
      .first()
      .within(() => {
        cy.get('[data-testid="species-group-column"] input')
          .invoke('val')
          .should('equal', 'PL') // Group name
        cy.get('[data-testid="species-group-percent-column"] input')
          .invoke('val')
          .should('equal', '30.0') // Percent
        cy.get('[data-testid="site-species-column"] input')
          .invoke('val')
          .should('equal', 'PL') // Site species
      })

    // Verify the second group's columns
    cy.get('[data-testid="species-group-row"]')
      .eq(1)
      .within(() => {
        cy.get('[data-testid="species-group-column"] input')
          .invoke('val')
          .should('equal', 'AC')
        cy.get('[data-testid="species-group-percent-column"] input')
          .invoke('val')
          .should('equal', '30.0')
        cy.get('[data-testid="site-species-column"] input')
          .invoke('val')
          .should('equal', 'AC')
      })

    // Verify the third group's columns
    cy.get('[data-testid="species-group-row"]')
      .eq(2)
      .within(() => {
        cy.get('[data-testid="species-group-column"] input')
          .invoke('val')
          .should('equal', 'H')
        cy.get('[data-testid="species-group-percent-column"] input')
          .invoke('val')
          .should('equal', '30.0')
        cy.get('[data-testid="site-species-column"] input')
          .invoke('val')
          .should('equal', 'H')
      })

    // Verify the fourth group's columns
    cy.get('[data-testid="species-group-row"]')
      .eq(3)
      .within(() => {
        cy.get('[data-testid="species-group-column"] input')
          .invoke('val')
          .should('equal', 'S')
        cy.get('[data-testid="species-group-percent-column"] input')
          .invoke('val')
          .should('equal', '10.0')
        cy.get('[data-testid="site-species-column"] input')
          .invoke('val')
          .should('equal', 'S')
      })
  })

  it('renders correctly without species groups', () => {
    mount(SpeciesGroupsDisplay, {
      props: { speciesGroups: [] },
      global: {
        plugins: [vuetify],
      },
    })

    // Verify the container is empty
    cy.get('[data-testid="species-groups-container"]').should('not.exist')
  })
})
