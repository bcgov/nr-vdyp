import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

type DialogVariant = 'info' | 'confirmation' | 'warning' | 'error' | 'destructive'

interface DialogOptions {
  width: number
  noconfirm: boolean
  variant?: DialogVariant
}

export const useAlertDialogStore = defineStore('alertDialog', () => {
  const dialog = ref<boolean>(false)
  const resolve = ref<((value: boolean) => void) | null>(null)
  const title = ref<string | null>(null)
  const message = ref<string | null>(null)
  const options = ref<DialogOptions>({
    width: 400,
    noconfirm: false,
    variant: 'confirmation',
  })

  const getIsOpen = computed(() => dialog.value)
  const getDialogTitle = computed(() => title.value)
  const getDialogMessage = computed(() => message.value)
  const getDialogOptions = computed(() => options.value)

  const openDialog = (
    newTitle: string,
    newMessage: string,
    newOptions?: Partial<DialogOptions>,
  ): Promise<boolean> => {
    dialog.value = true
    title.value = newTitle
    message.value = newMessage
    if (newOptions) {
      options.value = { ...options.value, ...newOptions }
    }
    return new Promise<boolean>((resolvePromise) => {
      resolve.value = resolvePromise
    })
  }

  const agree = () => {
    if (resolve.value) resolve.value(true)
    dialog.value = false
    resetState()
  }

  const cancel = () => {
    if (resolve.value) resolve.value(false)
    dialog.value = false
    resetState()
  }

  const resetState = () => {
    resolve.value = null
    title.value = ''
    message.value = ''
    options.value = { width: 400, noconfirm: false, variant: 'confirmation' }
  }

  return {
    dialog,
    resolve,
    title,
    message,
    options,
    getIsOpen,
    getDialogTitle,
    getDialogMessage,
    getDialogOptions,
    openDialog,
    agree,
    cancel,
    resetState,
  }
})
