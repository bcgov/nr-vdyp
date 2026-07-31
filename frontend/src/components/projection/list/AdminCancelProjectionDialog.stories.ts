import type { Meta, StoryObj } from '@storybook/vue3-vite'
import AdminCancelProjectionDialog from './AdminCancelProjectionDialog.vue'

const meta: Meta<typeof AdminCancelProjectionDialog> = {
  title: 'components/projection/list/AdminCancelProjectionDialog',
  component: AdminCancelProjectionDialog,
  tags: ['autodocs'],
}

export default meta
type Story = StoryObj<typeof AdminCancelProjectionDialog>

export const Default: Story = {
  render: (args) => ({
    components: { AdminCancelProjectionDialog },
    setup() {
      return { args }
    },
    template: `<AdminCancelProjectionDialog v-bind="args" />`,
  }),
  args: {
    modelValue: true,
    projectionTitle: 'Sample Projection',
  },
}
