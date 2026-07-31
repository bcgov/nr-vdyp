import AppButton from './AppButton.vue'

describe('<AppButton />', () => {
  it('renders with default props', () => {
    cy.mount(AppButton, {
      props: {
        label: 'Button',
      },
    })

    cy.get('button').should('exist')
    cy.get('button').should('contain', 'Button')
    cy.get('button').should('have.class', 'bcds-button')
    cy.get('button').should('have.class', 'primary')
    cy.get('button').should('have.class', 'medium')
  })

  it('renders each variant', () => {
    const variants = ['primary', 'secondary', 'tertiary', 'link', 'danger'] as const

    variants.forEach((variant) => {
      cy.mount(AppButton, {
        props: {
          label: `${variant} Button`,
          variant,
        },
      })

      cy.get('button').should('have.class', variant)
      cy.get('button').should('contain', `${variant} Button`)
    })
  })

  it('renders different sizes', () => {
    const sizes = ['xsmall', 'small', 'medium', 'large'] as const

    sizes.forEach((size) => {
      cy.mount(AppButton, {
        props: {
          label: `${size} Button`,
          size,
        },
      })

      cy.get('button').should('have.class', size)
    })
  })

  it('renders as disabled when isDisabled is true', () => {
    cy.mount(AppButton, {
      props: {
        label: 'Disabled Button',
        isDisabled: true,
      },
    })

    cy.get('button').should('be.disabled')
    cy.get('button').should('have.attr', 'data-disabled')
  })

  it('renders with left icon', () => {
    cy.mount(AppButton, {
      props: {
        label: 'Button with Icon',
        mdiName: 'mdi-check',
        iconPosition: 'left',
      },
    })

    cy.get('button').should('contain', 'Button with Icon')
    cy.get('.button-icon-left').should('exist')
  })

  it('renders with right icon', () => {
    cy.mount(AppButton, {
      props: {
        label: 'Button with Icon',
        mdiName: 'mdi-arrow-right',
        iconPosition: 'right',
      },
    })

    cy.get('button').should('contain', 'Button with Icon')
    cy.get('.button-icon-right').should('exist')
  })

  it('emits click event when clicked', () => {
    const onClickSpy = cy.spy().as('onClickSpy')

    cy.mount(AppButton, {
      props: {
        label: 'Clickable Button',
        onClick: onClickSpy,
      },
    })

    cy.get('button').click()

    cy.get('@onClickSpy').should('have.been.calledOnceWith', 1)
  })

  it('does not emit click event when disabled', () => {
    const onClickSpy = cy.spy().as('onClickSpy')

    cy.mount(AppButton, {
      props: {
        label: 'Disabled Button',
        isDisabled: true,
        onClick: onClickSpy,
      },
    })

    cy.get('button').should('be.disabled')

    cy.get('@onClickSpy').should('not.have.been.called')
  })

  it('combines multiple props correctly', () => {
    cy.mount(AppButton, {
      props: {
        label: 'Complex Button',
        variant: 'danger',
        size: 'large',
        mdiName: 'mdi-alert',
        iconPosition: 'left',
      },
    })

    cy.get('button').should('have.class', 'bcds-button')
    cy.get('button').should('have.class', 'danger')
    cy.get('button').should('have.class', 'large')
    cy.get('.button-icon-left').should('exist')
  })
})
