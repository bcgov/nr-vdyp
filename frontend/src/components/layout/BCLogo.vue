<template>
  <div class="bcds-logo-container">
    <img
      alt="B.C. Government Logo"
      :src="bcLogo"
      class="bcds-logo"
    />
  </div>
</template>

<script setup lang="ts">
import bcLogo from '@/assets/BCID_H_rgb_pos.svg'
</script>

<style scoped>
.bcds-logo-container {
  display: inline-block;
}

.bcds-logo {
  min-width: 129px;
  width: 129px;
  display: block;
}
</style>
