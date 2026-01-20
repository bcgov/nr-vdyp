<template>
  <span class="bcds-header--title">
    {{ text }}
  </span>
</template>

<script setup lang="ts">
defineProps({
  text: {
    type: String,
    default: 'Variable Density Yield Projection',
  },
})
</script>

<style scoped>
.bcds-header--title {
  font-family: var(--typography-font-families-bc-sans);
  font-weight: var(--typography-font-weights-bold);
  font-size: var(--typography-font-size-h4);
  line-height: var(--typography-line-heights-sparse);
  color: var(--typography-color-primary);
}
</style>
