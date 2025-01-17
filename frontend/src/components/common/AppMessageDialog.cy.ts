import AppMessageDialog from './AppMessageDialog.vue'
import { MDL_PRM_INPUT_ERR, MSG_DIALOG_TITLE } from '@/constants/message'
import { BUTTON_LABEL } from '@/constants/constants'

describe('<AppMessageDialog />', () => {
  beforeEach(() => {
    cy.viewport(1024, 768)

    // Add custom styles to override Vuetify's styles
    cy.document().then((doc) => {
      const style = doc.createElement('style')
      style.innerHTML = `
        .v-overlay__content {
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
  })

  const defaultProps = {
    dialog: true,
    title: MSG_DIALOG_TITLE.DATA_INCOMPLETE,
    message: MDL_PRM_INPUT_ERR.SPCZ_VLD_TOTAL_PCT,
    dialogWidth: 400,
    btnLabel: BUTTON_LABEL.CONT_EDIT,
  }

  it('renders with default props', () => {
    cy.mount(AppMessageDialog, { props: defaultProps })

    // Check if the dialog is visible
    cy.get('.v-dialog').should('be.visible')

    // Check if the dialog is visible and centered
    cy.get('.v-overlay__content').should(
      'have.css',
      'justify-content',
      'center',
    )
    cy.get('.v-overlay__content').should('have.css', 'align-items', 'center')

    // Check the title, message, button label
    cy.get('.popup-header').should('contain', defaultProps.title)
    cy.get('.v-card-text').should('contain', defaultProps.message)
    cy.get('button').should('contain', defaultProps.btnLabel)
  })

  it('emits "update:dialog" and "close" events when the button is clicked', () => {
    const updateDialogSpy = cy.spy().as('updateDialogSpy')
    const closeSpy = cy.spy().as('closeSpy')

    cy.mount(AppMessageDialog, {
      props: {
        ...defaultProps,
        'onUpdate:dialog': updateDialogSpy,
        onClose: closeSpy,
      },
    })

    // Click the button
    cy.get('button').click()

    // Check if the "update:dialog" event was emitted with false
    cy.get('@updateDialogSpy').should('have.been.calledOnceWith', false)

    // Check if the "close" event was emitted
    cy.get('@closeSpy').should('have.been.calledOnce')
  })

  it('does not display the message text if "message" prop is empty', () => {
    cy.mount(AppMessageDialog, {
      props: {
        ...defaultProps,
        message: '',
      },
    })

    // Check if the message container is not visible
    cy.get('.v-card-text').should('not.be.visible')
  })

  it('renders with modified custom props', () => {
    const customProps = {
      dialog: true,
      title: 'Custom Title',
      message: 'Custom message content.',
      dialogWidth: 500,
      btnLabel: 'Submit',
    }

    cy.mount(AppMessageDialog, { props: customProps })

    // Check if the dialog is visible
    cy.get('.v-dialog').should('be.visible')

    // Check the custom title
    cy.get('.popup-header').should('contain', customProps.title)

    // Check the custom message
    cy.get('.v-card-text').should('contain', customProps.message)

    // Check the custom dialog width
    cy.get('.v-overlay__content').should('have.css', 'max-width', '500px')

    // Check the custom button label
    cy.get('button').should('contain', customProps.btnLabel)
  })

  it('does not show the dialog when dialog is set to false', () => {
    cy.mount(AppMessageDialog, {
      props: {
        ...defaultProps,
        dialog: false,
      },
    })

    // Check if the dialog is not exist
    cy.get('.v-dialog').should('not.exist')
  })

  it('renders with default props', () => {
    cy.mount(AppMessageDialog, { props: defaultProps })

    cy.get('.v-dialog').should('be.visible')

    cy.get('.v-overlay__content').should(
      'have.css',
      'justify-content',
      'center',
    )
    cy.get('.v-overlay__content').should('have.css', 'align-items', 'center')

    cy.get('.popup-header').should('contain', defaultProps.title)
    cy.get('.v-card-text').should('contain', defaultProps.message)
    cy.get('button').should('contain', defaultProps.btnLabel)
  })
})
