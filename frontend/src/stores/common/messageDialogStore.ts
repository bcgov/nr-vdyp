import { defineStore } from 'pinia'

interface DialogOptions {
  color: string
  width: number
}

interface MessageDialogState {
  dialog: boolean
  title: string | null
  message: string | null
  options: DialogOptions
  btnLabel: string | null
}

export const useMessageDialogStore = defineStore('messageDialog', {
  state: (): MessageDialogState => ({
    dialog: false,
    title: null,
    message: null,
    options: {
      color: 'grey lighten-3',
      width: 400,
    },
    btnLabel: null,
  }),

  getters: {
    isOpen: (state) => state.dialog,
    dialogTitle: (state) => state.title,
    dialogMessage: (state) => state.message,
    dialogOptions: (state) => state.options,
    dialogBtnLabel: (state) => state.btnLabel,
  },

  actions: {
    openDialog(
      newTitle: string,
      newMessage: string,
      newOptions?: Partial<DialogOptions>,
      newBtnLabel?: string | null,
    ): void {
      this.dialog = true
      this.title = newTitle
      this.message = newMessage
      if (newOptions) {
        this.options = { ...this.options, ...newOptions }
      }
      this.btnLabel = newBtnLabel ?? 'Continue Editing'
    },

    agree() {
      this.dialog = false
    },
  },
})
