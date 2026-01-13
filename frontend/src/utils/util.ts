import type { NumStrNullType } from '@/types/types'
import JSZip from 'jszip'
import { CONSTANTS } from '@/constants'
import { ExecutionOptionsEnum } from '@/services/vdyp-api'

/**
 * Trims whitespace from a string value. Non-string values are returned unchanged.
 *
 * @param value - The value to trim (accepts any type)
 * @returns {any} Trimmed string if input is a string, otherwise the original value
 * @example
 *   trimValue("  hello  ") // "hello"
 *   trimValue(123) // 123
 *   trimValue(null) // null
 */
export const trimValue = (value: any): any => {
  if (typeof value === 'string') {
    return value.trim()
  }
  return value
}

/**
 * Checks if the given value is "blank". Considers undefined, null, NaN, empty arrays, and whitespace-only strings as "blank".
 * Note that a space-only string (" ") is trimmed to length 0 and returns true, but false itself is not considered "blank".
 *
 * @param item - The value to check (accepts any type)
 * @returns {boolean} True if the value is "blank", false otherwise
 * @example
 *   isBlank(undefined) // true
 *   isBlank(null) // true
 *   isBlank(NaN) // true
 *   isBlank([]) // true
 *   isBlank("  ") // true
 *   isBlank("text") // false
 *   isBlank(false) // false
 */
export const isBlank = (item: any): boolean => {
  if (item === undefined || item === null || Number.isNaN(item)) {
    return true
  } else if (Array.isArray(item)) {
    return item.length === 0
  } else if (typeof item === 'string') {
    return item.trim().length === 0
  }
  return false
}

/**
 * Checks if the given value is zero. Null, undefined, and empty strings are not considered zero.
 * Handles string representations of numbers (e.g., "0") and returns false for invalid numbers or non-string/number types.
 *
 * @param value - The value to check (accepts any type)
 * @returns {boolean} True if the value is 0, false otherwise
 * @example
 *   isZeroValue(0) // true
 *   isZeroValue("0") // true
 *   isZeroValue("") // false
 *   isZeroValue(null) // false
 *   isZeroValue("abc") // false
 *   isZeroValue({}) // false
 */
export const isZeroValue = (value: any): boolean => {
  if (value === null || value === undefined) {
    return false
  }

  // Explicitly check for string or number types
  if (typeof value !== 'string' && typeof value !== 'number') {
    return false
  }

  const trimmedValue = trimValue(value)

  if (typeof trimmedValue === 'string' && trimmedValue.trim() === '') {
    return false
  }

  const numericValue = Number(trimmedValue)

  return !Number.isNaN(numericValue) && numericValue === 0
}

/**
 * Checks if the given value is empty or zero. Returns true if the value is null, undefined, an empty string, or 0.
 * Uses trimValue internally to trim the value before calling isZeroValue and isBlank.
 *
 * @param value - The value to check (number, string, or null)
 * @returns {boolean} True if the value is empty or 0, false otherwise
 * @example
 *   isEmptyOrZero(0) // true
 *   isEmptyOrZero("") // true
 *   isEmptyOrZero(null) // true
 *   isEmptyOrZero("  ") // true
 *   isEmptyOrZero("1") // false
 */
export const isEmptyOrZero = (value: NumStrNullType): boolean => {
  const trimmedValue = trimValue(value)
  return isZeroValue(trimmedValue) || isBlank(trimmedValue)
}

/**
 * Parses a string or number into a number, returning null for invalid or empty inputs.
 * Empty strings and null values are explicitly handled as null.
 *
 * @param value - The value to parse (string, number, or null)
 * @returns {number | null} Parsed number or null if parsing fails
 * @example
 *   parseNumberOrNull("123") // 123
 *   parseNumberOrNull("") // null
 *   parseNumberOrNull(null) // null
 *   parseNumberOrNull("abc") // null
 */
export const parseNumberOrNull = (
  value: string | number | null,
): number | null => {
  if (value === '' || value === null) {
    return null
  }

  const parsedValue = Number(value)

  return Number.isNaN(parsedValue) ? null : parsedValue
}

