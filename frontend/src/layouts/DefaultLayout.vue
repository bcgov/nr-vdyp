<template>
   <div
    class="main-layout-container"
  >
    <slot />
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

console.log(`App Version: ${appVersion}`)
console.log(`Build Numer: ${BUILD_NUMBER}`)

</script>

<style scoped>
.main-layout-container {
  display: flex;
  flex-direction: column;
  flex: none;
  margin-bottom: 0px;
}
</style>
