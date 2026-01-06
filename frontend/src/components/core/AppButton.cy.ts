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

  it('renders primary variant', () => {
    cy.mount(AppButton, {
      props: {
        label: 'Primary Button',
        variant: 'primary',
      },
    })

    cy.get('button').should('have.class', 'primary')
    cy.get('button').should('contain', 'Primary Button')
  })

  it('renders secondary variant', () => {
    cy.mount(AppButton, {
      props: {
        label: 'Secondary Button',
        variant: 'secondary',
      },
    })

    cy.get('button').should('have.class', 'secondary')
    cy.get('button').should('contain', 'Secondary Button')
  })

  it('renders tertiary variant', () => {
    cy.mount(AppButton, {
      props: {
        label: 'Tertiary Button',
        variant: 'tertiary',
      },
    })

    cy.get('button').should('have.class', 'tertiary')
    cy.get('button').should('contain', 'Tertiary Button')
  })

  it('renders link variant', () => {
    cy.mount(AppButton, {
      props: {
        label: 'Link Button',
        variant: 'link',
      },
    })

    cy.get('button').should('have.class', 'link')
    cy.get('button').should('contain', 'Link Button')
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

  it('renders danger variant', () => {
    cy.mount(AppButton, {
      props: {
        label: 'Danger Button',
        variant: 'primary',
        danger: true,
      },
    })

    cy.get('button').should('have.class', 'danger')
    cy.get('button').should('contain', 'Danger Button')
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
        leftIcon: 'mdi-check',
      },
    })

    cy.get('button').should('contain', 'Button with Icon')
    cy.get('.button-icon-left').should('exist')
    cy.get('.button-icon-left').should('have.class', 'mdi-check')
  })

  it('renders with right icon', () => {
    cy.mount(AppButton, {
      props: {
        label: 'Button with Icon',
        rightIcon: 'mdi-arrow-right',
      },
    })

    cy.get('button').should('contain', 'Button with Icon')
    cy.get('.button-icon-right').should('exist')
    cy.get('.button-icon-right').should('have.class', 'mdi-arrow-right')
  })

  it('renders icon-only button', () => {
    cy.mount(AppButton, {
      props: {
        leftIcon: 'mdi-close',
        ariaLabel: 'Close',
      },
    })

    cy.get('button').should('have.class', 'icon')
    cy.get('button').should('have.attr', 'aria-label', 'Close')
    cy.get('.button-icon-left').should('exist')
  })

  it('applies aria-label', () => {
    cy.mount(AppButton, {
      props: {
        label: 'Submit',
        ariaLabel: 'Submit form',
      },
    })

    cy.get('button').should('have.attr', 'aria-label', 'Submit form')
  })

  it('uses label as aria-label when ariaLabel is not provided', () => {
    cy.mount(AppButton, {
      props: {
        label: 'Submit',
      },
    })

    cy.get('button').should('have.attr', 'aria-label', 'Submit')
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

  it('applies hover state', () => {
    cy.mount(AppButton, {
      props: {
        label: 'Hover Button',
      },
    })

    // Trigger mouseenter and wait for Vue to update
    cy.get('button').trigger('mouseenter')
    cy.get('button').should('have.attr', 'data-hovered')

    // Trigger mouseleave and verify the attribute is removed
    cy.get('button').trigger('mouseleave')
    cy.get('button').should('not.have.attr', 'data-hovered')
  })

  it('combines multiple props correctly', () => {
    cy.mount(AppButton, {
      props: {
        label: 'Complex Button',
        variant: 'secondary',
        size: 'large',
        danger: true,
        leftIcon: 'mdi-alert',
      },
    })

    cy.get('button').should('have.class', 'bcds-button')
    cy.get('button').should('have.class', 'secondary')
    cy.get('button').should('have.class', 'large')
    cy.get('button').should('have.class', 'danger')
    cy.get('.button-icon-left').should('exist')
  })
})
