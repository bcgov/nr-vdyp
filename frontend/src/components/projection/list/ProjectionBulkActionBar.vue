<template>
  <div v-if="isVisible" class="bulk-action-bar">
    <button class="bulk-bar-close-btn" title="Clear selection" @click="$emit('close')">
      <v-icon size="16">mdi-close</v-icon>
    </button>

    <span class="bulk-bar-count">{{ selectedCount }} Selected</span>

    <div class="bulk-bar-divider" />

    <button
      class="bulk-bar-action-btn"
      title="Download"
      :disabled="!canDownload"
      :class="{ 'bulk-bar-action-btn--disabled': !canDownload }"
      @click="canDownload && $emit('download')"
    >
      <img :src="DownloadIcon" alt="Download" class="bulk-bar-icon" />
      <span>Download</span>
    </button>

    <button class="bulk-bar-action-btn" title="Duplicate" @click="$emit('duplicate')">
      <img :src="DuplicateIcon" alt="Duplicate" class="bulk-bar-icon" />
      <span>Duplicate</span>
    </button>

    <button
      class="bulk-bar-action-btn"
      title="Cancel"
      :disabled="!canCancel"
      :class="{ 'bulk-bar-action-btn--disabled': !canCancel }"
      @click="canCancel && $emit('cancel')"
    >
      <img :src="CancelIcon" alt="Cancel" class="bulk-bar-icon" />
      <span>Cancel</span>
    </button>

    <button
      class="bulk-bar-action-btn"
      title="Delete"
      :disabled="!canDelete"
      :class="{ 'bulk-bar-action-btn--disabled': !canDelete }"
      @click="canDelete && $emit('delete')"
    >
      <img :src="DeleteIcon" alt="Delete" class="bulk-bar-icon" />
      <span>Delete</span>
    </button>
  </div>
</template>

<script setup lang="ts">
import { DownloadIcon, DuplicateIcon, CancelIcon, DeleteIcon } from '@/assets/'

defineProps<{
  isVisible: boolean
  selectedCount: number
  canDownload: boolean
  canCancel: boolean
  canDelete: boolean
}>()

defineEmits<{
  (e: 'close'): void
  (e: 'download'): void
  (e: 'duplicate'): void
  (e: 'cancel'): void
  (e: 'delete'): void
}>()
</script>

<style scoped>
.bulk-action-bar {
  display: flex;
  align-items: center;
  gap: var(--layout-padding-medium);
  padding: 6px var(--layout-padding-medium);
  background-color: var(--theme-blue-10);
  border-radius: var(--layout-border-radius-medium);
  margin-bottom: var(--layout-margin-xsmall);
  margin-left: var(--layout-margin-large);
  box-sizing: border-box;
}

.bulk-bar-close-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 2px;
  border-radius: var(--layout-border-radius-small);
  color: var(--typography-color-primary);
  flex-shrink: 0;
  line-height: 1;
  transition: background-color 0.2s;
}

.bulk-bar-close-btn:hover {
  background-color: rgba(0, 0, 0, 0.08);
}

.bulk-bar-count {
  font: var(--typography-bold-small-body);
  color: var(--typography-color-primary);
  white-space: nowrap;
  flex-shrink: 0;
}

.bulk-bar-divider {
  width: 1px;
  height: 16px;
  background-color: var(--surface-color-border-dark);
  flex-shrink: 0;
}

.bulk-bar-action-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 3px var(--layout-padding-small);
  border-radius: var(--layout-border-radius-small);
  font: var(--typography-regular-small-body);
  color: var(--typography-color-primary);
  white-space: nowrap;
  flex-shrink: 0;
  transition: background-color 0.2s;
}

.bulk-bar-action-btn:hover:not(.bulk-bar-action-btn--disabled) {
  background-color: rgba(0, 0, 0, 0.08);
}

.bulk-bar-action-btn--disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.bulk-bar-action-btn--danger {
  color: var(--support-color-danger, #ce3e39);
}

.bulk-bar-icon {
  width: 14px;
  height: 14px;
  object-fit: contain;
  flex-shrink: 0;
}
</style>
