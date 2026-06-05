import type { Meta, StoryObj } from '@storybook/vue3-vite'
import MinimumDBHPanel from './MinimumDBHPanel.vue'
import { useFileUploadStore } from '@/stores/projection/fileUploadStore'
import { useAppStore } from '@/stores/projection/appStore'
import { CONSTANTS } from '@/constants'

const meta: Meta<typeof MinimumDBHPanel> = {
  title: 'components/projection/input/MinimumDBHPanel',
  component: MinimumDBHPanel,
  tags: ['autodocs'],
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
}
