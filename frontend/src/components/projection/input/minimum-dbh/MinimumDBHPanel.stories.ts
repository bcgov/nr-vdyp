import type { Meta, StoryObj } from '@storybook/vue3-vite'
import MinimumDBHPanel from './MinimumDBHPanel.vue'
import { useFileUploadStore } from '@/stores/projection/fileUploadStore'
import { useAppStore } from '@/stores/projection/appStore'
import { CONSTANTS } from '@/constants'

const meta: Meta<typeof MinimumDBHPanel> = {
  title: 'components/projection/input/MinimumDBHPanel',
  component: MinimumDBHPanel,
  tags: ['autodocs'],
  parameters: {
    docs: {
      description: {
        component: `
Panel for configuring Minimum DBH (Diameter at Breast Height) limits per species group.

**Features:**
- Slider per species group (16 total) mapped to utilization class enum values
- Sliders are disabled when the panel is not editable or projection type is CFS Biomass
- Confirm / Cancel action buttons while in edit mode
- Edit button in the panel header after confirmation (hidden in read-only mode)
- Responsive layout: labels and slider ticks adjust on mobile viewports

**State management:**
- Species groups and slider values are driven by \`fileUploadStore.fileUploadSpeciesGroup\`
- Panel open/confirmed/editable state is managed via \`fileUploadStore\`
- Read-only mode is determined by \`appStore.isReadOnly\`
        `,
      },
    },
  },
}

export default meta
type Story = StoryObj<typeof MinimumDBHPanel>

export const Default: Story = {
  render: () => ({
    components: { MinimumDBHPanel },
    setup() {
      const fileUploadStore = useFileUploadStore()
      const appStore = useAppStore()

      appStore.setViewMode('create')
      appStore.setCurrentProjectionStatus('Draft')
      fileUploadStore.projectionType = CONSTANTS.PROJECTION_TYPE.VOLUME
      fileUploadStore.initializeSpeciesGroups()
      fileUploadStore.panelOpenStates.minimumDBH = CONSTANTS.PANEL.OPEN
      fileUploadStore.panelState.minimumDBH.confirmed = false
      fileUploadStore.panelState.minimumDBH.editable = true
    },
    template: '<MinimumDBHPanel />',
  }),
  parameters: {
    docs: {
      description: {
        story: 'Edit mode - panel open and editable with default Volume projection utilization values. Confirm and Cancel buttons are shown at the bottom.',
      },
    },
  },
}

export const Confirmed: Story = {
  render: () => ({
    components: { MinimumDBHPanel },
    setup() {
      const fileUploadStore = useFileUploadStore()
      const appStore = useAppStore()

      appStore.setViewMode('create')
      appStore.setCurrentProjectionStatus('Draft')
      fileUploadStore.projectionType = CONSTANTS.PROJECTION_TYPE.VOLUME
      fileUploadStore.initializeSpeciesGroups()
      fileUploadStore.panelOpenStates.minimumDBH = CONSTANTS.PANEL.OPEN
      fileUploadStore.panelState.minimumDBH.confirmed = true
      fileUploadStore.panelState.minimumDBH.editable = false
    },
    template: '<MinimumDBHPanel />',
  }),
  parameters: {
    docs: {
      description: {
        story: 'Edit mode - panel confirmed. Sliders are disabled and the Edit button is active in the header.',
      },
    },
  },
}

export const CFSBiomass: Story = {
  render: () => ({
    components: { MinimumDBHPanel },
    setup() {
      const fileUploadStore = useFileUploadStore()
      const appStore = useAppStore()

      appStore.setViewMode('create')
      appStore.setCurrentProjectionStatus('Draft')
      fileUploadStore.projectionType = CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS
      fileUploadStore.initializeSpeciesGroups()
      fileUploadStore.updateSpeciesGroupsForProjectionType(CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS)
      fileUploadStore.panelOpenStates.minimumDBH = CONSTANTS.PANEL.OPEN
      fileUploadStore.panelState.minimumDBH.confirmed = false
      fileUploadStore.panelState.minimumDBH.editable = true
    },
    template: '<MinimumDBHPanel />',
  }),
  parameters: {
    docs: {
      description: {
        story: 'CFS Biomass projection type - all sliders are disabled regardless of panel editable state, reflecting fixed utilization values.',
      },
    },
  },
}

export const PanelCollapsed: Story = {
  render: () => ({
    components: { MinimumDBHPanel },
    setup() {
      const fileUploadStore = useFileUploadStore()
      const appStore = useAppStore()

      appStore.setViewMode('create')
      appStore.setCurrentProjectionStatus('Draft')
      fileUploadStore.projectionType = CONSTANTS.PROJECTION_TYPE.VOLUME
      fileUploadStore.initializeSpeciesGroups()
      fileUploadStore.panelOpenStates.minimumDBH = CONSTANTS.PANEL.CLOSE
      fileUploadStore.panelState.minimumDBH.confirmed = false
      fileUploadStore.panelState.minimumDBH.editable = false
    },
    template: '<MinimumDBHPanel />',
  }),
  parameters: {
    docs: {
      description: {
        story: 'Panel in collapsed state. Only the header is visible; the species group sliders are hidden.',
      },
    },
  },
}
