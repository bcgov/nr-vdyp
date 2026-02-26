import ProjectionBulkActionBar from './ProjectionBulkActionBar.vue'

describe('ProjectionBulkActionBar.vue', () => {
  const defaultProps = {
    isVisible: true,
    selectedCount: 3,
    canDownload: true,
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

  describe('visibility', () => {
    it('renders the bar when isVisible is true', () => {
      mountComponent({ isVisible: true })
      cy.get('.bulk-action-bar').should('exist')
    })

    it('does not render the bar when isVisible is false', () => {
      mountComponent({ isVisible: false })
      cy.get('.bulk-action-bar').should('not.exist')
    })
  })

  describe('rendering', () => {
    it('displays the selected count text', () => {
      mountComponent({ selectedCount: 5 })
      cy.get('.bulk-bar-count').should('contain', '5 Selected')
    })

    it('displays the correct count for a single selection', () => {
      mountComponent({ selectedCount: 1 })
      cy.get('.bulk-bar-count').should('contain', '1 Selected')
    })

    it('renders the close button', () => {
      mountComponent()
      cy.get('.bulk-bar-close-btn').should('exist').and('have.attr', 'title', 'Clear selection')
    })

    it('renders all four action buttons', () => {
      mountComponent()
      cy.get('button[title="Download"]').should('exist')
      cy.get('button[title="Duplicate"]').should('exist')
      cy.get('button[title="Cancel"]').should('exist')
      cy.get('button[title="Delete"]').should('exist')
    })

    it('renders icons with correct alt text', () => {
      mountComponent()
      cy.get('img[alt="Download"]').should('exist')
      cy.get('img[alt="Duplicate"]').should('exist')
      cy.get('img[alt="Cancel"]').should('exist')
      cy.get('img[alt="Delete"]').should('exist')
    })

    it('renders the divider between count and action buttons', () => {
      mountComponent()
      cy.get('.bulk-bar-divider').should('exist')
    })
  })

  describe('close button', () => {
    it('emits close event when close button is clicked', () => {
      const onCloseSpy = cy.spy().as('closeSpy')
      mountComponent({}, { onClose: onCloseSpy })
      cy.get('.bulk-bar-close-btn').click()
      cy.get('@closeSpy').should('have.been.calledOnce')
    })
  })

  describe('Download button', () => {
    it('is enabled and emits download when canDownload is true', () => {
      const onDownloadSpy = cy.spy().as('downloadSpy')
      mountComponent({ canDownload: true }, { onDownload: onDownloadSpy })
      cy.get('button[title="Download"]').should('not.have.attr', 'disabled')
      cy.get('button[title="Download"]').click()
      cy.get('@downloadSpy').should('have.been.calledOnce')
    })

    it('is disabled and does not emit download when canDownload is false', () => {
      const onDownloadSpy = cy.spy().as('downloadSpy')
      mountComponent({ canDownload: false }, { onDownload: onDownloadSpy })
      cy.get('button[title="Download"]').should('have.attr', 'disabled')
      cy.get('button[title="Download"]').should('have.class', 'bulk-bar-action-btn--disabled')
      cy.get('button[title="Download"]').click({ force: true })
      cy.get('@downloadSpy').should('not.have.been.called')
    })
  })

  describe('Duplicate button', () => {
    it('is always enabled', () => {
      mountComponent()
      cy.get('button[title="Duplicate"]').should('not.have.attr', 'disabled')
    })

    it('emits duplicate when clicked', () => {
      const onDuplicateSpy = cy.spy().as('duplicateSpy')
      mountComponent({}, { onDuplicate: onDuplicateSpy })
      cy.get('button[title="Duplicate"]').click()
      cy.get('@duplicateSpy').should('have.been.calledOnce')
    })
  })

  describe('Cancel button', () => {
    it('is enabled and emits cancel when canCancel is true', () => {
      const onCancelSpy = cy.spy().as('cancelSpy')
      mountComponent({ canCancel: true }, { onCancel: onCancelSpy })
      cy.get('button[title="Cancel"]').should('not.have.attr', 'disabled')
      cy.get('button[title="Cancel"]').click()
      cy.get('@cancelSpy').should('have.been.calledOnce')
    })

    it('is disabled and does not emit cancel when canCancel is false', () => {
      const onCancelSpy = cy.spy().as('cancelSpy')
      mountComponent({ canCancel: false }, { onCancel: onCancelSpy })
      cy.get('button[title="Cancel"]').should('have.attr', 'disabled')
      cy.get('button[title="Cancel"]').should('have.class', 'bulk-bar-action-btn--disabled')
      cy.get('button[title="Cancel"]').click({ force: true })
      cy.get('@cancelSpy').should('not.have.been.called')
    })
  })

  describe('Delete button', () => {
    it('is enabled and emits delete when canDelete is true', () => {
      const onDeleteSpy = cy.spy().as('deleteSpy')
      mountComponent({ canDelete: true }, { onDelete: onDeleteSpy })
      cy.get('button[title="Delete"]').should('not.have.attr', 'disabled')
      cy.get('button[title="Delete"]').click()
      cy.get('@deleteSpy').should('have.been.calledOnce')
    })

    it('is disabled and does not emit delete when canDelete is false', () => {
      const onDeleteSpy = cy.spy().as('deleteSpy')
      mountComponent({ canDelete: false }, { onDelete: onDeleteSpy })
      cy.get('button[title="Delete"]').should('have.attr', 'disabled')
      cy.get('button[title="Delete"]').should('have.class', 'bulk-bar-action-btn--disabled')
      cy.get('button[title="Delete"]').click({ force: true })
      cy.get('@deleteSpy').should('not.have.been.called')
    })
  })

  describe('mixed disabled states', () => {
    it('shows only enabled buttons active when all are disabled except duplicate', () => {
      mountComponent({ canDownload: false, canCancel: false, canDelete: false })

      cy.get('button[title="Download"]').should('have.class', 'bulk-bar-action-btn--disabled')
      cy.get('button[title="Cancel"]').should('have.class', 'bulk-bar-action-btn--disabled')
      cy.get('button[title="Delete"]').should('have.class', 'bulk-bar-action-btn--disabled')
      cy.get('button[title="Duplicate"]').should('not.have.class', 'bulk-bar-action-btn--disabled')
    })
  })
})
