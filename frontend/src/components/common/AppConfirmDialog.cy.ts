import { createPinia, setActivePinia } from 'pinia'
import { useConfirmDialogStore } from '@/stores/common/confirmDialogStore'
import AppConfirmDialog from './AppConfirmDialog.vue'
import { CONSTANTS } from '@/constants'

describe('<AppConfirmDialog />', () => {
  let confirmDialogStore: ReturnType<typeof useConfirmDialogStore>

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
    confirmDialogStore = useConfirmDialogStore()
    confirmDialogStore.openDialog(
      'Confirm Action',
      'Are you sure you want to proceed?',
      { width: 400 },
    )

    // Log store state to debug
    cy.log('Store state before test:', { isOpen: confirmDialogStore.getIsOpen })
  })

  it('renders the dialog with default store state', () => {
    cy.mountWithVuetify(AppConfirmDialog)

    // Increase timeout to handle potential async rendering
    cy.get('.v-dialog', { timeout: 10000 }).should('be.visible')

    // Check if the dialog is centered
    cy.get('.v-overlay__content')
      .should('have.css', 'justify-content', 'center')
      .and('have.css', 'align-items', 'center')

    // Check the title, message, and buttons
    cy.get('.popup-header').should('contain', 'Confirm Action')
    cy.get('.v-card-text').should(
      'contain',
      'Are you sure you want to proceed?',
    )
    cy.get('button').eq(0).should('contain', CONSTANTS.BUTTON_LABEL.CONF_NO)
    cy.get('button').eq(1).should('contain', CONSTANTS.BUTTON_LABEL.CONF_YES)
  })

  it('does not display message text if getDialogMessage is empty', () => {
    confirmDialogStore.openDialog('Empty Message', '', { width: 400 })

    cy.mountWithVuetify(AppConfirmDialog)

    cy.get('.v-card-text', { timeout: 10000 }).should('not.be.visible')
  })

  it('renders dialog with manual store state change', () => {
    // Close dialog manually and reopen
    confirmDialogStore.dialog = false
    // cy.wait(500) // Allow time for state change
    confirmDialogStore.openDialog('Manual Test', 'Testing manual open', {
      width: 400,
    })

    cy.mountWithVuetify(AppConfirmDialog)

    cy.get('.v-dialog', { timeout: 10000 }).should('be.visible')
    cy.get('.popup-header').should('contain', 'Manual Test')
    cy.get('.v-card-text').should('contain', 'Testing manual open')
  })
})
