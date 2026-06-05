<template>
  <v-dialog v-model="isOpen" max-width="520" persistent>
    <v-card>
      <!-- Header -->
      <div class="modal-header d-flex align-center justify-space-between pa-4 pb-2">
        <span class="text-h6 font-weight-bold">Select Species for Calculation</span>
        <v-btn icon variant="text" size="small" @click="handleCancel" aria-label="Close">
          <v-icon>mdi-close</v-icon>
        </v-btn>
      </div>

      <!-- Body -->
      <div class="modal-body px-4 pt-2 pb-0">
        <!-- Search -->
        <v-text-field
          v-model="searchQuery"
          placeholder="Search"
          prepend-inner-icon="mdi-magnify"
          clearable
          variant="outlined"
          density="compact"
          hide-details
          class="mb-3"
        />

        <!-- Limit and selected count row -->
        <div class="d-flex justify-space-between align-center mb-1 px-1">
          <span class="text-body-2">Select Up to {{ maxSpecies }} Species</span>
          <span class="text-body-2 font-weight-bold">{{ selectedCodes.length }} Selected</span>
        </div>

        <!-- Column headers -->
        <div class="species-table-header d-flex align-center px-2 py-1">
          <div class="col-name text-caption font-weight-medium">Species Name</div>
          <div class="col-group text-caption font-weight-medium">Species Group</div>
          <div class="col-site text-caption font-weight-medium">Site Species</div>
          <div class="col-action"></div>
        </div>

        <!-- Species list -->
        <div class="species-list-scroll">
          <div
            v-for="item in filteredSpecies"
            :key="item.code"
            class="species-row d-flex align-center px-2"
            :class="{
              'species-row--selected': isSelected(item.code),
              'species-row--at-limit': !isSelected(item.code) && atLimit,
            }"
            @click="toggleSelection(item.code)"
          >
            <div class="col-name text-body-2">{{ item.name }}</div>
            <div class="col-group text-body-2">{{ item.group }}</div>
            <div class="col-site text-body-2">{{ item.code }}</div>
            <div class="col-action d-flex justify-end">
              <v-icon
                :color="iconColor(item.code)"
                size="20"
              >
                {{ isSelected(item.code) ? 'mdi-minus-circle-outline' : 'mdi-plus-circle-outline' }}
              </v-icon>
            </div>
          </div>
          <div
            v-if="filteredSpecies.length === 0"
            class="text-center py-4 text-body-2 text-medium-emphasis"
          >
            No species found.
          </div>
        </div>
      </div>

      <!-- Actions -->
      <div class="d-flex justify-end pa-4 pt-3">
        <AppButton label="Cancel" variant="tertiary" @click="handleCancel" />
        <AppButton label="Confirm" variant="primary" class="ml-4" @click="handleConfirm" />
      </div>
    </v-card>
  </v-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { BIZCONSTANTS } from '@/constants'
import { AppButton } from '@/components'

const props = defineProps<{
  modelValue: boolean
  existingSpecies: string[]
  maxSpecies?: number
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  confirm: [selected: string[]]
}>()

const resolvedMax = computed(() => props.maxSpecies ?? 6)

const isOpen = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

const searchQuery = ref('')
const selectedCodes = ref<string[]>([...props.existingSpecies])

watch(
  () => props.modelValue,
  (open) => {
    if (open) {
      selectedCodes.value = [...props.existingSpecies]
      searchQuery.value = ''
    }
  },
)

const allSpecies = computed(() =>
  (
    Object.keys(BIZCONSTANTS.SPECIES_MAP) as Array<keyof typeof BIZCONSTANTS.SPECIES_MAP>
  ).map((code) => ({
    code,
    name: BIZCONSTANTS.SPECIES_MAP[code],
    group: BIZCONSTANTS.SPECIES_GROUP_MAP[code] ?? code,
  })),
)

const filteredSpecies = computed(() => {
  const q = searchQuery.value?.toLowerCase().trim() ?? ''
  if (!q) return allSpecies.value
  return allSpecies.value.filter(
    (item) =>
      item.name.toLowerCase().includes(q) ||
      item.group.toLowerCase().includes(q) ||
      item.code.toLowerCase().includes(q),
  )
})

const isSelected = (code: string) => selectedCodes.value.includes(code)

const atLimit = computed(() => selectedCodes.value.length >= resolvedMax.value)

const iconColor = (code: string): string => {
  if (isSelected(code)) return 'error'
  if (atLimit.value) return 'grey-lighten-1'
  return 'primary'
}

const toggleSelection = (code: string) => {
  const idx = selectedCodes.value.indexOf(code)
  if (idx !== -1) {
    selectedCodes.value.splice(idx, 1)
  } else if (!atLimit.value) {
    selectedCodes.value.push(code)
  }
}

const handleCancel = () => {
  isOpen.value = false
}

const handleConfirm = () => {
  emit('confirm', [...selectedCodes.value])
  isOpen.value = false
}
</script>

<style scoped>
.modal-header {
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
}

.species-table-header {
  background-color: rgba(0, 0, 0, 0.04);
  border-radius: 4px 4px 0 0;
  border-bottom: 1px solid rgba(0, 0, 0, 0.1);
}

.species-list-scroll {
  max-height: 300px;
  overflow-y: auto;
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-top: none;
  border-radius: 0 0 4px 4px;
}

.species-row {
  cursor: pointer;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  padding-top: 8px;
  padding-bottom: 8px;
  transition: background-color 0.1s ease;
}

.species-row:last-child {
  border-bottom: none;
}

.species-row:hover:not(.species-row--selected):not(.species-row--at-limit) {
  background-color: rgba(0, 0, 0, 0.04);
}

.species-row--selected {
  background-color: #c8e6c9;
}

.species-row--at-limit {
  cursor: default;
  color: rgba(0, 0, 0, 0.38);
}

.col-name {
  flex: 1 1 45%;
  min-width: 0;
  padding-right: 8px;
}

.col-group {
  flex: 0 0 22%;
  text-align: center;
}

.col-site {
  flex: 0 0 18%;
  text-align: center;
}

.col-action {
  flex: 0 0 15%;
  text-align: right;
}
</style>
