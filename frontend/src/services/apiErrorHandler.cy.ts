/// <reference types="cypress" />

import { handleApiError } from '@/services/apiErrorHandler'
import { SVC_ERR } from '@/constants/message'
import axios, { AxiosError } from 'axios'
import type { AxiosRequestHeaders, InternalAxiosRequestConfig } from 'axios'
import { setActivePinia, createPinia } from 'pinia'
import { useNotificationStore } from '@/stores/common/notificationStore'
import { StatusCodes } from 'http-status-codes'

function createAxiosResponseError(status: number): AxiosError {
  return {
    isAxiosError: true,
    response: {
      status,
      data: 'Error',
      headers: {} as AxiosRequestHeaders,
      statusText: '',
      config: {} as InternalAxiosRequestConfig,
    },
    message: 'Error',
    name: 'AxiosError',
    config: { headers: {} as AxiosRequestHeaders },
    toJSON: () => ({}),
  }
}

describe('apiErrorHandler Unit Tests', () => {
  let notificationStore: ReturnType<typeof useNotificationStore>

  beforeEach(() => {
    setActivePinia(createPinia())
    notificationStore = useNotificationStore()
    cy.spy(notificationStore, 'showErrorMessage').as('showErrorMessageSpy')
    cy.spy(notificationStore, 'showInfoMessage').as('showInfoMessageSpy')
    cy.spy(console, 'warn').as('consoleWarnSpy')
    cy.spy(console, 'error').as('consoleErrorSpy')
  })

  it('should handle request cancellation errors', () => {
    const cancelError = new axios.Cancel('Request canceled')

    handleApiError(cancelError, 'Test Context')

    const expectedMessage = `Test Context due to ${SVC_ERR.REQUEST_CANCELED}`
    cy.get('@consoleWarnSpy').should('have.been.calledWith', expectedMessage, 'Request canceled')
    cy.get('@showInfoMessageSpy').should(
      'have.been.calledWith',
      expectedMessage,
      SVC_ERR.REQUEST_CANCELED_TITLE,
    )
  })

  it('should handle axios error with response', () => {
    const mockError: AxiosError = {
      isAxiosError: true,
      response: {
        status: StatusCodes.INTERNAL_SERVER_ERROR,
        data: 'Server Error',
        headers: {} as AxiosRequestHeaders,
        statusText: '',
        config: {} as InternalAxiosRequestConfig,
      },
      message: 'Internal server error',
      name: 'AxiosError',
      config: {
        headers: {} as AxiosRequestHeaders,
      },
      toJSON: () => ({}),
    }

    handleApiError(mockError)

    cy.get('@consoleErrorSpy').should(
      'have.been.calledWithMatch',
      'API Error Response:',
      {
        status: StatusCodes.INTERNAL_SERVER_ERROR,
        data: 'Server Error',
      },
    )

    cy.get('@showErrorMessageSpy').should(
      'have.been.calledWith',
      SVC_ERR.INTERNAL_SERVER_ERROR,
      SVC_ERR.INTERNAL_SERVER_ERROR_TITLE,
    )
  })

  it('should handle axios error without response', () => {
    const mockError: AxiosError = {
      isAxiosError: true,
      request: {},
      message: 'Network Error',
      name: 'AxiosError',
      config: {
        headers: {} as AxiosRequestHeaders,
      },
      toJSON: () => ({}),
    }

    handleApiError(mockError)

    cy.get('@consoleErrorSpy').should(
      'have.been.calledWith',
      `${SVC_ERR.DEFAULT} (Error: No Response)`,
    )

    cy.get('@showErrorMessageSpy').should(
      'have.been.calledWith',
      `${SVC_ERR.DEFAULT} (Error: No Response)`,
      SVC_ERR.DEFAULT_TITLE,
    )
  })

  it('should handle configuration errors', () => {
    const mockError: AxiosError = {
      isAxiosError: true,
      message: 'Invalid configuration',
      name: 'AxiosError',
      config: {
        headers: {} as AxiosRequestHeaders,
      },
      toJSON: () => ({}),
    }

    handleApiError(mockError)

    cy.get('@consoleErrorSpy').should(
      'have.been.calledWith',
      `${SVC_ERR.DEFAULT} (Error: Configuration Issue)`,
    )

    cy.get('@showErrorMessageSpy').should(
      'have.been.calledWith',
      `${SVC_ERR.DEFAULT} (Error: Configuration Issue)`,
      SVC_ERR.DEFAULT_TITLE,
    )
  })

  it('should handle non-axios errors', () => {
    const nonAxiosError = new Error('General JS Error')

    handleApiError(nonAxiosError)

    cy.get('@consoleErrorSpy').should(
      'have.been.calledWith',
      SVC_ERR.PROCESSING_ERROR,
      'General JS Error',
    )

    cy.get('@showErrorMessageSpy').should(
      'have.been.calledWith',
      SVC_ERR.PROCESSING_ERROR,
      SVC_ERR.PROCESSING_ERROR_TITLE,
    )
  })

  it('should warn when Pinia store is not active', () => {
    setActivePinia(null as any)

    const nonAxiosError = new Error('No Pinia instance')

    handleApiError(nonAxiosError)

    cy.get('@consoleWarnSpy').should(
      'have.been.calledWith',
      'Pinia is not active. Message will only be logged.',
    )

    cy.get('@consoleErrorSpy').should(
      'have.been.calledWith',
      SVC_ERR.PROCESSING_ERROR,
      'No Pinia instance',
    )
  })

  it('should prepend context message using "due to" format', () => {
    const mockError: AxiosError = {
      isAxiosError: true,
      response: {
        status: StatusCodes.NOT_FOUND,
        data: 'Not Found',
        headers: {} as AxiosRequestHeaders,
        statusText: '',
        config: {} as InternalAxiosRequestConfig,
      },
      message: 'Not Found',
      name: 'AxiosError',
      config: { headers: {} as AxiosRequestHeaders },
      toJSON: () => ({}),
    }

    handleApiError(mockError, 'Loading resource failed')

    cy.get('@showErrorMessageSpy').should(
      'have.been.calledWith',
      `Loading resource failed due to ${SVC_ERR.NOT_FOUND}`,
      SVC_ERR.NOT_FOUND_TITLE,
    )
  })

  describe('HTTP status code to error message mapping', () => {
    ;[
      {
        status: StatusCodes.REQUEST_TIMEOUT,
        message: SVC_ERR.REQUEST_TIMEOUT,
        title: SVC_ERR.REQUEST_TIMEOUT_TITLE,
      },
      {
        status: StatusCodes.SERVICE_UNAVAILABLE,
        message: SVC_ERR.SERVICE_UNAVAILABLE,
        title: SVC_ERR.SERVICE_UNAVAILABLE_TITLE,
      },
      {
        status: StatusCodes.BAD_GATEWAY,
        message: SVC_ERR.BAD_GATEWAY,
        title: SVC_ERR.BAD_GATEWAY_TITLE,
      },
      {
        status: StatusCodes.GATEWAY_TIMEOUT,
        message: SVC_ERR.GATEWAY_TIMEOUT,
        title: SVC_ERR.GATEWAY_TIMEOUT_TITLE,
      },
      {
        status: StatusCodes.BAD_REQUEST,
        message: SVC_ERR.BAD_REQUEST,
        title: SVC_ERR.BAD_REQUEST_TITLE,
      },
      {
        status: StatusCodes.FORBIDDEN,
        message: SVC_ERR.FORBIDDEN,
        title: SVC_ERR.FORBIDDEN_TITLE,
      },
      {
        status: StatusCodes.UNAUTHORIZED,
        message: SVC_ERR.UNAUTHORIZED,
        title: SVC_ERR.UNAUTHORIZED_TITLE,
      },
      {
        status: StatusCodes.NOT_FOUND,
        message: SVC_ERR.NOT_FOUND,
        title: SVC_ERR.NOT_FOUND_TITLE,
      },
      {
        status: StatusCodes.NOT_ACCEPTABLE,
        message: SVC_ERR.NOT_ACCEPTABLE,
        title: SVC_ERR.NOT_ACCEPTABLE_TITLE,
      },
      {
        status: StatusCodes.UNSUPPORTED_MEDIA_TYPE,
        message: SVC_ERR.UNSUPPORTED_MEDIA_TYPE,
        title: SVC_ERR.UNSUPPORTED_MEDIA_TYPE_TITLE,
      },
    ].forEach(({ status, message, title }) => {
      it(`should return correct error message and title for HTTP ${status}`, () => {
        handleApiError(createAxiosResponseError(status))

        cy.get('@showErrorMessageSpy').should('have.been.calledWith', message, title)
      })
    })

    it('should handle unknown status code with default message', () => {
      const unknownStatus = 418
      handleApiError(createAxiosResponseError(unknownStatus))

      cy.get('@consoleErrorSpy').should(
        'have.been.calledWith',
        `Unexpected status code: ${unknownStatus}`,
      )
      cy.get('@showErrorMessageSpy').should(
        'have.been.calledWith',
        `${SVC_ERR.DEFAULT} (Error Code: ${unknownStatus})`,
        SVC_ERR.DEFAULT_TITLE,
      )
    })
  })
})
