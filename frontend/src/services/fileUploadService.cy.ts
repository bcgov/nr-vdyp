/// <reference types="cypress" />

import {
  buildExecutionOptions,
  buildDebugOptions,
  getFormData,
  runModelFileUpload,
} from '@/services/fileUploadService'
import { useFileUploadStore } from '@/stores/fileUploadStore'
import {
  ExecutionOptionsEnum,
  DebugOptionsEnum,
  ParameterNamesEnum,
  OutputFormatEnum,
  CombineAgeYearRangeEnum,
  MetadataToOutputEnum,
} from '@/services/vdyp-api'
import { CONSTANTS, DEFAULTS } from '@/constants'
import { createPinia, setActivePinia } from 'pinia'

describe('File Upload Service Unit Tests', () => {
  let fileUploadStore: ReturnType<typeof useFileUploadStore>

  beforeEach(() => {
    setActivePinia(createPinia())
    fileUploadStore = useFileUploadStore()

    fileUploadStore.projectionType = CONSTANTS.PROJECTION_TYPE.VOLUME
    fileUploadStore.includeInReport = [CONSTANTS.INCLUDE_IN_REPORT.BY_SPECIES]
    fileUploadStore.selectedAgeYearRange = CONSTANTS.AGE_YEAR_RANGE.AGE
    fileUploadStore.startingAge = 10
    fileUploadStore.finishingAge = 100
    fileUploadStore.ageIncrement = 10
    fileUploadStore.startYear = 2020
    fileUploadStore.endYear = 2030
    fileUploadStore.yearIncrement = 2
    fileUploadStore.isForwardGrowEnabled =
      DEFAULTS.DEFAULT_VALUES.IS_FORWARD_GROW_ENABLED
    fileUploadStore.isBackwardGrowEnabled =
      DEFAULTS.DEFAULT_VALUES.IS_BACKWARD_GROW_ENABLED
    fileUploadStore.polygonFile = new File(['polygon content'], 'polygon.csv', {
      type: 'text/csv',
    })
    fileUploadStore.layerFile = new File(['layer content'], 'layer.csv', {
      type: 'text/csv',
    })
  })

  it('should return correct selected execution options for volume projection', () => {
    const { selectedExecutionOptions, excludedExecutionOptions } =
      buildExecutionOptions(fileUploadStore)
    expect(selectedExecutionOptions).to.include.members([
      ExecutionOptionsEnum.ForwardGrowEnabled,
      ExecutionOptionsEnum.BackGrowEnabled,
      ExecutionOptionsEnum.DoIncludeFileHeader,
      ExecutionOptionsEnum.DoIncludeProjectedMOFVolumes,
      ExecutionOptionsEnum.DoIncludeSpeciesProjection,
    ])
    expect(excludedExecutionOptions).to.include.members([
      ExecutionOptionsEnum.DoSaveIntermediateFiles,
      ExecutionOptionsEnum.DoForceReferenceYearInclusionInYieldTables,
      ExecutionOptionsEnum.DoForceCurrentYearInclusionInYieldTables,
      ExecutionOptionsEnum.DoIncludePolygonRecordIdInYieldTable,
      ExecutionOptionsEnum.DoSummarizeProjectionByPolygon,
      ExecutionOptionsEnum.DoIncludeSecondarySpeciesDominantHeightInYieldTable,
      ExecutionOptionsEnum.AllowAggressiveValueEstimation,
      ExecutionOptionsEnum.DoIncludeProjectionFiles,
      ExecutionOptionsEnum.DoDelayExecutionFolderDeletion,
    ])
  })

  it('should return correct selected execution options for CFS biomass projection', () => {
    fileUploadStore.projectionType = CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS
    fileUploadStore.includeInReport = []
    const { selectedExecutionOptions, excludedExecutionOptions } =
      buildExecutionOptions(fileUploadStore)
    expect(selectedExecutionOptions).to.include(
      ExecutionOptionsEnum.ForwardGrowEnabled,
    )
    expect(selectedExecutionOptions).to.include(
      ExecutionOptionsEnum.BackGrowEnabled,
    )
    expect(selectedExecutionOptions).to.include(
      ExecutionOptionsEnum.DoIncludeProjectedCFSBiomass,
    )
    expect(selectedExecutionOptions).not.to.include(
      ExecutionOptionsEnum.DoIncludeSpeciesProjection,
    )
    expect(excludedExecutionOptions).to.include(
      ExecutionOptionsEnum.DoIncludeProjectedMOFVolumes,
    )
    expect(excludedExecutionOptions).to.include(
      ExecutionOptionsEnum.DoIncludeSpeciesProjection,
    )
  })

  it('should return selected execution options with only ForwardGrowEnabled when forwardBackwardGrow includes FORWARD', () => {
    fileUploadStore.isForwardGrowEnabled = true
    fileUploadStore.isBackwardGrowEnabled = false

    const { selectedExecutionOptions, excludedExecutionOptions } =
      buildExecutionOptions(fileUploadStore)
    expect(selectedExecutionOptions).to.include(
      ExecutionOptionsEnum.ForwardGrowEnabled,
    )
    expect(selectedExecutionOptions).not.to.include(
      ExecutionOptionsEnum.BackGrowEnabled,
    )
    expect(excludedExecutionOptions).to.include(
      ExecutionOptionsEnum.BackGrowEnabled,
    )
  })

  it('should return selected execution options with only BackGrowEnabled when forwardBackwardGrow includes BACKWARD', () => {
    fileUploadStore.isForwardGrowEnabled = false
    fileUploadStore.isBackwardGrowEnabled = true

    const { selectedExecutionOptions, excludedExecutionOptions } =
      buildExecutionOptions(fileUploadStore)
    expect(selectedExecutionOptions).not.to.include(
      ExecutionOptionsEnum.ForwardGrowEnabled,
    )
    expect(selectedExecutionOptions).to.include(
      ExecutionOptionsEnum.BackGrowEnabled,
    )
    expect(excludedExecutionOptions).to.include(
      ExecutionOptionsEnum.ForwardGrowEnabled,
    )
  })

  it('should not include ForwardGrowEnabled or BackGrowEnabled when forwardBackwardGrow is empty', () => {
    fileUploadStore.isForwardGrowEnabled = false
    fileUploadStore.isBackwardGrowEnabled = false

    const { selectedExecutionOptions, excludedExecutionOptions } =
      buildExecutionOptions(fileUploadStore)
    expect(selectedExecutionOptions).not.to.include(
      ExecutionOptionsEnum.ForwardGrowEnabled,
    )
    expect(selectedExecutionOptions).not.to.include(
      ExecutionOptionsEnum.BackGrowEnabled,
    )
    expect(excludedExecutionOptions).to.include.members([
      ExecutionOptionsEnum.ForwardGrowEnabled,
      ExecutionOptionsEnum.BackGrowEnabled,
    ])
  })

  it('should call projectionHcsvPost with correct form data', () => {
    const projectionStub = cy
      .stub()
      .resolves(new Blob(['mock response'], { type: 'application/json' }))

    runModelFileUpload(fileUploadStore, projectionStub).then(() => {
      expect(projectionStub.calledOnce).to.be.true
      const formDataArg = projectionStub.getCall(0).args[0] as FormData
      expect(formDataArg.has(ParameterNamesEnum.PROJECTION_PARAMETERS)).to.be
        .true
      expect(formDataArg.has(ParameterNamesEnum.HCSV_POLYGON_INPUT_DATA)).to.be
        .true
      expect(formDataArg.has(ParameterNamesEnum.HCSV_LAYERS_INPUT_DATA)).to.be
        .true

      const projectionParamsBlob = formDataArg.get(
        ParameterNamesEnum.PROJECTION_PARAMETERS,
      ) as Blob

      cy.wrap(projectionParamsBlob)
        .then((blob) => blob.text())
        .then((text) => {
          const projectionParams = JSON.parse(text)
          expect(projectionParams.ageStart).to.equal(10)
          expect(projectionParams.ageEnd).to.equal(100)
          expect(projectionParams.ageIncrement).to.equal(10)
          expect(projectionParams.yearStart).to.be.null
          expect(projectionParams.yearEnd).to.be.null
          expect(projectionParams.outputFormat).to.equal(
            OutputFormatEnum.CSVYieldTable,
          )
          expect(projectionParams.combineAgeYearRange).to.equal(
            CombineAgeYearRangeEnum.Intersect,
          )
          expect(projectionParams.metadataToOutput).to.equal(
            MetadataToOutputEnum.VERSION,
          )
          expect(projectionParams.selectedExecutionOptions).to.include(
            ExecutionOptionsEnum.ForwardGrowEnabled,
          )
          expect(projectionParams.selectedExecutionOptions).to.include(
            ExecutionOptionsEnum.BackGrowEnabled,
          )
          expect(projectionParams.selectedExecutionOptions).to.include(
            ExecutionOptionsEnum.DoIncludeProjectedMOFVolumes,
          )
          expect(projectionParams.excludedExecutionOptions).to.include.members([
            ExecutionOptionsEnum.DoSaveIntermediateFiles,
            ExecutionOptionsEnum.DoForceReferenceYearInclusionInYieldTables,
            ExecutionOptionsEnum.DoForceCurrentYearInclusionInYieldTables,
            ExecutionOptionsEnum.DoIncludePolygonRecordIdInYieldTable,
            ExecutionOptionsEnum.DoSummarizeProjectionByPolygon,
            ExecutionOptionsEnum.DoIncludeSecondarySpeciesDominantHeightInYieldTable,
            ExecutionOptionsEnum.AllowAggressiveValueEstimation,
            ExecutionOptionsEnum.DoIncludeProjectionFiles,
            ExecutionOptionsEnum.DoDelayExecutionFolderDeletion,
          ])
        })
    })
  })

  it('should return default selected debug options', () => {
    const { selectedDebugOptions, excludedDebugOptions } = buildDebugOptions()
    expect(selectedDebugOptions).to.deep.equal([
      DebugOptionsEnum.DoIncludeDebugTimestamps,
      DebugOptionsEnum.DoIncludeDebugEntryExit,
      DebugOptionsEnum.DoIncludeDebugIndentBlocks,
      DebugOptionsEnum.DoIncludeDebugRoutineNames,
    ])
    expect(excludedDebugOptions).to.be.empty
  })

  it('should create form data with correct parameters for AGE range', () => {
    const formData = getFormData(fileUploadStore)

    expect(formData.has(ParameterNamesEnum.PROJECTION_PARAMETERS)).to.be.true
    expect(formData.has(ParameterNamesEnum.HCSV_POLYGON_INPUT_DATA)).to.be.true
    expect(formData.has(ParameterNamesEnum.HCSV_LAYERS_INPUT_DATA)).to.be.true

    const projectionParamsBlob = formData.get(
      ParameterNamesEnum.PROJECTION_PARAMETERS,
    ) as Blob

    cy.wrap(projectionParamsBlob)
      .then((blob) => blob.text())
      .then((text) => {
        const projectionParams = JSON.parse(text)
        expect(projectionParams.ageStart).to.equal(10)
        expect(projectionParams.ageEnd).to.equal(100)
        expect(projectionParams.ageIncrement).to.equal(10)
        expect(projectionParams.yearStart).to.be.null
        expect(projectionParams.yearEnd).to.be.null
        expect(projectionParams.outputFormat).to.equal(
          OutputFormatEnum.CSVYieldTable,
        )
        expect(projectionParams.combineAgeYearRange).to.equal(
          CombineAgeYearRangeEnum.Intersect,
        )
        expect(projectionParams.metadataToOutput).to.equal(
          MetadataToOutputEnum.VERSION,
        )
        expect(projectionParams.selectedExecutionOptions).to.include(
          ExecutionOptionsEnum.ForwardGrowEnabled,
        )
        expect(projectionParams.selectedExecutionOptions).to.include(
          ExecutionOptionsEnum.BackGrowEnabled,
        )
        expect(projectionParams.selectedExecutionOptions).to.include(
          ExecutionOptionsEnum.DoIncludeProjectedMOFVolumes,
        )
        expect(projectionParams.excludedExecutionOptions).to.include.members([
          ExecutionOptionsEnum.DoSaveIntermediateFiles,
          ExecutionOptionsEnum.DoForceReferenceYearInclusionInYieldTables,
          ExecutionOptionsEnum.DoForceCurrentYearInclusionInYieldTables,
          ExecutionOptionsEnum.DoIncludePolygonRecordIdInYieldTable,
          ExecutionOptionsEnum.DoSummarizeProjectionByPolygon,
          ExecutionOptionsEnum.DoIncludeSecondarySpeciesDominantHeightInYieldTable,
          ExecutionOptionsEnum.AllowAggressiveValueEstimation,
          ExecutionOptionsEnum.DoIncludeProjectionFiles,
          ExecutionOptionsEnum.DoDelayExecutionFolderDeletion,
        ])
        expect(projectionParams.selectedDebugOptions).to.deep.equal([
          DebugOptionsEnum.DoIncludeDebugTimestamps,
          DebugOptionsEnum.DoIncludeDebugEntryExit,
          DebugOptionsEnum.DoIncludeDebugIndentBlocks,
          DebugOptionsEnum.DoIncludeDebugRoutineNames,
        ])
        expect(projectionParams.excludedDebugOptions).to.be.empty
      })
  })

  it('should create form data with correct parameters for YEAR range', () => {
    fileUploadStore.selectedAgeYearRange = CONSTANTS.AGE_YEAR_RANGE.YEAR
    const formData = getFormData(fileUploadStore)

    expect(formData.has(ParameterNamesEnum.PROJECTION_PARAMETERS)).to.be.true
    expect(formData.has(ParameterNamesEnum.HCSV_POLYGON_INPUT_DATA)).to.be.true
    expect(formData.has(ParameterNamesEnum.HCSV_LAYERS_INPUT_DATA)).to.be.true

    const projectionParamsBlob = formData.get(
      ParameterNamesEnum.PROJECTION_PARAMETERS,
    ) as Blob

    cy.wrap(projectionParamsBlob)
      .then((blob) => blob.text())
      .then((text) => {
        const projectionParams = JSON.parse(text)
        expect(projectionParams.ageStart).to.be.null
        expect(projectionParams.ageEnd).to.be.null
        expect(projectionParams.yearStart).to.equal(2020)
        expect(projectionParams.yearEnd).to.equal(2030)
        expect(projectionParams.ageIncrement).to.equal(2)
        expect(projectionParams.outputFormat).to.equal(
          OutputFormatEnum.CSVYieldTable,
        )
        expect(projectionParams.combineAgeYearRange).to.equal(
          CombineAgeYearRangeEnum.Intersect,
        )
        expect(projectionParams.metadataToOutput).to.equal(
          MetadataToOutputEnum.VERSION,
        )
        expect(projectionParams.selectedExecutionOptions).to.include(
          ExecutionOptionsEnum.ForwardGrowEnabled,
        )
        expect(projectionParams.selectedExecutionOptions).to.include(
          ExecutionOptionsEnum.BackGrowEnabled,
        )
        expect(projectionParams.selectedExecutionOptions).to.include(
          ExecutionOptionsEnum.DoIncludeProjectedMOFVolumes,
        )
        expect(projectionParams.excludedExecutionOptions).to.include.members([
          ExecutionOptionsEnum.DoSaveIntermediateFiles,
          ExecutionOptionsEnum.DoForceReferenceYearInclusionInYieldTables,
          ExecutionOptionsEnum.DoForceCurrentYearInclusionInYieldTables,
          ExecutionOptionsEnum.DoIncludePolygonRecordIdInYieldTable,
          ExecutionOptionsEnum.DoSummarizeProjectionByPolygon,
          ExecutionOptionsEnum.DoIncludeSecondarySpeciesDominantHeightInYieldTable,
          ExecutionOptionsEnum.AllowAggressiveValueEstimation,
          ExecutionOptionsEnum.DoIncludeProjectionFiles,
          ExecutionOptionsEnum.DoDelayExecutionFolderDeletion,
        ])
        expect(projectionParams.selectedDebugOptions).to.deep.equal([
          DebugOptionsEnum.DoIncludeDebugTimestamps,
          DebugOptionsEnum.DoIncludeDebugEntryExit,
          DebugOptionsEnum.DoIncludeDebugIndentBlocks,
          DebugOptionsEnum.DoIncludeDebugRoutineNames,
        ])
        expect(projectionParams.excludedDebugOptions).to.be.empty
      })
  })
})
