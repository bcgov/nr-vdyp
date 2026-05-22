<template>
  <div>
    <span class="text-subtitle-2">Site Indices</span>
    <div v-if="mdAndUp" class="hr-line mt-2"></div>

    <!-- Table layout: md and above -->
    <table v-if="mdAndUp" class="site-indices-table">
      <colgroup>
        <col style="width: 60px" />
        <col style="width: 200px" />
        <col style="width: 295px" />
        <col style="width: 135px" />
        <col style="width: 140px" />
        <col />
      </colgroup>
      <thead>
        <tr>
          <th class="site-col font-weight-bold" scope="col">Site Species</th>
          <th class="computed-col font-weight-bold" scope="col">Computed Value</th>
          <th class="age-col font-weight-bold" scope="col">Age</th>
          <th class="font-weight-bold" scope="col">Height in Meters</th>
          <th class="font-weight-bold" scope="col">BHA Site Index</th>
          <th scope="col"></th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(row, index) in siteIndexRows" :key="row.speciesCode">
          <td class="site-col font-weight-bold">{{ row.speciesCode }}</td>
          <td class="computed-col">
            <v-select
              :model-value="isComputedValueNA(index) ? null : row.computedValue"
              :items="OPTIONS.computedValueOptions"
              item-title="label"
              item-value="value"
              :disabled="isComputedValueDisabled(index)"
              :placeholder="isComputedValueDisabled(index) ? CONSTANTS.SPECIAL_INDICATORS.NA : undefined"
              persistent-placeholder
              hide-details
              density="compact"
              append-inner-icon="mdi-chevron-down"
              @update:model-value="(val) => { row.computedValue = val; handleComputedValueChange(row) }"
            />
          </td>
          <td class="age-col">
            <div class="d-flex align-center age-col-inner">
              <v-radio-group
                :model-value="row.ageType"
                :disabled="isAgeRadioDisabled(row, index)"
                hide-details
                density="compact"
                class="flex-shrink-0"
                @update:model-value="row.ageType = $event"
              >
                <v-radio
                  v-for="opt in OPTIONS.tableAgeTypeOptions"
                  :key="opt.value"
                  :label="opt.label"
                  :value="opt.value"
                  density="compact"
                />
              </v-radio-group>
              <AppSpinField
                class="site-spin-field"
                :model-value="(isSupplied || isNonPrimaryRowDisabled(index)) ? null : row.age"
                :max="CONSTANTS.NUM_INPUT_LIMITS.SPZ_AGE_MAX"
                :min="CONSTANTS.NUM_INPUT_LIMITS.SPZ_AGE_MIN"
                :step="CONSTANTS.NUM_INPUT_LIMITS.SPZ_AGE_STEP"
                :persistent-placeholder="true"
                :placeholder="getAgePlaceholder(row, index)"
                :hide-details="true"
                :disabled="isAgeDisabled(row, index)"
                :interval="CONSTANTS.CONTINUOUS_INC_DEC.INTERVAL"
                :decimalAllowNumber="CONSTANTS.NUM_INPUT_LIMITS.SPZ_AGE_DECIMAL_NUM"
                @update:model-value="row.age = $event"
              />
            </div>
          </td>
          <td>
            <AppSpinField
              class="site-spin-field"
              :model-value="(isSupplied || isNonPrimaryRowDisabled(index)) ? null : row.height"
              :max="CONSTANTS.NUM_INPUT_LIMITS.SPZ_HEIGHT_MAX"
              :min="CONSTANTS.NUM_INPUT_LIMITS.SPZ_HEIGHT_MIN"
              :step="CONSTANTS.NUM_INPUT_LIMITS.SPZ_HEIGHT_STEP"
              :persistent-placeholder="true"
              :placeholder="getHeightPlaceholder(row, index)"
              :hide-details="true"
              :disabled="isHeightDisabled(row, index)"
              :interval="CONSTANTS.CONTINUOUS_INC_DEC.INTERVAL"
              :decimalAllowNumber="CONSTANTS.NUM_INPUT_LIMITS.SPZ_HEIGHT_DECIMAL_NUM"
              @update:model-value="row.height = $event"
            />
          </td>
          <td>
            <AppSpinField
              class="site-spin-field"
              :model-value="row.bhaSiteIndex"
              :max="CONSTANTS.NUM_INPUT_LIMITS.BHA50_SITE_INDEX_MAX"
              :min="CONSTANTS.NUM_INPUT_LIMITS.BHA50_SITE_INDEX_MIN"
              :step="CONSTANTS.NUM_INPUT_LIMITS.BHA50_SITE_INDEX_STEP"
              :persistent-placeholder="true"
              :placeholder="getBhaPlaceholder(row, index)"
              :hide-details="true"
              :disabled="isBhaDisabled(row, index)"
              :interval="CONSTANTS.CONTINUOUS_INC_DEC.INTERVAL"
              :decimalAllowNumber="CONSTANTS.NUM_INPUT_LIMITS.BHA50_SITE_INDEX_DECIMAL_NUM"
              @update:model-value="row.bhaSiteIndex = $event"
            />
          </td>
          <td></td>
        </tr>
      </tbody>
    </table>

    <!-- Card layout: sm and below -->
    <div v-else class="site-index-cards mt-2">
      <v-card
        v-for="(row, index) in siteIndexRows"
        :key="row.speciesCode"
        class="site-index-card mb-4"
        variant="flat"
      >
        <v-card-text class="pa-3">
          <v-row no-gutters>
            <v-col cols="12" sm="6" class="card-item d-flex align-end card-left-col card-site-species">
              <span class="card-field-label">Site<br>Species:</span>
              <span class="font-weight-bold ml-2">{{ row.speciesCode }}</span>
            </v-col>
            <v-col cols="12" sm="6" class="card-item d-flex align-end card-right-col card-computed-value">
              <span class="card-field-label">Computed<br>Value:</span>
              <div class="flex-grow-1 ml-2">
                <v-select
                  :model-value="isComputedValueNA(index) ? null : row.computedValue"
                  :items="OPTIONS.computedValueOptions"
                  item-title="label"
                  item-value="value"
                  :disabled="isComputedValueDisabled(index)"
                  :placeholder="isComputedValueDisabled(index) ? CONSTANTS.SPECIAL_INDICATORS.NA : undefined"
                  persistent-placeholder
                  hide-details
                  density="compact"
                  append-inner-icon="mdi-chevron-down"
                  @update:model-value="(val) => { row.computedValue = val; handleComputedValueChange(row) }"
                />
              </div>
            </v-col>
          </v-row>
          <v-divider color="white" :thickness="2" class="card-divider" />
          <div class="card-item">
            <div class="d-flex align-start card-age-inner">
              <span class="card-field-label mr-1 mt-2">Age:</span>
              <v-radio-group
                :model-value="row.ageType"
                :disabled="isAgeRadioDisabled(row, index)"
                hide-details
                density="compact"
                class="flex-grow-0 flex-shrink-0"
                @update:model-value="row.ageType = $event"
              >
                <v-radio
                  v-for="opt in OPTIONS.cardAgeTypeOptions"
                  :key="opt.value"
                  :label="opt.label"
                  :value="opt.value"
                  density="compact"
                />
              </v-radio-group>
              <AppSpinField
                class="card-age-spin-field ml-4"
                :model-value="(isSupplied || isNonPrimaryRowDisabled(index)) ? null : row.age"
                :max="CONSTANTS.NUM_INPUT_LIMITS.SPZ_AGE_MAX"
                :min="CONSTANTS.NUM_INPUT_LIMITS.SPZ_AGE_MIN"
                :step="CONSTANTS.NUM_INPUT_LIMITS.SPZ_AGE_STEP"
                :persistent-placeholder="true"
                :placeholder="getAgePlaceholder(row, index)"
                :hide-details="true"
                :disabled="isAgeDisabled(row, index)"
                :interval="CONSTANTS.CONTINUOUS_INC_DEC.INTERVAL"
                :decimalAllowNumber="CONSTANTS.NUM_INPUT_LIMITS.SPZ_AGE_DECIMAL_NUM"
                @update:model-value="row.age = $event"
              />
            </div>
          </div>
          <v-divider color="white" :thickness="2" class="card-divider" />
          <v-row no-gutters>
            <v-col cols="12" sm="6" class="card-item d-flex align-end card-left-col card-height-in-meters">
              <span class="card-field-label">Height in<br>Meters:</span>
              <div class="flex-grow-1 ml-2">
                <AppSpinField
                  class="card-spin-field"
                  :model-value="(isSupplied || isNonPrimaryRowDisabled(index)) ? null : row.height"
                  :max="CONSTANTS.NUM_INPUT_LIMITS.SPZ_HEIGHT_MAX"
                  :min="CONSTANTS.NUM_INPUT_LIMITS.SPZ_HEIGHT_MIN"
                  :step="CONSTANTS.NUM_INPUT_LIMITS.SPZ_HEIGHT_STEP"
                  :persistent-placeholder="true"
                  :placeholder="getHeightPlaceholder(row, index)"
                  :hide-details="true"
                  :disabled="isHeightDisabled(row, index)"
                  :interval="CONSTANTS.CONTINUOUS_INC_DEC.INTERVAL"
                  :decimalAllowNumber="CONSTANTS.NUM_INPUT_LIMITS.SPZ_HEIGHT_DECIMAL_NUM"
                  @update:model-value="row.height = $event"
                />
              </div>
            </v-col>
            <v-col cols="12" sm="6" class="card-item d-flex align-end card-right-col card-bha-site-index">
              <span class="card-field-label">BHA<br>Site Index:</span>
              <div class="flex-grow-1 ml-2">
                <AppSpinField
                  class="card-spin-field"
                  :model-value="row.bhaSiteIndex"
                  :max="CONSTANTS.NUM_INPUT_LIMITS.BHA50_SITE_INDEX_MAX"
                  :min="CONSTANTS.NUM_INPUT_LIMITS.BHA50_SITE_INDEX_MIN"
                  :step="CONSTANTS.NUM_INPUT_LIMITS.BHA50_SITE_INDEX_STEP"
                  :persistent-placeholder="true"
                  :placeholder="getBhaPlaceholder(row, index)"
                  :hide-details="true"
                  :disabled="isBhaDisabled(row, index)"
                  :interval="CONSTANTS.CONTINUOUS_INC_DEC.INTERVAL"
                  :decimalAllowNumber="CONSTANTS.NUM_INPUT_LIMITS.BHA50_SITE_INDEX_DECIMAL_NUM"
                  @update:model-value="row.bhaSiteIndex = $event"
                />
              </div>
            </v-col>
          </v-row>
        </v-card-text>
      </v-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { storeToRefs } from 'pinia'
