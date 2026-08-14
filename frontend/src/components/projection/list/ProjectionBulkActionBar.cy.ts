import ProjectionBulkActionBar from './ProjectionBulkActionBar.vue'

describe('ProjectionBulkActionBar.vue', () => {
  const defaultProps = {
    isVisible: true,
    selectedCount: 3,
    canDownload: true,
    canDuplicate: true,
    canCancel: true,
    canDelete: true,
  }

  const mountComponent = (
    props: Partial<typeof defaultProps> = {},
    eventHandlers: Record<string, Cypress.Agent<sinon.SinonSpy>> = {},
  ) => {
    return cy.mount(ProjectionBulkActionBar, {
      props: { ...defaultProps, ...props, ...eventHandlers },
    })
  }

  it('renders the bar when isVisible is true', () => {
    mountComponent({ isVisible: true })
    cy.get('.bulk-action-bar').should('exist')
  })

  it('does not render the bar when isVisible is false', () => {
    mountComponent({ isVisible: false })
    cy.get('.bulk-action-bar').should('not.exist')
  })

  it('displays the selected count text', () => {
    mountComponent({ selectedCount: 5 })
    cy.get('.bulk-bar-count').should('contain', '5 Selected')
  })

  it('emits close event when close button is clicked', () => {
    const onCloseSpy = cy.spy().as('closeSpy')
    mountComponent({}, { onClose: onCloseSpy })
    cy.get('.bulk-bar-close-btn').click()
    cy.get('@closeSpy').should('have.been.calledOnce')
  })

  it('is enabled and emits duplicate when canDuplicate is true', () => {
    const onDuplicateSpy = cy.spy().as('duplicateSpy')
    mountComponent({ canDuplicate: true }, { onDuplicate: onDuplicateSpy })
    cy.get('button[title="Duplicate"]').should('not.have.attr', 'disabled')
    cy.get('button[title="Duplicate"]').click()
    cy.get('@duplicateSpy').should('have.been.calledOnce')
  })

  it('is enabled and emits download when canDownload is true', () => {
    const onDownloadSpy = cy.spy().as('downloadSpy')
    mountComponent({ canDownload: true }, { onDownload: onDownloadSpy })
    cy.get('button[title="Download"]').should('not.have.attr', 'disabled')
    cy.get('button[title="Download"]').click()
    cy.get('@downloadSpy').should('have.been.calledOnce')
  })

  it('is enabled and emits cancel when canCancel is true', () => {
    const onCancelSpy = cy.spy().as('cancelSpy')
    mountComponent({ canCancel: true }, { onCancel: onCancelSpy })
    cy.get('button[title="Cancel"]').should('not.have.attr', 'disabled')
    cy.get('button[title="Cancel"]').click()
    cy.get('@cancelSpy').should('have.been.calledOnce')
  })

  it('is enabled and emits delete when canDelete is true', () => {
    const onDeleteSpy = cy.spy().as('deleteSpy')
    mountComponent({ canDelete: true }, { onDelete: onDeleteSpy })
    cy.get('button[title="Delete"]').should('not.have.attr', 'disabled')
    cy.get('button[title="Delete"]').click()
    cy.get('@deleteSpy').should('have.been.calledOnce')
  })

  it('disables Download, Duplicate, Cancel, and Delete buttons and suppresses their events when can-flags are false', () => {
    const onDownloadSpy = cy.spy().as('downloadSpy')
    const onDuplicateSpy = cy.spy().as('duplicateSpy')
    const onCancelSpy = cy.spy().as('cancelSpy')
    const onDeleteSpy = cy.spy().as('deleteSpy')
    mountComponent(
      { canDownload: false, canDuplicate: false, canCancel: false, canDelete: false },
      { onDownload: onDownloadSpy, onDuplicate: onDuplicateSpy, onCancel: onCancelSpy, onDelete: onDeleteSpy },
    )

    cy.get('button[title="Download"]').should('have.attr', 'disabled')
    cy.get('button[title="Download"]').click({ force: true })
    cy.get('button[title="Duplicate"]').should('have.attr', 'disabled')
    cy.get('button[title="Duplicate"]').click({ force: true })
    cy.get('button[title="Cancel"]').should('have.attr', 'disabled')
    cy.get('button[title="Cancel"]').click({ force: true })
    cy.get('button[title="Delete"]').should('have.attr', 'disabled')
    cy.get('button[title="Delete"]').click({ force: true })

    cy.get('@downloadSpy').should('not.have.been.called')
    cy.get('@duplicateSpy').should('not.have.been.called')
    cy.get('@cancelSpy').should('not.have.been.called')
    cy.get('@deleteSpy').should('not.have.been.called')
  })
})
