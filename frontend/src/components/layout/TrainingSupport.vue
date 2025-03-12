<template>
  <span :class="computedClass" :style="computedStyle">
    {{ text }}
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps({
  text: {
    type: String,
    default: 'Training and Support',
  },
  customStyle: {
    type: Object,
    default: () => ({}),
  },
})

const computedClass = computed(() => {
  return Object.keys(props.customStyle).length === 0
    ? 'header-training-support'
    : ''
})

const computedStyle = computed(() => {
  return Object.keys(props.customStyle).length === 0 ? {} : props.customStyle
})
</script>

<style scoped>
.header-training-support {
  font-size: 14px;
}
</style>
