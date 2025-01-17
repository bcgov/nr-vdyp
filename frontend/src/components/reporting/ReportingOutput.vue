<template>
  <div
    class="ml-2 mr-2"
    style="
      white-space: pre;
      font-family: 'Courier New', Courier, monospace;
      overflow-y: scroll;
      height: 420px;
      font-size: 14px;
      line-height: 1.5;
      overflow-x: auto;
      border: 1px solid #ccc;
      padding: 10px;
      background-color: #f9f9f9;
    "
  >
    {{ formattedData }}
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps({
  data: {
    type: Array,
    required: true,
  },
})

const formattedData = computed(() => props.data.join('\n'))
</script>
