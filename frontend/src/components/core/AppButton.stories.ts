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
    },
    variant: {
      control: 'select',
      options: ['primary', 'secondary', 'tertiary', 'link', 'danger'],
      description: 'The button variant/style',
    },
    size: {
      control: 'select',
      options: ['xsmall', 'small', 'medium', 'large'],
      description: 'The button size',
    },
    isDisabled: {
      control: 'boolean',
      description: 'Disables the button if true',
    },
    mdiName: {
      control: 'text',
      description: 'MDI icon name (e.g., mdi-cog)',
    },
    iconSrc: {
      control: 'text',
      description: 'Image source URL for custom icon',
    },
    iconPosition: {
      control: 'select',
      options: ['left', 'right', 'top', 'bottom'],
      description: 'Position of the icon relative to the label',
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
    variant: 'danger',
    size: 'medium',
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

export const IconOnly: Story = {
  args: {
    label: '',
    variant: 'primary',
    size: 'medium',
    mdiName: 'mdi-cog',
    isDisabled: false,
  },
}

export const IconLeft: Story = {
  args: {
    label: 'Left Icon',
    variant: 'primary',
    size: 'medium',
    mdiName: 'mdi-check-circle',
    iconPosition: 'left',
    isDisabled: false,
  },
}

export const IconRight: Story = {
  args: {
    label: 'Right Icon',
    variant: 'primary',
    size: 'medium',
    mdiName: 'mdi-arrow-right',
    iconPosition: 'right',
    isDisabled: false,
  },
}

export const IconTop: Story = {
  args: {
    label: 'Top Icon',
    variant: 'primary',
    size: 'medium',
    mdiName: 'mdi-account',
    iconPosition: 'top',
    isDisabled: false,
  },
}

export const IconBottom: Story = {
  args: {
    label: 'Bottom Icon',
    variant: 'primary',
    size: 'medium',
    mdiName: 'mdi-account',
    iconPosition: 'bottom',
    isDisabled: false,
  },
}
