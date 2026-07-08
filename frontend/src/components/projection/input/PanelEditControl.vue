<template>
  <div v-if="!isReadOnly" class="panel-edit-control">
    <div v-if="editable" class="editing-badge">
      <span class="editing-badge-text">Editing</span>
    </div>
    <v-tooltip v-else :text="editTooltipText" :disabled="!editTooltipText" location="top">
      <template #activator="{ props: tooltipProps }">
        <div
          v-bind="tooltipProps"
          class="edit-btn"
          :class="{ 'edit-btn--disabled': !isHeaderEditActive }"
          @click="handleClick"
        >
          <v-icon class="edit-btn-icon">mdi-pencil-outline</v-icon>
          <span class="edit-btn-label">Edit</span>
        </div>
      </template>
    </v-tooltip>
  </div>
</template>

<script setup lang="ts">
const props = defineProps<{
  isReadOnly: boolean
  editable: boolean
  isHeaderEditActive: boolean
  editTooltipText: string
}>()

const emit = defineEmits<{ edit: [] }>()

const handleClick = (event: MouseEvent) => {
  event.stopPropagation()
  if (!props.editable && props.isHeaderEditActive) {
    emit('edit')
  }
}
</script>

<style scoped>
.panel-edit-control {
  flex: 0 0 auto;
  width: auto;
  max-width: 100%;
  display: flex;
  align-items: center;
}

.edit-btn {
  position: relative;
  z-index: 1;
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  cursor: pointer;
  border-radius: var(--layout-border-radius-medium);
  background-color: var(--surface-color-tertiary-button-default);
  color: var(--typography-color-primary);
}

.edit-btn:hover:not(.edit-btn--disabled) {
  background-color: var(--surface-color-tertiary-button-hover);
}

.edit-btn--disabled {
  cursor: not-allowed;
  color: var(--typography-color-disabled);
}

.edit-btn-icon {
  font-size: 18px;
  padding-top: 12px;
}

.edit-btn-label {
  font-size: 11px;
  font-weight: 400;
}

.editing-badge {
  margin-right: -2px;
  display: inline-flex;
  align-items: center;
  padding: 2px 12px;
  border-radius: 999px;
  background-color: #d6d6d6;
  color: var(--typography-color-primary);
}

.editing-badge-text {
  font-style: italic;
  font-weight: 500;
  font-size: 13px;
}
</style>
