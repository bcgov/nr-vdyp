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
 * Returns the full Axios response including headers and data.
 * @param formData The FormData containing necessary files and parameters.
 * @param trialRun Boolean flag indicating if this is a trial run (default is false).
 * @returns A promise that resolves to the full Axios response.
 */
export const projectionHcsvPost = async (
  formData: FormData,
  trialRun: boolean = false,
): Promise<any> => {
  try {
    const response = await apiClient.projectionHcsvPost(formData, trialRun)

    // Check status and content type
    if (
      (response.status === 200 || response.status === 201) &&
      response.headers['content-type']?.includes('application/octet-stream')
    ) {
      // Success response, returns a blob
      return response
    } else if (
      response.status === 400 &&
      response.headers['content-type']?.includes('application/json')
    ) {
      // Errors including validation messages
      const errorData = await response.data.text()
      throw new Error(errorData)
    } else {
      throw new Error(`Unexpected response: ${response.status}`)
    }
  } catch (error) {
    console.error('Error running projection:', {
      message: (error as any).message,
      code: (error as any).code,
      response: (error as any).response,
      request: (error as any).request,
      config: (error as any).config,
    })
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
