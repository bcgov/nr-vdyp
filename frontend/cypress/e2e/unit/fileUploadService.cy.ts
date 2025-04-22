/// <reference types="cypress" />

import {
  getSelectedExecutionOptions,
  getSelectedDebugOptions,
  getFormData,
  runModelFileUpload,
} from '@/services/fileUploadService'
import { useFileUploadStore } from '@/stores/fileUploadStore'
import {
  SelectedExecutionOptionsEnum,
  SelectedDebugOptionsEnum,
  ParameterNamesEnum,
  OutputFormatEnum,
  CombineAgeYearRangeEnum,
  MetadataToOutputEnum,
} from '@/services/vdyp-api'
import * as apiActions from '@/services/apiActions'
import { CONSTANTS } from '@/constants'
import { createPinia, setActivePinia } from 'pinia'
import sinon, { SinonStub } from 'sinon'

describe('File Upload Service Unit Tests', () => {
  let fileUploadStore: ReturnType<typeof useFileUploadStore>
  let projectionStub: SinonStub

  beforeEach(() => {
    setActivePinia(createPinia())
    fileUploadStore = useFileUploadStore()

    fileUploadStore.projectionType = CONSTANTS.PROJECTION_TYPE.VOLUME
    fileUploadStore.includeInReport = [
      CONSTANTS.INCLUDE_IN_REPORT.SPECIES_COMPOSITION,
    ]
    fileUploadStore.selectedAgeYearRange = CONSTANTS.AGE_YEAR_RANGE.AGE
    fileUploadStore.startingAge = 10
    fileUploadStore.finishingAge = 100
    fileUploadStore.ageIncrement = 10
    fileUploadStore.startYear = 2020
    fileUploadStore.endYear = 2030
    fileUploadStore.yearIncrement = 2
    fileUploadStore.polygonFile = new File(['polygon content'], 'polygon.csv', {
      type: 'text/csv',
    })
    fileUploadStore.layerFile = new File(['layer content'], 'layer.csv', {
      type: 'text/csv',
    })

    projectionStub = sinon
      .stub(apiActions, 'projectionHcsvPost')
      .resolves(new Blob(['mock response'], { type: 'application/json' }))
  })

  afterEach(() => {
    sinon.restore()
  })

  it('should return correct selected execution options for volume projection', () => {
    const options = getSelectedExecutionOptions(fileUploadStore)
    expect(options).to.include.members([
      SelectedExecutionOptionsEnum.ForwardGrowEnabled,
      SelectedExecutionOptionsEnum.DoIncludeFileHeader,
      SelectedExecutionOptionsEnum.DoIncludeProjectedMOFVolumes,
      SelectedExecutionOptionsEnum.DoIncludeSpeciesProjection,
    ])
  })

  it('should return correct selected execution options for CFS biomass projection', () => {
    fileUploadStore.projectionType = CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS
    fileUploadStore.includeInReport = []
    const options = getSelectedExecutionOptions(fileUploadStore)
    expect(options).to.include(
      SelectedExecutionOptionsEnum.DoIncludeProjectedCFSBiomass,
    )
    expect(options).not.to.include(
      SelectedExecutionOptionsEnum.DoIncludeSpeciesProjection,
    )
  })

  it('should return default selected debug options', () => {
    const debugOptions = getSelectedDebugOptions()
    expect(debugOptions).to.deep.equal([
      SelectedDebugOptionsEnum.DoIncludeDebugTimestamps,
      SelectedDebugOptionsEnum.DoIncludeDebugEntryExit,
      SelectedDebugOptionsEnum.DoIncludeDebugIndentBlocks,
      SelectedDebugOptionsEnum.DoIncludeDebugRoutineNames,
    ])
  })

  it('should create form data with correct parameters for AGE range', () => {
    const formData = getFormData(fileUploadStore)

    expect(formData.has(ParameterNamesEnum.PROJECTION_PARAMETERS)).to.be.true
    expect(formData.has(ParameterNamesEnum.HCSV_POLYGON_INPUT_DATA)).to.be.true
    expect(formData.has(ParameterNamesEnum.HCSV_LAYERS_INPUT_DATA)).to.be.true

    const projectionParamsBlob = formData.get(
      ParameterNamesEnum.PROJECTION_PARAMETERS,
    ) as Blob
    cy.wrap(projectionParamsBlob.text()).then((text: string) => {
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
        SelectedExecutionOptionsEnum.DoIncludeProjectedMOFVolumes,
      )
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
    cy.wrap(projectionParamsBlob.text()).then((text: string) => {
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
        SelectedExecutionOptionsEnum.DoIncludeProjectedMOFVolumes,
      )
    })
  })

  it('should call projectionHcsvPost with correct form data', () => {
    cy.wrap(runModelFileUpload(fileUploadStore)).then(() => {
      expect(projectionStub.calledOnce).to.be.true
      const formDataArg = projectionStub.getCall(0).args[0] as FormData
      expect(formDataArg.has(ParameterNamesEnum.PROJECTION_PARAMETERS)).to.be
        .true
      expect(formDataArg.has(ParameterNamesEnum.HCSV_POLYGON_INPUT_DATA)).to.be
        .true
      expect(formDataArg.has(ParameterNamesEnum.HCSV_LAYERS_INPUT_DATA)).to.be
        .true
    })
  })
})
