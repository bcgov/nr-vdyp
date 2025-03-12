import { Configuration } from './configuration'
import globalAxios from 'axios'
import type { AxiosRequestConfig, AxiosInstance } from 'axios'

export const BASE_PATH = ''

export const COLLECTION_FORMATS = {
  csv: ',',
  ssv: ' ',
  tsv: '\t',
  pipes: '|',
}

export interface RequestArgs {
  url: string
  options: AxiosRequestConfig
}

export class BaseAPI {
  protected configuration: Configuration | undefined

  constructor(
    configuration?: Configuration,
    protected basePath: string = BASE_PATH,
    protected axios: AxiosInstance = globalAxios,
  ) {
    if (configuration) {
      this.configuration = configuration
      this.basePath = configuration.basePath || this.basePath
    }
  }
}

export class RequiredError extends Error {
  readonly name = 'RequiredError'
  constructor(
    public field: string,
    msg?: string,
  ) {
    super(msg)
  }
}
