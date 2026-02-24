<template>
  <div class="table-container">
    <table class="projections-table">
      <thead>
        <tr>
          <th
            v-for="header in headers"
            :key="header.key"
            class="table-header"
            :class="{ sortable: header.sortable }"
            @click="header.sortable ? handleSort(header.key) : null"
          >
            <div class="header-content">
              <span>{{ header.title }}</span>
              <v-icon
                v-if="header.sortable && sortBy === header.key"
                size="small"
                class="sort-icon"
              >
                {{ sortOrder === SORT_ORDER.ASC ? 'mdi-arrow-up' : 'mdi-arrow-down' }}
              </v-icon>
            </div>
          </th>
          <th class="table-header actions-header"></th>
        </tr>
      </thead>
      <tbody>
        <!-- Empty state when no projections -->
        <tr v-if="projections.length === 0" class="empty-state-row">
          <td :colspan="headers.length + 1" class="empty-state-cell">
            <div class="empty-state-content">
              <span class="empty-state-message">
                No projections found. Create a new projection to build your history.
              </span>
            </div>
          </td>
        </tr>
        <tr
          v-else
          v-for="projection in projections"
          :key="projection.projectionGUID"
          class="table-row clickable-row"
          @click="handleRowClick($event, projection)"
        >
          <td class="table-cell">
            <span class="cell-content cell-with-tooltip tooltip-right" :data-tooltip="projection.title">{{
              projection.title
            }}</span>
          </td>
          <td class="table-cell">
            <span class="cell-content cell-with-tooltip tooltip-right" :data-tooltip="projection.description">{{
              projection.description
            }}</span>
          </td>
          <td class="table-cell">
            <span class="cell-content cell-with-tooltip tooltip-right" :data-tooltip="projection.method">{{
              projection.method
            }}</span>
          </td>
          <td class="table-cell">
            <span class="cell-content cell-with-tooltip tooltip-right" :data-tooltip="projection.projectionType">{{
              projection.projectionType
            }}</span>
          </td>
          <td class="table-cell">
            <span class="cell-content cell-with-tooltip tooltip-right" :data-tooltip="formatDateTimeDisplay(projection.lastUpdated)">{{
              formatDateTimeDisplay(projection.lastUpdated)
            }}</span>
          </td>
          <td class="table-cell">
            <span class="cell-content cell-with-tooltip tooltip-right" :data-tooltip="formatDateDisplay(projection.expiration)">{{
              formatDateDisplay(projection.expiration)
            }}</span>
          </td>
          <td class="table-cell">
            <div class="status-cell cell-with-tooltip tooltip-right" :data-tooltip="projection.status">
              <ProjectionStatusBadge :status="projection.status" />
            </div>
          </td>
          <td class="table-cell actions-cell">
            <ProjectionActionsMenu
              :status="projection.status"
              :title="projection.title"
              @view="$emit(PROJECTION_USER_ACTION.VIEW, projection.projectionGUID)"
              @edit="$emit(PROJECTION_USER_ACTION.EDIT, projection.projectionGUID)"
              @duplicate="$emit(PROJECTION_USER_ACTION.DUPLICATE, projection.projectionGUID)"
              @download="$emit(PROJECTION_USER_ACTION.DOWNLOAD, projection.projectionGUID)"
              @cancel="$emit(PROJECTION_USER_ACTION.CANCEL, projection.projectionGUID)"
              @delete="$emit(PROJECTION_USER_ACTION.DELETE, projection.projectionGUID)"
            />
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup lang="ts">
import type { Projection, TableHeader } from '@/interfaces/interfaces'
import type { SortOrder } from '@/types/types'
import { PROJECTION_USER_ACTION, SORT_ORDER } from '@/constants/constants'
import { ProjectionActionsMenu, ProjectionStatusBadge} from '@/components/projection'
import { formatDateTimeDisplay, formatDateDisplay } from '@/utils/util'

interface Props {
  projections: Projection[]
  headers: TableHeader[]
  sortBy: string
  sortOrder: SortOrder
}

defineProps<Props>()

const emit = defineEmits<{
  (e: 'sort', key: string): void
  (e: 'view', projectionGUID: string): void
  (e: 'edit', projectionGUID: string): void
  (e: 'duplicate', projectionGUID: string): void
  (e: 'download', projectionGUID: string): void
  (e: 'cancel', projectionGUID: string): void
  (e: 'delete', projectionGUID: string): void
  (e: 'rowClick', projection: Projection): void
}>()