/**
 * Safely converts a string or number to a number, handling invalid inputs gracefully.
 * Returns null if the input is blank (null, undefined, empty array, whitespace-only string),
 * or if the string cannot be parsed to a valid number. Preserves existing numbers unchanged.
 *
 * @param item - The input value (number, string, or null) to convert
 * @returns {number | null} The converted number or null if conversion fails or input is blank
 * @example
 *   convertToNumberSafely("123") // 123
 *   convertToNumberSafely("  0  ") // 0
 *   convertToNumberSafely("") // null
 *   convertToNumberSafely(null) // null
 *   convertToNumberSafely("abc") // null
 *   convertToNumberSafely(42) // 42
 */
export const convertToNumberSafely = (item: NumStrNullType): number | null => {
  if (isBlank(item)) return null
  if (typeof item === 'string') {
    const trimmed = trimValue(item)
    const convertedNumber = Number(trimmed)
    return Number.isNaN(convertedNumber) ? null : convertedNumber
  }
  if (typeof item === 'number') return item
  return null
}

/**
 * Extracts the leading numeric value from a string, including optional negative sign and decimal.
 * Returns the parsed number from the start of the string, ignoring subsequent non-numeric characters.
 * Returns null if no valid leading number is found or if the input string does not start with a number.
 *
 * @param input - The input string that may contain a leading numeric value
 * @returns {number | null} The parsed leading number or null if no valid number is found at the start
 * @example
 *   extractLeadingNumber("123abc") // 123
 *   extractLeadingNumber("-12.5test") // -12.5
 *   extractLeadingNumber("abc123") // null
 *   extractLeadingNumber("  10.0  ") // 10
 *   extractLeadingNumber("") // null
 */
export const extractLeadingNumber = (input: string | null): number | null => {
  if (input == null) return null
  const regex = /^\s*-?\d+(\.\d+)?/
  const match = regex.exec(input)
  return match ? Number.parseFloat(match[0]) : null
}

/**
 * Formats a Date object into a string with the format "YYYY-MM-DD HH:MM:SS".
 * Returns null if the input date is null or invalid.
 *
 * @param date - The Date object to format, or null
 * @returns {string | null} Formatted date-time string (e.g., "2025-02-28 14:30:00") or null if input is invalid
 * @example
 *   formatDateTime(new Date(2025, 1, 28, 14, 30, 0)) // "2025-02-28 14:30:00"
 *   formatDateTime(null) // null
 */
export const formatDateTime = (date: Date | null): string | null => {
  if (!date) {
    return null
  }

  const year = new Intl.DateTimeFormat('en', { year: 'numeric' }).format(date)
  const month = new Intl.DateTimeFormat('en', { month: '2-digit' }).format(date)
  const day = new Intl.DateTimeFormat('en', { day: '2-digit' }).format(date)

  const hour = date.getHours().toString().padStart(2, '0')
  const minute = date.getMinutes().toString().padStart(2, '0')
  const second = date.getSeconds().toString().padStart(2, '0')

  return `${year}-${month}-${day} ${hour}:${minute}:${second}`
}

/**
 * Converts a Unix timestamp (in seconds) to a Date object.
 * Returns null if the conversion fails due to an invalid timestamp.
 *
 * @param timestamp - Unix timestamp in seconds (e.g., 1677655800)
 * @returns {Date | null} Converted Date object or null if conversion fails
 * @example
 *   formatUnixTimestampToDate(1677655800) // Date object for "2025-02-28 14:30:00 UTC"
 *   formatUnixTimestampToDate(NaN) // null
 */
export const formatUnixTimestampToDate = (timestamp: number): Date | null => {
  try {
    const date = new Date(timestamp * 1000) // Convert seconds to milliseconds
    return date
  } catch (error) {
    console.error('Failed to convert timestamp to date:', error)
    return null
  }
}

/**
 * Creates a delay for the specified number of milliseconds, returning a Promise that resolves afterward.
 *
 * @param ms - The delay duration in milliseconds
 * @returns {Promise<any>} A Promise that resolves after the delay
 * @example
 *   await delay(1000) // Waits 1 second, then resolves
 */
