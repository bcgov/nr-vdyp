import type { Meta, StoryObj } from '@storybook/vue3-vite'
import ReportConfigPanel from './ReportConfigPanel.vue'
import { useFileUploadStore } from '@/stores/projection/fileUploadStore'
import { useAppStore } from '@/stores/projection/appStore'
import { CONSTANTS, DEFAULTS } from '@/constants'

const meta: Meta<typeof ReportConfigPanel> = {
  title: 'components/projection/input/ReportConfigPanel',
  component: ReportConfigPanel,
  tags: ['autodocs'],
  }

export default meta
type Story = StoryObj<typeof ReportConfigPanel>

export const Default: Story = {
  render: () => ({
    components: { ReportConfigPanel },
    setup() {
      const fileUploadStore = useFileUploadStore()
      const appStore = useAppStore()

      appStore.setViewMode('create')
      fileUploadStore.panelOpenStates.reportConfig = CONSTANTS.PANEL.OPEN
      fileUploadStore.panelState.reportConfig.confirmed = false
      fileUploadStore.panelState.reportConfig.editable = true
      fileUploadStore.reportTitle = 'Sample Forest Projection 2024'
      fileUploadStore.projectionType = CONSTANTS.PROJECTION_TYPE.VOLUME
      fileUploadStore.reportDescription = 'This projection covers the north-east sector stand inventory for the 2024 planning cycle.'
      fileUploadStore.selectedAgeYearRange = DEFAULTS.DEFAULT_VALUES.SELECTED_AGE_YEAR_RANGE
      fileUploadStore.startingAge = '0'
      fileUploadStore.finishingAge = '250'
      fileUploadStore.ageIncrement = '5'
    },
    template: '<ReportConfigPanel />',
  }),
  parameters: {
    docs: {
      description: {
        story: 'Edit mode - panel open with title, description, and age range fields filled in. All inputs are enabled.',
      },
    },
  },
}

export const YearRangeMode: Story = {
  render: () => ({
    components: { ReportConfigPanel },
    setup() {
      const fileUploadStore = useFileUploadStore()
      const appStore = useAppStore()

      appStore.setViewMode('create')
      fileUploadStore.panelOpenStates.reportConfig = CONSTANTS.PANEL.OPEN
      fileUploadStore.panelState.reportConfig.confirmed = false
      fileUploadStore.panelState.reportConfig.editable = true
      fileUploadStore.reportTitle = 'Year Range Projection'
      fileUploadStore.projectionType = CONSTANTS.PROJECTION_TYPE.VOLUME
      fileUploadStore.selectedAgeYearRange = CONSTANTS.AGE_YEAR_RANGE.YEAR
      fileUploadStore.startYear = '2000'
      fileUploadStore.endYear = '2050'
      fileUploadStore.yearIncrement = '5'
    },
    template: '<ReportConfigPanel />',
  }),
  parameters: {
    docs: {
      description: {
        story: 'Numeric Range set to "Year" mode - Starting Year, Finishing Year, and Increment fields are shown instead of Age fields.',
      },
    },
  },
}

export const CFSBiomassProjection: Story = {
  render: () => ({
    components: { ReportConfigPanel },
    setup() {
      const fileUploadStore = useFileUploadStore()
      const appStore = useAppStore()

      appStore.setViewMode('create')
      fileUploadStore.panelOpenStates.reportConfig = CONSTANTS.PANEL.OPEN
      fileUploadStore.panelState.reportConfig.confirmed = false
      fileUploadStore.panelState.reportConfig.editable = true
      fileUploadStore.reportTitle = 'Biomass Projection Run'
      fileUploadStore.projectionType = CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS
      fileUploadStore.selectedAgeYearRange = DEFAULTS.DEFAULT_VALUES.SELECTED_AGE_YEAR_RANGE
      fileUploadStore.startingAge = '0'
      fileUploadStore.finishingAge = '250'
      fileUploadStore.ageIncrement = '5'
      fileUploadStore.isBySpeciesEnabled = false
    },
    template: '<ReportConfigPanel />',
  }),
  parameters: {
    docs: {
      description: {
        story: 'CFS Biomass projection type selected - the "By Species" checkbox is automatically disabled.',
      },
    },
  },
}

export const Confirmed: Story = {
  render: () => ({
    components: { ReportConfigPanel },
    setup() {
      const fileUploadStore = useFileUploadStore()
      const appStore = useAppStore()

      appStore.setViewMode('create')
      fileUploadStore.panelOpenStates.reportConfig = CONSTANTS.PANEL.OPEN
      fileUploadStore.panelState.reportConfig.confirmed = true
      fileUploadStore.panelState.reportConfig.editable = false
      fileUploadStore.reportTitle = 'Sample Forest Projection 2024'
      fileUploadStore.projectionType = CONSTANTS.PROJECTION_TYPE.VOLUME
      fileUploadStore.reportDescription = 'North-east sector stand inventory.'
      fileUploadStore.selectedAgeYearRange = DEFAULTS.DEFAULT_VALUES.SELECTED_AGE_YEAR_RANGE
      fileUploadStore.startingAge = '0'
      fileUploadStore.finishingAge = '250'
      fileUploadStore.ageIncrement = '5'
    },
    template: '<ReportConfigPanel />',
  }),
  parameters: {
    docs: {
      description: {
        story: 'Confirmed state - all fields are disabled and the Edit button in the header is enabled.',
      },
    },
  },
}

export const ReadOnly: Story = {
  render: () => ({
    components: { ReportConfigPanel },
    setup() {
      const fileUploadStore = useFileUploadStore()
      const appStore = useAppStore()

      appStore.setViewMode('view')
      fileUploadStore.panelOpenStates.reportConfig = CONSTANTS.PANEL.OPEN
      fileUploadStore.panelState.reportConfig.confirmed = true
      fileUploadStore.panelState.reportConfig.editable = false
      fileUploadStore.reportTitle = 'Sample Forest Projection 2024'
      fileUploadStore.projectionType = CONSTANTS.PROJECTION_TYPE.VOLUME
      fileUploadStore.reportDescription = 'North-east sector stand inventory.'
      fileUploadStore.selectedAgeYearRange = DEFAULTS.DEFAULT_VALUES.SELECTED_AGE_YEAR_RANGE
      fileUploadStore.startingAge = '0'
      fileUploadStore.finishingAge = '250'
      fileUploadStore.ageIncrement = '5'
    },
    template: '<ReportConfigPanel />',
  }),
  parameters: {
    docs: {
      description: {
        story: 'Read-only (View) mode - all fields are disabled and the ActionPanel and Edit button are hidden.',
      },
    },
  },
}

export const PanelCollapsed: Story = {
  render: () => ({
    components: { ReportConfigPanel },
    setup() {
      const fileUploadStore = useFileUploadStore()
      const appStore = useAppStore()

      appStore.setViewMode('create')
      fileUploadStore.panelOpenStates.reportConfig = CONSTANTS.PANEL.CLOSE
      fileUploadStore.panelState.reportConfig.confirmed = false
      fileUploadStore.panelState.reportConfig.editable = true
    },
    template: '<ReportConfigPanel />',
  }),
  parameters: {
    docs: {
      description: {
        story: 'Panel in collapsed state - only the "Report Details" header with the chevron icon is visible.',
      },
    },
  },
}
