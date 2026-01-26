import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { DEFAULTS } from '@/constants'
import { PROJECTION_VIEW_MODE } from '@/constants/constants'
import type { ProjectionViewMode } from '@/types/types'

export const useAppStore = defineStore('appStore', () => {
  const modelSelection = ref<string>(DEFAULTS.DEFAULT_VALUES.MODEL_SELECTION)

  // View mode: 'view' for read-only, 'edit' for editing, 'create' for new projection
  const viewMode = ref<ProjectionViewMode>(PROJECTION_VIEW_MODE.CREATE)

  // Current projection GUID being viewed/edited
  const currentProjectionGUID = ref<string | null>(null)

  const getModelSelection = computed(() => modelSelection.value)
  const getViewMode = computed(() => viewMode.value)
  const getCurrentProjectionGUID = computed(() => currentProjectionGUID.value)
  const isReadOnly = computed(() => viewMode.value === PROJECTION_VIEW_MODE.VIEW)

  const setModelSelection = (newSelection: string) => {
    modelSelection.value = newSelection
  }

  const setViewMode = (mode: ProjectionViewMode) => {
    viewMode.value = mode
  }

  const setCurrentProjectionGUID = (guid: string | null) => {
    currentProjectionGUID.value = guid
  }

  // Reset state for new projection creation
  const resetForNewProjection = () => {
    viewMode.value = PROJECTION_VIEW_MODE.CREATE
    currentProjectionGUID.value = null
  }

  return {
    modelSelection,
    viewMode,
    currentProjectionGUID,
    getModelSelection,
    getViewMode,
    getCurrentProjectionGUID,
    isReadOnly,
    setModelSelection,
    setViewMode,
    setCurrentProjectionGUID,
    resetForNewProjection,
  }
})
