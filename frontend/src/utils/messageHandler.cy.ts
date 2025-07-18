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

    it('should log to console when Pinia is not active', () => {
      setActivePinia(undefined)
      messageResult(true, 'Success!', 'Failure!')
      cy.get('@consoleInfo').should('be.calledWith', 'Success!')
      cy.get('@consoleWarn').should(
        'be.calledWith',
        'Pinia is not active. Message will only be logged.',
      )

      messageResult(false, 'Success!', 'Failure!')
      cy.get('@consoleWarn').should('be.calledWith', 'Failure!')
    })

    it('should handle null messages', () => {
      messageResult(true, null as any, 'Failure!')
      cy.get('@consoleInfo').should('be.calledWith', null)
      cy.get('@showSuccessMessage').should('be.calledWith', null)
    })
  })

  context('logInfoMessage', () => {
    it('should log info messages and trigger notification', () => {
      logInfoMessage('Info message')

      cy.get('@consoleInfo').should('be.calledWith', 'Info message')
      cy.get('@showInfoMessage').should('be.calledWith', 'Info message')
    })

    it('should include optional message in console', () => {
      logInfoMessage('Info message', 'Details')
      cy.get('@consoleInfo').should('be.calledWith', 'Info message (Details)')
      cy.get('@showInfoMessage').should('be.calledWith', 'Info message')
    })

    it('should disable console logging', () => {
      logInfoMessage('Info message', null, true)
      cy.get('@consoleInfo').should('not.be.called')
      cy.get('@showInfoMessage').should('be.calledWith', 'Info message')
    })

    it('should disable notification', () => {
      logInfoMessage('Info message', null, false, true)
      cy.get('@consoleInfo').should('be.calledWith', 'Info message')
      cy.get('@showInfoMessage').should('not.be.called')
    })

    it('should log to console when Pinia is not active', () => {
      setActivePinia(undefined)
      logInfoMessage('Info message')
      cy.get('@consoleInfo').should('be.calledWith', 'Info message')
      cy.get('@consoleWarn').should(
        'be.calledWith',
        'Pinia is not active. Message will only be logged.',
      )
    })

    it('should handle empty message', () => {
      logInfoMessage('')
      cy.get('@consoleInfo').should('be.calledWith', '')
      cy.get('@showInfoMessage').should('be.calledWith', '')
    })
  })

  context('logErrorMessage', () => {
    it('should log error messages and trigger notification', () => {
      logErrorMessage('Error message')

      cy.get('@consoleError').should('be.calledWith', 'Error message')
      cy.get('@showErrorMessage').should('be.calledWith', 'Error message')
    })

    it('should include optional message in console', () => {
      logErrorMessage('Error message', 'Details')
      cy.get('@consoleError').should('be.calledWith', 'Error message (Details)')
      cy.get('@showErrorMessage').should('be.calledWith', 'Error message')
    })

    it('should disable console and notification', () => {
      logErrorMessage('Error message', null, true, true)
      cy.get('@consoleError').should('not.be.called')
      cy.get('@showErrorMessage').should('not.be.called')
    })

    it('should handle null message', () => {
      logErrorMessage(null as any)
      cy.get('@consoleError').should('be.calledWith', null)
      cy.get('@showErrorMessage').should('be.calledWith', null)
    })
  })

  context('logSuccessMessage', () => {
    it('should log success messages and trigger notification', () => {
      logSuccessMessage('Success message')

      cy.get('@consoleLog').should('be.calledWith', 'Success message')
      cy.get('@showSuccessMessage').should('be.calledWith', 'Success message')
    })

    it('should include optional message in console', () => {
      logSuccessMessage('Success message', 'Details')
      cy.get('@consoleLog').should('be.calledWith', 'Success message (Details)')
      cy.get('@showSuccessMessage').should('be.calledWith', 'Success message')
    })

    it('should log to console when Pinia is not active', () => {
      setActivePinia(undefined)
      logSuccessMessage('Success message')
      cy.get('@consoleLog').should('be.calledWith', 'Success message')
    })
  })

  context('logWarningMessage', () => {
    it('should log warning messages and trigger notification', () => {
      logWarningMessage('Warning message')

      cy.get('@consoleWarn').should('be.calledWith', 'Warning message')
      cy.get('@showWarningMessage').should('be.calledWith', 'Warning message')
    })

    it('should include optional message in console', () => {
      logWarningMessage('Warning message', 'Details')
      cy.get('@consoleWarn').should(
        'be.calledWith',
        'Warning message (Details)',
      )
      cy.get('@showWarningMessage').should('be.calledWith', 'Warning message')
    })

    it('should handle non-string message', () => {
      logWarningMessage(123 as any)

      cy.get('@consoleWarn').should('be.calledWith', 123)
      cy.get('@showWarningMessage').should('be.calledWith', 123)
    })
  })

  context('Edge Cases', () => {
    it('should handle invalid message type', () => {
      logInfoMessage('Test', 'INVALID_TYPE')
      cy.get('@consoleInfo').should('be.calledWith', 'Test (INVALID_TYPE)')
      cy.get('@showInfoMessage').should('be.calledWith', 'Test')
    })

    it('should handle both console and notification disabled', () => {
      logSuccessMessage('Test', null, true, true)
      cy.get('@consoleLog').should('not.be.called')
      cy.get('@showSuccessMessage').should('not.be.called')
    })
  })
})
