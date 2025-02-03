<template>
  <div>
    <v-row>
      <v-col cols="3">
        <v-text-field
          id="startingAge"
          label="Starting Age"
          type="number"
          v-model.number="localStartingAge"
          :min="CONSTANTS.NUM_INPUT_LIMITS.STARTING_AGE_MIN"
          :max="CONSTANTS.NUM_INPUT_LIMITS.STARTING_AGE_MAX"
          :step="CONSTANTS.NUM_INPUT_LIMITS.STARTING_AGE_STEP"
          persistent-placeholder
          placeholder=""
          density="compact"
          dense
          :disabled="isDisabled"
          @update:model-value="handleStartingAgeInput"
        ></v-text-field>
      </v-col>
      <v-col class="col-space-3" />
      <v-col cols="3">
        <v-text-field
          id="finishingAge"
          label="Finishing Age"
          type="number"
          v-model.number="localFinishingAge"
          :min="CONSTANTS.NUM_INPUT_LIMITS.FINISHING_AGE_MIN"
          :max="CONSTANTS.NUM_INPUT_LIMITS.FINISHING_AGE_MAX"
          :step="CONSTANTS.NUM_INPUT_LIMITS.FINISHING_AGE_STEP"
          persistent-placeholder
          placeholder=""
          density="compact"
          dense
          :disabled="isDisabled"
          @update:model-value="handleFinishingAgeInput"
        ></v-text-field>
      </v-col>
      <v-col class="col-space-3" />
      <v-col cols="3">
        <v-text-field
          id="ageIncrement"
          label="Age Increment"
          type="number"
          v-model.number="localAgeIncrement"
          :min="CONSTANTS.NUM_INPUT_LIMITS.AGE_INC_MIN"
          :max="CONSTANTS.NUM_INPUT_LIMITS.AGE_INC_MAX"
          :step="CONSTANTS.NUM_INPUT_LIMITS.AGE_INC_STEP"
          persistent-placeholder
          placeholder=""
          density="compact"
          dense
          :disabled="isDisabled"
          @update:model-value="handleAgeIncrementInput"
        ></v-text-field>
      </v-col>
    </v-row>
  </div>
  <div class="ml-4 mt-5">
    <div class="ml-n4 mt-n5">
      <span class="text-h7">Volumes Reported</span>
    </div>
    <v-row class="ml-n6">
      <v-col cols="12" style="padding-top: 0px">
        <v-row>
          <v-col
            v-for="(option, index) in OPTIONS.volumeReportedOptions"
            :key="index"
            :style="{ 'max-width': index < 4 ? '20%' : 'auto' }"
          >
            <v-checkbox
              v-model="localVolumeReported"
              :label="option.label"
              :value="option.value"
              hide-details
              :disabled="isVolumeReportedDisabled"
              data-testid="volume-reported"
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
            v-for="(option, index) in OPTIONS.includeInReportOptions"
            :key="index"
            :style="{ 'max-width': index < 4 ? '20%' : 'auto' }"
          >
            <v-checkbox
              v-model="localIncludeInReport"
              :label="option.label"
              :value="option.value"
              hide-details
              :disabled="getIncludeInReportDisabled(option.value)"
            ></v-checkbox>
          </v-col>
          <v-col style="max-width: 20% !important">
            <v-select
              label="Projection Type"
              :items="OPTIONS.projectionTypeOptions"
              v-model="localProjectionType"
              item-title="label"
              item-value="value"
              hide-details="auto"
              persistent-placeholder
              placeholder="Select..."
              density="compact"
              dense
              style="max-width: 70% !important"
              :disabled="isDisabled"
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
      <v-col cols="6">
        <v-text-field
          id="reportTitle"
          type="string"
          v-model="localReportTitle"
          hide-details="auto"
          persistent-placeholder
          placeholder="Enter a report title..."
          density="compact"
          dense
          style="max-width: 50% !important"
          :disabled="isDisabled"
        ></v-text-field>
      </v-col>
    </v-row>
  </div>
</template>
<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { CONSTANTS, OPTIONS } from '@/constants'
import { Util } from '@/utils/util'

const props = defineProps<{
  startingAge: number | null
  finishingAge: number | null
  ageIncrement: number | null
  volumeReported: string[]
  includeInReport: string[]
  projectionType: string | null
  reportTitle: string | null
  isDisabled: boolean
}>()

const emit = defineEmits([
  'update:startingAge',
  'update:finishingAge',
  'update:ageIncrement',
  'update:volumeReported',
  'update:includeInReport',
  'update:projectionType',
  'update:reportTitle',
])

