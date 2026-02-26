import type { Meta, StoryObj } from '@storybook/vue3-vite'
import AttachmentsPanel from './AttachmentsPanel.vue'
import { useFileUploadStore } from '@/stores/projection/fileUploadStore'
import { useAppStore } from '@/stores/projection/appStore'
import { CONSTANTS } from '@/constants'

const meta: Meta<typeof AttachmentsPanel> = {
  title: 'components/projection/input/AttachmentsPanel',
  component: AttachmentsPanel,
  tags: ['autodocs'],
  parameters: {
    docs: {
      description: {
        component: `
Panel component for uploading Polygon and Layer CSV files.

**Features:**
- Separate upload slots for Polygon and Layer files
- Files are uploaded to the server immediately upon selection
- Uploaded files can be deleted (with a confirmation dialog)
- Supports Edit and Read-only (View) modes
- Validates file headers and checks for duplicate columns

**State management:**
- File info and panel open state are managed via \`fileUploadStore\`
- Read-only mode is determined by \`appStore.isReadOnly\`
        `,
      },
    },
  },
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
  parameters: {
    docs: {
      description: {
        story: 'Edit mode - panel open with no files uploaded yet.',
      },
    },
  },
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
  parameters: {
    docs: {
      description: {
        story: 'Edit mode - both Polygon and Layer files uploaded. Delete buttons are shown for each file.',
      },
    },
  },
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
  parameters: {
    docs: {
      description: {
        story: 'Edit mode - only the Polygon file has been uploaded; Layer file is still pending.',
      },
    },
  },
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
  parameters: {
    docs: {
      description: {
        story: 'Read-only (View) mode - both files uploaded. File names are displayed instead of the upload UI.',
      },
    },
  },
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
  parameters: {
    docs: {
      description: {
        story: 'Panel in collapsed state. Only the header is visible; the file upload content is hidden.',
      },
    },
  },
}
