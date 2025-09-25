<template>
  <div
    class="ml-2 mr-2"
    :style="outputStyle"
  >
    {{ formattedData }}
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { CONSTANTS } from '@/constants'

const props = defineProps({
  data: {
    type: Array,
    required: true,
  },
  tabname: {
    type: String,
    required: false,
  },
})

const formattedData = computed(() => props.data.join('\n'))

const outputStyle = computed(() => {
  const baseStyle = {
    whiteSpace: 'pre' as const,
    fontFamily: "'Courier New', Courier, monospace",
    overflowY: 'scroll' as const,
    fontSize: '14px',
    lineHeight: '1.5',
    overflowX: 'auto' as const,
    border: '1px solid #ccc',
    padding: '10px',
    backgroundColor: '#f9f9f9',
  }

  // For MODEL_REPORT tab, use dynamic height to fill available space
  if (props.tabname === CONSTANTS.REPORTING_TAB.MODEL_REPORT) {
    return {
      ...baseStyle,
      minHeight: '420px',
    }
  }

  // For other tabs, maintain the original fixed height
  return {
    ...baseStyle,
    height: '420px',
  }
})
</script>
