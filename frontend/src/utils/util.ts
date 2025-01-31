export class Util {
  /**
   * GENERATING
   */

  /**
   * Generates a unique numeric FEATURE_ID.
   * The FEATURE_ID is a 7 to 8-digit number.
   *
   * @returns {number} A unique FEATURE_ID as an integer.
   */
  public static generateFeatureId(): number {
    // Generate a random 7 to 8-digit number
    return Math.floor(1000000 + Math.random() * 9000000)
  }

  /**
   * VALIDATATING
   */
  public static readonly isZeroValue = (value: any): boolean => {
    if (value === null || value === undefined) {
      return false
    }

    const trimmedValue = this.trimValue(value)

    if (typeof trimmedValue === 'string' && trimmedValue.trim() === '') {
      return false
    }

    const numericValue = Number(trimmedValue)

    return !isNaN(numericValue) && numericValue === 0
  }

  public static readonly isEmptyOrZero = (
    value: number | string | null,
  ): boolean => {
    const trimmedValue = this.trimValue(value)
    return this.isZeroValue(trimmedValue) || this.isBlank(trimmedValue)
  }

  // undefined, null, NaN, 0, "" (empty string), and false
  // isBlank(" ") = false
  static isBlank(item: any): boolean {
    if (item === undefined || item === null || Number.isNaN(item)) {
      return true
    } else if (Array.isArray(item)) {
      return item.length === 0
    } else if (typeof item === 'string') {
      return item.trim().length === 0
    }
    return false
  }

  static areEqual(array1: Array<any>, array2: Array<any>): boolean {
    if (array1.length === array2.length) {
      return array1.every((element, index) => {
        if (element === array2[index]) {
          return true
        }

        return false
      })
    }

    return false
  }

  static isJson(str: any) {
    try {
      JSON.parse(str)
    } catch (e) {
      return false
    }
    return true
  }

  static isURL(input: string): boolean {
    // Regular expression for URL validation
    const urlRegex = /^(https?|ftp):\/\/[^\s/$.?#].[^\s]*$/
    return urlRegex.test(input)
  }

  /**
   * FORMATTING
   */

  /**
   * Returns the parameter of the given date type as a string with a certain pattern.
   * @param date
   * @returns YYYY-MM-DD hh:mm:ss
   */
  static formatDateTime(date: Date | null): string | null {
    if (!date) {
      return null
    }

    const year = new Intl.DateTimeFormat('en', { year: 'numeric' }).format(date)
    const month = new Intl.DateTimeFormat('en', { month: '2-digit' }).format(
      date,
    )
    const day = new Intl.DateTimeFormat('en', { day: '2-digit' }).format(date)

    const hour = date.getHours().toString().padStart(2, '0')
    const minute = date.getMinutes().toString().padStart(2, '0')
    const second = date.getSeconds().toString().padStart(2, '0')

    return `${year}-${month}-${day} ${hour}:${minute}:${second}`
  }

  static formatLargeNumber(number: number, decimalPlaces = 10): string | null {
    if (number) {
      return number.toFixed(decimalPlaces).replace(/\.?0+$/, '') // Remove trailing zeros
    } else {
      return null
    }
  }

  static formatNumberFractionGroup(
    minimumFractionDigits: number,
    maximumFractionDigits: number,
  ): any {
    return new Intl.NumberFormat('en-US', {
      minimumFractionDigits: minimumFractionDigits,
      maximumFractionDigits: maximumFractionDigits,
      useGrouping: true,
    })
  }

  static formatNumberGroup(): any {
    return new Intl.NumberFormat('en-US', {
      useGrouping: true,
    })
  }

  /**
   * converts a given Unix timestamp to a Date object.
   * @param {number} unixTimestamp - Unix timestamps in seconds
   * @returns {Date | null} the converted Date object or null if the conversion failed
   */
  static formatUnixTimestampToDate(timestamp: number): Date | null {
    try {
      const date = new Date(timestamp * 1000) // Convert seconds to milliseconds
      return date
    } catch (error) {
      console.error('Failed to convert timestamp to date:', error)
      return null
    }
  }

  /**
   * INSERTING
   */
  static insertComma(item: string): string {
    if (!Util.isBlank(item)) {
      return ', '
    } else {
      return ''
    }
  }

  static insertBR(item: string): string {
    if (!Util.isBlank(item)) {
      return '<br/>'
    } else {
      return ''
    }
  }

  /**
   * CONVERTING
   */

  public static readonly trimValue = (value: any): any => {
    if (typeof value === 'string') {
      return value.trim()
    }
    return value
  }

  /**
   * Parses the input value and converts it to a number or null.
   * If the input is an empty string, it returns null.
   * If the input is a valid number, it returns the number.
   * @param value - The value to be parsed (string | number | null)
   * @returns number | null
   */
  static parseNumberOrNull(value: string | number | null): number | null {
    if (value === '' || value === null) {
      return null
    }

    const parsedValue = Number(value)

    return isNaN(parsedValue) ? null : parsedValue
  }

  /**
   * Converts the input to a number if it's a string and returns it,
   * otherwise returns the input unchanged if it's a number.
   * The conversion only occurs if the input is not blank.
   *
   * @param item - The input to be processed (string or number or null).
   * @returns The processed input: number or unchanged if blank or not a string.
   */
  static toNumber(item: string | number | null): number | null {
    if (Util.isBlank(item)) {
      return null
    }

    if (typeof item === 'string') {
      const convertedNumber = Number(this.trimValue(item))
      return isNaN(convertedNumber) ? null : convertedNumber
    }

    if (typeof item === 'number') {
      return item
    }

    return null
  }

  static arrayToString(array: Array<any>, separator: string): string {
    if (array) {
      return array.join(separator)
    } else {
      return ''
    }
  }

  /**
   * EXTRACTING
   */

  /**
   * Extracts the numeric value from a given string.
   * It parses the leading numeric part of the string, ignoring any following non-numeric characters.
   * If the input string does not contain a valid numeric prefix, the function returns 0.
   *
   * @param input - The input string that may contain numeric values.
   * @returns A number parsed from the leading numeric part of the string, or 0 if no valid number is found.
   */
  static extractNumeric(input: string): number {
    const match = input.match(/^\s*-?\d+(\.\d+)?/)
    return match ? parseFloat(match[0]) : 0
  }

  static translateCode(code: string, codeList: Array<any>): string {
    if (code && codeList && codeList.length > 0) {
      return codeList.find((c) => c.codeName === code).description
    } else {
      return ''
    }
  }
  static userIdConversion(item: string): string {
    if (item && item.indexOf('\\') !== -1) {
      return item.split('\\')[1]
    } else {
      return item
    }
  }

  /**
   * Extracts all CSS styles from the provided style sheets that are either inline or belong to the same origin.
   * This function iterates through each style sheet and retrieves the CSS rules, appending them to a single string.
   *
   * @param {StyleSheetList} styleSheets - The list of style sheets from the document to extract CSS from.
   * @returns {string} A concatenated string of all CSS rules from the provided style sheets.
   */
  static extractStylesFromDocument(styleSheets: StyleSheetList): string {
    let styles = ''
    for (const sheet of Array.from(styleSheets)) {
      if (!sheet.href || sheet.href.startsWith(window.location.origin)) {
        try {
          if (sheet.cssRules) {
            for (const rule of Array.from(sheet.cssRules)) {
              styles += rule.cssText
            }
          }
        } catch (e) {
          console.warn('Could not access stylesheet:', sheet, e)
        }
      }
    }

    return styles
  }

  /**
   * SPLITING
   */
  static splitStringByCharCamelCase(inString: string): string {
    return !Util.isBlank(inString)
      ? inString.replace(/([a-z0-9])([A-Z])/g, '$1 $2')
      : ''
  }

  /**
   * MISCELLANEOUS
   */
  static flattenArray(inputArray: any[]): any[] {
    const result: any[] = []

    const flattenHelper = function (arr: any[]) {
      for (const element of arr) {
        if (Array.isArray(element) && Array.isArray(element[0])) {
          flattenHelper(element)
        } else {
          result.push(element)
        }
      }
    }

    flattenHelper(inputArray)
    return result
  }

  static openRequestedPopup(
    url: string,
    windowsName: string,
    windowFeatures?: string,
  ): void {
    if (Util.isBlank(windowFeatures))
      windowFeatures = 'toolbar=0,location=0,menubar=0'
    window.open(url, windowsName, windowFeatures)
  }

  static async delay(ms: number): Promise<any> {
    return new Promise((resolve) => setTimeout(resolve, ms))
  }

  static deepCopy(obj: any): any {
    return JSON.parse(JSON.stringify(obj)) as typeof obj
  }

  /**
   * Increases the numeric value extracted from the input string by a specified step,
   * ensuring that the result stays within the provided minimum and maximum limits.
   *
   * @param value - The input value as a string (e.g., '10.5') or null. It can contain numeric and non-numeric characters.
   * @param max - The maximum allowable value. If the calculated value exceeds this, the maximum value is returned.
   * @param min - The minimum allowable value. If the calculated value is lower than this, the minimum value is returned.
   * @param step - The amount by which to increase the numeric value.
   * @returns The new value, ensuring it is within the [min, max] range.
   */
  static increaseItemBySpinButton(
    value: string | null,
    max: number,
    min: number,
    step: number,
  ): number {
    let newValue

    // If value is null, assign step value and format
    if (!value) {
      newValue = step
    } else {
      // extract only numbers, commas, and minus signs
      const extractedValue = value.replace(/[^\d.-]/g, '')

      const numericValue = parseFloat(extractedValue)

      // Check if the extracted value is a valid number
      if (isNaN(numericValue)) {
        newValue = step // Assign step value if invalid
      } else {
        newValue = numericValue + step
      }

      if (newValue < min) {
        // 0
        newValue = min // 0
      }
    }

    if (newValue > max) {
      newValue = max
    }

    return newValue
  }

  /**
   * Decreases the numeric value extracted from the input string by a specified step,
   * ensuring that the result stays within the provided minimum and maximum limits.
   *
   * @param value - The input value as a string (e.g., '10.5') or null. It can contain numeric and non-numeric characters.
   * @param max - The maximum allowable value. If the calculated value exceeds this, the maximum value is returned.
   * @param min - The minimum allowable value. If the calculated value is lower than this, the minimum value is returned.
   * @param step - The amount by which to decrease the numeric value.
   * @returns The new value, ensuring it is within the [min, max] range.
   */
  static decrementItemBySpinButton(
    value: string | null,
    max: number,
    min: number,
    step: number,
  ): number {
    let newValue
    if (!value) {
      newValue = min // 0
    } else {
      // extract only numbers, commas, and minus signs
      const extractedValue = value.replace(/[^\d.-]/g, '')

      const numericValue = parseFloat(extractedValue)

      if (isNaN(numericValue)) {
        newValue = min // 0
      } else {
        newValue = numericValue - step
      }

      if (newValue < min) {
        newValue = min // 0
      }

      if (newValue > max) {
        newValue = max
      }
    }

    return newValue
  }
}
