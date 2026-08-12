import type { Meta, StoryObj } from '@storybook/vue3-vite'
import AdminCancellationBanner from './AdminCancellationBanner.vue'

const meta: Meta<typeof AdminCancellationBanner> = {
  title: 'Components/projection/input/AdminCancellationBanner',
  component: AdminCancellationBanner,
  tags: ['autodocs'],
  argTypes: {
    reason: {
      control: { type: 'text' },
      description: 'The admin-provided cancellation reason',
    },
  },
}

export default meta
type Story = StoryObj<typeof AdminCancellationBanner>

export const Default: Story = {
  args: {
    reason: 'Cancelled by admin for maintenance',
  },
}
