import TheHeader from './TheHeader.vue'
import { createPinia, setActivePinia } from 'pinia'
import { createVuetify } from 'vuetify'

describe('TheHeader.vue', () => {
  beforeEach(() => {
    // Set Cypress preview background color
    cy.document().then((doc) => {
      const style = doc.createElement('style')
      style.innerHTML = `
        body {
          background-color: rgb(0, 51, 102) !important;
        }
      `
      doc.head.appendChild(style)
    })

    // Initialize Pinia
    const pinia = createPinia()
    setActivePinia(pinia)
  })

  const vuetify = createVuetify()

  it('renders TheHeader with default props', () => {
    cy.mount(TheHeader, {
      global: {
        plugins: [vuetify, createPinia()],
      },
      props: {
        logoProps: {},
        titleProps: {},
        userMenuProps: {
          userIcon: 'mdi-account-circle',
          guestName: 'Guest',
          givenName: 'John',
          familyName: 'Doe',
        },
      },
    }).then(() => {
      // Check if BCLogo is rendered
      cy.get('.bcds-logo').should('exist')
      cy.get('.bcds-logo').should('have.attr', 'alt', 'B.C. Government Logo')

      // Check if HeaderTitle is rendered with default text
      cy.get('.bcds-header--title').should(
        'contain.text',
        'VARIABLE DENSITY YIELD PROJECTION',
      )

      // Check if UserMenu is rendered with the provided props
      cy.get('.header-user-name').should('contain.text', 'John Doe')

      // Check if TrainingSupport is rendered
      cy.get('.bcds-header-link').should(
        'contain.text',
        'Training and Support',
      )
    })
  })

  it('renders skip link for accessibility', () => {
    cy.mount(TheHeader, {
      global: {
        plugins: [vuetify, createPinia()],
      },
      props: {
        logoProps: {},
        titleProps: {},
        userMenuProps: {
          userIcon: 'mdi-account-circle',
          guestName: 'Guest',
        },
      },
    }).then(() => {
      // Check if skip link exists
      cy.get('.bcds-header--skiplinks a').should('exist')
      cy.get('.bcds-header--skiplinks a').should(
        'contain.text',
        'Skip to main content',
      )
      cy.get('.bcds-header--skiplinks a').should('have.attr', 'href', '#main')
    })
  })

  it('skip link becomes visible on focus', () => {
    // Add a main element to the document for the skip link to target
    cy.document().then((doc) => {
      const main = doc.createElement('main')
      main.id = 'main'
      main.setAttribute('tabindex', '-1')
      doc.body.appendChild(main)
    })

    cy.mount(TheHeader, {
      global: {
        plugins: [vuetify, createPinia()],
      },
      props: {
        logoProps: {},
        titleProps: {},
        userMenuProps: {
          userIcon: 'mdi-account-circle',
          guestName: 'Guest',
        },
      },
    }).then(() => {
      // Skip link should have negative margin-left when not focused
      cy.get('.bcds-header--skiplinks a').should(
        'have.css',
        'margin-left',
        '-100000px',
      )

      // Focus on the skip link
      cy.get('.bcds-header--skiplinks a').focus()

      // Skip link should have 0 margin-left when focused
      cy.get('.bcds-header--skiplinks a').should('have.css', 'margin-left', '0px')
    })
  })
})
