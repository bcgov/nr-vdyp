<template>
  <v-container fluid class="admin-dashboard-container">
    <div class="page-header">
      <h1 class="page-heading">Admin Dashboard</h1>
    </div>

    <div class="filter-row">
      <div class="filter-field">
        <label class="bcds-select-label" for="user-type-select">User Type</label>
        <v-select
          id="user-type-select"
          v-model="selectedUserType"
          :items="userTypeFilterOptions"
          clearable
          hide-details="auto"
          persistent-placeholder
          placeholder="Select"
          append-inner-icon="mdi-chevron-down"
          class="filter-select"
        />
      </div>
    </div>

    <!-- Desktop/Tablet Table View (above 1025px) -->
    <AdminProjectionTable
      v-if="!isCardView"
      :projections="paginatedProjections"
      :sort-by="sortBy"
      :sort-order="sortOrder"
      :now="now"
      @sort="handleSort"
      @cancel="handleCancel"
    />

    <!-- Mobile Card View (1025px and below) -->
    <AdminProjectionCardList
      v-else
      :projections="paginatedProjections"
      :sort-options="cardSortOptions"
      :sort-value="cardSortBy"
      :now="now"
      @sort="handleCardSort"
      @cancel="handleCancel"
    />

    <ProjectionPagination
      v-model:current-page="currentPage"
      v-model:items-per-page="itemsPerPage"
      :total-items="sortedProjections.length"
      :items-per-page-options="itemsPerPageOptions"
    />

    <AdminCancelProjectionDialog
      v-model="isCancelDialogOpen"
      :projection-title="projectionPendingCancel?.title ?? ''"
      @confirm="handleConfirmCancel"
    />

    <AppProgressCircular :is-show="isProgressVisible" :message="progressMessage" />
  </v-container>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import type { AdminProjection, UserTypeCode, SortOption } from '@/interfaces/interfaces'
import type { SortOrder } from '@/types/types'
import { ADMIN_DASHBOARD_HEADER_KEY, SORT_ORDER, PAGINATION, BREAKPOINT, USER_TYPE_CODE, REFRESH_INTERVAL_MS } from '@/constants/constants'
import { itemsPerPageOptions as defaultItemsPerPageOptions } from '@/constants/options'
import { PROGRESS_MSG, SUCCESS_MSG, PROJECTION_ERR } from '@/constants/message'
import { AppProgressCircular } from '@/components'
import { ProjectionPagination, AdminProjectionTable, AdminProjectionCardList, AdminCancelProjectionDialog } from '@/components/projection'
import { fetchAllRunningProjections } from '@/services/adminService'
import { cancelProjection } from '@/services/projectionService'
import { useNotificationStore } from '@/stores/common/notificationStore'

const notificationStore = useNotificationStore()

const projections = ref<AdminProjection[]>([])
const isProgressVisible = ref(false)
const progressMessage = ref('')
const isCancelDialogOpen = ref(false)
const projectionPendingCancel = ref<AdminProjection | null>(null)

const userTypeFilterOptions = [
  { title: 'IDIR', value: USER_TYPE_CODE.IDIR },
  { title: 'BCeID', value: USER_TYPE_CODE.BCEID },
]
const selectedUserType = ref<UserTypeCode | null>(null)

// Default sort: Threads (Highest First)
const sortBy = ref<string>(ADMIN_DASHBOARD_HEADER_KEY.THREADS)
const sortOrder = ref<SortOrder>(SORT_ORDER.DESC)
const cardSortBy = ref<string>(`${ADMIN_DASHBOARD_HEADER_KEY.THREADS}-${SORT_ORDER.DESC}`)

const cardSortOptions: SortOption[] = [
  { title: 'Threads (Highest First)', value: `${ADMIN_DASHBOARD_HEADER_KEY.THREADS}-${SORT_ORDER.DESC}` },
  { title: 'Threads (Lowest First)', value: `${ADMIN_DASHBOARD_HEADER_KEY.THREADS}-${SORT_ORDER.ASC}` },
  { title: 'Progress (Highest First)', value: `${ADMIN_DASHBOARD_HEADER_KEY.PROGRESS}-${SORT_ORDER.DESC}` },
  { title: 'Progress (Lowest First)', value: `${ADMIN_DASHBOARD_HEADER_KEY.PROGRESS}-${SORT_ORDER.ASC}` },
  { title: 'Polygons (Highest First)', value: `${ADMIN_DASHBOARD_HEADER_KEY.POLYGONS}-${SORT_ORDER.DESC}` },
  { title: 'Polygons (Lowest First)', value: `${ADMIN_DASHBOARD_HEADER_KEY.POLYGONS}-${SORT_ORDER.ASC}` },
]

const currentPage = ref<number>(PAGINATION.DEFAULT_PAGE)
const itemsPerPage = ref<number>(PAGINATION.DEFAULT_ITEMS_PER_PAGE)
const itemsPerPageOptions = defaultItemsPerPageOptions

const windowWidth = ref<number>(window.innerWidth)
const isCardView = computed(() => windowWidth.value <= BREAKPOINT.CARD_VIEW)
const handleResize = () => {
  windowWidth.value = window.innerWidth
}