const handleSort = (key: string) => {
  emit('sort', key)
}

const handleRowClick = (event: MouseEvent, projection: Projection) => {
  // Don't trigger row click if clicking on the actions cell or its children
  const target = event.target as HTMLElement
  if (target.closest('.actions-cell')) {
    return
  }
  emit('rowClick', projection)
}
</script>

<style scoped>
/* Table Styles */
.table-container {
  overflow: visible;
  background: var(--surface-color-forms-default);
  border-radius: var(--layout-border-radius-medium);
  border: none;
}

@media (max-width: 1400px) {
  .table-container {
    overflow-x: auto;
    overflow-y: visible;
  }
}

.projections-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
}

.projections-table thead,
.projections-table tbody,
.projections-table tr,
.projections-table td,
.projections-table th {
  overflow: visible;
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

.table-row.clickable-row {
  cursor: pointer;
}

.table-row.clickable-row:hover {
  background-color: var(--surface-color-background-light-gray);
}

.table-row:nth-child(even) {
  background-color: var(--surface-color-background-light-gray);
}

.table-cell {
  padding: var(--layout-padding-medium);
  font: var(--typography-regular-body);
  color: var(--typography-color-primary);
  vertical-align: middle;
  position: relative;
}

.cell-content {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-cell {
  display: flex;
  align-items: center;
}

.actions-cell {
  padding: var(--layout-padding-small);
}

/* Column widths - table-layout: fixed requires width on th/td */
.table-header:nth-child(1),
.table-cell:nth-child(1) {  /* Projection Title - accommodate ~20 characters */
  width: 180px;
}

.table-header:nth-child(2),
.table-cell:nth-child(2) {  /* Description - accommodate ~30 characters */
  width: 270px;
}

.table-header:nth-child(3),
.table-cell:nth-child(3) {  /* Method */
  width: 160px;
}

.table-header:nth-child(4),
.table-cell:nth-child(4) {  /* Projection Type */
  width: 130px;
}

.table-header:nth-child(5),
.table-cell:nth-child(5) {  /* Last Updated */
  width: 130px;
}

.table-header:nth-child(6),
.table-cell:nth-child(6) {  /* Expiration */
  width: 100px;
}

.table-header:nth-child(7),
.table-cell:nth-child(7) {  /* Status */
  width: 110px;
}

.table-header:nth-child(8),
.table-cell:nth-child(8) {  /* Actions */
  width: 50px;
}

/* Custom CSS Tooltip - BC Gov Design Standards */
.cell-with-tooltip {
  cursor: inherit;
}

.cell-with-tooltip:hover::after {
  content: attr(data-tooltip);
  position: absolute;
  bottom: calc(100% - 8px);
  left: 50%;
  transform: translateX(-50%);
  background-color: var(--theme-gray-110);
  color: var(--typography-color-primary-invert);
  padding: var(--layout-padding-xsmall) var(--layout-padding-small);
  border-radius: var(--layout-border-radius-medium);
  font: var(--typography-regular-small-body);
  white-space: normal;
  max-width: 700px;
  min-width: 150px;
  width: max-content;
  box-shadow: var(--surface-shadow-small);
  z-index: 9999;
}

.cell-with-tooltip:hover::before {
  content: '';
  position: absolute;
  bottom: calc(100% - 16px);
  left: 50%;
  transform: translate3d(0, 0, 0);
  border-style: solid;
  border-width: 8px 8px 0 8px;
  border-color: var(--theme-gray-110) transparent transparent transparent;
  z-index: 9999;
}

/* First column tooltip - positioned to the right to prevent left edge clipping */
.cell-with-tooltip.tooltip-right:hover::after {
  left: 0;
  transform: none;
}

.cell-with-tooltip.tooltip-right:hover::before {
  left: 16px;
  transform: none;
}

/* Empty state styles */
.empty-state-row {
  background-color: var(--surface-color-forms-default);
}

.empty-state-cell {
  padding: var(--layout-padding-xlarge) var(--layout-padding-medium);
  text-align: center;
}

.empty-state-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--layout-margin-small);
}

.empty-state-message {
  font: var(--typography-regular-body);
  color: var(--typography-color-secondary);
}
</style>
