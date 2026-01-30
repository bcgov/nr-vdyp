import type { Meta, StoryObj } from '@storybook/vue3-vite'
import ProjectionStatusBadge from './ProjectionStatusBadge.vue'

const meta: Meta<typeof ProjectionStatusBadge> = {
  title: 'Components/projection-list/ProjectionStatusBadge',
  component: ProjectionStatusBadge,
  tags: ['autodocs'],
  argTypes: {
    status: {
      control: 'select',
      options: ['Draft', 'Ready', 'Running', 'Failed'],
      description: 'The projection status',
    },
  },
}

export default meta
type Story = StoryObj<typeof ProjectionStatusBadge>

export const Draft: Story = {
  args: {
    status: 'Draft',
  },
}

export const Ready: Story = {
  args: {
    status: 'Ready',
  },
}

export const Running: Story = {
  args: {
    status: 'Running',
  },
}

export const Failed: Story = {
  args: {
    status: 'Failed',
  },
}

export const AllStatuses: Story = {
  render: () => ({
    components: { ProjectionStatusBadge },
    template: `
      <div style="display: flex; flex-direction: column; gap: 16px;">
        <ProjectionStatusBadge status="Draft" />
        <ProjectionStatusBadge status="Ready" />
        <ProjectionStatusBadge status="Running" />
        <ProjectionStatusBadge status="Failed" />
      </div>
    `,
  }),
}
