<template>
  <span class="bcds-header-link">
    <span class="bcds-header-link-full">{{ fullText }}</span>
    <span class="bcds-header-link-short">{{ shortText }}</span>
  </span>
</template>

<script setup lang="ts">
defineProps({
  fullText: {
    type: String,
    default: 'Training and Support',
  },
  shortText: {
    type: String,
    default: 'Support',
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
  text-decoration: none;
}

/* Show full text by default, hide short text */
.bcds-header-link-full {
  display: inline;
}

.bcds-header-link-short {
  display: none;
}

/* At screen width < 920px, show short text and hide full text */
@media (max-width: 919px) {
  .bcds-header-link-full {
    display: none;
  }

  .bcds-header-link-short {
    display: inline;
  }
}
</style>
