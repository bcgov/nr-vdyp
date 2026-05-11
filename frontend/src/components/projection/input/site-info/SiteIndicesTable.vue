<template>
  <div>
    <span class="text-subtitle-2">Site Indices</span>
    <div class="hr-line mt-2"></div>
    <table class="site-indices-table">
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
              :model-value="isComputedValueDisabled(index) ? null : row.computedValue"
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
                v-model="row.ageType"
                :disabled="isAgeRadioDisabled(index)"
                hide-details
                density="compact"
                class="flex-shrink-0"
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
                :model-value="row.age"
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
              :model-value="row.height"
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
  </div>
</template>

<script setup lang="ts">
import { computed, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { useModelParameterStore } from '@/stores/projection/modelParameterStore'
import { AppSpinField } from '@/components'
import type { SiteIndexSpeciesRow } from '@/interfaces/interfaces'
import { CONSTANTS, OPTIONS, DEFAULTS } from '@/constants'

const props = defineProps<{
  isConfirmEnabled: boolean
}>()

const modelParameterStore = useModelParameterStore()
const { siteIndexRows, siteSpeciesValues, derivedBy } = storeToRefs(modelParameterStore)

const isSupplied = computed(() => siteSpeciesValues.value === CONSTANTS.SITE_SPECIES_VALUES.SUPPLIED)
const isComputed = computed(() => siteSpeciesValues.value === CONSTANTS.SITE_SPECIES_VALUES.COMPUTED)
const isVolume = computed(() => derivedBy.value === CONSTANTS.DERIVED_BY.VOLUME)

const isNonPrimaryRowDisabled = (index: number) =>
  isComputed.value && isVolume.value && index > 0

const isComputedValueDisabled = (index: number) =>
  !props.isConfirmEnabled || isSupplied.value || isNonPrimaryRowDisabled(index)

const isAgeRadioDisabled = (index: number) =>
  !props.isConfirmEnabled || isSupplied.value || isNonPrimaryRowDisabled(index)

const isAgeDisabled = (row: SiteIndexSpeciesRow, index: number) =>
  !props.isConfirmEnabled ||
  isSupplied.value ||
  isNonPrimaryRowDisabled(index) ||
  row.computedValue === CONSTANTS.COMPUTED_VALUE.TOTAL_AGE

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

watch(isSupplied, (supplied) => {
  if (supplied) {
    siteIndexRows.value.forEach((row) => {
      row.age = null
      row.height = null
      row.bhaSiteIndex = DEFAULTS.DEFAULT_VALUES.BHA50_SITE_INDEX
    })
  } else {
    siteIndexRows.value.forEach((row) => {
      row.computedValue = CONSTANTS.COMPUTED_VALUE.BHA_SITE_INDEX
      row.ageType = CONSTANTS.AGE_TYPE.TOTAL
      row.age = DEFAULTS.DEFAULT_VALUES.SPZ_AGE
      row.height = DEFAULTS.DEFAULT_VALUES.SPZ_HEIGHT
      row.bhaSiteIndex = DEFAULTS.DEFAULT_VALUES.BHA50_SITE_INDEX
    })
  }
})

const handleComputedValueChange = (row: SiteIndexSpeciesRow) => {
  if (row.computedValue === CONSTANTS.COMPUTED_VALUE.BHA_SITE_INDEX) {
    row.bhaSiteIndex = null
    if (!row.age) row.age = DEFAULTS.DEFAULT_VALUES.SPZ_AGE
    if (!row.height) row.height = DEFAULTS.DEFAULT_VALUES.SPZ_HEIGHT
  } else if (row.computedValue === CONSTANTS.COMPUTED_VALUE.HEIGHT) {
    row.height = null
    if (!row.age) row.age = DEFAULTS.DEFAULT_VALUES.SPZ_AGE
    if (!row.bhaSiteIndex) row.bhaSiteIndex = DEFAULTS.DEFAULT_VALUES.BHA50_SITE_INDEX
  } else if (row.computedValue === CONSTANTS.COMPUTED_VALUE.TOTAL_AGE) {
    row.age = null
    if (!row.height) row.height = DEFAULTS.DEFAULT_VALUES.SPZ_HEIGHT
    if (!row.bhaSiteIndex) row.bhaSiteIndex = DEFAULTS.DEFAULT_VALUES.BHA50_SITE_INDEX
  }
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
</style>
