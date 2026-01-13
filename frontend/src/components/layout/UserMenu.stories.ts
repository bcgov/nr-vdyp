import type { Meta, StoryObj } from '@storybook/vue3-vite'
import UserMenu from './UserMenu.vue'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '@/stores/common/authStore'

const meta = {
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
        <div style="padding: 20px; display: flex; justify-content: center;">
          <story />
        </div>
      `,
    }),
  ],
  argTypes: {
    userIcon: {
      control: 'text',
      description: 'Icon to display for the user',
    },
    givenName: {
      control: 'text',
      description: 'User given name (overrides auth store)',
    },
    familyName: {
      control: 'text',
      description: 'User family name (overrides auth store)',
    },
    guestName: {
      control: 'text',
      description: 'Name to display when user is not authenticated',
    },
    logoutText: {
      control: 'text',
      description: 'Text to display for logout menu item',
    },
  },
} satisfies Meta<typeof UserMenu>

export default meta
type Story = StoryObj<typeof meta>

export const Default: Story = {
  args: {
    userIcon: 'mdi-account-circle',
    guestName: 'Guest',
    logoutText: 'Logout',
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
}
