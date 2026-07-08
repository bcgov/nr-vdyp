import type { Meta, StoryObj } from '@storybook/vue3-vite'
import PanelEditControl from './PanelEditControl.vue'

const meta: Meta<typeof PanelEditControl> = {
  title: 'Components/projection/input/PanelEditControl',
  component: PanelEditControl,
  tags: ['autodocs'],
  argTypes: {
    isReadOnly: { control: 'boolean' },
    editable: { control: 'boolean' },
    isHeaderEditActive: { control: 'boolean' },
    editTooltipText: { control: 'text' },
  },
}

export default meta
type Story = StoryObj<typeof PanelEditControl>

export const EditButton: Story = {
  args: {
    isReadOnly: false,
    editable: false,
    isHeaderEditActive: true,
    editTooltipText: '',
  },
}

export const EditButtonDisabled: Story = {
  args: {
    isReadOnly: false,
    editable: false,
    isHeaderEditActive: false,
    editTooltipText: 'Finish editing the current section first.',
  },
}

export const EditingBadge: Story = {
  args: {
    isReadOnly: false,
    editable: true,
    isHeaderEditActive: true,
  },
}
