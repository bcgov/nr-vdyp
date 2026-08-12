<template>
  <div class="admin-cancellation-banner" role="alert">
    <p class="admin-cancellation-banner--text">{{ reason }}</p>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  reason: string
}>()
</script>

<style scoped>
.admin-cancellation-banner {
  padding: var(--layout-padding-medium) var(--layout-padding-large);
  background-color: var(--surface-color-background-white);
  border: var(--layout-border-width-small) solid var(--surface-color-border-default);
  border-radius: var(--layout-border-radius-medium);
}

.admin-cancellation-banner--text {
  font: var(--typography-regular-body);
  font-weight: bold;
  color: var(--typography-color-primary);
  margin: 0;
  overflow-wrap: anywhere;
}
</style>
