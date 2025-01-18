import type { Meta, StoryObj } from '@storybook/vue3'
import SpeciesInfoPanel from './SpeciesInfoPanel.vue'

const meta: Meta<typeof SpeciesInfoPanel> = {
  title: 'components/input/species-info/SpeciesInfoPanel',
  component: SpeciesInfoPanel,
  argTypes: {
    speciesList: {
      control: { type: 'object' },
      description: 'List of species with their percentages.',
      defaultValue: [
        { species: 'PL', percent: '30.0' },
        { species: 'AC', percent: '30.0' },
        { species: 'H', percent: '30.0' },
        { species: 'S', percent: '10.0' },
      ],
    },
    speciesGroups: {
      control: { type: 'object' },
      description: 'Grouped species information.',
      defaultValue: [
        { group: 'Group 1', percent: '50.0', siteSpecies: 'Pine' },
        { group: 'Group 2', percent: '50.0', siteSpecies: 'Spruce' },
      ],
    },
    isConfirmEnabled: {
      control: { type: 'boolean' },
      description: 'Determines if the confirm actions are enabled.',
      defaultValue: true,
    },
    isConfirmed: {
      control: { type: 'boolean' },
      description: 'Indicates if the panel is confirmed.',
      defaultValue: false,
    },
  },
  tags: ['autodocs'],
}

export default meta

type Story = StoryObj<typeof SpeciesInfoPanel>

export const Default: Story = {
  render: (args) => ({
    components: { SpeciesInfoPanel },
    setup() {
      return { args }
    },
    template: `<SpeciesInfoPanel v-bind="args" />`,
  }),
  args: {
    speciesList: [
      { species: 'PL', percent: '30.0' },
      { species: 'AC', percent: '30.0' },
      { species: 'H', percent: '30.0' },
      { species: 'S', percent: '10.0' },
    ],
    speciesGroups: [
      { group: 'Group 1', percent: '50.0', siteSpecies: 'Pine' },
      { group: 'Group 2', percent: '50.0', siteSpecies: 'Spruce' },
    ],
    isConfirmEnabled: true,
    isConfirmed: false,
  },
}

export const Confirmed: Story = {
  render: (args) => ({
    components: { SpeciesInfoPanel },
    setup() {
      return { args }
    },
    template: `<SpeciesInfoPanel v-bind="args" />`,
  }),
  args: {
    speciesList: [
      { species: 'PL', percent: '40.0' },
      { species: 'AC', percent: '35.0' },
      { species: 'H', percent: '25.0' },
    ],
    speciesGroups: [
      { group: 'Group 1', percent: '60.0', siteSpecies: 'Lodgepole Pine' },
      { group: 'Group 2', percent: '40.0', siteSpecies: 'Hemlock' },
    ],
    isConfirmEnabled: false,
    isConfirmed: true,
  },
}
