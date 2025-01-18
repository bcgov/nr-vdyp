import type { Meta, StoryObj } from '@storybook/vue3'
import SpeciesGroupsDisplay from './SpeciesGroupsDisplay.vue'

const meta: Meta<typeof SpeciesGroupsDisplay> = {
  title: 'components/input/species-info/SpeciesGroupsDisplay',
  component: SpeciesGroupsDisplay,
  argTypes: {
    speciesGroups: {
      control: { type: 'object' },
      description: 'List of species groups to display.',
      defaultValue: [],
    },
  },
  tags: ['autodocs'],
}

export default meta

type Story = StoryObj<typeof SpeciesGroupsDisplay>

export const Default: Story = {
  render: (args) => ({
    components: { SpeciesGroupsDisplay },
    setup() {
      return { args }
    },
    template: `<SpeciesGroupsDisplay v-bind="args" />`,
  }),
  args: {
    speciesGroups: [
      { group: 'PL - Lodgepole Pine', percent: '30.0', siteSpecies: 'PL' },
      { group: 'AC - Poplar', percent: '30.0', siteSpecies: 'AC' },
      { group: 'H - Hemlock', percent: '30.0', siteSpecies: 'H' },
      { group: 'S - Spruce', percent: '10.0', siteSpecies: 'S' },
    ],
  },
}

export const Empty: Story = {
  render: (args) => ({
    components: { SpeciesGroupsDisplay },
    setup() {
      return { args }
    },
    template: `<SpeciesGroupsDisplay v-bind="args" />`,
  }),
  args: {
    speciesGroups: [],
  },
}
