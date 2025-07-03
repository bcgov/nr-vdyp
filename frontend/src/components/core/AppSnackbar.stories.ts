import type { Meta, StoryObj } from '@storybook/vue3-vite'
import AppSnackbar from './AppSnackbar.vue'

const meta: Meta<typeof AppSnackbar> = {
  title: 'components/core/AppSnackbar',
  component: AppSnackbar,
  tags: ['autodocs'],
  argTypes: {
    isVisible: {
      control: 'boolean',
      description: 'Controls visibility of the snackbar',
      defaultValue: true,
    },
    message: {
      control: 'text',
      description: 'Notification message to display',
      defaultValue: 'This is a notification',
    },
    type: {
      control: 'select',
      options: ['info', 'success', 'error', 'warning', ''],
      description: 'Type of notification',
      defaultValue: 'info',
    },
    timeout: {
      control: { type: 'range', min: 2000, max: 10000, step: 500 },
      description: 'Duration for the notification to stay visible',
      defaultValue: 5000,
    },
    autoTimeout: {
      control: 'boolean',
      description: 'Enable or disable auto timeout',
      defaultValue: true,
    },
    showTimer: {
      control: 'boolean',
      description: 'Show or hide timer on snackbar',
      defaultValue: false,
    },
    location: {
      control: 'select',
      options: ['top', 'center', 'bottom', 'right', 'left'],
      description: 'Location of the snackbar',
      defaultValue: 'top',
    },
    color: {
      control: 'color',
      description: 'Snackbar background color',
      defaultValue: '#2196F3',
    },
    rounded: {
      control: 'boolean',
      description: 'Enable or disable rounded corners',
      defaultValue: true,
    },
    onClose: { action: 'close', description: 'Emitted when snackbar closes' },
  },
}

export default meta
type Story = StoryObj<typeof AppSnackbar>

export const InfoNotification: Story = {
  args: {
    isVisible: true,
    autoTimeout: true,
    showTimer: false,
    message: 'This is an informational message.',
    type: 'info',
    timeout: 5000,
    location: 'top',
    color: '#d9eaf7',
    rounded: true,
  },
}

export const ErrorNotification: Story = {
  args: {
    isVisible: true,
    autoTimeout: true,
    showTimer: false,
    message: 'This is an error message.',
    type: 'error',
    timeout: 5000,
    location: 'top',
    color: '#d8292f',
    rounded: true,
  },
}

export const WarningNotification: Story = {
  args: {
    isVisible: true,
    autoTimeout: true,
    showTimer: false,
    message: 'This is a warning message.',
    type: 'warning',
    timeout: 5000,
    location: 'top',
    color: '#f9f1c6',
    rounded: true,
  },
}

export const SuccessNotification: Story = {
  args: {
    isVisible: true,
    autoTimeout: true,
    showTimer: false,
    message: 'This is a success message.',
    type: 'success',
    timeout: 5000,
    location: 'top',
    color: '#2e8540',
    rounded: true,
  },
}
