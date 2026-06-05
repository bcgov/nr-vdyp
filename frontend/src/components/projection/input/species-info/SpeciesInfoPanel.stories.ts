import type { Meta, StoryObj } from '@storybook/vue3-vite'
import SpeciesInfoPanel from './SpeciesInfoPanel.vue'
import { useModelParameterStore } from '@/stores/projection/modelParameterStore'
import { useAppStore } from '@/stores/projection/appStore'
import { CONSTANTS } from '@/constants'

const meta: Meta<typeof SpeciesInfoPanel> = {
  title: 'components/projection/input/species-info/SpeciesInfoPanel',
  component: SpeciesInfoPanel,
  tags: ['autodocs'],
}

export default meta
type Story = StoryObj<typeof SpeciesInfoPanel>

export const Editable: Story = {
  render: () => ({
    components: { SpeciesInfoPanel },
    setup() {
      const modelStore = useModelParameterStore()
      const appStore = useAppStore()

      appStore.setViewMode('create')
      modelStore.panelOpenStates.speciesInfo = CONSTANTS.PANEL.OPEN
      modelStore.panelState.speciesInfo.editable = true
      modelStore.panelState.speciesInfo.confirmed = false
      modelStore.derivedBy = CONSTANTS.DERIVED_BY.BASAL_AREA
      modelStore.speciesList = []
    },
    template: '<SpeciesInfoPanel />',
  }),
}

export const EditableWithSpecies: Story = {
  render: () => ({
    components: { SpeciesInfoPanel },
    setup() {
      const modelStore = useModelParameterStore()
      const appStore = useAppStore()

      appStore.setViewMode('create')
      modelStore.panelOpenStates.speciesInfo = CONSTANTS.PANEL.OPEN
      modelStore.panelState.speciesInfo.editable = true
      modelStore.panelState.speciesInfo.confirmed = false
      modelStore.derivedBy = CONSTANTS.DERIVED_BY.BASAL_AREA
      modelStore.speciesList = [
        { species: 'FD', percent: '60.0' },
        { species: 'PL', percent: '40.0' },
      ]
    },
    template: '<SpeciesInfoPanel />',
  }),
}

export const AtSpeciesLimit: Story = {
  render: () => ({
    components: { SpeciesInfoPanel },
    setup() {
      const modelStore = useModelParameterStore()
      const appStore = useAppStore()

      appStore.setViewMode('create')
      modelStore.panelOpenStates.speciesInfo = CONSTANTS.PANEL.OPEN
      modelStore.panelState.speciesInfo.editable = true
      modelStore.panelState.speciesInfo.confirmed = false
      modelStore.derivedBy = CONSTANTS.DERIVED_BY.VOLUME
      modelStore.speciesList = [
        { species: 'FD', percent: '30.0' },
        { species: 'PL', percent: '20.0' },
        { species: 'HW', percent: '20.0' },
        { species: 'CW', percent: '15.0' },
        { species: 'BL', percent: '10.0' },
        { species: 'AT', percent: '5.0' },
      ]
    },
    template: '<SpeciesInfoPanel />',
  }),
}

export const Confirmed: Story = {
  render: () => ({
    components: { SpeciesInfoPanel },
    setup() {
      const modelStore = useModelParameterStore()
      const appStore = useAppStore()

      appStore.setViewMode('create')
      modelStore.panelOpenStates.speciesInfo = CONSTANTS.PANEL.OPEN
      modelStore.panelState.speciesInfo.confirmed = true
      modelStore.panelState.speciesInfo.editable = false
      modelStore.derivedBy = CONSTANTS.DERIVED_BY.BASAL_AREA
      modelStore.speciesList = [
        { species: 'FD', percent: '60.0' },
        { species: 'PL', percent: '40.0' },
      ]
    },
    template: '<SpeciesInfoPanel />',
  }),
}

export const ReadOnly: Story = {
  render: () => ({
    components: { SpeciesInfoPanel },
    setup() {
      const modelStore = useModelParameterStore()
      const appStore = useAppStore()

      appStore.setViewMode('view')
      modelStore.panelOpenStates.speciesInfo = CONSTANTS.PANEL.OPEN
      modelStore.panelState.speciesInfo.confirmed = true
      modelStore.panelState.speciesInfo.editable = false
      modelStore.derivedBy = CONSTANTS.DERIVED_BY.VOLUME
      modelStore.speciesList = [
        { species: 'FD', percent: '70.0' },
        { species: 'HW', percent: '30.0' },
      ]
    },
    template: '<SpeciesInfoPanel />',
  }),
}

export const PanelCollapsed: Story = {
  render: () => ({
    components: { SpeciesInfoPanel },
    setup() {
      const modelStore = useModelParameterStore()
      const appStore = useAppStore()

      appStore.setViewMode('create')
      modelStore.panelOpenStates.speciesInfo = CONSTANTS.PANEL.CLOSE
      modelStore.panelState.speciesInfo.editable = false
      modelStore.panelState.speciesInfo.confirmed = false
    },
    template: '<SpeciesInfoPanel />',
  }),
}
