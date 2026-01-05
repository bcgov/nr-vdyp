<template>
  <span class="bcds-header-link">
    {{ text }}
  </span>
</template>

<script setup lang="ts">
defineProps({
  text: {
    type: String,
    default: 'Training and Support',
  },
})
</script>

<style scoped>
.bcds-header-link {
  font-family: var(--typography-font-families-bc-sans);
  font-weight: var(--typography-font-weights-regular);
  font-size: var(--typography-font-size-body);
  line-height: var(--typography-line-heights-xdense);
  color: var(--typography-color-primary);
  white-space: nowrap;
}
</style>
