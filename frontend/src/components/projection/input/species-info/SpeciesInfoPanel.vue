<template>
  <v-card class="elevation-4">
    <AppMessageDialog
      :dialog="messageDialog.dialog"
      :title="messageDialog.title"
      :message="messageDialog.message"
      :dialogWidth="messageDialog.dialogWidth"
      :btnLabel="messageDialog.btnLabel"
      :variant="messageDialog.variant"
      @update:dialog="(value) => (messageDialog.dialog = value)"
      @close="handleDialogClose"
    />
    <v-expansion-panels v-model="panelOpenStates.speciesInfo">
      <v-expansion-panel hide-actions>
        <v-expansion-panel-title>
          <v-row no-gutters class="expander-header">
            <v-col cols="auto" class="expansion-panel-icon-col">
              <v-icon class="expansion-panel-icon">
                {{
                  panelOpenStates.speciesInfo === CONSTANTS.PANEL.OPEN
                    ? 'mdi-chevron-up'
                    : 'mdi-chevron-down'
                }}
              </v-icon>
            </v-col>
            <v-col>
              <span class="text-h6">Species Information</span>
            </v-col>
          </v-row>
        </v-expansion-panel-title>
        <v-expansion-panel-text class="expansion-panel-text mt-n4">
          <v-form ref="form">
            <div class="mt-1">
              <v-row style="display: inline-flex; align-items: center">
                <v-col cols="auto" class="species-percent-derived-by-container">
                  <div>
                    <label class="bcds-radio-label" for="derivedBy">Species % derived by:</label>
                    <v-radio-group
                      id="derivedBy"
                      v-model="derivedBy"
                      inline
                      :disabled="isInputDisabled"
                    >
                      <v-radio
                        v-for="option in OPTIONS.derivedByOptions"
                        :key="option.value"
                        :label="option.label"
                        :value="option.value"
                      ></v-radio>
                    </v-radio-group>
                  </div>
                </v-col>
              </v-row>
            </div>
            <div class="mt-n3">
              <v-row>
                <v-col cols="5">
                  <SpeciesListInput
                    :speciesList="speciesList"
                    :computedSpeciesOptions="computedSpeciesOptions"
                    :isConfirmEnabled="!isInputDisabled"
                    :max="CONSTANTS.NUM_INPUT_LIMITS.SPECIES_PERCENT_MAX"
                    :min="CONSTANTS.NUM_INPUT_LIMITS.SPECIES_PERCENT_MIN"
                    :step="CONSTANTS.NUM_INPUT_LIMITS.SPECIES_PERCENT_STEP"
                    @update:speciesList="
                      (updatedList: SpeciesList[]) =>
                        handleSpeciesListUpdate(updatedList)
                    "
                  />
                </v-col>
                <v-col class="vertical-line pb-0" />
                <SpeciesGroupsDisplay :speciesGroups="speciesGroups" />
              </v-row>
            </div>
            <div>
              <v-row>
                <v-col cols="5" class="total-species-percent-container">
                  <div>
                    <v-row>
                      <v-col cols="6"></v-col>
                      <v-col cols="6">
                        <label class="bcds-text-field-label" for="totalSpeciesPercent">Total Species Percent</label>
                        <v-text-field
                          id="totalSpeciesPercent"
                          label="Total Species Percent"
                          :model-value="totalSpeciesPercent"
                          variant="underlined"
                          disabled
                          data-testid="total-species-percent"
                        ></v-text-field>
                      </v-col>
                    </v-row>
                  </div>
                </v-col>
                <v-col class="vertical-line" />
                <v-col cols="6" />
              </v-row>
            </div>
            <ActionPanel
              v-if="!isReadOnly"
              :isConfirmEnabled="isConfirmEnabled"
              :isConfirmed="isConfirmed"
              @clear="onClear"
              @confirm="onConfirm"
              @edit="onEdit"
            />
          </v-form>
        </v-expansion-panel-text>
      </v-expansion-panel>
    </v-expansion-panels>
  </v-card>
</template>
<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { storeToRefs } from 'pinia'
import { useModelParameterStore } from '@/stores/projection/modelParameterStore'
import { useAppStore } from '@/stores/projection/appStore'
import {
  AppMessageDialog,
} from '@/components'
import {
  ActionPanel,
  SpeciesListInput,
  SpeciesGroupsDisplay,
} from '@/components/projection'
import {
  BIZCONSTANTS,
  CONSTANTS,
  MESSAGE,
  OPTIONS,
} from '@/constants'
import { PROJECTION_ERR } from '@/constants/message'
import type { SpeciesList, MessageDialog } from '@/interfaces/interfaces'
import { speciesInfoValidation } from '@/validation'
import { cloneDeep } from 'lodash'
import { saveProjectionOnPanelConfirm } from '@/services/projection/modelParameterService'
import { useNotificationStore } from '@/stores/common/notificationStore'

const form = ref<HTMLFormElement>()

const modelParameterStore = useModelParameterStore()
const appStore = useAppStore()
const notificationStore = useNotificationStore()

// Check if we're in read-only (view) mode
const isReadOnly = computed(() => appStore.isReadOnly)

const messageDialog = ref<MessageDialog>({
  dialog: false,
  title: '',
  message: '',
})

const {
  panelOpenStates,
  derivedBy,
  speciesList,
  speciesGroups,
  totalSpeciesPercent,
  totalSpeciesGroupPercent,
} = storeToRefs(modelParameterStore)

