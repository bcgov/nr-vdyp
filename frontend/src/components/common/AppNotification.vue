<template>
  <AppSnackbar
    :is-visible="isShow"
    :message="message"
    :type="type"
    :color="color"
    :timeout="NOTIFICATION.SHOW_TIME"
    @close="closeMessage"
  />
</template>
<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { useNotificationStore } from '@/stores/common/notificationStore'
import AppSnackbar from '../core/AppSnackbar.vue'
import { NOTIFICATION } from '@/constants/constants'

const notificationStore = useNotificationStore()

const { isShow, message, type, color } = storeToRefs(notificationStore)

const closeMessage = () => {
  notificationStore.resetMessage()
}
</script>
