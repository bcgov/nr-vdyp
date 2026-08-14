<template>
  <v-container fluid class="admin-dashboard-container">
    <div class="page-header">
      <h1 class="page-heading">Admin Dashboard</h1>
    </div>

    <div class="filter-row">
      <div class="filter-group">
        <div class="filter-field">
          <label class="bcds-select-label" for="projection-status-select">Projection Status</label>
          <v-select
            id="projection-status-select"
            v-model="selectedStatus"
            :items="statusFilterOptions"
            clearable
            hide-details="auto"
            persistent-placeholder
            placeholder="Select"
            append-inner-icon="mdi-chevron-down"
            class="filter-select"
          />
        </div>

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

      <AdminResourceSummary
        :total-running="totalRunningCount"
        :threads-in-use="threadsInUseCount"
        :thread-capacity="threadCapacity"
        :thread-usage-percent="threadUsagePercent"
        :stuck-count="stuckCount"
        :queued-count="queuedCount"
      />
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
import { ADMIN_DASHBOARD_HEADER_KEY, SORT_ORDER, PAGINATION, BREAKPOINT, USER_TYPE_CODE, REFRESH_INTERVAL_MS, PROJECTION_STATUS } from '@/constants/constants'
import { itemsPerPageOptions as defaultItemsPerPageOptions } from '@/constants/options'
import { PROGRESS_MSG, SUCCESS_MSG, PROJECTION_ERR } from '@/constants/message'
import { AppProgressCircular } from '@/components'
import { ProjectionPagination, AdminProjectionTable, AdminProjectionCardList, AdminCancelProjectionDialog, AdminResourceSummary } from '@/components/projection'
import { fetchAllRunningProjections, fetchThreadCapacity } from '@/services/adminService'
import { cancelProjection } from '@/services/projectionService'
import { useNotificationStore } from '@/stores/common/notificationStore'

const notificationStore = useNotificationStore()

const projections = ref<AdminProjection[]>([])
const threadCapacity = ref<number>(0)
const isProgressVisible = ref(false)
const progressMessage = ref('')
const isCancelDialogOpen = ref(false)
const projectionPendingCancel = ref<AdminProjection | null>(null)

const userTypeFilterOptions = [
  { title: 'IDIR', value: USER_TYPE_CODE.IDIR },
  { title: 'BCeID', value: USER_TYPE_CODE.BCEID },
]
const selectedUserType = ref<UserTypeCode | null>(null)

// 'All' is a UI-only filter value (not a real projection status) meaning "no status filter applied".
const STATUS_FILTER_ALL = 'All'
const statusFilterOptions = [
  { title: 'Stuck', value: PROJECTION_STATUS.STUCK },
  { title: 'Running', value: PROJECTION_STATUS.RUNNING },
  { title: 'Queued', value: PROJECTION_STATUS.QUEUED },
  { title: STATUS_FILTER_ALL, value: STATUS_FILTER_ALL },
]
const selectedStatus = ref<string | null>(null)

// Default sort: Threads (Highest First)
const sortBy = ref<string>(ADMIN_DASHBOARD_HEADER_KEY.THREADS)
const sortOrder = ref<SortOrder>(SORT_ORDER.DESC)
const cardSortBy = ref<string>(`${ADMIN_DASHBOARD_HEADER_KEY.THREADS}-${SORT_ORDER.DESC}`)

