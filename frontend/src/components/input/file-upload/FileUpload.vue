<template>
  <div>
    <AppProgressCircular
      :isShow="isProgressVisible"
      :showMessage="true"
      :message="progressMessage"
      :hasBackground="true"
    />
    <AppMessageDialog
      :dialog="messageDialog.dialog"
      :title="messageDialog.title"
      :message="messageDialog.message"
      :dialogWidth="messageDialog.dialogWidth"
      :btnLabel="messageDialog.btnLabel"
      @update:dialog="(value) => (messageDialog.dialog = value)"
      @close="handleDialogClose"
    />
    <v-form ref="form" @submit.prevent="runModelHandler">
      <v-card class="elevation-4">
        <div class="pl-16 pt-10">
          <ReportConfiguration
            :startingAge="startingAge"
            :finishingAge="finishingAge"
            :ageIncrement="ageIncrement"
            :volumeReported="volumeReported"
            :includeInReport="includeInReport"
            :projectionType="projectionType"
            :reportTitle="reportTitle"
            :isDisabled="false"
            @update:startingAge="handleStartingAgeUpdate"
            @update:finishingAge="handleFinishingAgeUpdate"
            @update:ageIncrement="handleAgeIncrementUpdate"
            @update:volumeReported="handleVolumeReportedUpdate"
            @update:includeInReport="handleIncludeInReportUpdate"
            @update:projectionType="handleProjectionTypeUpdate"
            @update:reportTitle="handleReportTitleUpdate"
          />
          <div class="ml-4 mt-10 mb-10">
            <div class="ml-n4 mt-n5">
              <span class="text-h7">Attachments</span>
            </div>
            <v-row class="mb-n10">
              <v-col cols="5">
                <v-file-input
                  :label="
                    polygonFile ? 'Polygon File' : 'Select Polygon File...'
                  "
                  v-model="polygonFile"
                  show-size
                  chips
                  clearable
                  density="compact"
                  accept=".csv"
                />
              </v-col>
              <v-col class="col-space-3" />
              <v-col cols="5">
                <v-file-input
                  :label="layerFile ? 'Layer File' : 'Select Layer File...'"
                  v-model="layerFile"
                  show-size
                  chips
                  clearable
                  density="compact"
                  accept=".csv"
                />
              </v-col>
            </v-row>
          </div>
        </div>
        <AppRunModelButton
          :isDisabled="false"
          cardClass="file-upload-run-model-card"
          cardActionsClass="card-actions"
          @runModel="runModelHandler"
        />
      </v-card>
    </v-form>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useReportingStore } from '@/stores/reportingStore'
import {
  OutputFormatEnum,
  SelectedExecutionOptionsEnum,
  SelectedDebugOptionsEnum,
  MetadataToOutputEnum,
  CombineAgeYearRangeEnum,
  ParameterNamesEnum,
} from '@/services/vdyp-api'
import { projectionHcsvPost } from '@/services/apiActions'
import { handleApiError } from '@/services/apiErrorHandler'
import {
  AppRunModelButton,
  AppProgressCircular,
  AppMessageDialog,
  ReportConfiguration,
} from '@/components'
import type { MessageDialog } from '@/interfaces/interfaces'
import { CONSTANTS, MESSAGE, DEFAULTS } from '@/constants'
import { fileUploadValidation } from '@/validation'
import { delay, downloadFile, extractZipFileName } from '@/utils/util'
import { logSuccessMessage } from '@/utils/messageHandler'

const form = ref<HTMLFormElement>()

const isProgressVisible = ref(false)
const progressMessage = ref('')

const startingAge = ref<number | null>(DEFAULTS.DEFAULT_VALUES.STARTING_AGE)
const finishingAge = ref<number | null>(DEFAULTS.DEFAULT_VALUES.FINISHING_AGE)
const ageIncrement = ref<number | null>(DEFAULTS.DEFAULT_VALUES.AGE_INCREMENT)
const volumeReported = ref<string[]>(DEFAULTS.DEFAULT_VALUES.VOLUME_REPORTED)
const includeInReport = ref<string[]>([])
const projectionType = ref<string | null>(
  DEFAULTS.DEFAULT_VALUES.PROJECTION_TYPE,
)
const reportTitle = ref<string | null>(DEFAULTS.DEFAULT_VALUES.REPORT_TITLE)

