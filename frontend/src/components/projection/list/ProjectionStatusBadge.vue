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
import {
  AdminCancelledIcon16px,
  QueuedIcon16px,
  StuckIcon16px,
  DraftIcon16px,
  ReadyIcon16px,
  RunningIcon16px,
  FailedIcon16px,
} from '@/assets/'

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
  [CONSTANTS.PROJECTION_STATUS.DRAFT]: DraftIcon16px,
  [CONSTANTS.PROJECTION_STATUS.READY]: ReadyIcon16px,
  [CONSTANTS.PROJECTION_STATUS.RUNNING]: RunningIcon16px,
  [CONSTANTS.PROJECTION_STATUS.FAILED]: FailedIcon16px,
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
  color: #279D14;
}

.status-text.status-failed {
  font: var(--typography-bold-body);
  color: #CE3E39;
}

.status-text.status-running {
  font: var(--typography-bold-body);
  color: #FCBA19;
}

.status-text.status-queued {
  font: var(--typography-bold-body);
  color: var(--typography-color-placeholder);
}

.status-text.status-stuck {
  font: var(--typography-bold-body);
  color: #CE3E39;
}

.status-text.status-cancelled-by-administrator {
  font: var(--typography-bold-body);
  color: #CE3E39;
}
</style>
