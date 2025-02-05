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
                  label="Layer File"
                  v-model="layerFile"
                  show-size
                  chips
                  clearable
                  flat
                  persistent-placeholder
                  placeholder="Select Eco Zone"
                  density="compact"
                  accept=".csv"
                />
              </v-col>
              <v-col class="col-space-3" />
              <v-col cols="5">
                <v-file-input
                  label="Polygon File"
                  v-model="polygonFile"
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
import {
  OutputFormatEnum,
  SelectedExecutionOptionsEnum,
  SelectedDebugOptionsEnum,
  MetadataToOutputEnum,
  CombineAgeYearRangeEnum,
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
import { Util } from '@/utils/util'
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
})

const handleStartingAgeUpdate = (value: number | null) => {
  startingAge.value = value
}

const handleFinishingAgeUpdate = (value: number | null) => {
  finishingAge.value = value
}

const handleAgeIncrementUpdate = (value: number | null) => {
  ageIncrement.value = value
}

const handleVolumeReportedUpdate = (value: string[]) => {
  volumeReported.value = [...value]
}

const handleIncludeInReportUpdate = (value: string[]) => {
  includeInReport.value = [...value]
}

const handleProjectionTypeUpdate = (value: string | null) => {
  projectionType.value = value
}

const handleReportTitleUpdate = (value: string | null) => {
  reportTitle.value = value
}

const runModelHandler = async () => {
  try {
    // validation - comparison
    const comparisonResult = fileUploadValidation.validateComparison(
      startingAge.value,
      finishingAge.value,
    )
    if (!comparisonResult.isValid) {
      messageDialog.value = {
        dialog: true,
        title: MESSAGE.MSG_DIALOG_TITLE.INVALID_INPUT,
        message: MESSAGE.MDL_PRM_INPUT_ERR.RPT_VLD_COMP_FNSH_AGE,
        btnLabel: CONSTANTS.BUTTON_LABEL.CONT_EDIT,
      }
      return
    }

    // validation - required fields
    const requiredFieldsResult = fileUploadValidation.validateRequiredFields(
      startingAge.value,
      finishingAge.value,
      ageIncrement.value,
    )
    if (!requiredFieldsResult.isValid) {
      messageDialog.value = {
        dialog: true,
        title: MESSAGE.MSG_DIALOG_TITLE.INVALID_INPUT,
        message: MESSAGE.FILE_UPLOAD_ERR.RPT_VLD_REQUIRED_FIELDS,
        btnLabel: CONSTANTS.BUTTON_LABEL.CONT_EDIT,
      }
      return
    }

    // validation - range
    const rangeResult = fileUploadValidation.validateRange(
      startingAge.value,
      finishingAge.value,
      ageIncrement.value,
    )
    if (!rangeResult.isValid) {
      let message = ''
      switch (rangeResult.errorType) {
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
      return
    }

    // validation - files
    const filesResult = await fileUploadValidation.validateFiles(
      layerFile.value,
      polygonFile.value,
    )
    if (!filesResult.isValid) {
      let message = ''
      switch (filesResult.errorType) {
        case 'layerFileMissing':
          message = MESSAGE.FILE_UPLOAD_ERR.LAYER_FILE_MISSING
          break
        case 'polygonFileMissing':
          message = MESSAGE.FILE_UPLOAD_ERR.POLYGON_FILE_MISSING
          break
        case 'layerFileNotCSVFormat':
          message = MESSAGE.FILE_UPLOAD_ERR.LAYER_FILE_NOT_CSV_FORMAT
          break
        case 'polygonFileNotCSVFormat':
          message = MESSAGE.FILE_UPLOAD_ERR.POLYGON_FILE_NOT_CSV_FORMAT
          break
      }

      messageDialog.value = {
        dialog: true,
        title: MESSAGE.MSG_DIALOG_TITLE.INVALID_FILE,
        message: message,
        btnLabel: CONSTANTS.BUTTON_LABEL.CONT_EDIT,
      }
      return
    }

    if (form.value) {
      form.value.validate()
    } else {
      console.warn('Form reference is null. Validation skipped.')
    }

    isProgressVisible.value = true
    progressMessage.value = MESSAGE.PROGRESS_MSG.RUNNING_MODEL

    await Util.delay(1000)

    const formData = new FormData()

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

    const selectedDebugOptions: Array<SelectedDebugOptionsEnum> = [
      SelectedDebugOptionsEnum.DoIncludeDebugTimestamps,
      SelectedDebugOptionsEnum.DoIncludeDebugEntryExit,
      SelectedDebugOptionsEnum.DoIncludeDebugIndentBlocks,
      SelectedDebugOptionsEnum.DoIncludeDebugRoutineNames,
    ]

    const projectionParameters = {
      ageStart: startingAge.value,
      ageEnd: finishingAge.value,
      ageIncrement: ageIncrement.value,
      outputFormat: OutputFormatEnum.CSVYieldTable,
      selectedExecutionOptions: selectedExecutionOptions,
      selectedDebugOptions: selectedDebugOptions,
      combineAgeYearRange: CombineAgeYearRangeEnum.Intersect,
      metadataToOutput: MetadataToOutputEnum.VERSION,
    }

    formData.append(
      'projectionParameters',
      new Blob([JSON.stringify(projectionParameters)], {
        type: 'application/json',
      }),
    )
    formData.append('polygonInputData', polygonFile.value as Blob)
    formData.append('layersInputData', layerFile.value as Blob)

    const result = await projectionHcsvPost(formData, false)

    const url = window.URL.createObjectURL(new Blob([result]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', CONSTANTS.FILE_NAME.PROJECTION_RESULT_ZIP)
    document.body.appendChild(link)
    link.click()
    link.remove()

    logSuccessMessage(MESSAGE.SUCESS_MSG.FILE_UPLOAD_RUN_RESULT)
  } catch (error) {
    handleApiError(error, MESSAGE.FILE_UPLOAD_ERR.FAIL_RUN_MODEL)
  } finally {
    isProgressVisible.value = false
  }
}

const handleDialogClose = () => {}
</script>
<style scoped></style>
