import SpeciesListInput from './SpeciesListInput.vue'

describe('SpeciesListInput.vue', () => {
  const sampleSpeciesList = [
    { species: 'FD', percent: '60.0' },
    { species: 'PL', percent: '40.0' },
  ]

  it('shows empty state when speciesList is empty', () => {
    cy.mountWithVuetify(SpeciesListInput, {
      props: { speciesList: [], isConfirmEnabled: true },
    })

    cy.contains('No Species added.').should('be.visible')
    cy.contains('Add Species').should('be.visible')
  })

  it('renders species cards when speciesList is not empty', () => {
    cy.mountWithVuetify(SpeciesListInput, {
      props: { speciesList: sampleSpeciesList, isConfirmEnabled: true },
    })

    cy.get('.species-cards-grid').should('be.visible')
    cy.get('.species-cards-grid').children().should('have.length', 2)
  })

  it('does not show empty state when speciesList has items', () => {
    cy.mountWithVuetify(SpeciesListInput, {
      props: { speciesList: sampleSpeciesList, isConfirmEnabled: true },
    })

    cy.contains('No Species added.').should('not.exist')
  })

  it('disables Add Species button when isConfirmEnabled is false', () => {
    cy.mountWithVuetify(SpeciesListInput, {
      props: { speciesList: [], isConfirmEnabled: false },
    })

    cy.contains('Add Species').should('be.visible')
    cy.contains('button', 'Add Species').should('be.disabled')
  })

  it('emits request-add when Add Species button is clicked', () => {
    const onRequestAddSpy = cy.spy().as('requestAddSpy')

    cy.mountWithVuetify(SpeciesListInput, {
      props: {
        speciesList: [],
        isConfirmEnabled: true,
        'onRequest-add': onRequestAddSpy,
      },
    })

    cy.contains('Add Species').click()
    cy.get('@requestAddSpy').should('have.been.calledOnce')
  })

  it('emits update:speciesList when a species card is deleted', () => {
    const onUpdateSpy = cy.spy().as('updateSpy')

    cy.mountWithVuetify(SpeciesListInput, {
      props: {
        speciesList: sampleSpeciesList,
        isConfirmEnabled: true,
        'onUpdate:speciesList': onUpdateSpy,
      },
    })

    cy.get('.delete-btn').first().click()
    cy.get('@updateSpy').should('have.been.calledOnce')
  })

  it('emits update:speciesList with one fewer item after delete', () => {
    const emitted: unknown[][] = []

    cy.mountWithVuetify(SpeciesListInput, {
      props: {
        speciesList: sampleSpeciesList,
        isConfirmEnabled: true,
        'onUpdate:speciesList': (...args: unknown[]) => emitted.push(args),
      },
    })

    cy.get('.delete-btn').first().click()
    cy.then(() => {
      expect(emitted).to.have.length(1)
      expect(emitted[0][0]).to.have.length(1)
    })
  })

  it('updates when speciesList prop changes', () => {
    cy.mountWithVuetify(SpeciesListInput, {
      props: { speciesList: [], isConfirmEnabled: true },
    }).then(({ wrapper }) => {
      cy.contains('No Species added.').should('be.visible')
      wrapper.setProps({ speciesList: sampleSpeciesList })
      cy.get('.species-cards-grid').children().should('have.length', 2)
    })
  })
})