const layerFile = ref<File | null>(null)
const polygonFile = ref<File | null>(null)

const messageDialog = ref<MessageDialog>({
  dialog: false,
  title: '',
  message: '',
  dialogWidth: undefined,
  btnLabel: '',
})

const reportingStore = useReportingStore()

/**
 * Updates the starting age value.
 * @param value - The new starting age.
 */
const handleStartingAgeUpdate = (value: number | null) => {
  startingAge.value = value
}

/**
 * Updates the finishing age value.
 * @param value - The new finishing age.
 */
const handleFinishingAgeUpdate = (value: number | null) => {
  finishingAge.value = value
}

/**
 * Updates the age increment value.
 * @param value - The new age increment.
 */
const handleAgeIncrementUpdate = (value: number | null) => {
  ageIncrement.value = value
}

/**
 * Updates the volume reported array.
 * @param value - The new volume reported array.
 */
const handleVolumeReportedUpdate = (value: string[]) => {
  volumeReported.value = [...value]
}

/**
 * Updates the include in report array.
 * @param value - The new array for including items in the report.
 */
const handleIncludeInReportUpdate = (value: string[]) => {
  includeInReport.value = [...value]
}

/**
 * Updates the projection type.
 * @param value - The new projection type.
 */
const handleProjectionTypeUpdate = (value: string | null) => {
  projectionType.value = value
}

/**
 * Updates the report title.
 * @param value - The new report title.
 */
const handleReportTitleUpdate = (value: string | null) => {
  reportTitle.value = value
}

/**
 * Validates the starting and finishing age comparison.
 * Displays an error dialog if validation fails.
 * @returns True if the comparison is valid, false otherwise.
 */
const validateComparison = (): boolean => {
  const result = fileUploadValidation.validateComparison(
    startingAge.value,
    finishingAge.value,
  )
  if (!result.isValid) {
    messageDialog.value = {
      dialog: true,
      title: MESSAGE.MSG_DIALOG_TITLE.INVALID_INPUT,
      message: MESSAGE.MDL_PRM_INPUT_ERR.RPT_VLD_COMP_FNSH_AGE,
      btnLabel: CONSTANTS.BUTTON_LABEL.CONT_EDIT,
    }
  }
  return result.isValid
}

/**
 * Validates that all required fields are provided.
 * Displays an error dialog if any required field is missing.
 * @returns True if all required fields are valid, false otherwise.
 */
const validateRequiredFields = (): boolean => {
  const result = fileUploadValidation.validateRequiredFields(
    startingAge.value,
    finishingAge.value,
    ageIncrement.value,
  )
  if (!result.isValid) {
    messageDialog.value = {
      dialog: true,
      title: MESSAGE.MSG_DIALOG_TITLE.INVALID_INPUT,
      message: MESSAGE.FILE_UPLOAD_ERR.RPT_VLD_REQUIRED_FIELDS,
      btnLabel: CONSTANTS.BUTTON_LABEL.CONT_EDIT,
    }
  }
  return result.isValid
}

/**
 * Validates that the age range values fall within the allowed limits.
 * Displays an error dialog if validation fails.
 * @returns True if the age range is valid, false otherwise.
 */
