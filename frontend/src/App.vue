<template>
  <v-app class="rounded rounded-md">
    <TheHeader />

    <v-main id="main">
      <AppMessageDialog />
      <AppNotification />
      <AppConfirmDialog />
      <AppProgressCircular />
      <AppDialogDragger />

      <DefaultLayout>
        <router-view />
      </DefaultLayout>
    </v-main>
  </v-app>
</template>
<script setup lang="ts">
import TheHeader from '@/components/layout/TheHeader.vue'
import AppMessageDialog from '@/components/common/AppMessageDialog.vue'
import AppNotification from '@/components/common/AppNotification.vue'
import AppConfirmDialog from '@/components/common/AppConfirmDialog.vue'
import AppProgressCircular from '@/components/common/AppProgressCircular.vue'
import AppDialogDragger from '@/components/common/AppDialogDragger.vue'
import DefaultLayout from '@/layouts/DefaultLayout.vue'
</script>
<style lang="scss" scoped>
#main {
  min-height: 300px;
}
</style>
