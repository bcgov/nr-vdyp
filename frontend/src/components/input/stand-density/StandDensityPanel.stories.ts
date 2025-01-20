import type { Meta, StoryObj } from '@storybook/vue3'
import StandDensityPanel from './StandDensityPanel.vue'

const meta: Meta<typeof StandDensityPanel> = {
  title: 'components/input/stand-density/StandDensityPanel',
  component: StandDensityPanel,
  argTypes: {
    panelOpenStates: {
      control: { type: 'object' },
      description: 'State of panel (open or closed)',
      defaultValue: {
        standDensity: 1, // 1 for open, 0 for closed
      },
    },
    percentStockableArea: {
      control: { type: 'number' },
      description: 'Percent Stockable Area input value.',
      defaultValue: 75,
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

type Story = StoryObj<typeof StandDensityPanel>

export const Default: Story = {
  render: (args) => ({
    components: { StandDensityPanel },
    setup() {
      return { args }
    },
    template: `<StandDensityPanel v-bind="args" />`,
  }),
  args: {
    panelOpenStates: { standDensity: 1 },
    percentStockableArea: 75,
    isConfirmEnabled: true,
    isConfirmed: false,
  },
}

export const Confirmed: Story = {
  render: (args) => ({
    components: { StandDensityPanel },
    setup() {
      return { args }
    },
    template: `<StandDensityPanel v-bind="args" />`,
  }),
  args: {
    panelOpenStates: { standDensity: 0 },
    percentStockableArea: 85,
    isConfirmEnabled: false,
    isConfirmed: true,
  },
}

export const EmptyState: Story = {
  render: (args) => ({
    components: { StandDensityPanel },
    setup() {
      return { args }
    },
    template: `<StandDensityPanel v-bind="args" />`,
  }),
  args: {
    panelOpenStates: { standDensity: 1 },
    percentStockableArea: null,
    isConfirmEnabled: true,
    isConfirmed: false,
  },
}