const localStartingAge = ref<number | null>(props.startingAge)
const localFinishingAge = ref<number | null>(props.finishingAge)
const localAgeIncrement = ref<number | null>(props.ageIncrement)
const localVolumeReported = ref<string[]>([...props.volumeReported])
const localIncludeInReport = ref<string[]>([...props.includeInReport])
const localProjectionType = ref<string | null>(props.projectionType)
const localReportTitle = ref<string | null>(props.reportTitle)

// Watch props for changes (Prop -> Local State)
watch(
  () => props.startingAge,
  (newVal) => {
    localStartingAge.value = newVal
  },
)
watch(
  () => props.finishingAge,
  (newVal) => {
    localFinishingAge.value = newVal
  },
)
watch(
  () => props.ageIncrement,
  (newVal) => {
    localAgeIncrement.value = newVal
  },
)
watch(
  () => props.volumeReported,
  (newVal) => {
    if (JSON.stringify(newVal) !== JSON.stringify(localVolumeReported.value)) {
      localVolumeReported.value = [...newVal]
    }
  },
)
watch(
  () => props.includeInReport,
  (newVal) => {
    if (JSON.stringify(newVal) !== JSON.stringify(localIncludeInReport.value)) {
      localIncludeInReport.value = [...newVal]
    }
  },
)
watch(
  () => props.projectionType,
  (newVal) => {
    localProjectionType.value = newVal
  },
)
watch(
  () => props.reportTitle,
  (newVal) => {
    localReportTitle.value = newVal
  },
)

// Calculating whether the "Culmination Values" checkbox is enabled
const isCulminationValuesEnabled = computed(() => {
  return (
    localStartingAge.value !== null &&
    localStartingAge.value <= 10 &&
    localFinishingAge.value !== null &&
    localFinishingAge.value >= 300
  )
})

// Watch local state for changes (Local State -> Parent Emit)
watch(localStartingAge, (newVal) => emit('update:startingAge', newVal))
watch(localFinishingAge, (newVal) => emit('update:finishingAge', newVal))

watch([localStartingAge, localFinishingAge], () => {
  if (!isCulminationValuesEnabled.value) {
    // Remove the selection if "Culmination Values" is disabled
    localIncludeInReport.value = localIncludeInReport.value.filter(
      (item) => item !== CONSTANTS.INCLUDE_IN_REPORT.CULMINATION_VALUES,
    )
  }
})

watch(localAgeIncrement, (newVal) => emit('update:ageIncrement', newVal))
watch(localVolumeReported, (newVal) => {
  if (JSON.stringify(newVal) !== JSON.stringify(props.volumeReported)) {
    emit('update:volumeReported', [...newVal])
  }
})
watch(localIncludeInReport, (newVal) => {
  if (JSON.stringify(newVal) !== JSON.stringify(props.includeInReport)) {
    emit('update:includeInReport', [...newVal])
  }
})

watch(localProjectionType, (newVal) => {
  if (newVal === CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS) {
    // Reset the "Volumes Reported" checkbox and force select only 'Close Utilization' checkbox
    localVolumeReported.value = [CONSTANTS.VOLUME_REPORTED.CLOSE_UTIL]
    emit('update:volumeReported', [...localVolumeReported.value])
  }

  emit('update:projectionType', newVal)
})

watch(localReportTitle, (newVal) => emit('update:reportTitle', newVal))

// Decide whether to disable the "Volumes Reported" checkbox
const isVolumeReportedDisabled = computed(() => {
  return (
    localProjectionType.value === CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS ||
    props.isDisabled
  )
})

// Decide to enable/disable the "Include in Report" checkbox
const getIncludeInReportDisabled = (value: string) => {
  if (value === CONSTANTS.INCLUDE_IN_REPORT.CULMINATION_VALUES) {
    return !isCulminationValuesEnabled.value
  } else if (value === CONSTANTS.INCLUDE_IN_REPORT.COMPUTED_MAI) {
    return localProjectionType.value === CONSTANTS.PROJECTION_TYPE.CFS_BIOMASS
  }
  return props.isDisabled
}

const handleStartingAgeInput = (value: string) => {
  // Convert an empty string to null
  localStartingAge.value = Util.parseNumberOrNull(value)
}

const handleFinishingAgeInput = (value: string) => {
  localFinishingAge.value = Util.parseNumberOrNull(value)
}

const handleAgeIncrementInput = (value: string) => {
  localAgeIncrement.value = Util.parseNumberOrNull(value)
}
</script>
<style scoped></style>
