<template>
  <div class="resource-summary">
    <div class="summary-pill summary-pill--total">
      <span class="pill-icon" aria-hidden="true">
        <img :src="ChartLineIcon" alt="" class="pill-icon-img" />
      </span>
      <span class="pill-label">Total Running:</span>
      <span class="pill-value">{{ totalRunning }}</span>
    </div>

    <div class="summary-pill summary-pill--queued">
      <span class="pill-icon" aria-hidden="true">
        <img :src="QueuedIcon14px" alt="" class="pill-icon-img pill-icon-img--queued" />
      </span>
      <span class="pill-label">Total Queued:</span>
      <span class="pill-value">{{ queuedCount }}</span>
    </div>

    <div class="summary-pill summary-pill--stuck">
      <span class="pill-icon" aria-hidden="true">
        <img :src="ExclamationMarkIcon" alt="" class="pill-icon-img pill-icon-img--stuck" />
      </span>
      <span class="pill-label">Stuck:</span>
      <span class="pill-value">{{ stuckCount }}</span>
    </div>

    <v-tooltip location="top">
      <template #activator="{ props: tooltipProps }">
        <div
          class="summary-pill summary-pill--storage"
          :class="{ 'summary-pill--storage-alert': storageOutOfSpec }"
          v-bind="tooltipProps"
        >
          <div class="pill-header">
            <span class="pill-label">System Storage</span>
            <span class="pill-value">{{ storagePercent }}%</span>
          </div>
          <v-progress-linear
            :model-value="storagePercent"
            :color="storageOutOfSpec ? 'var(--progress-alert-color)' : 'var(--theme-primary-color, #003366)'"
            bg-color="var(--progress-track-color)"
            :bg-opacity="1"
            height="12"
            rounded="pill"
            class="thread-progress-bar"
          />
        </div>
      </template>
      <span>
        {{ formatBytes(storageUsedBytes) }} / {{ formatBytes(storageTotalBytes) }} used
        <template v-if="storageOutOfSpec"> — Out of Spec</template>
      </span>
    </v-tooltip>

    <div class="summary-pill summary-pill--threads">
      <div class="pill-header">
        <span class="pill-label">Threads in Use</span>
        <span class="pill-value">{{ threadsInUse }}/ {{ threadCapacity }}</span>
      </div>
      <v-progress-linear
        :model-value="threadUsagePercent"
        color="var(--theme-primary-color, #003366)"
        bg-color="var(--progress-track-color)"
        :bg-opacity="1"
        height="12"
        rounded="pill"
        class="thread-progress-bar"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ChartLineIcon, ExclamationMarkIcon, QueuedIcon14px } from '@/assets'

defineProps<{
  totalRunning: number
  threadsInUse: number
  threadCapacity: number
  threadUsagePercent: number
  stuckCount: number
  queuedCount: number
  storagePercent: number
  storageUsedBytes: number
  storageTotalBytes: number
  storageOutOfSpec: boolean
}>()

const BYTE_UNITS = ['B', 'KB', 'MB', 'GB', 'TB'] as const

/**
 * Formats a byte count as a human-readable string for the hover tooltip, auto-scaling to the
 * smallest unit that keeps at least one significant digit.
 */
const formatBytes = (bytes: number): string => {
  if (bytes <= 0) return '0 B'
  const exponent = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), BYTE_UNITS.length - 1)
  const value = bytes / 1024 ** exponent
  return `${exponent === 0 ? value : value.toFixed(1)} ${BYTE_UNITS[exponent]}`
}
</script>

<style scoped>
.resource-summary {
  --progress-track-color: #e5e7eb;
  --progress-alert-color: var(--support-color-danger, #d32f2f);
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 30px;
  padding: 0 15px 20px 24px;
  /* Drops this row to align with the User Type <select>'s top, not its label above it. */
  margin-top: calc(var(--typography-font-size-label) * 1.5 + 4px + var(--layout-padding-xsmall) - 3px);
  border-radius: 4px;
}

.summary-pill {
  background: var(--theme-gray-20, #f3f2f1);
  border-radius: var(--layout-borderRadius-circular, 9999px);
}

.summary-pill--total,
.summary-pill--stuck,
.summary-pill--queued {
  display: inline-flex;
  height: 32px;
  padding: var(--layout-margin-hair, 2px) var(--layout-padding-medium, 16px);
  align-items: center;
  gap: var(--layout-margin-small, 8px);
}

.summary-pill--threads,
.summary-pill--storage {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 6px;
  min-width: 189px;
  background: none;
  border-radius: 0;
}

.summary-pill--storage-alert .pill-value {
  color: var(--progress-alert-color);
}

.pill-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--layout-padding-small);
}

.pill-icon {
  display: flex;
  align-items: center;
}

.pill-icon-img {
  width: 11.2px;
  height: 10.4px;
  aspect-ratio: 14 / 13;
  flex-shrink: 0;
}

.pill-icon-img--stuck,
.pill-icon-img--queued {
  width: 14px;
  height: 14px;
  aspect-ratio: 1 / 1;
}

.pill-label {
  font: var(--typography-regular-body);
  color: var(--typography-color-primary);
  white-space: nowrap;
}

.pill-value {
  font: var(--typography-bold-body);
  color: var(--typography-color-primary);
}

.summary-pill--total .pill-label,
.summary-pill--total .pill-value,
.summary-pill--stuck .pill-label,
.summary-pill--stuck .pill-value,
.summary-pill--queued .pill-label,
.summary-pill--queued .pill-value {
  font: var(--typography-regular-label);
  color: var(--typography-color-primary);
}

.summary-pill--threads .pill-label,
.summary-pill--storage .pill-label {
  font: var(--typography-regular-label);
  color: var(--typography-color-primary);
}

.summary-pill--threads .pill-value,
.summary-pill--storage .pill-value {
  font: var(--typography-regular-label);
  color: var(--typography-color-secondary, #4a5565);
  text-align: right;
  width: 66px;
  flex-shrink: 0;
}

.thread-progress-bar {
  transform: translateY(-2px);
}

/* Card-view breakpoint (matches BREAKPOINT.CARD_VIEW in AdminDashboardView.vue): this
   row must always wrap onto its own line below "User Type". Natural flex-wrap isn't
   reliable for that at every width in this range (e.g. 820px can still fit both on one
   line), so flex-basis: 100% forces the wrap unconditionally. */
@media (max-width: 1025px) {
  .resource-summary {
    flex-basis: 100%;
    padding-left: 0;
    padding-right: 0;
    /* Avoids stacking with .filter-row's margin-bottom (AdminDashboardView.vue). */
    padding-bottom: 0;
    /* margin-top above only aligns this row with the User Type <select> when they
       share a line; here they're always stacked, so drop it to keep normal rhythm. */
    margin-top: 0;
  }
}


</style>
