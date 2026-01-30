import { useNotificationStore } from '@/stores/common/notificationStore'
import type { MessageType } from '@/types/types'
import { MESSAGE_TYPE } from '@/constants/constants'
import { getActivePinia } from 'pinia'

/**
 * Displays job success or failure messages to notification and/or console.
 * @param {boolean} isSuccess - Whether the operation succeeded.
 * @param {string} successMessage - Message for successful operation.
 * @param {string} failMessage - Message for failed operation.
 * @param {Error | null} error - Optional error object for logging.
 * @param {string} successTitle - Optional title for successful operation.
 * @param {string} failTitle - Optional title for failed operation.
 */
export const messageResult = (
  isSuccess: boolean,
  successMessage: string,
  failMessage: string,
  error: Error | null = null,
  successTitle: string = '',
  failTitle: string = '',
) => {
  const pinia = getActivePinia()
  let notificationStore

  if (pinia) {
    notificationStore = useNotificationStore()
  } else {
    console.warn('Pinia is not active. Message will only be logged.')
  }

  if (isSuccess) {
    console.info(successMessage)
    if (notificationStore) {
      notificationStore.showSuccessMessage(successMessage, successTitle)
    }
  } else {
    console.warn(failMessage, error)
    if (notificationStore) {
      notificationStore.showWarningMessage(failMessage, failTitle)
    }
  }
}

/**
 * Logs a message to the console based on message type.
 */
const logToConsole = (message: string, messageType: MessageType) => {
  switch (messageType) {
    case MESSAGE_TYPE.ERROR:
      console.error(message)
      break
    case MESSAGE_TYPE.WARNING:
      console.warn(message)
      break
    case MESSAGE_TYPE.INFO:
      console.info(message)
      break
    case MESSAGE_TYPE.SUCCESS:
      console.log(message)
      break
    default:
      console.log(message)
  }
}

/**
 * Shows a notification message based on message type.
 */
const showNotification = (
  message: string,
  messageType: MessageType,
  notificationStore: ReturnType<typeof useNotificationStore>,
  title: string = '',
) => {
  switch (messageType) {
    case MESSAGE_TYPE.ERROR:
      notificationStore.showErrorMessage(message, title)
      break
    case MESSAGE_TYPE.WARNING:
      notificationStore.showWarningMessage(message, title)
      break
    case MESSAGE_TYPE.SUCCESS:
      notificationStore.showSuccessMessage(message, title)
      break
    case MESSAGE_TYPE.INFO:
    default:
      notificationStore.showInfoMessage(message, title)
  }
}

/**
 * Logs messages to both the console or/and notification with conditional control.
 * @param {string} message - The message to display.
 * @param {string} messageType - The type of message.
 * @param {string} [optionalMessage] - Optional detail message for console output.
 * @param {boolean} [disableConsole=false] - Whether to disable console logging.
 * @param {boolean} [disableNotification=false] - Whether to disable notification messages.
 * @param {string} [title=''] - Optional title for the notification.
 */
const logMessage = (
  message: string,
  messageType: MessageType = MESSAGE_TYPE.INFO,
  optionalMessage?: string | null,
  disableConsole: boolean = false,
  disableNotification: boolean = false,
  title: string = '',
) => {
  const pinia = getActivePinia()
  let notificationStore

  if (pinia) {
    notificationStore = useNotificationStore()
  } else {
    console.warn('Pinia is not active. Message will only be logged.')
  }

  const consoleMessage = optionalMessage
    ? `${message} (${optionalMessage})`
    : message

  if (!disableConsole) {
    logToConsole(consoleMessage, messageType)
  }

  if (!disableNotification && notificationStore) {
    showNotification(message, messageType, notificationStore, title)
  }
}

/**
 * Logs info messages to both the console or/and notification with conditional control.
 * @param {string} message - The message to display.
 * @param {string} [optionalMessage] - Optional detail message for console output.
 * @param {boolean} [disableConsole=false] - Whether to disable console logging.
 * @param {boolean} [disableNotification=false] - Whether to disable notification messages.
 * @param {string} [title=''] - Optional title for the notification.
 */
export const logInfoMessage = (
  message: string,
  optionalMessage?: string | null,
  disableConsole = false,
  disableNotification = false,
  title = '',
) =>
  logMessage(
    message,
    MESSAGE_TYPE.INFO,
    optionalMessage,
    disableConsole,
    disableNotification,
    title,
  )

/**
 * Logs error messages to both the console or/and notification with conditional control.
 * @param {string} message - The message to display.
 * @param {string} [optionalMessage] - Optional detail message for console output.
 * @param {boolean} [disableConsole=false] - Whether to disable console logging.
 * @param {boolean} [disableNotification=false] - Whether to disable notification messages.
 * @param {string} [title=''] - Optional title for the notification.
 */
export const logErrorMessage = (
  message: string,
  optionalMessage?: string | null,
  disableConsole = false,
  disableNotification = false,
  title = '',
) =>
  logMessage(
    message,
    MESSAGE_TYPE.ERROR,
    optionalMessage,
    disableConsole,
    disableNotification,
    title,
  )

/**
 * Logs success messages to both the console or/and notification with conditional control.
 * @param {string} message - The message to display.
 * @param {string} [optionalMessage] - Optional detail message for console output.
 * @param {boolean} [disableConsole=false] - Whether to disable console logging.
 * @param {boolean} [disableNotification=false] - Whether to disable notification messages.
 * @param {string} [title=''] - Optional title for the notification.
 */
export const logSuccessMessage = (
  message: string,
  optionalMessage?: string | null,
  disableConsole = false,
  disableNotification = false,
  title = '',
) =>
  logMessage(
    message,
    MESSAGE_TYPE.SUCCESS,
    optionalMessage,
    disableConsole,
    disableNotification,
    title,
  )

/**
 * Logs warning messages to both the console or/and notification with conditional control.
 * @param {string} message - The message to display.
 * @param {string} [optionalMessage] - Optional detail message for console output.
 * @param {boolean} [disableConsole=false] - Whether to disable console logging.
 * @param {boolean} [disableNotification=false] - Whether to disable notification messages.
 * @param {string} [title=''] - Optional title for the notification.
 */
export const logWarningMessage = (
  message: string,
  optionalMessage?: string | null,
  disableConsole = false,
  disableNotification = false,
  title = '',
) =>
  logMessage(
    message,
    MESSAGE_TYPE.WARNING,
    optionalMessage,
    disableConsole,
    disableNotification,
    title,
  )
