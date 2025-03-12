<template>
  <div class="top-project-year mt-3">
    <h1 class="top-project">{{ title }}</h1>
    <span class="top-year">Year: {{ year }}</span>
  </div>
</template>

<script setup lang="ts">
defineProps({
  title: {
    type: String,
    default: 'Projects',
  },
  year: {
    type: String,
    default: '2024/2025',
  },
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