import { useDisplay } from 'vuetify'
import { useModelParameterStore } from '@/stores/projection/modelParameterStore'
import { AppSpinField } from '@/components'
import type { SiteIndexSpeciesRow } from '@/interfaces/interfaces'
import { CONSTANTS, OPTIONS, DEFAULTS } from '@/constants'

const props = defineProps<{
  isConfirmEnabled: boolean
}>()

const { mdAndUp } = useDisplay()

const modelParameterStore = useModelParameterStore()
const { siteIndexRows, siteSpeciesValues, derivedBy, isSupplied } = storeToRefs(modelParameterStore)

const isComputed = computed(() => siteSpeciesValues.value === CONSTANTS.SITE_SPECIES_VALUES.COMPUTED)
const isVolume = computed(() => derivedBy.value === CONSTANTS.DERIVED_BY.VOLUME)

const isNonPrimaryRowDisabled = (index: number) =>
  isComputed.value && isVolume.value && index > 0

const isComputedValueNA = (index: number) =>
  isSupplied.value || isNonPrimaryRowDisabled(index)

const isComputedValueDisabled = (index: number) =>
  !props.isConfirmEnabled || isComputedValueNA(index)

const isAgeRadioDisabled = (row: SiteIndexSpeciesRow, index: number) =>
  isAgeDisabled(row, index)

