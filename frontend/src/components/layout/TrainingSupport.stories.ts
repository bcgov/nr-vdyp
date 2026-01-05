import type { Meta, StoryObj } from '@storybook/vue3-vite'
import TrainingSupport from './TrainingSupport.vue'

const meta = {
  title: 'components/layout/TrainingSupport',
  component: TrainingSupport,
  tags: ['autodocs'],
  argTypes: {
    text: {
      control: 'text',
      description: 'Text to display in the component',
    },
  },
} satisfies Meta<typeof TrainingSupport>

export default meta
type Story = StoryObj<typeof meta>

export const Default: Story = {
  args: {
    text: 'Training and Support',
  },
}
