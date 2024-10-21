<template>
  <v-card class="elevation-4">
    <v-expansion-panels v-model="panelOpen">
      <v-expansion-panel hide-actions>
        <v-expansion-panel-title>
          <v-row no-gutters class="expander-header">
            <!-- Place an arrow icon to the left of the title -->
            <v-col cols="auto" class="expansion-panel-icon-col">
              <v-icon class="expansion-panel-icon">{{
                panelOpen === 0 ? 'mdi-chevron-up' : 'mdi-chevron-down'
              }}</v-icon>
            </v-col>
            <v-col>
              <span class="text-h6">Additional Stand Attributes</span>
            </v-col>
          </v-row>
        </v-expansion-panel-title>
        <v-expansion-panel-text class="expansion-panel-text mt-n3">
          <div>
            <v-row>
              <v-col cols="auto">
                <v-radio-group
                  v-model="computedValues"
                  density="compact"
                  dense
                  :disabled="isComputedValuesDisabled"
                >
                  <v-radio
                    v-for="option in additionalStandAttributesOptions"
                    :key="option.value"
                    :label="option.label"
                    :value="option.value"
                  ></v-radio>
                </v-radio-group>
              </v-col>
            </v-row>
          </div>
          <div>
            <v-row>
              <v-col cols="3">
                <v-text-field
                  label="Lorey Height - 7.5cm+ (meters)"
                  type="number"
                  v-model="loreyHeight"
                  min="0"
                  step="0.01"
                  :rules="[validateMinimum]"
                  :error-messages="loreyHeightError"
                  persistent-placeholder
                  placeholder="N/A"
                  density="compact"
                  dense
                  :disabled="isLoreyHeightDisabled"
                ></v-text-field>
              </v-col>
              <v-col class="col-space-3" />
              <v-col cols="3">
                <v-text-field
                  type="number"
                  v-model="wholeStemVolume75cm"
                  min="0"
                  step="0.1"
                  :rules="[validateMinimum]"
                  :error-messages="wholeStemVolume75cmError"
                  persistent-placeholder
                  placeholder="N/A"
                  density="compact"
                  dense
                  :disabled="isWholeStemVolume75cmDisabled"
                >
                  <template v-slot:label>
                    Whole Stem Volume - 7.5cm+ (m<sup>3</sup>/ha)
                  </template>
                </v-text-field>
              </v-col>
            </v-row>
            <v-row>
              <v-col cols="3">
                <v-text-field
                  type="number"
                  v-model="basalArea125cm"
                  min="0"
                  step="0.0001"
                  :rules="[validateMinimum]"
                  :error-messages="basalArea125cmError"
                  persistent-placeholder
                  placeholder="N/A"
                  density="compact"
                  dense
                  :disabled="isBasalArea125cmDisabled"
                >
                  <template v-slot:label>
                    Basal Area - 12.5cm+ (m<sup>2</sup>/ha)
                  </template>
                </v-text-field>
              </v-col>
              <v-col class="col-space-3" />
              <v-col cols="3">
                <v-text-field
                  type="number"
                  v-model="wholeStemVolume125cm"
                  min="0"
                  step="0.1"
                  :rules="[validateMinimum]"
                  :error-messages="wholeStemVolume125cmError"
                  persistent-placeholder
                  placeholder="N/A"
                  density="compact"
                  dense
                  :disabled="isWholeStemVolume125cmDisabled"
                >
                  <template v-slot:label>
                    Whole Stem Volume - 12.5cm+ (m<sup>3</sup>/ha)
                  </template>
                </v-text-field>
              </v-col>
            </v-row>
            <v-row>
              <v-col cols="3">
                <v-text-field
                  type="number"
                  v-model="closeUtilVolume"
                  min="0"
                  step="0.1"
                  :rules="[validateMinimum]"
                  :error-messages="closeUtilVolumeError"
                  persistent-placeholder
                  placeholder="N/A"
                  density="compact"
                  dense
                  :disabled="isCloseUtilVolumeDisabled"
                >
                  <template v-slot:label>
                    Close Utilization Volume - 12.5cm+ (m<sup>3</sup>/ha)
                  </template>
                </v-text-field>
              </v-col>
              <v-col class="col-space-3" />
              <v-col cols="3">
                <v-text-field
                  type="number"
                  v-model="closeUtilNetDecayVolume"
                  min="0"
                  step="0.1"
                  :rules="[validateMinimum]"
                  :error-messages="closeUtilNetDecayVolumeError"
                  persistent-placeholder
                  placeholder="N/A"
                  density="compact"
                  dense
                  :disabled="isCloseUtilNetDecayVolumeDisabled"
                >
                  <template v-slot:label>
                    Close Utilization Net Decay Volume - 12.5cm+
                    (m<sup>3</sup>/ha)
                  </template>
                </v-text-field>
              </v-col>
            </v-row>
            <v-row>
              <v-col cols="3">
                <v-text-field
                  type="number"
                  v-model="closeUtilNetDecayWasteVolume"
                  min="0"
                  step="0.1"
                  :rules="[validateMinimum]"
                  :error-messages="closeUtilNetDecayWasteVolumeError"
                  persistent-placeholder
                  placeholder="N/A"
                  density="compact"
                  dense
                  :disabled="isCloseUtilNetDecayWasteVolumeDisabled"
                >
                  <template v-slot:label>
                    Close Utilization Net Decay Waste Volume - 12.5cm+
                    (m<sup>3</sup>/ha)
                  </template>
                </v-text-field>
              </v-col>
            </v-row>
          </div>
          <v-card-actions class="mt-5 pr-0">
            <v-spacer></v-spacer>
            <v-btn class="white-btn" @click="clear">Clear</v-btn>
            <v-btn class="blue-btn ml-2" @click="confirm">Confirm</v-btn>
          </v-card-actions>
        </v-expansion-panel-text>
      </v-expansion-panel>
    </v-expansion-panels>
  </v-card>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useModelParameterStore } from '@/stores/modelParameterStore'
