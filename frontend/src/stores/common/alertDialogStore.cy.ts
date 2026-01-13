/// <reference types="cypress" />

import { setActivePinia, createPinia } from 'pinia'
import { useAlertDialogStore } from '@/stores/common/alertDialogStore'

describe('Confirm Dialog Store Unit Tests', () => {
  let alertDialogStore: ReturnType<typeof useAlertDialogStore>

  beforeEach(() => {
    setActivePinia(createPinia())
    alertDialogStore = useAlertDialogStore()
  })

  it('should initialize with default state', () => {
    expect(alertDialogStore.dialog).to.be.false
    expect(alertDialogStore.resolve).to.be.null
    expect(alertDialogStore.title).to.be.null
    expect(alertDialogStore.message).to.be.null
    expect(alertDialogStore.options).to.deep.equal({
      width: 400,
      noconfirm: false,
      variant: 'confirmation',
    })
  })

  it('should return correct getter values', () => {
    expect(alertDialogStore.getIsOpen).to.be.false
    expect(alertDialogStore.getDialogTitle).to.be.null
    expect(alertDialogStore.getDialogMessage).to.be.null
    expect(alertDialogStore.getDialogOptions).to.deep.equal({
      width: 400,
      noconfirm: false,
      variant: 'confirmation',
    })
  })

  it('should open dialog and set title, message, and options', async () => {
    const title = 'Confirm Action'
    const message = 'Are you sure?'
    const options = { width: 500, noconfirm: true, variant: 'warning' as const }

    const promise = alertDialogStore.openDialog(title, message, options)

    expect(alertDialogStore.dialog).to.be.true
    expect(alertDialogStore.title).to.equal(title)
    expect(alertDialogStore.message).to.equal(message)
    expect(alertDialogStore.options).to.deep.equal(options)

    alertDialogStore.agree()
    const result = await promise
    expect(result).to.be.true
  })

  it('should resolve promise with true when agree is called', async () => {
    const promise = alertDialogStore.openDialog('Test', 'Test message')
    alertDialogStore.agree()
    const result = await promise
    expect(result).to.be.true
    expect(alertDialogStore.dialog).to.be.false
  })

  it('should resolve promise with false when cancel is called', async () => {
    const promise = alertDialogStore.openDialog('Test', 'Test message')
    alertDialogStore.cancel()
    const result = await promise
    expect(result).to.be.false
    expect(alertDialogStore.dialog).to.be.false
  })

  it('should reset state after agree or cancel', () => {
    alertDialogStore.openDialog('Test', 'Test message')
    alertDialogStore.agree()
    expect(alertDialogStore.resolve).to.be.null
    expect(alertDialogStore.title).to.equal('')
    expect(alertDialogStore.message).to.equal('')
    expect(alertDialogStore.options).to.deep.equal({
      width: 400,
      noconfirm: false,
      variant: 'confirmation',
    })

    alertDialogStore.openDialog('Test', 'Test message')
    alertDialogStore.cancel()
    expect(alertDialogStore.resolve).to.be.null
    expect(alertDialogStore.title).to.equal('')
    expect(alertDialogStore.message).to.equal('')
    expect(alertDialogStore.options).to.deep.equal({
      width: 400,
      noconfirm: false,
      variant: 'confirmation',
    })
  })

  it('should handle openDialog without options', async () => {
    const title = 'No Options'
    const message = 'This has default options'

    const promise = alertDialogStore.openDialog(title, message)

    expect(alertDialogStore.dialog).to.be.true
    expect(alertDialogStore.title).to.equal(title)
    expect(alertDialogStore.message).to.equal(message)
    expect(alertDialogStore.options).to.deep.equal({
      width: 400,
      noconfirm: false,
      variant: 'confirmation',
    })

    alertDialogStore.cancel()
    const result = await promise
    expect(result).to.be.false
  })

  it('should not resolve promise if dialog is not open', () => {
    alertDialogStore.agree()
    expect(alertDialogStore.dialog).to.be.false

    alertDialogStore.cancel()
    expect(alertDialogStore.dialog).to.be.false
  })
})
