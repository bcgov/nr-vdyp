import type { Meta, StoryObj } from '@storybook/vue3-vite'
import AdminProjectionCardList from './AdminProjectionCardList.vue'
import type { AdminProjection, SortOption } from '@/interfaces/interfaces'

const sortOptions: SortOption[] = [
  { title: 'Threads (Highest First)', value: 'workerCount-desc' },
  { title: 'Threads (Lowest First)', value: 'workerCount-asc' },
  { title: 'Progress (Highest First)', value: 'progress-desc' },
  { title: 'Progress (Lowest First)', value: 'progress-asc' },
  { title: 'Polygons (Highest First)', value: 'completedPolygonCount-desc' },
  { title: 'Polygons (Lowest First)', value: 'completedPolygonCount-asc' },
]

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
    isPrioritized: true,
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
    isPrioritized: false,
  },
]

const meta: Meta<typeof AdminProjectionCardList> = {
  title: 'components/projection/list/AdminProjectionCardList',
  component: AdminProjectionCardList,
  tags: ['autodocs'],
}

export default meta

type Story = StoryObj<typeof AdminProjectionCardList>

export const Default: Story = {
  args: {
    projections: sampleProjections,
    sortOptions,
    sortValue: 'workerCount-desc',
    now: Date.now(),
  },
}

export const EmptyList: Story = {
  args: {
    projections: [],
    sortOptions,
    sortValue: 'workerCount-desc',
    now: Date.now(),
  },
}
