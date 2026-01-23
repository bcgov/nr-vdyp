import type { Meta, StoryObj } from '@storybook/vue3-vite'
import ProjectionActionsMenu from './ProjectionActionsMenu.vue'

const meta: Meta<typeof ProjectionActionsMenu> = {
  title: 'components/projection-list/ProjectionActionsMenu',
  component: ProjectionActionsMenu,
  tags: ['autodocs'],
  argTypes: {
    status: {
      control: { type: 'select' },
      options: ['Draft', 'Ready', 'Running', 'Failed'],
      description: 'The projection status determines which menu items are visible.',
    },
    title: {
      control: { type: 'text' },
      description: 'The projection title used for accessibility.',
    },
  },
  parameters: {
    docs: {
      description: {
        component: `
A dropdown menu component that displays different action options based on the projection status.

**Menu items by status:**
- **Draft**: Edit, Duplicate, Delete
- **Ready**: View, Duplicate, Download, Delete
- **Running**: Cancel, Delete
- **Failed**: Edit, Duplicate, Download, Delete
        `,
      },
    },
  },
}

export default meta

type Story = StoryObj<typeof ProjectionActionsMenu>

export const Draft: Story = {
  render: (args) => ({
    components: { ProjectionActionsMenu },
    setup() {
      return { args }
    },
    template: `
      <ProjectionActionsMenu
        v-bind="args"
        @view="args.view"
        @edit="args.edit"
        @duplicate="args.duplicate"
        @download="args.download"
        @cancel="args.cancel"
        @delete="args.delete"
      />
    `,
  }),
  args: {
    status: 'Draft',
    title: 'Sample Draft Projection',
  },
  parameters: {
    docs: {
      description: {
        story: 'Draft status shows: Edit, Duplicate, Delete',
      },
    },
  },
}

export const Ready: Story = {
  render: (args) => ({
    components: { ProjectionActionsMenu },
    setup() {
      return { args }
    },
    template: `
      <ProjectionActionsMenu
        v-bind="args"
        @view="args.view"
        @edit="args.edit"
        @duplicate="args.duplicate"
        @download="args.download"
        @cancel="args.cancel"
        @delete="args.delete"
      />
    `,
  }),
  args: {
    status: 'Ready',
    title: 'Sample Ready Projection',
  },
  parameters: {
    docs: {
      description: {
        story: 'Ready status shows: View, Duplicate, Download, Delete',
      },
    },
  },
}

export const Running: Story = {
  render: (args) => ({
    components: { ProjectionActionsMenu },
    setup() {
      return { args }
    },
    template: `
      <ProjectionActionsMenu
        v-bind="args"
        @view="args.view"
        @edit="args.edit"
        @duplicate="args.duplicate"
        @download="args.download"
        @cancel="args.cancel"
        @delete="args.delete"
      />
    `,
  }),
  args: {
    status: 'Running',
    title: 'Sample Running Projection',
  },
  parameters: {
    docs: {
      description: {
        story: 'Running status shows: Cancel, Delete',
      },
    },
  },
}

export const Failed: Story = {
  render: (args) => ({
    components: { ProjectionActionsMenu },
    setup() {
      return { args }
    },
    template: `
      <ProjectionActionsMenu
        v-bind="args"
        @view="args.view"
        @edit="args.edit"
        @duplicate="args.duplicate"
        @download="args.download"
        @cancel="args.cancel"
        @delete="args.delete"
      />
    `,
  }),
  args: {
    status: 'Failed',
    title: 'Sample Failed Projection',
  },
  parameters: {
    docs: {
      description: {
        story: 'Failed status shows: Edit, Duplicate, Download, Delete',
      },
    },
  },
}
