import { ValidationBase } from './validationBase'
import { CONSTANTS, BIZCONSTANTS } from '@/constants'
import { convertToNumberSafely, extractLeadingNumber } from '@/utils/util'

export class StandInfoValidator extends ValidationBase {
  validatePercentStockableAreaRange(psa: number | null): boolean {
    if (!psa) return true

    return this.validateRange(
      psa,
      CONSTANTS.NUM_INPUT_LIMITS.PERCENT_STOCKABLE_AREA_MIN,
      CONSTANTS.NUM_INPUT_LIMITS.PERCENT_STOCKABLE_AREA_MAX,
    )
  }

  validateBasalAreaRange(basalArea: string | null): boolean {
    const numericBasalArea = convertToNumberSafely(basalArea)
    if (!numericBasalArea) return true

    return this.validateRange(
      numericBasalArea,
      CONSTANTS.NUM_INPUT_LIMITS.BASAL_AREA_MIN,
      CONSTANTS.NUM_INPUT_LIMITS.BASAL_AREA_MAX,
    )
  }

  validateTreesPerHectareRange(tph: string | null): boolean {
    const numericTph = convertToNumberSafely(tph)
    if (!numericTph) return true

    return this.validateRange(
      numericTph,
      CONSTANTS.NUM_INPUT_LIMITS.TPH_MIN,
      CONSTANTS.NUM_INPUT_LIMITS.TPH_MAX,
    )
  }

  validatePercentCrownClosureRange(pcc: number | null): boolean {
    if (!pcc) return true

    return this.validateRange(
      pcc,
      CONSTANTS.NUM_INPUT_LIMITS.CROWN_CLOSURE_MIN,
      CONSTANTS.NUM_INPUT_LIMITS.CROWN_CLOSURE_MAX,
    )
  }

  /**
   * Function that validates whether the given basal area is within acceptable limits for the species.
   * It calculates the maximum allowable basal area using species-specific coefficients and height.
   * @param species - The species code to look up the basal area coefficients.
   * @param isCoastal - Boolean indicating whether the region is coastal (true) or interior (false).
   * @param basalArea - The basal area to be validated.
   * @param height - The height of the stand used in the calculation.
   * @returns true if the basal area is within the valid limit, false otherwise.
   *          Returns true on errors for input arguments that cannot be validated.
   * @example speceis:'H', coastal, ba: 50, height: 8
   */
  validateBasalAreaLimits(
    species: string,
    isCoastal: boolean,
    basalArea: string,
    height: string,
  ): boolean {
    if (!(species in BIZCONSTANTS.BA_LIMIT_COEFFICIENTS)) {
      console.warn(`Species ${species} not found in BA_LIMIT_COEFFICIENTS.`)
      return true
    }

    const speciesData =
      BIZCONSTANTS.BA_LIMIT_COEFFICIENTS[
        species as keyof typeof BIZCONSTANTS.BA_LIMIT_COEFFICIENTS
      ]

    const region = isCoastal ? 'coastal' : 'interior'
    const coeffs = speciesData[region]

    // -999 indicates unavailable coefficient
    if (coeffs.coeff1 === -999 || coeffs.coeff2 === -999) {
      return true
    }

    // Ensure these arguments are valid, handle invalid input
    const parsedHeight = Number.parseFloat(height)
    const parsedBasalArea = Number.parseFloat(basalArea)

    if (Number.isNaN(parsedHeight) || parsedHeight <= 0) {
      console.warn(
        `Invalid height value: ${height}. Unable to perform calculation.`,
      )
      return true
    }

    if (Number.isNaN(parsedBasalArea) || parsedBasalArea <= 0) {
      console.warn(
        `Invalid basal area value: ${basalArea}. Unable to perform calculation.`,
      )
      return true
    }

    // Equation constants
    const { const1, const2 } = BIZCONSTANTS.BA_EQUATION_CONSTANTS

    // Validate the basal area against the calculated limit
    const fBALimit =
      Math.exp(coeffs.coeff2 / (Number.parseFloat(height) - const2)) * coeffs.coeff1 +
      const1

    return Number.parseFloat(basalArea) <= fBALimit
  }

