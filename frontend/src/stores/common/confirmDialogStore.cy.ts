/// <reference types="cypress" />

import { setActivePinia, createPinia } from 'pinia'
import { useConfirmDialogStore } from '@/stores/common/confirmDialogStore'

describe('Confirm Dialog Store Unit Tests', () => {
  let confirmDialogStore: ReturnType<typeof useConfirmDialogStore>

  beforeEach(() => {
    setActivePinia(createPinia())
    confirmDialogStore = useConfirmDialogStore()
  })

  it('should initialize with default state', () => {
    expect(confirmDialogStore.dialog).to.be.false
    expect(confirmDialogStore.resolve).to.be.null
    expect(confirmDialogStore.title).to.be.null
    expect(confirmDialogStore.message).to.be.null
    expect(confirmDialogStore.options).to.deep.equal({
      width: 400,
      noconfirm: false,
    })
  })

  it('should return correct getter values', () => {
    expect(confirmDialogStore.getIsOpen).to.be.false
    expect(confirmDialogStore.getDialogTitle).to.be.null
    expect(confirmDialogStore.getDialogMessage).to.be.null
    expect(confirmDialogStore.getDialogOptions).to.deep.equal({
      width: 400,
      noconfirm: false,
    })
  })

  it('should open dialog and set title, message, and options', async () => {
    const title = 'Confirm Action'
    const message = 'Are you sure?'
    const options = { width: 500, noconfirm: true }

    const promise = confirmDialogStore.openDialog(title, message, options)

    expect(confirmDialogStore.dialog).to.be.true
    expect(confirmDialogStore.title).to.equal(title)
    expect(confirmDialogStore.message).to.equal(message)
    expect(confirmDialogStore.options).to.deep.equal(options)

    confirmDialogStore.agree()
    const result = await promise
    expect(result).to.be.true
  })

  it('should resolve promise with true when agree is called', async () => {
    const promise = confirmDialogStore.openDialog('Test', 'Test message')
    confirmDialogStore.agree()
    const result = await promise
    expect(result).to.be.true
    expect(confirmDialogStore.dialog).to.be.false
  })

  it('should resolve promise with false when cancel is called', async () => {
    const promise = confirmDialogStore.openDialog('Test', 'Test message')
    confirmDialogStore.cancel()
    const result = await promise
    expect(result).to.be.false
    expect(confirmDialogStore.dialog).to.be.false
  })

  it('should reset state after agree or cancel', () => {
    confirmDialogStore.openDialog('Test', 'Test message')
    confirmDialogStore.agree()
    expect(confirmDialogStore.resolve).to.be.null
    expect(confirmDialogStore.title).to.equal('')
    expect(confirmDialogStore.message).to.equal('')
    expect(confirmDialogStore.options).to.deep.equal({
      width: 400,
      noconfirm: false,
    })

    confirmDialogStore.openDialog('Test', 'Test message')
    confirmDialogStore.cancel()
    expect(confirmDialogStore.resolve).to.be.null
    expect(confirmDialogStore.title).to.equal('')
    expect(confirmDialogStore.message).to.equal('')
    expect(confirmDialogStore.options).to.deep.equal({
      width: 400,
      noconfirm: false,
    })
  })

  it('should handle openDialog without options', async () => {
    const title = 'No Options'
    const message = 'This has default options'

    const promise = confirmDialogStore.openDialog(title, message)

    expect(confirmDialogStore.dialog).to.be.true
    expect(confirmDialogStore.title).to.equal(title)
    expect(confirmDialogStore.message).to.equal(message)
    expect(confirmDialogStore.options).to.deep.equal({
      width: 400,
      noconfirm: false,
    })

    confirmDialogStore.cancel()
    const result = await promise
    expect(result).to.be.false
  })

  it('should not resolve promise if dialog is not open', () => {
    confirmDialogStore.agree()
    expect(confirmDialogStore.dialog).to.be.false

    confirmDialogStore.cancel()
    expect(confirmDialogStore.dialog).to.be.false
  })
})
