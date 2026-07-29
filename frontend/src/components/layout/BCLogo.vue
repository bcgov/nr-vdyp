<template>
  <router-link :to="ROUTE_PATH.PROJECTION_LIST" class="bcds-logo-container" aria-label="Go to My Projections">
    <img
      alt="B.C. Government Logo"
      :src="bcLogo"
      class="bcds-logo"
    />
  </router-link>
</template>

<script setup lang="ts">
import bcLogo from '@/assets/BCID_H_rgb_pos.svg'
import { ROUTE_PATH } from '@/constants/constants'
</script>

<style scoped>
.bcds-logo-container {
  display: inline-block;
  line-height: 0;
}

.bcds-logo {
  min-width: 129px;
  width: 129px;
  display: block;
}

@media (max-width: 480px) {
  .bcds-logo {
    min-width: 100px;
    width: 100px;
  }
}

@media (max-width: 360px) {
  .bcds-logo {
    min-width: 80px;
    width: 80px;
  }
}
</style>
