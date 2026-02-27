<template>
  <v-container fluid class="projection-list-container">
    <!-- Header with Title and New Projection Button -->
    <div class="page-header">
      <h1 class="page-heading">Projections</h1>
      <v-menu>
        <template v-slot:activator="{ props }">
          <AppButton
            :activator-props="props"
            :label="newProjectionLabel"
            mdi-name="mdi-plus"
            variant="primary"
          />
        </template>
        <v-list>
          <v-list-item
            class="new-projection-menu-item"
            @click="handleNewProjection(PROJECTION_INPUT_METHOD.INPUT_MODEL_PARAMETERS)"
          >
            <v-list-item-title>Manual Input</v-list-item-title>
          </v-list-item>
          <v-list-item
            class="new-projection-menu-item"
            @click="handleNewProjection(PROJECTION_INPUT_METHOD.FILE_UPLOAD)"
          >
            <v-list-item-title>File Upload</v-list-item-title>
          </v-list-item>
        </v-list>
      </v-menu>
    </div>

    <!-- Bulk Action Bar (visible when checkboxes are selected in table view) -->
    <ProjectionBulkActionBar
      v-if="!isCardView"
      :is-visible="selectedGUIDs.length > 0"
      :selected-count="selectedGUIDs.length"
      :can-download="canBulkDownload"
      :can-cancel="canBulkCancel"
      :can-delete="canBulkDelete"
      @close="clearSelection"
      @download="handleBulkDownload"
      @duplicate="handleBulkDuplicate"
      @cancel="handleBulkCancel"
      @delete="handleBulkDelete"
    />

    <!-- Desktop/Tablet Table View (above 1025px) -->
    <ProjectionTable
      v-if="!isCardView"
      :projections="paginatedProjections"
      :headers="headers"
      :sort-by="sortBy"
      :sort-order="sortOrder"
      :selectedGUIDs="selectedGUIDs"
      @sort="sortColumn"
      @view="handleView"
      @edit="handleEdit"
      @duplicate="handleDuplicate"
      @download="handleDownload"
      @cancel="handleCancel"
      @delete="handleDelete"
      @rowClick="handleRowClick"
      @selectionChange="handleSelectionChange"
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
import JSZip from 'jszip'
import { useRouter } from 'vue-router'
import type { Projection, TableHeader, SortOption } from '@/interfaces/interfaces'
import type { SortOrder } from '@/types/types'
import { itemsPerPageOptions as defaultItemsPerPageOptions } from '@/constants/options'
import { PROJECTION_LIST_HEADER_KEY, SORT_ORDER, BREAKPOINT, PAGINATION, MODEL_SELECTION, PROJECTION_VIEW_MODE, PROJECTION_STATUS, PROJECTION_INPUT_METHOD, ROUTE_PATH } from '@/constants/constants'
import { saveExistingProjectionSession, saveNewProjectionSession } from '@/utils/projectionSession'
import { PROGRESS_MSG, SUCCESS_MSG, PROJECTION_ERR } from '@/constants/message'
import { downloadFile, sanitizeFileName } from '@/utils/util'
import { AppButton, AppProgressCircular } from '@/components'
import { ProjectionTable, ProjectionCardList, ProjectionPagination, ProjectionBulkActionBar } from '@/components/projection'
import {
  fetchUserProjections,
  deleteProjectionWithFiles,
  cancelProjection,
  duplicateProjection,
  getProjectionById,
  transformProjection,
  isProjectionReadOnly,
  mapProjectionStatus,
  streamResultsZip,
} from '@/services/projectionService'
import { useAppStore } from '@/stores/projection/appStore'
import { useModelParameterStore } from '@/stores/projection/modelParameterStore'
import { useFileUploadStore } from '@/stores/projection/fileUploadStore'
import { useAlertDialogStore } from '@/stores/common/alertDialogStore'
import { useNotificationStore } from '@/stores/common/notificationStore'
import { useProjectionLoader } from '@/composables/useProjectionLoader'

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

// Bulk selection state
const selectedGUIDs = ref<string[]>([])

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
const newProjectionLabel = computed(() => windowWidth.value <= 360 ? 'Projection' : 'New Projection')
console.log(`windowWidth: ${windowWidth.value}px`)

// Selected projections (full objects) from the full projections list
const selectedProjections = computed<Projection[]>(() =>
  projections.value.filter(p => selectedGUIDs.value.includes(p.projectionGUID))
)

// Bulk action button enable states (based on current frontend status)
const canBulkDownload = computed(() =>
  selectedProjections.value.some(p => p.status === PROJECTION_STATUS.READY || p.status === PROJECTION_STATUS.FAILED)
)
const canBulkCancel = computed(() =>
  selectedProjections.value.some(p => p.status === PROJECTION_STATUS.RUNNING)
)
const canBulkDelete = computed(() =>
  selectedProjections.value.some(p => p.status !== PROJECTION_STATUS.RUNNING)
)

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

