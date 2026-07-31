import type { Meta, StoryObj } from '@storybook/vue3-vite'
import AdminProjectionTable from './AdminProjectionTable.vue'
import type { AdminProjection } from '@/interfaces/interfaces'

const sampleProjections: AdminProjection[] = [
  {
    projectionGUID: '63c26de0-f6f3-42c2-bbb2-3c2b1e60d033',
    title: 'TFL48 Timber Supply Analysis',
    ownerDisplayName: 'R. MacLeod',
    userType: 'IDIR',
    startDate: '2026-01-10T14:30:00',
    status: 'Running',
    workerCount: 16,
    completedPolygonCount: 184320,
    polygonCount: 256000,
  },
  {
    projectionGUID: '63c26de0-f6f3-42c2-ccc2-3c2b1e60d033',
    title: 'North Interior Wildfire Risk',
    ownerDisplayName: 'M. Petrov',
    userType: 'IDIR',
    startDate: '2026-01-10T12:20:00',
    status: 'Running',
    workerCount: 6,
    completedPolygonCount: 121584,
    polygonCount: 256000,
  },
]

const meta: Meta<typeof AdminProjectionTable> = {
  title: 'components/projection/list/AdminProjectionTable',
  component: AdminProjectionTable,
  tags: ['autodocs'],
}

export default meta

type Story = StoryObj<typeof AdminProjectionTable>

export const Default: Story = {
  args: {
    projections: sampleProjections,
    sortBy: '',
    sortOrder: 'asc',
    now: Date.now(),
  },
}

export const EmptyList: Story = {
  args: {
    projections: [],
    sortBy: '',
    sortOrder: 'asc',
    now: Date.now(),
  },
}
