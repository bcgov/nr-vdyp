import type { Meta, StoryObj } from '@storybook/vue3'
import AppRunModelButton from './AppRunModelButton.vue'

const meta: Meta<typeof AppRunModelButton> = {
  title: 'components/common/AppRunModelButton',
  component: AppRunModelButton,
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

type Story = StoryObj<typeof AppRunModelButton>

export const FileUploadRunModel: Story = {
  render: (args) => ({
    components: { AppRunModelButton },
    setup() {
      return { args }
    },
    template: `
      <AppRunModelButton
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
    components: { AppRunModelButton },
    setup() {
      return { args }
    },
    template: `
      <AppRunModelButton
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
