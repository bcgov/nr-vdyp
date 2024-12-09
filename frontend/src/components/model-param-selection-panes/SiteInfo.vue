<template>
  <v-card class="elevation-4">
    <v-expansion-panels v-model="panelOpenStates.siteInfo">
      <v-expansion-panel hide-actions>
        <v-expansion-panel-title>
          <v-row no-gutters class="expander-header">
            <v-col cols="auto" class="expansion-panel-icon-col">
              <v-icon class="expansion-panel-icon">{{
                panelOpenStates.siteInfo === PANEL.OPEN
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
                        :items="becZoneOptions"
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
                        :items="ecoZoneOptions"
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
                      v-for="option in siteSpeciesValuesOptions"
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
                      <div style="position: relative; width: 100%">
                        <v-text-field
                          label="BHA 50 Site Index"
                          type="text"
                          v-model="bha50SiteIndex"
                          persistent-placeholder
                          placeholder=""
                          hide-details
                          density="compact"
                          dense
                          style="padding-left: 15px"
                          variant="plain"
                          :disabled="
                            isBHA50SiteIndexDisabled || !isConfirmEnabled
                          "
                        ></v-text-field>
                        <!-- spin buttons -->
                        <div class="spin-box">
                          <div
                            class="spin-up-arrow-button"
                            @mousedown="startIncrementBHA50SiteIndex"
                            @mouseup="stopIncrementBHA50SiteIndex"
                            @mouseleave="stopIncrementBHA50SiteIndex"
                            :class="{
                              disabled:
                                isBHA50SiteIndexDisabled || !isConfirmEnabled,
                            }"
                          >
                            {{ SPIN_BUTTON.UP }}
                          </div>
                          <div
                            class="spin-down-arrow-button"
                            @mousedown="startDecrementBHA50SiteIndex"
                            @mouseup="stopDecrementBHA50SiteIndex"
                            @mouseleave="stopDecrementBHA50SiteIndex"
                            :class="{
                              disabled:
                                isBHA50SiteIndexDisabled || !isConfirmEnabled,
                            }"
                          >
                            {{ SPIN_BUTTON.DOWN }}
                          </div>
                        </div>
                        <div class="spin-text-field-bottom-line"></div>
                      </div>
                      <v-label
                        v-show="Util.isZeroValue(bha50SiteIndex)"
                        style="font-size: 12px"
                        >{{ MDL_PRM_INPUT_HINT.SITE_ZERO_NOT_KNOW }}</v-label
                      >
                    </v-col>
                  </v-row>
                </v-col>
              </v-row>
            </div>
            <v-card-actions class="mt-5 pr-0">
              <v-spacer></v-spacer>
              <v-btn
                class="white-btn"
                :disabled="!isConfirmEnabled"
                @click="clear"
                >Clear</v-btn
              >
              <v-btn
                v-show="!isConfirmed"
                class="blue-btn ml-2"
                :disabled="!isConfirmEnabled"
                @click="onConfirm"
                >Confirm</v-btn
              >
              <v-btn v-show="isConfirmed" class="blue-btn ml-2" @click="onEdit"
                >Edit</v-btn
              >
            </v-card-actions>
          </v-form>
        </v-expansion-panel-text>
      </v-expansion-panel>
    </v-expansion-panels>
  </v-card>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { Util } from '@/utils/util'
import { useModelParameterStore } from '@/stores/modelParameterStore'
import { useMessageDialogStore } from '@/stores/common/messageDialogStore'
import { storeToRefs } from 'pinia'
import {
  becZoneOptions,
  ecoZoneOptions,
  siteSpeciesValuesOptions,
} from '@/constants/options'
import {
  PANEL,
  DERIVED_BY,
  MODEL_PARAMETER_PANEL,
  NUM_INPUT_LIMITS,
  CONTINUOUS_INC_DEC,
  SPIN_BUTTON,
} from '@/constants/constants'
import { DEFAULT_VALUES } from '@/constants/defaults'
import {
  MDL_PRM_INPUT_ERR,
  MSG_DIALOG_TITLE,
  MDL_PRM_INPUT_HINT,
} from '@/constants/message'
import { SiteInfoValidation } from '@/validation/siteInfoValidation'

const form = ref<HTMLFormElement>()

const siteInfoValidator = new SiteInfoValidation()

const modelParameterStore = useModelParameterStore()
const messageDialogStore = useMessageDialogStore()

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

const panelName = MODEL_PARAMETER_PANEL.SITE_INFO
const isConfirmEnabled = computed(
  () => modelParameterStore.panelState[panelName].editable,
)
const isConfirmed = computed(
  () => modelParameterStore.panelState[panelName].confirmed,
)

const siteSpeciesOptions = computed(() =>
  speciesGroups.value.map((group) => ({
    label: group.siteSpecies,
    value: group.siteSpecies,
  })),
)

