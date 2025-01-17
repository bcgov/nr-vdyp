import TheHeader from '@/components/layout/TheHeader.vue'
import { createPinia, setActivePinia } from 'pinia'
import { createVuetify } from 'vuetify'

describe('TheHeader.vue', () => {
  beforeEach(() => {
    cy.viewport(1024, 768)

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
      cy.get('#header-bc-logo-img').should('exist')

      // Check if HeaderTitle is rendered with default text
      cy.get('.header-toolbar-title').should(
        'contain.text',
        'VARIABLE DENSITY YIELD PROJECTION',
      )

      // Check if UserMenu is rendered with the provided props
      cy.get('.header-user-name').should('contain.text', 'John Doe')

      // Check if TrainingSupport is rendered
      cy.get('.header-training-support').should(
        'contain.text',
        'Training and Support',
      )
    })
  })
})
