import type { Meta, StoryObj } from '@storybook/vue3'
import TheHeader from './TheHeader.vue'
import { createPinia, setActivePinia } from 'pinia'

import BCLogo from './BCLogo.vue'
import HeaderTitle from './HeaderTitle.vue'
import TrainingSupport from './TrainingSupport.vue'
import UserMenu from './UserMenu.vue'

const meta: Meta<typeof TheHeader> = {
  title: 'components/layout/TheHeader',
  component: TheHeader,
  tags: ['autodocs'],
  decorators: [
    () => ({
      setup() {
        const pinia = createPinia()
        setActivePinia(pinia)

        return {}
      },
      template: `
        <div style="display: flex; justify-content: center; align-items: center; height: 100vh;">
          <div style="background-color: rgb(0, 51, 102); width: 100%; max-width: 1280px;">
            <story />
          </div>
        </div>
      `,
    }),
  ],
  argTypes: {
    // Add controls for testing child components
    logoProps: { control: 'object', description: 'Props for BCLogo component' },
    titleProps: {
      control: 'object',
      description: 'Props for HeaderTitle component',
    },
    userMenuProps: {
      control: 'object',
      description: 'Props for UserMenu component',
    },
  },
}

export default meta
type Story = StoryObj<typeof TheHeader>

export const Default: Story = {
  args: {
    logoProps: {},
    titleProps: {},
    userMenuProps: {},
  },
  render: (args) => ({
    components: { TheHeader, BCLogo, HeaderTitle, TrainingSupport, UserMenu },
    setup() {
      return { args }
    },
    template: `
      <TheHeader
        v-bind="args"
        v-slot="{ logoProps, titleProps, userMenuProps }"
      />
    `,
  }),
}

export const CustomStyle: Story = {
  args: {
    logoProps: { maxHeight: 50, maxWidth: 150, marginLeft: 15 },
    titleProps: {
      text: 'Custom Header Title',
      style: { fontSize: '24px', color: 'white', fontWeight: 'bold' },
    },
    userMenuProps: {
      userIcon: 'mdi-face',
      guestName: 'Anonymous',
      logoutText: 'Sign Out',
    },
  },
}
