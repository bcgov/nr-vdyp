<template>
  <div>
    <div class="progress-instruction-text">
      Complete the following sections to run the projections model. You may return to any section and make edits if needed.
    </div>
    <v-card class="progress-bar-card" elevation="0">
      <div class="progress-header">
        <span class="step-text">Step {{ completedCount }} of {{ totalSections }} Complete</span>
        <template v-if="showStatus">
          <div class="status-display">
            <img :src="statusIcon" :alt="projectionStatus" class="status-icon" />
            <span :class="statusTextClass">{{ projectionStatus }}</span>
          </div>
        </template>
        <template v-else>
          <span class="percentage-text">{{ percentage }}% Complete</span>
        </template>
      </div>
      <v-progress-linear
        :model-value="percentage"
        :color="progressColor"
        height="14"
        rounded
        class="progress-bar"
      />
      <div class="section-labels">
        <span
          v-for="(section, index) in sections"
          :key="index"
          class="section-label"
          :class="{ 'section-complete': section.completed }"
          :style="{ left: `${(index / sections.length) * 100}%` }"
        >
          {{ section.label }}
        </span>
      </div>
    </v-card>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { CONSTANTS } from '@/constants'
import {
  DraftStatusIcon,
  FailedStatusIcon,
  ReadyStatusIcon,
  RunningStatusIcon,
} from '@/assets/'

export interface ProgressSection {
  label: string
  completed: boolean
}

const props = defineProps<{
  sections: ProgressSection[]
  percentage: number
  completedCount: number
  projectionStatus: string
}>()

const totalSections = computed(() => props.sections.length)

const showStatus = computed(() => {
  const status = props.projectionStatus
  return (
    status === CONSTANTS.PROJECTION_STATUS.RUNNING ||
    status === CONSTANTS.PROJECTION_STATUS.READY ||
    status === CONSTANTS.PROJECTION_STATUS.FAILED
  )
})

const statusIcon = computed(() => {
  const iconMap: Record<string, string> = {
    Draft: DraftStatusIcon,
    Ready: ReadyStatusIcon,
    Running: RunningStatusIcon,
    Failed: FailedStatusIcon,
  }
  return iconMap[props.projectionStatus] || ''
})

const statusTextClass = computed(() => {
  const classMap: Record<string, string> = {
    Draft: 'status-text status-text--draft',
    Ready: 'status-text status-text--ready',
    Running: 'status-text status-text--running',
    Failed: 'status-text status-text--failed',
  }
  return classMap[props.projectionStatus] || 'status-text'
})

const progressColor = computed(() => {
  if (props.projectionStatus === CONSTANTS.PROJECTION_STATUS.FAILED) return 'error'
  if (props.projectionStatus === CONSTANTS.PROJECTION_STATUS.READY) return 'success'
  if (props.projectionStatus === CONSTANTS.PROJECTION_STATUS.RUNNING) return 'warning'
  return '#013366'
})
</script>

<style scoped>
.progress-bar-card {
  padding: var(--layout-padding-large);
  border: 1px solid var(--surface-color-border-default);
}

.progress-instruction-text {
  font: var(--typography-regular-body);
  color: var(--typography-color-primary);
  margin-bottom: var(--layout-margin-medium);
}

.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--layout-margin-small);
}

.step-text {
  font: var(--typography-bold-small-body);
  color: var(--theme-primary-blue);
}

.percentage-text {
  font: var(--typography-regular-small-body);
  color: var(--typography-color-secondary);
}

.status-display {
  display: flex;
  align-items: center;
  gap: var(--layout-padding-small);
}

.status-icon {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
  image-rendering: -webkit-optimize-contrast;
  image-rendering: crisp-edges;
}

.status-text {
  font-size: 14pt;
  font-weight: bold;
  line-height: 1;
}

.status-text--draft {
  color: var(--typography-color-secondary);
}

.status-text--ready {
  color: var(--support-border-color-success);
}

.status-text--running {
  color: var(--support-border-color-warning);
}

.status-text--failed {
  color: var(--support-border-color-danger);
}

.progress-bar {
  margin-bottom: var(--layout-margin-small);
}

.section-labels {
  position: relative;
  height: 1.5em;
}

.section-label {
  position: absolute;
  font: var(--typography-regular-small-body);
  font-weight: bold;
  color: var(--theme-primary-blue);
}

.section-complete {
  font-weight: bold;
  color: var(--theme-primary-blue);
}

@media (max-width: 600px) {
  .section-labels {
    display: flex;
    justify-content: space-between;
    height: auto;
  }

  .section-label {
    position: static;
    font-size: 11px;
  }
}
</style>
