import type { Meta, StoryObj } from '@storybook/vue3-vite'
import { ref } from 'vue'
import ProjectionCardList from './ProjectionCardList.vue'
import type { Projection, SortOption } from '@/interfaces/interfaces'

const sampleProjections: Projection[] = [
  {
    projectionGUID: '63c26de0-f6f3-42c2-bbb2-3c2b1e60d033',
    title: 'Forest Yield Analysis 2026',
    description: 'Annual projection for coastal forest regions with detailed volume calculations',
    method: 'File Upload',
    projectionType: 'Volume',
    lastUpdated: '2026-01-10T14:30:00',
    expiration: '2026-01-15',
    status: 'Running',
  },
  {
    projectionGUID: '63c26de0-f6f3-42c2-ccc2-3c2b1e60d033',
    title: 'Interior Region Draft',
    description: 'Draft projection for interior region analysis',
    method: 'Manual Input',
    projectionType: 'CFS Biomass',
    lastUpdated: '2026-01-09T09:15:00',
    expiration: '2026-01-14',
    status: 'Draft',
  },
  {
    projectionGUID: '63c26de0-f6f3-42c2-ddd2-3c2b1e60d033',
    title: 'Northern Zone Study',
    description: 'Completed analysis for northern forest zones',
    method: 'File Upload',
    projectionType: 'Volume',
    lastUpdated: '2026-01-08T16:45:00',
    expiration: '2026-01-13',
    status: 'Ready',
  },
  {
    projectionGUID: '63c26de0-f6f3-42c2-sss2-3c2b1e60d033',
    title: 'Failed Import Test',
    description: 'This projection encountered an error during processing',
    method: 'File Upload',
    projectionType: 'Volume',
    lastUpdated: '2026-01-07T11:20:00',
    expiration: '2026-01-12',
    status: 'Failed',
  },
]

const sortOptions: SortOption[] = [
  { title: 'Projection Title (A-Z)', value: 'title-asc' },
  { title: 'Projection Title (Z-A)', value: 'title-desc' },
  { title: 'Last Updated (Newest)', value: 'lastUpdated-desc' },
  { title: 'Last Updated (Oldest)', value: 'lastUpdated-asc' },
  { title: 'Method (A-Z)', value: 'method-asc' },
  { title: 'Method (Z-A)', value: 'method-desc' },
  { title: 'Status (A-Z)', value: 'status-asc' },
  { title: 'Status (Z-A)', value: 'status-desc' },
]

const meta: Meta<typeof ProjectionCardList> = {
  title: 'components/projection-list/ProjectionCardList',
  component: ProjectionCardList,
  tags: ['autodocs'],
  argTypes: {
    projections: {
      control: { type: 'object' },
      description: 'Array of projection objects to display as cards.',
    },
    sortOptions: {
      control: { type: 'object' },
      description: 'Array of sort options for the dropdown.',
    },
    sortValue: {
      control: { type: 'select' },
      options: sortOptions.map((opt) => opt.value),
      description: 'Currently selected sort option value.',
    },
  },
  parameters: {
    docs: {
      description: {
        component: `
A mobile-friendly card list component for displaying projections. This component is used in the ProjectionListView when the screen width is 1025px or below.

**Features:**
- Sort dropdown to change the order of projections
- Cards display projection details including title, status, method, range type, expiration, and description
- Action buttons vary based on projection status:
  - **Draft**: Edit, Duplicate, Delete
  - **Ready**: View, Duplicate, Download, Delete
  - **Running**: Cancel
  - **Failed**: Edit, Duplicate, Download, Delete

**Responsive behavior:**
- Description moves to a separate row on screens narrower than 700px
        `,
      },
    },
  },
}

export default meta

type Story = StoryObj<typeof ProjectionCardList>

export const Default: Story = {
  render: (args) => ({
    components: { ProjectionCardList },
    setup() {
      const currentSortValue = ref(args.sortValue)
      const handleSort = (value: string) => {
        currentSortValue.value = value
      }
      return { args, currentSortValue, handleSort }
    },
    template: `
      <ProjectionCardList
        :projections="args.projections"
        :sort-options="args.sortOptions"
        :sort-value="currentSortValue"
        @sort="handleSort"
        @view="args.view"
        @edit="args.edit"
        @duplicate="args.duplicate"
        @download="args.download"
        @cancel="args.cancel"
        @delete="args.delete"
        @row-click="args.rowClick"
      />
    `,
  }),
  args: {
    projections: sampleProjections,
    sortOptions: sortOptions,
    sortValue: 'lastUpdated-desc',
  },
  parameters: {
    docs: {
      description: {
        story: 'Default view showing multiple projections with different statuses.',
      },
    },
  },
}

export const SingleDraftCard: Story = {
  render: (args) => ({
    components: { ProjectionCardList },
    setup() {
      const currentSortValue = ref(args.sortValue)
      const handleSort = (value: string) => {
        currentSortValue.value = value
      }
      return { args, currentSortValue, handleSort }
    },
    template: `
      <ProjectionCardList
        :projections="args.projections"
        :sort-options="args.sortOptions"
        :sort-value="currentSortValue"
        @sort="handleSort"
        @view="args.view"
        @edit="args.edit"
        @duplicate="args.duplicate"
        @download="args.download"
        @cancel="args.cancel"
        @delete="args.delete"
        @row-click="args.rowClick"
      />
    `,
  }),
  args: {
    projections: [sampleProjections[1]],
    sortOptions: sortOptions,
    sortValue: 'lastUpdated-desc',
  },
  parameters: {
    docs: {
      description: {
        story: 'Single Draft card showing Edit, Duplicate, and Delete actions.',
      },
    },
  },
}

