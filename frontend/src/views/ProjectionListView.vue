<template>
  <v-container fluid class="projection-list-container">
    <!-- Header with Title and New Projection Button -->
    <div class="page-header">
      <h1 class="page-heading">Projections</h1>
      <v-menu>
        <template v-slot:activator="{ props }">
          <AppButton
            :activator-props="props"
            label="New Projection"
            mdi-name="mdi-plus"
            variant="primary"
          />
        </template>
        <v-list>
          <v-list-item
            @click="handleNewProjection(NEW_PROJECTION_TYPE.INPUT_MODEL_PARAMETERS)"
          >
            <v-list-item-title>Input Model Parameters</v-list-item-title>
          </v-list-item>
          <v-list-item
            @click="handleNewProjection(NEW_PROJECTION_TYPE.FILE_UPLOAD)"
          >
            <v-list-item-title>File Upload</v-list-item-title>
          </v-list-item>
        </v-list>
      </v-menu>
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
      @rowClick="handleRowClick"
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
      @rowClick="handleRowClick"
    />

    <!-- Pagination -->
    <ProjectionPagination
      v-model:current-page="currentPage"
      v-model:items-per-page="itemsPerPage"
      :total-items="projections.length"
      :items-per-page-options="itemsPerPageOptions"
    />

    <!-- Progress Indicator -->
    <AppProgressCircular
      :is-show="isProgressVisible"
      :message="progressMessage"
    />
  </v-container>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import type { Projection, TableHeader, SortOption } from '@/interfaces/interfaces'
import type { SortOrder } from '@/types/types'
import { itemsPerPageOptions as defaultItemsPerPageOptions } from '@/constants/options'
import { PROJECTION_LIST_HEADER_KEY, SORT_ORDER, BREAKPOINT, PAGINATION, MODEL_SELECTION, PROJECTION_VIEW_MODE, PROJECTION_STATUS, NEW_PROJECTION_TYPE, ROUTE_PATH } from '@/constants/constants'
import { PROGRESS_MSG, SUCCESS_MSG, PROJECTION_ERR } from '@/constants/message'
import { AppButton, AppProgressCircular } from '@/components'
import { ProjectionTable, ProjectionCardList, ProjectionPagination } from '@/components/projection'
import {
  fetchUserProjections,
  deleteProjectionWithFiles,
  getProjectionById,
  getFileSetFiles,
  parseProjectionParams,
  isProjectionReadOnly,
  mapProjectionStatus,
} from '@/services/projectionService'
import { useAppStore } from '@/stores/projection/appStore'
import { useModelParameterStore } from '@/stores/projection/modelParameterStore'
import { useFileUploadStore } from '@/stores/projection/fileUploadStore'
import { useAlertDialogStore } from '@/stores/common/alertDialogStore'
import { useNotificationStore } from '@/stores/common/notificationStore'
import {
  ExecutionOptionsEnum,
  type ModelParameters,
} from '@/services/vdyp-api'

const router = useRouter()
const appStore = useAppStore()
const modelParameterStore = useModelParameterStore()
const fileUploadStore = useFileUploadStore()
const alertDialogStore = useAlertDialogStore()
const notificationStore = useNotificationStore()

// Projections data from API
const projections = ref<Projection[]>([])
const isLoading = ref(false)
const error = ref<string | null>(null)

// Progress state
const isProgressVisible = ref(false)
const progressMessage = ref('')

