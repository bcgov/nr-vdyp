import AppSnackbar from './AppSnackbar.vue'

describe('AppSnackbar.vue', () => {
  beforeEach(() => {
    // Set Cypress preview background color
    cy.document().then((doc) => {
      const style = doc.createElement('style')
      style.innerHTML = `
        body {
          background-color: rgb(240, 240, 240) !important;
        }
        .v-application {
          position: relative !important;
          z-index: 0 !important;
        }
        .v-overlay__content {
          z-index: 9999 !important;
        }
      `
      doc.head.appendChild(style)
    })
  })

  it('displays an informational notification', () => {
    const onCloseSpy = cy.spy().as('onCloseSpy')
    cy.mount(AppSnackbar, {
      props: {
        isVisible: true,
        message: 'This is an informational message.',
        type: 'info',
        location: 'top',
        autoTimeout: false,
        'onUpdate:isVisible': onCloseSpy,
      },
    })

    // Verify snackbar visibility
    cy.get('.v-snackbar__wrapper')
      .should('be.visible')
      .and('contain.text', 'This is an informational message')

    // Verify the icon (outline version)
    cy.get('.app-snackbar__icon').should('have.class', 'mdi-information-outline')

    // Verify snackbar has info class
    cy.get('.app-snackbar').should('have.class', 'info')

    // Close the snackbar and verify close event
    cy.get('.v-snackbar__actions .v-btn').click()
    cy.get('@onCloseSpy').should('have.been.calledWith', false)
  })

  it('displays an error notification', () => {
    const onCloseSpy = cy.spy().as('onCloseSpy')
    cy.mount(AppSnackbar, {
      props: {
        isVisible: true,
        message: 'This is an error message.',
        type: 'error',
        location: 'top',
        autoTimeout: false,
        'onUpdate:isVisible': onCloseSpy,
      },
    })

    // Verify snackbar visibility
    cy.get('.v-snackbar__wrapper')
      .should('be.visible')
      .and('contain.text', 'This is an error message')

    // Verify the icon (outline version)
    cy.get('.app-snackbar__icon').should('have.class', 'mdi-alert-circle-outline')

    // Verify snackbar has error class
    cy.get('.app-snackbar').should('have.class', 'error')

    // Close the snackbar and verify close event
    cy.get('.v-snackbar__actions .v-btn').click()
    cy.get('@onCloseSpy').should('have.been.calledWith', false)
  })

  it('displays a warning notification', () => {
    const onCloseSpy = cy.spy().as('onCloseSpy')
    cy.mount(AppSnackbar, {
      props: {
        isVisible: true,
        message: 'This is a warning message.',
        type: 'warning',
        location: 'top',
        autoTimeout: false,
        'onUpdate:isVisible': onCloseSpy,
      },
    })

    // Verify snackbar visibility
    cy.get('.v-snackbar__wrapper')
      .should('be.visible')
      .and('contain.text', 'This is a warning message')

    // Verify the icon (outline version)
    cy.get('.app-snackbar__icon').should('have.class', 'mdi-alert-outline')

    // Verify snackbar has warning class
    cy.get('.app-snackbar').should('have.class', 'warning')

    // Close the snackbar and verify close event
    cy.get('.v-snackbar__actions .v-btn').click()
    cy.get('@onCloseSpy').should('have.been.calledWith', false)
  })

  it('displays a success notification', () => {
    const onCloseSpy = cy.spy().as('onCloseSpy')
    cy.mount(AppSnackbar, {
      props: {
        isVisible: true,
        message: 'This is a success message.',
        type: 'success',
        location: 'top',
        autoTimeout: false,
        'onUpdate:isVisible': onCloseSpy,
      },
    })

    // Verify snackbar visibility
    cy.get('.v-snackbar__wrapper')
      .should('be.visible')
      .and('contain.text', 'This is a success message')

    // Verify the icon (outline version)
    cy.get('.app-snackbar__icon').should('have.class', 'mdi-check-circle-outline')

    // Verify snackbar has success class
    cy.get('.app-snackbar').should('have.class', 'success')

    // Close the snackbar and verify close event
    cy.get('.v-snackbar__actions .v-btn').click()
    cy.get('@onCloseSpy').should('have.been.calledWith', false)
  })

  it('handles auto timeout', () => {
    const onCloseSpy = cy.spy().as('onCloseSpy')
    cy.mount(AppSnackbar, {
      props: {
        isVisible: true,
        message: 'This message will auto-hide.',
        type: 'info',
        timeout: 2000, // 2 seconds
        autoTimeout: true,
        'onUpdate:isVisible': onCloseSpy,
      },
    })

    // Verify snackbar visibility
    cy.get('.v-snackbar__wrapper').should('be.visible')

    // Wait for auto timeout and verify close event was called
    cy.get('@onCloseSpy', { timeout: 3000 }).should('have.been.calledWith', false)
  })

  it('displays correct BC Gov Design System styling', () => {
    cy.mount(AppSnackbar, {
      props: {
        isVisible: true,
        message: 'Testing BC Gov Design System styling.',
        type: 'info',
        location: 'top',
        autoTimeout: false,
      },
    })

    // Verify layout structure
    cy.get('.v-snackbar__wrapper').should('have.css', 'display', 'flex')
    cy.get('.v-snackbar__wrapper').should('have.css', 'flex-direction', 'row')
    cy.get('.v-snackbar__wrapper').should('have.css', 'align-items', 'center')

    // Verify icon and content are inline
    cy.get('.app-snackbar__icon').should('be.visible')
    cy.get('.app-snackbar__container').should('be.visible')
    cy.get('.app-snackbar__close-icon').should('be.visible')

    // Verify border radius is applied
    cy.get('.v-snackbar__wrapper').should('have.css', 'border-radius')
  })
})
