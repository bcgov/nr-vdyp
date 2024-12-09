import { defineStore } from 'pinia'
import { ref } from 'vue'
import { MODEL_SELECTION } from '@/constants/constants'

export const useAppStore = defineStore('appStore', () => {
  // Model Parameter Selection bar
  const modelSelection = ref<string>(MODEL_SELECTION.FILE_UPLOAD)

  // Tabs
  const currentTab = ref<number>(0)

  return {
    // Model Parameter Selection bar
    modelSelection,
    // Tabs
    currentTab,
  }
})