// Load projections from API
const loadProjections = async () => {
  isLoading.value = true
  error.value = null
  try {
    projections.value = await fetchUserProjections()
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

/**
 * Loads a projection and navigates to the detail view
 * @param projectionGUID The projection GUID to load
 * @param isViewMode If true, opens in view (read-only) mode; otherwise in edit mode
 */
const loadAndNavigateToProjection = async (projectionGUID: string, isViewMode: boolean) => {
  isProgressVisible.value = true
  progressMessage.value = PROGRESS_MSG.LOADING_PROJECTION

  try {
    // Fetch the projection data
    const projectionModel = await getProjectionById(projectionGUID)
    const params = parseProjectionParams(projectionModel.projectionParameters)

    // Determine the method (File Upload or Input Model Parameters)
    const isInputModelParams = params.selectedExecutionOptions.includes(ExecutionOptionsEnum.DoEnableProjectionReport)
    const method = isInputModelParams
      ? MODEL_SELECTION.INPUT_MODEL_PARAMETERS
      : MODEL_SELECTION.FILE_UPLOAD

    // Set the app store state
    appStore.setModelSelection(method)
    appStore.setViewMode(isViewMode ? PROJECTION_VIEW_MODE.VIEW : PROJECTION_VIEW_MODE.EDIT)
    appStore.setCurrentProjectionGUID(projectionGUID)
    appStore.setCurrentProjectionStatus(
      mapProjectionStatus(projectionModel.projectionStatusCode?.code || PROJECTION_STATUS.DRAFT),
    )

    if (isInputModelParams) {
      // Reset model parameter store
      modelParameterStore.resetStore()

      // First, restore species/site/stand info to populate speciesGroups
      if (projectionModel.modelParameters) {
        try {
          const modelParams: ModelParameters = JSON.parse(projectionModel.modelParameters)
          modelParameterStore.restoreFromModelParameters(modelParams)
        } catch (err) {
          console.error('Error parsing modelParameters:', err)
        }
      }

      // Then, restore report settings and utilization levels (speciesGroups is now populated)
      modelParameterStore.restoreFromProjectionParams(params, isViewMode)
    } else {
      // Reset and restore file upload store
      fileUploadStore.resetStore()
      fileUploadStore.restoreFromProjectionParams(params, isViewMode)

      // Set file references for display (file names only, not actual files)
      await loadFileReferencesForFileUpload(
        projectionGUID,
        projectionModel.polygonFileSet?.projectionFileSetGUID,
        projectionModel.layerFileSet?.projectionFileSetGUID,
      )
    }

    // Navigate to the projection detail view
    router.push(ROUTE_PATH.PROJECTION_DETAIL)
  } catch (err) {
    console.error('Error loading projection:', err)
    notificationStore.showErrorMessage(PROJECTION_ERR.LOAD_FAILED, 'Load failed')
  } finally {
    isProgressVisible.value = false
  }
}

/**
 * Loads file references for File Upload mode (names only, not actual files)
 * Since FileMappingModel doesn't have fileName, we use the fileSet name
 */
const loadFileReferencesForFileUpload = async (
  projectionGUID: string,
  polygonFileSetGUID: string | undefined,
  layerFileSetGUID: string | undefined,
) => {
  try {
    let polygonFileName: string | null = null
    let layerFileName: string | null = null

    if (polygonFileSetGUID) {
      const polygonFiles = await getFileSetFiles(projectionGUID, polygonFileSetGUID)
      if (polygonFiles.length > 0) {
        // Use fileSet name if available, otherwise use a default
        polygonFileName = polygonFiles[0].projectionFileSet?.fileSetName || 'Polygon File'
      }
    }

    if (layerFileSetGUID) {
      const layerFiles = await getFileSetFiles(projectionGUID, layerFileSetGUID)
      if (layerFiles.length > 0) {
        // Use fileSet name if available, otherwise use a default
        layerFileName = layerFiles[0].projectionFileSet?.fileSetName || 'Layer File'
      }
    }

    fileUploadStore.setFileReferences(polygonFileName, layerFileName)
  } catch (err) {
    console.error('Error loading file references:', err)
  }
}

// Action handlers
const handleView = async (projectionGUID: string) => {
  console.log('View projection:', projectionGUID)
  await loadAndNavigateToProjection(projectionGUID, true)
}

const handleEdit = async (projectionGUID: string) => {
  console.log('Edit projection:', projectionGUID)
  // Find the projection to check its status
  const projection = projections.value.find(p => p.projectionGUID === projectionGUID)
  if (projection && isProjectionReadOnly(projection.status)) {
    // If status is Ready or Running, open in view mode instead
    notificationStore.showWarningMessage('This projection is read-only and cannot be edited.', 'Read-only projection')
    await loadAndNavigateToProjection(projectionGUID, true)
  } else {
    await loadAndNavigateToProjection(projectionGUID, false)
  }
}

const handleDuplicate = (projectionGUID: string) => {
  console.log('Duplicate projection:', projectionGUID)
}

const handleDownload = (projectionGUID: string) => {
  console.log('Download projection:', projectionGUID)
}

const handleDelete = async (projectionGUID: string) => {
  const confirmed = await alertDialogStore.openDialog(
    'Confirmation',
    'Are you sure you want to delete this Projection? Once deleted, it will not be recoverable and will be removed forever.',
    { variant: 'confirmation' },
  )

  if (confirmed) {
    isProgressVisible.value = true
    progressMessage.value = PROGRESS_MSG.DELETING_PROJECTION
    try {
      await deleteProjectionWithFiles(projectionGUID)
      notificationStore.showSuccessMessage(SUCCESS_MSG.PROJECTION_DELETED, 'Projection deleted')
      // Refresh the projections list
      await loadProjections()
    } catch (err) {
      console.error('Error deleting projection:', err)
      notificationStore.showErrorMessage(PROJECTION_ERR.DELETE_FAILED, 'Delete failed')
    } finally {
      isProgressVisible.value = false
    }
  }
}

const handleCancel = (projectionGUID: string) => {
  console.log('Cancel projection:', projectionGUID)
}

/**
 * Handles row/card click - opens projection in view or edit mode based on status
 * @param projection The projection that was clicked
 */
const handleRowClick = async (projection: Projection) => {
  // If status is Ready or Running, open in view mode; otherwise open in edit mode
  const isViewMode = isProjectionReadOnly(projection.status)
  await loadAndNavigateToProjection(projection.projectionGUID, isViewMode)
}

const handleNewProjection = (type: (typeof NEW_PROJECTION_TYPE)[keyof typeof NEW_PROJECTION_TYPE]) => {
  // Reset app store for new projection
  appStore.resetForNewProjection()

  if (type === NEW_PROJECTION_TYPE.INPUT_MODEL_PARAMETERS) {
    appStore.setModelSelection(MODEL_SELECTION.INPUT_MODEL_PARAMETERS)
    // Reset model parameter store
    modelParameterStore.resetStore()
  } else {
    appStore.setModelSelection(MODEL_SELECTION.FILE_UPLOAD)
    // Reset file upload store
    fileUploadStore.resetStore()
  }
  router.push(ROUTE_PATH.PROJECTION_DETAIL)
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
