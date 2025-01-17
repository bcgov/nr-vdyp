import type { Meta, StoryObj } from '@storybook/vue3'
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
        return {}
      },
      template: `
        <div style="padding: 20px; background-color: rgb(0, 51, 102); display: flex; justify-content: center;">
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
}