const isAgeDisabled = (row: SiteIndexSpeciesRow, index: number) =>
  !props.isConfirmEnabled ||
  isSupplied.value ||
  isNonPrimaryRowDisabled(index) ||
  (isComputed.value && row.computedValue === CONSTANTS.COMPUTED_VALUE.TOTAL_AGE)

const isHeightDisabled = (row: SiteIndexSpeciesRow, index: number) =>
  !props.isConfirmEnabled ||
  isSupplied.value ||
  isNonPrimaryRowDisabled(index) ||
  (isComputed.value && row.computedValue === CONSTANTS.COMPUTED_VALUE.HEIGHT)

const isBhaDisabled = (row: SiteIndexSpeciesRow, index: number) =>
  !props.isConfirmEnabled ||
  isNonPrimaryRowDisabled(index) ||
  (isComputed.value && row.computedValue === CONSTANTS.COMPUTED_VALUE.BHA_SITE_INDEX)

const getAgePlaceholder = (row: SiteIndexSpeciesRow, index: number): string => {
  if (isSupplied.value || isNonPrimaryRowDisabled(index)) return CONSTANTS.SPECIAL_INDICATORS.NA
  if (row.computedValue === CONSTANTS.COMPUTED_VALUE.TOTAL_AGE) return CONSTANTS.SPECIAL_INDICATORS.CALC
  return ''
}

const getHeightPlaceholder = (row: SiteIndexSpeciesRow, index: number): string => {
  if (isSupplied.value || isNonPrimaryRowDisabled(index)) return CONSTANTS.SPECIAL_INDICATORS.NA
  if (isComputed.value && row.computedValue === CONSTANTS.COMPUTED_VALUE.HEIGHT) return CONSTANTS.SPECIAL_INDICATORS.CALC
  return ''
}

const getBhaPlaceholder = (row: SiteIndexSpeciesRow, index: number): string => {
  if (isNonPrimaryRowDisabled(index)) return CONSTANTS.SPECIAL_INDICATORS.NA
  if (isComputed.value && row.computedValue === CONSTANTS.COMPUTED_VALUE.BHA_SITE_INDEX) return CONSTANTS.SPECIAL_INDICATORS.CALC
  return ''
}


