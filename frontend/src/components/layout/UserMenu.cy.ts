import UserMenu from './UserMenu.vue'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '@/stores/common/authStore'

describe('UserMenu', () => {
  let authStore: ReturnType<typeof useAuthStore>

  beforeEach(() => {
    const pinia = createPinia()
    setActivePinia(pinia)
    authStore = useAuthStore()
  })

  it('renders with authenticated user from token', () => {
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
        familyName:  'Doe',
        givenName: 'John',
        logoutText: 'Logout',
      },
    })

    cy.get('.header-user-button').should('be.visible')
    cy.get('.header-user-name').should('contain', 'John Doe')
    cy.get('.header-user-icon').should('have.class', 'mdi-account-circle')

    cy.get('.header-user-button').click()
    cy.get('.v-list-item-title').should('contain', 'Logout')
  })

  it('renders guest user when no authentication', () => {
    cy.stub(authStore, 'getParsedIdToken').returns(null)

    cy.mount(UserMenu, {
      props: {
        userIcon: 'mdi-account-circle',
        guestName: 'Guest',
        logoutText: 'Logout',
      },
    })

    cy.get('.header-user-button').should('be.visible')
    cy.get('.header-user-name').should('contain', 'Guest')
  })

  it('renders with props-based name', () => {
    cy.stub(authStore, 'getParsedIdToken').returns(null)

    cy.mount(UserMenu, {
      props: {
        userIcon: 'mdi-account-circle',
        givenName: 'Jane',
        familyName: 'Smith',
        guestName: 'Guest',
        logoutText: 'Logout',
      },
    })

    cy.get('.header-user-button').should('be.visible')
    cy.get('.header-user-name').should('contain', 'Jane Smith')
  })

  it('displays custom guest name', () => {
    cy.stub(authStore, 'getParsedIdToken').returns(null)

    cy.mount(UserMenu, {
      props: {
        userIcon: 'mdi-account-circle',
        guestName: 'Visitor',
        logoutText: 'Logout',
      },
    })

    cy.get('.header-user-name').should('contain', 'Visitor')
  })
})
