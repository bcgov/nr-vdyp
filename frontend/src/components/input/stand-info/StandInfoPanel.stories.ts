import type { Meta, StoryObj } from '@storybook/vue3-vite'
import { createPinia, setActivePinia } from 'pinia'
import { useModelParameterStore } from '@/stores/modelParameterStore'
import StandInfoPanel from './StandInfoPanel.vue'
import { CONSTANTS } from '@/constants'

const pinia = createPinia()
setActivePinia(pinia)

const meta: Meta<typeof StandInfoPanel> = {
  title: 'components/input/stand-info/StandInfoPanel',
  component: StandInfoPanel,
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

type Story = StoryObj<typeof StandInfoPanel>

export const Default: Story = {
  render: () => {
    const modelParameterStore = useModelParameterStore()
    modelParameterStore.editPanel(CONSTANTS.MODEL_PARAMETER_PANEL.STAND_INFO)

    return {
      components: { StandInfoPanel },
      template: '<StandInfoPanel />',
    }
  },
}

export const Confirmed: Story = {
  render: () => {
    const modelParameterStore = useModelParameterStore()
    modelParameterStore.confirmPanel(CONSTANTS.MODEL_PARAMETER_PANEL.STAND_INFO)

    return {
      components: { StandInfoPanel },
      template: '<StandInfoPanel />',
    }
  },
}
