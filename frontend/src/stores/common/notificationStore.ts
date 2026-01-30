import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { MessageType } from '@/types/types'
import { NOTIFICATION, MESSAGE_TYPE } from '@/constants/constants'

export const useNotificationStore = defineStore('notificationStore', () => {
  const isShow = ref<boolean>(false)
  const title = ref<string>('')
  const message = ref<string>('')
  const type = ref<MessageType>('')
  const color = ref<string>('')
  const timeoutId = ref<number | null>(null)

  const getIsShow = computed(() => isShow.value)
  const getTitle = computed(() => title.value)
  const getMessage = computed(() => message.value)
  const getType = computed(() => type.value)
  const getColor = computed(() => color.value)
  const getTimeoutId = computed(() => timeoutId.value)

  const resetMessage = () => {
    isShow.value = false
    title.value = ''
    if (timeoutId.value) {
      clearTimeout(timeoutId.value)
      timeoutId.value = null
    }
  }

  const showMessage = (
    messageParam: string,
    typeParam: MessageType = '',
    titleParam: string = '',
  ) => {
    resetMessage()
    title.value = titleParam
    message.value = messageParam
    type.value = typeParam
    color.value = typeParam
    isShow.value = true

    // Automatically close messages after NOTIFICATION.SHOW_TIME
    timeoutId.value = setTimeout(() => {
      isShow.value = false
    }, NOTIFICATION.SHOW_TIME) as unknown as number
  }

  const showSuccessMessage = (messageParam: string, titleParam: string = '') => {
    showMessage(messageParam, MESSAGE_TYPE.SUCCESS, titleParam)
  }

  const showErrorMessage = (messageParam: string, titleParam: string = '') => {
    showMessage(messageParam, MESSAGE_TYPE.ERROR, titleParam)
  }

  const showInfoMessage = (messageParam: string, titleParam: string = '') => {
    showMessage(messageParam, MESSAGE_TYPE.INFO, titleParam)
  }

  const showWarningMessage = (messageParam: string, titleParam: string = '') => {
    showMessage(messageParam, MESSAGE_TYPE.WARNING, titleParam)
  }

  return {
    isShow,
    title,
    message,
    type,
    color,
    timeoutId,
    getIsShow,
    getTitle,
    getMessage,
    getType,
    getColor,
    getTimeoutId,
    resetMessage,
    showMessage,
    showSuccessMessage,
    showErrorMessage,
    showInfoMessage,
    showWarningMessage,
  }
})
