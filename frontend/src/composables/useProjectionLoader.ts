import { ref } from 'vue'
import {
  getProjectionById,
  parseProjectionParams,
  mapProjectionStatus,
  getFileSetFiles,
} from '@/services/projectionService'
import { useAppStore } from '@/stores/projection/appStore'
import { useModelParameterStore } from '@/stores/projection/modelParameterStore'
import { useFileUploadStore } from '@/stores/projection/fileUploadStore'
import {
  MODEL_SELECTION,
  PROJECTION_STATUS,
  PROJECTION_VIEW_MODE
} from '@/constants/constants'
import {
  ExecutionOptionsEnum,
  type ModelParameters,
} from '@/services/vdyp-api'
import type { ProjectionViewMode } from '@/types/types'

export function useProjectionLoader() {
  const isLoading = ref(false)
  const loadError = ref<string | null>(null)

  const appStore = useAppStore()
  const modelParameterStore = useModelParameterStore()
  const fileUploadStore = useFileUploadStore()

/**
 * Loads file set files and applies the file info to the store
 */
  const loadFileSetInfo = async (
    projectionGUID: string,
    fileSetGUID: string,
    defaultName: string,
    setFileInfo: (info: { filename: string; fileMappingGUID: string; fileSetGUID: string }) => void,
  ) => {
    try {
      const files = await getFileSetFiles(projectionGUID, fileSetGUID)
      if (files.length > 0) {
        const file = files[0]
        setFileInfo({
          filename: file.filename || defaultName,
          fileMappingGUID: file.fileMappingGUID,
          fileSetGUID,
        })
      }
    } catch (err) {
      console.error(`Error loading ${defaultName.toLowerCase()} info:`, err)
    }
  }

/**
 * Restores model parameter state from a projection
 */
  const restoreInputModelParamsState = (
    modelParameters: string | null | undefined,
    params: ReturnType<typeof parseProjectionParams>,
    isViewMode: boolean,
  ) => {
    modelParameterStore.resetStore()

    if (modelParameters) {
      try {
        const modelParams: ModelParameters = JSON.parse(modelParameters)
        modelParameterStore.restoreFromModelParameters(modelParams)
      } catch (err) {
        console.error('Error parsing modelParameters:', err)
      }
    }

    modelParameterStore.restoreFromProjectionParams(params, isViewMode)
  }

/**
 * Restores file upload state from a projection
 */
  const restoreFileUploadState = async (
    projectionGUID: string,
    projectionModel: Awaited<ReturnType<typeof getProjectionById>>,
    params: ReturnType<typeof parseProjectionParams>,
    isViewMode: boolean,
  ) => {
    fileUploadStore.resetStore()
    fileUploadStore.restoreFromProjectionParams(params, isViewMode)

    const polygonFileSetGUID = projectionModel.polygonFileSet?.projectionFileSetGUID
    const layerFileSetGUID = projectionModel.layerFileSet?.projectionFileSetGUID

    await Promise.all([
      polygonFileSetGUID
        ? loadFileSetInfo(projectionGUID, polygonFileSetGUID, 'Polygon File', fileUploadStore.setPolygonFileInfo)
        : Promise.resolve(),
      layerFileSetGUID
        ? loadFileSetInfo(projectionGUID, layerFileSetGUID, 'Layer File', fileUploadStore.setLayerFileInfo)
        : Promise.resolve(),
    ])
  }

  /**
   * Loads a projection by GUID and populates all stores.
   * Does NOT navigate — caller is responsible for navigation.
   */
  const loadProjection = async (projectionGUID: string, viewMode: ProjectionViewMode): Promise<boolean> => {
    isLoading.value = true
    loadError.value = null

    try {
      const isViewMode = viewMode === PROJECTION_VIEW_MODE.VIEW
      const projectionModel = await getProjectionById(projectionGUID)
      const params = parseProjectionParams(projectionModel.projectionParameters)

      const isInputModelParams = params.selectedExecutionOptions.includes(
        ExecutionOptionsEnum.DoEnableProjectionReport,
      )
      const method = isInputModelParams
        ? MODEL_SELECTION.INPUT_MODEL_PARAMETERS
        : MODEL_SELECTION.FILE_UPLOAD

      appStore.setModelSelection(method)
      appStore.setViewMode(viewMode)
      appStore.setCurrentProjectionGUID(projectionGUID)
      appStore.setCurrentProjectionStatus(
        mapProjectionStatus(projectionModel.projectionStatusCode?.code || PROJECTION_STATUS.DRAFT),
      )

      // Set or clear duplicated-from info based on copyTitle in projectionParameters
      if (params.copyTitle) {
        appStore.setDuplicatedFromInfo({
          originalName: params.copyTitle,
          duplicatedAt: projectionModel.createDate || new Date().toISOString(),
        })
      } else {
        appStore.setDuplicatedFromInfo(null)
      }

      if (isInputModelParams) {
        restoreInputModelParamsState(projectionModel.modelParameters, params, isViewMode)
      } else {
        await restoreFileUploadState(projectionGUID, projectionModel, params, isViewMode)
      }

      return true
    } catch (err) {
      console.error('Error loading projection:', err)
      loadError.value = 'Failed to load the projection. Please try again later.'
      return false
    } finally {
      isLoading.value = false
    }
  }

  return {
    isLoading,
    loadError,
    loadProjection,
  }
}
