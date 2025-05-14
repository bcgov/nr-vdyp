/// <reference types="cypress" />

import {
  messageResult,
  logInfoMessage,
  logErrorMessage,
  logSuccessMessage,
  logWarningMessage,
} from '@/utils/messageHandler'
import { setActivePinia, createPinia } from 'pinia'
import { useNotificationStore } from '@/stores/common/notificationStore'

describe('Message Handler Unit Tests', () => {
  let notificationStore: ReturnType<typeof useNotificationStore>

  beforeEach(() => {
    setActivePinia(createPinia())
    notificationStore = useNotificationStore()

    cy.spy(notificationStore, 'showSuccessMessage').as('showSuccessMessage')
    cy.spy(notificationStore, 'showErrorMessage').as('showErrorMessage')
    cy.spy(notificationStore, 'showWarningMessage').as('showWarningMessage')
    cy.spy(notificationStore, 'showInfoMessage').as('showInfoMessage')

    cy.stub(console, 'log').as('consoleLog')
    cy.stub(console, 'warn').as('consoleWarn')
    cy.stub(console, 'error').as('consoleError')
    cy.stub(console, 'info').as('consoleInfo')
  })

  context('messageResult', () => {
    it('should show success message when operation succeeds', () => {
      messageResult(true, 'Success!', 'Failure!')

      cy.get('@consoleInfo').should('be.calledWith', 'Success!')
      cy.get('@showSuccessMessage').should('be.calledWith', 'Success!')
    })

    it('should show failure message when operation fails', () => {
      messageResult(false, 'Success!', 'Failure!')

      cy.get('@consoleWarn').should('be.calledWith', 'Failure!')
      cy.get('@showWarningMessage').should('be.calledWith', 'Failure!')
    })

    it('should log error message when operation fails with error object', () => {
      const error = new Error('Test error')
      messageResult(false, 'Success!', 'Failure!', error)

      cy.get('@consoleWarn').should('be.calledWith', 'Failure!', error)
      cy.get('@showWarningMessage').should('be.calledWith', 'Failure!')
    })
  })

  context('logInfoMessage', () => {
    it('should log info messages and trigger notification', () => {
      logInfoMessage('Info message')

      cy.get('@consoleInfo').should('be.calledWith', 'Info message')
      cy.get('@showInfoMessage').should('be.calledWith', 'Info message')
    })
  })

  context('logErrorMessage', () => {
    it('should log error messages and trigger notification', () => {
      logErrorMessage('Error message')

      cy.get('@consoleError').should('be.calledWith', 'Error message')
      cy.get('@showErrorMessage').should('be.calledWith', 'Error message')
    })
  })

  context('logSuccessMessage', () => {
    it('should log success messages and trigger notification', () => {
      logSuccessMessage('Success message')

      cy.get('@consoleLog').should('be.calledWith', 'Success message')
      cy.get('@showSuccessMessage').should('be.calledWith', 'Success message')
    })
  })

  context('logWarningMessage', () => {
    it('should log warning messages and trigger notification', () => {
      logWarningMessage('Warning message')

      cy.get('@consoleWarn').should('be.calledWith', 'Warning message')
      cy.get('@showWarningMessage').should('be.calledWith', 'Warning message')
    })
  })
})
