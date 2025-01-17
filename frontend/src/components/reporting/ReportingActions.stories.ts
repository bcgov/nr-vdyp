import type { Meta, StoryObj } from '@storybook/vue3'
import ReportingActions from './ReportingActions.vue'

const meta: Meta<typeof ReportingActions> = {
  title: 'components/reporting/ReportingActions',
  component: ReportingActions,
  tags: ['autodocs'],
  argTypes: {
    isButtonDisabled: {
      control: 'boolean',
      description: 'Disables both buttons if true',
      defaultValue: false,
    },
    onPrint: {
      action: 'print',
      description: 'Emitted when the Print button is clicked',
    },
    onDownload: {
      action: 'download',
      description: 'Emitted when the Download button is clicked',
    },
  },
}

export default meta

type Story = StoryObj<typeof ReportingActions>

export const Enabled: Story = {
  args: {
    isButtonDisabled: false,
  },
}

export const Disabled: Story = {
  args: {
    isButtonDisabled: true,
  },
}
