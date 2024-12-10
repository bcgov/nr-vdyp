<template>
  <div>
    <v-snackbar
      v-model="isShow"
      :timeout="NOTIFICATION.SHOW_TIME"
      :color="type || 'info'"
      class="elevation-12"
      location="top"
      @click:outside="closeMessage"
    >
      <div class="d-flex align-center">
        <v-icon class="mr-2">{{ getIcon(type) }}</v-icon>
        <span> {{ message || 'Notification message' }}</span>
      </div>

      <template #actions>
        <v-btn icon variant="text" @click="closeMessage">
          <v-icon>mdi-close</v-icon>
        </v-btn>
      </template>
    </v-snackbar>
  </div>
</template>
<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { useNotificationStore } from '@/stores/common/notificationStore'
import type { MessageType } from '@/types/types'
import { NOTIFICATION } from '@/constants/constants'

const notificationStore = useNotificationStore()

const { isShow, message, type } = storeToRefs(notificationStore)

const closeMessage = () => {
  notificationStore.resetMessage()
}

const getIcon = (type: MessageType) => {
  const icon: { [key in MessageType]: string } = {
    '': '',
    info: 'mdi-information',
    success: 'mdi-check-circle',
    error: 'mdi-alert-circle',
    warning: 'mdi-alert',
  }

  return icon[type] || 'mdi-information'
}
</script>
