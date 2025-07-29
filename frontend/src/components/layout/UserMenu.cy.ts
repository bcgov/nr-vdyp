/// <reference types="cypress" />

import UserMenu from './UserMenu.vue'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '@/stores/common/authStore'

describe('UserMenu.vue', () => {
  let authStore: ReturnType<typeof useAuthStore>

  beforeEach(() => {
    cy.document().then((doc) => {
      const style = doc.createElement('style')
      style.innerHTML = `
        body {
          background-color: rgb(0, 51, 102) !important;
        }
      `
      doc.head.appendChild(style)
    })

    const pinia = createPinia()
    setActivePinia(pinia)
    authStore = useAuthStore()
  })

  it('renders the user menu for a logged-in user', () => {
    // Mock getParsedIdToken for logged-in user
    cy.stub(authStore, 'getParsedIdToken').returns({
      client_roles: [],
      display_name: null,
      email: null,
      exp: null,
      family_name: 'Doe',
      given_name: 'John',
      idir_username: null,
      name: null,
      preferred_username: null,
      user_principal_name: null,
    })

    cy.mount(UserMenu, {
      props: {
        userIcon: 'mdi-account-circle',
        guestName: 'Guest',
        logoutText: 'Logout',
      },
    }).then(() => {
      // Wait for DOM to be ready
      cy.get('.header-user-button', { timeout: 6000 }).should('be.visible')

      // Assert the icon is displayed
      cy.get('.header-user-icon')
        .should('exist')
        .and('have.class', 'mdi-account-circle')

      // Click the user menu button to open the menu
      cy.get('.header-user-button').click()

      // Assert the logout button exists
      cy.get('.v-list-item-title').contains('Logout').should('exist')
    })
  })

  it('renders the user menu for a guest user', () => {
    // Mock getParsedIdToken for guest user
    cy.stub(authStore, 'getParsedIdToken').returns(null)

    cy.mount(UserMenu, {
      props: {
        userIcon: 'mdi-account-circle',
        guestName: 'Guest',
        logoutText: 'Logout',
      },
    }).then(() => {
      // Wait for DOM to be ready
      cy.get('.header-user-button', { timeout: 6000 }).should('be.visible')

      // Assert the default guest name is displayed
      cy.get('.header-user-name').should('contain', 'Guest')
    })
  })

  it('renders the user menu with props-based name when token is null', () => {
    // Mock getParsedIdToken for null token
    cy.stub(authStore, 'getParsedIdToken').returns(null)

    cy.mount(UserMenu, {
      props: {
        userIcon: 'mdi-account-circle',
        givenName: 'Jane',
        familyName: 'Smith',
        guestName: 'Guest',
        logoutText: 'Logout',
      },
    }).then(() => {
      // Wait for DOM to be ready
      cy.get('.header-user-button', { timeout: 6000 }).should('be.visible')

      // Assert the props-based name is displayed
      cy.get('.header-user-name').should('contain', 'Jane Smith')
    })
  })
})