const isIncSecondaryHeightDisabled = ref(false)
const isSelectedSiteSpeciesDisabled = ref(false)
const isSiteSpeciesValueDisabled = ref(false)
const isBHA50SiteIndexDisabled = ref(false)

// Interval references for continuous increment/decrement
let bha50IncrementInterval: number | null = null
let bha50DecrementInterval: number | null = null

const handleDerivedByChange = (
  newDerivedBy: string | null,
  newSiteSpecies: string | null,
) => {
  if (newDerivedBy === DERIVED_BY.VOLUME) {
    incSecondaryHeight.value = false
    isIncSecondaryHeightDisabled.value = true
    isSelectedSiteSpeciesDisabled.value = true
  } else if (newDerivedBy === DERIVED_BY.BASAL_AREA) {
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

const incrementBHA50SiteIndex = () => {
  const newValue = Util.increaseItemBySpinButton(
    bha50SiteIndex.value,
    NUM_INPUT_LIMITS.BHA50_SITE_INDEX_MAX,
    NUM_INPUT_LIMITS.BHA50_SITE_INDEX_MIN,
    NUM_INPUT_LIMITS.BHA50_SITE_INDEX_STEP,
  )

  bha50SiteIndex.value = newValue.toFixed(
    NUM_INPUT_LIMITS.BHA50_SITE_INDEX_DECIMAL_NUM,
  )
}

const decrementBHA50SiteIndex = () => {
  let newValue = Util.decrementItemBySpinButton(
    bha50SiteIndex.value,
    NUM_INPUT_LIMITS.BHA50_SITE_INDEX_MAX,
    NUM_INPUT_LIMITS.BHA50_SITE_INDEX_MIN,
    NUM_INPUT_LIMITS.BHA50_SITE_INDEX_STEP,
  )

  bha50SiteIndex.value = newValue.toFixed(
    NUM_INPUT_LIMITS.BHA50_SITE_INDEX_DECIMAL_NUM,
  )
}

// Methods to handle continuous increment/decrement for BHA 50 Site Index
const startIncrementBHA50SiteIndex = () => {
  incrementBHA50SiteIndex()
  bha50IncrementInterval = window.setInterval(
    incrementBHA50SiteIndex,
    CONTINUOUS_INC_DEC.INTERVAL,
  )
}

const stopIncrementBHA50SiteIndex = () => {
  if (bha50IncrementInterval !== null) {
    clearInterval(bha50IncrementInterval)
    bha50IncrementInterval = null
  }
}

const startDecrementBHA50SiteIndex = () => {
  decrementBHA50SiteIndex()
  bha50DecrementInterval = window.setInterval(
    decrementBHA50SiteIndex,
    CONTINUOUS_INC_DEC.INTERVAL,
  )
}

const stopDecrementBHA50SiteIndex = () => {
  if (bha50DecrementInterval !== null) {
    clearInterval(bha50DecrementInterval)
    bha50DecrementInterval = null
  }
}

const validateRange = (): boolean => {
  if (!siteInfoValidator.validateBha50SiteIndexRange(bha50SiteIndex.value)) {
    messageDialogStore.openDialog(
      MSG_DIALOG_TITLE.INVALID_INPUT,
      MDL_PRM_INPUT_ERR.SITE_VLD_SI_RNG,
      { width: 400 },
    )
    return false
  }

  return true
}

const validateRequiredFields = (): boolean => {
  if (!siteInfoValidator.validateRequiredFields(bha50SiteIndex.value)) {
    messageDialogStore.openDialog(
      MSG_DIALOG_TITLE.MISSING_INFO,
      MDL_PRM_INPUT_ERR.SITE_VLD_SPCZ_REQ_SI_VAL(selectedSiteSpecies.value),
      { width: 400 },
    )
    return false
  }

  return true
}

const formattingValues = (): void => {
  if (bha50SiteIndex.value) {
    bha50SiteIndex.value = parseFloat(bha50SiteIndex.value).toFixed(
      NUM_INPUT_LIMITS.BHA50_SITE_INDEX_DECIMAL_NUM,
    )
  }
}

const onConfirm = () => {
  if (validateRequiredFields() && validateRange()) {
    if (form.value) {
      form.value.validate()
    }

    formattingValues()

    // this panel is not in a confirmed state
    if (!isConfirmed.value) {
      modelParameterStore.confirmPanel(panelName)
    }
  }
}

const onEdit = () => {
  // this panel has already been confirmed.
  if (isConfirmed.value) {
    modelParameterStore.editPanel(panelName)
  }
}

const clear = () => {
  if (form.value) {
    form.value.reset()
  }

  selectedSiteSpecies.value = highestPercentSpecies.value

  becZone.value = DEFAULT_VALUES.BEC_ZONE
  siteSpeciesValues.value = DEFAULT_VALUES.SITE_SPECIES_VALUES

  handleDerivedByChange(derivedBy.value, selectedSiteSpecies.value)
}
</script>

<style scoped></style>
