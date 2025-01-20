import { mount } from 'cypress/vue'
import { createVuetify } from 'vuetify'
import 'vuetify/styles'
import SpeciesGroupsDisplay from './SpeciesGroupsDisplay.vue'

const vuetify = createVuetify()

describe('SpeciesGroupsDisplay.vue', () => {
  beforeEach(() => {
    cy.viewport(1024, 768)

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
      { group: 'PL - Lodgepole Pine', percent: '30.0', siteSpecies: 'PL' },
      { group: 'AC - Poplar', percent: '30.0', siteSpecies: 'AC' },
      { group: 'H - Hemlock', percent: '30.0', siteSpecies: 'H' },
      { group: 'S - Spruce', percent: '10.0', siteSpecies: 'S' },
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
          .should('equal', 'PL - Lodgepole Pine') // Group name
        cy.get('[data-testid="species-group-percent-column"] input')
          .invoke('val')
          .should('equal', '30.0') // Percent
        cy.get('[data-testid="site-species-column"] input')
          .invoke('val')
          .should('equal', 'PL') // Site species
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
