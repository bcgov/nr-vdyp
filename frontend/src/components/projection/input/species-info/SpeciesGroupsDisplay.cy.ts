import SpeciesGroupsDisplay from './SpeciesGroupsDisplay.vue'
import type { SpeciesGroup } from '@/interfaces/interfaces'

describe('SpeciesGroupsDisplay.vue', () => {
  const sampleGroups: SpeciesGroup[] = [
    { group: 'FD', percent: '50.0', siteSpecies: 'FD' },
    { group: 'PL', percent: '30.0', siteSpecies: 'PLI' },
    { group: 'HW', percent: '20.0', siteSpecies: 'HW' },
  ]

  it('renders nothing when speciesGroups is empty', () => {
    cy.mountWithVuetify(SpeciesGroupsDisplay, { props: { speciesGroups: [] } })

    cy.get('[data-testid="species-groups-container"]').should('not.exist')
  })

  it('renders the container when speciesGroups has items', () => {
    cy.mountWithVuetify(SpeciesGroupsDisplay, { props: { speciesGroups: sampleGroups } })

    cy.get('[data-testid="species-groups-container"]').should('exist')
  })

  it('renders a row for each unique species group', () => {
    cy.mountWithVuetify(SpeciesGroupsDisplay, { props: { speciesGroups: sampleGroups } })

    cy.get('[data-testid="species-group-row"]').should('have.length', 3)
  })

  it('displays correct group codes in the group column', () => {
    cy.mountWithVuetify(SpeciesGroupsDisplay, { props: { speciesGroups: sampleGroups } })

    cy.get('[data-testid="species-group-column"]').eq(0).should('contain.text', 'FD')
    cy.get('[data-testid="species-group-column"]').eq(1).should('contain.text', 'PL')
    cy.get('[data-testid="species-group-column"]').eq(2).should('contain.text', 'HW')
  })

  it('displays correct site species in the site species column', () => {
    cy.mountWithVuetify(SpeciesGroupsDisplay, { props: { speciesGroups: sampleGroups } })

    cy.get('[data-testid="site-species-column"]').eq(0).should('contain.text', 'FD')
    cy.get('[data-testid="site-species-column"]').eq(1).should('contain.text', 'PLI')
    cy.get('[data-testid="site-species-column"]').eq(2).should('contain.text', 'HW')
  })

  it('displays correct percentage for each group', () => {
    cy.mountWithVuetify(SpeciesGroupsDisplay, { props: { speciesGroups: sampleGroups } })

    cy.get('[data-testid="species-group-percent-column"]').eq(0).should('contain.text', '50.0%')
    cy.get('[data-testid="species-group-percent-column"]').eq(1).should('contain.text', '30.0%')
    cy.get('[data-testid="species-group-percent-column"]').eq(2).should('contain.text', '20.0%')
  })

  it('shows correct group count in the header', () => {
    cy.mountWithVuetify(SpeciesGroupsDisplay, { props: { speciesGroups: sampleGroups } })

    cy.get('thead').should('contain.text', 'Species Group (3)')
  })

  it('shows correct site species count in the header', () => {
    cy.mountWithVuetify(SpeciesGroupsDisplay, { props: { speciesGroups: sampleGroups } })

    cy.get('thead').should('contain.text', 'Site Species (3)')
  })

  it('shows the correct total percent in the footer', () => {
    cy.mountWithVuetify(SpeciesGroupsDisplay, { props: { speciesGroups: sampleGroups } })

    cy.get('tfoot').should('contain.text', '100.0%')
  })

  it('merges multiple site species under the same group', () => {
    const groups: SpeciesGroup[] = [
      { group: 'PL', percent: '40.0', siteSpecies: 'PLI' },
      { group: 'PL', percent: '20.0', siteSpecies: 'PLC' },
    ]
    cy.mountWithVuetify(SpeciesGroupsDisplay, { props: { speciesGroups: groups } })

    cy.get('[data-testid="species-group-row"]').should('have.length', 1)
    cy.get('[data-testid="site-species-column"]').should('contain.text', 'PLI, PLC')
    cy.get('[data-testid="species-group-percent-column"]').should('contain.text', '60.0%')
  })

  it('shows the "Species Summary" title', () => {
    cy.mountWithVuetify(SpeciesGroupsDisplay, { props: { speciesGroups: sampleGroups } })

    cy.get('.summary-title').should('contain.text', 'Species Summary')
  })
})
