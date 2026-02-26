import type { Meta, StoryObj } from '@storybook/vue3-vite'
import ProjectionStatusBadge from './ProjectionStatusBadge.vue'

const meta: Meta<typeof ProjectionStatusBadge> = {
  title: 'Components/projection-list/ProjectionStatusBadge',
  component: ProjectionStatusBadge,
  tags: ['autodocs'],
  argTypes: {
    status: {
      control: { type: 'select' },
      options: ['Draft', 'Ready', 'Running', 'Failed'],
      description: 'The projection status',
    },
  },
  parameters: {
    docs: {
      description: {
        component: `
A badge component that displays a projection status with an icon and styled text.

**Status styles:**
- **Draft**: Bold text with secondary color
- **Ready**: Bold text with success color
- **Running**: Bold text with warning color
- **Failed**: Bold text with error color
        `,
      },
    },
  },
}

export default meta
type Story = StoryObj<typeof ProjectionStatusBadge>

export const Draft: Story = {
  args: {
    status: 'Draft',
  },
  parameters: {
    docs: {
      description: {
        story: 'Draft status: bold secondary-colored text with Draft_Icon_Status icon.',
      },
    },
  },
}

export const Ready: Story = {
  args: {
    status: 'Ready',
  },
  parameters: {
    docs: {
      description: {
        story: 'Ready status: bold success-colored text with Ready_Icon_Status icon.',
      },
    },
  },
}

export const Running: Story = {
  args: {
    status: 'Running',
  },
  parameters: {
    docs: {
      description: {
        story: 'Running status: bold warning-colored text with Running_Icon_Status icon.',
      },
    },
  },
}

export const Failed: Story = {
  args: {
    status: 'Failed',
  },
  parameters: {
    docs: {
      description: {
        story: 'Failed status: bold error-colored text with Failed_Icon_Status icon.',
      },
    },
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
  parameters: {
    docs: {
      description: {
        story: 'All four supported statuses displayed together for visual comparison.',
      },
    },
  },
}
