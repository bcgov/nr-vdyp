import type { Meta, StoryObj } from '@storybook/vue3-vite'
import AppRunModelButtonPanel from './AppRunModelButtonPanel.vue'

const meta: Meta<typeof AppRunModelButtonPanel> = {
  title: 'components/common/AppRunModelButtonPanel',
  component: AppRunModelButtonPanel,
  argTypes: {
    isDisabled: {
      control: { type: 'boolean' },
      description: 'Determines whether the button is disabled.',
      defaultValue: false,
    },
    cardClass: {
      control: 'text',
      description: 'CSS class for the card element.',
      defaultValue: 'file-upload-run-model-card',
    },
    cardActionsClass: {
      control: 'text',
      description: 'CSS class for the card actions element.',
      defaultValue: 'card-actions',
    },
  },
  tags: ['autodocs'],
}

export default meta

type Story = StoryObj<typeof AppRunModelButtonPanel>

export const FileUploadRunModel: Story = {
  render: (args) => ({
    components: { AppRunModelButtonPanel },
    setup() {
      return { args }
    },
    template: `
      <AppRunModelButtonPanel
        v-bind="args"
        @runModel="args.runModel"
      />
    `,
  }),
  args: {
    isDisabled: false,
    cardClass: 'file-upload-run-model-card',
    cardActionsClass: 'card-actions',
  },
}

export const InputModelParametersRunModel: Story = {
  render: (args) => ({
    components: { AppRunModelButtonPanel },
    setup() {
      return { args }
    },
    template: `
      <AppRunModelButtonPanel
        v-bind="args"
        @runModel="args.runModel"
      />
    `,
  }),
  args: {
    isDisabled: false,
    cardClass: 'input-model-param-run-model-card',
    cardActionsClass: 'card-actions',
  },
}
