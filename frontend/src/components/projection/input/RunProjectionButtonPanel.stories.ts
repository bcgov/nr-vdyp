import type { Meta, StoryObj } from '@storybook/vue3-vite'
import RunProjectionButtonPanel from './RunProjectionButtonPanel.vue'

const meta: Meta<typeof RunProjectionButtonPanel> = {
  title: 'components/projection/input/RunProjectionButtonPanel',
  component: RunProjectionButtonPanel,
  tags: ['autodocs'],
  argTypes: {
    isDisabled: {
      control: { type: 'boolean' },
    },
    showCancelButton: {
      control: { type: 'boolean' },
    },
    cardClass: {
      control: 'text',
    },
    cardActionsClass: {
      control: 'text',
    },
    disabledText: {
      control: 'text',
    },
    onRunModel: {
      action: 'runModel',
    },
    onCancelRun: {
      action: 'cancelRun',
    },
  },
}

export default meta

type Story = StoryObj<typeof RunProjectionButtonPanel>

export const Default: Story = {
  render: (args) => ({
    components: { RunProjectionButtonPanel },
    setup() {
      return { args }
    },
    template: `
      <RunProjectionButtonPanel
        v-bind="args"
        @runModel="args.onRunModel"
        @cancelRun="args.onCancelRun"
      />
    `,
  }),
  args: {
    isDisabled: false,
    showCancelButton: false,
  },
}

export const Disabled: Story = {
  render: Default.render,
  args: {
    isDisabled: true,
    showCancelButton: false,
  },
}

export const DisabledWithTooltip: Story = {
  render: Default.render,
  args: {
    isDisabled: true,
    showCancelButton: false,
    disabledText: 'Please fill in all required fields before running the projection.',
  },
}

export const CancelRun: Story = {
  render: Default.render,
  args: {
    isDisabled: false,
    showCancelButton: true,
  },
}
