import type { Meta, StoryObj } from '@storybook/vue3-vite'
import ParameterSelectionProgressBar from './ParameterSelectionProgressBar.vue'

const meta: Meta<typeof ParameterSelectionProgressBar> = {
  title: 'components/projection/input/ParameterSelectionProgressBar',
  component: ParameterSelectionProgressBar,
  tags: ['autodocs'],
  argTypes: {
    sections: {
      control: 'object',
      description: 'List of sections with label and completed state.',
    },
    percentage: {
      control: { type: 'range', min: 0, max: 100, step: 1 },
      description: 'Current completion percentage (0–100).',
    },
    completedCount: {
      control: { type: 'number', min: 0 },
      description: 'Number of completed sections.',
    },
    projectionStatus: {
      control: { type: 'select' },
      options: ['Draft', 'Ready', 'Running', 'Failed'],
      description: 'Current projection status. Determines progress bar color and header display.',
    },
  },
}

export default meta

type Story = StoryObj<typeof ParameterSelectionProgressBar>

const render: Story['render'] = (args) => ({
  components: { ParameterSelectionProgressBar },
  setup() {
    return { args }
  },
  template: `<ParameterSelectionProgressBar v-bind="args" />`,
})

const allSections = [
  { label: 'Details', completed: false },
  { label: 'Species', completed: false },
  { label: 'Site', completed: false },
  { label: 'Stand', completed: false },
  { label: 'Report', completed: false },
]

export const NoneComplete: Story = {
  render,
  args: {
    sections: allSections,
    percentage: 0,
    completedCount: 0,
    projectionStatus: 'Draft',
  },
}

export const PartiallyComplete: Story = {
  render,
  args: {
    sections: [
      { label: 'Details', completed: true },
      { label: 'Species', completed: true },
      { label: 'Site', completed: true },
      { label: 'Stand', completed: false },
      { label: 'Report', completed: false },
    ],
    percentage: 60,
    completedCount: 3,
    projectionStatus: 'Draft',
  },
}

export const Ready: Story = {
  render,
  args: {
    sections: [
      { label: 'Details', completed: true },
      { label: 'Species', completed: true },
      { label: 'Site', completed: true },
      { label: 'Stand', completed: true },
      { label: 'Report', completed: true },
    ],
    percentage: 100,
    completedCount: 5,
    projectionStatus: 'Ready',
  },
}

export const Running: Story = {
  render,
  args: {
    sections: [
      { label: 'Details', completed: true },
      { label: 'Species', completed: true },
      { label: 'Site', completed: true },
      { label: 'Stand', completed: true },
      { label: 'Report', completed: true },
    ],
    percentage: 100,
    completedCount: 5,
    projectionStatus: 'Running',
  },
}

export const Failed: Story = {
  render,
  args: {
    sections: [
      { label: 'Details', completed: true },
      { label: 'Species', completed: true },
      { label: 'Site', completed: true },
      { label: 'Stand', completed: true },
      { label: 'Report', completed: true },
    ],
    percentage: 100,
    completedCount: 5,
    projectionStatus: 'Failed',
  },
}
