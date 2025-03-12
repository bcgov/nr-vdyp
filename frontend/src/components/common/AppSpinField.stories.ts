import type { Meta, StoryObj } from '@storybook/vue3'
import AppSpinField from './AppSpinField.vue'
import { CONSTANTS } from '@/constants'

const meta: Meta<typeof AppSpinField> = {
  title: 'components/common/AppSpinField',
  component: AppSpinField,
  argTypes: {
    label: {
      control: 'text',
      description: 'The label for the input field.',
      defaultValue: 'Spin Field',
    },
    modelValue: {
      control: 'text',
      description: 'The current value of the field.',
      defaultValue: '10',
    },
    max: {
      control: 'number',
      description: 'The maximum value allowed.',
      defaultValue: 60,
    },
    min: {
      control: 'number',
      description: 'The minimum value allowed.',
      defaultValue: 0,
    },
    step: {
      control: 'number',
      description: 'The increment/decrement step value.',
      defaultValue: 1,
    },
    persistentPlaceholder: {
      control: 'boolean',
      description: 'Whether to persist the placeholder text.',
      defaultValue: false,
    },
    placeholder: {
      control: 'text',
      description: 'Placeholder text for the input field.',
      defaultValue: 'Enter a value...',
    },
    hideDetails: {
      control: 'boolean',
      description: 'Whether to hide the input field details.',
      defaultValue: true,
    },
    density: {
      control: 'select',
      options: ['default', 'compact', 'comfortable'],
      description: 'The density of the input field.',
      defaultValue: 'default',
    },
    dense: {
      control: 'boolean',
      description: 'Whether to use dense mode.',
      defaultValue: false,
    },
    customStyle: {
      control: 'text',
      description: 'Custom CSS styles for the input field.',
      defaultValue: '',
    },
    variant: {
      control: 'select',
      options: ['filled', 'outlined', 'solo', 'plain'],
      description: 'The variant of the input field.',
      defaultValue: 'filled',
    },
    disabled: {
      control: 'boolean',
      description: 'Whether the input field is disabled.',
      defaultValue: false,
    },
    interval: {
      control: 'number',
      description: 'Interval for continuous increment/decrement.',
      defaultValue: 100,
    },
    decimalAllowNumber: {
      control: 'number',
      description: 'Number of decimal places allowed.',
      defaultValue: 2,
    },
    'update:modelValue': {
      action: 'update:modelValue',
      description: 'Emits the updated value of the field.',
    },
  },
  tags: ['autodocs'],
}

export default meta

type Story = StoryObj<typeof AppSpinField>

export const Default: Story = {
  render: (args) => ({
    components: { AppSpinField },
    setup() {
      return { args }
    },
    template: `
      <AppSpinField v-bind="args" @update:modelValue="args['update:modelValue']" />
    `,
  }),
  args: {
    label: 'Spin Field',
    modelValue: '10',
    max: 60,
    min: 0,
    step: 1,
    placeholder: '',
    persistentPlaceholder: true,
    hideDetails: true,
    density: 'compact',
    dense: true,
    customStyle: 'padding-left: 15px',
    variant: 'plain',
    disabled: false,
    interval: CONSTANTS.CONTINUOUS_INC_DEC.INTERVAL,
    decimalAllowNumber: 2,
  },
}

export const Disabled: Story = {
  render: (args) => ({
    components: { AppSpinField },
    setup() {
      return { args }
    },
    template: `
      <AppSpinField v-bind="args" @update:modelValue="args['update:modelValue']" />
    `,
  }),
  args: {
    ...Default.args,
    disabled: true,
  },
}
