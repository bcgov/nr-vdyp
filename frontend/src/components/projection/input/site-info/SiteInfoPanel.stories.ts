import type { Meta, StoryObj } from '@storybook/vue3-vite'
import SiteInfoPanel from './SiteInfoPanel.vue'
import { useModelParameterStore } from '@/stores/projection/modelParameterStore'
import { useAppStore } from '@/stores/projection/appStore'
import { CONSTANTS, DEFAULTS } from '@/constants'
import type { SiteIndexSpeciesRow } from '@/interfaces/interfaces'

const makeRow = (speciesCode: string, overrides: Partial<SiteIndexSpeciesRow> = {}): SiteIndexSpeciesRow => ({
  speciesCode,
  computedValue: CONSTANTS.COMPUTED_VALUE.BHA_SITE_INDEX,
  ageType: CONSTANTS.AGE_TYPE.TOTAL,
  age: DEFAULTS.DEFAULT_VALUES.SPZ_AGE,
  height: DEFAULTS.DEFAULT_VALUES.SPZ_HEIGHT,
  bhaSiteIndex: DEFAULTS.DEFAULT_VALUES.BHA50_SITE_INDEX,
  ...overrides,
})

const meta: Meta<typeof SiteInfoPanel> = {
  title: 'components/projection/input/site-info/SiteInfoPanel',
  component: SiteInfoPanel,
  tags: ['autodocs'],
}

export default meta
type Story = StoryObj<typeof SiteInfoPanel>

export const EditableComputed: Story = {
  render: () => ({
    components: { SiteInfoPanel },
    setup() {
      const modelStore = useModelParameterStore()
      const appStore = useAppStore()

      appStore.setViewMode('create')
      modelStore.panelOpenStates.siteInfo = CONSTANTS.PANEL.OPEN
      modelStore.panelState.siteInfo.editable = true
      modelStore.panelState.siteInfo.confirmed = false

      modelStore.becZone = 'CWH'
      modelStore.ecoZone = null
      modelStore.siteSpeciesValues = CONSTANTS.SITE_SPECIES_VALUES.COMPUTED
      modelStore.highestPercentSpecies = 'FD'
      modelStore.selectedSiteSpecies = 'FD'
      modelStore.siteIndexRows = [
        makeRow('FD', { bhaSiteIndex: null, computedValue: CONSTANTS.COMPUTED_VALUE.BHA_SITE_INDEX }),
        makeRow('PL'),
        makeRow('S'),
      ]
    },
    template: '<SiteInfoPanel />',
  }),
  parameters: {
    docs: {
      description: {
        story: 'Editable state with Site Index = Computed and three species rows. BHA Site Index is the computed field for the primary row (FD).',
      },
    },
  },
}

export const EditableSupplied: Story = {
  render: () => ({
    components: { SiteInfoPanel },
    setup() {
      const modelStore = useModelParameterStore()
      const appStore = useAppStore()

      appStore.setViewMode('create')
      modelStore.panelOpenStates.siteInfo = CONSTANTS.PANEL.OPEN
      modelStore.panelState.siteInfo.editable = true
      modelStore.panelState.siteInfo.confirmed = false

      modelStore.becZone = 'IDF'
      modelStore.ecoZone = null
      modelStore.siteSpeciesValues = CONSTANTS.SITE_SPECIES_VALUES.SUPPLIED
      modelStore.highestPercentSpecies = 'PL'
      modelStore.selectedSiteSpecies = 'PL'
      modelStore.siteIndexRows = [
        makeRow('PL', { age: null, height: null, bhaSiteIndex: '20.00' }),
        makeRow('FD', { age: null, height: null, bhaSiteIndex: '18.50' }),
      ]
    },
    template: '<SiteInfoPanel />',
  }),
  parameters: {
    docs: {
      description: {
        story: 'Editable state with Site Index = Supplied. Age and Height columns are N/A; BHA Site Index is entered directly.',
      },
    },
  },
}

export const Confirmed: Story = {
  render: () => ({
    components: { SiteInfoPanel },
    setup() {
      const modelStore = useModelParameterStore()
      const appStore = useAppStore()

      appStore.setViewMode('create')
      modelStore.panelOpenStates.siteInfo = CONSTANTS.PANEL.OPEN
      modelStore.panelState.siteInfo.confirmed = true
      modelStore.panelState.siteInfo.editable = false

      modelStore.becZone = 'CWH'
      modelStore.ecoZone = '7'
      modelStore.siteSpeciesValues = CONSTANTS.SITE_SPECIES_VALUES.COMPUTED
      modelStore.highestPercentSpecies = 'FD'
      modelStore.selectedSiteSpecies = 'FD'
      modelStore.siteIndexRows = [
        makeRow('FD', { bhaSiteIndex: null, computedValue: CONSTANTS.COMPUTED_VALUE.BHA_SITE_INDEX }),
        makeRow('PL'),
      ]
    },
    template: '<SiteInfoPanel />',
  }),
  parameters: {
    docs: {
      description: {
        story: 'Confirmed state - all inputs are disabled and the ActionPanel shows the Edit button.',
      },
    },
  },
}

export const ReadOnly: Story = {
  render: () => ({
    components: { SiteInfoPanel },
    setup() {
      const modelStore = useModelParameterStore()
      const appStore = useAppStore()

      appStore.setViewMode('view')
      modelStore.panelOpenStates.siteInfo = CONSTANTS.PANEL.OPEN
      modelStore.panelState.siteInfo.confirmed = true
      modelStore.panelState.siteInfo.editable = false

      modelStore.becZone = 'SBS'
      modelStore.ecoZone = '9'
      modelStore.siteSpeciesValues = CONSTANTS.SITE_SPECIES_VALUES.COMPUTED
      modelStore.highestPercentSpecies = 'S'
      modelStore.selectedSiteSpecies = 'S'
      modelStore.siteIndexRows = [makeRow('S'), makeRow('FD')]
    },
    template: '<SiteInfoPanel />',
  }),
  parameters: {
    docs: {
      description: {
        story: 'Read-only (view) mode - all inputs are disabled and the ActionPanel is hidden entirely.',
      },
    },
  },
}

export const PanelCollapsed: Story = {
  render: () => ({
    components: { SiteInfoPanel },
    setup() {
      const modelStore = useModelParameterStore()
      const appStore = useAppStore()

      appStore.setViewMode('create')
      modelStore.panelOpenStates.siteInfo = CONSTANTS.PANEL.CLOSE
      modelStore.panelState.siteInfo.editable = false
      modelStore.panelState.siteInfo.confirmed = false
    },
    template: '<SiteInfoPanel />',
  }),
  parameters: {
    docs: {
      description: {
        story: 'Panel in collapsed state. Only the "Site Information" header with the chevron icon is visible.',
      },
    },
  },
}
