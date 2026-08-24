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
    queuedCount: {
      control: { type: 'number' },
      description: 'Total number of projections currently waiting in the queue',
    },
    storagePercent: {
      control: { type: 'number' },
      description: 'PVC storage usage percentage shown in the progress bar',
    },
    storageUsedBytes: {
      control: { type: 'number' },
      description: 'Absolute PVC storage bytes used, shown in the hover tooltip',
    },
    storageTotalBytes: {
      control: { type: 'number' },
      description: 'Absolute PVC storage total capacity in bytes, shown in the hover tooltip',
    },
    storageOutOfSpec: {
      control: { type: 'boolean' },
      description: 'Whether PVC storage usage exceeds the configured out-of-spec threshold',
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
    queuedCount: 2,
    storagePercent: 20,
    storageUsedBytes: 20 * 1024 ** 3,
    storageTotalBytes: 100 * 1024 ** 3,
    storageOutOfSpec: false,
  },
}

export const StorageOutOfSpec: Story = {
  args: {
    ...Default.args,
    storagePercent: 92,
    storageUsedBytes: 92 * 1024 ** 3,
    storageOutOfSpec: true,
  },
}
