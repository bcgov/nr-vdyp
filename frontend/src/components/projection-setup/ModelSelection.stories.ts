import type { Meta, StoryObj } from '@storybook/vue3'
import ModelSelection from './ModelSelection.vue'

const meta: Meta<typeof ModelSelection> = {
  title: 'components/projection-setup/ModelSelection',
  component: ModelSelection,
  argTypes: {
    'onUpdate:modelSelection': {
      action: 'update:modelSelection',
      description: 'Emits the selected model value.',
    },
  },
  tags: ['autodocs'],
}

export default meta

type Story = StoryObj<typeof ModelSelection>

export const Default: Story = {}
