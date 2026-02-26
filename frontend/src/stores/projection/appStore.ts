import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { DEFAULTS } from '@/constants'
import { PROJECTION_VIEW_MODE, PROJECTION_STATUS } from '@/constants/constants'
import type { ProjectionViewMode } from '@/types/types'
import type { ProjectionStatus } from '@/interfaces/interfaces'

export const useAppStore = defineStore('appStore', () => {
  const modelSelection = ref<string>(DEFAULTS.DEFAULT_VALUES.MODEL_SELECTION)

  // View mode: 'view' for read-only, 'edit' for editing, 'create' for new projection
  const viewMode = ref<ProjectionViewMode>(PROJECTION_VIEW_MODE.CREATE)

  // Current projection GUID being viewed/edited
  const currentProjectionGUID = ref<string | null>(null)

  // Current projection status
  const currentProjectionStatus = ref<ProjectionStatus>(PROJECTION_STATUS.DRAFT)

  // Saving state for progress indicator
  const isSavingProjection = ref(false)

  // Duplicate origin info: set when navigating to a newly duplicated projection
  const duplicatedFromInfo = ref<{ originalName: string; duplicatedAt: string } | null>(null)

  const getModelSelection = computed(() => modelSelection.value)
  const getViewMode = computed(() => viewMode.value)
  const getCurrentProjectionGUID = computed(() => currentProjectionGUID.value)
  const getCurrentProjectionStatus = computed(() => currentProjectionStatus.value)
  const isReadOnly = computed(() => viewMode.value === PROJECTION_VIEW_MODE.VIEW)
  const isDraft = computed(() => currentProjectionStatus.value === PROJECTION_STATUS.DRAFT)

  const setModelSelection = (newSelection: string) => {
    modelSelection.value = newSelection
  }

  const setViewMode = (mode: ProjectionViewMode) => {
    viewMode.value = mode
  }

  const setCurrentProjectionGUID = (guid: string | null) => {
    currentProjectionGUID.value = guid
  }

  const setCurrentProjectionStatus = (status: ProjectionStatus) => {
    currentProjectionStatus.value = status
  }

  const setDuplicatedFromInfo = (info: { originalName: string; duplicatedAt: string } | null) => {
    duplicatedFromInfo.value = info
  }

  // Reset state for new projection creation
  const resetForNewProjection = () => {
    viewMode.value = PROJECTION_VIEW_MODE.CREATE
    currentProjectionGUID.value = null
    currentProjectionStatus.value = PROJECTION_STATUS.DRAFT
    isSavingProjection.value = false
    duplicatedFromInfo.value = null
  }

  return {
    modelSelection,
    viewMode,
    currentProjectionGUID,
    currentProjectionStatus,
    getModelSelection,
    getViewMode,
    getCurrentProjectionGUID,
    getCurrentProjectionStatus,
    isReadOnly,
    isDraft,
    setModelSelection,
    setViewMode,
    setCurrentProjectionGUID,
    setCurrentProjectionStatus,
    isSavingProjection,
    duplicatedFromInfo,
    setDuplicatedFromInfo,
    resetForNewProjection,
  }
})
