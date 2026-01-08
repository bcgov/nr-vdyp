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
                      <label class="bcds-select-label" for="bec-zone-select">BEC Zone</label>
                      <v-select
                        id="bec-zone-select"
                        :items="OPTIONS.becZoneOptions"
                        v-model="becZone"
                        item-title="label"
                        item-value="value"
                        hide-details="auto"
                        persistent-placeholder
                        placeholder="Select Bec Zone"
                        :disabled="!isConfirmEnabled"
                        append-inner-icon="mdi-chevron-down"
                      ></v-select>
                    </v-col>
                    <v-col class="col-space-6" />
                    <v-col>
                      <label class="bcds-select-label" for="eco-zone-select">Eco Zone</label>
                      <v-select
                        id="eco-zone-select"
                        :items="OPTIONS.ecoZoneOptions"
                        v-model="ecoZone"
                        item-title="label"
                        item-value="value"
                        clearable
                        hide-details="auto"
                        persistent-placeholder
                        placeholder="Select Eco Zone"
                        :disabled="!isConfirmEnabled"
                        append-inner-icon="mdi-chevron-down"
                      ></v-select>
                    </v-col>
                  </v-row>
                </v-col>
              </v-row>
              <div class="hr-line"></div>
              <v-row class="mt-7">
                <v-col cols="6">
                  <v-row class="mb-2">
                    <v-col cols="6">
                      <label class="bcds-select-label" for="site-species-select">Site Species</label>
                      <v-select
                        id="site-species-select"
                        :items="siteSpeciesOptions"
                        v-model="selectedSiteSpecies"
                        item-title="label"
                        item-value="value"
                        hide-details="auto"
                        persistent-placeholder
                        placeholder="Select..."
                        data-testid="selected-site-species"
                        disabled
                        append-inner-icon="mdi-chevron-down"
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
                  <div class="mt-2">Site Index:</div>
                </v-col>
                <v-col cols="auto">
                  <v-radio-group
                    v-model="siteSpeciesValues"
                    inline
                    hide-details
                    :disabled="isSiteSpeciesValueDisabled || !isConfirmEnabled"
                  >
                    <v-radio
                      style="width: 110px"
                      :key="OPTIONS.siteSpeciesValuesOptions[0].value"
                      :label="OPTIONS.siteSpeciesValuesOptions[0].label"
                      :value="OPTIONS.siteSpeciesValuesOptions[0].value"
                    ></v-radio>
                    <v-radio
                      class="pl-7"
                      :key="OPTIONS.siteSpeciesValuesOptions[1].value"
                      :label="OPTIONS.siteSpeciesValuesOptions[1].label"
                      :value="OPTIONS.siteSpeciesValuesOptions[1].value"
                    ></v-radio>
                  </v-radio-group>
                </v-col>
              </v-row>
              <v-row class="mt-n5">
                <v-col cols="auto" style="margin-bottom: 20px">
                  <div class="mt-2">Age Years:</div>
                </v-col>
                <v-col cols="auto">
                  <v-radio-group
                    v-model="ageType"
                    inline
                    hide-details
                    :disabled="isAgeTypeDisabled || !isConfirmEnabled"
                  >
                    <v-radio
                      style="width: 110px"
                      :key="OPTIONS.ageTypeOptions[0].value"
                      :label="OPTIONS.ageTypeOptions[0].label"
                      :value="OPTIONS.ageTypeOptions[0].value"
                    ></v-radio>
                    <v-radio
                      class="pl-7"
                      :key="OPTIONS.ageTypeOptions[1].value"
                      :label="OPTIONS.ageTypeOptions[1].label"
                      :value="OPTIONS.ageTypeOptions[1].value"
                    ></v-radio>
                  </v-radio-group>
                </v-col>
              </v-row>
              <v-row>
                <v-col cols="6">
                  <v-row class="mb-2">
                    <v-col cols="6">
                      <v-text-field
                        label="Years"
                        type="number"
                        v-model.number="spzAge"
                        :max="CONSTANTS.NUM_INPUT_LIMITS.SPZ_AGE_MAX"
                        :min="CONSTANTS.NUM_INPUT_LIMITS.SPZ_AGE_MIN"
                        :step="CONSTANTS.NUM_INPUT_LIMITS.SPZ_AGE_STEP"
                        persistent-placeholder
                        :placeholder="spzAgePlaceholder"
                        hide-details
                        density="compact"
                        dense
                        :disabled="isSpzAgeDisabled || !isConfirmEnabled"
                        data-testid="spz-age"
                      ></v-text-field>
                      <v-label
                        v-show="isZeroValue(spzAge)"
                        style="font-size: 12px"
                        >{{
                          MESSAGE.MDL_PRM_INPUT_HINT.SITE_ZERO_NOT_KNOW
                        }}</v-label
                      >
                    </v-col>
                    <v-col class="col-space-6" />
                    <v-col>
                      <AppSpinField
                        label="Height in Meters"
                        :model-value="spzHeight"
                        :max="CONSTANTS.NUM_INPUT_LIMITS.SPZ_HEIGHT_MAX"
                        :min="CONSTANTS.NUM_INPUT_LIMITS.SPZ_HEIGHT_MIN"
                        :step="CONSTANTS.NUM_INPUT_LIMITS.SPZ_HEIGHT_STEP"
                        :persistent-placeholder="true"
                        :placeholder="spzHeightPlaceholder"
                        :hideDetails="true"
                        density="compact"
                        :dense="true"
                        customStyle="padding-left: 0px"
                        :disabled="isSpzHeightDisabled || !isConfirmEnabled"
                        :interval="CONSTANTS.CONTINUOUS_INC_DEC.INTERVAL"
                        :decimalAllowNumber="
                          CONSTANTS.NUM_INPUT_LIMITS.SPZ_HEIGHT_DECIMAL_NUM
                        "
                        data-testid="spz-height"
                        @update:modelValue="handleSpzHeightUpdate"
                      />
                      <v-label
                        v-show="isZeroValue(spzHeight)"
                        style="font-size: 12px"
                        >{{
                          MESSAGE.MDL_PRM_INPUT_HINT.SITE_ZERO_NOT_KNOW
                        }}</v-label
                      >
                    </v-col>
                  </v-row>
                </v-col>
                <v-col class="col-space-6" />
                <v-col>
                  <v-row>
                    <v-col cols="6">
                      <AppSpinField
                        label="BHA 50 Site Index"
                        :model-value="bha50SiteIndex"
                        :max="CONSTANTS.NUM_INPUT_LIMITS.BHA50_SITE_INDEX_MAX"
                        :min="CONSTANTS.NUM_INPUT_LIMITS.BHA50_SITE_INDEX_MIN"
                        :step="CONSTANTS.NUM_INPUT_LIMITS.BHA50_SITE_INDEX_STEP"
                        :persistent-placeholder="true"
                        :placeholder="bha50SiteIndexPlaceholder"
                        :hideDetails="true"
                        density="compact"
                        :dense="true"
                        customStyle="padding-left: 10px"
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
                        v-show="isZeroValue(bha50SiteIndex)"
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
import { isZeroValue } from '@/utils/util'

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
  siteSpeciesValues,
  ageType,
  spzAge,
  spzHeight,
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

