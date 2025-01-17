import AppSnackbar from '@/components/core/AppSnackbar.vue'

describe('AppSnackbar.vue', () => {
  beforeEach(() => {
    cy.viewport(1024, 768)

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
    cy.mount(AppSnackbar, {
      props: {
        isVisible: true,
        message: 'This is an informational message.',
        type: 'info',
        color: '#d9eaf7',
        location: 'top',
        rounded: true,
        autoTimeout: false,
      },
    })

    // Verify snackbar visibility
    cy.get('.v-snackbar__wrapper')
      .should('be.visible')
      .and('contain.text', 'This is an informational message')
      .and('have.css', 'background-color', 'rgb(217, 234, 247)')

    // Verify the icon
    cy.get('.v-icon').should('have.class', 'mdi-information')

    // Close the snackbar
    cy.get('.v-snackbar__actions .v-btn').click()
    cy.get('.v-snackbar__wrapper').should('not.be.visible')
  })

  it('displays an error notification', () => {
    cy.mount(AppSnackbar, {
      props: {
        isVisible: true,
        message: 'This is an error message.',
        type: 'error',
        color: '#d8292f',
        location: 'top',
        rounded: true,
        autoTimeout: false,
      },
    })

    // Verify snackbar visibility
    cy.get('.v-snackbar__wrapper')
      .should('be.visible')
      .and('contain.text', 'This is an error message')
      .and('have.css', 'background-color', 'rgb(216, 41, 47)')

    // Verify the icon
    cy.get('.v-icon').should('have.class', 'mdi-alert-circle')

    // Close the snackbar
    cy.get('.v-snackbar__actions .v-btn').click()
    cy.get('.v-snackbar__wrapper').should('not.be.visible')
  })

  it('displays a warning notification', () => {
    cy.mount(AppSnackbar, {
      props: {
        isVisible: true,
        message: 'This is a warning message.',
        type: 'warning',
        color: '#f9f1c6',
        location: 'top',
        rounded: true,
        autoTimeout: true,
      },
    })

    // Verify snackbar visibility
    cy.get('.v-snackbar__wrapper')
      .should('be.visible')
      .and('contain.text', 'This is a warning message')
      .and('have.css', 'background-color', 'rgb(249, 241, 198)')

    // Verify the icon
    cy.get('.v-icon').should('have.class', 'mdi-alert')

    // Close the snackbar
    cy.get('.v-snackbar__actions .v-btn').click()
    cy.get('.v-snackbar__wrapper').should('not.be.visible')
  })

  it('displays a success notification', () => {
    cy.mount(AppSnackbar, {
      props: {
        isVisible: true,
        message: 'This is a success message.',
        type: 'success',
        color: '#2e8540',
        location: 'top',
        rounded: true,
        autoTimeout: false,
      },
    })

    // Verify snackbar visibility
    cy.get('.v-snackbar__wrapper')
      .should('be.visible')
      .and('contain.text', 'This is a success message')
      .and('have.css', 'background-color', 'rgb(46, 133, 64)')

    // Verify the icon
    cy.get('.v-icon').should('have.class', 'mdi-check-circle')

    // Close the snackbar
    cy.get('.v-snackbar__actions .v-btn').click()
    cy.get('.v-snackbar__wrapper').should('not.be.visible')
  })

  it('handles auto timeout', () => {
    cy.mount(AppSnackbar, {
      props: {
        isVisible: true,
        message: 'This message will auto-hide.',
        type: 'info',
        color: '#d9eaf7',
        timeout: 2000, // 2 seconds
        autoTimeout: true,
      },
    })

    // Verify snackbar visibility
    cy.get('.v-snackbar__wrapper').should('be.visible')

    // Wait for auto timeout
    cy.get('.v-snackbar__wrapper', { timeout: 3000 }).should('not.be.visible')
  })
})
