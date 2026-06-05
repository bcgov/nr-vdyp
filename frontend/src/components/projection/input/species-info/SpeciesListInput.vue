<template>
  <div class="species-list-input">
    <!-- Empty state -->
    <div v-if="localSpeciesList.length === 0" class="empty-state">
      <p class="empty-text mb-3">No Species added.</p>
      <AppButton
        label="Add Species"
        variant="primary"
        mdi-name="mdi-plus"
        :isDisabled="!isConfirmEnabled"
        @click="$emit('request-add')"
      />
    </div>

    <!-- Cards grid -->
    <div v-else class="species-cards-grid">
      <SpeciesCard
        v-for="item in localSpeciesList"
        :key="item.species!"
        :speciesCode="item.species!"
        :percent="item.percent"
        :isDisabled="!isConfirmEnabled"
        @update:percent="(v) => handlePercentUpdate(item.species!, v)"
        @percent-blur="handlePercentBlur"
        @delete="handleDelete(item.species!)"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, type PropType } from 'vue'
import { cloneDeep } from 'lodash'
import type { SpeciesList } from '@/interfaces/interfaces'
import { AppButton } from '@/components'
import SpeciesCard from './SpeciesCard.vue'

const props = defineProps({
  speciesList: {
    type: Array as PropType<SpeciesList[]>,
    required: true,
  },
  isConfirmEnabled: {
    type: Boolean,
    required: true,
  },
})

const emit = defineEmits<{
  'update:speciesList': [list: SpeciesList[]]
  'request-add': []
}>()

const localSpeciesList = ref<SpeciesList[]>(cloneDeep(props.speciesList))

watch(
  () => props.speciesList,
  (newList) => {
    const isDifferent =
      newList.length !== localSpeciesList.value.length ||
      newList.some(
        (item, i) =>
          item.species !== localSpeciesList.value[i]?.species ||
          item.percent !== localSpeciesList.value[i]?.percent,
      )
    if (isDifferent) {
      localSpeciesList.value = cloneDeep(newList)
    }
  },
  { deep: true },
)

const sortByPercent = () => {
  localSpeciesList.value.sort((a, b) => {
    const pA = Number.parseFloat(a.percent || '0')
    const pB = Number.parseFloat(b.percent || '0')
    return pB - pA
  })
}

const handlePercentUpdate = (code: string, value: string) => {
  const item = localSpeciesList.value.find((s) => s.species === code)
  if (item) {
    item.percent = value
    emit('update:speciesList', cloneDeep(localSpeciesList.value))
  }
}

const handlePercentBlur = () => {
  sortByPercent()
  emit('update:speciesList', cloneDeep(localSpeciesList.value))
}

const handleDelete = (code: string) => {
  localSpeciesList.value = localSpeciesList.value.filter((s) => s.species !== code)
  emit('update:speciesList', cloneDeep(localSpeciesList.value))
}
</script>

<style scoped>
.species-list-input {
  width: 100%;
}

.empty-text {
  font: var(--typography-regular-small-body);
  color: var(--typography-color-danger);
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  padding: 6px 0 0 0;
}

.species-cards-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  grid-template-rows: repeat(3, auto);
  grid-auto-flow: column;
  gap: 10px;
}

@media (max-width: 599px) {
  .species-cards-grid {
    grid-template-columns: 1fr;
    grid-template-rows: unset;
    grid-auto-flow: row;
  }
}

@media (max-width: 700px) {
  .species-cards-grid {
    grid-template-columns: 1fr;
  }
}
</style>
