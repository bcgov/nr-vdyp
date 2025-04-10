import axios from 'axios'
import type {
  AxiosInstance,
  AxiosResponse,
  InternalAxiosRequestConfig,
} from 'axios'
import { useAuthStore } from '@/stores/common/authStore'
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
      `Request timeout: ${config.timeout}, ${config.headers}, method: ${config.method}, responseType: ${config.responseType}, url: ${config.url}`,
    )
    // TODO - performance issues or network overhead issue?
    // then consider timer-based refresh + refresh token on 401 error
    // Ensure token is valid or refreshed

    const authStore = useAuthStore()
    if (authStore && authStore.user && authStore.user.accessToken) {
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
    return Promise.reject(convertToAxiosError(error))
  },
)

axiosInstance.interceptors.response.use(
  (response: AxiosResponse) => response,
  async (error) => {
    // TODO - 401 Unauthorized handling: Retry after token renewal
    // TODO - Other alternatives => axios-auth-refresh npm package

    return Promise.reject(convertToAxiosError(error))
  },
)

// TODO - If don't need 401 Unauthorized handling in interceptors.response, use the following

// convert an error object to AxiosError
const convertToAxiosError = (error: unknown): Error => {
  if (error instanceof Error) {
    return error
  }

  return new Error(String(error))
}

// TODO - Other alternatives => axios-auth-refresh npm package

export default axiosInstance
