import type { Meta, StoryObj } from '@storybook/vue3-vite'
import SpeciesSelectionModal from './SpeciesSelectionModal.vue'

const meta: Meta<typeof SpeciesSelectionModal> = {
  title: 'components/projection/input/species-info/SpeciesSelectionModal',
  component: SpeciesSelectionModal,
  tags: ['autodocs'],
}

export default meta
type Story = StoryObj<typeof SpeciesSelectionModal>

export const Empty: Story = {
  render: (args) => ({
    components: { SpeciesSelectionModal },
    setup() {
      return { args }
    },
    template: `<SpeciesSelectionModal v-bind="args" />`,
  }),
  args: {
    modelValue: true,
    existingSpecies: [],
    maxSpecies: 6,
  },
}

export const WithPreselectedSpecies: Story = {
  render: (args) => ({
    components: { SpeciesSelectionModal },
    setup() {
      return { args }
    },
    template: `<SpeciesSelectionModal v-bind="args" />`,
  }),
  args: {
    modelValue: true,
    existingSpecies: ['FD', 'PL'],
    maxSpecies: 6,
  },
}

export const AtLimit: Story = {
  render: (args) => ({
    components: { SpeciesSelectionModal },
    setup() {
      return { args }
    },
    template: `<SpeciesSelectionModal v-bind="args" />`,
  }),
  args: {
    modelValue: true,
    existingSpecies: ['FD', 'PL', 'AC', 'AT', 'B', 'BA'],
    maxSpecies: 6,
  },
}
