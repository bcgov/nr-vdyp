<template>
  <div class="table-container">
    <table class="admin-table">
      <thead>
        <tr>
          <th
            class="table-header sortable"
            @click="handleSort(ADMIN_DASHBOARD_HEADER_KEY.TITLE)"
          >
            <div class="header-content">
              <span>Projection Name</span>
              <v-icon
                v-if="sortBy === ADMIN_DASHBOARD_HEADER_KEY.TITLE"
                size="small"
                class="sort-icon"
              >
                {{ sortOrder === SORT_ORDER.ASC ? 'mdi-arrow-up' : 'mdi-arrow-down' }}
              </v-icon>
            </div>
          </th>
          <th
            class="table-header sortable"
            @click="handleSort(ADMIN_DASHBOARD_HEADER_KEY.OWNER)"
          >
            <div class="header-content">
              <span>Owner</span>
              <v-icon
                v-if="sortBy === ADMIN_DASHBOARD_HEADER_KEY.OWNER"
                size="small"
                class="sort-icon"
              >
                {{ sortOrder === SORT_ORDER.ASC ? 'mdi-arrow-up' : 'mdi-arrow-down' }}
              </v-icon>
            </div>
          </th>
          <th
            class="table-header sortable"
            @click="handleSort(ADMIN_DASHBOARD_HEADER_KEY.USER_TYPE)"
          >
            <div class="header-content">
              <span>User Type</span>
              <v-icon
                v-if="sortBy === ADMIN_DASHBOARD_HEADER_KEY.USER_TYPE"
                size="small"
                class="sort-icon"
              >
                {{ sortOrder === SORT_ORDER.ASC ? 'mdi-arrow-up' : 'mdi-arrow-down' }}
              </v-icon>
            </div>
          </th>
          <th
            class="table-header sortable"
            @click="handleSort(ADMIN_DASHBOARD_HEADER_KEY.ELAPSED)"
          >
            <div class="header-content">
              <span>Elapsed</span>
              <v-icon
                v-if="sortBy === ADMIN_DASHBOARD_HEADER_KEY.ELAPSED"
                size="small"
                class="sort-icon"
              >
                {{ sortOrder === SORT_ORDER.ASC ? 'mdi-arrow-up' : 'mdi-arrow-down' }}
              </v-icon>
            </div>
          </th>
          <th
            class="table-header sortable"
            @click="handleSort(ADMIN_DASHBOARD_HEADER_KEY.THREADS)"
          >
            <div class="header-content">
              <span>Threads</span>
              <v-icon
                v-if="sortBy === ADMIN_DASHBOARD_HEADER_KEY.THREADS"
                size="small"
                class="sort-icon"
              >
                {{ sortOrder === SORT_ORDER.ASC ? 'mdi-arrow-up' : 'mdi-arrow-down' }}
              </v-icon>
            </div>
          </th>
          <th
            class="table-header sortable"
            @click="handleSort(ADMIN_DASHBOARD_HEADER_KEY.PROGRESS)"
          >
            <div class="header-content">
              <span>Progress</span>
              <v-icon
                v-if="sortBy === ADMIN_DASHBOARD_HEADER_KEY.PROGRESS"
                size="small"
                class="sort-icon"
              >
                {{ sortOrder === SORT_ORDER.ASC ? 'mdi-arrow-up' : 'mdi-arrow-down' }}
              </v-icon>
            </div>
          </th>
          <th
            class="table-header sortable"
            @click="handleSort(ADMIN_DASHBOARD_HEADER_KEY.POLYGONS)"
          >
            <div class="header-content">
              <span>Polygons</span>
              <v-icon
                v-if="sortBy === ADMIN_DASHBOARD_HEADER_KEY.POLYGONS"
                size="small"
                class="sort-icon"
              >
                {{ sortOrder === SORT_ORDER.ASC ? 'mdi-arrow-up' : 'mdi-arrow-down' }}
              </v-icon>
            </div>
          </th>
          <th class="table-header actions-header">Actions</th>
        </tr>
      </thead>
      <tbody>
        <tr v-if="projections.length === 0" class="empty-state-row">
          <td colspan="8" class="empty-state-cell">
            <span class="empty-state-message">No projections found.</span>
          </td>
        </tr>
        <tr
          v-else
          v-for="projection in projections"
          :key="projection.projectionGUID"
          class="table-row"
        >
          <td class="table-cell">
            <div class="projection-name-cell">
              <span class="projection-title">{{ projection.title }}</span>
              <span
                v-if="projection.status === PROJECTION_STATUS.STUCK"
                class="status-badge status-stuck"
              >
                <img :src="StuckIcon14px" alt="" class="status-badge-icon" />
                Stuck
              </span>
              <span
                v-else-if="projection.status === PROJECTION_STATUS.RUNNING"
                class="status-badge status-running"
              >
                <img :src="RunningIcon" alt="" class="status-badge-icon" />
                Running
              </span>
              <span
                v-else-if="projection.status === PROJECTION_STATUS.QUEUED"
                class="status-badge status-queued"
              >
                <img :src="QueuedIcon14px" alt="" class="status-badge-icon" />
                Queued
              </span>
            </div>
          </td>
          <td class="table-cell">{{ projection.ownerDisplayName }}</td>
          <td class="table-cell">
            <span v-if="projection.userType" class="user-type-chip">
              {{ projection.userType }}
            </span>
            <span v-else>-</span>
          </td>
          <td class="table-cell">
            <span :class="{ 'elapsed-stuck': projection.status === PROJECTION_STATUS.STUCK }">
              {{ formatElapsedTime(projection.startDate) }}
            </span>
          </td>
          <td class="table-cell">
            <span v-if="projection.status === PROJECTION_STATUS.QUEUED">-</span>
            <span v-else>{{ projection.workerCount }}</span>
          </td>
          <td class="table-cell">
            <span v-if="projection.status === PROJECTION_STATUS.QUEUED">-</span>
            <div v-else class="progress-cell">
              <div class="progress-track">
                <div
                  class="progress-fill"
                  :class="{ 'progress-fill-danger': projection.status === PROJECTION_STATUS.STUCK }"
                  :style="{ width: `${getProgressPercent(projection)}%` }"
                />
              </div>
              <span class="progress-percent">{{ getProgressPercent(projection) }}%</span>
            </div>
          </td>
          <td class="table-cell">
            <span v-if="projection.status === PROJECTION_STATUS.QUEUED">-</span>
            <div v-else class="polygons-cell">
              <span class="polygons-completed">{{ formatNumber(projection.completedPolygonCount) }}</span>
              <span class="polygons-total">/ {{ formatNumber(projection.polygonCount) }}</span>
            </div>
          </td>
          <td class="table-cell actions-cell">
            <AppButton
              label="Cancel"
              variant="secondary"
              size="small"
              mdi-name="mdi-stop-circle-outline"
              class="cancel-button"
              @click="$emit('cancel', projection.projectionGUID)"
            />
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup lang="ts">
import type { AdminProjection } from '@/interfaces/interfaces'
import type { SortOrder } from '@/types/types'
import { ADMIN_DASHBOARD_HEADER_KEY, SORT_ORDER, PROJECTION_STATUS } from '@/constants/constants'
import { formatNumber } from '@/utils/util'
import { AppButton } from '@/components'
import { RunningIcon, StuckIcon14px, QueuedIcon14px } from '@/assets/'

