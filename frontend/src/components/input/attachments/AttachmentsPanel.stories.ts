import type { Meta, StoryObj } from '@storybook/vue3'
import { createPinia, setActivePinia } from 'pinia'
import { useFileUploadStore } from '@/stores/fileUploadStore'
import AttachmentsPanel from './AttachmentsPanel.vue'
import { CONSTANTS } from '@/constants'

const pinia = createPinia()
setActivePinia(pinia)

const meta: Meta<typeof AttachmentsPanel> = {
  title: 'components/input/attachments/AttachmentsPanel',
  component: AttachmentsPanel,
  decorators: [
    (story) => {
      const fileUploadStore = useFileUploadStore()
      fileUploadStore.setDefaultValues()
      fileUploadStore.panelOpenStates[CONSTANTS.FILE_UPLOAD_PANEL.ATTACHMENTS] =
        CONSTANTS.PANEL.OPEN
      fileUploadStore.panelState[CONSTANTS.FILE_UPLOAD_PANEL.ATTACHMENTS] = {
        confirmed: false,
        editable: true,
      }
      return {
        components: { story },
        template: `<div><story /></div>`,
      }
    },
  ],
  tags: ['autodocs'],
}

export default meta

type Story = StoryObj<typeof AttachmentsPanel>

export const Default: Story = {
  render: () => {
    const fileUploadStore = useFileUploadStore()
    fileUploadStore.editPanel(CONSTANTS.FILE_UPLOAD_PANEL.ATTACHMENTS)
    return {
      components: { AttachmentsPanel },
      template: '<AttachmentsPanel />',
    }
  },
}

export const Confirmed: Story = {
  render: () => {
    const fileUploadStore = useFileUploadStore()
    fileUploadStore.confirmPanel(CONSTANTS.FILE_UPLOAD_PANEL.ATTACHMENTS)
    return {
      components: { AttachmentsPanel },
      template: '<AttachmentsPanel />',
    }
  },
}