const { loadProjection } = useProjectionLoader()

const clearSelection = () => {
  selectedGUIDs.value = []
}

const handleSelectionChange = (guids: string[]) => {
  selectedGUIDs.value = guids
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
    const viewMode = isViewMode ? PROJECTION_VIEW_MODE.VIEW : PROJECTION_VIEW_MODE.EDIT
    const success = await loadProjection(projectionGUID, viewMode)

    if (success) {
      saveExistingProjectionSession(projectionGUID, viewMode)
      router.push(ROUTE_PATH.PROJECTION_DETAIL)
    } else {
      notificationStore.showErrorMessage(PROJECTION_ERR.LOAD_FAILED, PROJECTION_ERR.LOAD_FAILED_TITLE)
    }
  } catch (err) {
    console.error('Error loading projection:', err)
    notificationStore.showErrorMessage(PROJECTION_ERR.LOAD_FAILED, PROJECTION_ERR.LOAD_FAILED_TITLE)
  } finally {
    isProgressVisible.value = false
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

/**
 * Duplicates a single projection and refreshes the list
 */
const handleDuplicate = async (projectionGUID: string) => {
  const projection = projections.value.find(p => p.projectionGUID === projectionGUID)
  const originalName = projection?.title || 'Projection'

  isProgressVisible.value = true
  progressMessage.value = PROGRESS_MSG.DUPLICATING_PROJECTION
  try {
    await duplicateProjection(projectionGUID)

    notificationStore.showSuccessMessage(
      SUCCESS_MSG.PROJECTION_DUPLICATED(originalName),
      SUCCESS_MSG.PROJECTION_DUPLICATED_TITLE,
    )
    await loadProjections()
  } catch (err) {
    console.error('Error duplicating projection:', err)
    notificationStore.showErrorMessage(PROJECTION_ERR.DUPLICATE_FAILED, PROJECTION_ERR.DUPLICATE_FAILED_TITLE)
  } finally {
    isProgressVisible.value = false
  }
}

const handleDownload = async (projectionGUID: string) => {
  const projection = projections.value.find(p => p.projectionGUID === projectionGUID)
  const reportTitle = projection?.title || 'Projection'
  const zipFileName = sanitizeFileName(`${reportTitle}_All Files`) + '.zip'

  isProgressVisible.value = true
  progressMessage.value = PROGRESS_MSG.DOWNLOADING_PROJECTION

  try {
    // Stream the results zip via backend proxy and download with custom filename
    const { zipBlob } = await streamResultsZip(projectionGUID)
    downloadFile(zipBlob, zipFileName)

    notificationStore.showSuccessMessage(
      SUCCESS_MSG.DOWNLOAD_SUCCESS(zipFileName),
      SUCCESS_MSG.DOWNLOAD_SUCCESS_TITLE,
    )
  } catch (err) {
    console.error('Error downloading projection files:', err)
    notificationStore.showErrorMessage(
      PROJECTION_ERR.DOWNLOAD_FAILED(zipFileName),
      PROJECTION_ERR.DOWNLOAD_FAILED_TITLE,
    )
  } finally {
    isProgressVisible.value = false
  }
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
      notificationStore.showSuccessMessage(SUCCESS_MSG.PROJECTION_DELETED, SUCCESS_MSG.PROJECTION_DELETED_TITLE)
      // Refresh the projections list
      await loadProjections()
    } catch (err) {
      console.error('Error deleting projection:', err)
      notificationStore.showErrorMessage(PROJECTION_ERR.DELETE_FAILED, PROJECTION_ERR.DELETE_FAILED_TITLE)
    } finally {
      isProgressVisible.value = false
    }
  }
}

const updateProjectionInList = (projectionModel: Awaited<ReturnType<typeof getProjectionById>>) => {
  const updated = transformProjection(projectionModel)
  const index = projections.value.findIndex(p => p.projectionGUID === updated.projectionGUID)
  if (index !== -1) {
    projections.value[index] = updated
  }
}

