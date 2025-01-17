import type { Meta, StoryObj } from '@storybook/vue3'
import AppButton from './AppButton.vue'

const meta: Meta<typeof AppButton> = {
  title: 'components/core/AppButton',
  component: AppButton,
  tags: ['autodocs'],
  argTypes: {
    label: {
      control: 'text',
      description: 'The label displayed on the button',
      defaultValue: 'Button',
    },
    isDisabled: {
      control: 'boolean',
      description: 'Disables the button if true',
      defaultValue: false,
    },
    customClass: {
      control: 'text',
      description: 'Determines the button style',
      defaultValue: true,
    },
    onClick: {
      action: 'click',
      description: 'Emits when the button is clicked',
    },
  },
}

export default meta
type Story = StoryObj<typeof AppButton>

export const Primary: Story = {
  args: {
    label: 'Button',
    isDisabled: false,
    customClass: 'blue-btn',
  },
}

export const Secondary: Story = {
  args: {
    label: 'Button',
    isDisabled: false,
    customClass: 'white-btn',
  },
}
