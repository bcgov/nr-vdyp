import axios from 'axios'
import type { CancelTokenSource } from 'axios'

export const createCancelToken = (): CancelTokenSource => {
  return axios.CancelToken.source()
}
