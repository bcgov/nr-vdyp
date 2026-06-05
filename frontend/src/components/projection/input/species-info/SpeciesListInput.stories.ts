import type { Meta, StoryObj } from '@storybook/vue3-vite'
import SpeciesListInput from './SpeciesListInput.vue'

const meta: Meta<typeof SpeciesListInput> = {
  title: 'components/projection/input/species-info/SpeciesListInput',
  component: SpeciesListInput,
  argTypes: {
    isConfirmEnabled: {
      control: 'boolean',
    },
  },
  tags: ['autodocs'],
}

export default meta
type Story = StoryObj<typeof SpeciesListInput>

export const Empty: Story = {
  render: (args) => ({
    components: { SpeciesListInput },
    setup() {
      return { args }
    },
    template: `<SpeciesListInput v-bind="args" />`,
  }),
  args: {
    speciesList: [],
    isConfirmEnabled: true,
  },
}

export const EmptyDisabled: Story = {
  render: (args) => ({
    components: { SpeciesListInput },
    setup() {
      return { args }
    },
    template: `<SpeciesListInput v-bind="args" />`,
  }),
  args: {
    speciesList: [],
    isConfirmEnabled: false,
  },
}

export const WithSpecies: Story = {
  render: (args) => ({
    components: { SpeciesListInput },
    setup() {
      return { args }
    },
    template: `<SpeciesListInput v-bind="args" />`,
  }),
  args: {
    speciesList: [
      { species: 'FD', percent: '60.0' },
      { species: 'PL', percent: '40.0' },
    ],
    isConfirmEnabled: true,
  },
}

export const FullList: Story = {
  render: (args) => ({
    components: { SpeciesListInput },
    setup() {
      return { args }
    },
    template: `<SpeciesListInput v-bind="args" />`,
  }),
  args: {
    speciesList: [
      { species: 'FD', percent: '25.0' },
      { species: 'PL', percent: '25.0' },
      { species: 'HW', percent: '20.0' },
      { species: 'CW', percent: '15.0' },
      { species: 'AT', percent: '10.0' },
      { species: 'BL', percent: '5.0' },
    ],
    isConfirmEnabled: true,
  },
}

export const Disabled: Story = {
  render: (args) => ({
    components: { SpeciesListInput },
    setup() {
      return { args }
    },
    template: `<SpeciesListInput v-bind="args" />`,
  }),
  args: {
    ...WithSpecies.args,
    isConfirmEnabled: false,
  },
}
