import type { Meta, StoryObj } from '@storybook/vue3'
import HeaderTitle from './HeaderTitle.vue'

const meta = {
  title: 'components/layout/HeaderTitle',
  component: HeaderTitle,
  tags: ['autodocs'],
  argTypes: {
    text: {
      control: 'text',
      description: 'Text to display in the toolbar title',
    },
    style: {
      control: 'object',
      description: 'Custom styles to override the default styles',
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

export const CustomStyle: Story = {
  args: {
    text: 'Custom Styled Header Title',
    style: {
      textAlign: 'left',
      fontWeight: 'bold',
      fontSize: '24px',
      color: '#1976d2',
    },
  },
}
