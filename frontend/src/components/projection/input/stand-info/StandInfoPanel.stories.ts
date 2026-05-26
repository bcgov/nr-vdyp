import type { Meta, StoryObj } from '@storybook/vue3-vite'
import StandInfoPanel from './StandInfoPanel.vue'
import { useModelParameterStore } from '@/stores/projection/modelParameterStore'
import { useAppStore } from '@/stores/projection/appStore'
import { CONSTANTS, DEFAULTS } from '@/constants'

const meta: Meta<typeof StandInfoPanel> = {
  title: 'components/projection/input/StandInfoPanel',
  component: StandInfoPanel,
  tags: ['autodocs'],
}

export default meta
type Story = StoryObj<typeof StandInfoPanel>

export const EditableBasalArea: Story = {
  render: () => ({
    components: { StandInfoPanel },
    setup() {
      const modelStore = useModelParameterStore()
      const appStore = useAppStore()

      appStore.setViewMode('create')
      modelStore.panelOpenStates.standInfo = CONSTANTS.PANEL.OPEN
      modelStore.panelState.standInfo.editable = true
      modelStore.panelState.standInfo.confirmed = false

      modelStore.derivedBy = CONSTANTS.DERIVED_BY.BASAL_AREA
      modelStore.siteSpeciesValues = CONSTANTS.SITE_SPECIES_VALUES.COMPUTED
      modelStore.percentStockableArea = DEFAULTS.DEFAULT_VALUES.PERCENT_STOCKABLE_AREA
      modelStore.basalArea = DEFAULTS.DEFAULT_VALUES.BASAL_AREA
      modelStore.treesPerHectare = DEFAULTS.DEFAULT_VALUES.TPH
      modelStore.crownClosure = null
    },
    template: '<StandInfoPanel />',
  })
}

export const EditableVolume: Story = {
  render: () => ({
    components: { StandInfoPanel },
    setup() {
      const modelStore = useModelParameterStore()
      const appStore = useAppStore()

      appStore.setViewMode('create')
      modelStore.panelOpenStates.standInfo = CONSTANTS.PANEL.OPEN
      modelStore.panelState.standInfo.editable = true
      modelStore.panelState.standInfo.confirmed = false

      modelStore.derivedBy = CONSTANTS.DERIVED_BY.VOLUME
      modelStore.siteSpeciesValues = CONSTANTS.SITE_SPECIES_VALUES.COMPUTED
      modelStore.percentStockableArea = DEFAULTS.DEFAULT_VALUES.PERCENT_STOCKABLE_AREA
      modelStore.basalArea = null
      modelStore.treesPerHectare = null
      modelStore.crownClosure = DEFAULTS.DEFAULT_VALUES.CROWN_CLOSURE
    },
    template: '<StandInfoPanel />',
  })
}

export const Confirmed: Story = {
  render: () => ({
    components: { StandInfoPanel },
    setup() {
      const modelStore = useModelParameterStore()
      const appStore = useAppStore()

      appStore.setViewMode('create')
      modelStore.panelOpenStates.standInfo = CONSTANTS.PANEL.OPEN
      modelStore.panelState.standInfo.confirmed = true
      modelStore.panelState.standInfo.editable = false

      modelStore.derivedBy = CONSTANTS.DERIVED_BY.BASAL_AREA
      modelStore.siteSpeciesValues = CONSTANTS.SITE_SPECIES_VALUES.COMPUTED
      modelStore.percentStockableArea = '75'
      modelStore.basalArea = '25.5000'
      modelStore.treesPerHectare = '800.00'
      modelStore.crownClosure = null
    },
    template: '<StandInfoPanel />',
  })
}

export const ReadOnly: Story = {
  render: () => ({
    components: { StandInfoPanel },
    setup() {
      const modelStore = useModelParameterStore()
      const appStore = useAppStore()

      appStore.setViewMode('view')
      modelStore.panelOpenStates.standInfo = CONSTANTS.PANEL.OPEN
      modelStore.panelState.standInfo.confirmed = true
      modelStore.panelState.standInfo.editable = false

      modelStore.derivedBy = CONSTANTS.DERIVED_BY.BASAL_AREA
      modelStore.siteSpeciesValues = CONSTANTS.SITE_SPECIES_VALUES.COMPUTED
      modelStore.percentStockableArea = '60'
      modelStore.basalArea = '15.0000'
      modelStore.treesPerHectare = '500.00'
      modelStore.crownClosure = null
    },
    template: '<StandInfoPanel />',
  })
}

export const PanelCollapsed: Story = {
  render: () => ({
    components: { StandInfoPanel },
    setup() {
      const modelStore = useModelParameterStore()
      const appStore = useAppStore()

      appStore.setViewMode('create')
      modelStore.panelOpenStates.standInfo = CONSTANTS.PANEL.CLOSE
      modelStore.panelState.standInfo.editable = false
      modelStore.panelState.standInfo.confirmed = false
    },
    template: '<StandInfoPanel />',
  })
}
