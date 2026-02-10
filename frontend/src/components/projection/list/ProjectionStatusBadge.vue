<template>
  <div class="status-badge">
    <img
      :src="getStatusIcon(status)"
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

interface Props {
  status: string
}

const props = defineProps<Props>()

const statusClass = computed(() => {
  return `status-${props.status.toLowerCase()}`
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
</style>
