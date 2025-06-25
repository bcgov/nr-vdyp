/// <reference types="cypress" />

import { handleApiError } from '@/services/apiErrorHandler'
import { SVC_ERR } from '@/constants/message'
import axios, { AxiosError } from 'axios'
import type { AxiosRequestHeaders, InternalAxiosRequestConfig } from 'axios'
import { setActivePinia, createPinia } from 'pinia'
import { useNotificationStore } from '@/stores/common/notificationStore'
import { StatusCodes } from 'http-status-codes'

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

    cy.get('@consoleWarnSpy').should(
      'have.been.calledWith',
      'Test Context: Request was canceled.',
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
    )
  })

  it('should handle non-axios errors', () => {
    const nonAxiosError = new Error('General JS Error')

    handleApiError(nonAxiosError)

    cy.get('@consoleErrorSpy').should(
      'have.been.calledWith',
      'The request could not be processed properly. Please try again.',
      'General JS Error',
    )

    cy.get('@showErrorMessageSpy').should(
      'have.been.calledWith',
      'The request could not be processed properly. Please try again.',
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
      'The request could not be processed properly. Please try again.',
      'No Pinia instance',
    )
  })
})
