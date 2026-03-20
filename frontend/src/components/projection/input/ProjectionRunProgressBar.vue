<template>
  <div class="run-progress-container">
    <!-- 4 Status Tiles -->
    <div class="tiles-row">
      <!-- Status Tile -->
      <div class="tile tile--status">
        <span class="tile-label">Status</span>
        <div class="tile-value-row">
          <img :src="statusIcon20px" alt="" class="tile-icon" />
          <span class="tile-value" :class="statusValueClass">{{ statusText }}</span>
        </div>
      </div>

      <!-- Time Elapsed Tile -->
      <div class="tile tile--time">
        <span class="tile-label">Time Elapsed</span>
        <div class="tile-value-row">
          <img :src="TimeElapsedIcon" alt="" class="tile-icon" />
          <span class="tile-value">{{ formattedTimeElapsed }}</span>
        </div>
      </div>

      <!-- Polygons Processed Tile -->
      <div class="tile tile--polygons">
        <span class="tile-label">Polygons Processed</span>
        <div class="tile-value-row">
          <img :src="PolygonsProcessedIcon" alt="" class="tile-icon" />
          <span class="tile-value">{{ polygonsDisplay }}</span>
        </div>
      </div>

      <!-- Error Count Tile -->
      <div class="tile tile--errors">
        <span class="tile-label">Error Count</span>
        <div class="tile-value-row">
          <img :src="ErrorCountIcon" alt="" class="tile-icon" />
          <span class="tile-value">{{ formatNumber(errorCount) }}</span>
        </div>
      </div>
    </div>

    <!-- Progress Bar Section -->
    <div class="progress-section">
      <div class="progress-labels">
        <span class="progress-left-text">{{ progressLeftText }}</span>
        <span class="progress-right-text">{{ progressRightText }}</span>
      </div>
      <div class="progress-track">
        <div
          class="progress-fill"
          :class="progressFillClass"
          :style="{ width: `${progressPercent}%` }"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { CONSTANTS } from '@/constants'
import { formatNumber } from '@/utils/util'
import {
  TimeElapsedIcon,
  PolygonsProcessedIcon,
  ErrorCountIcon,
  RunningIcon20px,
  ReadyIcon20px,
  FailedIcon20px,
  DraftIcon20px,
  CancelIcon20px,
} from '@/assets/'

const props = defineProps<{
  status: string
  polygonCount: number | null
  completedPolygonCount: number | null
  errorCount: number | null
  startDate: string | null
  endDate: string | null
}>()

// Timer for updating elapsed time display
const currentTime = ref(Date.now())
let clockTimer: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  clockTimer = setInterval(() => {
    currentTime.value = Date.now()
  }, 60_000) // refresh Time Elapsed display every 1 minute
})

onUnmounted(() => {
  if (clockTimer !== null) {
    clearInterval(clockTimer)
  }
})

const statusIcon20px = computed(() => {
  const map: Record<string, string> = {
    [CONSTANTS.PROJECTION_STATUS.RUNNING]: RunningIcon20px,
    [CONSTANTS.PROJECTION_STATUS.READY]: ReadyIcon20px,
    [CONSTANTS.PROJECTION_STATUS.FAILED]: FailedIcon20px,
    [CONSTANTS.PROJECTION_STATUS.CANCELLED]: CancelIcon20px,
  }
  return map[props.status] ?? DraftIcon20px
})

const statusText = computed(() => props.status)

const statusValueClass = computed(() => {
  const map: Record<string, string> = {
    [CONSTANTS.PROJECTION_STATUS.RUNNING]: 'tile-value--running',
    [CONSTANTS.PROJECTION_STATUS.READY]: 'tile-value--ready',
    [CONSTANTS.PROJECTION_STATUS.FAILED]: 'tile-value--failed',
    [CONSTANTS.PROJECTION_STATUS.CANCELLED]: 'tile-value--cancelled',
  }
  return map[props.status] ?? ''
})

const formattedTimeElapsed = computed(() => {
  if (!props.startDate) return '-'
  const startMs = new Date(props.startDate).getTime()
  if (Number.isNaN(startMs)) return '-'

  const upperBoundMs = props.endDate ? new Date(props.endDate).getTime() : currentTime.value
  const elapsedMs = Math.max(0, upperBoundMs - startMs)
  const totalMinutes = Math.floor(elapsedMs / 60_000)
  const totalHours = Math.floor(totalMinutes / 60)
  const days = Math.floor(totalHours / 24)
  const hours = totalHours % 24
  const minutes = totalMinutes % 60

  const parts: string[] = []
  if (days > 0) parts.push(`${days}d`)
  if (totalHours > 0 || days > 0) parts.push(`${hours}h`)
  parts.push(`${minutes}m`)
  return parts.join(' ')
})