  /**
   * Function that takes in a BEC Zone and returns whether the Zone is coastal or not
   * @param becZone BEC Zone code
   * @returns true if the BEC Zone is coastal, false if interior
   */
  isCoastalZone(becZone: string): boolean {
    const normalizedBecZone = becZone.trim().toUpperCase()

    if (normalizedBecZone in BIZCONSTANTS.BEC_ZONE_COASTAL_MAP) {
      return BIZCONSTANTS.BEC_ZONE_COASTAL_MAP[normalizedBecZone]
    } else {
      console.warn(`BEC Zone ${becZone} is not recognized.`)
      return false
    }
  }

  /**
   * Validates the Trees per Hectare (TPH) limits based on the basal area, TPH, spzHeight, species, and region.
   * This function calculates the likely minimum and maximum TPH values using species-specific coefficients
   * for coastal or interior regions and compares the input TPH with those limits.
   *
   * @param basalArea - The basal area in square meters per hectare.
   * @param tph - Trees per hectare.
   * @param spzHeight - The height of the trees in meters.
   * @param species - The species code to look up TPH limit coefficients.
   * @param coastal - Boolean indicating whether the region is coastal (true) or interior (false).
   * @returns A string with an error message if TPH is outside the calculated limits, or null if valid.
   *
   * @example
   * tph < tphMin: species = AC, coastal, spzHeight = 10.0, ba = 25.0, TPH = 50
   * tph > tphMax: species = AC, coastal, spzHeight = 10.0, ba = 5.0, TPH = 4000
   */
  validateTreePerHectareLimits(
    basalArea: string,
    tph: string,
    spzHeight: string,
    species: string,
    coastal: boolean,
  ): string | null {
    // Handle unknown species
    if (!(species in BIZCONSTANTS.TPH_LIMIT_COEFFICIENTS)) {
      console.warn('Unknown species')
      return null
    }

    const speciesCoefficients = BIZCONSTANTS.TPH_LIMIT_COEFFICIENTS[
      species as keyof typeof BIZCONSTANTS.TPH_LIMIT_COEFFICIENTS
    ] as { coastal?: any; interior?: any }

    // Set region based on whether it's Coastal or not
    const region = coastal ? 'coastal' : 'interior'

    // Check if the region data exists
    if (!speciesCoefficients[region]) {
      console.warn('No data for region')
      return null
    }

    const regionCoefficients = speciesCoefficients[region]
    const { P10, P90 } = regionCoefficients || {}

    // Verify that P10 and P90 data are present
    if (!P10 || !P90) {
      console.warn('No data for region')
      return null
    }

    // Ensure these input arguments are valid, handle invalid input
    const parsedHeight = Number.parseFloat(spzHeight)
    const parsedBasalArea = Number.parseFloat(basalArea)
    const parsedTph = Number.parseFloat(tph)

    if (Number.isNaN(parsedHeight) || parsedHeight <= 0) {
      console.warn(
        `Invalid height value: ${spzHeight}. Unable to perform calculation.`,
      )
      return null
    }

    if (Number.isNaN(parsedBasalArea) || parsedBasalArea <= 0) {
      console.warn(
        `Invalid basal area value: ${basalArea}. Unable to perform calculation.`,
      )
      return null
    }

    if (Number.isNaN(parsedTph) || parsedTph <= 0) {
      console.warn(
        `Invalid trees per hectare value: ${tph}. Unable to perform calculation.`,
      )
      return null
    }

    // Defining constants
    const const1 = BIZCONSTANTS.TPH_EQUATION_CONSTANTS.const1
    const const2 = BIZCONSTANTS.TPH_EQUATION_CONSTANTS.const2
    const const3 = BIZCONSTANTS.TPH_EQUATION_CONSTANTS.const3
    const unavailable = -999 // Handle when data is unavailable

    let tphMin: number | null = null
    let tphMax: number | null = null

    // Return null if P10, P90 values are invalid
    if (P10.a0 === unavailable || P90.a0 === unavailable) {
      console.warn('Unavailable TPH coefficients for this region')
      return null
    }

    // Minimum TPH calculation
    const dqMax =
      const1 +
      P90.a0 +
      P90.b0 * (Number.parseFloat(spzHeight) - const2) +
      P90.b1 * (Number.parseFloat(spzHeight) - const2) ** 2

    if (dqMax > 0) {
      tphMin = Number.parseFloat(basalArea) / (const3 * dqMax ** 2)
    }

    // Maximum TPH calculation
    const dqMin =
      -const1 +
      P10.a0 +
      P10.b0 * (Number.parseFloat(spzHeight) - const2) +
      P10.b1 * (Number.parseFloat(spzHeight) - const2) ** 2

    if (dqMin > 0) {
      tphMax = Number.parseFloat(basalArea) / (const3 * dqMin ** 2)
    }

    // Minimum and maximum TPH values not calculated correctly
    if (tphMin === null || tphMax === null) {
      console.warn('TPH calculation failed')
      return null
    }

    if (Number.parseFloat(tph) < tphMin) {
      return 'Trees/ha is less than a likely minimum for entered height. Do you wish to proceed?'
    }

    if (Number.parseFloat(tph) > tphMax) {
      return 'Trees/ha is above a likely maximum for entered height. Do you wish to proceed?'
    }

    // Return null if in range (valid value)
    return null
  }