const handleComputedValueChange = (row: SiteIndexSpeciesRow) => {
  const isBha = row.computedValue === CONSTANTS.COMPUTED_VALUE.BHA_SITE_INDEX
  const isHeight = row.computedValue === CONSTANTS.COMPUTED_VALUE.HEIGHT
  const isTotal = row.computedValue === CONSTANTS.COMPUTED_VALUE.TOTAL_AGE
  row.bhaSiteIndex = isBha ? null : row.bhaSiteIndex ?? DEFAULTS.DEFAULT_VALUES.BHA50_SITE_INDEX
  row.height = isHeight ? null : row.height ?? DEFAULTS.DEFAULT_VALUES.SPZ_HEIGHT
  row.age = isTotal ? null : row.age ?? DEFAULTS.DEFAULT_VALUES.SPZ_AGE
  row.ageType = isTotal ? CONSTANTS.AGE_TYPE.TOTAL : (row.ageType ?? CONSTANTS.AGE_TYPE.TOTAL)
}
</script>

<style scoped>
.site-indices-table {
  table-layout: fixed;
  width: 100%;
  border-collapse: collapse;
}
.site-indices-table thead tr {
  border-bottom: 1px solid rgba(0, 0, 0, 0.12);
}
.site-indices-table th {
  text-align: left !important;
  vertical-align: middle;
  padding-top: 0 !important;
  padding-bottom: 0 !important;
  height: 34px !important;
}
.site-indices-table td {
  text-align: left !important;
  vertical-align: middle;
  padding-top: 12px !important;
  padding-bottom: 12px !important;
}
.site-indices-table tbody tr td {
  border-bottom: 1px solid rgba(0, 0, 0, 0.12) !important;
}
.age-col :deep(.v-radio-group .v-selection-control-group:not(.v-selection-control-group--inline)) {
  gap: 0 !important;
}
.site-spin-field {
  width: 120px !important;
  max-width: 120px !important;
  flex: 0 0 120px;
}
.age-col-inner {
  gap: 16px;
  width: fit-content;
}
.site-indices-table th:not(:first-child),
.site-indices-table td:not(:first-child) {
  padding-left: 16px;
}

/* Card layout styles */
.site-index-card {
  background-color: #ddeeff;
  border-radius: 8px;
  border: 1.5px solid rgba(0, 0, 0, 0.4) !important;
}
.card-item {
  padding: 8px 0;
}
.card-divider {
  margin: 4px 0;
  opacity: 1 !important;
  border-color: white !important;
}
.card-field-label {
  font-weight: bold;
  font-size: 0.875rem;
  flex-shrink: 0;
  line-height: 1.4;
}
.card-age-inner {
  align-items: flex-start;
}
.card-spin-field {
  width: 100% !important;
  max-width: none !important;
}
.card-item :deep(.v-radio-group .v-selection-control-group:not(.v-selection-control-group--inline)) {
  gap: 0 !important;
}

@media (min-width: 768px) and (max-width: 912px) {
  .card-age-spin-field {
    width: 100px !important;
    max-width: 100px !important;
    flex: 0 0 100px;
  }
}

/* sm: right col gets white left border as vertical separator between columns */
@media (min-width: 600px) {
  .card-left-col {
    padding-right: 16px !important;
    padding-bottom: 4px !important;
    margin-bottom: 4px !important;
  }
  .card-right-col {
    border-left: 2px solid white;
    padding-left: 16px !important;
    padding-top: 2px !important;
    margin-top: 2px !important;
    padding-bottom: 4px !important;
    margin-bottom: 4px !important;
  }
  .card-computed-value {
    margin-top: 0px !important;
  }
  .card-height-in-meters {
    margin-bottom: 0px !important;
    padding-bottom: 0px !important;
  }
  .card-bha-site-index {
    margin-bottom: 0px !important;
    padding-bottom: 0px !important;
  }
}

/* xs: left col gets white bottom border to separate from right col (stacked) */
@media (max-width: 599px) {
  .card-left-col {
    border-bottom: 2px solid white;
    padding-top: 2px !important;
    margin-top: 2px !important;
    padding-bottom: 12px !important;
    margin-bottom: 12px !important;
  }
  .card-right-col {
    padding-bottom: 4px !important;
    margin-bottom: 4px !important;
  }
  .card-site-species {
    margin-top: 0px !important;
    padding-top: 0px !important;
  }
  .card-bha-site-index {
    margin-bottom: 0px !important;
    padding-bottom: 0px !important;
  }
}

@media (max-width: 350px) {
  .card-age-spin-field {
    width: 85px !important;
    max-width: 85px !important;
    flex: 0 0 85px;
  }
}
</style>
