import type { Meta, StoryObj } from '@storybook/vue3'
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
    dialogBorderRadius: {
      control: { type: 'range', min: 0, max: 50, step: 1 },
      description: 'Dialog border radius',
      defaultValue: 8,
    },
    btnLabel: {
      control: 'text',
      description: 'Button label',
      defaultValue: 'OK',
    },
    headerBackground: {
      control: 'color',
      description: 'Background color for header',
      defaultValue: '#003366',
    },
    headerColor: {
      control: 'color',
      description: 'Color for header',
      defaultValue: '#ffffff',
    },
    actionsBackground: {
      control: 'color',
      description: 'Background color for actions',
      defaultValue: '#f6f6f6',
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

export const Primary: Story = {
  args: {
    dialog: true,
    title: 'Missing Information',
    message:
      'Input field is missing essential information which must be filled in order to confirm and continue.',
    dialogWidth: 400,
    dialogBorderRadius: 8,
    btnLabel: 'Continue Editing',
    headerBackground: '#003366',
    headerColor: '#ffffff',
    actionsBackground: '#f6f6f6',
  },
}
