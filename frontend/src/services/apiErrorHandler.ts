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
    contextMessage ? `${contextMessage}: ${message}` : message

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
  const message = prependMessage('Request was canceled.')
  console.warn(message, (error as AxiosError).message)
  notificationStore?.showInfoMessage(message, 'Request canceled')
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

  const message = prependMessage(getErrorMessage(response.status))
  notificationStore?.showErrorMessage(message, 'API error')
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
  notificationStore?.showErrorMessage(message, 'Network error')
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
  notificationStore?.showErrorMessage(message, 'Configuration error')
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
    'The request could not be processed properly. Please try again.',
  )
  console.error(message, (error as Error).message)
  notificationStore?.showErrorMessage(message, 'Request error')
}

/**
 * Return error messages based on status code
 * @param status
 * @returns
 */
function getErrorMessage(status: number): string {
  let logMessage = ''

  switch (status) {
    case StatusCodes.REQUEST_TIMEOUT:
      return SVC_ERR.REQUEST_TIMEOUT
    case StatusCodes.SERVICE_UNAVAILABLE:
      return SVC_ERR.SERVICE_UNAVAILABLE
    case StatusCodes.BAD_GATEWAY:
      return SVC_ERR.BAD_GATEWAY
    case StatusCodes.GATEWAY_TIMEOUT:
      return SVC_ERR.GATEWAY_TIMEOUT
    case StatusCodes.INTERNAL_SERVER_ERROR:
      return SVC_ERR.INTERNAL_SERVER_ERROR
    case StatusCodes.BAD_REQUEST:
      logMessage = 'Bad Request: The server could not understand the request.'
      break
    case StatusCodes.FORBIDDEN:
      logMessage = 'Forbidden: Do not have permission to access this resource.'
      break
    case StatusCodes.UNAUTHORIZED:
      logMessage = 'Unauthorized: Log in to access this resource.'
      break
    case StatusCodes.NOT_FOUND:
      logMessage = 'Not Found: The requested resource could not be found.'
      break
    case StatusCodes.NOT_ACCEPTABLE:
      logMessage = 'Not Acceptable: The requested format is not supported.'
      break
    case StatusCodes.UNSUPPORTED_MEDIA_TYPE:
      logMessage = 'Unsupported Media Type: Please check the content type.'
      break
    default:
      logMessage = `Unexpected status code: ${status}`
      break
  }

  console.error(logMessage)
  return `${SVC_ERR.DEFAULT} (Error Code: ${status})`
}

/**
 * Type guard to determine if the error is an AxiosError.
 * @param error - The error object to check.
 * @returns true if the error is an AxiosError.
 */
function isAxiosError(error: unknown): error is AxiosError {
  return !!error && (error as AxiosError).isAxiosError !== undefined
}
