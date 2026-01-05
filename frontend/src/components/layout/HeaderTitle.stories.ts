import type { Meta, StoryObj } from '@storybook/vue3-vite'
import HeaderTitle from './HeaderTitle.vue'

const meta = {
  title: 'components/layout/HeaderTitle',
  component: HeaderTitle,
  tags: ['autodocs'],
  argTypes: {
    text: {
      control: 'text',
      description: 'Text to display in the header title',
    },
  },
} satisfies Meta<typeof HeaderTitle>

export default meta
type Story = StoryObj<typeof meta>

export const Default: Story = {
  args: {
    text: 'VARIABLE DENSITY YIELD PROJECTION',
  },
}
