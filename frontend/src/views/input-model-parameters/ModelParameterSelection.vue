<template>
  <SpeciesInfo />
  <!-- <SiteInfo v-if="currentStep === 2" />
  <StandDensity v-if="currentStep === 3" /> -->
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useStepStore } from '@/stores/stepStore'

const stepStore = useStepStore()
const currentStep = computed(() => stepStore.currentStep)

import SpeciesInfo from '@/components/model-param-selection-panes/SpeciesInfo.vue'
import SiteInfo from '@/components/model-param-selection-panes/SiteInfo.vue'
import StandDensity from '@/components/model-param-selection-panes/StandDensity.vue'
</script>

<style scoped>
/* .v-row > div {
  line-height: 36px;
  font-size: 16px;
} */
</style>
