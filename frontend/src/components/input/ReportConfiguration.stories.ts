import type { Meta, StoryObj } from '@storybook/vue3-vite'
import { createPinia, setActivePinia } from 'pinia'
import { useModelParameterStore } from '@/stores/modelParameterStore'
import { useAppStore } from '@/stores/appStore'
import { useFileUploadStore } from '@/stores/fileUploadStore'
import ReportConfiguration from './ReportConfiguration.vue'
import { CONSTANTS, DEFAULTS, OPTIONS } from '@/constants'

const pinia = createPinia()
setActivePinia(pinia)

const meta: Meta<typeof ReportConfiguration> = {
  title: 'components/input/ReportConfiguration',
  component: ReportConfiguration,
  decorators: [
    (story) => {
      const appStore = useAppStore()
      // Set app to model parameter mode
      appStore.modelSelection = CONSTANTS.MODEL_SELECTION.INPUT_MODEL_PARAMETERS

      const modelParameterStore = useModelParameterStore()
      modelParameterStore.setDefaultValues()

      modelParameterStore.confirmPanel(
        CONSTANTS.MODEL_PARAMETER_PANEL.SPECIES_INFO,
      )
      modelParameterStore.confirmPanel(
        CONSTANTS.MODEL_PARAMETER_PANEL.SITE_INFO,
      )
      modelParameterStore.confirmPanel(
        CONSTANTS.MODEL_PARAMETER_PANEL.STAND_INFO,
      )

      return {
        components: { story },
        template: `<div><story /></div>`,
      }
    },
  ],
  tags: ['autodocs'],
  argTypes: {
    startingAge: {
      control: { type: 'number' },
      description: 'The starting age for the report configuration',
      defaultValue: DEFAULTS.DEFAULT_VALUES.STARTING_AGE,
    },
    finishingAge: {
      control: { type: 'number' },
      description: 'The finishing age for the report configuration',
      defaultValue: DEFAULTS.DEFAULT_VALUES.FINISHING_AGE,
    },
    ageIncrement: {
      control: { type: 'number' },
      description: 'The age increment for the report configuration',
      defaultValue: DEFAULTS.DEFAULT_VALUES.AGE_INCREMENT,
    },
    startYear: {
      control: { type: 'number' },
      description: 'The start year for the report configuration',
      defaultValue: DEFAULTS.DEFAULT_VALUES.START_YEAR,
    },
    endYear: {
      control: { type: 'number' },
      description: 'The end year for the report configuration',
      defaultValue: DEFAULTS.DEFAULT_VALUES.END_YEAR,
    },
    yearIncrement: {
      control: { type: 'number' },
      description: 'The year increment for the report configuration',
      defaultValue: DEFAULTS.DEFAULT_VALUES.YEAR_INCREMENT,
    },
    isForwardGrowEnabled: {
      control: { type: 'boolean' },
      description: 'Enables forward growth calculation',
      defaultValue: DEFAULTS.DEFAULT_VALUES.IS_FORWARD_GROW_ENABLED,
    },
    isBackwardGrowEnabled: {
      control: { type: 'boolean' },
      description: 'Enables backward growth calculation',
      defaultValue: DEFAULTS.DEFAULT_VALUES.IS_BACKWARD_GROW_ENABLED,
    },
    projectionType: {
      control: {
        type: 'select',
        options: OPTIONS.projectionTypeOptions.map((opt) => opt.value),
      },
      description: 'The projection type for the report configuration',
      defaultValue: null,
    },
    reportTitle: {
      control: { type: 'text' },
      description: 'The report title',
      defaultValue: DEFAULTS.DEFAULT_VALUES.REPORT_TITLE,
    },
    isDisabled: {
      control: { type: 'boolean' },
      description: 'Disables the entire form when set to true',
      defaultValue: false,
    },
    isModelParametersMode: {
      control: { type: 'boolean' },
      description:
        'Toggles visibility of Minimum DBH Limit by Species Group section',
      defaultValue: true,
    },
  },
}

export default meta
type Story = StoryObj<typeof ReportConfiguration>

