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
              <span class="text-h6">Stand Density</span>
            </v-col>
          </v-row>
        </v-expansion-panel-title>
        <v-expansion-panel-text class="expansion-panel-text mt-n2">
          <div>
            <v-row>
              <v-col cols="3">
                <v-text-field
                  label="Percent Stockable Area"
                  type="number"
                  v-model="percentStockableArea"
                  max="100"
                  min="0"
                  step="5"
                  :rules="[validatePercentStockableArea]"
                  :error-messages="percentStockableAreaError"
                  placeholder="Select..."
                  persistent-placeholder
                  density="compact"
                  dense
                ></v-text-field
              ></v-col>
              <v-col class="col-space-3" />
              <v-col cols="3">
                <v-text-field
                  type="number"
                  v-model="basalArea"
                  min="0"
                  step="0.0001"
                  :rules="[validateMinimum]"
                  :error-messages="basalAreaError"
                  persistent-placeholder
                  placeholder="N/A"
                  density="compact"
                  dense
                  :disabled="isBasalAreaDisabled"
                >
                  <template v-slot:label>
                    Basal Area (m<sup>2</sup>/ha)
                  </template>
                </v-text-field></v-col
              >
            </v-row>
            <v-row>
              <v-col cols="3">
                <v-text-field
                  label="Trees per Hectare"
                  type="number"
                  v-model="treesPerHectare"
                  min="0"
                  step="0.01"
                  :rules="[validateMinimum]"
                  :error-messages="treesPerHectareError"
                  persistent-placeholder
                  placeholder="N/A"
                  density="compact"
                  dense
                  :disabled="isTreesPerHectareDisabled"
                ></v-text-field>
              </v-col>
              <v-col class="col-space-3" />
              <v-col cols="3">
                <v-select
                  label="Minimum DBH Limit"
                  :items="minimumDBHLimitsOptions"
                  v-model="minimumDBHLimit"
                  item-title="label"
                  item-value="value"
                  clearable
                  hide-details
                  persistent-placeholder
                  placeholder="Select..."
                  density="compact"
                  dense
                  disabled
                ></v-select>
              </v-col>
            </v-row>
            <v-row>
              <v-col cols="3">
                <v-text-field
                  label="Percent Crown Closure"
                  type="number"
                  v-model="percentCrownClosure"
                  max="100"
                  min="0"
                  step="0.1"
                  :rules="[validatePercentCrownClosure]"
                  :error-messages="percentCrownClosureError"
                  persistent-placeholder
                  placeholder="N/A"
                  density="compact"
                  dense
                  :disabled="isPercentCrownClosureDisabled"
                ></v-text-field>
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
import { minimumDBHLimitsOptions } from '@/constants/options'
import { DERIVED_BY, SITE_SPECIES_VALUES } from '@/constants/constants'

const panelOpen = ref(0)

const modelParameterStore = useModelParameterStore()
const {
  derivedBy,
  siteSpeciesValues,
  percentStockableArea,
  basalArea,
  treesPerHectare,
  minimumDBHLimit,
  percentCrownClosure,
} = storeToRefs(modelParameterStore)

const isPercentCrownClosureDisabled = ref(false)
const isBasalAreaDisabled = ref(false)
const isTreesPerHectareDisabled = ref(false)

const updatePercentCrownClosureState = (
  newDerivedBy: string | null,
  newSiteSpeciesValues: string | null,
) => {
  isPercentCrownClosureDisabled.value = !(
    newDerivedBy === DERIVED_BY.VOLUME &&
    newSiteSpeciesValues === SITE_SPECIES_VALUES.COMPUTED
  )
}

const updateBasalAreaAndTreesState = (
  newDerivedBy: string | null,
  newSiteSpeciesValues: string | null,
) => {
  const isBasalAreaEnabled =
    newDerivedBy === DERIVED_BY.BASAL_AREA &&
    newSiteSpeciesValues === SITE_SPECIES_VALUES.COMPUTED

  isBasalAreaDisabled.value = !isBasalAreaEnabled
  isTreesPerHectareDisabled.value = !isBasalAreaEnabled
}

watch(
  [derivedBy, siteSpeciesValues],
  ([newDerivedBy, newSiteSpeciesValues]) => {
    updatePercentCrownClosureState(newDerivedBy, newSiteSpeciesValues)
    updateBasalAreaAndTreesState(newDerivedBy, newSiteSpeciesValues)
  },
  { immediate: true },
)

const validatePercentStockableArea = (value: any) => {
  if (value === null || value === '') {
    return 'Percent Stockable Area is required'
  }
  if (value < 0 || value > 100) {
    return 'Please enter a value between 0 and 100'
  }
  return true
}

const validatePercentCrownClosure = (value: any) => {
  if (value === null || value === '') {
    return true
  }
  if (value < 0 || value > 100) {
    return 'Please enter a value between 0 and 100'
  }
  return true
}

const validateMinimum = (value: any) => {
  if (value === null || value === '') {
    return true
  }
  if (value < 0) {
    return 'Please enter a value greater than 0'
  }
  return true
}

const percentStockableAreaError = computed(() => {
  const error = validatePercentStockableArea(percentStockableArea.value)
  return error === true ? [] : [error]
})

const percentCrownClosureError = computed(() => {
  const error = validatePercentCrownClosure(percentCrownClosure.value)
  return error === true ? [] : [error]
})

const basalAreaError = computed(() => {
  const error = validateMinimum(basalArea.value)
  return error === true ? [] : [error]
})

const treesPerHectareError = computed(() => {
  const error = validateMinimum(treesPerHectare.value)
  return error === true ? [] : [error]
})

const clear = () => {}
const confirm = () => {}
</script>

<style scoped></style>
