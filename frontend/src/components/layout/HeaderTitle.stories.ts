import type { Meta, StoryObj } from '@storybook/vue3-vite'
import HeaderTitle from './HeaderTitle.vue'

const meta = {
  title: 'components/layout/HeaderTitle',
  component: HeaderTitle,
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
} satisfies Meta<typeof HeaderTitle>

export default meta
type Story = StoryObj<typeof meta>

export const Default: Story = {
  args: {
    fullText: 'Variable Density Yield Projection',
    shortText: 'VDYP',
  },
}
