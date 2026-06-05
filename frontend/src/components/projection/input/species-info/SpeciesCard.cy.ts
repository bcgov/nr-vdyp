import SpeciesCard from './SpeciesCard.vue'

describe('SpeciesCard.vue', () => {
  const defaultProps = {
    speciesCode: 'FD',
    percent: '30.0',
    isDisabled: false,
  }

  it('renders species name, group code, and species code', () => {
    cy.mountWithVuetify(SpeciesCard, { props: defaultProps })

    cy.get('.species-name-text').should('contain.text', 'Douglas Fir')
    cy.get('.species-details-row').should('contain.text', 'Species Group:')
    cy.get('.species-details-row').should('contain.text', 'Site Species:')
    cy.get('.species-details-row').should('contain.text', 'FD')
  })

  it('renders the initial percent value', () => {
    cy.mountWithVuetify(SpeciesCard, { props: defaultProps })

    cy.get('input').should('have.value', '30.0')
  })

  it('falls back to speciesCode when code is not in SPECIES_MAP', () => {
    cy.mountWithVuetify(SpeciesCard, {
      props: { ...defaultProps, speciesCode: 'UNKNOWN' },
    })

    cy.get('.species-name-text').should('contain.text', 'UNKNOWN')
  })

  it('does not exceed max percent (100)', () => {
    cy.mountWithVuetify(SpeciesCard, {
      props: { ...defaultProps, percent: '100.0' },
    })

    cy.get('.spin-up-arrow-button').click({ force: true })
    cy.get('input').should('have.value', '100.0')
  })

  it('does not go below min percent (0)', () => {
    cy.mountWithVuetify(SpeciesCard, {
      props: { ...defaultProps, percent: '0.0' },
    })

    cy.get('.spin-down-arrow-button').click({ force: true })
    cy.get('input').should('have.value', '0.0')
  })

  it('disables input and spin buttons when isDisabled is true', () => {
    cy.mountWithVuetify(SpeciesCard, {
      props: { ...defaultProps, isDisabled: true },
    })

    cy.get('input').should('be.disabled')
    cy.get('.spin-up-arrow-button').should('have.class', 'disabled')
    cy.get('.spin-down-arrow-button').should('have.class', 'disabled')
  })

  it('disables delete button when isDisabled is true', () => {
    cy.mountWithVuetify(SpeciesCard, {
      props: { ...defaultProps, isDisabled: true },
    })

    cy.get('.delete-btn').should('be.disabled')
    cy.get('.delete-btn').should('have.class', 'delete-btn--disabled')
  })

  it('emits delete event when delete button is clicked', () => {
    const onDeleteSpy = cy.spy().as('deleteSpy')

    cy.mountWithVuetify(SpeciesCard, {
      props: { ...defaultProps, onDelete: onDeleteSpy },
    })

    cy.get('.delete-btn').click()
    cy.get('@deleteSpy').should('have.been.calledOnce')
  })

  it('does not emit delete event when disabled and delete button is clicked', () => {
    const onDeleteSpy = cy.spy().as('deleteSpy')

    cy.mountWithVuetify(SpeciesCard, {
      props: { ...defaultProps, isDisabled: true, onDelete: onDeleteSpy },
    })

    cy.get('.delete-btn').click({ force: true })
    cy.get('@deleteSpy').should('not.have.been.called')
  })

  it('emits update:percent when percent value changes via spin buttons', () => {
    const onUpdateSpy = cy.spy().as('updateSpy')

    cy.mountWithVuetify(SpeciesCard, {
      props: { ...defaultProps, 'onUpdate:percent': onUpdateSpy },
    })

    cy.get('.spin-up-arrow-button').click({ force: true })
    cy.get('@updateSpy').should('have.been.called')
  })
})
