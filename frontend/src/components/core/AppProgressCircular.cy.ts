import AppProgressCircular from './AppProgressCircular.vue'

describe('<AppProgressCircular />', () => {
  const defaultProps = {
    isShow: true,
    showMessage: true,
    message: 'Loading...',
  }

  it('renders with default props', () => {
    cy.mount(AppProgressCircular, { props: defaultProps })

    // Check if the progress wrapper exists
    cy.get('.centered-progress').should('exist')

    // Check if the message is displayed
    cy.get('.message').should('contain', 'Loading...')
  })

  it('does not render when isShow is false', () => {
    cy.mount(AppProgressCircular, {
      props: {
        ...defaultProps,
        isShow: false,
      },
    })

    // Ensure the component is hidden
    cy.get('.centered-progress').should('not.exist')
  })

  it('does not display message when showMessage is false', () => {
    cy.mount(AppProgressCircular, {
      props: {
        ...defaultProps,
        showMessage: false,
      },
    })

    cy.get('.centered-progress').should('exist')
    cy.get('.message').should('not.exist')
  })

  it('handles missing optional props gracefully', () => {
    cy.mount(AppProgressCircular, {
      props: {
        ...defaultProps,
        message: 'Default message test',
      },
    })

    cy.get('.centered-progress').should('exist')
    cy.get('.message').should('contain', 'Default message test')
  })

  it('renders with default props', () => {
    cy.mount(AppProgressCircular, { props: defaultProps })
    cy.get('.centered-progress').should('exist')
    cy.get('.message').should('contain', 'Loading...')
  })
})
