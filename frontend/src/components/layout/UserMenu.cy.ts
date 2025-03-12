import UserMenu from './UserMenu.vue'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '@/stores/common/authStore'

describe('UserMenu.vue', () => {
  let authStore: ReturnType<typeof useAuthStore>

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
    authStore = useAuthStore()

    // Mock the auth store's parseIdToken function
    authStore.parseIdToken = () => ({
      given_name: 'John',
      family_name: 'Doe',
      auth_time: null,
      client_roles: [],
      display_name: null,
      email: null,
      exp: null,
      idir_username: null,
      name: null,
      preferred_username: null,
      user_principal_name: null,
    })
  })

  it('renders the user menu for a logged-in user', () => {
    cy.mountWithVuetify(UserMenu, {
      props: {
        userIcon: 'mdi-account-circle',
        guestName: 'Guest',
        logoutText: 'Logout',
      },
    }).then(() => {
      // Assert the name is displayed
      cy.get('.header-user-name').should('contain', 'John Doe')

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
    // Update mock data to simulate guest user
    authStore.parseIdToken = () => null

    cy.mountWithVuetify(UserMenu, {
      props: {
        userIcon: 'mdi-account-circle',
        guestName: 'Guest',
        logoutText: 'Logout',
      },
    }).then(() => {
      // Assert the default guest name is displayed
      cy.get('.header-user-name').should('contain', 'Guest')
    })
  })
})
