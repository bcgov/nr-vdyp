import type { Meta, StoryObj } from '@storybook/vue3-vite'
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
  },
  tags: ['autodocs'],
}

export default meta

type Story = StoryObj<typeof TopProjectYear>

export const Default: Story = {
  args: {
    title: 'Projects',
  },
}
