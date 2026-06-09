import type { Meta, StoryObj } from '@storybook/vue3-vite'
import ReportDetailsPanel from './ReportDetailsPanel.vue'
import { useModelParameterStore } from '@/stores/projection/modelParameterStore'
import { useAppStore } from '@/stores/projection/appStore'
import { CONSTANTS } from '@/constants'

const meta: Meta<typeof ReportDetailsPanel> = {
  title: 'components/projection/input/ReportDetailsPanel',
  component: ReportDetailsPanel,
  tags: ['autodocs'],
}

export default meta
type Story = StoryObj<typeof ReportDetailsPanel>

export const Default: Story = {
  render: () => ({
    components: { ReportDetailsPanel },
    setup() {
      const modelStore = useModelParameterStore()
      const appStore = useAppStore()

      appStore.setViewMode('create')
      modelStore.panelOpenStates.reportDetails = CONSTANTS.PANEL.OPEN
      modelStore.panelState.reportDetails.confirmed = false
      modelStore.panelState.reportDetails.editable = true
      modelStore.reportTitle = 'Sample Forest Projection 2024'
      modelStore.projectionType = CONSTANTS.PROJECTION_TYPE.VOLUME
      modelStore.reportDescription = 'This projection covers the north-east sector stand inventory for the 2024 planning cycle.'
    },
    template: '<ReportDetailsPanel />',
  })
}

export const Confirmed: Story = {
  render: () => ({
    components: { ReportDetailsPanel },
    setup() {
      const modelStore = useModelParameterStore()
      const appStore = useAppStore()

      appStore.setViewMode('create')
      modelStore.panelOpenStates.reportDetails = CONSTANTS.PANEL.OPEN
      modelStore.panelState.reportDetails.confirmed = true
      modelStore.panelState.reportDetails.editable = false
      modelStore.reportTitle = 'Sample Forest Projection 2024'
      modelStore.projectionType = CONSTANTS.PROJECTION_TYPE.VOLUME
      modelStore.reportDescription = 'North-east sector stand inventory.'
    },
    template: '<ReportDetailsPanel />',
  })
}

export const ReadOnly: Story = {
  render: () => ({
    components: { ReportDetailsPanel },
    setup() {
      const modelStore = useModelParameterStore()
      const appStore = useAppStore()

      appStore.setViewMode('view')
      modelStore.panelOpenStates.reportDetails = CONSTANTS.PANEL.OPEN
      modelStore.panelState.reportDetails.confirmed = true
      modelStore.panelState.reportDetails.editable = false
      modelStore.reportTitle = 'Sample Forest Projection 2024'
      modelStore.projectionType = CONSTANTS.PROJECTION_TYPE.VOLUME
      modelStore.reportDescription = 'North-east sector stand inventory.'
    },
    template: '<ReportDetailsPanel />',
  })
}

export const PanelCollapsed: Story = {
  render: () => ({
    components: { ReportDetailsPanel },
    setup() {
      const modelStore = useModelParameterStore()
      const appStore = useAppStore()

      appStore.setViewMode('create')
      modelStore.panelOpenStates.reportDetails = CONSTANTS.PANEL.CLOSE
      modelStore.panelState.reportDetails.confirmed = false
      modelStore.panelState.reportDetails.editable = true
    },
    template: '<ReportDetailsPanel />',
  })
}

export const ProjectionRunning: Story = {
  render: () => ({
    components: { ReportDetailsPanel },
    setup() {
      const modelStore = useModelParameterStore()
      const appStore = useAppStore()

      appStore.setViewMode('create')
      appStore.currentProjectionStatus = CONSTANTS.PROJECTION_STATUS.RUNNING
      modelStore.panelOpenStates.reportDetails = CONSTANTS.PANEL.OPEN
      modelStore.panelState.reportDetails.confirmed = true
      modelStore.panelState.reportDetails.editable = false
      modelStore.reportTitle = 'Sample Forest Projection 2024'
      modelStore.projectionType = CONSTANTS.PROJECTION_TYPE.VOLUME
      modelStore.reportDescription = 'North-east sector stand inventory.'
    },
    template: '<ReportDetailsPanel />',
  })
}

export const TitleValidationError: Story = {
  render: () => ({
    components: { ReportDetailsPanel },
    setup() {
      const modelStore = useModelParameterStore()
      const appStore = useAppStore()

      appStore.setViewMode('create')
      modelStore.panelOpenStates.reportDetails = CONSTANTS.PANEL.OPEN
      modelStore.panelState.reportDetails.confirmed = false
      modelStore.panelState.reportDetails.editable = true
      modelStore.reportTitle = null
      modelStore.projectionType = CONSTANTS.PROJECTION_TYPE.VOLUME
      modelStore.reportDescription = ''
    },
    template: '<ReportDetailsPanel />',
  })
}