import { storeToRefs } from 'pinia'
import { additionalStandAttributesOptions } from '@/constants/options'
import {
  DERIVED_BY,
  SITE_SPECIES_VALUES,
  COMPUTED_VALUES,
} from '@/constants/constants'

const panelOpen = ref(0)

const modelParameterStore = useModelParameterStore()
const {
  derivedBy,
  siteSpeciesValues,
  computedValues,
  loreyHeight,
  basalArea125cm,
  closeUtilVolume,
  closeUtilNetDecayWasteVolume,
  wholeStemVolume75cm,
  wholeStemVolume125cm,
  closeUtilNetDecayVolume,
} = storeToRefs(modelParameterStore)

const isComputedValuesDisabled = ref(false)
const isLoreyHeightDisabled = ref(false)
const isWholeStemVolume75cmDisabled = ref(false)
const isBasalArea125cmDisabled = ref(false)
const isWholeStemVolume125cmDisabled = ref(false)
const isCloseUtilVolumeDisabled = ref(false)
const isCloseUtilNetDecayVolumeDisabled = ref(false)
const isCloseUtilNetDecayWasteVolumeDisabled = ref(false)

const updateComputedValuesState = (
  newDerivedBy: string | null,
  newSiteSpeciesValues: string | null,
) => {
  isComputedValuesDisabled.value = !(
    newDerivedBy === DERIVED_BY.BASAL_AREA &&
    newSiteSpeciesValues === SITE_SPECIES_VALUES.COMPUTED
  )

  if (isComputedValuesDisabled.value) {
    computedValues.value = COMPUTED_VALUES.USE
  }
}

const updateFieldDisabledStates = (newComputedValues: string | null) => {
  const isDisabled = newComputedValues === COMPUTED_VALUES.USE

  isLoreyHeightDisabled.value = isDisabled
  isWholeStemVolume75cmDisabled.value = isDisabled
  isBasalArea125cmDisabled.value = isDisabled
  isWholeStemVolume125cmDisabled.value = isDisabled
  isCloseUtilVolumeDisabled.value = isDisabled
  isCloseUtilNetDecayVolumeDisabled.value = isDisabled
  isCloseUtilNetDecayWasteVolumeDisabled.value = isDisabled
}

watch(
  [derivedBy, siteSpeciesValues, computedValues],
  ([newDerivedBy, newSiteSpeciesValues, newComputedValues]) => {
    updateComputedValuesState(newDerivedBy, newSiteSpeciesValues)
    updateFieldDisabledStates(newComputedValues)
  },
  { immediate: true },
)

const validateMinimum = (value: any) => {
  if (value === null || value === '') {
    return true
  }
  if (value < 0) {
    return 'Please enter a value greater than 0'
  }
  return true
}

const loreyHeightError = computed(() => {
  const error = validateMinimum(loreyHeight.value)
  return error === true ? [] : [error]
})

const wholeStemVolume75cmError = computed(() => {
  const error = validateMinimum(wholeStemVolume75cm.value)
  return error === true ? [] : [error]
})

const basalArea125cmError = computed(() => {
  const error = validateMinimum(basalArea125cm.value)
  return error === true ? [] : [error]
})

const wholeStemVolume125cmError = computed(() => {
  const error = validateMinimum(wholeStemVolume125cm.value)
  return error === true ? [] : [error]
})

const closeUtilVolumeError = computed(() => {
  const error = validateMinimum(closeUtilVolume.value)
  return error === true ? [] : [error]
})

const closeUtilNetDecayVolumeError = computed(() => {
  const error = validateMinimum(closeUtilNetDecayVolume.value)
  return error === true ? [] : [error]
})

const closeUtilNetDecayWasteVolumeError = computed(() => {
  const error = validateMinimum(closeUtilNetDecayWasteVolume.value)
  return error === true ? [] : [error]
})

const clear = () => {}
const confirm = () => {}
</script>
<style scoped></style>