const isSiteSpeciesValueDisabled = ref(false)
const isAgeTypeDisabled = ref(false)
const isSpzAgeDisabled = ref(false)
const isSpzHeightDisabled = ref(false)
const isBHA50SiteIndexDisabled = ref(false)

const spzAgePlaceholder = ref('')
const spzHeightPlaceholder = ref('')
const bha50SiteIndexPlaceholder = ref('')

const handleSiteSpeciesValuesState = (newSiteSpeciesValues: string | null) => {
  if (newSiteSpeciesValues === CONSTANTS.SITE_SPECIES_VALUES.COMPUTED) {
    isAgeTypeDisabled.value = false
    isSpzAgeDisabled.value = false
    isSpzHeightDisabled.value = false
    isBHA50SiteIndexDisabled.value = true

    spzAge.value = DEFAULTS.DEFAULT_VALUES.SPZ_AGE
    spzHeight.value = DEFAULTS.DEFAULT_VALUES.SPZ_HEIGHT
    bha50SiteIndex.value = ''

    spzAgePlaceholder.value = ''
    spzHeightPlaceholder.value = ''
    bha50SiteIndexPlaceholder.value = CONSTANTS.SPECIAL_INDICATORS.COMPUTED
  } else if (newSiteSpeciesValues === CONSTANTS.SITE_SPECIES_VALUES.SUPPLIED) {
    isAgeTypeDisabled.value = true
    isSpzAgeDisabled.value = true
    isSpzHeightDisabled.value = true
    isBHA50SiteIndexDisabled.value = false

    spzAge.value = null
    spzHeight.value = null
    bha50SiteIndex.value = DEFAULTS.DEFAULT_VALUES.BHA50_SITE_INDEX

    spzAgePlaceholder.value = CONSTANTS.SPECIAL_INDICATORS.NA
    spzHeightPlaceholder.value = CONSTANTS.SPECIAL_INDICATORS.NA
    bha50SiteIndexPlaceholder.value = ''
  }
}