interface Props {
  projections: AdminProjection[]
  sortBy: string
  sortOrder: SortOrder
  now: number
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'sort', key: string): void
  (e: 'cancel', projectionGUID: string): void
}>()

const handleSort = (key: string) => {
  emit('sort', key)
}

const getProgressPercent = (projection: AdminProjection): number => {
  if (!projection.polygonCount) return 0
  return Math.min(
    100,
    Math.round((projection.completedPolygonCount / projection.polygonCount) * 100),
  )
}

const formatElapsedTime = (startDate: string | null): string => {
  if (!startDate) return '-'
  const startMs = new Date(startDate).getTime()
  if (Number.isNaN(startMs)) return '-'

  const elapsedSeconds = Math.max(0, Math.floor((props.now - startMs) / 1000))
  const hours = Math.floor(elapsedSeconds / 3600)
  const minutes = Math.floor((elapsedSeconds % 3600) / 60)
  const seconds = elapsedSeconds % 60
  const pad = (n: number) => n.toString().padStart(2, '0')
  return `${pad(hours)}:${pad(minutes)}:${pad(seconds)}`
}
</script>

<style scoped>
.table-container {
  overflow-x: auto;
  background: var(--surface-color-forms-default);
  border-radius: var(--layout-border-radius-medium);
}

