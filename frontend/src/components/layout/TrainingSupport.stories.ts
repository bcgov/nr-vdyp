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
    customStyle: {
      control: 'object',
      description: 'Custom style to apply to the component.',
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

export const CustomStyle: Story = {
  args: {
    text: 'Styled Training and Support',
    customStyle: {
      fontSize: '18px',
      color: '#1976d2',
      fontWeight: 'bold',
      textDecoration: 'underline',
    },
  },
}
