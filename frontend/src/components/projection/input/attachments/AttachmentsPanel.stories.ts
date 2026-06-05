import type { Meta, StoryObj } from '@storybook/vue3-vite'
import AttachmentsPanel from './AttachmentsPanel.vue'
import { useFileUploadStore } from '@/stores/projection/fileUploadStore'
import { useAppStore } from '@/stores/projection/appStore'
import { CONSTANTS } from '@/constants'

const meta: Meta<typeof AttachmentsPanel> = {
  title: 'components/projection/input/AttachmentsPanel',
  component: AttachmentsPanel,
  tags: ['autodocs'],
}

export default meta
type Story = StoryObj<typeof AttachmentsPanel>

export const Default: Story = {
  render: () => ({
    components: { AttachmentsPanel },
    setup() {
      const fileUploadStore = useFileUploadStore()
      const appStore = useAppStore()

      appStore.setViewMode('create')
      fileUploadStore.panelOpenStates.attachments = CONSTANTS.PANEL.OPEN
      fileUploadStore.setPolygonFileInfo(null)
      fileUploadStore.setLayerFileInfo(null)
      fileUploadStore.isUploadingPolygon = false
      fileUploadStore.isUploadingLayer = false
    },
    template: '<AttachmentsPanel />',
  }),
}

export const WithFilesUploaded: Story = {
  render: () => ({
    components: { AttachmentsPanel },
    setup() {
      const fileUploadStore = useFileUploadStore()
      const appStore = useAppStore()

      appStore.setViewMode('create')
      fileUploadStore.panelOpenStates.attachments = CONSTANTS.PANEL.OPEN
      fileUploadStore.setPolygonFileInfo({
        filename: 'polygon_data.csv',
        fileMappingGUID: 'polygon-file-guid-001',
        fileSetGUID: 'polygon-set-guid-001',
      })
      fileUploadStore.setLayerFileInfo({
        filename: 'layer_data.csv',
        fileMappingGUID: 'layer-file-guid-001',
        fileSetGUID: 'layer-set-guid-001',
      })
      fileUploadStore.isUploadingPolygon = false
      fileUploadStore.isUploadingLayer = false
    },
    template: '<AttachmentsPanel />',
  }),
}

export const PolygonFileOnly: Story = {
  render: () => ({
    components: { AttachmentsPanel },
    setup() {
      const fileUploadStore = useFileUploadStore()
      const appStore = useAppStore()

      appStore.setViewMode('create')
      fileUploadStore.panelOpenStates.attachments = CONSTANTS.PANEL.OPEN
      fileUploadStore.setPolygonFileInfo({
        filename: 'polygon_data.csv',
        fileMappingGUID: 'polygon-file-guid-001',
        fileSetGUID: 'polygon-set-guid-001',
      })
      fileUploadStore.setLayerFileInfo(null)
      fileUploadStore.isUploadingPolygon = false
      fileUploadStore.isUploadingLayer = false
    },
    template: '<AttachmentsPanel />',
  }),
}

export const ReadOnlyWithFiles: Story = {
  render: () => ({
    components: { AttachmentsPanel },
    setup() {
      const fileUploadStore = useFileUploadStore()
      const appStore = useAppStore()

      appStore.setViewMode('view')
      fileUploadStore.panelOpenStates.attachments = CONSTANTS.PANEL.OPEN
      fileUploadStore.setPolygonFileInfo({
        filename: 'polygon_data.csv',
        fileMappingGUID: 'polygon-file-guid-001',
        fileSetGUID: 'polygon-set-guid-001',
      })
      fileUploadStore.setLayerFileInfo({
        filename: 'layer_data.csv',
        fileMappingGUID: 'layer-file-guid-001',
        fileSetGUID: 'layer-set-guid-001',
      })
    },
    template: '<AttachmentsPanel />',
  }),
}

export const PanelCollapsed: Story = {
  render: () => ({
    components: { AttachmentsPanel },
    setup() {
      const fileUploadStore = useFileUploadStore()
      const appStore = useAppStore()

      appStore.setViewMode('create')
      fileUploadStore.panelOpenStates.attachments = CONSTANTS.PANEL.CLOSE
      fileUploadStore.setPolygonFileInfo(null)
      fileUploadStore.setLayerFileInfo(null)
    },
    template: '<AttachmentsPanel />',
  }),
}
