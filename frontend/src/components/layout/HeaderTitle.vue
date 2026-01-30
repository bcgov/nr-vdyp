<template>
  <span class="bcds-header--title">
    <span class="bcds-header--title-full">{{ fullText }}</span>
    <span class="bcds-header--title-short">{{ shortText }}</span>
  </span>
</template>

<script setup lang="ts">
defineProps({
  fullText: {
    type: String,
    default: 'Variable Density Yield Projection',
  },
  shortText: {
    type: String,
    default: 'VDYP',
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

/* Show full text by default, hide short text */
.bcds-header--title-full {
  display: inline;
}

.bcds-header--title-short {
  display: none;
}

/* At screen width < 920px, show short text and hide full text */
@media (max-width: 919px) {
  .bcds-header--title-full {
    display: none;
  }

  .bcds-header--title-short {
    display: inline;
  }
}
</style>
