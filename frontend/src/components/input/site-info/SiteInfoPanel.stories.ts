import type { Meta, StoryObj } from '@storybook/vue3'
import { createPinia, setActivePinia } from 'pinia'
import { useModelParameterStore } from '@/stores/modelParameterStore'
import SiteInfoPanel from './SiteInfoPanel.vue'
import { CONSTANTS } from '@/constants'

const pinia = createPinia()
setActivePinia(pinia)

const meta: Meta<typeof SiteInfoPanel> = {
  title: 'components/input/site-info/SiteInfoPanel',
  component: SiteInfoPanel,
  decorators: [
    (story) => {
      const modelParameterStore = useModelParameterStore()
      modelParameterStore.setDefaultValues()

      modelParameterStore.confirmPanel(
        CONSTANTS.MODEL_PARAMETER_PANEL.SPECIES_INFO,
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

type Story = StoryObj<typeof SiteInfoPanel>

export const Default: Story = {
  render: () => {
    const modelParameterStore = useModelParameterStore()
    modelParameterStore.editPanel(CONSTANTS.MODEL_PARAMETER_PANEL.SITE_INFO)

    return {
      components: { SiteInfoPanel },
      template: '<SiteInfoPanel />',
    }
  },
}

export const Confirmed: Story = {
  render: () => {
    const modelParameterStore = useModelParameterStore()
    modelParameterStore.confirmPanel(CONSTANTS.MODEL_PARAMETER_PANEL.SITE_INFO)

    return {
      components: { SiteInfoPanel },
      template: '<SiteInfoPanel />',
    }
  },
}
