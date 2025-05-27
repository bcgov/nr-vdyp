<template>
  <div class="top-project-year mt-3">
    <h1 class="top-project">{{ title }}</h1>
    <span class="top-year">Year: {{ fiscalYear }}</span>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'

defineProps({
  title: {
    type: String,
    default: 'Projects',
  },
})

const fiscalYear = ref('2024/2025')

const calculateFiscalYear = (date: Date): string => {
  const year = date.getFullYear()
  const month = date.getMonth() // 0-11 (January is 0, March is 2, April is 3)

  // Fiscal year changes on April 1st
  if (month >= 3) {
    // April (3) or later
    return `${year}/${year + 1}`
  } else {
    return `${year - 1}/${year}`
  }
}

onMounted(() => {
  const currentDate = new Date()
  fiscalYear.value = calculateFiscalYear(currentDate)
})
</script>
<style scoped>
.top-project-year {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  padding-bottom: 60px;
}

.top-project {
  font-size: 24px;
  font-weight: bold;
}

.top-year {
  font-size: 24px;
  font-weight: bold;
}
</style>
