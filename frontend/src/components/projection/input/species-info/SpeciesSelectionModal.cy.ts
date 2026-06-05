import SpeciesSelectionModal from './SpeciesSelectionModal.vue'

describe('SpeciesSelectionModal.vue', () => {
  const defaultProps = {
    modelValue: true,
    existingSpecies: [],
    maxSpecies: 6,
  }

  it('renders the modal title when open', () => {
    cy.mountWithVuetify(SpeciesSelectionModal, { props: defaultProps })

    cy.contains('Select Species for Calculation').should('be.visible')
  })

  it('shows the species list', () => {
    cy.mountWithVuetify(SpeciesSelectionModal, { props: defaultProps })

    cy.get('.species-row').should('have.length.greaterThan', 0)
  })

  it('shows existing species as pre-selected', () => {
    cy.mountWithVuetify(SpeciesSelectionModal, {
      props: { ...defaultProps, existingSpecies: ['FD', 'PL'] },
    })

    cy.contains('2 Selected').should('be.visible')
    cy.get('.species-row--selected').should('have.length', 2)
  })

  it('shows 0 Selected when no existing species', () => {
    cy.mountWithVuetify(SpeciesSelectionModal, { props: defaultProps })

    cy.contains('0 Selected').should('be.visible')
  })

  it('shows the max species limit text', () => {
    cy.mountWithVuetify(SpeciesSelectionModal, {
      props: { ...defaultProps, maxSpecies: 4 },
    })

    cy.contains('Select Up to 4 Species').should('be.visible')
  })

  it('selects a species on row click and increments selected count', () => {
    cy.mountWithVuetify(SpeciesSelectionModal, { props: defaultProps })

    cy.get('.species-row').first().click()
    cy.contains('1 Selected').should('be.visible')
    cy.get('.species-row--selected').should('have.length', 1)
  })

  it('deselects a species when clicking a selected row', () => {
    cy.mountWithVuetify(SpeciesSelectionModal, {
      props: { ...defaultProps, existingSpecies: ['FD'] },
    })

    cy.get('.species-row--selected').first().click()
    cy.contains('0 Selected').should('be.visible')
    cy.get('.species-row--selected').should('have.length', 0)
  })

  it('does not select more species than maxSpecies allows', () => {
    cy.mountWithVuetify(SpeciesSelectionModal, {
      props: { ...defaultProps, existingSpecies: ['FD', 'PL', 'AC', 'AT', 'B', 'BA'], maxSpecies: 6 },
    })

    cy.contains('6 Selected').should('be.visible')
    cy.get('.species-row:not(.species-row--selected)').first().click()
    cy.contains('6 Selected').should('be.visible')
  })

  it('applies species-row--at-limit class to unselected rows when at limit', () => {
    cy.mountWithVuetify(SpeciesSelectionModal, {
      props: { ...defaultProps, existingSpecies: ['FD', 'PL', 'AC', 'AT', 'B', 'BA'], maxSpecies: 6 },
    })

    cy.get('.species-row--at-limit').should('have.length.greaterThan', 0)
  })

  it('emits update:modelValue false when Cancel is clicked', () => {
    const onUpdateSpy = cy.spy().as('updateSpy')

    cy.mountWithVuetify(SpeciesSelectionModal, {
      props: { ...defaultProps, 'onUpdate:modelValue': onUpdateSpy },
    })

    cy.contains('button', 'Cancel').click({ force: true })
    cy.get('@updateSpy').should('have.been.calledOnceWith', false)
  })

  it('emits update:modelValue false when close icon is clicked', () => {
    const onUpdateSpy = cy.spy().as('updateSpy')

    cy.mountWithVuetify(SpeciesSelectionModal, {
      props: { ...defaultProps, 'onUpdate:modelValue': onUpdateSpy },
    })

    cy.get('[aria-label="Close"]').click()
    cy.get('@updateSpy').should('have.been.calledOnceWith', false)
  })

  it('emits confirm with selected species when Confirm is clicked', () => {
    const onConfirmSpy = cy.spy().as('confirmSpy')

    cy.mountWithVuetify(SpeciesSelectionModal, {
      props: { ...defaultProps, existingSpecies: ['FD'], onConfirm: onConfirmSpy },
    })

    cy.contains('button', 'Confirm').click({ force: true })
    cy.get('@confirmSpy').should('have.been.calledOnce')
    cy.get('@confirmSpy').should('have.been.calledWith', ['FD'])
  })

  it('emits update:modelValue false when Confirm is clicked', () => {
    const onUpdateSpy = cy.spy().as('updateSpy')

    cy.mountWithVuetify(SpeciesSelectionModal, {
      props: { ...defaultProps, 'onUpdate:modelValue': onUpdateSpy },
    })

    cy.contains('button', 'Confirm').click({ force: true })
    cy.get('@updateSpy').should('have.been.calledOnceWith', false)
  })
})
