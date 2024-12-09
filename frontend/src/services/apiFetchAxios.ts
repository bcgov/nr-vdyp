import axiosInstance from './axiosInstance'
import { StatusCodes } from 'http-status-codes'

export default class ApiFetchAxios {
  static async getHelp(): Promise<any> {
    const response = await axiosInstance.get<any>('/api/v8/help')

    if (!response || !response.status || !response.data) {
      console.warn('Unexpected response format or status')
      return null
    }

    return response.status === StatusCodes.OK ? response.data : null
  }

  static async projectionHcsvPost(): Promise<Blob> {
    const body = {
      projectionParameters: {
        startingAge: null,
        finishingAge: null,
        ageIncrement: null,
      },
      layerInputData: null,
      polygonInputData: null,
    }

    const response = await axiosInstance.post<Blob>(
      '/api/v8/projection/hcsv',
      body,
      {
        headers: {
          'Content-Type': 'application/json',
        },
        responseType: 'blob',
      },
    )

    if (response.status === StatusCodes.CREATED) {
      return response.data
    } else {
      throw new Error(`Unexpected status code: ${response.status}`)
    }
  }
}
