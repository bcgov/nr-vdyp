<template>
  <div class="pagination-container">
    <div class="pagination-info-left">
      Showing {{ paginationStart }} to {{ paginationEnd }} of
      {{ totalItems }}
    </div>
    <div class="pagination-center">
      <button
        class="pagination-arrow"
        :disabled="currentPage === 1"
        @click="goToPage(currentPage - 1)"
      >
        <v-icon>mdi-chevron-left</v-icon>
      </button>
      <button
        v-for="page in visiblePages"
        :key="page"
        class="pagination-number"
        :class="{ active: page === currentPage }"
        @click="goToPage(page)"
      >
        {{ page }}
      </button>
      <button
        class="pagination-arrow"
        :disabled="currentPage >= totalPages || totalPages === 0"
        @click="goToPage(currentPage + 1)"
      >
        <v-icon>mdi-chevron-right</v-icon>
      </button>
    </div>
    <div class="pagination-controls">
      <span class="pagination-label">Show</span>
      <v-select
        :model-value="itemsPerPage"
        :items="itemsPerPageOptions"
        variant="outlined"
        density="compact"
        class="items-per-page-select"
        hide-details
        @update:model-value="updateItemsPerPage"
      ></v-select>
      <span class="pagination-label">entries</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { itemsPerPageOptions as defaultItemsPerPageOptions } from '@/constants/options'

interface Props {
  currentPage: number
  itemsPerPage: number
  totalItems: number
  itemsPerPageOptions?: number[]
}

const props = withDefaults(defineProps<Props>(), {
  itemsPerPageOptions: () => defaultItemsPerPageOptions,
})

const emit = defineEmits<{
  (e: 'update:currentPage', value: number): void
  (e: 'update:itemsPerPage', value: number): void
}>()

const totalPages = computed(() =>
  Math.ceil(props.totalItems / props.itemsPerPage),
)

const paginationStart = computed(
  () => (props.currentPage - 1) * props.itemsPerPage + 1,
)

const paginationEnd = computed(() =>
  Math.min(props.currentPage * props.itemsPerPage, props.totalItems),
)

const generatePageRange = (start: number, end: number): number[] => {
  const pages: number[] = []
  for (let i = start; i <= end; i++) {
    pages.push(i)
  }
  return pages
}

const visiblePages = computed(() => {
  const total = totalPages.value
  const current = props.currentPage
  const ELLIPSIS = -1

  if (total <= 7) {
    return generatePageRange(1, total)
  }

  if (current <= 4) {
    return [...generatePageRange(1, 5), ELLIPSIS, total]
  }

  if (current >= total - 3) {
    return [1, ELLIPSIS, ...generatePageRange(total - 4, total)]
  }

  return [1, ELLIPSIS, ...generatePageRange(current - 1, current + 1), ELLIPSIS, total]
})

const goToPage = (page: number) => {
  if (page >= 1 && page <= totalPages.value && page !== props.currentPage) {
    emit('update:currentPage', page)
  }
}

const updateItemsPerPage = (value: number) => {
  emit('update:itemsPerPage', value)
}
</script>

<style scoped>
.pagination-container {
  margin-top: var(--layout-margin-small);
  padding-top: var(--layout-padding-medium);
  border-top: 1px solid var(--surface-color-border-default);
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--layout-margin-medium);
}

.pagination-info-left {
  font: var(--typography-regular-body);
  color: var(--typography-color-secondary);
  order: 1;
}

.pagination-center {
  display: flex;
  align-items: center;
  gap: var(--layout-padding-xsmall);
  order: 2;
}

.pagination-arrow,
.pagination-number {
  background: var(--surface-color-forms-default);
  border: 1px solid var(--surface-color-border-default);
  padding: var(--layout-padding-xsmall) var(--layout-padding-small);
  cursor: pointer;
  border-radius: var(--layout-border-radius-small);
  font: var(--typography-regular-body);
  color: var(--typography-color-primary);
  min-width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.pagination-arrow:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.pagination-number:hover:not(.active),
.pagination-arrow:hover:not(:disabled) {
  background-color: var(--surface-color-background-light-gray);
}

.pagination-number.active {
  background: var(--surface-color-primary-button-default);
  color: var(--icons-color-primary-invert);
  border-color: var(--surface-color-primary-button-default);
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: var(--layout-padding-small);
  order: 3;
}

.pagination-label {
  font: var(--typography-regular-body);
  color: var(--typography-color-primary);
}

.items-per-page-select {
  width: 80px;
}
</style>
