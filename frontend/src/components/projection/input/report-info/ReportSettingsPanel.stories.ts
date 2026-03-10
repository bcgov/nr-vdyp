import type { Meta, StoryObj } from '@storybook/vue3-vite'
import ReportSettingsPanel from './ReportSettingsPanel.vue'
import { useModelParameterStore } from '@/stores/projection/modelParameterStore'
import { useAppStore } from '@/stores/projection/appStore'
import { CONSTANTS } from '@/constants'
import { UtilizationClassSetEnum } from '@/services/vdyp-api/models/utilization-class-set-enum'

const meta: Meta<typeof ReportSettingsPanel> = {
  title: 'components/projection/input/ReportSettingsPanel',
  component: ReportSettingsPanel,
  tags: ['autodocs'],
}

export default meta
type Story = StoryObj<typeof ReportSettingsPanel>

const sampleSpeciesGroups = [
  { group: 'PL', percent: '30', siteSpecies: 'PL', minimumDBHLimit: UtilizationClassSetEnum._75 },
  { group: 'FD', percent: '25', siteSpecies: 'FD', minimumDBHLimit: UtilizationClassSetEnum._125 },
  { group: 'HW', percent: '15', siteSpecies: 'HW', minimumDBHLimit: UtilizationClassSetEnum._40 },
  { group: 'BL', percent: '15', siteSpecies: 'BL', minimumDBHLimit: UtilizationClassSetEnum._75 },
  { group: 'CW', percent: '10', siteSpecies: 'CW', minimumDBHLimit: UtilizationClassSetEnum._175 },
  { group: 'SX', percent: '5',  siteSpecies: 'SX', minimumDBHLimit: UtilizationClassSetEnum._225 },
]

const setSpeciesGroups = (modelStore: ReturnType<typeof useModelParameterStore>) => {
  modelStore.speciesGroups.splice(0, modelStore.speciesGroups.length, ...sampleSpeciesGroups)
}

export const Default: Story = {
  render: () => ({
    components: { ReportSettingsPanel },
    setup() {
      const modelStore = useModelParameterStore()
      const appStore = useAppStore()

      appStore.setViewMode('create')
      modelStore.panelOpenStates.reportSettings = CONSTANTS.PANEL.OPEN
      modelStore.panelState.reportSettings.confirmed = false
      modelStore.panelState.reportSettings.editable = true
      modelStore.selectedAgeYearRange = CONSTANTS.AGE_YEAR_RANGE.AGE
      modelStore.startingAge = '0'
      modelStore.finishingAge = '400'
      modelStore.ageIncrement = '5'
      modelStore.isComputedMAIEnabled = true
      modelStore.isCulminationValuesEnabled = true
      modelStore.isBySpeciesEnabled = true
      modelStore.incSecondaryHeight = false
      setSpeciesGroups(modelStore)
    },
    template: '<ReportSettingsPanel />',
  }),
}

export const Confirmed: Story = {
  render: () => ({
    components: { ReportSettingsPanel },
    setup() {
      const modelStore = useModelParameterStore()
      const appStore = useAppStore()

      appStore.setViewMode('create')
      modelStore.panelOpenStates.reportSettings = CONSTANTS.PANEL.OPEN
      modelStore.panelState.reportSettings.confirmed = true
      modelStore.panelState.reportSettings.editable = false
      modelStore.selectedAgeYearRange = CONSTANTS.AGE_YEAR_RANGE.AGE
      modelStore.startingAge = '0'
      modelStore.finishingAge = '400'
      modelStore.ageIncrement = '5'
      modelStore.isComputedMAIEnabled = true
      modelStore.isCulminationValuesEnabled = true
      modelStore.isBySpeciesEnabled = true
      modelStore.incSecondaryHeight = false
      setSpeciesGroups(modelStore)
    },
    template: '<ReportSettingsPanel />',
  }),
}

export const PanelCollapsed: Story = {
  render: () => ({
    components: { ReportSettingsPanel },
    setup() {
      const modelStore = useModelParameterStore()
      const appStore = useAppStore()

      appStore.setViewMode('create')
      modelStore.panelOpenStates.reportSettings = CONSTANTS.PANEL.CLOSE
      modelStore.panelState.reportSettings.confirmed = false
      modelStore.panelState.reportSettings.editable = true
    },
    template: '<ReportSettingsPanel />',
  }),
}