export const SingleReadyCard: Story = {
  render: (args) => ({
    components: { ProjectionCardList },
    setup() {
      const currentSortValue = ref(args.sortValue)
      const handleSort = (value: string) => {
        currentSortValue.value = value
      }
      return { args, currentSortValue, handleSort }
    },
    template: `
      <ProjectionCardList
        :projections="args.projections"
        :sort-options="args.sortOptions"
        :sort-value="currentSortValue"
        @sort="handleSort"
        @view="args.view"
        @edit="args.edit"
        @duplicate="args.duplicate"
        @download="args.download"
        @cancel="args.cancel"
        @delete="args.delete"
        @row-click="args.rowClick"
      />
    `,
  }),
  args: {
    projections: [sampleProjections[2]],
    sortOptions: sortOptions,
    sortValue: 'lastUpdated-desc',
  },
  parameters: {
    docs: {
      description: {
        story: 'Single Ready card showing View, Duplicate, Download, and Delete actions.',
      },
    },
  },
}

export const SingleRunningCard: Story = {
  render: (args) => ({
    components: { ProjectionCardList },
    setup() {
      const currentSortValue = ref(args.sortValue)
      const handleSort = (value: string) => {
        currentSortValue.value = value
      }
      return { args, currentSortValue, handleSort }
    },
    template: `
      <ProjectionCardList
        :projections="args.projections"
        :sort-options="args.sortOptions"
        :sort-value="currentSortValue"
        @sort="handleSort"
        @view="args.view"
        @edit="args.edit"
        @duplicate="args.duplicate"
        @download="args.download"
        @cancel="args.cancel"
        @delete="args.delete"
        @row-click="args.rowClick"
      />
    `,
  }),
  args: {
    projections: [sampleProjections[0]],
    sortOptions: sortOptions,
    sortValue: 'lastUpdated-desc',
  },
  parameters: {
    docs: {
      description: {
        story: 'Single Running card showing Cancel action only.',
      },
    },
  },
}

export const SingleFailedCard: Story = {
  render: (args) => ({
    components: { ProjectionCardList },
    setup() {
      const currentSortValue = ref(args.sortValue)
      const handleSort = (value: string) => {
        currentSortValue.value = value
      }
      return { args, currentSortValue, handleSort }
    },
    template: `
      <ProjectionCardList
        :projections="args.projections"
        :sort-options="args.sortOptions"
        :sort-value="currentSortValue"
        @sort="handleSort"
        @view="args.view"
        @edit="args.edit"
        @duplicate="args.duplicate"
        @download="args.download"
        @cancel="args.cancel"
        @delete="args.delete"
        @row-click="args.rowClick"
      />
    `,
  }),
  args: {
    projections: [sampleProjections[3]],
    sortOptions: sortOptions,
    sortValue: 'lastUpdated-desc',
  },
  parameters: {
    docs: {
      description: {
        story: 'Single Failed card showing Edit, Duplicate, Download, and Delete actions.',
      },
    },
  },
}

export const LongDescription: Story = {
  render: (args) => ({
    components: { ProjectionCardList },
    setup() {
      const currentSortValue = ref(args.sortValue)
      const handleSort = (value: string) => {
        currentSortValue.value = value
      }
      return { args, currentSortValue, handleSort }
    },
    template: `
      <ProjectionCardList
        :projections="args.projections"
        :sort-options="args.sortOptions"
        :sort-value="currentSortValue"
        @sort="handleSort"
        @view="args.view"
        @edit="args.edit"
        @duplicate="args.duplicate"
        @download="args.download"
        @cancel="args.cancel"
        @delete="args.delete"
        @row-click="args.rowClick"
      />
    `,
  }),
  args: {
    projections: [
      {
        projectionGUID: '63c26de0-f6f3-42c2-zzz2-3c2b1e60d033',
        title: 'Projection with Very Long Description',
        description:
          'Responsive design will ensure that fields are maximizing the information - eg. Projection Title will have a minimum size that would accommodate 20 characters and description should have a minimum size to accommodate 30 characters. This is a very long description to test how the card handles overflow and text wrapping in different screen sizes.',
        method: 'File Upload',
        projectionType: 'Volume',
        lastUpdated: '2026-01-10T14:30:00',
        expiration: '2026-01-15',
        status: 'Ready',
      },
    ],
    sortOptions: sortOptions,
    sortValue: 'lastUpdated-desc',
  },
  parameters: {
    docs: {
      description: {
        story: 'Card with a very long description to demonstrate text wrapping behavior.',
      },
    },
  },
}

export const EmptyList: Story = {
  render: (args) => ({
    components: { ProjectionCardList },
    setup() {
      const currentSortValue = ref(args.sortValue)
      const handleSort = (value: string) => {
        currentSortValue.value = value
      }
      return { args, currentSortValue, handleSort }
    },
    template: `
      <ProjectionCardList
        :projections="args.projections"
        :sort-options="args.sortOptions"
        :sort-value="currentSortValue"
        @sort="handleSort"
        @view="args.view"
        @edit="args.edit"
        @duplicate="args.duplicate"
        @download="args.download"
        @cancel="args.cancel"
        @delete="args.delete"
        @row-click="args.rowClick"
      />
    `,
  }),
  args: {
    projections: [],
    sortOptions: sortOptions,
    sortValue: 'lastUpdated-desc',
  },
  parameters: {
    docs: {
      description: {
        story: 'Empty state showing the sort dropdown and an empty state card with the message "No projections found. Create a new projection to build your history."',
      },
    },
  },
}
