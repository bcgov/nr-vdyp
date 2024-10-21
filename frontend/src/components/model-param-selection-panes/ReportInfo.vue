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
              <span class="text-h6">Report Information</span>
            </v-col>
          </v-row>
        </v-expansion-panel-title>
        <v-expansion-panel-text class="expansion-panel-text mt-n2">
          <div>
            <v-row>
              <v-col cols="3">
                <v-text-field
                  label="Starting Age"
                  type="number"
                  v-model="startingAge"
                  min="0"
                  max="500"
                  step="1"
                  :rules="[validateAge]"
                  :error-messages="startingAgeError"
                  persistent-placeholder
                  placeholder="Select..."
                  density="compact"
                  dense
                ></v-text-field>
              </v-col>
              <v-col class="col-space-3" />
              <v-col cols="3">
                <v-text-field
                  label="Finishing Age"
                  type="number"
                  v-model="finishingAge"
                  min="0"
                  max="500"
                  step="10"
                  :rules="[validateAge]"
                  :error-messages="finishingAgeError"
                  persistent-placeholder
                  placeholder="Select..."
                  density="compact"
                  dense
                ></v-text-field>
              </v-col>
              <v-col class="col-space-3" />
              <v-col cols="3">
                <v-text-field
                  label="Age Increment"
                  type="number"
                  v-model="ageIncrement"
                  min="1"
                  max="350"
                  step="5"
                  :rules="[validateAgeIncrement]"
                  :error-messages="ageIncrementError"
                  persistent-placeholder
                  placeholder="Select..."
                  density="compact"
                  dense
                ></v-text-field>
              </v-col>
            </v-row>
          </div>
          <div class="ml-4 mt-5">
            <div class="ml-n4 mt-n5">
              <span class="text-h7">Volume Reported</span>
            </div>
            <v-row class="ml-n6">
              <v-col cols="12" style="padding-top: 0px">
                <v-row>
                  <v-col
                    v-for="(option, index) in volumeReportedOptions"
                    :key="index"
                    :style="{ 'max-width': index < 4 ? '20%' : 'auto' }"
                  >
                    <v-checkbox
                      v-model="selectedVolumeReported"
                      :label="option.label"
                      :value="option.value"
                      hide-details
                    ></v-checkbox>
                  </v-col>
                </v-row>
              </v-col>
            </v-row>
          </div>
          <div class="ml-4 mt-5">
            <div class="ml-n4 mt-n5">
              <span class="text-h7">Include in Report</span>
            </div>
            <v-row class="ml-n6">
              <v-col cols="12" style="padding-top: 0px">
                <v-row>
                  <v-col
                    v-for="(option, index) in includeInReportOptions"
                    :key="index"
                    :style="{ 'max-width': index < 4 ? '20%' : 'auto' }"
                  >
                    <v-checkbox
                      v-model="selectedVolumeReported"
                      :label="option.label"
                      :value="option.value"
                      hide-details
                    ></v-checkbox>
                  </v-col>

                  <v-col style="max-width: 20% !important">
                    <v-select
                      label="Projection Type"
                      :items="projectionTypeOptions"
                      v-model="projectionType"
                      item-title="label"
                      item-value="value"
                      clearable
                      hide-details="auto"
                      persistent-placeholder
                      placeholder="Select..."
                      density="compact"
                      dense
                      style="max-width: 70% !important"
                    ></v-select>
                  </v-col>
                </v-row>
              </v-col>
            </v-row>
          </div>
          <div class="ml-4 mt-5">
            <div class="ml-n4 mt-n5">
              <span class="text-h7">Report Title</span>
            </div>
            <v-row>
              <v-col cols="12">
                <v-text-field
                  type="string"
                  v-model="reportTitle"
                  hide-details="auto"
                  persistent-placeholder
                  placeholder="Enter a report title..."
                  density="compact"
                  dense
                  style="max-width: 50% !important"
                ></v-text-field>
              </v-col>
            </v-row>
          </div>
          <div class="mt-5">
            <v-row v-for="(group, index) in speciesGroups" :key="index">
              <v-col cols="3">
                <v-text-field
                  :label="`Minimum DBH Limit by Species #${index + 1}`"
                  type="string"
                  v-model="group.group"
                  persistent-placeholder
                  placeholder="Select..."
                  density="compact"
                  dense
                  readonly
                ></v-text-field>
              </v-col>
              <v-col class="col-space-3" />
              <v-col cols="5" class="ma-5">
                <vue-slider
                  v-model="group.minimumDBHLimit"
                  :data="minimumDBHLimitsOptions"
                  :data-value="'value'"
                  :data-label="'label'"
                  :contained="true"
                  :tooltip="'none'"
                  :dotStyle="{ backgroundColor: '#787878' }"
                  :rail-style="{ backgroundColor: '#f5f5f5' }"
                  :process-style="{ backgroundColor: '#787878' }"
                />
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
import { ref, computed } from 'vue'
import { useModelParameterStore } from '@/stores/modelParameterStore'
import { storeToRefs } from 'pinia'
import VueSlider from 'vue-slider-component'
import 'vue-slider-component/theme/default.css'
import {
  volumeReportedOptions,
  includeInReportOptions,
  projectionTypeOptions,
  minimumDBHLimitsOptions,
} from '@/constants/options'

const panelOpen = ref(0)

const modelParameterStore = useModelParameterStore()
const {
  speciesGroups,
  startingAge,
  finishingAge,
  ageIncrement,
  selectedVolumeReported,
  projectionType,
  reportTitle,
} = storeToRefs(modelParameterStore)

const validateAge = (value: any) => {
  if (value === null || value === '') {
    return true
  }
  if (value < 0 || value > 500) {
    return 'Please enter a value between 0 and 500'
  }
  return true
}

const validateAgeIncrement = (value: any) => {
  if (value === null || value === '') {
    return true
  }
  if (value < 1 || value > 350) {
    return 'Please enter a value between 1 and 350'
  }
  if (value !== null && value % 5 !== 0) {
    return 'Please enter a value that is a multiple of 5.'
  }

  return true
}

const startingAgeError = computed(() => {
  const error = validateAge(startingAge.value)
  return error === true ? [] : [error]
})

const finishingAgeError = computed(() => {
  const error = validateAge(finishingAge.value)
  return error === true ? [] : [error]
})

const ageIncrementError = computed(() => {
  const error = validateAgeIncrement(ageIncrement.value)
  return error === true ? [] : [error]
})

const clear = () => {}
const confirm = () => {}
</script>
<style scoped></style>
