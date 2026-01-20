<template>
  <v-container fluid class="projection-list-container">
    <!-- Header with Title and New Projection Button -->
    <div class="page-header">
      <h1 class="page-heading">Projections</h1>
    </div>

    <!-- Desktop/Tablet Table View (above 1025px) -->
    <ProjectionTable
      v-if="!isCardView"
      :projections="paginatedProjections"
      :headers="headers"
      :sort-by="sortBy"
      :sort-order="sortOrder"
      @sort="sortColumn"
      @view="handleView"
      @edit="handleEdit"
      @duplicate="handleDuplicate"
      @download="handleDownload"
      @cancel="handleCancel"
      @delete="handleDelete"
    />

    <!-- Mobile Card View (1025px and below) -->
    <ProjectionCardList
      v-else
      :projections="paginatedProjections"
      :sort-options="sortOptions"
      :sort-value="cardSortBy"
      @sort="handleCardSort"
      @view="handleView"
      @edit="handleEdit"
      @duplicate="handleDuplicate"
      @download="handleDownload"
      @cancel="handleCancel"
      @delete="handleDelete"
    />

    <!-- Pagination -->
    <ProjectionPagination
      v-model:current-page="currentPage"
      v-model:items-per-page="itemsPerPage"
      :total-items="projections.length"
      :items-per-page-options="itemsPerPageOptions"
    />
  </v-container>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import type { Projection, TableHeader, SortOption } from '@/interfaces/interfaces'
import type { SortOrder } from '@/types/types'
import { itemsPerPageOptions as defaultItemsPerPageOptions } from '@/constants/options'
import { PROJECTION_LIST_HEADER_KEY, SORT_ORDER, BREAKPOINT, PAGINATION } from '@/constants/constants'
import ProjectionTable from '@/components/projection-list/ProjectionTable.vue'
import ProjectionCardList from '@/components/projection-list/ProjectionCardList.vue'
import ProjectionPagination from '@/components/projection-list/ProjectionPagination.vue'
import { fetchUserProjections } from '@/services/projectionListService'

// Projections data from API
const projections = ref<Projection[]>([])
const isLoading = ref(false)
const error = ref<string | null>(null)

// Load projections from API
const loadProjections = async () => {
  isLoading.value = true
  error.value = null
  try {
    projections.value = await fetchUserProjections()
    console.debug('>>>>>>>> loaded Projections!!')
  } catch (err) {
    error.value = 'Failed to load projections'
    console.error('Error loading projections:', err)
  } finally {
    isLoading.value = false
  }
}

// Table headers
const headers: TableHeader[] = [
  { key: PROJECTION_LIST_HEADER_KEY.TITLE, title: 'Projection Title', sortable: true },
  { key: PROJECTION_LIST_HEADER_KEY.DESCRIPTION, title: 'Description', sortable: true },
  { key: PROJECTION_LIST_HEADER_KEY.METHOD, title: 'Method', sortable: true },
  { key: PROJECTION_LIST_HEADER_KEY.PROJECTION_TYPE, title: 'Projection Type', sortable: true },
  { key: PROJECTION_LIST_HEADER_KEY.LAST_UPDATED, title: 'Last Updated', sortable: true },
  { key: PROJECTION_LIST_HEADER_KEY.EXPIRATION, title: 'Expiration', sortable: true },
  { key: PROJECTION_LIST_HEADER_KEY.STATUS, title: 'Status', sortable: true },
]

// Sort options for card view dropdown
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

// State
const sortBy = ref<string>(PROJECTION_LIST_HEADER_KEY.LAST_UPDATED)
const sortOrder = ref<SortOrder>(SORT_ORDER.DESC)
const currentPage = ref<number>(PAGINATION.DEFAULT_PAGE)
const itemsPerPage = ref<number>(PAGINATION.DEFAULT_ITEMS_PER_PAGE)
const itemsPerPageOptions = defaultItemsPerPageOptions
const windowWidth = ref<number>(window.innerWidth)
const cardSortBy = ref<string>(PAGINATION.DEFAULT_CARD_SORT_BY)

// Computed
const isCardView = computed(() => windowWidth.value <= BREAKPOINT.CARD_VIEW)
console.log(`windowWidth: ${windowWidth.value}px`)

const sortedProjections = computed(() => {
  const sorted = [...projections.value].sort((a, b) => {
    const aValue = a[sortBy.value as keyof Projection]
    const bValue = b[sortBy.value as keyof Projection]

    // Handle date sorting
    if (sortBy.value === PROJECTION_LIST_HEADER_KEY.LAST_UPDATED || sortBy.value === PROJECTION_LIST_HEADER_KEY.EXPIRATION) {
      const aDate = new Date(aValue as string).getTime()
      const bDate = new Date(bValue as string).getTime()
      return sortOrder.value === SORT_ORDER.ASC ? aDate - bDate : bDate - aDate
    }

    // Natural sort for strings with numbers
    if (typeof aValue === 'string' && typeof bValue === 'string') {
      return sortOrder.value === SORT_ORDER.ASC
        ? aValue.localeCompare(bValue, undefined, { numeric: true })
        : bValue.localeCompare(aValue, undefined, { numeric: true })
    }

    // Default comparison
    if (aValue < bValue) return sortOrder.value === SORT_ORDER.ASC ? -1 : 1
    if (aValue > bValue) return sortOrder.value === SORT_ORDER.ASC ? 1 : -1
    return 0
  })
  return sorted
})

const paginatedProjections = computed(() => {
  const start = (currentPage.value - 1) * itemsPerPage.value
  const end = start + itemsPerPage.value
  return sortedProjections.value.slice(start, end)
})

const totalPages = computed(() =>
  Math.ceil(projections.value.length / itemsPerPage.value),
)

// Reset currentPage when itemsPerPage changes to ensure valid page
watch(itemsPerPage, () => {
  if (currentPage.value > totalPages.value) {
    currentPage.value = Math.max(1, totalPages.value)
  }
})

// Methods
const sortColumn = (key: string) => {
  if (sortBy.value === key) {
    sortOrder.value = sortOrder.value === SORT_ORDER.ASC ? SORT_ORDER.DESC : SORT_ORDER.ASC
  } else {
    sortBy.value = key
    sortOrder.value = SORT_ORDER.ASC
  }
}

const handleCardSort = (value: string) => {
  cardSortBy.value = value
  const [key, order] = value.split('-')
  sortBy.value = key
  sortOrder.value = order as SortOrder
}

// Action handlers (placeholders for future implementation)
const handleView = (projectionGUID: string) => {
  console.log('View projection:', projectionGUID)
}

const handleEdit = (projectionGUID: string) => {
  console.log('Edit projection:', projectionGUID)
}

const handleDuplicate = (projectionGUID: string) => {
  console.log('Duplicate projection:', projectionGUID)
}

const handleDownload = (projectionGUID: string) => {
  console.log('Download projection:', projectionGUID)
}

const handleDelete = (projectionGUID: string) => {
  console.log('Delete projection:', projectionGUID)
}

const handleCancel = (projectionGUID: string) => {
  console.log('Cancel projection:', projectionGUID)
}

// Window resize handler
const handleResize = () => {
  windowWidth.value = window.innerWidth
}

onMounted(() => {
  window.addEventListener('resize', handleResize)
  loadProjections()
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.projection-list-container {
  padding: var(--layout-padding-medium);
  max-width: 100%;
}

/* Header with Title and Button */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--layout-margin-large);
}

.page-heading {
  font: var(--typography-bold-h4);
  color: var(--typography-color-primary);
  margin: 0;
}
</style>