const validateRange = (): boolean => {
  const result = fileUploadValidation.validateRange(
    startingAge.value,
    finishingAge.value,
    ageIncrement.value,
  )
  if (!result.isValid) {
    let message = ''
    switch (result.errorType) {
      case 'startingAge':
        message = MESSAGE.MDL_PRM_INPUT_ERR.RPT_VLD_START_AGE_RNG(
          CONSTANTS.NUM_INPUT_LIMITS.STARTING_AGE_MIN,
          CONSTANTS.NUM_INPUT_LIMITS.STARTING_AGE_MAX,
        )
        break
      case 'finishingAge':
        message = MESSAGE.MDL_PRM_INPUT_ERR.RPT_VLD_START_FNSH_RNG(
          CONSTANTS.NUM_INPUT_LIMITS.FINISHING_AGE_MIN,
          CONSTANTS.NUM_INPUT_LIMITS.FINISHING_AGE_MAX,
        )
        break
      case 'ageIncrement':
        message = MESSAGE.MDL_PRM_INPUT_ERR.RPT_VLD_AGE_INC_RNG(
          CONSTANTS.NUM_INPUT_LIMITS.AGE_INC_MIN,
          CONSTANTS.NUM_INPUT_LIMITS.AGE_INC_MAX,
        )
        break
    }
    messageDialog.value = {
      dialog: true,
      title: MESSAGE.MSG_DIALOG_TITLE.INVALID_INPUT,
      message: message,
      btnLabel: CONSTANTS.BUTTON_LABEL.CONT_EDIT,
    }
  }
  return result.isValid
}

/**
 * Asynchronously validates that the necessary files are provided and have correct formats.
 * Displays an error dialog if validation fails.
 * @returns A Promise that resolves to true if file validation passes, false otherwise.
 */
const validateFiles = async (): Promise<boolean> => {
  const result = await fileUploadValidation.validateFiles(
    polygonFile.value,
    layerFile.value,
  )
  if (!result.isValid) {
    let message = ''
    switch (result.errorType) {
      case 'polygonFileMissing':
        message = MESSAGE.FILE_UPLOAD_ERR.POLYGON_FILE_MISSING
        break
      case 'layerFileMissing':
        message = MESSAGE.FILE_UPLOAD_ERR.LAYER_FILE_MISSING
        break
      case 'polygonFileNotCSVFormat':
        message = MESSAGE.FILE_UPLOAD_ERR.POLYGON_FILE_NOT_CSV_FORMAT
        break
      case 'layerFileNotCSVFormat':
        message = MESSAGE.FILE_UPLOAD_ERR.LAYER_FILE_NOT_CSV_FORMAT
        break
    }
    messageDialog.value = {
      dialog: true,
      title: MESSAGE.MSG_DIALOG_TITLE.INVALID_FILE,
      message: message,
      btnLabel: CONSTANTS.BUTTON_LABEL.CONT_EDIT,
    }
  }
  return result.isValid
}

/**
 * Returns an array of selected execution options based on the current projection type and report settings.
 * @returns An array of execution option enums.
 */
const getSelectedExecutionOptions = () => {
  const selectedExecutionOptions = [
    SelectedExecutionOptionsEnum.ForwardGrowEnabled,
    SelectedExecutionOptionsEnum.DoIncludeFileHeader,
    SelectedExecutionOptionsEnum.DoIncludeProjectionModeInYieldTable,
    SelectedExecutionOptionsEnum.DoIncludeAgeRowsInYieldTable,
    SelectedExecutionOptionsEnum.DoIncludeYearRowsInYieldTable,
    SelectedExecutionOptionsEnum.DoSummarizeProjectionByLayer,
    SelectedExecutionOptionsEnum.DoIncludeColumnHeadersInYieldTable,
    SelectedExecutionOptionsEnum.DoAllowBasalAreaAndTreesPerHectareValueSubstitution,
    SelectedExecutionOptionsEnum.DoEnableProgressLogging,
    SelectedExecutionOptionsEnum.DoEnableErrorLogging,
    SelectedExecutionOptionsEnum.DoEnableDebugLogging,
  ]

  if (projectionType.value === CONSTANTS.PROJECTION_TYPE.VOLUME) {
    selectedExecutionOptions.push(
      SelectedExecutionOptionsEnum.DoIncludeProjectedMOFVolumes,
    )
  } else if (projectionType.value === CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS) {
    selectedExecutionOptions.push(
      SelectedExecutionOptionsEnum.DoIncludeProjectedCFSBiomass,
    )
  }

  if (
    includeInReport.value &&
    includeInReport.value.includes(
      CONSTANTS.INCLUDE_IN_REPORT.SPECIES_COMPOSITION,
    )
  ) {
    selectedExecutionOptions.push(
      SelectedExecutionOptionsEnum.DoIncludeSpeciesProjection,
    )
  }

  return selectedExecutionOptions
}

