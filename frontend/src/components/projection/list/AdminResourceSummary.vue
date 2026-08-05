<template>
  <div class="resource-summary">
    <div class="summary-pill summary-pill--total">
      <span class="pill-icon" aria-hidden="true">
        <img :src="ChartLineIcon" alt="" class="pill-icon-img" />
      </span>
      <span class="pill-label">Total Running:</span>
      <span class="pill-value">{{ totalRunning }}</span>
    </div>

    <div class="summary-pill summary-pill--threads">
      <div class="pill-header">
        <span class="pill-label">Threads in Use</span>
        <span class="pill-value">{{ threadsInUse }}/ {{ threadCapacity }}</span>
      </div>
      <v-progress-linear
        :model-value="threadUsagePercent"
        color="var(--theme-primary-color, #003366)"
        bg-color="#E5E7EB"
        :bg-opacity="1"
        height="12"
        rounded="pill"
        class="thread-progress-bar"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ChartLineIcon } from '@/assets'

defineProps<{
  totalRunning: number
  threadsInUse: number
  threadCapacity: number
  threadUsagePercent: number
}>()
</script>

<style scoped>
.resource-summary {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 30px;
  padding: 0 15px 20px 24px;
  /* Offsets this row down to align with the User Type <select> box's top (not the
     "User Type" label above it): matches the label's own rendered height
     (font-size * line-height + padding-bottom) plus the filter-field gap that
     separates the label from the select, so it tracks those tokens if they change. */
  margin-top: calc(var(--typography-font-size-label) * 1.5 + 4px + var(--layout-padding-xsmall) - 3px);
  border-radius: 4px;
}

.summary-pill {
  background: var(--theme-gray-20, #f3f2f1);
  border-radius: var(--layout-borderRadius-circular, 9999px);
}

.summary-pill--total {
  display: inline-flex;
  height: 32px;
  padding: var(--layout-margin-hair, 2px) var(--layout-padding-medium, 16px);
  align-items: center;
  gap: var(--layout-margin-small, 8px);
}

.summary-pill--threads {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  padding: 0 var(--layout-padding-medium);
  gap: 6px;
  min-width: 220px;
  background: none;
  border-radius: 0;
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
.summary-pill--total .pill-value {
  font: var(--typography-regular-label);
  color: var(--typography-color-primary);
}

.summary-pill--threads .pill-label {
  font: var(--typography-regular-label);
  color: var(--typography-color-primary);
}

.summary-pill--threads .pill-value {
  font: var(--typography-regular-label);
  color: var(--typography-color-secondary, #4a5565);
  text-align: right;
  width: 66px;
  flex-shrink: 0;
}

.thread-progress-bar {
  transform: translateY(-2px);
}

/* On phone-width viewports, the left/right padding on this row and on the
   threads pill pushed both pills out of alignment with the "User Type"/
   "Sort By" fields to the left. Dropping that padding lets Total Running's
   left edge (and Threads in Use's, when it wraps to its own line) line up
   with those fields, and frees up enough width for both pills to fit on one
   line more often. */
@media (max-width: 540px) {
  .resource-summary {
    padding-left: 0;
    padding-right: 0;
    /* This 20px bottom padding stacked with .filter-row's 30px margin-bottom
       (AdminDashboardView.vue) to push Sort By ~50px away from Threads in
       Use instead of the intended 30px, so it's dropped here. */
    padding-bottom: 0;
    /* The margin-top above only exists to line this row up with the User
       Type select box when they share a row. Below 540px the User Type
       field and this row are always stacked (one per line), so the offset
       is unwanted here — it would otherwise inflate the User Type -> Total
       Running gap beyond the 30px rhythm used everywhere else in this
       column. */
    margin-top: 0;
  }

  .summary-pill--threads {
    padding-left: 0;
    padding-right: 0;
  }
}


</style>