export const delay = (ms: number): Promise<any> => {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

/**
 * Increases the numeric value extracted from an input string by a specified step,
 * ensuring the result stays within the provided minimum and maximum limits.
 * Non-numeric characters are stripped, and invalid or null inputs default to the step value.
 * Throws an error if step is not positive.
 *
 * @param value - The input value as a string (e.g., "10.5") or null, potentially containing non-numeric characters
 * @param max - The maximum allowable value; if exceeded, returns this value
 * @param min - The minimum allowable value; if undercut, returns this value
 * @param step - The amount by which to increase the numeric value (must be positive)
 * @returns {number} The new value, constrained within the [min, max] range
 * @throws {Error} If step is not a positive number
 * @example
 *   increaseItemBySpinButton("10", 20, 0, 5) // 15
 *   increaseItemBySpinButton(null, 20, 0, 5) // 5
 *   increaseItemBySpinButton("10a", 20, 0, 5) // 15
 *   increaseItemBySpinButton("25", 20, 0, 5) // 20
 *   increaseItemBySpinButton("-5", 20, 0, 5) // 0
 */
export const increaseItemBySpinButton = (
  value: string | null,
  max: number,
  min: number,
  step: number,
): number => {
  if (step <= 0) {
    throw new Error('Step must be a positive number')
  }

  let newValue

  // If value is null, assign step value and format
  if (value) {
    // extract only numbers, commas, and minus signs
    const extractedValue = value.replaceAll(/[^\d.-]/g, '')

    const numericValue = Number.parseFloat(extractedValue)

    // Check if the extracted value is a valid number
    if (Number.isNaN(numericValue)) {
      newValue = step // Assign step value if invalid
    } else {
      newValue = numericValue + step
    }

    if (newValue < min) {
      newValue = min
    }
  } else {
    newValue = step
  }

  if (newValue > max) {
    newValue = max
  }

  return newValue
}

/**
 * Decreases the numeric value extracted from an input string by a specified step,
 * ensuring the result stays within the provided minimum and maximum limits.
 * Non-numeric characters are stripped, and invalid or null inputs default to the minimum value.
 * Throws an error if step is not positive.
 *
 * @param value - The input value as a string (e.g., "10.5") or null, potentially containing non-numeric characters
 * @param max - The maximum allowable value; if exceeded, returns this value
 * @param min - The minimum allowable value; if undercut, returns this value
 * @param step - The amount by which to decrease the numeric value (must be positive)
 * @returns {number} The new value, constrained within the [min, max] range
 * @throws {Error} If step is not a positive number
 * @example
 *   decrementItemBySpinButton("10", 20, 0, 5) // 5
 *   decrementItemBySpinButton(null, 20, 0, 5) // 0
 *   decrementItemBySpinButton("10a", 20, 0, 5) // 5
 *   decrementItemBySpinButton("2", 20, 0, 5) // 0
 *   decrementItemBySpinButton("25", 20, 0, 5) // 20
 */
export const decrementItemBySpinButton = (
  value: string | null,
  max: number,
  min: number,
  step: number,
): number => {
  if (step <= 0) {
    throw new Error('Step must be a positive number')
  }

  let newValue
  if (value) {
    // extract only numbers, commas, and minus signs
    const extractedValue = value.replaceAll(/[^\d.-]/g, '')

    const numericValue = Number.parseFloat(extractedValue)

    if (Number.isNaN(numericValue)) {
      newValue = min
    } else {
      newValue = numericValue - step
    }

    if (newValue < min) {
      newValue = min
    }

    if (newValue > max) {
      newValue = max
    }
  } else {
    newValue = min
  }

  return newValue
}

/**
 * Downloads a Blob as a file by triggering a browser download.
 * Creates a temporary URL and simulates a link click to download the file,
 * regardless of whether the file is a CSV, ZIP, or any other type.
 *
 * @param blob - The Blob object containing the file data.
 * @param fileName - The name of the file to save (e.g., "data.zip" or "data.csv").
 * @example
 *   const fileBlob = new Blob(["sample content"], { type: "application/octet-stream" });
 *   downloadFile(fileBlob, "example.zip"); // Downloads "example.zip"
 */
export const downloadFile = (blob: Blob, fileName: string) => {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = fileName
  document.body.appendChild(a)
  a.click()
  a.remove()
  URL.revokeObjectURL(url)
}

/**
 * Extracts the zip file name from the response headers.
 * It checks for a 'content-disposition' header and extracts the file name.
 * @param headers - The response headers object (either Axios or Fetch style)
 * @returns The extracted zip file name or a default name if not found.
 */
export const extractZipFileName = (headers: any): string | null => {
  let contentDisposition: string | undefined

  // Support for both Axios (plain object) and Fetch (Headers instance)
  if (typeof headers.get === 'function') {
    contentDisposition = headers.get('content-disposition')
  } else {
    contentDisposition = headers['content-disposition']
  }

  if (contentDisposition) {
    const regex = /filename="([^"]+)"/
    const fileNameMatch = regex.exec(contentDisposition)
    return fileNameMatch?.[1] ?? null
  }
  return null
}

