import type { Meta, StoryObj } from '@storybook/vue3-vite'
import { createPinia, setActivePinia } from 'pinia'
import AppAlertDialog from './AppAlertDialog.vue'
import { useAlertDialogStore } from '@/stores/common/alertDialogStore'

const pinia = createPinia()
setActivePinia(pinia)

const meta: Meta<typeof AppAlertDialog> = {
  title: 'components/common/AppAlertDialog',
  component: AppAlertDialog,
  decorators: [
    (story) => ({
      components: { story },
      template: `<div><story /></div>`,
    }),
  ],
  tags: ['autodocs'],
  argTypes: {},
}

export default meta
type Story = StoryObj<typeof AppAlertDialog>

export const InfoAlert: Story = {
  render: () => ({
    components: { AppAlertDialog },
    setup() {
      const alertDialogStore = useAlertDialogStore()
      alertDialogStore.openDialog(
        'Information',
        'This is an informational message to provide additional context.',
        { width: 450, variant: 'info' },
      )
      return { alertDialogStore }
    },
    template: '<AppAlertDialog />',
  }),
}

export const ConfirmationAlert: Story = {
  render: () => ({
    components: { AppAlertDialog },
    setup() {
      const alertDialogStore = useAlertDialogStore()
      alertDialogStore.openDialog(
        'Confirmation',
        'Are you sure you want to proceed with this action?',
        { width: 450, variant: 'confirmation' },
      )
      return { alertDialogStore }
    },
    template: '<AppAlertDialog />',
  }),
}

export const WarningAlert: Story = {
  render: () => ({
    components: { AppAlertDialog },
    setup() {
      const alertDialogStore = useAlertDialogStore()
      alertDialogStore.openDialog(
        'Warning',
        'This action may have unintended consequences. Please review before proceeding.',
        { width: 450, variant: 'warning' },
      )
      return { alertDialogStore }
    },
    template: '<AppAlertDialog />',
  }),
}

export const ErrorAlert: Story = {
  render: () => ({
    components: { AppAlertDialog },
    setup() {
      const alertDialogStore = useAlertDialogStore()
      alertDialogStore.openDialog(
        'Error',
        'An error has occurred. Would you like to retry?',
        { width: 450, variant: 'error' },
      )
      return { alertDialogStore }
    },
    template: '<AppAlertDialog />',
  }),
}

export const DestructiveAlert: Story = {
  render: () => ({
    components: { AppAlertDialog },
    setup() {
      const alertDialogStore = useAlertDialogStore()
      alertDialogStore.openDialog(
        'Delete Item',
        'This action cannot be undone. Are you sure you want to permanently delete this item?',
        { width: 450, variant: 'destructive' },
      )
      return { alertDialogStore }
    },
    template: '<AppAlertDialog />',
  }),
}
