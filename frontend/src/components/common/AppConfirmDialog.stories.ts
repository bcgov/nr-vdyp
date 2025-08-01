import type { Meta, StoryObj } from '@storybook/vue3-vite'
import { createPinia, setActivePinia } from 'pinia'
import AppConfirmDialog from './AppConfirmDialog.vue'
import { useConfirmDialogStore } from '@/stores/common/confirmDialogStore'

const pinia = createPinia()
setActivePinia(pinia)

const meta: Meta<typeof AppConfirmDialog> = {
  title: 'components/common/AppConfirmDialog',
  component: AppConfirmDialog,
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
type Story = StoryObj<typeof AppConfirmDialog>

export const DefaultConfirm: Story = {
  render: () => ({
    components: { AppConfirmDialog },
    setup() {
      const confirmDialogStore = useConfirmDialogStore()
      confirmDialogStore.openDialog(
        'Confirm Action',
        'Are you sure you want to proceed?',
        { width: 400 },
      )
      return { confirmDialogStore }
    },
    template: '<AppConfirmDialog />',
  }),
}

export const CustomLabels: Story = {
  render: () => ({
    components: { AppConfirmDialog },
    setup() {
      const confirmDialogStore = useConfirmDialogStore()
      confirmDialogStore.openDialog(
        'Custom Confirmation',
        'Please confirm your choice.',
        { width: 450 },
      )
      return { confirmDialogStore }
    },
    template: '<AppConfirmDialog />',
  }),
}

export const WideDialog: Story = {
  render: () => ({
    components: { AppConfirmDialog },
    setup() {
      const confirmDialogStore = useConfirmDialogStore()
      confirmDialogStore.openDialog(
        'Wide Confirmation',
        'This is a wider dialog with a longer message to test layout.',
        { width: 600 },
      )
      return { confirmDialogStore }
    },
    template: '<AppConfirmDialog />',
  }),
}
