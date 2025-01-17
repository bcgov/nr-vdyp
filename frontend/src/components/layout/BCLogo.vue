<template>
  <v-img
    alt="B.C. Government Logo"
    :lazy-src="bcLogo"
    :src="bcLogo"
    :style="{
      maxHeight: `${maxHeight}px`,
      maxWidth: `${maxWidth}px`,
      marginLeft: `${marginLeft}px`,
    }"
    id="header-bc-logo-img"
  ></v-img>
</template>

<script setup lang="ts">
import { defineProps } from 'vue'
import bcLogo from '@/assets/gov-bc-logo-horiz.png'

defineProps({
  maxHeight: {
    type: Number,
    default: 50,
  },
  maxWidth: {
    type: Number,
    default: 150,
  },
  marginLeft: {
    type: Number,
    default: 15,
  },
})
</script>

<style scoped>
#header-bc-logo-img {
  display: block;
}
</style>