const cardSortOptions: SortOption[] = [
  { title: 'Projection Name (A-Z)', value: `${ADMIN_DASHBOARD_HEADER_KEY.TITLE}-${SORT_ORDER.ASC}` },
  { title: 'Projection Name (Z-A)', value: `${ADMIN_DASHBOARD_HEADER_KEY.TITLE}-${SORT_ORDER.DESC}` },
  { title: 'Owner (A-Z)', value: `${ADMIN_DASHBOARD_HEADER_KEY.OWNER}-${SORT_ORDER.ASC}` },
  { title: 'Owner (Z-A)', value: `${ADMIN_DASHBOARD_HEADER_KEY.OWNER}-${SORT_ORDER.DESC}` },
  { title: 'User Type (A-Z)', value: `${ADMIN_DASHBOARD_HEADER_KEY.USER_TYPE}-${SORT_ORDER.ASC}` },
  { title: 'User Type (Z-A)', value: `${ADMIN_DASHBOARD_HEADER_KEY.USER_TYPE}-${SORT_ORDER.DESC}` },
  { title: 'Elapsed (Highest First)', value: `${ADMIN_DASHBOARD_HEADER_KEY.ELAPSED}-${SORT_ORDER.DESC}` },
  { title: 'Elapsed (Lowest First)', value: `${ADMIN_DASHBOARD_HEADER_KEY.ELAPSED}-${SORT_ORDER.ASC}` },
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

// Silent by design: thread capacity is secondary/contextual to the Threads in Use metric, so a
// failure here shouldn't block the dashboard or interrupt the user with a notification. The
// summary card degrades gracefully to 0/0 (0%) per the zero-data state requirement.
const loadThreadCapacity = async () => {
  try {
    threadCapacity.value = await fetchThreadCapacity()
  } catch (err) {
    console.error('Error loading thread capacity:', err)
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
    (p) =>
      (!selectedUserType.value || p.userType === selectedUserType.value) &&
      (!selectedStatus.value ||
        selectedStatus.value === STATUS_FILTER_ALL ||
        p.status === selectedStatus.value),
  ),
)

const totalRunningCount = computed(
  () => filteredProjections.value.filter((p) => p.status === PROJECTION_STATUS.RUNNING).length,
)

const stuckCount = computed(
  () => filteredProjections.value.filter((p) => p.status === PROJECTION_STATUS.STUCK).length,
)

const queuedCount = computed(
  () => filteredProjections.value.filter((p) => p.status === PROJECTION_STATUS.QUEUED).length,
)

const threadsInUseCount = computed(() =>
  filteredProjections.value.reduce((sum, p) => sum + p.workerCount, 0),
)

const threadUsagePercent = computed(() => {
  if (!threadCapacity.value) return 0
  return Math.min(100, Math.round((threadsInUseCount.value / threadCapacity.value) * 100))
})

const getProgressPercent = (projection: AdminProjection): number => {
  if (!projection.polygonCount) return 0
  return Math.min(
    100,
    Math.round((projection.completedPolygonCount / projection.polygonCount) * 100),
  )
}

const getSortValue = (projection: AdminProjection, key: string): string | number => {
  if (key === ADMIN_DASHBOARD_HEADER_KEY.TITLE) return projection.title.toLowerCase()
  if (key === ADMIN_DASHBOARD_HEADER_KEY.OWNER) return projection.ownerDisplayName.toLowerCase()
  if (key === ADMIN_DASHBOARD_HEADER_KEY.USER_TYPE) return (projection.userType ?? '').toLowerCase()
  if (key === ADMIN_DASHBOARD_HEADER_KEY.ELAPSED) {
    // Queued projections display '-' (no startDate yet), so they're sorted as lower than any
    // running projection's elapsed time: below it in descending order, above it (first) in
    // ascending order.
    if (!projection.startDate) return -1
    return now.value - new Date(projection.startDate).getTime()
  }
  if (key === ADMIN_DASHBOARD_HEADER_KEY.PROGRESS) {
    // Queued projections display '-' (processing hasn't started) rather than their underlying 0%
    // progress, so they're sorted as lower than any 0% running projection: below it in descending
    // order, above it (first) in ascending order.
    if (projection.status === PROJECTION_STATUS.QUEUED) return -1
    return getProgressPercent(projection)
  }
  if (key === ADMIN_DASHBOARD_HEADER_KEY.THREADS) {
    // Queued projections display '-' (no real thread count) rather than their underlying 0
    // workerCount, so they're sorted as lower than any 0-thread running projection: below it in
    // descending order, above it (first) in ascending order.
    if (projection.status === PROJECTION_STATUS.QUEUED) return -1
    return projection.workerCount
  }
  return 0
}

// Polygons sorts by the total (denominator) count, since the Progress column already covers the
// completed/total ratio. Queued projections display '-' (no polygon counts yet), so they're
// sorted as lower than any running projection's counts: below it in descending order, above it
// (first) in ascending order.
const getPolygonSortValues = (projection: AdminProjection): [number, number] => {
  if (projection.status === PROJECTION_STATUS.QUEUED) return [-1, -1]
  return [projection.polygonCount, projection.completedPolygonCount]
}

// When two projections have the same total polygon count, break the tie by completed count.
const comparePolygons = (a: AdminProjection, b: AdminProjection): number => {
  const [aTotal, aCompleted] = getPolygonSortValues(a)
  const [bTotal, bCompleted] = getPolygonSortValues(b)
  if (aTotal !== bTotal) {
    return sortOrder.value === SORT_ORDER.ASC ? aTotal - bTotal : bTotal - aTotal
  }
  return sortOrder.value === SORT_ORDER.ASC ? aCompleted - bCompleted : bCompleted - aCompleted
}

const sortedProjections = computed(() => {
  if (!sortBy.value) return filteredProjections.value
  return [...filteredProjections.value].sort((a, b) => {
    if (sortBy.value === ADMIN_DASHBOARD_HEADER_KEY.POLYGONS) return comparePolygons(a, b)

    const aValue = getSortValue(a, sortBy.value)
    const bValue = getSortValue(b, sortBy.value)
    if (typeof aValue === 'string' && typeof bValue === 'string') {
      return sortOrder.value === SORT_ORDER.ASC
        ? aValue.localeCompare(bValue)
        : bValue.localeCompare(aValue)
    }
    return sortOrder.value === SORT_ORDER.ASC
      ? (aValue as number) - (bValue as number)
      : (bValue as number) - (aValue as number)
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
  await loadThreadCapacity()
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
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--layout-margin-medium);
  margin-bottom: var(--layout-margin-medium);
}

.filter-group {
  display: flex;
  flex-wrap: wrap;
  gap: var(--layout-margin-medium);
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

/* Below 540px, User Type and the Total Running/Threads in Use row are
   always stacked (never side by side), so this only affects the vertical
   gap between them and between that row and "Sort By:". 30px matches the
   actual rendered gap between "Sort By:"'s select and the first card
   (its 8px flex gap plus the select's own reserved, empty v-input details
   row beneath it), so the whole column reads with one consistent rhythm. */
@media (max-width: 540px) {
  .filter-row {
    gap: 30px;
    margin-bottom: 30px;
  }
}
</style>
