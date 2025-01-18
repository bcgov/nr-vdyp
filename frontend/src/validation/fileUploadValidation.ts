import { FileUploadValidator } from './fileUploadValidator'

const fileUploadValidator = new FileUploadValidator()

export const validateComparison = (
  startingAge: number | null,
  finishingAge: number | null,
) => {
  if (!fileUploadValidator.validateAgeComparison(startingAge, finishingAge)) {
    return { isValid: false }
  }

  return { isValid: true }
}

export const validateRequiredFields = (
  startingAge: number | null,
  finishingAge: number | null,
  ageIncrement: number | null,
) => {
  if (
    !fileUploadValidator.validateRequiredFields(
      startingAge,
      finishingAge,
      ageIncrement,
    )
  ) {
    return { isValid: false }
  }
  return { isValid: true }
}

export const validateRange = (
  startingAge: number | null,
  finishingAge: number | null,
  ageIncrement: number | null,
) => {
  if (!fileUploadValidator.validateStartingAgeRange(startingAge)) {
    return {
      isValid: false,
      errorType: 'startingAge',
    }
  }

  if (!fileUploadValidator.validateFinishingAgeRange(finishingAge)) {
    return {
      isValid: false,
      errorType: 'finishingAge',
    }
  }

  if (!fileUploadValidator.validateAgeIncrementRange(ageIncrement)) {
    return {
      isValid: false,
      errorType: 'ageIncrement',
    }
  }

  return { isValid: true }
}

export const validateFiles = async (
  layerFile: File | null,
  polygonFile: File | null,
) => {
  if (!layerFile) {
    return {
      isValid: false,
      errorType: 'layerFileMissing',
    }
  }

  if (!polygonFile) {
    return {
      isValid: false,
      errorType: 'polygonFileMissing',
    }
  }

  if (!(await fileUploadValidator.isCSVFile(layerFile))) {
    return {
      isValid: false,
      errorType: 'layerFileNotCSVFormat',
    }
  }

  if (!(await fileUploadValidator.isCSVFile(polygonFile))) {
    return {
      isValid: false,
      errorType: 'polygonFileNotCSVFormat',
    }
  }

  return { isValid: true }
}
