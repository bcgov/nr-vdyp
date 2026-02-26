import { mount } from 'cypress/vue'
import { createVuetify } from 'vuetify'
import { h } from 'vue'
import { VApp } from 'vuetify/components'
import 'vuetify/styles'
import ProjectionActionsMenu from './ProjectionActionsMenu.vue'
import type { ProjectionStatus } from '@/interfaces/interfaces'

const vuetify = createVuetify({
  defaults: {
    VMenu: {
      scrollStrategy: 'none',
      transition: false,
    },
  },
})

describe('ProjectionActionsMenu.vue', () => {
  const mountComponent = (
    status: ProjectionStatus,
    title: string = 'Test Projection',
    eventHandlers: Record<string, Cypress.Agent<sinon.SinonSpy>> = {},
  ) => {
    return mount(
      {
        render() {
          return h(VApp, {}, [h(ProjectionActionsMenu, { status, title, ...eventHandlers })])
        },
      },
      {
        global: {
          plugins: [vuetify],
        },
      },
    )
  }

  const openMenu = () => {
    cy.get('.action-menu-button').click()
    cy.get('.action-menu-list').should('be.visible')
  }

  beforeEach(() => {
    cy.document().then((doc) => {
      const style = doc.createElement('style')
      style.innerHTML = `
        body {
          background-color: rgb(240, 240, 240) !important;
        }
      `
      doc.head.appendChild(style)
    })
  })

  describe('rendering', () => {
    it('renders the action menu button', () => {
      mountComponent('Draft')

      cy.get('.action-menu-button').should('exist')
      cy.get('.action-menu-button').should(
        'have.attr',
        'title',
        'Actions for Test Projection',
      )
    })

    it('renders the menu icon', () => {
      mountComponent('Draft')

      cy.get('.action-menu-button .v-icon').should('exist')
    })
  })

  describe('Draft status', () => {
    it('shows Edit, Duplicate, and Delete menu items', () => {
      mountComponent('Draft')
      openMenu()

      cy.contains('.menu-text', 'Edit').should('be.visible')
      cy.contains('.menu-text', 'Duplicate').should('be.visible')
      cy.contains('.menu-text', 'Delete').should('be.visible')

      cy.contains('.menu-text', 'View').should('not.exist')
      cy.contains('.menu-text', 'Download').should('not.exist')
      cy.contains('.menu-text', 'Cancel').should('not.exist')
    })

    it('emits edit event when Edit is clicked', () => {
      const onEditSpy = cy.spy().as('editSpy')
      mountComponent('Draft', 'Test Projection', { onEdit: onEditSpy })
      openMenu()

      cy.contains('.menu-text', 'Edit').click()
      cy.get('@editSpy').should('have.been.called')
    })

    it('emits duplicate event when Duplicate is clicked', () => {
      const onDuplicateSpy = cy.spy().as('duplicateSpy')
      mountComponent('Draft', 'Test Projection', {
        onDuplicate: onDuplicateSpy,
      })
      openMenu()

      cy.contains('.menu-text', 'Duplicate').click()
      cy.get('@duplicateSpy').should('have.been.called')
    })

    it('emits delete event when Delete is clicked', () => {
      const onDeleteSpy = cy.spy().as('deleteSpy')
      mountComponent('Draft', 'Test Projection', { onDelete: onDeleteSpy })
      openMenu()

      cy.contains('.menu-text', 'Delete').click()
      cy.get('@deleteSpy').should('have.been.called')
    })
  })

  describe('Ready status', () => {
    it('shows View, Duplicate, Download, and Delete menu items', () => {
      mountComponent('Ready')
      openMenu()

      cy.contains('.menu-text', 'View').should('be.visible')
      cy.contains('.menu-text', 'Duplicate').should('be.visible')
      cy.contains('.menu-text', 'Download').should('be.visible')
      cy.contains('.menu-text', 'Delete').should('be.visible')

      cy.contains('.menu-text', 'Edit').should('not.exist')
      cy.contains('.menu-text', 'Cancel').should('not.exist')
    })

    it('emits view event when View is clicked', () => {
      const onViewSpy = cy.spy().as('viewSpy')
      mountComponent('Ready', 'Test Projection', { onView: onViewSpy })
      openMenu()

      cy.contains('.menu-text', 'View').click()
      cy.get('@viewSpy').should('have.been.called')
    })

    it('emits download event when Download is clicked', () => {
      const onDownloadSpy = cy.spy().as('downloadSpy')
      mountComponent('Ready', 'Test Projection', { onDownload: onDownloadSpy })
      openMenu()

      cy.contains('.menu-text', 'Download').click()
      cy.get('@downloadSpy').should('have.been.called')
    })
  })

  describe('Running status', () => {
    it('shows Cancel menu item only', () => {
      mountComponent('Running')
      openMenu()

      cy.contains('.menu-text', 'Cancel').should('be.visible')

      cy.contains('.menu-text', 'View').should('not.exist')
      cy.contains('.menu-text', 'Edit').should('not.exist')
      cy.contains('.menu-text', 'Duplicate').should('not.exist')
      cy.contains('.menu-text', 'Download').should('not.exist')
      cy.contains('.menu-text', 'Delete').should('not.exist')
    })

    it('emits cancel event when Cancel is clicked', () => {
      const onCancelSpy = cy.spy().as('cancelSpy')
      mountComponent('Running', 'Test Projection', { onCancel: onCancelSpy })
      openMenu()

      cy.contains('.menu-text', 'Cancel').click()
      cy.get('@cancelSpy').should('have.been.called')
    })
  })

  describe('Failed status', () => {
    it('shows Edit, Duplicate, Download, and Delete menu items', () => {
      mountComponent('Failed')
      openMenu()

      cy.contains('.menu-text', 'Edit').should('be.visible')
      cy.contains('.menu-text', 'Duplicate').should('be.visible')
      cy.contains('.menu-text', 'Download').should('be.visible')
      cy.contains('.menu-text', 'Delete').should('be.visible')

      cy.contains('.menu-text', 'View').should('not.exist')
      cy.contains('.menu-text', 'Cancel').should('not.exist')
    })
  })

  describe('Delete action', () => {
    it('is available for Draft status', () => {
      mountComponent('Draft')
      openMenu()
      cy.contains('.menu-text', 'Delete').should('be.visible')
    })

    it('is available for Ready status', () => {
      mountComponent('Ready')
      openMenu()
      cy.contains('.menu-text', 'Delete').should('be.visible')
    })

    it('is not available for Running status', () => {
      mountComponent('Running')
      openMenu()
      cy.contains('.menu-text', 'Delete').should('not.exist')
    })

    it('is available for Failed status', () => {
      mountComponent('Failed')
      openMenu()
      cy.contains('.menu-text', 'Delete').should('be.visible')
    })

    it('has danger styling', () => {
      mountComponent('Draft')
      openMenu()

      cy.get('.action-menu-item.danger').should('exist')
    })
  })

  describe('menu icons', () => {
    it('displays correct icons for each menu item', () => {
      mountComponent('Ready')
      openMenu()

      cy.get('img[alt="View"]').should('exist')
      cy.get('img[alt="Duplicate"]').should('exist')
      cy.get('img[alt="Download"]').should('exist')
      cy.get('img[alt="Delete"]').should('exist')
    })
  })

  describe('accessibility', () => {
    it('has a descriptive title attribute on the button', () => {
      mountComponent('Draft', 'My Custom Projection')

      cy.get('.action-menu-button').should(
        'have.attr',
        'title',
        'Actions for My Custom Projection',
      )
    })
  })
})
