import axios, { AxiosError } from 'axios'
import type { AxiosResponse } from 'axios'
import { StatusCodes } from 'http-status-codes'
import { useNotificationStore } from '@/stores/common/notificationStore'
import { getActivePinia } from 'pinia'
import { SVC_ERR } from '@/constants/message'

/**
 * Handles API errors by logging relevant details based on the error type.
 * @param error - The error thrown by an Axios request.
 * @param contextMessage - Optional context message to prepend to error messages.
 */
export const handleApiError = (error: unknown, contextMessage?: string) => {
  const notificationStore = getNotificationStore()
  const prependMessage = (message: string) =>
    contextMessage ? `${contextMessage} due to ${message}` : message

  if (axios.isCancel(error)) {
    handleCanceledRequest(error, prependMessage, notificationStore)
    return
  }

  if (isAxiosError(error)) {
    handleAxiosError(error, prependMessage, notificationStore)
  } else {
    handleGenericError(error, prependMessage, notificationStore)
  }
}

/**
 * Gets the notification store or returns undefined if Pinia is not active.
 */
function getNotificationStore() {
  const pinia = getActivePinia()
  if (pinia) {
    return useNotificationStore(pinia)
  }
  console.warn('Pinia is not active. Message will only be logged.')
  return undefined
}

/**
 * Handles canceled requests.
 */
function handleCanceledRequest(
  error: unknown,
  prependMessage: (message: string) => string,
  notificationStore?: ReturnType<typeof useNotificationStore>,
) {
  const message = prependMessage(SVC_ERR.REQUEST_CANCELED)
  console.warn(message, (error as AxiosError).message)
  notificationStore?.showInfoMessage(message, SVC_ERR.REQUEST_CANCELED_TITLE)
}

/**
 * Handles Axios-specific errors.
 */
function handleAxiosError(
  axiosError: AxiosError,
  prependMessage: (message: string) => string,
  notificationStore?: ReturnType<typeof useNotificationStore>,
) {
  if (axiosError.response) {
    handleResponseError(axiosError.response, prependMessage, notificationStore)
  } else if (axiosError.request) {
    handleRequestError(axiosError.request, prependMessage, notificationStore)
  } else {
    handleConfigurationError(axiosError, prependMessage, notificationStore)
  }

  logAxiosDebugInfo(axiosError)
}

/**
 * Handles errors from server responses.
 */
function handleResponseError(
  response: AxiosResponse,
  prependMessage: (message: string) => string,
  notificationStore?: ReturnType<typeof useNotificationStore>,
) {
  console.error(prependMessage('API Error Response:'), {
    status: response.status,
    data: response.data,
    headers: response.headers,
  })

  const { message, title } = getErrorMessage(response.status)
  notificationStore?.showErrorMessage(prependMessage(message), title)
}

/**
 * Handles cases where the server fails to return a response.
 * This can be due to network issues or a failed connection to the server.
 */
function handleRequestError(
  request: unknown,
  prependMessage: (message: string) => string,
  notificationStore?: ReturnType<typeof useNotificationStore>,
) {
  const message = prependMessage(`${SVC_ERR.DEFAULT} (Error: No Response)`)
  console.error(message, request)
  notificationStore?.showErrorMessage(message, SVC_ERR.DEFAULT_TITLE)
}

/**
 * Handles cases where a configuration error occurs while sending a request.
 * For example, an invalid URL or header setting can cause an error.
 */
function handleConfigurationError(
  axiosError: AxiosError,
  prependMessage: (message: string) => string,
  notificationStore?: ReturnType<typeof useNotificationStore>,
) {
  const message = prependMessage(`${SVC_ERR.DEFAULT} (Error: Configuration Issue)`)
  console.error(message, axiosError.message)
  notificationStore?.showErrorMessage(message, SVC_ERR.DEFAULT_TITLE)
}

/**
 * Logs additional Axios error information for debugging.
 */
function logAxiosDebugInfo(axiosError: AxiosError) {
  console.error('Axios Config:', axiosError.config)
  if (axiosError.code) {
    console.error(`Axios Error Code: ${axiosError.code}`)
  }
}

/**
 * Handles generic JavaScript errors.
 */
function handleGenericError(
  error: unknown,
  prependMessage: (message: string) => string,
  notificationStore?: ReturnType<typeof useNotificationStore>,
) {
  const message = prependMessage(
    SVC_ERR.PROCESSING_ERROR,
  )
  console.error(message, (error as Error).message)
  notificationStore?.showErrorMessage(message, SVC_ERR.PROCESSING_ERROR_TITLE)
}

/**
 * Return error messages and titles based on status code
 * @param status
 * @returns { message, title }
 */
function getErrorMessage(status: number): { message: string; title: string } {
  let logMessage = ''

  switch (status) {
    case StatusCodes.REQUEST_TIMEOUT:
      return { message: SVC_ERR.REQUEST_TIMEOUT, title: SVC_ERR.REQUEST_TIMEOUT_TITLE }
    case StatusCodes.SERVICE_UNAVAILABLE:
      return { message: SVC_ERR.SERVICE_UNAVAILABLE, title: SVC_ERR.SERVICE_UNAVAILABLE_TITLE }
    case StatusCodes.BAD_GATEWAY:
      return { message: SVC_ERR.BAD_GATEWAY, title: SVC_ERR.BAD_GATEWAY_TITLE }
    case StatusCodes.GATEWAY_TIMEOUT:
      return { message: SVC_ERR.GATEWAY_TIMEOUT, title: SVC_ERR.GATEWAY_TIMEOUT_TITLE }
    case StatusCodes.INTERNAL_SERVER_ERROR:
      return { message: SVC_ERR.INTERNAL_SERVER_ERROR, title: SVC_ERR.INTERNAL_SERVER_ERROR_TITLE }
    case StatusCodes.BAD_REQUEST:
      return { message: SVC_ERR.BAD_REQUEST, title: SVC_ERR.BAD_REQUEST_TITLE }
    case StatusCodes.FORBIDDEN:
      return { message: SVC_ERR.FORBIDDEN, title: SVC_ERR.FORBIDDEN_TITLE }
    case StatusCodes.UNAUTHORIZED:
      return { message: SVC_ERR.UNAUTHORIZED, title: SVC_ERR.UNAUTHORIZED_TITLE }
    case StatusCodes.NOT_FOUND:
      return { message: SVC_ERR.NOT_FOUND, title: SVC_ERR.NOT_FOUND_TITLE }
    case StatusCodes.NOT_ACCEPTABLE:
      return { message: SVC_ERR.NOT_ACCEPTABLE, title: SVC_ERR.NOT_ACCEPTABLE_TITLE }
    case StatusCodes.UNSUPPORTED_MEDIA_TYPE:
      return { message: SVC_ERR.UNSUPPORTED_MEDIA_TYPE, title: SVC_ERR.UNSUPPORTED_MEDIA_TYPE_TITLE }
    default:
      logMessage = `Unexpected status code: ${status}`
      console.error(logMessage)
      return { message: `${SVC_ERR.DEFAULT} (Error Code: ${status})`, title: SVC_ERR.DEFAULT_TITLE }
  }
}

/**
 * Type guard to determine if the error is an AxiosError.
 * @param error - The error object to check.
 * @returns true if the error is an AxiosError.
 */
function isAxiosError(error: unknown): error is AxiosError {
  return !!error && (error as AxiosError).isAxiosError !== undefined
}
