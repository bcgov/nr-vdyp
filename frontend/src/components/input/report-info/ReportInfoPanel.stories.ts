import type { Meta, StoryObj } from '@storybook/vue3'
import ReportInfoPanel from './ReportInfoPanel.vue'

const meta: Meta<typeof ReportInfoPanel> = {
  title: 'components/input/report-info/ReportInfoPanel',
  component: ReportInfoPanel,
  argTypes: {
    panelOpenStates: {
      control: { type: 'object' },
      description: 'State of panel (open or closed)',
      defaultValue: {
        reportInfo: 1, // 1 for open, 0 for closed
      },
    },
    startingAge: {
      control: { type: 'number' },
      description: 'Starting age for the report.',
      defaultValue: 10,
    },
    finishingAge: {
      control: { type: 'number' },
      description: 'Finishing age for the report.',
      defaultValue: 50,
    },
    ageIncrement: {
      control: { type: 'number' },
      description: 'Age increment value.',
      defaultValue: 5,
    },
    volumeReported: {
      control: { type: 'array' },
      description: 'Volume reported options.',
      defaultValue: ['Option 1', 'Option 2'],
    },
    includeInReport: {
      control: { type: 'array' },
      description: 'Items to include in the report.',
      defaultValue: ['Include 1', 'Include 2'],
    },
    projectionType: {
      control: { type: 'text' },
      description: 'Projection type for the report.',
      defaultValue: 'Projection 1',
    },
    reportTitle: {
      control: { type: 'text' },
      description: 'Report title.',
      defaultValue: 'Sample Report',
    },
    isConfirmEnabled: {
      control: { type: 'boolean' },
      description: 'Determines if the confirm actions are enabled.',
      defaultValue: true,
    },
    isConfirmed: {
      control: { type: 'boolean' },
      description: 'Indicates if the panel is confirmed.',
      defaultValue: false,
    },
  },
  tags: ['autodocs'],
}

export default meta

type Story = StoryObj<typeof ReportInfoPanel>

export const Default: Story = {
  render: (args) => ({
    components: { ReportInfoPanel },
    setup() {
      return { args }
    },
    template: `<ReportInfoPanel v-bind="args" />`,
  }),
  args: {
    panelOpenStates: { reportInfo: 1 },
    startingAge: 10,
    finishingAge: 50,
    ageIncrement: 5,
    volumeReported: ['Option 1', 'Option 2'],
    includeInReport: ['Include 1', 'Include 2'],
    projectionType: 'Projection 1',
    reportTitle: 'Sample Report',
    isConfirmEnabled: true,
    isConfirmed: false,
  },
}

export const Confirmed: Story = {
  render: (args) => ({
    components: { ReportInfoPanel },
    setup() {
      return { args }
    },
    template: `<ReportInfoPanel v-bind="args" />`,
  }),
  args: {
    panelOpenStates: { reportInfo: 0 },
    startingAge: 20,
    finishingAge: 60,
    ageIncrement: 10,
    volumeReported: ['Option A', 'Option B'],
    includeInReport: ['Include A', 'Include B'],
    projectionType: 'Projection 2',
    reportTitle: 'Confirmed Report',
    isConfirmEnabled: false,
    isConfirmed: true,
  },
}

export const EmptyState: Story = {
  render: (args) => ({
    components: { ReportInfoPanel },
    setup() {
      return { args }
    },
    template: `<ReportInfoPanel v-bind="args" />`,
  }),
  args: {
    panelOpenStates: { reportInfo: 1 },
    startingAge: null,
    finishingAge: null,
    ageIncrement: null,
    volumeReported: [],
    includeInReport: [],
    projectionType: '',
    reportTitle: '',
    isConfirmEnabled: true,
    isConfirmed: false,
  },
}
