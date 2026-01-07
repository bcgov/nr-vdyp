<template>
  <div v-if="isShow" :class="['progress-container', { 'with-background': hasBackground }]">
    <v-progress-circular
      indeterminate
      :size="circleSize"
      :width="circleWidth"
      :color="circleColor"
    ></v-progress-circular>
    <div v-if="showMessage" class="progress-message">
      {{ message }}
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps({
  isShow: { type: Boolean, default: false },
  showMessage: { type: Boolean, default: true },
  message: { type: String, default: 'Loading...' },
  circleSize: { type: Number, default: 70 },
  circleWidth: { type: Number, default: 5 },
  circleColor: { type: String, default: 'primary' },
  hasBackground: { type: Boolean, default: true },
})
</script>

<style scoped>
/*
 * Styling based on BC Gov Design Standards - Using Design Tokens
 */
.progress-container {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 9999;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--layout-margin-small);
}

.progress-container.with-background {
  background-color: var(--surface-color-background-white);
  border: var(--layout-border-width-small) solid var(--surface-color-border-default);
  border-radius: var(--layout-border-radius-medium);
  padding: var(--layout-padding-large);
  box-shadow: var(--surface-shadow-medium);
}

.progress-message {
  font: var(--typography-bold-body);
  color: var(--typography-color-primary);
  margin-top: var(--layout-margin-small);
}
</style>
