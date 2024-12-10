import { defineStore } from 'pinia'

interface DialogOptions {
  width: number
  noconfirm: boolean
}

interface ConfirmDialogState {
  dialog: boolean
  resolve: ((value: boolean) => void) | null
  title: string | null
  message: string | null
  options: DialogOptions
}

export const useConfirmDialogStore = defineStore('confirmDialog', {
  state: (): ConfirmDialogState => ({
    dialog: false,
    resolve: null,
    title: null,
    message: null,
    options: {
      width: 400,
      noconfirm: false,
    },
  }),

  getters: {
    isOpen: (state) => state.dialog,
    dialogTitle: (state) => state.title,
    dialogMessage: (state) => state.message,
    dialogOptions: (state) => state.options,
  },

  actions: {
    openDialog(
      newTitle: string,
      newMessage: string,
      newOptions?: Partial<DialogOptions>,
    ): Promise<boolean> {
      this.dialog = true
      this.title = newTitle
      this.message = newMessage
      if (newOptions) {
        this.options = { ...this.options, ...newOptions }
      }
      return new Promise<boolean>((resolve) => {
        this.resolve = resolve
      })
    },

    agree() {
      if (this.resolve) this.resolve(true)
      this.dialog = false
      this.resetState()
    },

    cancel() {
      if (this.resolve) this.resolve(false)
      this.dialog = false
      this.resetState()
    },

    resetState() {
      this.resolve = null
      this.title = ''
      this.message = ''
      this.options = { width: 400, noconfirm: false }
    },
  },
})