const handleDerivedByChange = (
  newDerivedBy: string | null,
  newSiteSpecies: string | null,
  newSiteSpeciesValues: string | null,
) => {
  if (!newDerivedBy) return

  if (newDerivedBy === CONSTANTS.DERIVED_BY.BASAL_AREA) {
    isSiteSpeciesValueDisabled.value =
      newSiteSpecies !== highestPercentSpecies.value
  }
  handleSiteSpeciesValuesState(newSiteSpeciesValues)
}

watch(
  [derivedBy, selectedSiteSpecies, siteSpeciesValues],
  ([newDerivedBy, newSiteSpecies, newSiteSpeciesValues]) => {
    handleDerivedByChange(newDerivedBy, newSiteSpecies, newSiteSpeciesValues)
  },
  { immediate: true },
)

const handleSpzHeightUpdate = (value: string | null) => {
  spzHeight.value = value
}

const handleBha50SiteIndexUpdate = (value: string | null) => {
  bha50SiteIndex.value = value
}

const formattingValues = (): void => {
  if (bha50SiteIndex.value) {
    bha50SiteIndex.value = Number.parseFloat(bha50SiteIndex.value).toFixed(
      CONSTANTS.NUM_INPUT_LIMITS.BHA50_SITE_INDEX_DECIMAL_NUM,
    )
  }
}

const onConfirm = () => {
  // validation - required fields
  const requiredResult = siteInfoValidation.validateRequiredFields(
    siteSpeciesValues.value,
    spzAge.value,
    spzHeight.value,
    bha50SiteIndex.value,
  )
  if (!requiredResult.isValid) {
    messageDialog.value = {
      dialog: true,
      title: MESSAGE.MSG_DIALOG_TITLE.MISSING_INFO,
      message:
        siteSpeciesValues.value === CONSTANTS.SITE_SPECIES_VALUES.COMPUTED
          ? MESSAGE.MDL_PRM_INPUT_ERR.SITE_VLD_SPCZ_REQ_VALS_SUP(
              selectedSiteSpecies.value,
            )
          : MESSAGE.MDL_PRM_INPUT_ERR.SITE_VLD_SPCZ_REQ_SI_VAL(
              selectedSiteSpecies.value,
            ),
      btnLabel: CONSTANTS.BUTTON_LABEL.CONT_EDIT,
      variant: 'error',
    }
    return
  }

  // validation - range
  const rangeResult = siteInfoValidation.validateRange(
    spzAge.value,
    spzHeight.value,
    bha50SiteIndex.value,
  )
  if (!rangeResult.isValid) {
    let message = ''

    switch (rangeResult.errorType) {
      case 'spzAge':
        message = MESSAGE.MDL_PRM_INPUT_ERR.SITE_VLD_AGE_RNG
        break
      case 'spzHeight':
        message = MESSAGE.MDL_PRM_INPUT_ERR.SITE_VLD_HIGHT_RNG
        break
      case 'bha50SiteIndex':
        message = MESSAGE.MDL_PRM_INPUT_ERR.SITE_VLD_SI_RNG
        break
    }

    messageDialog.value = {
      dialog: true,
      title: MESSAGE.MSG_DIALOG_TITLE.INVALID_INPUT,
      message: message,
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
  becZone.value = DEFAULTS.DEFAULT_VALUES.BEC_ZONE
  ecoZone.value = null
  selectedSiteSpecies.value = highestPercentSpecies.value
  siteSpeciesValues.value = DEFAULTS.DEFAULT_VALUES.SITE_SPECIES_VALUES
  ageType.value = DEFAULTS.DEFAULT_VALUES.AGE_TYPE

  handleDerivedByChange(
    derivedBy.value,
    selectedSiteSpecies.value,
    siteSpeciesValues.value,
  )

  spzAge.value = DEFAULTS.DEFAULT_VALUES.SPZ_AGE
  spzHeight.value = DEFAULTS.DEFAULT_VALUES.SPZ_HEIGHT
  bha50SiteIndex.value = ''
  bha50SiteIndexPlaceholder.value = CONSTANTS.SPECIAL_INDICATORS.COMPUTED
}

const handleDialogClose = () => {}
</script>

<style scoped />
