import type { Meta, StoryObj } from '@storybook/vue3-vite'
import TrainingSupport from './TrainingSupport.vue'

const meta = {
  title: 'components/layout/TrainingSupport',
  component: TrainingSupport,
  tags: ['autodocs'],
  argTypes: {
    fullText: {
      control: 'text',
      description: 'Full text to display on large screens (>= 920px)',
    },
    shortText: {
      control: 'text',
      description: 'Abbreviated text to display on small screens (< 920px)',
    },
  },
} satisfies Meta<typeof TrainingSupport>

export default meta
type Story = StoryObj<typeof meta>

export const Default: Story = {
  args: {
    fullText: 'Training and Support',
    shortText: 'Support',
  },
}
