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
  })
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
  })
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
  })
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
  })
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
  })
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
  })
}

export const Empty: Story = {
  render: () => ({
    components: { SiteIndicesTable },
    setup() {
      const store = useModelParameterStore()
      store.siteIndexRows = []
    },
    template: '<SiteIndicesTable :isConfirmEnabled="false" />',
  })
}
