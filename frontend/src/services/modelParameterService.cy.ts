/// <reference types="cypress" />

import {
  createCSVFiles,
  runModel,
  generateFeatureId,
  generatePolygonNumber,
  generateTreeCoverLayerEstimatedId,
  computeBclcsLevel1,
  computeBclcsLevel2,
  computeBclcsLevel3,
  determineBclcsLevel4,
  determineBclcsLevel5,
  getSpeciesData,
  flattenSpeciesData,
} from '@/services/modelParameterService'
import {
  ExecutionOptionsEnum,
  ParameterNamesEnum,
  MetadataToOutputEnum,
  OutputFormatEnum,
} from '@/services/vdyp-api'
import { BIZCONSTANTS, CONSTANTS, DEFAULTS, OPTIONS } from '@/constants'
import { UtilizationClassSetEnum } from '@/services/vdyp-api/models/utilization-class-set-enum'

describe('Model Parameter Service Unit Tests', () => {
  const mockModelParameterStore = {
    derivedBy: DEFAULTS.DEFAULT_VALUES.DERIVED_BY,
    speciesList: [
      { species: 'PL', percent: '30.0' },
      { species: 'AC', percent: '30.0' },
      { species: 'H', percent: '30.0' },
      { species: 'S', percent: '10.0' },
      { species: null, percent: '0.0' },
      { species: null, percent: '0.0' },
    ],
    speciesGroups: [],
    becZone: DEFAULTS.DEFAULT_VALUES.BEC_ZONE,
    ecoZone: OPTIONS.ecoZoneOptions[0].value,
    incSecondaryHeight: false,
    highestPercentSpecies: 'PL',
    percentStockableArea: DEFAULTS.DEFAULT_VALUES.PERCENT_STOCKABLE_AREA,
    selectedAgeYearRange: CONSTANTS.AGE_YEAR_RANGE.AGE,
    startingAge: DEFAULTS.DEFAULT_VALUES.STARTING_AGE,
    finishingAge: DEFAULTS.DEFAULT_VALUES.FINISHING_AGE,
    ageIncrement: DEFAULTS.DEFAULT_VALUES.AGE_INCREMENT,
    startYear: DEFAULTS.DEFAULT_VALUES.START_YEAR,
    endYear: DEFAULTS.DEFAULT_VALUES.END_YEAR,
    yearIncrement: DEFAULTS.DEFAULT_VALUES.YEAR_INCREMENT,
    isForwardGrowEnabled: DEFAULTS.DEFAULT_VALUES.IS_FORWARD_GROW_ENABLED,
    isBackwardGrowEnabled: DEFAULTS.DEFAULT_VALUES.IS_BACKWARD_GROW_ENABLED,
    includeInReport: [],
    projectionType: CONSTANTS.PROJECTION_TYPE.VOLUME,
    bha50SiteIndex: null,
    referenceYear: new Date().getFullYear(),
  }

  it('should generate a valid feature ID', () => {
    const featureId = generateFeatureId()
    expect(String(featureId).length).to.be.within(9, 10)
    expect(Number.isInteger(featureId)).to.be.true
  })

  it('should generate an 8-digit polygon number', () => {
    const polygonNumber = generatePolygonNumber()
    expect(polygonNumber.length).to.equal(8)
    expect(Number.isInteger(Number(polygonNumber))).to.be.true
  })

  it('should generate a tree cover layer estimated ID with 4 to 10 digits', () => {
    const treeCoverLayerEstimatedId = generateTreeCoverLayerEstimatedId()
    expect(treeCoverLayerEstimatedId.length).to.be.within(4, 10)
    expect(Number.isInteger(Number(treeCoverLayerEstimatedId))).to.be.true
  })

  it('should compute BCLCS Level 1 correctly', () => {
    expect(computeBclcsLevel1(50)).to.equal(BIZCONSTANTS.BCLCS_LEVEL1_VEG)
    expect(computeBclcsLevel1(4)).to.equal(BIZCONSTANTS.BCLCS_LEVEL1_NON_VEG)
  })

  it('should compute BCLCS Level 2 correctly', () => {
    expect(computeBclcsLevel2(50)).to.equal(BIZCONSTANTS.BCLCS_LEVEL2_TREED)
    expect(computeBclcsLevel2(5)).to.equal(BIZCONSTANTS.BCLCS_LEVEL2_NON_TREED)
  })

  it('should compute BCLCS Level 3 correctly', () => {
    expect(computeBclcsLevel3(BIZCONSTANTS.BCLCS_LEVEL3_BECZONE_AT)).to.equal(
      BIZCONSTANTS.BCLCS_LEVEL3_ALPINE,
    )
    expect(computeBclcsLevel3('Other')).to.equal(
      BIZCONSTANTS.BCLCS_LEVEL3_DEFAULT,
    )
    expect(computeBclcsLevel3(undefined)).to.equal(
      BIZCONSTANTS.BCLCS_LEVEL3_DEFAULT,
    )
  })

  it('should determine BCLCS Level 4 correctly', () => {
    const coniferousSpeciesGroups = [
      { group: 'PL', siteSpecies: 'PL', percent: '80.0' },
      { group: 'AC', siteSpecies: 'AC', percent: '20.0' },
    ]
    const broadleafSpeciesGroups = [
      { group: 'AC', siteSpecies: 'AC', percent: '80.0' },
      { group: 'PL', siteSpecies: 'PL', percent: '20.0' },
    ]
    const mixedSpeciesGroups = [
      { group: 'PL', siteSpecies: 'PL', percent: '50.0' },
      { group: 'AC', siteSpecies: 'AC', percent: '50.0' },
    ]

    expect(determineBclcsLevel4(coniferousSpeciesGroups)).to.equal(
      BIZCONSTANTS.BCLCS_LEVEL4_TC,
    )
    expect(determineBclcsLevel4(broadleafSpeciesGroups)).to.equal(
      BIZCONSTANTS.BCLCS_LEVEL4_TB,
    )
    expect(determineBclcsLevel4(mixedSpeciesGroups)).to.equal(
      BIZCONSTANTS.BCLCS_LEVEL4_TM,
    )
  })

  it('should determine BCLCS Level 5 correctly', () => {
    expect(determineBclcsLevel5(70)).to.equal(BIZCONSTANTS.BCLCS_LEVEL5_DE)
    expect(determineBclcsLevel5(40)).to.equal(BIZCONSTANTS.BCLCS_LEVEL5_OP)
    expect(determineBclcsLevel5(10)).to.equal(BIZCONSTANTS.BCLCS_LEVEL5_SP)
  })

  it('should get species data correctly', () => {
    const speciesList = [
      { species: 'PL', percent: '30.0' },
      { species: 'AC', percent: '0' },
      { species: null, percent: '10.0' },
    ]
    const result = getSpeciesData(speciesList)

    expect(result).to.have.length(3)
    expect(result[0].species).to.equal('PL')
    expect(result[0].percent).to.equal('30.0')
    expect(result[1].species).to.equal('AC')
    expect(result[1].percent).to.equal('0')
    expect(result[2].species).to.be.null
    expect(result[2].percent).to.equal('')
  })

  it('should flatten species data correctly', () => {
    const speciesData = [
      { species: 'PL', percent: '30.0' },
      { species: 'AC', percent: '20.0' },
      { species: 'H', percent: '10.0' },
    ]
    const result = flattenSpeciesData(speciesData, 2)
    expect(result).to.deep.equal(['PL', '30.0', 'AC', '20.0'])
  })

  it('should call projectionHcsvPost once', () => {
    const projectionStub = cy
      .stub()
      .resolves(new Blob(['mock response'], { type: 'application/zip' }))

    cy.wrap(runModel(mockModelParameterStore, projectionStub)).then(() => {
      expect(projectionStub).to.be.calledOnce
    })
  })

  it('should create CSV files correctly', () => {
    const { blobPolygon, blobLayer } = createCSVFiles(mockModelParameterStore)

    cy.wrap(blobPolygon).should('be.instanceOf', Blob)
    cy.wrap(blobLayer).should('be.instanceOf', Blob)

    const derivedByCode =
      mockModelParameterStore.derivedBy === CONSTANTS.DERIVED_BY.VOLUME
        ? BIZCONSTANTS.INVENTORY_CODES.FIP
        : mockModelParameterStore.derivedBy === CONSTANTS.DERIVED_BY.BASAL_AREA
          ? BIZCONSTANTS.INVENTORY_CODES.VRI
          : ''

    cy.wrap(blobPolygon)
      .then((blob) => blob.text())
      .then((text) => {
        expect(text).to.include(derivedByCode)
        expect(text).to.include(mockModelParameterStore.becZone)
        expect(text).to.include(mockModelParameterStore.ecoZone)
        expect(text).to.include(mockModelParameterStore.percentStockableArea)
        expect(text).to.include(
          computeBclcsLevel1(mockModelParameterStore.percentStockableArea),
        )
        expect(text).to.include(
          computeBclcsLevel2(mockModelParameterStore.percentStockableArea),
        )
        expect(text).to.include(
          computeBclcsLevel3(mockModelParameterStore.becZone),
        )
        expect(text).to.include(
          determineBclcsLevel4(mockModelParameterStore.speciesGroups),
        )
        expect(text).to.include(
          determineBclcsLevel5(mockModelParameterStore.percentStockableArea),
        )
        expect(text).to.include(mockModelParameterStore.referenceYear)
      })

    cy.wrap(blobLayer)
      .then((blob) => blob.text())
      .then((text) => {
        expect(text).to.include(mockModelParameterStore.highestPercentSpecies)
        expect(text).to.include(mockModelParameterStore.bha50SiteIndex ?? '')
        expect(text).to.include('PL,30.0')
        expect(text).to.include('AC,30.0')
        expect(text).to.include('H,30.0')
        expect(text).to.include('S,10.0')
      })
  })

  it('should call projectionHcsvPost with correct form data', () => {
    const projectionStub = cy
      .stub()
      .resolves(new Blob(['mock response'], { type: 'application/zip' }))

    cy.wrap(runModel(mockModelParameterStore, projectionStub)).then(() => {
      expect(projectionStub).to.be.calledOnce
      const formDataArg = projectionStub.getCall(0).args[0] as FormData

      expect(formDataArg.has(ParameterNamesEnum.HCSV_POLYGON_INPUT_DATA)).to.be
        .true
      expect(formDataArg.has(ParameterNamesEnum.HCSV_LAYERS_INPUT_DATA)).to.be
        .true
      expect(formDataArg.has(ParameterNamesEnum.PROJECTION_PARAMETERS)).to.be
        .true

      const projectionParamsBlob = formDataArg.get(
        ParameterNamesEnum.PROJECTION_PARAMETERS,
      ) as Blob

      cy.wrap(projectionParamsBlob)
        .then((blob) => blob.text())
        .then((text) => {
          const projectionParams = JSON.parse(text)
          expect(projectionParams.ageStart).to.equal(
            mockModelParameterStore.startingAge,
          )
          expect(projectionParams.ageEnd).to.equal(
            mockModelParameterStore.finishingAge,
          )
          expect(projectionParams.yearStart).to.be.null
          expect(projectionParams.yearEnd).to.be.null
          expect(projectionParams.ageIncrement).to.equal(
            mockModelParameterStore.ageIncrement,
          )
          expect(projectionParams.outputFormat).to.equal(
            OutputFormatEnum.CSVYieldTable,
          )
          expect(projectionParams.metadataToOutput).to.equal(
            MetadataToOutputEnum.NONE,
          )
          expect(projectionParams.selectedExecutionOptions).to.include(
            ExecutionOptionsEnum.DoIncludeProjectedMOFVolumes,
          )
          expect(projectionParams.selectedExecutionOptions).not.to.include(
            ExecutionOptionsEnum.DoIncludeProjectedCFSBiomass,
          )
          expect(projectionParams.selectedExecutionOptions).to.include(
            ExecutionOptionsEnum.ForwardGrowEnabled,
          )
          expect(projectionParams.excludedExecutionOptions).not.to.include(
            ExecutionOptionsEnum.ForwardGrowEnabled,
          )
          expect(projectionParams.selectedExecutionOptions).to.include(
            ExecutionOptionsEnum.BackGrowEnabled,
          )
          expect(projectionParams.excludedExecutionOptions).not.to.include(
            ExecutionOptionsEnum.BackGrowEnabled,
          )
        })
    })
  })

  it('should include ForwardGrowEnabled when forwardBackwardGrow includes FORWARD', () => {
    const updatedStore = {
      ...mockModelParameterStore,
      isForwardGrowEnabled: true,
      isBackwardGrowEnabled: false,
    }
    const projectionStub = cy
      .stub()
      .resolves(new Blob(['mock response'], { type: 'application/zip' }))

    cy.wrap(runModel(updatedStore, projectionStub)).then(() => {
      const formDataArg = projectionStub.getCall(0).args[0] as FormData
      const projectionParamsBlob = formDataArg.get(
        ParameterNamesEnum.PROJECTION_PARAMETERS,
      ) as Blob

      cy.wrap(projectionParamsBlob)
        .then((blob) => blob.text())
        .then((text) => {
          const projectionParams = JSON.parse(text)
          expect(projectionParams.selectedExecutionOptions).to.include(
            ExecutionOptionsEnum.ForwardGrowEnabled,
          )
          expect(projectionParams.selectedExecutionOptions).not.to.include(
            ExecutionOptionsEnum.BackGrowEnabled,
          )
        })
    })
  })

  it('should include BackGrowEnabled when forwardBackwardGrow includes BACKWARD', () => {
    const updatedStore = {
      ...mockModelParameterStore,
      isForwardGrowEnabled: false,
      isBackwardGrowEnabled: true,
    }
    const projectionStub = cy
      .stub()
      .resolves(new Blob(['mock response'], { type: 'application/zip' }))

    cy.wrap(runModel(updatedStore, projectionStub)).then(() => {
      const formDataArg = projectionStub.getCall(0).args[0] as FormData
      const projectionParamsBlob = formDataArg.get(
        ParameterNamesEnum.PROJECTION_PARAMETERS,
      ) as Blob

      cy.wrap(projectionParamsBlob)
        .then((blob) => blob.text())
        .then((text) => {
          const projectionParams = JSON.parse(text)
          expect(projectionParams.selectedExecutionOptions).not.to.include(
            ExecutionOptionsEnum.ForwardGrowEnabled,
          )
          expect(projectionParams.selectedExecutionOptions).to.include(
            ExecutionOptionsEnum.BackGrowEnabled,
          )
        })
    })
  })

  it('should include both ForwardGrowEnabled and BackGrowEnabled when forwardBackwardGrow includes both', () => {
    const updatedStore = {
      ...mockModelParameterStore,
      isForwardGrowEnabled: true,
      isBackwardGrowEnabled: true,
    }
    const projectionStub = cy
      .stub()
      .resolves(new Blob(['mock response'], { type: 'application/zip' }))

    cy.wrap(runModel(updatedStore, projectionStub)).then(() => {
      const formDataArg = projectionStub.getCall(0).args[0] as FormData
      const projectionParamsBlob = formDataArg.get(
        ParameterNamesEnum.PROJECTION_PARAMETERS,
      ) as Blob

      cy.wrap(projectionParamsBlob)
        .then((blob) => blob.text())
        .then((text) => {
          const projectionParams = JSON.parse(text)
          expect(projectionParams.selectedExecutionOptions).to.include(
            ExecutionOptionsEnum.ForwardGrowEnabled,
          )
          expect(projectionParams.selectedExecutionOptions).to.include(
            ExecutionOptionsEnum.BackGrowEnabled,
          )
        })
    })
  })

  it('should include additional options when secondary height is enabled', () => {
    const updatedModelParameterStore = {
      ...mockModelParameterStore,
      incSecondaryHeight: true,
    }

    const projectionStub = cy
      .stub()
      .resolves(new Blob(['mock response'], { type: 'application/zip' }))

    cy.wrap(runModel(updatedModelParameterStore, projectionStub)).then(() => {
      const formDataArg = projectionStub.getCall(0).args[0] as FormData
      const projectionParamsBlob = formDataArg.get(
        ParameterNamesEnum.PROJECTION_PARAMETERS,
      ) as Blob

      cy.wrap(projectionParamsBlob)
        .then((blob) => blob.text())
        .then((text) => {
          const projectionParams = JSON.parse(text)
          expect(projectionParams.selectedExecutionOptions).to.include(
            ExecutionOptionsEnum.DoIncludeSecondarySpeciesDominantHeightInYieldTable,
          )
        })
    })
  })

  it('should contain expected execution options', () => {
    const projectionStub = cy
      .stub()
      .resolves(new Blob(['mock response'], { type: 'application/zip' }))

    cy.wrap(runModel(mockModelParameterStore, projectionStub)).then(() => {
      const formDataArg = projectionStub.getCall(0).args[0] as FormData
      const projectionParamsBlob = formDataArg.get(
        ParameterNamesEnum.PROJECTION_PARAMETERS,
      ) as Blob

      cy.wrap(projectionParamsBlob)
        .then((blob) => blob.text())
        .then((text) => {
          const projectionParams = JSON.parse(text)
          expect(projectionParams.selectedExecutionOptions).to.include(
            ExecutionOptionsEnum.DoEnableProgressLogging,
            ExecutionOptionsEnum.DoEnableErrorLogging,
          )
        })
    })
  })

  it('should handle empty species list without errors', () => {
    const emptySpeciesStore = {
      ...mockModelParameterStore,
      speciesList: new Array(6).fill({ species: null, percent: '0.0' }),
    }

    const projectionStub = cy
      .stub()
      .resolves(new Blob(['mock response'], { type: 'application/zip' }))

    cy.wrap(runModel(emptySpeciesStore, projectionStub)).then(() => {
      expect(projectionStub).to.be.calledOnce
    })
  })

  it('should handle missing fields in model parameter store', () => {
    const invalidStore = {
      ...mockModelParameterStore,
      becZone: undefined,
    }

    const { blobPolygon } = createCSVFiles(invalidStore)
    cy.wrap(blobPolygon)
      .then((blob) => blob.text())
      .then((text) => {
        expect(text).not.to.include(DEFAULTS.DEFAULT_VALUES.BEC_ZONE)
      })
  })

  it('should use year range parameters when selectedAgeYearRange is YEAR', () => {
    const yearRangeStore = {
      ...mockModelParameterStore,
      selectedAgeYearRange: CONSTANTS.AGE_YEAR_RANGE.YEAR,
    }

    const projectionStub = cy
      .stub()
      .resolves(new Blob(['mock response'], { type: 'application/zip' }))

    cy.wrap(runModel(yearRangeStore, projectionStub)).then(() => {
      const formDataArg = projectionStub.getCall(0).args[0] as FormData
      const projectionParamsBlob = formDataArg.get(
        ParameterNamesEnum.PROJECTION_PARAMETERS,
      ) as Blob

      cy.wrap(projectionParamsBlob)
        .then((blob) => blob.text())
        .then((text) => {
          const projectionParams = JSON.parse(text)
          expect(projectionParams.ageStart).to.be.null
          expect(projectionParams.ageEnd).to.be.null
          expect(projectionParams.yearStart).to.equal(yearRangeStore.startYear)
          expect(projectionParams.yearEnd).to.equal(yearRangeStore.endYear)
          expect(projectionParams.ageIncrement).to.equal(
            yearRangeStore.yearIncrement,
          )
        })
    })
  })

  it('should generate utils array correctly from speciesGroups', () => {
    const mockModelParameterStoreWithSpecies = {
      ...mockModelParameterStore,
      speciesGroups: [
        { group: 'PL', minimumDBHLimit: UtilizationClassSetEnum._225 },
        { group: 'AC', minimumDBHLimit: UtilizationClassSetEnum._125 },
      ],
    }

    const projectionStub = cy
      .stub()
      .resolves(new Blob(['mock response'], { type: 'application/zip' }))

    cy.wrap(runModel(mockModelParameterStoreWithSpecies, projectionStub)).then(
      () => {
        const formDataArg = projectionStub.getCall(0).args[0] as FormData
        const projectionParamsBlob = formDataArg.get(
          ParameterNamesEnum.PROJECTION_PARAMETERS,
        ) as Blob

        cy.wrap(projectionParamsBlob)
          .then((blob) => blob.text())
          .then((text) => {
            const projectionParams = JSON.parse(text)
            expect(projectionParams.utils).to.be.an('array').with.lengthOf(2)
            expect(projectionParams.utils[0]).to.deep.equal({
              speciesName: 'PL',
              utilizationClass: UtilizationClassSetEnum._225,
            })
            expect(projectionParams.utils[1]).to.deep.equal({
              speciesName: 'AC',
              utilizationClass: UtilizationClassSetEnum._125,
            })
          })
      },
    )
  })

  it('should handle empty speciesGroups with empty utils array', () => {
    const mockModelParameterStoreWithEmptySpecies = {
      ...mockModelParameterStore,
      speciesGroups: [],
    }

    const projectionStub = cy
      .stub()
      .resolves(new Blob(['mock response'], { type: 'application/zip' }))

    cy.wrap(
      runModel(mockModelParameterStoreWithEmptySpecies, projectionStub),
    ).then(() => {
      const formDataArg = projectionStub.getCall(0).args[0] as FormData
      const projectionParamsBlob = formDataArg.get(
        ParameterNamesEnum.PROJECTION_PARAMETERS,
      ) as Blob

      cy.wrap(projectionParamsBlob)
        .then((blob) => blob.text())
        .then((text) => {
          const projectionParams = JSON.parse(text)
          expect(projectionParams.utils).to.be.an('array').with.lengthOf(0)
        })
    })
  })
})
