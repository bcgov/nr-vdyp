import { StatusCodes } from 'http-status-codes'
import { useAuthStore } from '@/stores/common/authStore'

export default class ApiFetchDirect {
  getRequest(path: string, headers: any): Promise<Response> {
    return this.request(path, 'GET', null, headers)
  }

  request(
    path: string,
    type: string,
    body: any,
    headers: any,
  ): Promise<Response> {
    const request: any = {
      method: type,
      headers: headers,
    }

    if (body && (type === 'POST' || type === 'PUT')) {
      request.body = JSON.stringify(body)
    }

    console.log(`path: ${path}`)
    console.log(`request: ${JSON.stringify(request)}`)

    return fetch(path, request)
  }

  async getHelp(): Promise<any> {
    const apiUrl = '/api/v8/help'
    const authStore = useAuthStore()
    let token
    if (authStore && authStore.user && authStore.user.accessToken) {
      token = authStore.user.accessToken
    } else {
      console.warn('Authorization token or authStore is not available.')
    }
    console.log(token)

    const response = await this.getRequest(apiUrl, {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    })

    if (response.status === StatusCodes.OK) {
      return response.json()
    } else {
      return null
    }
  }

  async projectionHcsvPost(): Promise<any> {
    const apiUrl = '/api/v8/projection/hcsv'
    const authStore = useAuthStore()
    let token

    if (authStore && authStore.user && authStore.user.accessToken) {
      token = authStore.user.accessToken
    } else {
      console.warn('Authorization token or authStore is not available.')
    }

    const body = {
      projectionParameters: {
        startingAge: null,
        finishingAge: null,
        ageIncrement: null,
      },
      layerInputData: null,
      polygonInputData: null,
    }

    const response = await this.request(apiUrl, 'POST', body, {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    })

    if (response.status === StatusCodes.CREATED) {
      return response.blob()
    } else {
      throw new Error(`Error: ${response.status} ${response.statusText}`)
    }
  }
}