/**
 * Returns an array of selected debug options.
 * @returns An array of debug option enums.
 */
const getSelectedDebugOptions = () => {
  const selectedDebugOptions: Array<SelectedDebugOptionsEnum> = [
    SelectedDebugOptionsEnum.DoIncludeDebugTimestamps,
    SelectedDebugOptionsEnum.DoIncludeDebugEntryExit,
    SelectedDebugOptionsEnum.DoIncludeDebugIndentBlocks,
    SelectedDebugOptionsEnum.DoIncludeDebugRoutineNames,
  ]

  return selectedDebugOptions
}

/**
 * Constructs a FormData object with the projection parameters and attached files.
 * @returns The FormData object containing projection parameters and file inputs.
 */
const getFormData = () => {
  const projectionParameters = {
    ageStart: startingAge.value,
    ageEnd: finishingAge.value,
    ageIncrement: ageIncrement.value,
    outputFormat: OutputFormatEnum.CSVYieldTable,
    selectedExecutionOptions: getSelectedExecutionOptions(),
    selectedDebugOptions: getSelectedDebugOptions(),
    combineAgeYearRange: CombineAgeYearRangeEnum.Intersect,
    metadataToOutput: MetadataToOutputEnum.VERSION,
  }

  const formData = new FormData()

  formData.append(
    ParameterNamesEnum.PROJECTION_PARAMETERS,
    new Blob([JSON.stringify(projectionParameters)], {
      type: 'application/json',
    }),
  )
  formData.append(
    ParameterNamesEnum.HCSV_POLYGON_INPUT_DATA,
    polygonFile.value as Blob,
  )
  formData.append(
    ParameterNamesEnum.HCSV_LAYERS_INPUT_DATA,
    layerFile.value as Blob,
  )

  return formData
}

/**
 * Handles the run model process.
 * Validates inputs, sends the projection request, and downloads the resulting file.
 */
const runModelHandler = async () => {
  try {
    if (!validateComparison()) return
    if (!validateRequiredFields()) return
    if (!validateRange()) return
    if (!(await validateFiles())) return

    if (form.value) {
      form.value.validate()
    } else {
      console.warn('Form reference is null. Validation skipped.')
    }

    isProgressVisible.value = true
    progressMessage.value = MESSAGE.PROGRESS_MSG.RUNNING_MODEL

    await delay(1000)

    const response = await projectionHcsvPost(getFormData(), false)

    console.debug('Full response:', response)

    const zipFileName =
      extractZipFileName(response.headers) ||
      CONSTANTS.FILE_NAME.PROJECTION_RESULT_ZIP
    console.debug('download zip file name:', zipFileName)

    const resultBlob = response.data

    console.debug('resultBlob:', resultBlob, 'type:', resultBlob?.type)
    console.debug('resultBlob size:', resultBlob.size)

    if (!resultBlob) {
      throw new Error('Response data is undefined')
    }

    if (!(resultBlob instanceof Blob)) {
      throw new Error('Response data is not a Blob')
    }

    downloadFile(resultBlob, zipFileName)

    logSuccessMessage(MESSAGE.SUCCESS_MSG.FILE_UPLOAD_RUN_RESULT)

    reportingStore.fileUploadEnableTabs()
  } catch (error) {
    handleApiError(error, MESSAGE.FILE_UPLOAD_ERR.FAIL_RUN_MODEL)
  } finally {
    isProgressVisible.value = false
  }
}

/**
 * Handles the closing of the message dialog.
 */
const handleDialogClose = () => {}
</script>
<style scoped></style>
