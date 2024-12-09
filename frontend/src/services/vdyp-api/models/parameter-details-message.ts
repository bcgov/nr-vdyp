/* tslint:disable */
/* eslint-disable */
export interface ParameterDetailsMessage {
  /**
   * the parameter name
   *
   * @type {string}
   * @memberof ParameterDetailsMessage
   */
  field?: string

  /**
   * a brief description of the parameter's purpose
   *
   * @type {string}
   * @memberof ParameterDetailsMessage
   */
  shortDescription?: string

  /**
   * if the parameter has a value, a description of the value
   *
   * @type {string}
   * @memberof ParameterDetailsMessage
   */
  parameterValue?: string

  /**
   * a description of the parameter
   *
   * @type {string}
   * @memberof ParameterDetailsMessage
   */
  longDescription?: string

  /**
   * the default value used if the parameter is not specified
   *
   * @type {string}
   * @memberof ParameterDetailsMessage
   */
  defaultValue?: string
}
