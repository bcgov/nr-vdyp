import type { Meta, StoryObj } from '@storybook/vue3-vite'
import BCLogo from './BCLogo.vue'

const meta = {
  title: 'components/layout/BCLogo',
  component: BCLogo,
  tags: ['autodocs'],
  decorators: [
    () => ({
      template: `
        <div style="padding: 20px; display: flex; justify-content: center;">
          <story />
        </div>
      `,
    }),
  ],
} satisfies Meta<typeof BCLogo>

export default meta
type Story = StoryObj<typeof meta>

export const Default: Story = {}
