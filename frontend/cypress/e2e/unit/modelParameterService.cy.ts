/// <reference types="cypress" />

import { createCSVFiles, runModel } from '@/services/modelParameterService'
import { SelectedExecutionOptionsEnum } from '@/services/vdyp-api'
import * as apiActions from '@/services/apiActions'
import { CONSTANTS, DEFAULTS, OPTIONS } from '@/constants'
import sinon from 'sinon'

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
    startingAge: DEFAULTS.DEFAULT_VALUES.STARTING_AGE,
    finishingAge: DEFAULTS.DEFAULT_VALUES.FINISHING_AGE,
    ageIncrement: DEFAULTS.DEFAULT_VALUES.AGE_INCREMENT,
    includeInReport: [],
  }

  let projectionStub: sinon.SinonStub

  beforeEach(() => {
    projectionStub = sinon
      .stub(apiActions, 'projectionHcsvPost')
      .resolves(new Blob(['mock response'], { type: 'application/json' }))
  })

  afterEach(() => {
    projectionStub.restore()
  })

  it('should call projectionHcsvPost once', async () => {
    await runModel(mockModelParameterStore)
    expect(projectionStub.calledOnce).to.be.true
  })

  it('should create CSV files correctly', () => {
    const { blobPolygon, blobLayer } = createCSVFiles(mockModelParameterStore)

    cy.wrap(blobPolygon).should('be.instanceOf', Blob)
    cy.wrap(blobLayer).should('be.instanceOf', Blob)

    const derivedByCode =
      mockModelParameterStore.derivedBy === CONSTANTS.DERIVED_BY.VOLUME
        ? CONSTANTS.INVENTORY_CODES.FIP
        : mockModelParameterStore.derivedBy === CONSTANTS.DERIVED_BY.BASAL_AREA
          ? CONSTANTS.INVENTORY_CODES.VRI
          : ''

    cy.wrap(blobPolygon.text()).then((text) => {
      expect(text).to.include(derivedByCode)
      expect(text).to.include(mockModelParameterStore.becZone)
      expect(text).to.include(mockModelParameterStore.ecoZone)
      expect(text).to.include(mockModelParameterStore.percentStockableArea)
    })

    cy.wrap(blobLayer.text()).then((text) => {
      expect(text).to.include(mockModelParameterStore.highestPercentSpecies)
      expect(text).to.include('PL,30.0')
      expect(text).to.include('AC,30.0')
      expect(text).to.include('H,30.0')
      expect(text).to.include('S,10.0')
    })
  })

  it('should reject with error for invalid model parameters', async () => {
    const invalidModelParameterStore = {
      ...mockModelParameterStore,
      startingAge: null,
    }

    try {
      await runModel(invalidModelParameterStore)
    } catch (error) {
      expect(error).to.be.an('error')
    }
  })

  it('should call projectionHcsvPost with correct form data', async () => {
    await runModel(mockModelParameterStore)

    expect(projectionStub.calledOnce).to.be.true
    const formDataArg = projectionStub.getCall(0).args[0] as FormData

    expect(formDataArg.has('polygonInputData')).to.be.true
    expect(formDataArg.has('layersInputData')).to.be.true
    expect(formDataArg.has('projectionParameters')).to.be.true
  })

  it('should include additional options when secondary height is enabled', async () => {
    const updatedModelParameterStore = {
      ...mockModelParameterStore,
      incSecondaryHeight: true,
    }

    await runModel(updatedModelParameterStore)

    const formDataArg = projectionStub.getCall(0).args[0] as FormData
    const projectionParamsBlob = formDataArg.get('projectionParameters') as Blob

    const projectionParamsText = await projectionParamsBlob.text()
    const projectionParams = JSON.parse(projectionParamsText)

    expect(projectionParams.selectedExecutionOptions).to.include(
      SelectedExecutionOptionsEnum.DoIncludeSecondarySpeciesDominantHeightInYieldTable,
    )
  })

  it('should contain expected execution options', async () => {
    await runModel(mockModelParameterStore)

    const formDataArg = projectionStub.getCall(0).args[0] as FormData
    const projectionParamsBlob = formDataArg.get('projectionParameters') as Blob

    const projectionParamsText = await projectionParamsBlob.text()
    const projectionParams = JSON.parse(projectionParamsText)

    expect(projectionParams.selectedExecutionOptions).to.include(
      SelectedExecutionOptionsEnum.DoEnableProgressLogging,
      SelectedExecutionOptionsEnum.DoEnableErrorLogging,
    )
  })

  it('should handle empty species list without errors', async () => {
    const emptySpeciesStore = {
      ...mockModelParameterStore,
      speciesList: new Array(6).fill({ species: null, percent: '0.0' }),
    }

    await runModel(emptySpeciesStore)
    expect(projectionStub.calledOnce).to.be.true
  })

  it('should handle missing fields in model parameter store', () => {
    const invalidStore = {
      ...mockModelParameterStore,
      becZone: undefined,
    }

    const { blobPolygon } = createCSVFiles(invalidStore)
    cy.wrap(blobPolygon.text()).then((text) => {
      expect(text).not.to.include(DEFAULTS.DEFAULT_VALUES.BEC_ZONE)
    })
  })

  it('should handle projectionHcsvPost failure', async () => {
    projectionStub.rejects(new Error('API call failed'))

    try {
      await runModel(mockModelParameterStore)
    } catch (error) {
      expect(error).to.be.an('error')
      expect(error.message).to.equal('API call failed')
    }
  })
})
