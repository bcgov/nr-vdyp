import apiClient from '@/services/apiClient'
import type { ParameterDetailsMessage, RootResource } from '@/services/vdyp-api'

export const helpGet = async (): Promise<ParameterDetailsMessage[]> => {
  try {
    const response = await apiClient.helpGet()
    return response.data
  } catch (error) {
    console.error('Error fetching help details:', error)
    throw error
  }
}

export const projectionHcsvPost = async (
  formData: FormData,
  trialRun: boolean = false,
): Promise<Blob> => {
  try {
    const response = await apiClient.projectionHcsvPost(formData, trialRun)
    return response.data
  } catch (error) {
    console.error('Error running projection:', error)
    throw error
  }
}

export const rootGet = async (): Promise<RootResource> => {
  try {
    const response = await apiClient.rootGet()
    return response.data
  } catch (error) {
    console.error('Error fetching root details:', error)
    throw error
  }
}
