import type { Meta, StoryObj } from '@storybook/vue3-vite'
import AppPanelActions from './AppPanelActions.vue'

const meta: Meta<typeof AppPanelActions> = {
  title: 'components/common/AppPanelActions',
  component: AppPanelActions,
  tags: ['autodocs'],
  argTypes: {
    isConfirmEnabled: {
      control: { type: 'boolean' },
      description:
        'Determines whether the confirm and clear buttons are enabled.',
      defaultValue: true,
    },
    isConfirmed: {
      control: { type: 'boolean' },
      description:
        'Determines whether the Confirm button is replaced by the Edit button.',
      defaultValue: false,
    },
  },
}

export default meta

type Story = StoryObj<typeof AppPanelActions>

export const Default: Story = {
  render: (args) => ({
    components: { AppPanelActions },
    setup() {
      return { args }
    },
    template: `
      <AppPanelActions
        v-bind="args"
        @clear="args.clear"
        @confirm="args.confirm"
        @edit="args.edit"
      />
    `,
  }),
  args: {
    isConfirmEnabled: true,
    isConfirmed: false,
  },
}

export const ConfirmDisabled: Story = {
  render: (args) => ({
    components: { AppPanelActions },
    setup() {
      return { args }
    },
    template: `
      <AppPanelActions
        v-bind="args"
        @clear="args.clear"
        @confirm="args.confirm"
        @edit="args.edit"
      />
    `,
  }),
  args: {
    isConfirmEnabled: false,
    isConfirmed: false,
  },
}

export const Confirmed: Story = {
  render: (args) => ({
    components: { AppPanelActions },
    setup() {
      return { args }
    },
    template: `
      <AppPanelActions
        v-bind="args"
        @clear="args.clear"
        @confirm="args.confirm"
        @edit="args.edit"
      />
    `,
  }),
  args: {
    isConfirmEnabled: true,
    isConfirmed: true,
  },
}
