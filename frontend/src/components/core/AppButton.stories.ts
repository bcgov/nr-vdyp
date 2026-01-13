import type { Meta, StoryObj } from '@storybook/vue3-vite'
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
    variant: {
      control: 'select',
      options: ['primary', 'secondary', 'tertiary', 'link'],
      description: 'The button variant/style',
      defaultValue: 'primary',
    },
    size: {
      control: 'select',
      options: ['xsmall', 'small', 'medium', 'large'],
      description: 'The button size',
      defaultValue: 'medium',
    },
    danger: {
      control: 'boolean',
      description: 'Applies danger styling',
      defaultValue: false,
    },
    isDisabled: {
      control: 'boolean',
      description: 'Disables the button if true',
      defaultValue: false,
    },
    leftIcon: {
      control: 'text',
      description: 'Icon to display on the left side',
      defaultValue: '',
    },
    rightIcon: {
      control: 'text',
      description: 'Icon to display on the right side',
      defaultValue: '',
    },
    ariaLabel: {
      control: 'text',
      description: 'Accessible label for screen readers (required for icon-only buttons)',
      defaultValue: '',
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
    label: 'Primary Button',
    variant: 'primary',
    size: 'medium',
    isDisabled: false,
  },
}

export const Secondary: Story = {
  args: {
    label: 'Secondary Button',
    variant: 'secondary',
    size: 'medium',
    isDisabled: false,
  },
}

export const Tertiary: Story = {
  args: {
    label: 'Tertiary Button',
    variant: 'tertiary',
    size: 'medium',
    isDisabled: false,
  },
}

export const Link: Story = {
  args: {
    label: 'Link Button',
    variant: 'link',
    size: 'medium',
    isDisabled: false,
  },
}

export const Danger: Story = {
  args: {
    label: 'Danger Button',
    variant: 'primary',
    size: 'medium',
    danger: true,
    isDisabled: false,
  },
}

export const Disabled: Story = {
  args: {
    label: 'Disabled Button',
    variant: 'primary',
    size: 'medium',
    isDisabled: true,
  },
}

export const Small: Story = {
  args: {
    label: 'Small Button',
    variant: 'primary',
    size: 'small',
    isDisabled: false,
  },
}

export const Large: Story = {
  args: {
    label: 'Large Button',
    variant: 'primary',
    size: 'large',
    isDisabled: false,
  },
}

export const Icon: Story = {
  args: {
    label: '',
    variant: 'primary',
    size: 'medium',
    leftIcon: 'mdi-cog',
    ariaLabel: 'Settings',
    isDisabled: false,
  },
}

export const LeftIcon: Story = {
  args: {
    label: 'Left Icon',
    variant: 'primary',
    size: 'medium',
    leftIcon: 'mdi-check-circle',
    isDisabled: false,
  },
}

export const RightIcon: Story = {
  args: {
    label: 'Right Icon',
    variant: 'primary',
    size: 'medium',
    rightIcon: 'mdi-arrow-right',
    isDisabled: false,
  },
}

export const BothIcons: Story = {
  args: {
    label: 'Both Icons',
    variant: 'primary',
    size: 'medium',
    leftIcon: 'mdi-check-circle',
    rightIcon: 'mdi-arrow-right',
    isDisabled: false,
  },
}
