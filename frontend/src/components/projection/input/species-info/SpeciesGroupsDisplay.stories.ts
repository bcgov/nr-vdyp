import type { Meta, StoryObj } from '@storybook/vue3-vite'
import SpeciesGroupsDisplay from './SpeciesGroupsDisplay.vue'

const meta: Meta<typeof SpeciesGroupsDisplay> = {
  title: 'components/projection/input/species-info/SpeciesGroupsDisplay',
  component: SpeciesGroupsDisplay,
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
    template: `<v-row><SpeciesGroupsDisplay v-bind="args" /></v-row>`,
  }),
  args: {
    speciesGroups: [
      { group: 'FD', percent: '50.0', siteSpecies: 'FD' },
      { group: 'PL', percent: '30.0', siteSpecies: 'PLI' },
      { group: 'HW', percent: '20.0', siteSpecies: 'HW' },
    ],
  },
}

export const SingleGroup: Story = {
  render: (args) => ({
    components: { SpeciesGroupsDisplay },
    setup() {
      return { args }
    },
    template: `<v-row><SpeciesGroupsDisplay v-bind="args" /></v-row>`,
  }),
  args: {
    speciesGroups: [{ group: 'FD', percent: '100.0', siteSpecies: 'FD' }],
  },
}

export const MergedGroups: Story = {
  render: (args) => ({
    components: { SpeciesGroupsDisplay },
    setup() {
      return { args }
    },
    template: `<v-row><SpeciesGroupsDisplay v-bind="args" /></v-row>`,
  }),
  args: {
    speciesGroups: [
      { group: 'PL', percent: '40.0', siteSpecies: 'PLI' },
      { group: 'PL', percent: '20.0', siteSpecies: 'PLC' },
      { group: 'HW', percent: '40.0', siteSpecies: 'HW' },
    ],
  },
}
