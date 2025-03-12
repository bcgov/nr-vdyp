import type { Meta, StoryObj } from '@storybook/vue3'
import { createPinia, setActivePinia } from 'pinia'
import { useModelParameterStore } from '@/stores/modelParameterStore'
import SpeciesInfoPanel from './SpeciesInfoPanel.vue'
import { CONSTANTS } from '@/constants'

const pinia = createPinia()
setActivePinia(pinia)

const meta: Meta<typeof SpeciesInfoPanel> = {
  title: 'components/input/species-info/SpeciesInfoPanel',
  component: SpeciesInfoPanel,
  decorators: [
    (story) => {
      const modelParameterStore = useModelParameterStore()
      modelParameterStore.setDefaultValues()

      return {
        components: { story },
        template: `<div><story /></div>`,
      }
    },
  ],
  tags: ['autodocs'],
}

export default meta

type Story = StoryObj<typeof SpeciesInfoPanel>

export const Default: Story = {
  render: () => {
    const modelParameterStore = useModelParameterStore()
    modelParameterStore.editPanel(CONSTANTS.MODEL_PARAMETER_PANEL.SPECIES_INFO)

    return {
      components: { SpeciesInfoPanel },
      template: '<SpeciesInfoPanel />',
    }
  },
}

export const Confirmed: Story = {
  render: () => {
    const modelParameterStore = useModelParameterStore()
    modelParameterStore.confirmPanel(
      CONSTANTS.MODEL_PARAMETER_PANEL.SPECIES_INFO,
    )

    return {
      components: { SpeciesInfoPanel },
      template: '<SpeciesInfoPanel />',
    }
  },
}
