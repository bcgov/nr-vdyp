import type { Meta, StoryObj } from '@storybook/vue3-vite'
import ActionPanel from './ActionPanel.vue'

const meta: Meta<typeof ActionPanel> = {
  title: 'components/projection/input/ActionPanel',
  component: ActionPanel,
  tags: ['autodocs'],
  argTypes: {
    isConfirmEnabled: {
      control: { type: 'boolean' },
    },
    isConfirmed: {
      control: { type: 'boolean' },
    },
    hideClearButton: {
      control: { type: 'boolean' },
    },
    hideEditButton: {
      control: { type: 'boolean' },
    },
    showCancelButton: {
      control: { type: 'boolean' },
    },
    onClear: { action: 'clear' },
    onConfirm: { action: 'confirm' },
    onEdit: { action: 'edit' },
    onCancel: { action: 'cancel' },
  },
}

export default meta

type Story = StoryObj<typeof ActionPanel>

const render: Story['render'] = (args) => ({
  components: { ActionPanel },
  setup() {
    return { args }
  },
  template: `
    <ActionPanel
      v-bind="args"
      @clear="args.onClear"
      @confirm="args.onConfirm"
      @edit="args.onEdit"
      @cancel="args.onCancel"
    />
  `,
})

export const Default: Story = {
  render,
  args: {
    isConfirmEnabled: true,
    isConfirmed: false,
    hideClearButton: false,
    hideEditButton: false,
    showCancelButton: false,
  },
}

export const Disabled: Story = {
  render,
  args: {
    isConfirmEnabled: false,
    isConfirmed: false,
  },
}

export const Confirmed: Story = {
  render,
  args: {
    isConfirmEnabled: true,
    isConfirmed: true,
  },
}

export const WithCancelButton: Story = {
  render,
  args: {
    isConfirmEnabled: true,
    isConfirmed: false,
    showCancelButton: true,
  },
}

export const HideClearButton: Story = {
  render,
  args: {
    isConfirmEnabled: true,
    isConfirmed: false,
    hideClearButton: true,
  },
}

export const HideEditButton: Story = {
  render,
  args: {
    isConfirmEnabled: true,
    isConfirmed: true,
    hideEditButton: true,
  },
}
