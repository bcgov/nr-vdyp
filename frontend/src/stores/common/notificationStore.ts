import { defineStore } from 'pinia'
import type { MessageType } from '@/types/types'
import { NOTIFICATION, MESSAGE_TYPE } from '@/constants/constants'
import type { NotificationState } from '@/interfaces/interfaces'

export const useNotificationStore = defineStore({
  id: 'notificationStore',
  state: (): NotificationState => ({
    isShow: false,
    message: '',
    type: '',
    color: '',
    timeoutId: null,
  }),
  getters: {},
  actions: {
    resetMessage() {
      this.isShow = false
      if (this.timeoutId) {
        clearTimeout(this.timeoutId)
        this.timeoutId = null
      }
    },
    showMessage(message: string, type: MessageType = '') {
      this.resetMessage()
      this.message = message
      this.type = type
      this.color = type
      this.isShow = true

      // Automatically close messages after NOTIFICATION.SHOW_TIME
      this.timeoutId = setTimeout(() => {
        this.isShow = false
      }, NOTIFICATION.SHOW_TIME) as unknown as number
    },
    showSuccessMessage(message: string) {
      this.showMessage(message, MESSAGE_TYPE.SUCCESS)
    },
    showErrorMessage(message: string) {
      this.showMessage(message, MESSAGE_TYPE.ERROR)
    },
    showInfoMessage(message: string) {
      this.showMessage(message, MESSAGE_TYPE.INFO)
    },
    showWarningMessage(message: string) {
      this.showMessage(message, MESSAGE_TYPE.WARNING)
    },
  },
})
