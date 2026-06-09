import type { Meta, StoryObj } from '@storybook/vue3-vite'
import { ref } from 'vue'
import ProjectionPagination from './ProjectionPagination.vue'

const meta: Meta<typeof ProjectionPagination> = {
  title: 'components/projection/list/ProjectionPagination',
  component: ProjectionPagination,
  tags: ['autodocs'],
  argTypes: {
    currentPage: {
      control: { type: 'number', min: 1 },
      description: 'The current active page number.',
    },
    itemsPerPage: {
      control: { type: 'select' },
      options: [10, 20, 50, 100],
      description: 'Number of items displayed per page.',
    },
    totalItems: {
      control: { type: 'number', min: 0 },
      description: 'Total number of items across all pages.',
    },
    itemsPerPageOptions: {
      control: { type: 'object' },
      description: 'Array of options for items per page dropdown.',
    },
  }
}

export default meta

type Story = StoryObj<typeof ProjectionPagination>

export const Default: Story = {
  render: (args) => ({
    components: { ProjectionPagination },
    setup() {
      const currentPage = ref(args.currentPage)
      const itemsPerPage = ref(args.itemsPerPage)

      const handlePageChange = (page: number) => {
        currentPage.value = page
      }

      const handleItemsPerPageChange = (value: number) => {
        itemsPerPage.value = value
        currentPage.value = 1
      }

      return { args, currentPage, itemsPerPage, handlePageChange, handleItemsPerPageChange }
    },
    template: `
      <ProjectionPagination
        :current-page="currentPage"
        :items-per-page="itemsPerPage"
        :total-items="args.totalItems"
        :items-per-page-options="args.itemsPerPageOptions"
        @update:current-page="handlePageChange"
        @update:items-per-page="handleItemsPerPageChange"
      />
    `,
  }),
  args: {
    currentPage: 1,
    itemsPerPage: 10,
    totalItems: 35,
    itemsPerPageOptions: [10, 20, 50, 100],
  }
}

export const SinglePage: Story = {
  render: (args) => ({
    components: { ProjectionPagination },
    setup() {
      const currentPage = ref(args.currentPage)
      const itemsPerPage = ref(args.itemsPerPage)

      const handlePageChange = (page: number) => {
        currentPage.value = page
      }

      const handleItemsPerPageChange = (value: number) => {
        itemsPerPage.value = value
        currentPage.value = 1
      }

      return { args, currentPage, itemsPerPage, handlePageChange, handleItemsPerPageChange }
    },
    template: `
      <ProjectionPagination
        :current-page="currentPage"
        :items-per-page="itemsPerPage"
        :total-items="args.totalItems"
        :items-per-page-options="args.itemsPerPageOptions"
        @update:current-page="handlePageChange"
        @update:items-per-page="handleItemsPerPageChange"
      />
    `,
  }),
  args: {
    currentPage: 1,
    itemsPerPage: 10,
    totalItems: 5,
    itemsPerPageOptions: [10, 20, 50, 100],
  }
}

// totalItems = 0  ->  totalPages = 0, both arrows disabled, no page buttons
export const EmptyState: Story = {
  render: (args) => ({
    components: { ProjectionPagination },
    setup() {
      const currentPage = ref(args.currentPage)
      const itemsPerPage = ref(args.itemsPerPage)

      const handlePageChange = (page: number) => {
        currentPage.value = page
      }

      const handleItemsPerPageChange = (value: number) => {
        itemsPerPage.value = value
        currentPage.value = 1
      }

      return { args, currentPage, itemsPerPage, handlePageChange, handleItemsPerPageChange }
    },
    template: `
      <ProjectionPagination
        :current-page="currentPage"
        :items-per-page="itemsPerPage"
        :total-items="args.totalItems"
        :items-per-page-options="args.itemsPerPageOptions"
        @update:current-page="handlePageChange"
        @update:items-per-page="handleItemsPerPageChange"
      />
    `,
  }),
  args: {
    currentPage: 1,
    itemsPerPage: 10,
    totalItems: 0,
    itemsPerPageOptions: [10, 20, 50, 100],
  }
}
