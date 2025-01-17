import type { Meta, StoryObj } from '@storybook/vue3'
import TopProjectYear from './TopProjectYear.vue'

const meta: Meta<typeof TopProjectYear> = {
  title: 'components/projection-setup/TopProjectYear',
  component: TopProjectYear,
  argTypes: {
    title: {
      control: 'text',
      description: 'The title displayed in the component.',
      defaultValue: 'Projects',
    },
    year: {
      control: 'text',
      description: 'The year displayed in the component.',
      defaultValue: '2024/2025',
    },
  },
  tags: ['autodocs'],
}

export default meta

type Story = StoryObj<typeof TopProjectYear>

export const Default: Story = {
  args: {
    title: 'Projects',
    year: '2024/2025',
  },
}