const polygonsDisplay = computed(() => {
  const completed = formatNumber(props.completedPolygonCount)
  const total = formatNumber(props.polygonCount)
  return `${completed}/${total}`
})

const progressPercent = computed(() => {
  // Ready means the run finished - fill the bar completely regardless of error count
  if (props.status === CONSTANTS.PROJECTION_STATUS.READY) return 100
  const total = props.polygonCount
  const completed = props.completedPolygonCount
  if (!total || total === 0) return 0
  return Math.min(100, Math.round(((completed ?? 0) / total) * 100))
})

const progressFillClass = computed(() => {
  if (
    props.status === CONSTANTS.PROJECTION_STATUS.FAILED ||
    props.status === CONSTANTS.PROJECTION_STATUS.CANCELLED
  ) {
    return 'progress-fill--red'
  }
  return 'progress-fill--green'
})

const progressLeftText = computed(() => {
  const map: Record<string, string> = {
    [CONSTANTS.PROJECTION_STATUS.RUNNING]: 'Running Projection Model...',
    [CONSTANTS.PROJECTION_STATUS.READY]: 'Projection Complete',
    [CONSTANTS.PROJECTION_STATUS.FAILED]: 'Projection Run Failed',
    [CONSTANTS.PROJECTION_STATUS.CANCELLED]: 'Projection Run Cancelled',
  }
  return map[props.status] ?? ''
})

const progressRightText = computed(() => {
  if (props.status === CONSTANTS.PROJECTION_STATUS.READY) return 'Done'
  return `${progressPercent.value}% Complete`
})
</script>

<style scoped>
.run-progress-container {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

/* Tiles */

.tiles-row {
  display: flex;
  gap: 10px;
}

.tile {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 10px;
  border-radius: 4px;
  border: 1px solid;
  min-width: 0;
}

.tile--status {
  background-color: #f7f9fc;
  border-color: #9f9d9c;
}

.tile--time {
  background-color: #fef0d8;
  border-color: #facc75;
}

.tile--polygons {
  background-color: #d8eafd;
  border-color: #7ab8f9;
}

.tile--errors {
  background-color: #f4e1e2;
  border-color: #d14a46;
}

.tile-label {
  font-size: 14pt;
  font-weight: 400;
  color: #2d2d2d;
  white-space: nowrap;
}

.tile-value-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.tile-icon {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
  image-rendering: -webkit-optimize-contrast;
  image-rendering: crisp-edges;
}

.tile-value {
  font-size: 16pt;
  font-weight: 700;
  color: #2d2d2d;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tile-value--running {
  color: #c27f00;
}

.tile-value--ready {
  color: #2e7d32;
}

.tile-value--failed {
  color: #d14a46;
}

.tile-value--cancelled {
  color: #2d2d2d;
}

/* Progress Bar */

.progress-section {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 10px;
  border: 1px solid #d8d8d8;
  border-radius: 4px;
}

.progress-labels {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.progress-left-text {
  font-size: 14pt;
  font-weight: 700;
  color: #2d2d2d;
}

.progress-right-text {
  font-size: 14pt;
  font-weight: 400;
  color: #2d2d2d;
}

.progress-track {
  width: 100%;
  height: 14px;
  background-color: #e0e0e0;
  border-radius: 7px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: 7px;
  transition: width 0.4s ease;
}

.progress-fill--green {
  background-color: #42814a;
}

.progress-fill--red {
  background-color: #d14a46;
}

/* Responsive */

@media (max-width: 720px) {
  .tiles-row {
    flex-wrap: wrap;
  }

  .tile {
    flex: 1 1 calc(50% - 5px);
    min-width: calc(50% - 5px);
  }
}

@media (min-width: 721px) and (max-width: 830px) {
  .tile-label {
    font-size: 11pt;
  }

  .tile-value {
    font-size: 12pt;
  }
}

@media (max-width: 480px) {
  .tile-label {
    font-size: 10pt;
  }

  .tile-value {
    font-size: 10pt;
  }

  .progress-left-text,
  .progress-right-text {
    font-size: 11pt;
  }
}
</style>
