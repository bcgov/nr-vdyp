<template>
  <v-toolbar-title
    :text="text"
    :class="!style ? 'header-toolbar-title' : ''"
    :style="style"
  />
</template>

<script setup lang="ts">
defineProps({
  text: {
    type: String,
    default: 'VARIABLE DENSITY YIELD PROJECTION',
  },
  style: {
    type: Object,
    default: null,
  },
})
</script>

<style scoped>
.header-toolbar-title {
  text-align: center;
  flex: 1;
  font-weight: 300;
}
</style>
