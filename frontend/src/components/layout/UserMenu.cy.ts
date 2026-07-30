import { mount } from 'cypress/vue'
import { createVuetify } from 'vuetify'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import { h } from 'vue'
import { VApp } from 'vuetify/components'
import 'vuetify/styles'
import UserMenu from './UserMenu.vue'
import { useAuthStore } from '@/stores/common/authStore'
import { ROUTE_PATH } from '@/constants/constants'

const vuetify = createVuetify({
  defaults: {
    VMenu: {
      scrollStrategy: 'none',
      transition: false,
    },
  },
})

const router = createRouter({
  history: createMemoryHistory(),
  routes: [
    { path: ROUTE_PATH.PROJECTION_LIST, component: { template: '<div />' } },
    { path: ROUTE_PATH.ADMIN_DASHBOARD, component: { template: '<div />' } },
  ],
})

describe('UserMenu', () => {
  let authStore: ReturnType<typeof useAuthStore>
  let pinia: ReturnType<typeof createPinia>

  beforeEach(() => {
    pinia = createPinia()
    setActivePinia(pinia)
    authStore = useAuthStore()
  })

  const mountUserMenu = (props: Record<string, unknown> = {}) => {
    mount(
      {
        render() {
          return h(VApp, {}, [h(UserMenu, props)])
        },
      },
      { global: { plugins: [pinia, vuetify, router] } },
    )
  }

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

    mountUserMenu({
      userIcon: 'mdi-account-circle',
      familyName: 'Doe',
      givenName: 'John',
      logoutText: 'Logout',
    })

    cy.get('.header-user-button').should('be.visible')
    cy.get('.header-user-name').should('contain', 'John Doe')
    cy.get('.header-user-icon').should('have.class', 'mdi-account-circle')

    cy.get('.header-user-button').click()
    cy.get('.v-list-item-title').should('contain', 'Logout')
  })

  it('renders with props-based name', () => {
    cy.stub(authStore, 'getParsedIdToken').returns(null)

    mountUserMenu({
      userIcon: 'mdi-account-circle',
      givenName: 'Jane',
      familyName: 'Smith',
      guestName: 'Guest',
      logoutText: 'Logout',
    })

    cy.get('.header-user-button').should('be.visible')
    cy.get('.header-user-name').should('contain', 'Jane Smith')
  })

  it('displays custom guest name', () => {
    cy.stub(authStore, 'getParsedIdToken').returns(null)

    mountUserMenu({
      userIcon: 'mdi-account-circle',
      guestName: 'Visitor',
      logoutText: 'Logout',
    })

    cy.get('.header-user-name').should('contain', 'Visitor')
  })

  it('shows My Projections and Admin Dashboard links for admin users', () => {
    cy.stub(authStore, 'getParsedIdToken').returns(null)
    cy.stub(authStore, 'hasRole').returns(true)

    mountUserMenu({ logoutText: 'Logout' })

    cy.get('.header-user-button').click()
    cy.get('.v-list-item-title').should('contain', 'My Projections')
    cy.get('.v-list-item-title').should('contain', 'Admin Dashboard')
  })
})
