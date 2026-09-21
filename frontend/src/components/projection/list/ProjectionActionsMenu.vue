<template>
  <v-menu>
    <template #activator="{ props }">
      <button
        v-bind="props"
        class="action-menu-button"
        :title="`Actions for ${title}`"
      >
        <v-icon>mdi-dots-vertical</v-icon>
      </button>
    </template>
    <v-list class="action-menu-list">
      <!-- Run: for Draft; disabled with a tooltip until all parameters are specified -->
      <v-tooltip
        v-if="status === PROJECTION_STATUS.DRAFT"
        :disabled="canRun"
        :text="RUN_DISABLED_TOOLTIP"
        max-width="260"
        location="left"
      >
        <template #activator="{ props: tooltipProps }">
          <div v-bind="tooltipProps">
            <v-list-item
              class="action-menu-item run-menu-item"
              :class="{ 'action-menu-item--disabled': !canRun }"
              :disabled="!canRun"
              @click="canRun && $emit(PROJECTION_USER_ACTION.RUN)"
            >
              <div class="menu-item-content">
                <img
                  src="@/assets/icons/Running_Icon_16px.png"
                  alt="Run"
                  class="menu-icon"
                />
                <span class="menu-text">Run</span>
              </div>
            </v-list-item>
          </div>
        </template>
      </v-tooltip>

      <!-- View: only for Ready -->
      <v-list-item
        v-if="status === PROJECTION_STATUS.READY"
        class="action-menu-item"
        @click="$emit(PROJECTION_USER_ACTION.VIEW)"
      >
        <div class="menu-item-content">
          <img
            src="@/assets/icons/View_Icon_Menu.png"
            alt="View"
            class="menu-icon"
          />
          <span class="menu-text">View</span>
        </div>
      </v-list-item>

      <!-- Edit: for Draft, Failed, and Cancelled By Administrator -->
      <v-list-item
        v-if="status === PROJECTION_STATUS.DRAFT || status === PROJECTION_STATUS.FAILED || status === PROJECTION_STATUS.ADMN_CNCLD"
        class="action-menu-item"
        @click="$emit(PROJECTION_USER_ACTION.EDIT)"
      >
        <div class="menu-item-content">
          <img
            src="@/assets/icons/Edit_Icon_Menu.png"
            alt="Edit"
            class="menu-icon"
          />
          <span class="menu-text">Edit</span>
        </div>
      </v-list-item>

      <!-- Duplicate: for Draft, Failed, Ready, Cancelled By Administrator -->
      <v-list-item
        v-if="status === PROJECTION_STATUS.DRAFT || status === PROJECTION_STATUS.FAILED || status === PROJECTION_STATUS.READY || status === PROJECTION_STATUS.ADMN_CNCLD"
        class="action-menu-item"
        @click="$emit(PROJECTION_USER_ACTION.DUPLICATE)"
      >
        <div class="menu-item-content">
          <img
            src="@/assets/icons/Duplicate_Icon_Menu.png"
            alt="Duplicate"
            class="menu-icon"
          />
          <span class="menu-text">Duplicate</span>
        </div>
      </v-list-item>

      <!-- Download: for Failed and Ready -->
      <v-list-item
        v-if="status === PROJECTION_STATUS.FAILED || status === PROJECTION_STATUS.READY"
        class="action-menu-item"
        @click="$emit(PROJECTION_USER_ACTION.DOWNLOAD)"
      >
        <div class="menu-item-content">
          <img
            src="@/assets/icons/Download_Icon_Menu.png"
            alt="Download"
            class="menu-icon"
          />
          <span class="menu-text">Download</span>
        </div>
      </v-list-item>

      <!-- Cancel: only for Running -->
      <v-list-item
        v-if="status === PROJECTION_STATUS.RUNNING || status === PROJECTION_STATUS.STUCK || status === PROJECTION_STATUS.QUEUED"
        class="action-menu-item"
        @click="$emit(PROJECTION_USER_ACTION.CANCEL)"
      >
        <div class="menu-item-content">
          <img
            src="@/assets/icons/Cancel_Icon_Menu.png"
            alt="Cancel"
            class="menu-icon"
          />
          <span class="menu-text">Cancel</span>
        </div>
      </v-list-item>

      <!-- Delete: for Draft, Failed, Ready, Cancelled By Administrator (not Running) -->
      <v-list-item
        v-if="status === PROJECTION_STATUS.DRAFT || status === PROJECTION_STATUS.FAILED || status === PROJECTION_STATUS.READY || status === PROJECTION_STATUS.ADMN_CNCLD"
        class="action-menu-item danger"
        @click="$emit(PROJECTION_USER_ACTION.DELETE)"
      >
        <div class="menu-item-content">
          <img
            src="@/assets/icons/Delete_Icon_Menu.png"
            alt="Delete"
            class="menu-icon"
          />
          <span class="menu-text">Delete</span>
        </div>
      </v-list-item>
    </v-list>
  </v-menu>
</template>

<script setup lang="ts">
import type { ProjectionStatus } from '@/interfaces/interfaces'
import { PROJECTION_STATUS, PROJECTION_USER_ACTION } from '@/constants/constants'

import { RUN_DISABLED_TOOLTIP } from '@/constants/message'

withDefaults(
  defineProps<{
    status: ProjectionStatus
    title: string
    canRun?: boolean
  }>(),
  { canRun: false },
)

defineEmits<{
  run: []
  view: []
  edit: []
  duplicate: []
  download: []
  cancel: []
  delete: []
}>()
</script>

<style scoped>
.action-menu-button {
  background: transparent;
  border: none;
  padding: var(--layout-padding-xsmall);
  cursor: pointer;
  border-radius: var(--layout-border-radius-small);
  transition: background-color 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--typography-color-primary);
}

.action-menu-button:hover {
  background-color: var(--surface-color-background-light-gray);
}

.action-menu-button:focus {
  outline: 2px solid var(--surface-color-border-active);
  outline-offset: 2px;
}

.action-menu-list {
  min-width: 160px;
}

.action-menu-item {
  cursor: pointer;
}

.action-menu-item:hover {
  background-color: #eceae8;
}

.action-menu-item--disabled,
.action-menu-item--disabled:hover {
  background-color: transparent;
  cursor: not-allowed;
}

.action-menu-item--disabled .menu-icon,
.action-menu-item--disabled .menu-text {
  opacity: 0.4;
}

.menu-item-content {
  display: flex;
  align-items: center;
  gap: var(--layout-padding-medium);
  padding-left: var(--layout-padding-small);
}

.menu-icon {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
  object-fit: contain;
}

.menu-text {
  font: var(--typography-regular-body);
  color: var(--typography-color-primary);
}
</style>
