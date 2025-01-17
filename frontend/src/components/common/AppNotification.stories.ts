import type { Meta, StoryObj } from '@storybook/vue3'
import { createPinia, setActivePinia } from 'pinia'
import AppNotification from '.AppNotification.vue'
import { useNotificationStore } from '@/stores/common/notificationStore'

const pinia = createPinia()
setActivePinia(pinia)

const meta: Meta<typeof AppNotification> = {
  title: 'components/common/AppNotification',
  component: AppNotification,
  decorators: [
    (story) => {
      const notificationStore = useNotificationStore()

      notificationStore.showInfoMessage('This is an informational message.')
      return {
        components: { story },
        template: `<div><story /></div>`,
      }
    },
  ],
  tags: ['autodocs'],
}

export default meta

type Story = StoryObj<typeof AppNotification>

export const InfoNotification: Story = {
  render: () => ({
    components: { AppNotification },
    template: '<AppNotification />',
  }),
}

export const ErrorNotification: Story = {
  render: () => {
    const notificationStore = useNotificationStore()
    notificationStore.showErrorMessage('This is an error message.')
    return {
      components: { AppNotification },
      template: '<AppNotification />',
    }
  },
}

export const WarningNotification: Story = {
  render: () => {
    const notificationStore = useNotificationStore()
    notificationStore.showWarningMessage('This is a warning message.')
    return {
      components: { AppNotification },
      template: '<AppNotification />',
    }
  },
}

export const SuccessNotification: Story = {
  render: () => {
    const notificationStore = useNotificationStore()
    notificationStore.showSuccessMessage('This is a success message.')
    return {
      components: { AppNotification },
      template: '<AppNotification />',
    }
  },
}
