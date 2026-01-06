import AppMessageDialog from './AppMessageDialog.vue'
import { MDL_PRM_INPUT_ERR, MSG_DIALOG_TITLE } from '@/constants/message'
import { BUTTON_LABEL } from '@/constants/constants'

describe('<AppMessageDialog />', () => {
  beforeEach(() => {
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
    scrollStrategy: 'none',
    variant: 'info' as const,
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
    cy.get('.bcds-message-dialog--header').should('contain', defaultProps.title)
    cy.get('.bcds-message-dialog--children').should('contain', defaultProps.message)
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
    cy.get('.bcds-message-dialog--children').should('not.be.visible')
  })

  it('renders with modified custom props', () => {
    const customProps = {
      dialog: true,
      title: 'Custom Title',
      message: 'Custom message content.',
      dialogWidth: 500,
      btnLabel: 'Submit',
      variant: 'warning' as const,
    }

    cy.mount(AppMessageDialog, { props: customProps })

    // Check if the dialog is visible
    cy.get('.v-dialog').should('be.visible')

    // Check the custom title
    cy.get('.bcds-message-dialog--header').should('contain', customProps.title)

    // Check the custom message
    cy.get('.bcds-message-dialog--children').should('contain', customProps.message)

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

  it('displays icon for info variant', () => {
    cy.mount(AppMessageDialog, {
      props: {
        ...defaultProps,
        variant: 'info',
      },
    })

    // Check if the icon element exists
    cy.get('.bcds-message-dialog--icon').should('exist')
  })

  it('displays icon for confirmation variant', () => {
    cy.mount(AppMessageDialog, {
      props: {
        ...defaultProps,
        variant: 'confirmation',
      },
    })

    // Check if the icon element exists
    cy.get('.bcds-message-dialog--icon').should('exist')
  })

  it('displays icon for warning variant', () => {
    cy.mount(AppMessageDialog, {
      props: {
        ...defaultProps,
        variant: 'warning',
      },
    })

    // Check if the icon element exists
    cy.get('.bcds-message-dialog--icon').should('exist')
  })

  it('displays icon for error variant', () => {
    cy.mount(AppMessageDialog, {
      props: {
        ...defaultProps,
        variant: 'error',
      },
    })

    // Check if the icon element exists
    cy.get('.bcds-message-dialog--icon').should('exist')
  })

  it('emits "update:dialog" and "close" events when close icon is clicked', () => {
    const updateDialogSpy = cy.spy().as('updateDialogSpy')
    const closeSpy = cy.spy().as('closeSpy')

    cy.mount(AppMessageDialog, {
      props: {
        ...defaultProps,
        'onUpdate:dialog': updateDialogSpy,
        onClose: closeSpy,
      },
    })

    // Click the close icon
    cy.get('.bcds-message-dialog--close-icon').click()

    // Check if the "update:dialog" event was emitted with false
    cy.get('@updateDialogSpy').should('have.been.calledOnceWith', false)

    // Check if the "close" event was emitted
    cy.get('@closeSpy').should('have.been.calledOnce')
  })

  it('applies correct CSS class for info variant', () => {
    cy.mount(AppMessageDialog, {
      props: {
        ...defaultProps,
        variant: 'info',
      },
    })

    // Check if the variant class is applied
    cy.get('.bcds-message-dialog').should('have.class', 'info')
  })

  it('applies correct CSS class for error variant', () => {
    cy.mount(AppMessageDialog, {
      props: {
        ...defaultProps,
        variant: 'error',
      },
    })

    // Check if the variant class is applied
    cy.get('.bcds-message-dialog').should('have.class', 'error')
  })

  it('applies correct CSS class for warning variant', () => {
    cy.mount(AppMessageDialog, {
      props: {
        ...defaultProps,
        variant: 'warning',
      },
    })

    // Check if the variant class is applied
    cy.get('.bcds-message-dialog').should('have.class', 'warning')
  })

  it('applies correct CSS class for confirmation variant', () => {
    cy.mount(AppMessageDialog, {
      props: {
        ...defaultProps,
        variant: 'confirmation',
      },
    })

    // Check if the variant class is applied
    cy.get('.bcds-message-dialog').should('have.class', 'confirmation')
  })
})
