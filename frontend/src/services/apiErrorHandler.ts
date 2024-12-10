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
  const pinia = getActivePinia()
  let notificationStore

  if (pinia) {
    notificationStore = useNotificationStore(pinia)
  } else {
    console.warn('Pinia is not active. Message will only be logged.')
  }

  const prependMessage = (message: string) =>
    contextMessage ? `${contextMessage}: ${message}` : message

  if (axios.isCancel(error)) {
    const message = prependMessage('Request was canceled.')
    console.warn(message, (error as AxiosError).message)
    if (notificationStore) {
      notificationStore.showInfoMessage(message)
    }
    return
  }

  if (isAxiosError(error)) {
    const axiosError = error as AxiosError

    /* If the server returned a response */
    if (axiosError.response) {
      const response: AxiosResponse = axiosError.response

      console.error(prependMessage('API Error Response:'), {
        status: response.status,
        data: response.data,
        headers: response.headers,
      })

      const message = prependMessage(getErrorMessage(response.status))

      if (notificationStore) {
        notificationStore.showErrorMessage(message)
      }
    } else if (axiosError.request) {
      /*
      Handles cases where the server fails to return a response.
      This can be due to network issues or a failed connection to the server. */
      const message = prependMessage(`${SVC_ERR.DEFAULT} (Error: No Response)`)
      console.error(message, axiosError.request)
      if (notificationStore) {
        notificationStore.showErrorMessage(message)
      }
    } else {
      /*
      Handles cases where a configuration error occurs while sending a request.
      For example, an invalid URL or header setting can cause an error.
      */
      const message = prependMessage(
        `${SVC_ERR.DEFAULT} (Error: Configuration Issue)`,
      )
      console.error(message, axiosError.message)
      if (notificationStore) {
        notificationStore.showErrorMessage(message)
      }
    }

    // Log additional error information for debugging
    console.error('Axios Config:', axiosError.config)
    if (axiosError.code) {
      // Refer to Axios Error code
      console.error(`Axios Error Code: ${axiosError.code}`)
    }
  } else {
    // Treat it as a normal JavaScript error if the error passed in is not an Axios error.
    const message = prependMessage(
      'The request could not be processed properly. Please try again.',
    )
    console.error(message, (error as Error).message)
    if (notificationStore) {
      notificationStore.showErrorMessage(message)
    }
  }
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
  return (error as AxiosError).isAxiosError !== undefined
}
