import type { Meta, StoryObj } from '@storybook/vue3-vite'
import ProjectionRunProgressBar from './ProjectionRunProgressBar.vue'

const meta: Meta<typeof ProjectionRunProgressBar> = {
  title: 'components/projection/input/ProjectionRunProgressBar',
  component: ProjectionRunProgressBar,
  tags: ['autodocs'],
  argTypes: {
    status: {
      control: { type: 'select' },
      options: ['Running', 'Ready', 'Failed', 'Cancelled'],
      description: 'Current projection status.',
    },
    polygonCount: {
      control: { type: 'number', min: 0 },
      description: 'Total number of polygons to process.',
    },
    completedPolygonCount: {
      control: { type: 'number', min: 0 },
      description: 'Number of polygons processed so far.',
    },
    errorCount: {
      control: { type: 'number', min: 0 },
      description: 'Number of errors encountered.',
    },
    startDate: {
      control: 'text',
      description: 'ISO 8601 start date string for elapsed time calculation.',
    },
  },
}

export default meta

type Story = StoryObj<typeof ProjectionRunProgressBar>

const render: Story['render'] = (args) => ({
  components: { ProjectionRunProgressBar },
  setup() {
    return { args }
  },
  template: `<ProjectionRunProgressBar v-bind="args" />`,
})

const twoHoursAgo = new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString()

export const Running: Story = {
  render,
  args: {
    status: 'Running',
    polygonCount: 10000,
    completedPolygonCount: 4200,
    errorCount: 3,
    startDate: twoHoursAgo,
  },
}

export const Ready: Story = {
  render,
  args: {
    status: 'Ready',
    polygonCount: 10000,
    completedPolygonCount: 10000,
    errorCount: 3,
    startDate: twoHoursAgo,
  },
}

export const Failed: Story = {
  render,
  args: {
    status: 'Failed',
    polygonCount: 10000,
    completedPolygonCount: 6100,
    errorCount: 42,
    startDate: twoHoursAgo,
  },
}

export const Cancelled: Story = {
  render,
  args: {
    status: 'Cancelled',
    polygonCount: 10000,
    completedPolygonCount: 3300,
    errorCount: 0,
    startDate: twoHoursAgo,
  },
}

export const NoData: Story = {
  render,
  args: {
    status: 'Running',
    polygonCount: null,
    completedPolygonCount: null,
    errorCount: null,
    startDate: null,
  },
}
