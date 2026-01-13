import AppProgressCircular from './AppProgressCircular.vue'

describe('<AppProgressCircular />', () => {
  const defaultProps = {
    isShow: true,
    showMessage: true,
    message: 'Loading...',
  }

  it('renders with default props', () => {
    cy.mount(AppProgressCircular, { props: defaultProps })

    // Check if the progress container exists
    cy.get('.progress-container').should('exist')

    // Check if the message is displayed
    cy.get('.progress-message').should('contain', 'Loading...')

    // Check if Vuetify progress circular is rendered
    cy.get('.v-progress-circular').should('exist')
  })

  it('does not render when isShow is false', () => {
    cy.mount(AppProgressCircular, {
      props: {
        ...defaultProps,
        isShow: false,
      },
    })

    // Ensure the component is hidden
    cy.get('.progress-container').should('not.exist')
  })

  it('does not display message when showMessage is false', () => {
    cy.mount(AppProgressCircular, {
      props: {
        ...defaultProps,
        showMessage: false,
      },
    })

    cy.get('.progress-container').should('exist')
    cy.get('.progress-message').should('not.exist')
  })

  it('displays custom message', () => {
    cy.mount(AppProgressCircular, {
      props: {
        ...defaultProps,
        message: 'Processing your request...',
      },
    })

    cy.get('.progress-container').should('exist')
    cy.get('.progress-message').should('contain', 'Processing your request...')
  })

  it('renders with background when hasBackground is true', () => {
    cy.mount(AppProgressCircular, {
      props: {
        ...defaultProps,
        hasBackground: true,
      },
    })

    cy.get('.progress-container').should('exist')
    cy.get('.progress-container').should('have.class', 'with-background')
  })

  it('renders without background when hasBackground is false', () => {
    cy.mount(AppProgressCircular, {
      props: {
        ...defaultProps,
        hasBackground: false,
      },
    })

    cy.get('.progress-container').should('exist')
    cy.get('.progress-container').should('not.have.class', 'with-background')
  })

  it('renders with custom circle size and width', () => {
    cy.mount(AppProgressCircular, {
      props: {
        ...defaultProps,
        circleSize: 100,
        circleWidth: 10,
      },
    })

    cy.get('.v-progress-circular').should('exist')
    // Vuetify sets size as inline style
    cy.get('.v-progress-circular').should('have.attr', 'style').and('include', '100')
  })
})
