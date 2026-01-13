import type { Meta, StoryObj } from '@storybook/vue3-vite'
import AppMessageDialog from './AppMessageDialog.vue'

const meta: Meta<typeof AppMessageDialog> = {
  title: 'components/common/AppMessageDialog',
  component: AppMessageDialog,
  tags: ['autodocs'],
  argTypes: {
    dialog: {
      control: 'boolean',
      description: 'Dialog visibility',
      defaultValue: true,
    },
    title: {
      control: 'text',
      description: 'Dialog title',
      defaultValue: 'VDYP Message',
    },
    message: {
      control: 'text',
      description: 'Dialog message',
      defaultValue: 'This is a message.',
    },
    dialogWidth: {
      control: { type: 'range', min: 200, max: 800, step: 50 },
      description: 'Dialog width',
      defaultValue: 400,
    },
    btnLabel: {
      control: 'text',
      description: 'Button label',
      defaultValue: 'OK',
    },
    variant: {
      control: 'select',
      options: ['info', 'confirmation', 'warning', 'error'],
      description: 'Dialog variant (determines icon and color scheme)',
      defaultValue: 'info',
    },
    scrollStrategy: {
      control: 'text',
      description: 'Scroll strategy for dialog',
      defaultValue: 'block',
    },
    'onUpdate:dialog': {
      action: 'update:dialog',
      description: 'Emitted when dialog state changes',
    },
    onClose: {
      action: 'close',
      description: 'Emitted when close button is clicked',
    },
  },
}

export default meta
type Story = StoryObj<typeof AppMessageDialog>

export const Info: Story = {
  args: {
    dialog: true,
    title: 'Information',
    message:
      'This is an informational message to provide additional context or details.',
    dialogWidth: 400,
    btnLabel: 'Continue Editing',
    variant: 'info',
  },
}

export const Confirmation: Story = {
  args: {
    dialog: true,
    title: 'Success',
    message: 'Your changes have been saved successfully.',
    dialogWidth: 400,
    btnLabel: 'OK',
    variant: 'confirmation',
  },
}

export const Warning: Story = {
  args: {
    dialog: true,
    title: 'Missing Information',
    message:
      'Input field is missing essential information which must be filled in order to confirm and continue.',
    dialogWidth: 400,
    btnLabel: 'Continue Editing',
    variant: 'warning',
  },
}

export const ErrorState: Story = {
  args: {
    dialog: true,
    title: 'Error Occurred',
    message:
      'An unexpected error has occurred. Please try again or contact support if the problem persists.',
    dialogWidth: 400,
    btnLabel: 'Close',
    variant: 'error',
  },
}