const panelName = CONSTANTS.MODEL_PARAMETER_PANEL.SPECIES_INFO
const isConfirmEnabled = computed(
  () => !isReadOnly.value && modelParameterStore.panelState[panelName].editable,
)
const isConfirmed = computed(
  () => modelParameterStore.panelState[panelName].confirmed,
)

// Determine if inputs should be disabled (read-only mode or not editable)
const isInputDisabled = computed(
  () => isReadOnly.value || !modelParameterStore.panelState[panelName].editable,
)

const computedSpeciesOptions = computed(() =>
  (
    Object.keys(BIZCONSTANTS.SPECIES_MAP) as Array<
      keyof typeof BIZCONSTANTS.SPECIES_MAP
    >
  ).map((code) => ({
    label: `${code} - ${BIZCONSTANTS.SPECIES_MAP[code]}`,
    value: code,
  })),
)

const updateSpeciesGroup = modelParameterStore.updateSpeciesGroup

watch(
  speciesList,
  () => {
    updateSpeciesGroup()
  },
  { deep: true },
)

const handleSpeciesListUpdate = (updatedList: SpeciesList[]) => {
  const isDifferent = updatedList.some((item, index) => {
    return (
      item.species !== speciesList.value[index]?.species ||
      item.percent !== speciesList.value[index]?.percent
    )
  })

  // Update speciesList only if there are differences
  if (isDifferent) {
    speciesList.value = cloneDeep(updatedList)
  }
}

const onConfirm = async () => {
  // validation - duplicate
  const duplicateSpeciesResult = speciesInfoValidation.validateDuplicateSpecies(
    speciesList.value,
  )
  if (!duplicateSpeciesResult.isValid) {
    const duplicateSpecies =
      duplicateSpeciesResult.duplicateSpecies as keyof typeof BIZCONSTANTS.SPECIES_MAP
    const speciesLabel = (
      Object.keys(BIZCONSTANTS.SPECIES_MAP) as Array<
        keyof typeof BIZCONSTANTS.SPECIES_MAP
      >
    ).includes(duplicateSpeciesResult.duplicateSpecies as keyof typeof BIZCONSTANTS.SPECIES_MAP)
      ? BIZCONSTANTS.SPECIES_MAP[duplicateSpecies]
      : ''

    const message = speciesLabel
      ? MESSAGE.MDL_PRM_INPUT_ERR.SPCZ_VLD_DUP_W_LABEL(
          duplicateSpecies,
          speciesLabel,
        )
      : MESSAGE.MDL_PRM_INPUT_ERR.SPCZ_VLD_DUP_WO_LABEL(duplicateSpecies)

    messageDialog.value = {
      dialog: true,
      title: MESSAGE.MSG_DIALOG_TITLE.DATA_DUPLICATED,
      message: message,
      btnLabel: CONSTANTS.BUTTON_LABEL.CONT_EDIT,
      variant: 'error',
    }
    return
  }

  // validation - total percent
  const totalPercentResult = speciesInfoValidation.validateTotalSpeciesPercent(
    totalSpeciesPercent.value,
    totalSpeciesGroupPercent.value,
  )
  if (!totalPercentResult.isValid) {
    messageDialog.value = {
      dialog: true,
      title: MESSAGE.MSG_DIALOG_TITLE.DATA_INCOMPLETE,
      message: MESSAGE.MDL_PRM_INPUT_ERR.SPCZ_VLD_TOTAL_PCT,
      btnLabel: CONSTANTS.BUTTON_LABEL.CONT_EDIT,
      variant: 'error',
    }
    return
  }

  // validation - required fields
  const requiredResult = speciesInfoValidation.validateRequired(derivedBy.value)
  if (!requiredResult.isValid) {
    messageDialog.value = {
      dialog: true,
      title: MESSAGE.MSG_DIALOG_TITLE.MISSING_INFO,
      message: MESSAGE.MDL_PRM_INPUT_ERR.SPCZ_VLD_MISSING_DERIVED_BY,
      btnLabel: CONSTANTS.BUTTON_LABEL.CONT_EDIT,
      variant: 'error',
    }
    return
  }

  if (form.value) {
    form.value.validate()
  } else {
    console.warn('Form reference is null. Validation skipped.')
  }

  // Save projection (create or update) before confirming the panel
  try {
    await saveProjectionOnPanelConfirm(modelParameterStore, panelName)
  } catch (error) {
    console.error('Error saving projection:', error)
    notificationStore.showErrorMessage(PROJECTION_ERR.SAVE_FAILED, 'Projection Save Failed')
    return
  }

  // this panel is not in a confirmed state
  if (!isConfirmed.value) {
    modelParameterStore.confirmPanel(panelName)
  }
}

const onEdit = () => {
  // this panel has already been confirmed.
  if (isConfirmed.value) {
    modelParameterStore.editPanel(panelName)
  }
}

const onClear = () => {
  for (const item of speciesList.value) {
    item.species = null
    item.percent = null
  }
  derivedBy.value = null
}

const handleDialogClose = () => {}
</script>

<style scoped>
.vertical-line {
  display: flex;
  align-items: center;
  justify-content: center;
  max-width: 1px;
}

.vertical-line::before {
  content: '';
  display: block;
  border-left: 1px dashed rgba(0, 0, 0, 0.12);
  height: 100%;
}

.total-species-percent-container {
  padding-top: 6px;
}

.species-percent-derived-by-container {
  padding-bottom: 0px;
}

</style>
