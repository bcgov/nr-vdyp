<template>
  <v-app class="rounded rounded-md">
    <TheHeader />

    <v-main id="main">
      <AppNotification />
      <AppAlertDialog />
      <AppDialogDragger />

      <DefaultLayout>
        <router-view />
      </DefaultLayout>
    </v-main>

    <TheFooter />
  </v-app>
</template>
<script setup lang="ts">
import {
  TheHeader,
  TheFooter,
  AppNotification,
  AppAlertDialog,
  AppDialogDragger,
} from '@/components'

import DefaultLayout from '@/layouts/DefaultLayout.vue'
</script>
<style lang="scss" scoped>
#main {
  min-height: 300px;
}
</style>
