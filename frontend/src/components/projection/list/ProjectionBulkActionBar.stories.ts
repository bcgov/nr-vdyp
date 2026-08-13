import type { Meta, StoryObj } from '@storybook/vue3-vite'
import ProjectionBulkActionBar from './ProjectionBulkActionBar.vue'

const meta: Meta<typeof ProjectionBulkActionBar> = {
  title: 'components/projection/list/ProjectionBulkActionBar',
  component: ProjectionBulkActionBar,
  tags: ['autodocs'],
  argTypes: {
    isVisible: {
      control: { type: 'boolean' },
      description: 'Controls whether the bulk action bar is rendered.',
    },
    selectedCount: {
      control: { type: 'number' },
      description: 'Number of currently selected projections.',
    },
    canDownload: {
      control: { type: 'boolean' },
      description: 'Enables the Download action button.',
    },
    canDuplicate: {
      control: { type: 'boolean' },
      description: 'Enables the Duplicate action button.',
    },
    canCancel: {
      control: { type: 'boolean' },
      description: 'Enables the Cancel action button.',
    },
    canDelete: {
      control: { type: 'boolean' },
      description: 'Enables the Delete action button.',
    },
  },
  parameters: {
    docs: {
      description: {
        component: `
A bulk action bar displayed when one or more projections are selected in the list.

**Actions:**
- **Close**: Clears the current selection
- **Download**: Downloads selected projections (disabled when \`canDownload\` is false)
- **Duplicate**: Duplicates selected projections (disabled when \`canDuplicate\` is false)
- **Cancel**: Cancels selected projections (disabled when \`canCancel\` is false)
- **Delete**: Deletes selected projections (disabled when \`canDelete\` is false)
        `,
      },
    },
  },
}

export default meta

type Story = StoryObj<typeof ProjectionBulkActionBar>

export const Default: Story = {
  render: (args) => ({
    components: { ProjectionBulkActionBar },
    setup() {
      return { args }
    },
    template: `
      <ProjectionBulkActionBar
        v-bind="args"
        @close="args.close"
        @download="args.download"
        @duplicate="args.duplicate"
        @cancel="args.cancel"
        @delete="args.delete"
      />
    `,
  }),
  args: {
    isVisible: true,
    selectedCount: 3,
    canDownload: true,
    canDuplicate: true,
    canCancel: true,
    canDelete: true,
  },
  parameters: {
    docs: {
      description: {
        story: 'Default state with 3 items selected and all actions enabled.',
      },
    },
  },
}

export const SingleSelection: Story = {
  render: (args) => ({
    components: { ProjectionBulkActionBar },
    setup() {
      return { args }
    },
    template: `
      <ProjectionBulkActionBar
        v-bind="args"
        @close="args.close"
        @download="args.download"
        @duplicate="args.duplicate"
        @cancel="args.cancel"
        @delete="args.delete"
      />
    `,
  }),
  args: {
    isVisible: true,
    selectedCount: 1,
    canDownload: true,
    canDuplicate: true,
    canCancel: true,
    canDelete: true,
  },
  parameters: {
    docs: {
      description: {
        story: 'Single item selected with all actions enabled.',
      },
    },
  },
}

export const AllActionsDisabled: Story = {
  render: (args) => ({
    components: { ProjectionBulkActionBar },
    setup() {
      return { args }
    },
    template: `
      <ProjectionBulkActionBar
        v-bind="args"
        @close="args.close"
        @download="args.download"
        @duplicate="args.duplicate"
        @cancel="args.cancel"
        @delete="args.delete"
      />
    `,
  }),
  args: {
    isVisible: true,
    selectedCount: 2,
    canDownload: false,
    canDuplicate: false,
    canCancel: false,
    canDelete: false,
  },
  parameters: {
    docs: {
      description: {
        story: 'Download, Duplicate, Cancel, and Delete are all disabled.',
      },
    },
  },
}

export const Hidden: Story = {
  render: (args) => ({
    components: { ProjectionBulkActionBar },
    setup() {
      return { args }
    },
    template: `
      <ProjectionBulkActionBar
        v-bind="args"
        @close="args.close"
        @download="args.download"
        @duplicate="args.duplicate"
        @cancel="args.cancel"
        @delete="args.delete"
      />
    `,
  }),
  args: {
    isVisible: false,
    selectedCount: 0,
    canDownload: false,
    canDuplicate: false,
    canCancel: false,
    canDelete: false,
  },
  parameters: {
    docs: {
      description: {
        story: 'Bar is hidden when isVisible is false (no selection).',
      },
    },
  },
}
