<template>
  <div>
    <v-row class="mt-n5">
      <v-col cols="3" sm="3" md="3">
        <span class="search-label">Model</span>
        <v-select
          :items="modelSelectionOptions"
          v-model="modelSelection"
          item-title="label"
          item-value="value"
          hide-details
          dense
          density="compact"
          variant="outlined"
          style="background-color: white !important"
        ></v-select>
      </v-col>
    </v-row>
  </div>
</template>
<script setup lang="ts">
import { useAppStore } from '@/stores/appStore'
import { storeToRefs } from 'pinia'
import { modelSelectionOptions } from '@/constants/options'

const appStore = useAppStore()
const { modelSelection } = storeToRefs(appStore)
</script>

<style scoped>
.no-padding {
  padding: 0px;
}
.no-padding-bottom {
  padding-bottom: 0px;
}
</style>
