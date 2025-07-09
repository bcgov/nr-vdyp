/// <reference types="cypress" />

import { setActivePinia, createPinia } from 'pinia'
import { useNotificationStore } from '@/stores/common/notificationStore'
import { MESSAGE_TYPE, NOTIFICATION } from '@/constants/constants'

describe('Notification Store Unit Tests', () => {
  let notificationStore: ReturnType<typeof useNotificationStore>

  beforeEach(() => {
    setActivePinia(createPinia())
    notificationStore = useNotificationStore()
  })

  it('should initialize with default state', () => {
    expect(notificationStore.isShow).to.be.false
    expect(notificationStore.message).to.equal('')
    expect(notificationStore.type).to.equal('')
    expect(notificationStore.color).to.equal('')
    expect(notificationStore.timeoutId).to.be.null
  })

  it('should return correct getter values', () => {
    expect(notificationStore.getIsShow).to.be.false
    expect(notificationStore.getMessage).to.equal('')
    expect(notificationStore.getType).to.equal('')
    expect(notificationStore.getColor).to.equal('')
    expect(notificationStore.getTimeoutId).to.be.null
  })

  it('should show message and set properties correctly', () => {
    const testMessage = 'Test message'
    const testType = MESSAGE_TYPE.SUCCESS

    notificationStore.showMessage(testMessage, testType)

    expect(notificationStore.message).to.equal(testMessage)
    expect(notificationStore.type).to.equal(testType)
    expect(notificationStore.color).to.equal(testType)
    expect(notificationStore.isShow).to.be.true
    expect(notificationStore.timeoutId).to.not.be.null
  })

  it('should reset message and clear timeout', () => {
    notificationStore.showMessage('Test', MESSAGE_TYPE.INFO)
    notificationStore.resetMessage()

    expect(notificationStore.isShow).to.be.false
    expect(notificationStore.timeoutId).to.be.null
  })

  it(
    'should automatically close message after timeout',
    {
      slowTestThreshold: NOTIFICATION.SHOW_TIME + 1000, // Increase the time by 1 second to prevent displaying the test time in yellow
    },
    () => {
      const testMessage = 'Auto close test'
      const testType = MESSAGE_TYPE.WARNING

      notificationStore.showMessage(testMessage, testType)

      // Wait for NOTIFICATION.SHOW_TIME + a small buffer
      cy.wait(NOTIFICATION.SHOW_TIME + 100, { log: false }).then(() => {
        expect(notificationStore.isShow).to.be.false
      })
    },
  )

  it('should show success message correctly', () => {
    const testMessage = 'Success message'

    notificationStore.showSuccessMessage(testMessage)

    expect(notificationStore.message).to.equal(testMessage)
    expect(notificationStore.type).to.equal(MESSAGE_TYPE.SUCCESS)
    expect(notificationStore.color).to.equal(MESSAGE_TYPE.SUCCESS)
    expect(notificationStore.isShow).to.be.true
  })

  it('should show error message correctly', () => {
    const testMessage = 'Error message'

    notificationStore.showErrorMessage(testMessage)

    expect(notificationStore.message).to.equal(testMessage)
    expect(notificationStore.type).to.equal(MESSAGE_TYPE.ERROR)
    expect(notificationStore.color).to.equal(MESSAGE_TYPE.ERROR)
    expect(notificationStore.isShow).to.be.true
  })

  it('should show info message correctly', () => {
    const testMessage = 'Info message'

    notificationStore.showInfoMessage(testMessage)

    expect(notificationStore.message).to.equal(testMessage)
    expect(notificationStore.type).to.equal(MESSAGE_TYPE.INFO)
    expect(notificationStore.color).to.equal(MESSAGE_TYPE.INFO)
    expect(notificationStore.isShow).to.be.true
  })

  it('should show warning message correctly', () => {
    const testMessage = 'Warning message'

    notificationStore.showWarningMessage(testMessage)

    expect(notificationStore.message).to.equal(testMessage)
    expect(notificationStore.type).to.equal(MESSAGE_TYPE.WARNING)
    expect(notificationStore.color).to.equal(MESSAGE_TYPE.WARNING)
    expect(notificationStore.isShow).to.be.true
  })

  it('should handle showMessage without type', () => {
    const testMessage = 'No type message'

    notificationStore.showMessage(testMessage)

    expect(notificationStore.message).to.equal(testMessage)
    expect(notificationStore.type).to.equal('')
    expect(notificationStore.color).to.equal('')
    expect(notificationStore.isShow).to.be.true
  })

  it('should clear existing timeout when showing a new message', () => {
    notificationStore.showMessage('First message', MESSAGE_TYPE.INFO)
    const firstTimeoutId = notificationStore.timeoutId

    notificationStore.showMessage('Second message', MESSAGE_TYPE.SUCCESS)
    const secondTimeoutId = notificationStore.timeoutId

    expect(firstTimeoutId).to.not.equal(secondTimeoutId)
    expect(notificationStore.message).to.equal('Second message')
  })
})
