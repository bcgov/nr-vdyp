import type { Meta, StoryObj } from '@storybook/vue3'
import ReportConfiguration from './ReportConfiguration.vue'
import { CONSTANTS, OPTIONS } from '@/constants'

const meta: Meta<typeof ReportConfiguration> = {
  title: 'components/input/ReportConfiguration',
  component: ReportConfiguration,
  tags: ['autodocs'],
  argTypes: {
    startingAge: {
      control: { type: 'number' },
      description: 'The starting age for the report configuration',
      defaultValue: null,
    },
    finishingAge: {
      control: { type: 'number' },
      description: 'The finishing age for the report configuration',
      defaultValue: null,
    },
    ageIncrement: {
      control: { type: 'number' },
      description: 'The age increment for the report configuration',
      defaultValue: null,
    },
    volumeReported: {
      control: { type: 'array' },
      description: 'Selected volume reported options',
      defaultValue: [],
    },
    includeInReport: {
      control: { type: 'array' },
      description: 'Selected include in report options',
      defaultValue: [],
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
      defaultValue: '',
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
    startingAge: CONSTANTS.NUM_INPUT_LIMITS.STARTING_AGE_MIN,
    finishingAge: CONSTANTS.NUM_INPUT_LIMITS.FINISHING_AGE_MIN,
    ageIncrement: CONSTANTS.NUM_INPUT_LIMITS.AGE_INC_MIN,
    volumeReported: [OPTIONS.volumeReportedOptions[0].value],
    includeInReport: [OPTIONS.includeInReportOptions[0].value],
    projectionType: OPTIONS.projectionTypeOptions[0].value,
    reportTitle: 'Sample Report Title',
    isDisabled: false,
  },
}

export const DisabledConfiguration: Story = {
  args: {
    startingAge: CONSTANTS.NUM_INPUT_LIMITS.STARTING_AGE_MIN,
    finishingAge: CONSTANTS.NUM_INPUT_LIMITS.FINISHING_AGE_MIN,
    ageIncrement: CONSTANTS.NUM_INPUT_LIMITS.AGE_INC_MIN,
    volumeReported: [OPTIONS.volumeReportedOptions[0].value],
    includeInReport: [OPTIONS.includeInReportOptions[0].value],
    projectionType: OPTIONS.projectionTypeOptions[0].value,
    reportTitle: 'Disabled Report Title',
    isDisabled: true,
  },
}
