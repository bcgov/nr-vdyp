<template>
  <router-link to="/" class="bcds-logo-link">
    <img
      alt="B.C. Government Logo"
      :src="bcLogo"
      class="bcds-logo"
    />
  </router-link>
</template>

<script setup lang="ts">
import bcLogo from '@/assets/BCID_H_rgb_pos.svg'
</script>

<style scoped>
.bcds-logo-link {
  display: inline-block;
  cursor: pointer;
  text-decoration: none;
  outline: none;
}

.bcds-logo-link:focus {
  outline: none;
}

.bcds-logo-link:focus-visible {
  outline: 2px solid var(--surface-color-border-active);
  outline-offset: 2px;
  border-radius: var(--layout-border-radius-small);
}

.bcds-logo {
  min-width: 129px;
  width: 129px;
  display: block;
}
</style>
