<template>
  <v-card class="elevation-4">
    <AppMessageDialog
      :dialog="messageDialog.dialog"
      :title="messageDialog.title"
      :message="messageDialog.message"
      :dialogWidth="messageDialog.dialogWidth"
      :btnLabel="messageDialog.btnLabel"
      @update:dialog="(value) => (messageDialog.dialog = value)"
      @close="handleDialogClose"
    />
    <v-expansion-panels v-model="panelOpenStates.siteInfo">
      <v-expansion-panel hide-actions>
        <v-expansion-panel-title>
          <v-row no-gutters class="expander-header">
            <v-col cols="auto" class="expansion-panel-icon-col">
              <v-icon class="expansion-panel-icon">{{
                panelOpenStates.siteInfo === CONSTANTS.PANEL.OPEN
                  ? 'mdi-chevron-up'
                  : 'mdi-chevron-down'
              }}</v-icon>
            </v-col>
            <v-col>
              <span class="text-h6">Site Information</span>
            </v-col>
          </v-row>
        </v-expansion-panel-title>
        <v-expansion-panel-text class="expansion-panel-text mt-n2">
          <v-form ref="form">
            <div>
              <v-row>
                <v-col cols="6">
                  <v-row class="mb-2">
                    <v-col cols="6">
                      <v-select
                        label="BEC Zone"
                        :items="OPTIONS.becZoneOptions"
                        v-model="becZone"
                        item-title="label"
                        item-value="value"
                        hide-details="auto"
                        persistent-placeholder
                        placeholder="Select Bec Zone"
                        density="compact"
                        dense
                        :disabled="!isConfirmEnabled"
                      ></v-select>
                    </v-col>
                    <v-col class="col-space-6" />
                    <v-col>
                      <v-select
                        label="Eco Zone"
                        :items="OPTIONS.ecoZoneOptions"
                        v-model="ecoZone"
                        item-title="label"
                        item-value="value"
                        clearable
                        hide-details="auto"
                        persistent-placeholder
                        placeholder="Select Eco Zone"
                        density="compact"
                        dense
                        :disabled="!isConfirmEnabled"
                      ></v-select>
                    </v-col>
                  </v-row>
                </v-col>
                <v-col class="col-space-6" />
                <v-col>
                  <v-row>
                    <v-col cols="12">
                      <v-checkbox
                        label="Include Secondary Dominant Height in Yield Table"
                        v-model="incSecondaryHeight"
                        hide-details="auto"
                        :disabled="
                          isIncSecondaryHeightDisabled || !isConfirmEnabled
                        "
                      ></v-checkbox>
                    </v-col>
                  </v-row>
                </v-col>
              </v-row>
              <div class="hr-line"></div>
              <v-row class="mt-7">
                <v-col cols="6">
                  <v-row class="mb-2">
                    <v-col cols="6">
                      <v-select
                        label="Site Species"
                        :items="siteSpeciesOptions"
                        v-model="selectedSiteSpecies"
                        item-title="label"
                        item-value="value"
                        hide-details="auto"
                        persistent-placeholder
                        placeholder="Select..."
                        density="compact"
                        dense
                        data-testid="selected-site-species"
                        :disabled="
                          isSelectedSiteSpeciesDisabled || !isConfirmEnabled
                        "
                      ></v-select>
                    </v-col>
                  </v-row>
                </v-col>
              </v-row>
              <div class="hr-line"></div>
              <v-row
                class="mt-1"
                style="display: inline-flex; align-items: center"
              >
                <v-col cols="auto" style="margin-bottom: 20px">
                  <div class="mt-2">Site Species Values:</div>
                </v-col>
                <v-col cols="auto">
                  <v-radio-group
                    v-model="siteSpeciesValues"
                    inline
                    :disabled="isSiteSpeciesValueDisabled || !isConfirmEnabled"
                  >
                    <v-radio
                      v-for="option in OPTIONS.siteSpeciesValuesOptions"
                      :key="option.value"
                      :label="option.label"
                      :value="option.value"
                    ></v-radio>
                  </v-radio-group>
                </v-col>
              </v-row>
              <v-row>
                <v-col cols="6">
                  <v-row style="height: 70px !important">
                    <v-col cols="6">
                      <AppSpinField
                        label="BHA 50 Site Index"
                        :model-value="bha50SiteIndex"
                        :max="CONSTANTS.NUM_INPUT_LIMITS.BHA50_SITE_INDEX_MAX"
                        :min="CONSTANTS.NUM_INPUT_LIMITS.BHA50_SITE_INDEX_MIN"
                        :step="CONSTANTS.NUM_INPUT_LIMITS.BHA50_SITE_INDEX_STEP"
                        :persistent-placeholder="true"
                        placeholder=""
                        :hideDetails="true"
                        density="compact"
                        :dense="true"
                        customStyle="padding-left: 15px"
                        variant="plain"
                        :disabled="
                          isBHA50SiteIndexDisabled || !isConfirmEnabled
                        "
                        :interval="CONSTANTS.CONTINUOUS_INC_DEC.INTERVAL"
                        :decimalAllowNumber="
                          CONSTANTS.NUM_INPUT_LIMITS
                            .BHA50_SITE_INDEX_DECIMAL_NUM
                        "
                        data-testid="bha-50-site-index"
                        @update:modelValue="handleBha50SiteIndexUpdate"
                      />
                      <v-label
                        v-show="Util.isZeroValue(bha50SiteIndex)"
                        style="font-size: 12px"
                        >{{
                          MESSAGE.MDL_PRM_INPUT_HINT.SITE_ZERO_NOT_KNOW
                        }}</v-label
                      >
                    </v-col>
                  </v-row>
                </v-col>
              </v-row>
            </div>
            <AppPanelActions
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
import { ref, computed, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { useModelParameterStore } from '@/stores/modelParameterStore'
import { AppMessageDialog, AppPanelActions, AppSpinField } from '@/components'
import type { SpeciesGroup, MessageDialog } from '@/interfaces/interfaces'
import { CONSTANTS, OPTIONS, DEFAULTS, MESSAGE } from '@/constants'
import { siteInfoValidation } from '@/validation'
import { Util } from '@/utils/util'

const form = ref<HTMLFormElement>()

const modelParameterStore = useModelParameterStore()

const messageDialog = ref<MessageDialog>({
  dialog: false,
  title: '',
  message: '',
})

const {
  panelOpenStates,
  derivedBy,
  speciesGroups,
  highestPercentSpecies,
  selectedSiteSpecies,
  becZone,
  ecoZone,
  incSecondaryHeight,
  siteSpeciesValues,
  bha50SiteIndex,
} = storeToRefs(modelParameterStore)

const panelName = CONSTANTS.MODEL_PARAMETER_PANEL.SITE_INFO
const isConfirmEnabled = computed(
  () => modelParameterStore.panelState[panelName].editable,
)
const isConfirmed = computed(
  () => modelParameterStore.panelState[panelName].confirmed,
)

const siteSpeciesOptions = computed(() =>
  speciesGroups.value.map((group: SpeciesGroup) => ({
    label: group.siteSpecies,
    value: group.siteSpecies,
  })),
)

const isIncSecondaryHeightDisabled = ref(false)
const isSelectedSiteSpeciesDisabled = ref(false)
const isSiteSpeciesValueDisabled = ref(false)
const isBHA50SiteIndexDisabled = ref(false)

const handleDerivedByChange = (
  newDerivedBy: string | null,
  newSiteSpecies: string | null,
) => {
  if (newDerivedBy === CONSTANTS.DERIVED_BY.VOLUME) {
    incSecondaryHeight.value = false
    isIncSecondaryHeightDisabled.value = true
    isSelectedSiteSpeciesDisabled.value = true
  } else if (newDerivedBy === CONSTANTS.DERIVED_BY.BASAL_AREA) {
    isIncSecondaryHeightDisabled.value = false
    isSelectedSiteSpeciesDisabled.value = false
    isSiteSpeciesValueDisabled.value =
      newSiteSpecies !== highestPercentSpecies.value
  }
}

watch(
  [derivedBy, selectedSiteSpecies, siteSpeciesValues],
  ([newDerivedBy, newSiteSpecies]) => {
    handleDerivedByChange(newDerivedBy, newSiteSpecies)
  },
  { immediate: true },
)

const handleBha50SiteIndexUpdate = (value: string | null) => {
  bha50SiteIndex.value = value
}

const formattingValues = (): void => {
  if (bha50SiteIndex.value) {
    bha50SiteIndex.value = parseFloat(bha50SiteIndex.value).toFixed(
      CONSTANTS.NUM_INPUT_LIMITS.BHA50_SITE_INDEX_DECIMAL_NUM,
    )
  }
}

const onConfirm = () => {
  // validation - required fields
  const requiredResult = siteInfoValidation.validateRequiredFields(
    bha50SiteIndex.value,
  )
  if (!requiredResult.isValid) {
    messageDialog.value = {
      dialog: true,
      title: MESSAGE.MSG_DIALOG_TITLE.MISSING_INFO,
      message: MESSAGE.MDL_PRM_INPUT_ERR.SITE_VLD_SPCZ_REQ_SI_VAL(
        selectedSiteSpecies.value,
      ),
      btnLabel: CONSTANTS.BUTTON_LABEL.CONT_EDIT,
    }
    return
  }

  // validation - range
  const rangeResult = siteInfoValidation.validateRange(bha50SiteIndex.value)
  if (!rangeResult.isValid) {
    messageDialog.value = {
      dialog: true,
      title: MESSAGE.MSG_DIALOG_TITLE.INVALID_INPUT,
      message: MESSAGE.MDL_PRM_INPUT_ERR.SITE_VLD_SI_RNG,
      btnLabel: CONSTANTS.BUTTON_LABEL.CONT_EDIT,
    }
    return
  }

  if (form.value) {
    form.value.validate()
  } else {
    console.warn('Form reference is null. Validation skipped.')
  }

  formattingValues()

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
  if (form.value) {
    form.value.reset()
  }

  selectedSiteSpecies.value = highestPercentSpecies.value

  becZone.value = DEFAULTS.DEFAULT_VALUES.BEC_ZONE
  siteSpeciesValues.value = DEFAULTS.DEFAULT_VALUES.SITE_SPECIES_VALUES

  handleDerivedByChange(derivedBy.value, selectedSiteSpecies.value)
}

const handleDialogClose = () => {}
</script>

<style scoped></style>
