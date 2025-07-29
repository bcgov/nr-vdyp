import type { Meta, StoryObj } from '@storybook/vue3-vite'
import UserMenu from './UserMenu.vue'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '@/stores/common/authStore'

const meta: Meta<typeof UserMenu> = {
  title: 'components/layout/UserMenu',
  component: UserMenu,
  tags: ['autodocs'],
  decorators: [
    () => ({
      setup() {
        const pinia = createPinia()
        setActivePinia(pinia)
        const authStore = useAuthStore()
        authStore.getParsedIdToken = () => ({
          at_hash: null,
          aud: null,
          azp: null,
          client_roles: [],
          display_name: null,
          email: null,
          email_verified: null,
          exp: null,
          family_name: 'Doe',
          given_name: 'John',
          iat: null,
          identity_provider: null,
          idir_user_guid: null,
          idir_username: null,
          iss: null,
          jti: null,
          name: null,
          nonce: null,
          preferred_username: null,
          session_state: null,
          sid: null,
          sub: null,
          typ: null,
          user_principal_name: null,
        })
        return {}
      },
      template: `
        <div style="padding: 20px; background-color: rgb(0, 51, 102); display: flex; justify-content: center;" data-testid="story-root">
          <story />
        </div>`,
    }),
  ],
  argTypes: {
    userIcon: { control: 'text' },
    guestName: { control: 'text' },
    logoutText: { control: 'text' },
    givenName: { control: 'text' },
    familyName: { control: 'text' },
  },
}

export default meta
type Story = StoryObj<typeof UserMenu>

export const Default: Story = {
  args: {
    userIcon: 'mdi-account-circle',
    guestName: 'Guest',
    logoutText: 'Logout',
    givenName: 'John',
    familyName: 'Doe',
  },
  play: () => {
    cy.get('[data-testid="story-root"]')
      .find('.header-user-button')
      .should('exist')
    cy.get('[data-testid="story-root"]')
      .find('.header-user-name')
      .should('contain', 'John Doe') // Token-based name
    cy.get('[data-testid="story-root"]')
      .find('.header-user-icon')
      .should('have.class', 'mdi-account-circle')
    cy.get('[data-testid="story-root"]').find('.header-user-button').click()
    cy.get('[data-testid="story-root"]')
      .find('.v-list-item-title')
      .should('contain', 'Logout')
  },
}

export const GuestUser: Story = {
  args: {
    userIcon: 'mdi-account-circle',
    guestName: 'Guest',
    logoutText: 'Logout',
  },
  play: () => {
    cy.stub(useAuthStore(), 'getParsedIdToken').returns(null)
    cy.get('[data-testid="story-root"]')
      .find('.header-user-button')
      .should('exist')
    cy.get('[data-testid="story-root"]')
      .find('.header-user-name')
      .should('contain', 'Guest')
    cy.get('[data-testid="story-root"]')
      .find('.header-user-icon')
      .should('have.class', 'mdi-account-circle')
    cy.get('[data-testid="story-root"]').find('.header-user-button').click()
    cy.get('[data-testid="story-root"]')
      .find('.v-list-item-title')
      .should('contain', 'Logout')
  },
}

export const PropBasedName: Story = {
  args: {
    userIcon: 'mdi-account-circle',
    givenName: 'Jane',
    familyName: 'Smith',
    guestName: 'Guest',
    logoutText: 'Logout',
  },
  play: () => {
    cy.stub(useAuthStore(), 'getParsedIdToken').returns(null)
    cy.get('[data-testid="story-root"]')
      .find('.header-user-button')
      .should('exist')
    cy.get('[data-testid="story-root"]')
      .find('.header-user-name')
      .should('contain', 'Jane Smith')
    cy.get('[data-testid="story-root"]')
      .find('.header-user-icon')
      .should('have.class', 'mdi-account-circle')
    cy.get('[data-testid="story-root"]').find('.header-user-button').click()
    cy.get('[data-testid="story-root"]')
      .find('.v-list-item-title')
      .should('contain', 'Logout')
  },
}
