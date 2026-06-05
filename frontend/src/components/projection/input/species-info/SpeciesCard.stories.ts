import type { Meta, StoryObj } from '@storybook/vue3-vite'
import SpeciesCard from './SpeciesCard.vue'

const meta: Meta<typeof SpeciesCard> = {
  title: 'components/projection/input/species-info/SpeciesCard',
  component: SpeciesCard,
  argTypes: {
    speciesCode: {
      control: 'select',
      options: ['FD', 'PL', 'HW', 'CW', 'AT', 'BL', 'SE', 'SX'],
    },
    percent: {
      control: 'text',
    },
    isDisabled: {
      control: 'boolean',
    },
  },
  tags: ['autodocs'],
}

export default meta
type Story = StoryObj<typeof SpeciesCard>

export const Default: Story = {
  render: (args) => ({
    components: { SpeciesCard },
    setup() {
      return { args }
    },
    template: `<SpeciesCard v-bind="args" />`,
  }),
  args: {
    speciesCode: 'FD',
    percent: '30.0',
    isDisabled: false,
  },
}

export const Disabled: Story = {
  render: (args) => ({
    components: { SpeciesCard },
    setup() {
      return { args }
    },
    template: `<SpeciesCard v-bind="args" />`,
  }),
  args: {
    ...Default.args,
    isDisabled: true,
  },
}

export const FullPercent: Story = {
  render: (args) => ({
    components: { SpeciesCard },
    setup() {
      return { args }
    },
    template: `<SpeciesCard v-bind="args" />`,
  }),
  args: {
    ...Default.args,
    percent: '100.0',
  },
}