.admin-table {
  width: 100%;
  border-collapse: collapse;
}

.table-header {
  font: var(--typography-bold-small-body);
  color: var(--typography-color-primary);
  text-align: left;
  padding: var(--layout-padding-medium);
  border-bottom: 1px solid var(--surface-color-border-dark);
  white-space: nowrap;
}

.table-header.sortable {
  cursor: pointer;
  user-select: none;
}

.table-header.sortable:hover {
  background-color: #eceae8;
}

.header-content {
  display: flex;
  align-items: center;
  gap: var(--layout-padding-xsmall);
}

.sort-icon {
  color: var(--icons-color-primary-default);
}

.table-row {
  border-bottom: 1px solid var(--surface-color-border-default);
}

.table-row:nth-child(even) {
  background-color: var(--surface-color-background-light-gray);
}

.table-cell {
  padding: var(--layout-padding-medium);
  font: var(--typography-regular-body);
  color: var(--typography-color-primary);
  vertical-align: middle;
  white-space: nowrap;
}

.actions-cell {
  white-space: nowrap;
}

.polygons-cell {
  display: flex;
  flex-direction: column;
}

.polygons-completed {
  font: var(--typography-regular-body);
  color: var(--typography-color-primary);
}

.polygons-total {
  font: var(--typography-regular-small-body);
  color: var(--typography-color-secondary);
}

.cancel-button {
  border-color: var(--support-border-color-danger) !important;
  color: var(--support-border-color-danger) !important;
}

.cancel-button :deep(.v-icon) {
  color: var(--support-border-color-danger);
}

.user-type-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 24px;
  padding: var(--layout-margin-hair) var(--layout-padding-small);
  border-radius: var(--layout-border-radius-small);
  border: 1px solid var(--surface-color-border-dark);
  background: var(--theme-gray-20);
  font: var(--typography-regular-small-body);
  color: var(--typography-color-primary);
  white-space: nowrap;
}

.projection-name-cell {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: var(--layout-padding-xsmall);
}

.projection-title {
  font: var(--typography-regular-body);
  color: var(--typography-color-primary);
}

.status-badge {
  display: flex;
  align-items: center;
  height: 24px;
  padding: var(--layout-margin-hair) var(--layout-padding-small);
  gap: var(--layout-margin-small);
  border-radius: var(--layout-border-radius-small);
  font: var(--typography-regular-small-body);
  color: var(--typography-color-primary);
  white-space: nowrap;
}

.status-badge-icon {
  width: 14px;
  height: 14px;
  flex-shrink: 0;
}

.status-badge.status-running {
  border: 1px solid var(--support-border-color-warning);
  background: var(--support-surface-color-warning);
  color: #FCBA19;
}

.status-badge.status-stuck {
  border: 1px solid var(--support-border-color-danger);
  background: var(--support-surface-color-danger);
  color: #CE3E39;
}

.status-badge.status-queued {
  border: 1px solid var(--support-border-color-info);
  background: var(--support-surface-color-info);
  color: var(--typography-color-primary);
}

.elapsed-stuck {
  color: var(--typography-color-danger);
}

.progress-cell {
  display: flex;
  align-items: center;
  gap: var(--layout-padding-small);
  min-width: 140px;
}

.progress-track {
  flex: 1;
  height: 8px;
  background-color: #e8e8e8;
  border: 1px solid #d8d8d8;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background-color: #003366;
  transition: width 0.4s ease;
}

.progress-fill.progress-fill-danger {
  background-color: var(--support-border-color-danger);
}

.progress-percent {
  font: var(--typography-regular-small-body);
  white-space: nowrap;
}

.empty-state-row {
  background-color: var(--surface-color-forms-default);
}

.empty-state-cell {
  padding: var(--layout-padding-xlarge) var(--layout-padding-medium);
  text-align: center;
}

.empty-state-message {
  font: var(--typography-regular-body);
  color: var(--typography-color-secondary);
}
</style>
