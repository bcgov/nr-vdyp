import type { Meta, StoryObj } from '@storybook/vue3'
import { createPinia, setActivePinia } from 'pinia'
import { useModelParameterStore } from '@/stores/modelParameterStore'
import StandDensityPanel from './StandDensityPanel.vue'
import { CONSTANTS } from '@/constants'

const pinia = createPinia()
setActivePinia(pinia)

const meta: Meta<typeof StandDensityPanel> = {
  title: 'components/input/stand-density/StandDensityPanel',
  component: StandDensityPanel,
  decorators: [
    (story) => {
      const modelParameterStore = useModelParameterStore()
      modelParameterStore.setDefaultValues()

      modelParameterStore.confirmPanel(
        CONSTANTS.MODEL_PARAMETER_PANEL.SPECIES_INFO,
      )
      modelParameterStore.confirmPanel(
        CONSTANTS.MODEL_PARAMETER_PANEL.SITE_INFO,
      )

      return {
        components: { story },
        template: `<div><story /></div>`,
      }
    },
  ],
  tags: ['autodocs'],
}

export default meta

type Story = StoryObj<typeof StandDensityPanel>

export const Default: Story = {
  render: () => {
    const modelParameterStore = useModelParameterStore()
    modelParameterStore.editPanel(CONSTANTS.MODEL_PARAMETER_PANEL.STAND_DENSITY)

    return {
      components: { StandDensityPanel },
      template: '<StandDensityPanel />',
    }
  },
}

export const Confirmed: Story = {
  render: () => {
    const modelParameterStore = useModelParameterStore()
    modelParameterStore.confirmPanel(
      CONSTANTS.MODEL_PARAMETER_PANEL.STAND_DENSITY,
    )

    return {
      components: { StandDensityPanel },
      template: '<StandDensityPanel />',
    }
  },
}
