import { createPinia, setActivePinia } from 'pinia'
import { useNotificationStore } from '@/stores/common/notificationStore'
import AppNotification from './AppNotification.vue'

describe('AppNotification.vue', () => {
  let notificationStore: ReturnType<typeof useNotificationStore>

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

    // Set up Pinia
    const pinia = createPinia()
    setActivePinia(pinia)
    notificationStore = useNotificationStore()
  })

  it('displays an informational notification', () => {
    notificationStore.showInfoMessage('This is an informational message.')

    cy.mountWithVuetify(AppNotification)

    // Verify the snackbar is visible with the correct message and style
    cy.get('.v-snackbar__wrapper')
      .should('be.visible')
      .and('contain.text', 'This is an informational message')
      .and('have.css', 'background-color', 'rgb(33, 150, 243)')

    // Verify the icon
    cy.get('.v-icon').should('have.class', 'mdi-information')
  })

  it('displays an error notification', () => {
    notificationStore.showErrorMessage('This is an error message.')

    cy.mountWithVuetify(AppNotification)

    // Verify the snackbar is visible with the correct message and style
    cy.get('.v-snackbar__wrapper')
      .should('be.visible')
      .and('contain.text', 'This is an error message')
      .and('have.css', 'background-color', 'rgb(176, 0, 32)')

    // Verify the icon
    cy.get('.v-icon').should('have.class', 'mdi-alert-circle')
  })

  it('displays a warning notification', () => {
    notificationStore.showWarningMessage('This is a warning message.')

    cy.mountWithVuetify(AppNotification)

    // Verify the snackbar is visible with the correct message and style
    cy.get('.v-snackbar__wrapper')
      .should('be.visible')
      .and('contain.text', 'This is a warning message')
      .and('have.css', 'background-color', 'rgb(251, 140, 0)')

    // Verify the icon
    cy.get('.v-icon').should('have.class', 'mdi-alert')
  })

  it('displays a success notification', () => {
    notificationStore.showSuccessMessage('This is a success message.')

    cy.mountWithVuetify(AppNotification)

    // Verify the snackbar is visible with the correct message and style
    cy.get('.v-snackbar__wrapper')
      .should('be.visible')
      .and('contain.text', 'This is a success message')
      .and('have.css', 'background-color', 'rgb(76, 175, 80)')

    // Verify the icon
    cy.get('.v-icon').should('have.class', 'mdi-check-circle')

    // Close the notification
    cy.get('.v-snackbar__actions .v-btn').click()
    cy.get('.v-snackbar__wrapper').should('not.be.visible')
  })
})
