import apiClient from '@/services/apiClient'
import type { ParameterDetailsMessage, RootResource } from '@/services/vdyp-api'

/**
 * Fetches help details from the API.
 * @returns A promise that resolves to an array of ParameterDetailsMessage.
 */
export const helpGet = async (): Promise<ParameterDetailsMessage[]> => {
  try {
    const response = await apiClient.helpGet()
    return response.data
  } catch (error) {
    console.error('Error fetching help details:', error)
    throw error
  }
}

/**
 * Sends a projection HCSV POST request to the API using the provided FormData.
 * @param formData The FormData containing necessary files and parameters.
 * @param trialRun Boolean flag indicating if this is a trial run (default is false).
 * @returns A promise that resolves to a Blob containing the projection result.
 */
export const projectionHcsvPost = async (
  formData: FormData,
  trialRun: boolean = false,
): Promise<Blob> => {
  try {
    const response = await projectionHcsvPostResponse(formData, trialRun)
    return response.data
  } catch (error) {
    console.error('Error running projection:', error)
    throw error
  }
}

/**
 * Sends a projection HCSV POST request and returns the full Axios response.
 * Useful for cases where need access to the response headers.
 * @param formData The FormData containing necessary files and parameters.
 * @param trialRun Boolean flag indicating if this is a trial run (default is false).
 * @returns A promise that resolves to the full Axios response.
 */
export const projectionHcsvPostResponse = async (
  formData: FormData,
  trialRun: boolean = false,
): Promise<any> => {
  try {
    const response = await apiClient.projectionHcsvPost(formData, trialRun)
    return response
  } catch (error) {
    console.error('Error running projection:', error)
    throw error
  }
}

/**
 * Fetches root details from the API.
 * @returns A promise that resolves to a RootResource object.
 */
export const rootGet = async (): Promise<RootResource> => {
  try {
    const response = await apiClient.rootGet()
    return response.data
  } catch (error) {
    console.error('Error fetching root details:', error)
    throw error
  }
}
