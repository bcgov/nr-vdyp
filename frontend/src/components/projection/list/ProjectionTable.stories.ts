import type { Meta, StoryObj } from '@storybook/vue3-vite'
import { ref } from 'vue'
import ProjectionTable from './ProjectionTable.vue'
import type { Projection, TableHeader } from '@/interfaces/interfaces'

const sampleProjections: Projection[] = [
  {
    projectionGUID: '63c26de0-f6f3-42c2-kkk2-3c2b1e60d033',
    title: 'Forest Yield Analysis 2026',
    description: 'Annual projection for coastal forest regions with detailed volume calculations',
    method: 'File Upload',
    projectionType: 'Volume',
    lastUpdated: '2026-01-10T14:30:00',
    expiration: '2026-01-15',
    status: 'Running',
  },
  {
    projectionGUID: '63c26de0-f6f3-42c2-bbb2-3c2b1e60d033',
    title: 'Interior Region Draft',
    description: 'Draft projection for interior region analysis',
    method: 'Manual Input',
    projectionType: 'CFS Biomass',
    lastUpdated: '2026-01-09T09:15:00',
    expiration: '2026-01-14',
    status: 'Draft',
  },
  {
    projectionGUID: '63c26de0-f6f3-42c2-ccc2-3c2b1e60d033',
    title: 'Northern Zone Study',
    description: 'Completed analysis for northern forest zones',
    method: 'File Upload',
    projectionType: 'Volume',
    lastUpdated: '2026-01-08T16:45:00',
    expiration: '2026-01-13',
    status: 'Ready',
  },
  {
    projectionGUID: '63c26de0-f6f3-42c2-czz2-3c2b1e60d033',
    title: 'Failed Import Test',
    description: 'This projection encountered an error during processing',
    method: 'File Upload',
    projectionType: 'Volume',
    lastUpdated: '2026-01-07T11:20:00',
    expiration: '2026-01-12',
    status: 'Failed',
  },
]

const tableHeaders: TableHeader[] = [
  { key: 'title', title: 'Projection Title', sortable: true },
  { key: 'description', title: 'Description', sortable: false },
  { key: 'method', title: 'Method', sortable: true },
  { key: 'projectionType', title: 'Projection Type', sortable: true },
  { key: 'lastUpdated', title: 'Last Updated', sortable: true },
  { key: 'expiration', title: 'Expiration', sortable: true },
  { key: 'status', title: 'Status', sortable: true },
]

const meta: Meta<typeof ProjectionTable> = {
  title: 'components/projection-list/ProjectionTable',
  component: ProjectionTable,
  tags: ['autodocs'],
  argTypes: {
    projections: {
      control: { type: 'object' },
      description: 'Array of projection objects to display in the table.',
    },
    headers: {
      control: { type: 'object' },
      description: 'Array of table header configurations.',
    },
    sortBy: {
      control: { type: 'select' },
      options: ['title', 'method', 'projectionType', 'lastUpdated', 'expiration', 'status'],
      description: 'Current column being sorted.',
    },
    sortOrder: {
      control: { type: 'radio' },
      options: ['asc', 'desc'],
      description: 'Current sort order (ascending or descending).',
    },
  },
  parameters: {
    docs: {
      description: {
        component: `
A desktop table component for displaying projections. This component is used in the ProjectionListView when the screen width is above 1025px.

**Features:**
- Sortable columns with visual indicators
- Hover tooltips showing full content for truncated cells
- Status icons for each projection state
- Action menu for each row with status-dependent options:
  - **Draft**: Edit, Duplicate, Delete
  - **Ready**: View, Duplicate, Download, Delete
  - **Running**: Cancel, Delete
  - **Failed**: Edit, Duplicate, Download, Delete

**Column widths are fixed:**
- Projection Title: 180px
- Description: 270px
- Method: 160px
- Projection Type: 130px
- Last Updated: 130px
- Expiration: 100px
- Status: 110px
- Actions: 50px
        `,
      },
    },
  },
}

export default meta

type Story = StoryObj<typeof ProjectionTable>

