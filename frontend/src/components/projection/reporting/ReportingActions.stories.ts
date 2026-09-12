import type { Meta, StoryObj } from '@storybook/vue3-vite'
import ReportingActions from './ReportingActions.vue'
import { REPORTING_TAB } from '@/constants/constants'

const meta: Meta<typeof ReportingActions> = {
  title: 'components/projection/reporting/ReportingActions',
  component: ReportingActions,
  tags: ['autodocs'],
  argTypes: {
    isButtonDisabled: {
      control: { type: 'boolean' },
      description: 'Disables both the Print and Download buttons when true.',
    },
    tabname: {
      control: { type: 'select' },
      options: Object.values(REPORTING_TAB),
      description:
        'Active reporting tab. Changes the Download button label to "Download Yield Table" on the Model Report tab.',
    },
  }
}

export default meta

type Story = StoryObj<typeof ReportingActions>

export const ModelReport: Story = {
  render: (args) => ({
    components: { ReportingActions },
    setup() {
      return { args }
    },
    template: `
      <ReportingActions
        v-bind="args"
        @print="args.print"
        @download="args.download"
      />
    `,
  }),
  args: {
    isButtonDisabled: false,
    tabname: REPORTING_TAB.MODEL_REPORT,
  },
  parameters: {
    docs: {
      description: {
        story: 'Model Report tab - Download button shows "Download Yield Table".',
      },
    },
  },
}

export const OtherTab: Story = {
  render: (args) => ({
    components: { ReportingActions },
    setup() {
      return { args }
    },
    template: `
      <ReportingActions
        v-bind="args"
        @print="args.print"
        @download="args.download"
      />
    `,
  }),
  args: {
    isButtonDisabled: false,
    tabname: REPORTING_TAB.VIEW_ERR_MSG,
  },
  parameters: {
    docs: {
      description: {
        story: 'Non-Model-Report tab - Download button shows "Download".',
      },
    },
  },
}

export const Disabled: Story = {
  render: (args) => ({
    components: { ReportingActions },
    setup() {
      return { args }
    },
    template: `
      <ReportingActions
        v-bind="args"
        @print="args.print"
        @download="args.download"
      />
    `,
  }),
  args: {
    isButtonDisabled: true,
    tabname: REPORTING_TAB.MODEL_REPORT,
  },
  parameters: {
    docs: {
      description: {
        story: 'Both buttons disabled - used while a projection is still running or loading.',
      },
    },
  },
}