const handleCancel = async (projectionGUID: string) => {
  isProgressVisible.value = true
  progressMessage.value = PROGRESS_MSG.CANCELLING_PROJECTION
  try {
    // Pre-check: fetch the latest projection status from the backend
    const latestProjection = await getProjectionById(projectionGUID)
    const latestStatus = mapProjectionStatus(latestProjection.projectionStatusCode?.code || PROJECTION_STATUS.DRAFT)

    if (latestStatus !== PROJECTION_STATUS.RUNNING) {
      // Projection is no longer running - update the single item and show appropriate message
      updateProjectionInList(latestProjection)

      if (latestStatus === PROJECTION_STATUS.READY) {
        notificationStore.showInfoMessage(PROJECTION_ERR.CANCEL_ALREADY_COMPLETED, PROJECTION_ERR.CANCEL_ALREADY_COMPLETED_TITLE)
      } else if (latestStatus === PROJECTION_STATUS.FAILED) {
        notificationStore.showWarningMessage(PROJECTION_ERR.CANCEL_ALREADY_FAILED, PROJECTION_ERR.CANCEL_ALREADY_FAILED_TITLE)
      } else {
        notificationStore.showInfoMessage(PROJECTION_ERR.CANCEL_NOT_RUNNING, PROJECTION_ERR.CANCEL_NOT_RUNNING_TITLE)
      }
      return
    }

    const cancelledProjection = await cancelProjection(projectionGUID)
    updateProjectionInList(cancelledProjection)
    notificationStore.showSuccessMessage(SUCCESS_MSG.PROJECTION_CANCELLED, SUCCESS_MSG.PROJECTION_CANCELLED_TITLE)
  } catch (err) {
    console.error('Error cancelling projection:', err)
    notificationStore.showErrorMessage(PROJECTION_ERR.CANCEL_FAILED, PROJECTION_ERR.CANCEL_FAILED_TITLE)
  } finally {
    isProgressVisible.value = false
  }
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

const handleNewProjection = (type: (typeof PROJECTION_INPUT_METHOD)[keyof typeof PROJECTION_INPUT_METHOD]) => {
  // Reset app store for new projection
  appStore.resetForNewProjection()

  if (type === PROJECTION_INPUT_METHOD.INPUT_MODEL_PARAMETERS) {
    appStore.setModelSelection(MODEL_SELECTION.INPUT_MODEL_PARAMETERS)
    // Reset model parameter store
    modelParameterStore.resetStore()
  } else {
    appStore.setModelSelection(MODEL_SELECTION.FILE_UPLOAD)
    // Reset file upload store
    fileUploadStore.resetStore()
  }
  saveNewProjectionSession(
    type === PROJECTION_INPUT_METHOD.INPUT_MODEL_PARAMETERS
      ? MODEL_SELECTION.INPUT_MODEL_PARAMETERS
      : MODEL_SELECTION.FILE_UPLOAD
  )
  router.push(ROUTE_PATH.PROJECTION_DETAIL)
}

// ============================================================================
// Bulk Actions
// ============================================================================

/**
 * Downloads all selected projections' result zips.
 * If only 1 zip is downloaded, it is saved directly without re-zipping.
 * If 2 or more zips are downloaded, they are merged into a single zip file.
 */
const handleBulkDownload = async () => {
  if (selectedGUIDs.value.length === 0) return

  isProgressVisible.value = true
  progressMessage.value = PROGRESS_MSG.DOWNLOADING_PROJECTION

  try {
    const downloaded: { zipBlob: Blob; title: string }[] = []

    for (const guid of selectedGUIDs.value) {
      const projection = projections.value.find(p => p.projectionGUID === guid)
      try {
        const { zipBlob } = await streamResultsZip(guid)
        downloaded.push({ zipBlob, title: projection?.title || guid })
      } catch (err) {
        console.error(`Error downloading projection ${guid}:`, err)
      }
    }

    if (downloaded.length === 0) {
      notificationStore.showErrorMessage(
        PROJECTION_ERR.DOWNLOAD_FAILED('Projections'),
        PROJECTION_ERR.DOWNLOAD_FAILED_TITLE,
      )
      return
    }

    let fileName: string
    let fileBlob: Blob

    if (downloaded.length === 1) {
      // Single result - download the backend zip directly without re-zipping
      fileName = sanitizeFileName(`${downloaded[0].title}_All Files`) + '.zip'
      fileBlob = downloaded[0].zipBlob
    } else {
      // Multiple results - wrap each backend zip as-is into a single master zip
      const masterZip = new JSZip()
      for (const { zipBlob, title } of downloaded) {
        masterZip.file(sanitizeFileName(`${title}_All Files`) + '.zip', zipBlob)
      }
      const now = new Date()
      const dateStr = `${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}${String(now.getDate()).padStart(2, '0')}`
      fileName = `VDYP_Projections_All Files_${dateStr}.zip`
      fileBlob = await masterZip.generateAsync({ type: 'blob' })
    }

    downloadFile(fileBlob, fileName)
    notificationStore.showSuccessMessage(
      SUCCESS_MSG.DOWNLOAD_SUCCESS(fileName),
      SUCCESS_MSG.DOWNLOAD_SUCCESS_TITLE,
    )
  } catch (err) {
    console.error('Error during bulk download:', err)
    notificationStore.showErrorMessage(
      PROJECTION_ERR.DOWNLOAD_FAILED('Projections'),
      PROJECTION_ERR.DOWNLOAD_FAILED_TITLE,
    )
  } finally {
    isProgressVisible.value = false
  }
}

/**
 * Duplicates all selected projections.
 */
const handleBulkDuplicate = async () => {
  if (selectedGUIDs.value.length === 0) return

  isProgressVisible.value = true
  progressMessage.value = PROGRESS_MSG.DUPLICATING_PROJECTION

  let successCount = 0
  let failCount = 0

  try {
    for (const guid of selectedGUIDs.value) {
      try {
        await duplicateProjection(guid)
        successCount++
      } catch (err) {
        console.error(`Error duplicating projection ${guid}:`, err)
        failCount++
      }
    }

    if (successCount > 0) {
      notificationStore.showSuccessMessage(
        `${successCount} projection(s) duplicated successfully.`,
        SUCCESS_MSG.PROJECTION_DUPLICATED_TITLE,
      )
    }
    if (failCount > 0) {
      notificationStore.showErrorMessage(
        `${failCount} projection(s) could not be duplicated.`,
        PROJECTION_ERR.DUPLICATE_FAILED_TITLE,
      )
    }

    clearSelection()
    await loadProjections()
  } finally {
    isProgressVisible.value = false
  }
}

/**
 * Cancels all selected projections that are Running (checks backend status).
 */
const handleBulkCancel = async () => {
  if (selectedGUIDs.value.length === 0) return

  isProgressVisible.value = true
  progressMessage.value = PROGRESS_MSG.CANCELLING_PROJECTION

  let successCount = 0
  let skipCount = 0

  try {
    for (const guid of selectedGUIDs.value) {
      try {
        // Check actual backend status
        const latestProjection = await getProjectionById(guid)
        const latestStatus = mapProjectionStatus(latestProjection.projectionStatusCode?.code || PROJECTION_STATUS.DRAFT)

        // Update frontend status if it differs
        updateProjectionInList(latestProjection)

        if (latestStatus !== PROJECTION_STATUS.RUNNING) {
          skipCount++
          continue
        }

        const cancelledProjection = await cancelProjection(guid)
        updateProjectionInList(cancelledProjection)
        successCount++
      } catch (err) {
        console.error(`Error cancelling projection ${guid}:`, err)
      }
    }

    if (successCount > 0) {
      notificationStore.showSuccessMessage(
        `${successCount} projection(s) cancelled successfully.`,
        SUCCESS_MSG.PROJECTION_CANCELLED_TITLE,
      )
    }
    if (skipCount > 0) {
      notificationStore.showInfoMessage(
        `${skipCount} projection(s) were not running and could not be cancelled.`,
        PROJECTION_ERR.CANCEL_NOT_RUNNING_TITLE,
      )
    }

    clearSelection()
  } finally {
    isProgressVisible.value = false
  }
}

/**
 * Deletes all selected projections that are not Running (checks backend status).
 */
const handleBulkDelete = async () => {
  if (selectedGUIDs.value.length === 0) return

  const confirmed = await alertDialogStore.openDialog(
    'Confirmation',
    `Are you sure you want to delete ${selectedGUIDs.value.length} projection(s)? Once deleted, they will not be recoverable.`,
    { variant: 'confirmation' },
  )

  if (!confirmed) return

  isProgressVisible.value = true
  progressMessage.value = PROGRESS_MSG.DELETING_PROJECTION

  let successCount = 0
  let skipCount = 0

  try {
    for (const guid of selectedGUIDs.value) {
      try {
        // Check actual backend status
        const latestProjection = await getProjectionById(guid)
        const latestStatus = mapProjectionStatus(latestProjection.projectionStatusCode?.code || PROJECTION_STATUS.DRAFT)

        // Update frontend status if it differs
        updateProjectionInList(latestProjection)

        if (latestStatus === PROJECTION_STATUS.RUNNING) {
          skipCount++
          continue
        }

        await deleteProjectionWithFiles(guid)
        successCount++
      } catch (err) {
        console.error(`Error deleting projection ${guid}:`, err)
      }
    }

    if (successCount > 0) {
      notificationStore.showSuccessMessage(
        `${successCount} projection(s) deleted successfully.`,
        SUCCESS_MSG.PROJECTION_DELETED_TITLE,
      )
    }
    if (skipCount > 0) {
      notificationStore.showWarningMessage(
        `${skipCount} running projection(s) were skipped and could not be deleted.`,
        'Running Projections Skipped',
      )
    }

    clearSelection()
    await loadProjections()
  } finally {
    isProgressVisible.value = false
  }
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
  overflow-x: hidden;
  box-sizing: border-box;
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

/* Prevent button label from wrapping on narrow screens */
.page-header :deep(.v-btn) {
  white-space: nowrap;
  flex-shrink: 0;
}

.new-projection-menu-item {
  cursor: pointer;
}

.new-projection-menu-item:hover {
  background-color: #eceae8;
}
</style>