export const Default: Story = {
  render: (args) => ({
    components: { ProjectionTable },
    setup() {
      const currentSortBy = ref(args.sortBy)
      const currentSortOrder = ref<'asc' | 'desc'>(args.sortOrder)

      const handleSort = (key: string) => {
        if (currentSortBy.value === key) {
          currentSortOrder.value = currentSortOrder.value === 'asc' ? 'desc' : 'asc'
        } else {
          currentSortBy.value = key
          currentSortOrder.value = 'asc'
        }
      }

      return { args, currentSortBy, currentSortOrder, handleSort }
    },
    template: `
      <ProjectionTable
        :projections="args.projections"
        :headers="args.headers"
        :sort-by="currentSortBy"
        :sort-order="currentSortOrder"
        @sort="handleSort"
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
    projections: sampleProjections,
    headers: tableHeaders,
    sortBy: 'lastUpdated',
    sortOrder: 'desc',
  },
  parameters: {
    docs: {
      description: {
        story: 'Default table view showing multiple projections with different statuses.',
      },
    },
  },
}

export const SortedByTitle: Story = {
  render: (args) => ({
    components: { ProjectionTable },
    setup() {
      const currentSortBy = ref(args.sortBy)
      const currentSortOrder = ref<'asc' | 'desc'>(args.sortOrder)

      const handleSort = (key: string) => {
        if (currentSortBy.value === key) {
          currentSortOrder.value = currentSortOrder.value === 'asc' ? 'desc' : 'asc'
        } else {
          currentSortBy.value = key
          currentSortOrder.value = 'asc'
        }
      }

      return { args, currentSortBy, currentSortOrder, handleSort }
    },
    template: `
      <ProjectionTable
        :projections="args.projections"
        :headers="args.headers"
        :sort-by="currentSortBy"
        :sort-order="currentSortOrder"
        @sort="handleSort"
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
    projections: sampleProjections,
    headers: tableHeaders,
    sortBy: 'title',
    sortOrder: 'asc',
  },
  parameters: {
    docs: {
      description: {
        story: 'Table sorted by Projection Title in ascending order.',
      },
    },
  },
}

export const SingleDraftRow: Story = {
  render: (args) => ({
    components: { ProjectionTable },
    setup() {
      return { args }
    },
    template: `
      <ProjectionTable
        :projections="args.projections"
        :headers="args.headers"
        :sort-by="args.sortBy"
        :sort-order="args.sortOrder"
        @sort="args.sort"
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
    projections: [sampleProjections[1]],
    headers: tableHeaders,
    sortBy: 'lastUpdated',
    sortOrder: 'desc',
  },
  parameters: {
    docs: {
      description: {
        story: 'Single Draft row showing Edit, Duplicate, and Delete actions in the menu.',
      },
    },
  },
}

export const SingleReadyRow: Story = {
  render: (args) => ({
    components: { ProjectionTable },
    setup() {
      return { args }
    },
    template: `
      <ProjectionTable
        :projections="args.projections"
        :headers="args.headers"
        :sort-by="args.sortBy"
        :sort-order="args.sortOrder"
        @sort="args.sort"
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
    projections: [sampleProjections[2]],
    headers: tableHeaders,
    sortBy: 'lastUpdated',
    sortOrder: 'desc',
  },
  parameters: {
    docs: {
      description: {
        story: 'Single Ready row showing View, Duplicate, Download, and Delete actions.',
      },
    },
  },
}

export const SingleRunningRow: Story = {
  render: (args) => ({
    components: { ProjectionTable },
    setup() {
      return { args }
    },
    template: `
      <ProjectionTable
        :projections="args.projections"
        :headers="args.headers"
        :sort-by="args.sortBy"
        :sort-order="args.sortOrder"
        @sort="args.sort"
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
    projections: [sampleProjections[0]],
    headers: tableHeaders,
    sortBy: 'lastUpdated',
    sortOrder: 'desc',
  },
  parameters: {
    docs: {
      description: {
        story: 'Single Running row showing Cancel and Delete actions.',
      },
    },
  },
}

export const SingleFailedRow: Story = {
  render: (args) => ({
    components: { ProjectionTable },
    setup() {
      return { args }
    },
    template: `
      <ProjectionTable
        :projections="args.projections"
        :headers="args.headers"
        :sort-by="args.sortBy"
        :sort-order="args.sortOrder"
        @sort="args.sort"
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
    projections: [sampleProjections[3]],
    headers: tableHeaders,
    sortBy: 'lastUpdated',
    sortOrder: 'desc',
  },
  parameters: {
    docs: {
      description: {
        story: 'Single Failed row showing Edit, Duplicate, Download, and Delete actions.',
      },
    },
  },
}

export const LongContent: Story = {
  render: (args) => ({
    components: { ProjectionTable },
    setup() {
      return { args }
    },
    template: `
      <ProjectionTable
        :projections="args.projections"
        :headers="args.headers"
        :sort-by="args.sortBy"
        :sort-order="args.sortOrder"
        @sort="args.sort"
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
    projections: [
      {
        projectionGUID: '63c26de0-f6f3-42c2-adf2-3c2b1e60d033',
        title: 'Very Long Projection Title That Exceeds The Column Width',
        description:
          'Responsive design will ensure that fields are maximizing the information - eg. Projection Title will have a minimum size that would accommodate 20 characters and description should have a minimum size to accommodate 30 characters. This is a very long description to test how the table handles overflow and text truncation.',
        method: 'Manual Input',
        projectionType: 'CFS Biomass',
        lastUpdated: '2026-01-10T14:30:00',
        expiration: '2026-01-15',
        status: 'Ready',
      },
    ],
    headers: tableHeaders,
    sortBy: 'lastUpdated',
    sortOrder: 'desc',
  },
  parameters: {
    docs: {
      description: {
        story:
          'Row with very long title and description to demonstrate text truncation with ellipsis and tooltip on hover.',
      },
    },
  },
}

export const ManyRows: Story = {
  render: (args) => ({
    components: { ProjectionTable },
    setup() {
      const currentSortBy = ref(args.sortBy)
      const currentSortOrder = ref<'asc' | 'desc'>(args.sortOrder)

      const handleSort = (key: string) => {
        if (currentSortBy.value === key) {
          currentSortOrder.value = currentSortOrder.value === 'asc' ? 'desc' : 'asc'
        } else {
          currentSortBy.value = key
          currentSortOrder.value = 'asc'
        }
      }

      return { args, currentSortBy, currentSortOrder, handleSort }
    },
    template: `
      <ProjectionTable
        :projections="args.projections"
        :headers="args.headers"
        :sort-by="currentSortBy"
        :sort-order="currentSortOrder"
        @sort="handleSort"
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
    projections: [
      ...sampleProjections,
      {
        projectionGUID: '63c26de0-f6f3-42c2-eee2-3c2b1e60d033',
        title: 'Coastal Region Analysis',
        description: 'Detailed study of coastal forest regions',
        method: 'File Upload',
        projectionType: 'Volume',
        lastUpdated: '2026-01-06T10:00:00',
        expiration: '2026-01-11',
        status: 'Ready',
      },
      {
        projectionGUID: '63c26de0-f6f3-42c2-grr2-3c2b1e60d033',
        title: 'Mountain Zone Draft',
        description: 'Initial draft for mountain zone projections',
        method: 'Manual Input',
        projectionType: 'CFS Biomass',
        lastUpdated: '2026-01-05T08:30:00',
        expiration: '2026-01-10',
        status: 'Draft',
      },
      {
        projectionGUID: '63c26de0-f6f3-42c2-dec2-3c2b1e60d033',
        title: 'Valley Floor Study',
        description: 'Valley floor ecosystem analysis',
        method: 'File Upload',
        projectionType: 'Volume',
        lastUpdated: '2026-01-04T15:20:00',
        expiration: '2026-01-09',
        status: 'Running',
      },
      {
        projectionGUID: '63c26de0-f6f3-42c2-zex2-3c2b1e60d033',
        title: 'Wetland Area Projection',
        description: 'Wetland area yield projection analysis',
        method: 'File Upload',
        projectionType: 'Volume',
        lastUpdated: '2026-01-03T12:45:00',
        expiration: '2026-01-08',
        status: 'Failed',
      },
    ],
    headers: tableHeaders,
    sortBy: 'lastUpdated',
    sortOrder: 'desc',
  },
  parameters: {
    docs: {
      description: {
        story: 'Table with many rows showing alternating row backgrounds.',
      },
    },
  },
}

export const EmptyTable: Story = {
  render: (args) => ({
    components: { ProjectionTable },
    setup() {
      return { args }
    },
    template: `
      <ProjectionTable
        :projections="args.projections"
        :headers="args.headers"
        :sort-by="args.sortBy"
        :sort-order="args.sortOrder"
        @sort="args.sort"
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
    projections: [],
    headers: tableHeaders,
    sortBy: 'lastUpdated',
    sortOrder: 'desc',
  },
  parameters: {
    docs: {
      description: {
        story: 'Empty table showing only headers with no data rows.',
      },
    },
  },
}
