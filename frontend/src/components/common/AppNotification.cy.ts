import { createPinia, setActivePinia } from 'pinia'
import { useNotificationStore } from '@/stores/common/notificationStore'
import AppNotification from './AppNotification.vue'

describe('AppNotification.vue', () => {
  let notificationStore: ReturnType<typeof useNotificationStore>

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

    // Set up Pinia
    const pinia = createPinia()
    setActivePinia(pinia)
    notificationStore = useNotificationStore()
  })

  it('displays an informational notification', () => {
    notificationStore.showInfoMessage('This is an informational message.')

    cy.mountWithVuetify(AppNotification)

    // Verify the snackbar is visible with the correct message
    cy.get('.v-snackbar__wrapper')
      .should('be.visible')
      .and('contain.text', 'This is an informational message')

    // Verify BC Gov Design Standards info background color
    cy.get('.app-snackbar.info .v-snackbar__wrapper')
      .should('have.css', 'background-color', 'rgb(247, 249, 252)') // --support-surface-color-info

    // Verify the icon is info type
    cy.get('.app-snackbar__icon').should('have.class', 'mdi-information-outline')
  })

  it('displays an error notification', () => {
    notificationStore.showErrorMessage('This is an error message.')

    cy.mountWithVuetify(AppNotification)

    // Verify the snackbar is visible with the correct message
    cy.get('.v-snackbar__wrapper')
      .should('be.visible')
      .and('contain.text', 'This is an error message')

    // Verify BC Gov Design Standards error background color
    cy.get('.app-snackbar.error .v-snackbar__wrapper')
      .should('have.css', 'background-color', 'rgb(244, 225, 226)') // --support-surface-color-danger

    // Verify the icon is error type
    cy.get('.app-snackbar__icon').should('have.class', 'mdi-alert-circle-outline')
  })

  it('displays a warning notification', () => {
    notificationStore.showWarningMessage('This is a warning message.')

    cy.mountWithVuetify(AppNotification)

    // Verify the snackbar is visible with the correct message
    cy.get('.v-snackbar__wrapper')
      .should('be.visible')
      .and('contain.text', 'This is a warning message')

    // Verify BC Gov Design Standards warning background color
    cy.get('.app-snackbar.warning .v-snackbar__wrapper')
      .should('have.css', 'background-color', 'rgb(254, 241, 216)') // --support-surface-color-warning

    // Verify the icon is warning type
    cy.get('.app-snackbar__icon').should('have.class', 'mdi-alert-outline')
  })

  it('displays a success notification', () => {
    notificationStore.showSuccessMessage('This is a success message.')

    cy.mountWithVuetify(AppNotification)

    // Verify the snackbar is visible with the correct message
    cy.get('.v-snackbar__wrapper')
      .should('be.visible')
      .and('contain.text', 'This is a success message')

    // Verify BC Gov Design Standards success background color
    cy.get('.app-snackbar.success .v-snackbar__wrapper')
      .should('have.css', 'background-color', 'rgb(246, 255, 248)') // --support-surface-color-success

    // Verify the icon is success type
    cy.get('.app-snackbar__icon').should('have.class', 'mdi-check-circle-outline')
  })
})