  /**
   * Validates the quadratic mean diameter based on the basal area, trees per hectare (TPH), and minimum DBH limit.
   * If the calculated quadratic diameter is less than the required minimum DBH limit, an error message is returned.
   *
   * @param basalArea - The basal area in m²/ha.
   * @param tph - Trees per hectare.
   * @param minDBHLimit - The minimum DBH limit in cm, provided as a string.
   * @returns - Returns an error message if the quadratic diameter is less than the minimum limit, otherwise null.
   *
   * @example
   * basalArea: 4, tph: 1000, minDBHLimit: 7.5 cm+
   */
  validateQuadraticDiameter(
    basalArea: string,
    tph: string,
    minDBHLimit: string | null,
  ): string | null {
    if (!minDBHLimit) {
      console.warn('Unknown minDBHLimit')
      return null
    }

    const parsedBasalArea = Number.parseFloat(basalArea)
    const parsedTph = Number.parseFloat(tph)

    if (Number.isNaN(parsedBasalArea) || parsedBasalArea <= 0) {
      console.warn(
        `Invalid basal area value: ${basalArea}. Unable to perform calculation.`,
      )
      return null
    }

    if (Number.isNaN(parsedTph) || parsedTph <= 0) {
      console.warn(
        `Invalid trees per hectare value: ${tph}. Unable to perform calculation.`,
      )
      return null
    }

    let diam = 0
    if (Number.parseFloat(tph) > 0) {
      diam = Math.sqrt(Number.parseFloat(basalArea) / Number.parseFloat(tph) / 0.00007854)
    }

    const minDBH = extractLeadingNumber(minDBHLimit)
    if (minDBH === null) {
      console.warn(
        `Invalid minDBHLimit value: ${minDBHLimit}. Skipping comparison.`,
      )
      return null
    }

    if (diam < minDBH) {
      return `Quadratic Mean Diameter of ${diam.toFixed(1)} cm is less than the required diameter of: ${minDBHLimit}\nModify one or both of Basal Area or Trees per Hectare.`
    }

    // Return null if in range (valid value)
    return null
  }

  validateBALimits(
    selectedSiteSpecies: string | null,
    becZone: string | null,
    basalArea: string | null,
    height: string | null,
  ): boolean {
    if (selectedSiteSpecies && becZone && basalArea && height) {
      const isValid = this.validateBasalAreaLimits(
        selectedSiteSpecies,
        this.isCoastalZone(becZone),
        basalArea,
        height,
      )

      return isValid
    }
    return true
  }

  validateTPHLimits(
    basalArea: string | null,
    treesPerHectare: string | null,
    spzHeight: string | null,
    selectedSiteSpecies: string | null,
    becZone: string | null,
  ): string | null {
    if (
      basalArea &&
      treesPerHectare &&
      spzHeight &&
      selectedSiteSpecies &&
      becZone
    ) {
      return this.validateTreePerHectareLimits(
        basalArea,
        treesPerHectare,
        spzHeight,
        selectedSiteSpecies,
        this.isCoastalZone(becZone),
      )
    }

    return null
  }

  validateQuadDiameter(
    basalArea: string | null,
    treesPerHectare: string | null,
    minimumDBHLimit: string | null,
  ): string | null {
    if (basalArea && treesPerHectare && minimumDBHLimit) {
      return this.validateQuadraticDiameter(
        basalArea,
        treesPerHectare,
        minimumDBHLimit,
      )
    }

    return null
  }
}
