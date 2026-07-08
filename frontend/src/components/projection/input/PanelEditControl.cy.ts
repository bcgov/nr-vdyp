import { mount } from 'cypress/vue'
import { createVuetify } from 'vuetify'
import 'vuetify/styles'
import PanelEditControl from './PanelEditControl.vue'

const vuetify = createVuetify()

const mountControl = (props: {
  isReadOnly: boolean
  editable: boolean
  isHeaderEditActive: boolean
  editTooltipText?: string
}) => {
  const onEdit = cy.stub().as('onEdit')

  mount(PanelEditControl, {
    props: { editTooltipText: '', ...props },
    global: { plugins: [vuetify] },
    attrs: { onEdit },
  })

  return { onEdit }
}

describe('<PanelEditControl />', () => {
  it('renders nothing in read-only mode', () => {
    mountControl({ isReadOnly: true, editable: false, isHeaderEditActive: true })
    cy.get('.panel-edit-control').should('not.exist')
  })

  it('shows the "Editing" badge when editable', () => {
    mountControl({ isReadOnly: false, editable: true, isHeaderEditActive: true })
    cy.get('.editing-badge').should('exist').and('contain.text', 'Editing')
    cy.get('.edit-btn').should('not.exist')
  })

  it('shows the Edit button when not editable', () => {
    mountControl({ isReadOnly: false, editable: false, isHeaderEditActive: true })
    cy.get('.edit-btn').should('exist').and('contain.text', 'Edit')
  })

  it('emits "edit" when the Edit button is clicked and header edit is active', () => {
    mountControl({ isReadOnly: false, editable: false, isHeaderEditActive: true })
    cy.get('.edit-btn').click()
    cy.get('@onEdit').should('have.been.calledOnce')
  })

  it('does not emit "edit" when header edit is not active', () => {
    mountControl({ isReadOnly: false, editable: false, isHeaderEditActive: false })
    cy.get('.edit-btn').should('have.class', 'edit-btn--disabled').click()
    cy.get('@onEdit').should('not.have.been.called')
  })
})