const loadProjections = async () => {
  try {
    projections.value = await fetchAllRunningProjections()
  } catch (err) {
    console.error('Error loading running projections:', err)
    notificationStore.showErrorMessage(
      'Failed to load running projections.',
      'Load Failed',
    )
  }
}

// Silent refresh used by polling so transient failures don't spam the user with
// a notification every 5 seconds; errors are still logged for diagnostics.
const refreshProjections = async () => {
  try {
    projections.value = await fetchAllRunningProjections()
  } catch (err) {
    console.error('Error refreshing running projections:', err)
  }
}

const filteredProjections = computed(() =>
  projections.value.filter(
    (p) => !selectedUserType.value || p.userType === selectedUserType.value,
  ),
)

const getProgressPercent = (projection: AdminProjection): number => {
  if (!projection.polygonCount) return 0
  return Math.min(
    100,
    Math.round((projection.completedPolygonCount / projection.polygonCount) * 100),
  )
}

const getSortValue = (projection: AdminProjection, key: string): number => {
  if (key === ADMIN_DASHBOARD_HEADER_KEY.PROGRESS) return getProgressPercent(projection)
  if (key === ADMIN_DASHBOARD_HEADER_KEY.THREADS) return projection.workerCount
  if (key === ADMIN_DASHBOARD_HEADER_KEY.POLYGONS) return projection.polygonCount
  return 0
}

const sortedProjections = computed(() => {
  if (!sortBy.value) return filteredProjections.value
  return [...filteredProjections.value].sort((a, b) => {
    const aValue = getSortValue(a, sortBy.value)
    const bValue = getSortValue(b, sortBy.value)
    return sortOrder.value === SORT_ORDER.ASC ? aValue - bValue : bValue - aValue
  })
})

const paginatedProjections = computed(() => {
  const start = (currentPage.value - 1) * itemsPerPage.value
  const end = start + itemsPerPage.value
  return sortedProjections.value.slice(start, end)
})

const handleSort = (key: string) => {
  if (sortBy.value === key) {
    sortOrder.value = sortOrder.value === SORT_ORDER.ASC ? SORT_ORDER.DESC : SORT_ORDER.ASC
  } else {
    sortBy.value = key
    sortOrder.value = SORT_ORDER.ASC
  }
  cardSortBy.value = `${sortBy.value}-${sortOrder.value}`
}

const handleCardSort = (value: string) => {
  cardSortBy.value = value
  const [key, order] = value.split('-')
  sortBy.value = key
  sortOrder.value = order as SortOrder
}

const handleCancel = (projectionGUID: string) => {
  projectionPendingCancel.value = projections.value.find((p) => p.projectionGUID === projectionGUID) ?? null
  isCancelDialogOpen.value = true
}

const handleConfirmCancel = async (reason: string) => {
  const projectionGUID = projectionPendingCancel.value?.projectionGUID
  projectionPendingCancel.value = null
  if (!projectionGUID) return

  isProgressVisible.value = true
  progressMessage.value = PROGRESS_MSG.CANCELLING_PROJECTION
  try {
    await cancelProjection(projectionGUID, reason)
    notificationStore.showSuccessMessage(SUCCESS_MSG.PROJECTION_CANCELLED, SUCCESS_MSG.PROJECTION_CANCELLED_TITLE)
    await loadProjections()
  } catch (err) {
    console.error('Error cancelling projection:', err)
    notificationStore.showErrorMessage(PROJECTION_ERR.CANCEL_FAILED, PROJECTION_ERR.CANCEL_FAILED_TITLE)
  } finally {
    isProgressVisible.value = false
  }
}

// Ticking clock so Elapsed (HH:MM:SS) updates live for RUNNING projections
const now = ref(Date.now())
let clockTimer: ReturnType<typeof setInterval> | null = null

// Poll running projections data (Threads/Progress/Polygons) so the dashboard stays in
// sync with a single projection's detail progress bar, which polls every 5 seconds.
let dataPollingTimer: ReturnType<typeof setInterval> | null = null

onMounted(async () => {
  window.addEventListener('resize', handleResize)
  await loadProjections()
  clockTimer = setInterval(() => {
    now.value = Date.now()
  }, REFRESH_INTERVAL_MS.ADMIN_DASHBOARD_CLOCK_TICK)
  dataPollingTimer = setInterval(refreshProjections, REFRESH_INTERVAL_MS.ADMIN_DASHBOARD_DATA_POLL)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (clockTimer !== null) {
    clearInterval(clockTimer)
  }
  if (dataPollingTimer !== null) {
    clearInterval(dataPollingTimer)
  }
})
</script>

<style scoped>
.admin-dashboard-container {
  padding: var(--layout-padding-medium);
  max-width: 100%;
  overflow-x: hidden;
  box-sizing: border-box;
}

.page-header {
  margin-bottom: var(--layout-margin-large);
}

.page-heading {
  font: var(--typography-bold-h4);
  color: var(--typography-color-primary);
  margin: 0;
}

.filter-row {
  display: flex;
  gap: var(--layout-margin-medium);
  margin-bottom: var(--layout-margin-medium);
}

.filter-field {
  display: flex;
  flex-direction: column;
  gap: var(--layout-padding-xsmall);
  min-width: 200px;
}

.filter-select {
  max-width: 240px;
}
</style>
