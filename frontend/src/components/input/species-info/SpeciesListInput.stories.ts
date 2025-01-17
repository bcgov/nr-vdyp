import type { Meta, StoryObj } from '@storybook/vue3'
import SpeciesListInput from './SpeciesListInput.vue'
import type { SpeciesList } from '@/interfaces/interfaces'

const meta: Meta<typeof SpeciesListInput> = {
  title: 'components/input/species-info/SpeciesListInput',
  component: SpeciesListInput,
  argTypes: {
    speciesList: {
      control: { type: 'object' },
      description: 'List of species with their percentages.',
      defaultValue: [
        { species: 'PL - Lodgepole Pine', percent: '30.0' },
        { species: 'AC - Poplar', percent: '30.0' },
        { species: 'H - Hemlock', percent: '30.0' },
        { species: 'S - Spruce', percent: '10.0' },
        { species: null, percent: '0.0' },
        { species: null, percent: '0.0' },
      ],
    },
    computedSpeciesOptions: {
      control: { type: 'object' },
      description: 'Options for species selection.',
      defaultValue: [
        { label: 'PL - Lodgepole Pine', value: 'PL' },
        { label: 'AC - Poplar', value: 'AC' },
        { label: 'H - Hemlock', value: 'H' },
        { label: 'S - Spruce', value: 'S' },
        { label: 'FD - Douglas-fir', value: 'FD' },
      ],
    },
    isConfirmEnabled: {
      control: { type: 'boolean' },
      description: 'Whether the inputs are enabled for editing.',
      defaultValue: true,
    },
    max: {
      control: { type: 'number' },
      description: 'Maximum value for the percent input.',
      defaultValue: 100,
    },
    min: {
      control: { type: 'number' },
      description: 'Minimum value for the percent input.',
      defaultValue: 0,
    },
    step: {
      control: { type: 'number' },
      description: 'Step value for the percent input.',
      defaultValue: 1,
    },
    'update:speciesList': {
      action: 'update:speciesList',
      description: 'Event emitted when the species list is updated.',
    },
  },
  tags: ['autodocs'],
}

export default meta

type Story = StoryObj<typeof SpeciesListInput>

export const Default: Story = {
  render: (args, { argTypes }) => ({
    components: { SpeciesListInput },
    props: Object.keys(argTypes),
    template: `
      <SpeciesListInput
        v-bind="$props"
        @update:speciesList="onSpeciesListUpdate"
      />
    `,
    methods: {
      onSpeciesListUpdate(newSpeciesList: SpeciesList) {
        console.log('Updated Species List:', newSpeciesList)
      },
    },
  }),
  args: {
    speciesList: [
      { species: 'PL - Lodgepole Pine', percent: '30.0' },
      { species: 'AC - Poplar', percent: '30.0' },
      { species: 'H - Hemlock', percent: '30.0' },
      { species: 'S - Spruce', percent: '10.0' },
      { species: null, percent: '0.0' },
      { species: null, percent: '0.0' },
    ],
    computedSpeciesOptions: [
      { label: 'PL - Lodgepole Pine', value: 'PL' },
      { label: 'AC - Poplar', value: 'AC' },
      { label: 'H - Hemlock', value: 'H' },
      { label: 'S - Spruce', value: 'S' },
      { label: 'FD - Douglas-fir', value: 'FD' },
    ],
    isConfirmEnabled: true,
    max: 100,
    min: 0,
    step: 1,
  },
}

export const Disabled: Story = {
  render: Default.render,
  args: {
    ...Default.args,
    isConfirmEnabled: false,
  },
}
