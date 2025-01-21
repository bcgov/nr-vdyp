import type { Meta, StoryObj } from '@storybook/vue3'
import ReportConfiguration from './ReportConfiguration.vue'
import { DEFAULTS, OPTIONS } from '@/constants'

const meta: Meta<typeof ReportConfiguration> = {
  title: 'components/input/ReportConfiguration',
  component: ReportConfiguration,
  tags: ['autodocs'],
  argTypes: {
    startingAge: {
      control: { type: 'number' },
      description: 'The starting age for the report configuration',
      defaultValue: DEFAULTS.DEFAULT_VALUES.STARTING_AGE,
    },
    finishingAge: {
      control: { type: 'number' },
      description: 'The finishing age for the report configuration',
      defaultValue: DEFAULTS.DEFAULT_VALUES.FINISHING_AGE,
    },
    ageIncrement: {
      control: { type: 'number' },
      description: 'The age increment for the report configuration',
      defaultValue: DEFAULTS.DEFAULT_VALUES.AGE_INCREMENT,
    },
    volumeReported: {
      control: { type: 'array' },
      description: 'Selected volume reported options',
      defaultValue: [],
    },
    includeInReport: {
      control: { type: 'array' },
      description: 'Selected include in report options',
      defaultValue: [OPTIONS.volumeReportedOptions[0].value],
    },
    projectionType: {
      control: {
        type: 'select',
        options: OPTIONS.projectionTypeOptions.map((opt) => opt.value),
      },
      description: 'The projection type for the report configuration',
      defaultValue: null,
    },
    reportTitle: {
      control: { type: 'text' },
      description: 'The report title',
      defaultValue: DEFAULTS.DEFAULT_VALUES.REPORT_TITLE,
    },
    isDisabled: {
      control: { type: 'boolean' },
      description: 'Disables the entire form when set to true',
      defaultValue: false,
    },
  },
}

export default meta
type Story = StoryObj<typeof ReportConfiguration>

export const DefaultConfiguration: Story = {
  args: {
    startingAge: DEFAULTS.DEFAULT_VALUES.STARTING_AGE,
    finishingAge: DEFAULTS.DEFAULT_VALUES.FINISHING_AGE,
    ageIncrement: DEFAULTS.DEFAULT_VALUES.AGE_INCREMENT,
    volumeReported: [OPTIONS.volumeReportedOptions[0].value],
    includeInReport: [],
    projectionType: DEFAULTS.DEFAULT_VALUES.PROJECTION_TYPE,
    reportTitle: DEFAULTS.DEFAULT_VALUES.REPORT_TITLE,
    isDisabled: false,
  },
}

export const DisabledConfiguration: Story = {
  args: {
    startingAge: DEFAULTS.DEFAULT_VALUES.STARTING_AGE,
    finishingAge: DEFAULTS.DEFAULT_VALUES.FINISHING_AGE,
    ageIncrement: DEFAULTS.DEFAULT_VALUES.AGE_INCREMENT,
    volumeReported: [OPTIONS.volumeReportedOptions[0].value],
    includeInReport: [],
    projectionType: DEFAULTS.DEFAULT_VALUES.PROJECTION_TYPE,
    reportTitle: DEFAULTS.DEFAULT_VALUES.REPORT_TITLE,
    isDisabled: true,
  },
}
