import apiClient from '@/services/apiClient'

export const helpGet = async (): Promise<any> => {
  try {
    const response = await apiClient.helpGet()
    return response.data
  } catch (error) {
    console.error('Error fetching help details:', error)
    throw error
  }
}

export const projectionHcsvPost = async (body: any): Promise<Blob> => {
  try {
    const response = await apiClient.projectionHcsvPost(body)
    return response.data
  } catch (error) {
    console.error('Error running projection:', error)
    throw error
  }
}

export const rootGet = async (): Promise<any> => {
  try {
    const response = await apiClient.rootGet()
    return response.data
  } catch (error) {
    console.error('Error fetching root details:', error)
    throw error
  }
}
