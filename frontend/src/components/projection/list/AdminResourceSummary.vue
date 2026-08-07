<template>
  <div class="resource-summary">
    <div class="summary-pill summary-pill--total">
      <span class="pill-icon" aria-hidden="true">
        <img :src="ChartLineIcon" alt="" class="pill-icon-img" />
      </span>
      <span class="pill-label">Total Running:</span>
      <span class="pill-value">{{ totalRunning }}</span>
    </div>

    <div class="summary-pill summary-pill--stuck">
      <span class="pill-icon" aria-hidden="true">
        <img :src="ExclamationMarkIcon" alt="" class="pill-icon-img pill-icon-img--stuck" />
      </span>
      <span class="pill-label">Stuck:</span>
      <span class="pill-value">{{ stuckCount }}</span>
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
import { ChartLineIcon, ExclamationMarkIcon } from '@/assets'

defineProps<{
  totalRunning: number
  threadsInUse: number
  threadCapacity: number
  threadUsagePercent: number
  stuckCount: number
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

.summary-pill--total,
.summary-pill--stuck {
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
  gap: 6px;
  min-width: 189px;
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

.pill-icon-img--stuck {
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
.summary-pill--stuck .pill-value {
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

/* Below the card-view breakpoint (matches BREAKPOINT.CARD_VIEW in
   AdminDashboardView.vue), this row must always sit on its own line below
   "User Type", never beside it. Relying on flex-wrap's organic wrapping
   (i.e. only wrapping once "User Type" + this row's natural width no longer
   fit the line) is not reliable across the whole range: at some widths in
   this range (e.g. 820px) there's just enough spare room for both to still
   fit on one line, which also isn't future-proof once System Storage (a
   later ticket) adds a 4th item here and further changes the natural width.
   flex-basis: 100% forces this row to claim the full line width, which
   guarantees flex-wrap pushes it onto its own line for every width in this
   range regardless of its actual content width. */
@media (max-width: 1025px) {
  .resource-summary {
    flex-basis: 100%;
    padding-left: 0;
    padding-right: 0;
    /* This 20px bottom padding stacked with .filter-row's margin-bottom
       (AdminDashboardView.vue) to push the next row further away than
       intended, so it's dropped here. */
    padding-bottom: 0;
    /* The margin-top above only exists to line this row up with the User
       Type select box when they share a row. In the card-view range the
       User Type field and this row are always stacked (one per line), so
       the offset is unwanted here — it would otherwise inflate the User
       Type -> Total Running gap beyond the rhythm used elsewhere in this
       column. */
    margin-top: 0;
  }
}


</style>
