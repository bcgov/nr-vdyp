import { createPinia, setActivePinia } from 'pinia'
import { useAlertDialogStore } from '@/stores/common/alertDialogStore'
import AppAlertDialog from './AppAlertDialog.vue'
import { CONSTANTS } from '@/constants'

describe('<AppAlertDialog />', () => {
  let alertDialogStore: ReturnType<typeof useAlertDialogStore>

  beforeEach(() => {
    // Set Cypress preview background color and adjust Vuetify overlay styles
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
          position: relative !important;
          display: flex !important;
          justify-content: center !important;
          align-items: center !important;
          top: 0 !important;
          left: 0 !important;
        }
      `
      doc.head.appendChild(style)
    })

    // Set up Pinia
    const pinia = createPinia()
    setActivePinia(pinia)
    alertDialogStore = useAlertDialogStore()
    alertDialogStore.openDialog(
      'Confirm Action',
      'Are you sure you want to proceed?',
      { width: 400 },
    )

    // Log store state to debug
    cy.log('Store state before test:', { isOpen: alertDialogStore.getIsOpen })
  })

  it('renders the dialog with default store state', () => {
    cy.mountWithVuetify(AppAlertDialog)

    // Increase timeout to handle potential async rendering
    cy.get('.v-dialog', { timeout: 10000 }).should('be.visible')

    // Check if the dialog is centered
    cy.get('.v-overlay__content')
      .should('have.css', 'justify-content', 'center')
      .and('have.css', 'align-items', 'center')

    // Check the title, message, and buttons
    cy.get('.bcds-alert-dialog--header').should('be.visible')
    cy.get('.bcds-alert-dialog--title').should('contain', 'Confirm Action')
    cy.get('.bcds-alert-dialog--children').should(
      'contain',
      'Are you sure you want to proceed?',
    )
    cy.get('button').eq(0).should('contain', CONSTANTS.BUTTON_LABEL.ALERT_CANCEL)
    cy.get('button').eq(1).should('contain', CONSTANTS.BUTTON_LABEL.ALERT_CONTINUE)
  })

  it('does not display message text if getDialogMessage is empty', () => {
    alertDialogStore.openDialog('Empty Message', '', { width: 400 })

    cy.mountWithVuetify(AppAlertDialog)

    cy.get('.bcds-alert-dialog--children', { timeout: 10000 }).should('not.be.visible')
  })

  it('renders dialog with manual store state change', () => {
    // Close dialog manually and reopen
    alertDialogStore.dialog = false
    // cy.wait(500) // Allow time for state change
    alertDialogStore.openDialog('Manual Test', 'Testing manual open', {
      width: 400,
    })

    cy.mountWithVuetify(AppAlertDialog)

    cy.get('.v-dialog', { timeout: 10000 }).should('be.visible')
    cy.get('.bcds-alert-dialog--title').should('contain', 'Manual Test')
    cy.get('.bcds-alert-dialog--children').should('contain', 'Testing manual open')
  })

  it('renders dialog with info variant and icon', () => {
    alertDialogStore.openDialog('Info Dialog', 'This is an info message', {
      width: 400,
      variant: 'info',
    })

    cy.mountWithVuetify(AppAlertDialog)

    cy.get('.bcds-alert-dialog').should('have.class', 'info')
    cy.get('.bcds-alert-dialog--icon')
      .should('be.visible')
      .and('have.class', 'mdi-information')
  })

  it('renders dialog with confirmation variant and icon', () => {
    alertDialogStore.openDialog('Confirm Dialog', 'Please confirm', {
      width: 400,
      variant: 'confirmation',
    })

    cy.mountWithVuetify(AppAlertDialog)

    cy.get('.bcds-alert-dialog').should('have.class', 'confirmation')
    cy.get('.bcds-alert-dialog--icon')
      .should('be.visible')
      .and('have.class', 'mdi-check-circle')
  })

  it('renders dialog with warning variant and icon', () => {
    alertDialogStore.openDialog('Warning Dialog', 'This is a warning', {
      width: 400,
      variant: 'warning',
    })

    cy.mountWithVuetify(AppAlertDialog)

    cy.get('.bcds-alert-dialog').should('have.class', 'warning')
    cy.get('.bcds-alert-dialog--icon')
      .should('be.visible')
      .and('have.class', 'mdi-alert')
  })

  it('renders dialog with error variant and icon', () => {
    alertDialogStore.openDialog('Error Dialog', 'An error occurred', {
      width: 400,
      variant: 'error',
    })

    cy.mountWithVuetify(AppAlertDialog)

    cy.get('.bcds-alert-dialog').should('have.class', 'error')
    cy.get('.bcds-alert-dialog--icon')
      .should('be.visible')
      .and('have.class', 'mdi-alert-circle')
  })

  it('renders dialog with destructive variant and icon', () => {
    alertDialogStore.openDialog('Destructive Dialog', 'This action is destructive', {
      width: 400,
      variant: 'destructive',
    })

    cy.mountWithVuetify(AppAlertDialog)

    cy.get('.bcds-alert-dialog').should('have.class', 'destructive')
    cy.get('.bcds-alert-dialog--icon')
      .should('be.visible')
      .and('have.class', 'mdi-alert-circle')
  })

  it('closes dialog when close icon is clicked', () => {
    cy.mountWithVuetify(AppAlertDialog)

    cy.get('.bcds-alert-dialog--close-icon').click()
    cy.wrap(alertDialogStore).its('getIsOpen').should('be.false')
  })

  it('closes dialog when cancel button is clicked', () => {
    cy.mountWithVuetify(AppAlertDialog)

    cy.get('button').eq(0).click()
    cy.wrap(alertDialogStore).its('getIsOpen').should('be.false')
  })

  it('calls agree when continue button is clicked', () => {
    cy.mountWithVuetify(AppAlertDialog)

    cy.spy(alertDialogStore, 'agree').as('agreeSpy')
    cy.get('button').eq(1).click()
    cy.get('@agreeSpy').should('have.been.called')
  })
})
