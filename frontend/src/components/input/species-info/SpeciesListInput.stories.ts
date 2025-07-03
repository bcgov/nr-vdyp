import type { Meta, StoryObj } from '@storybook/vue3-vite'
import SpeciesListInput from './SpeciesListInput.vue'
import { CONSTANTS, BIZCONSTANTS } from '@/constants'
import { action } from 'storybook/actions'

const computedSpeciesOptions = Object.keys(BIZCONSTANTS.SPECIES_MAP).map(
  (code) => ({
    label: `${code} - ${BIZCONSTANTS.SPECIES_MAP[code as keyof typeof BIZCONSTANTS.SPECIES_MAP]}`,
    value: code,
  }),
)

const meta: Meta<typeof SpeciesListInput> = {
  title: 'components/input/species-info/SpeciesListInput',
  component: SpeciesListInput,
  argTypes: {
    speciesList: {
      control: { type: 'object' },
      description: 'List of species with their percentages.',
      defaultValue: [
        { species: 'PL', percent: '30.0' },
        { species: 'AC', percent: '30.0' },
        { species: 'H', percent: '30.0' },
        { species: 'S', percent: '10.0' },
        { species: null, percent: '0.0' },
        { species: null, percent: '0.0' },
      ],
    },
    computedSpeciesOptions: {
      control: { type: 'object' },
      description: 'Options for species selection.',
      defaultValue: computedSpeciesOptions,
    },
    isConfirmEnabled: {
      control: { type: 'boolean' },
      description: 'Whether the inputs are enabled for editing.',
      defaultValue: true,
    },
    max: {
      control: { type: 'number' },
      description: 'Maximum value for the percent input.',
      defaultValue: CONSTANTS.NUM_INPUT_LIMITS.SPECIES_PERCENT_MAX,
    },
    min: {
      control: { type: 'number' },
      description: 'Minimum value for the percent input.',
      defaultValue: CONSTANTS.NUM_INPUT_LIMITS.SPECIES_PERCENT_MIN,
    },
    step: {
      control: { type: 'number' },
      description: 'Step value for the percent input.',
      defaultValue: CONSTANTS.NUM_INPUT_LIMITS.SPECIES_PERCENT_STEP,
    },
  },
  tags: ['autodocs'],
}

export default meta

type Story = StoryObj<typeof SpeciesListInput>

export const Default: Story = {
  render: (args) => ({
    components: { SpeciesListInput },
    setup() {
      return {
        args,
        onUpdateSpeciesList: action('update:speciesList'),
      }
    },
    template: `
      <SpeciesListInput
        v-bind="args"
        @update:speciesList="onUpdateSpeciesList"
      />
    `,
  }),
  args: {
    speciesList: [
      { species: 'PL', percent: '30.0' },
      { species: 'AC', percent: '30.0' },
      { species: 'H', percent: '30.0' },
      { species: 'S', percent: '10.0' },
      { species: null, percent: '0.0' },
      { species: null, percent: '0.0' },
    ],
    computedSpeciesOptions: computedSpeciesOptions,
    isConfirmEnabled: true,
    max: CONSTANTS.NUM_INPUT_LIMITS.SPECIES_PERCENT_MAX,
    min: CONSTANTS.NUM_INPUT_LIMITS.SPECIES_PERCENT_MIN,
    step: CONSTANTS.NUM_INPUT_LIMITS.SPECIES_PERCENT_STEP,
  },
}

export const Disabled: Story = {
  render: Default.render,
  args: {
    ...Default.args,
    isConfirmEnabled: false,
  },
}
