<template>
  <img
    alt="B.C. Government Logo"
    :src="bcLogo"
    class="bcds-logo"
  />
</template>

<script setup lang="ts">
import bcLogo from '@/assets/BCID_H_rgb_pos.svg'
</script>

<style scoped>
.bcds-logo {
  min-width: 129px;
  width: 129px;
}
</style>
