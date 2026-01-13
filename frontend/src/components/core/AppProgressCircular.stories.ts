import type { Meta, StoryObj } from '@storybook/vue3-vite'
import AppProgressCircular from './AppProgressCircular.vue'

const meta: Meta<typeof AppProgressCircular> = {
  title: 'components/core/AppProgressCircular',
  component: AppProgressCircular,
  tags: ['autodocs'],
  argTypes: {
    isShow: { control: { type: 'boolean' }, defaultValue: true },
    showMessage: { control: { type: 'boolean' }, defaultValue: true },
    hasBackground: {
      control: { type: 'boolean' },
      defaultValue: true,
      description: 'Whether to show background wrapper with border and shadow',
    },
    message: { control: { type: 'text' }, defaultValue: 'Loading...' },
    circleSize: {
      control: { type: 'range', min: 10, max: 200, step: 1 },
      defaultValue: 70,
      description: 'Size of the circular progress indicator',
    },
    circleWidth: {
      control: { type: 'range', min: 1, max: 50, step: 1 },
      defaultValue: 5,
      description: 'Width of the circular progress indicator',
    },
    circleColor: {
      control: { type: 'color' },
      defaultValue: 'primary',
      description: 'Color of the circular progress indicator',
    },
  },
}

export default meta

type Story = StoryObj<typeof AppProgressCircular>

export const Primary: Story = {
  args: {
    isShow: true,
    showMessage: true,
    hasBackground: true,
    message: 'Loading...',
    circleSize: 70,
    circleWidth: 5,
    circleColor: 'primary',
  },
}

export const WithoutBackground: Story = {
  args: {
    isShow: true,
    showMessage: true,
    hasBackground: false,
    message: 'Processing...',
    circleSize: 70,
    circleWidth: 5,
    circleColor: 'primary',
  },
}

export const WithoutMessage: Story = {
  args: {
    isShow: true,
    showMessage: false,
    hasBackground: true,
    circleSize: 70,
    circleWidth: 5,
    circleColor: 'primary',
  },
}
