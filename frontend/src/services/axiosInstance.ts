import axios from 'axios'
import type {
  AxiosInstance,
  AxiosResponse,
  InternalAxiosRequestConfig,
} from 'axios'
import { useAuthStore } from '@/stores/common/authStore'
import { handleTokenValidation } from '@/services/keycloak'
import { AXIOS } from '@/constants/constants'
import { logErrorMessage } from '@/utils/messageHandler'
import { AXIOS_INST_ERR } from '@/constants/message'

const axiosInstance: AxiosInstance = axios.create({
  headers: {
    Accept: AXIOS.ACCEPT,
    'Content-Type': AXIOS.CONTENT_TYPE,
  },
})

axiosInstance.interceptors.request.use(
  async (config: InternalAxiosRequestConfig) => {
    console.debug(
      `Request timeout: ${config.timeout}, ${JSON.stringify(config.headers)}, method: ${config.method}, responseType: ${config.responseType}, url: ${config.url}`,
    )

    const authStore = useAuthStore()
    if (authStore?.user?.accessToken) {
      // Validate and refresh token if necessary
      await handleTokenValidation()

      // After validation/refresh, set the latest token
      const token = authStore.user.accessToken
      if (token && config.headers) {
        config.headers.Authorization = `Bearer ${token}`
      }
    } else {
      logErrorMessage(
        AXIOS_INST_ERR.SESSION_INACTIVE,
        'Authorization token or authStore is not available.',
      )
    }

    return config
  },
  (error) => {
    console.error('Request error:', error)
    throw convertToAxiosError(error)
  },
)

axiosInstance.interceptors.response.use(
  (response: AxiosResponse) => response,
  async (error) => {
    const originalRequest = error.config
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true
      const authStore = useAuthStore()

      // Validate and refresh token safely
      await handleTokenValidation()

      // After refresh, retry with new token
      if (authStore.user?.accessToken) {
        originalRequest.headers.Authorization = `Bearer ${authStore.user.accessToken}`
        return axiosInstance(originalRequest)
      }
    }

    throw convertToAxiosError(error)
  },
)

// convert an error object to AxiosError
const convertToAxiosError = (error: unknown): Error => {
  if (error instanceof Error) {
    return error
  }

  return new Error(String(error))
}

export default axiosInstance
