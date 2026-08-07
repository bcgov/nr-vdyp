import type { Meta, StoryObj } from '@storybook/vue3-vite'
import AdminResourceSummary from './AdminResourceSummary.vue'

const meta: Meta<typeof AdminResourceSummary> = {
  title: 'Components/projection/list/AdminResourceSummary',
  component: AdminResourceSummary,
  tags: ['autodocs'],
  argTypes: {
    totalRunning: {
      control: { type: 'number' },
      description: 'Total number of currently running projections',
    },
    threadsInUse: {
      control: { type: 'number' },
      description: 'Number of batch threads currently in use',
    },
    threadCapacity: {
      control: { type: 'number' },
      description: 'Maximum available batch thread capacity',
    },
    threadUsagePercent: {
      control: { type: 'number' },
      description: 'Thread usage percentage shown in the progress bar',
    },
    stuckCount: {
      control: { type: 'number' },
      description: 'Total number of projections currently flagged as stuck',
    },
  },
}

export default meta
type Story = StoryObj<typeof AdminResourceSummary>

export const Default: Story = {
  args: {
    totalRunning: 5,
    threadsInUse: 3,
    threadCapacity: 10,
    threadUsagePercent: 30,
    stuckCount: 1,
  },
}