export const DefaultConfiguration: Story = {
  args: {
    startingAge: DEFAULTS.DEFAULT_VALUES.STARTING_AGE,
    finishingAge: DEFAULTS.DEFAULT_VALUES.FINISHING_AGE,
    ageIncrement: DEFAULTS.DEFAULT_VALUES.AGE_INCREMENT,
    startYear: DEFAULTS.DEFAULT_VALUES.START_YEAR,
    endYear: DEFAULTS.DEFAULT_VALUES.END_YEAR,
    yearIncrement: DEFAULTS.DEFAULT_VALUES.YEAR_INCREMENT,
    isForwardGrowEnabled: DEFAULTS.DEFAULT_VALUES.IS_FORWARD_GROW_ENABLED,
    isBackwardGrowEnabled: DEFAULTS.DEFAULT_VALUES.IS_BACKWARD_GROW_ENABLED,
    projectionType: DEFAULTS.DEFAULT_VALUES.PROJECTION_TYPE,
    reportTitle: DEFAULTS.DEFAULT_VALUES.REPORT_TITLE,
    isDisabled: false,
    isModelParametersMode: true,
  },
}

export const DisabledConfiguration: Story = {
  args: {
    startingAge: DEFAULTS.DEFAULT_VALUES.STARTING_AGE,
    finishingAge: DEFAULTS.DEFAULT_VALUES.FINISHING_AGE,
    ageIncrement: DEFAULTS.DEFAULT_VALUES.AGE_INCREMENT,
    startYear: DEFAULTS.DEFAULT_VALUES.START_YEAR,
    endYear: DEFAULTS.DEFAULT_VALUES.END_YEAR,
    yearIncrement: DEFAULTS.DEFAULT_VALUES.YEAR_INCREMENT,
    isForwardGrowEnabled: DEFAULTS.DEFAULT_VALUES.IS_FORWARD_GROW_ENABLED,
    isBackwardGrowEnabled: DEFAULTS.DEFAULT_VALUES.IS_BACKWARD_GROW_ENABLED,
    projectionType: DEFAULTS.DEFAULT_VALUES.PROJECTION_TYPE,
    reportTitle: DEFAULTS.DEFAULT_VALUES.REPORT_TITLE,
    isDisabled: true,
    isModelParametersMode: true,
  },
}

export const FileUploadMode: Story = {
  decorators: [
    (story) => {
      const appStore = useAppStore()
      const fileUploadStore = useFileUploadStore()

      // Set app to file upload mode
      appStore.modelSelection = CONSTANTS.MODEL_SELECTION.FILE_UPLOAD

      // Initialize file upload store
      fileUploadStore.setDefaultValues()

      return {
        components: { story },
        template: `<div><story /></div>`,
      }
    },
  ],
  args: {
    startingAge: DEFAULTS.DEFAULT_VALUES.STARTING_AGE,
    finishingAge: DEFAULTS.DEFAULT_VALUES.FINISHING_AGE,
    ageIncrement: DEFAULTS.DEFAULT_VALUES.AGE_INCREMENT,
    startYear: DEFAULTS.DEFAULT_VALUES.START_YEAR,
    endYear: DEFAULTS.DEFAULT_VALUES.END_YEAR,
    yearIncrement: DEFAULTS.DEFAULT_VALUES.YEAR_INCREMENT,
    isForwardGrowEnabled: DEFAULTS.DEFAULT_VALUES.IS_FORWARD_GROW_ENABLED,
    isBackwardGrowEnabled: DEFAULTS.DEFAULT_VALUES.IS_BACKWARD_GROW_ENABLED,
    isByLayerEnabled: DEFAULTS.DEFAULT_VALUES.IS_BY_LAYER_ENABLED,
    isBySpeciesEnabled: DEFAULTS.DEFAULT_VALUES.IS_BY_SPECIES_ENABLED,
    isPolygonIDEnabled: DEFAULTS.DEFAULT_VALUES.IS_POLYGON_ID_ENABLED,
    isProjectionModeEnabled: DEFAULTS.DEFAULT_VALUES.IS_PROJECTION_MODE_ENABLED,
    isCurrentYearEnabled: DEFAULTS.DEFAULT_VALUES.IS_CURRENT_YEAR_ENABLED,
    isReferenceYearEnabled: DEFAULTS.DEFAULT_VALUES.IS_REFERENCE_YEAR_ENABLED,
    projectionType: DEFAULTS.DEFAULT_VALUES.PROJECTION_TYPE,
    reportTitle: DEFAULTS.DEFAULT_VALUES.REPORT_TITLE,
    isDisabled: false,
    isModelParametersMode: false,
  },
}
