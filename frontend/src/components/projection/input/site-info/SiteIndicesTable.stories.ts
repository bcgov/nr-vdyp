import type { Meta, StoryObj } from '@storybook/vue3-vite'
import SiteIndicesTable from './SiteIndicesTable.vue'
import { useModelParameterStore } from '@/stores/projection/modelParameterStore'
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

const meta: Meta<typeof SiteIndicesTable> = {
  title: 'components/projection/input/site-info/SiteIndicesTable',
  component: SiteIndicesTable,
  tags: ['autodocs'],
}

export default meta
type Story = StoryObj<typeof SiteIndicesTable>

export const Editable: Story = {
  render: () => ({
    components: { SiteIndicesTable },
    setup() {
      const store = useModelParameterStore()
      store.siteSpeciesValues = CONSTANTS.SITE_SPECIES_VALUES.COMPUTED
      store.derivedBy = CONSTANTS.DERIVED_BY.BASAL_AREA
      store.siteIndexRows = [
        makeRow('FD', { bhaSiteIndex: null, computedValue: CONSTANTS.COMPUTED_VALUE.BHA_SITE_INDEX }),
        makeRow('PL'),
        makeRow('S'),
      ]
    },
    template: '<SiteIndicesTable :isConfirmEnabled="true" />',
  }),
  parameters: {
    docs: {
      description: {
        story: 'Computed + Basal Area: all rows editable. Primary row has BHA Site Index computed (Calc. placeholder). Confirm is enabled.',
      },
    },
  },
}

export const Disabled: Story = {
  render: () => ({
    components: { SiteIndicesTable },
    setup() {
      const store = useModelParameterStore()
      store.siteSpeciesValues = CONSTANTS.SITE_SPECIES_VALUES.COMPUTED
      store.siteIndexRows = [makeRow('FD'), makeRow('PL'), makeRow('S')]
    },
    template: '<SiteIndicesTable :isConfirmEnabled="false" />',
  }),
  parameters: {
    docs: {
      description: {
        story: 'isConfirmEnabled = false: all inputs are disabled regardless of other state.',
      },
    },
  },
}

export const Supplied: Story = {
  render: () => ({
    components: { SiteIndicesTable },
    setup() {
      const store = useModelParameterStore()
      store.siteSpeciesValues = CONSTANTS.SITE_SPECIES_VALUES.SUPPLIED
      store.siteIndexRows = [
        makeRow('FD', { age: null, height: null }),
        makeRow('PL', { age: null, height: null }),
      ]
    },
    template: '<SiteIndicesTable :isConfirmEnabled="true" />',
  }),
  parameters: {
    docs: {
      description: {
        story: 'siteSpeciesValues = Supplied: Computed Value select shows N/A placeholder; Age and Height spin fields show N/A.',
      },
    },
  },
}

export const ComputedVolumePrimaryOnly: Story = {
  render: () => ({
    components: { SiteIndicesTable },
    setup() {
      const store = useModelParameterStore()
      store.siteSpeciesValues = CONSTANTS.SITE_SPECIES_VALUES.COMPUTED
      store.derivedBy = CONSTANTS.DERIVED_BY.VOLUME
      store.siteIndexRows = [
        makeRow('FD', { bhaSiteIndex: null, computedValue: CONSTANTS.COMPUTED_VALUE.BHA_SITE_INDEX }),
        makeRow('PL', { age: null, height: null, bhaSiteIndex: null }),
        makeRow('S', { age: null, height: null, bhaSiteIndex: null }),
      ]
    },
    template: '<SiteIndicesTable :isConfirmEnabled="true" />',
  }),
  parameters: {
    docs: {
      description: {
        story: 'Computed + Volume: only the primary row (FD) is editable. Non-primary rows show N/A in all fields.',
      },
    },
  },
}

export const TotalAgeComputed: Story = {
  render: () => ({
    components: { SiteIndicesTable },
    setup() {
      const store = useModelParameterStore()
      store.siteSpeciesValues = CONSTANTS.SITE_SPECIES_VALUES.COMPUTED
      store.derivedBy = CONSTANTS.DERIVED_BY.BASAL_AREA
      store.siteIndexRows = [
        makeRow('FD', { age: null, computedValue: CONSTANTS.COMPUTED_VALUE.TOTAL_AGE }),
      ]
    },
    template: '<SiteIndicesTable :isConfirmEnabled="true" />',
  }),
  parameters: {
    docs: {
      description: {
        story: 'computedValue = Total Age: Age spin shows Calc. placeholder and is disabled; Height and BHA remain editable.',
      },
    },
  },
}

export const HeightComputed: Story = {
  render: () => ({
    components: { SiteIndicesTable },
    setup() {
      const store = useModelParameterStore()
      store.siteSpeciesValues = CONSTANTS.SITE_SPECIES_VALUES.COMPUTED
      store.derivedBy = CONSTANTS.DERIVED_BY.BASAL_AREA
      store.siteIndexRows = [
        makeRow('FD', { height: null, computedValue: CONSTANTS.COMPUTED_VALUE.HEIGHT }),
      ]
    },
    template: '<SiteIndicesTable :isConfirmEnabled="true" />',
  }),
  parameters: {
    docs: {
      description: {
        story: 'computedValue = Height in Meters: Height spin shows Calc. placeholder and is disabled; Age and BHA remain editable.',
      },
    },
  },
}

export const Empty: Story = {
  render: () => ({
    components: { SiteIndicesTable },
    setup() {
      const store = useModelParameterStore()
      store.siteIndexRows = []
    },
    template: '<SiteIndicesTable :isConfirmEnabled="false" />',
  }),
  parameters: {
    docs: {
      description: {
        story: 'No species rows - only the header and divider are rendered.',
      },
    },
  },
}