/**
 * Checks if the ZIP file contains an Error.txt file with content.
 * @param zipFile - The ZIP file Blob to check.
 * @returns A boolean indicating whether the Error.txt file exists and has content.
 * @throws Error if the ZIP file cannot be processed.
 */
export const checkZipForErrors = async (zipFile: Blob): Promise<boolean> => {
  try {
    const zip = await JSZip.loadAsync(zipFile)

    const errorFile = zip.file(CONSTANTS.FILE_NAME.ERROR_TXT)
    if (!errorFile) {
      return false
    }

    const errorContent = await errorFile.async('string')
    const errorLines = errorContent.split(/\r?\n/).filter((line) => {
      const trimmedLine = line.trim()
      return trimmedLine !== '' && trimmedLine !== 'null'
    })
    return errorLines.length > 0
  } catch (error) {
    console.error('Error checking ZIP file for errors:', error)
    throw error
  }
}

/**
 * Sanitizes a file name by allowing only alphanumeric characters, dot, underscore, and hyphen.
 * Replaces disallowed characters with underscore, consolidates consecutive underscores, removes leading/trailing underscores,
 * and handles file extensions by preserving the last dot and ensuring no trailing underscore before it.
 *
 * @param name - The input string to sanitize as a file name
 * @returns {string} The sanitized file name
 * @example
 *   sanitizeFileName("test__file!.zip") // "test_file.zip"
 *   sanitizeFileName("file@name#$.zip") // "file_name.zip"
 *   sanitizeFileName("valid.file_name-123.zip") // "valid.file_name-123.zip"
 */
export const sanitizeFileName = (name: string) => {
  // Find the last occurrence of '.' to separate extension
  const lastDotIndex = name.lastIndexOf('.')
  let baseName = name
  let extension = ''

  if (lastDotIndex !== -1) {
    extension = name.substring(lastDotIndex) // e.g., '.zip'
    baseName = name.substring(0, lastDotIndex) // e.g., 'valid.file_name-123'
  }

  // Allow only alphanumeric characters, dot, underscore, and hyphen in base name
  let sanitized = baseName.replaceAll(/[^a-zA-Z0-9._-]/g, '_')
  // Replace consecutive underscores with a single underscore
  sanitized = sanitized.replaceAll(/_+/g, '_')
  // Remove leading and trailing underscores with explicit grouping
  sanitized = sanitized.replaceAll(/(^_+)|(_+$)/g, '')
  // Remove trailing underscore before extension if it exists
  if (extension && sanitized.endsWith('_')) {
    sanitized = sanitized.slice(0, -1)
  }

  // Combine sanitized base name with extension
  return sanitized + extension
}

/**
 * Adds execution options to selected or excluded arrays based on flag mappings.
 *
 * @param selected - Array for selected options.
 * @param excluded - Array for excluded options.
 * @param mappings - Array of { flag: boolean, option: ExecutionOptionsEnum } mappings.
 * @example
 * addExecutionOptionsFromMappings(selectedOptions, excludedOptions, [{ flag: true, option: ExecutionOptionsEnum.SomeOption }])
 */
export const addExecutionOptionsFromMappings = (
  selected: ExecutionOptionsEnum[],
  excluded: ExecutionOptionsEnum[],
  mappings: { flag: boolean; option: ExecutionOptionsEnum }[],
) => {
  mappings.forEach(({ flag, option }) => {
    if (flag) {
      selected.push(option)
    } else {
      excluded.push(option)
    }
  })
}
