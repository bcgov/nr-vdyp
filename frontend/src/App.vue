<template>
  <v-app class="rounded rounded-md">
    <TheHeader />

    <v-main id="main">
      <AppNotification />
      <AppConfirmDialog />
      <AppDialogDragger />

      <DefaultLayout>
        <router-view />
      </DefaultLayout>
    </v-main>
  </v-app>
</template>
<script setup lang="ts">
import {
  TheHeader,
  AppNotification,
  AppConfirmDialog,
  AppDialogDragger,
} from '@/components'

import DefaultLayout from '@/layouts/DefaultLayout.vue'
</script>
<style lang="scss" scoped>
#main {
  min-height: 300px;
}
</style>
