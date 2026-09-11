<template>
  <div class="main-layout-container">
    <slot />
    <div
      v-if="appVersion"
      class="vdyp-version-info"
    >
      VDYP Version: {{ appVersion }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { APP_VERSION } from '@/constants/appVersion'
import { BUILD_NUMBER } from '@/constants/buildNumber'

let appVersion = ''
if (APP_VERSION) {
  const baseVersion = APP_VERSION.replace(
    /(-snapshot|-SNAPSHOT|-Snapshot)/i,
    '',
  )
  appVersion = `${baseVersion}.${BUILD_NUMBER || ''}`
}

console.info(`Build Numer: ${BUILD_NUMBER}`)
</script>

<style scoped>
.main-layout-container {
  display: flex;
  flex-direction: column;
  flex: none;
  margin-bottom: 0px;
  min-width: 0;
  width: 100%;
  overflow-x: hidden;
}

.vdyp-version-info {
  color: #2D2D2D;
  font-family: var(--typography-font-families-bc-sans);
  font-size: 14px;
  padding: 4px 16px 8px 16px;
  text-align: left;
}
</style>
