import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { DEFAULTS } from '@/constants'

export const useAppStore = defineStore('appStore', () => {
  const modelSelection = ref<string>(DEFAULTS.DEFAULT_VALUES.MODEL_SELECTION)

  const getModelSelection = computed(() => modelSelection.value)

  const setModelSelection = (newSelection: string) => {
    modelSelection.value = newSelection
  }

  return {
    modelSelection,
    getModelSelection,
    setModelSelection,
  }
})
