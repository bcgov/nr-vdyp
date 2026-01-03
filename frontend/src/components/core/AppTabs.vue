<template>
  <v-tabs
    :model-value="localCurrentTab"
    @update:model-value="handleTabUpdate"
    :hideSlider="true"
    :centerActive="true"
    :showArrows="true"
    height="60px"
    :key="tabKey"
  >
    <v-tab
      v-for="(tab, index) in tabs"
      :key="index"
      :class="{ 'first-tab': index === 0 }"
      :disabled="tab.disabled"
    >
      {{ tab.label }}
    </v-tab>
  </v-tabs>
  <v-tabs-window :model-value="localCurrentTab">
    <v-tabs-window-item
      v-for="(tab, index) in tabs"
      :key="index"
      :value="index"
    >
      <component :is="tab.component" :tabname="tab.tabname" />
    </v-tabs-window-item>
  </v-tabs-window>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import type { Tab } from '@/interfaces/interfaces'

const props = defineProps<{
  currentTab: number
  tabs: Tab[]
}>()

const emit = defineEmits(['update:currentTab'])

// Create a local copy of currentTab to avoid mutating props
const localCurrentTab = ref<number>(props.currentTab)
// Add a key to force re-render when currentTab changes
const tabKey = ref<number>(0)

// Watch for changes in props.currentTab to sync with localCurrentTab
watch(
  () => props.currentTab,
  (newValue) => {
    if (newValue !== localCurrentTab.value) {
      localCurrentTab.value = newValue
      tabKey.value += 1 // Force re-render of v-tabs
    }
  },
)

// Watch for changes in localCurrentTab and emit updates to the parent
watch(localCurrentTab, (newValue) => {
  emit('update:currentTab', newValue)
})

// Handle tab updates
const handleTabUpdate = (newValue: unknown) => {
  if (typeof newValue === 'number') {
    localCurrentTab.value = newValue
    emit('update:currentTab', newValue)
    tabKey.value += 1 // Force re-render of v-tabs
  }
}
</script>

<style scoped />
