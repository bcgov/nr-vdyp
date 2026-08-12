<template>
  <div class="status-badge">
    <img
      :src="statusIcon"
      :alt="status"
      class="status-icon"
    />
    <span :class="['status-text', statusClass]">
      {{ status }}
    </span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { getStatusIcon } from '@/utils/util'
import { CONSTANTS } from '@/constants'
import { AdminCancelledIcon16px, QueuedIcon16px, StuckIcon16px } from '@/assets/'

interface Props {
  status: string
}

const props = defineProps<Props>()

// This badge always renders its icon at 16px, so the dedicated 16px assets are used directly
// here rather than the larger assets getStatusIcon() returns for the projection detail header badge.
const statusIcon16pxOverrides: Record<string, string> = {
  [CONSTANTS.PROJECTION_STATUS.ADMN_CNCLD]: AdminCancelledIcon16px,
  [CONSTANTS.PROJECTION_STATUS.QUEUED]: QueuedIcon16px,
  [CONSTANTS.PROJECTION_STATUS.STUCK]: StuckIcon16px,
}

const statusIcon = computed(() => statusIcon16pxOverrides[props.status] ?? getStatusIcon(props.status))

const statusClass = computed(() => {
  return `status-${props.status.toLowerCase().replace(/\s+/g, '-')}`
})
</script>

<style scoped>
.status-badge {
  display: flex;
  align-items: center;
  gap: var(--layout-padding-xsmall);
}

.status-icon {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
  image-rendering: -webkit-optimize-contrast;
  image-rendering: crisp-edges;
}

.status-text {
  font: var(--typography-regular-body);
}

.status-text.status-draft {
  font: var(--typography-bold-body);
  color: var(--typography-color-secondary);
}

.status-text.status-ready {
  font: var(--typography-bold-body);
  color: var(--support-border-color-success);
}

.status-text.status-failed {
  font: var(--typography-bold-body);
  color: var(--support-border-color-error);
}

.status-text.status-running {
  font: var(--typography-bold-body);
  color: var(--support-border-color-warning);
}

.status-text.status-queued {
  font: var(--typography-bold-body);
  color: var(--support-border-color-warning);
}
</style>
