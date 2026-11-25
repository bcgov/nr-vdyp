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
      description: 'Total number of alerts (errors, warnings and info messages), shown on the Alert Count tile.',
    },
    alertCounts: {
      control: 'object',
      description:
        'Alert breakdown by type with polygon counts, shown in the Alert Count tile tooltip. While Running or Ready, its error polygon count also gives the Polygons Processed tile tooltip (successfully projected and failed to project). No tooltips when null.',
    },
    startDate: {
      control: 'text',
      description: 'ISO 8601 start date string for elapsed time calculation.',
    },
    batchFailureTypeDescription: {
      control: 'text',
      description: 'Optional batch failure type description shown when the projection fails.',
    },
    failureMessage: {
      control: 'text',
      description: 'Optional batch failure message shown when the projection fails.',
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
    alertCounts: {
      errorMessageCount: 1,
      errorPolygonCount: 1,
      warningMessageCount: 2,
      warningPolygonCount: 1,
      infoMessageCount: 0,
      infoPolygonCount: 0,
    },
    startDate: twoHoursAgo,
  },
}

export const Ready: Story = {
  render,
  args: {
    status: 'Ready',
    polygonCount: 36210,
    completedPolygonCount: 36210,
    errorCount: 4092,
    alertCounts: {
      errorMessageCount: 892,
      errorPolygonCount: 200,
      warningMessageCount: 1200,
      warningPolygonCount: 600,
      infoMessageCount: 2000,
      infoPolygonCount: 1500,
    },
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
    batchFailureTypeDescription: 'Processing Inputs',
    failureMessage: 'Could not fetch and partition input files. Exception type: Exception, Root cause: Ultimate failure Test.',
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
