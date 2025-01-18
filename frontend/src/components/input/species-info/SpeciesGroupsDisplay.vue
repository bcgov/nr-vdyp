<template>
  <v-col
    cols="6"
    v-if="speciesGroups.length > 0"
    data-testid="species-groups-container"
  >
    <div
      v-for="(group, index) in speciesGroups"
      :key="index"
      class="mt-2"
      data-testid="species-group-row"
    >
      <v-row>
        <v-col cols="4" sm="4" md="4" data-testid="species-group-column">
          <v-text-field
            label="Species Group"
            :model-value="group.group"
            variant="underlined"
            disabled
            density="compact"
            dense
          ></v-text-field>
        </v-col>
        <v-col
          cols="4"
          sm="4"
          md="4"
          data-testid="species-group-percent-column"
        >
          <v-text-field
            label="Species Group Percent"
            :model-value="group.percent"
            variant="underlined"
            disabled
            density="compact"
            dense
          ></v-text-field>
        </v-col>
        <v-col cols="4" sm="4" md="4" data-testid="site-species-column">
          <v-text-field
            label="Site Species"
            :model-value="group.siteSpecies"
            variant="underlined"
            disabled
            density="compact"
            dense
          ></v-text-field>
        </v-col>
      </v-row>
      <div class="hr-line mb-3"></div>
    </div>
  </v-col>
  <v-col cols="6" v-else>
    <div class="mt-2">
      <v-row>
        <v-col cols="4" sm="4" md="4">
          <v-text-field
            label="Species Group"
            variant="underlined"
            disabled
            persistent-placeholder
            placeholder=""
            density="compact"
            dense
          ></v-text-field>
        </v-col>
        <v-col cols="4" sm="4" md="4">
          <v-text-field
            label="Species Group Percent"
            variant="underlined"
            disabled
            persistent-placeholder
            placeholder=""
            density="compact"
            dense
          ></v-text-field>
        </v-col>
        <v-col cols="4" sm="4" md="4">
          <v-text-field
            label="Site Species"
            variant="underlined"
            disabled
            persistent-placeholder
            placeholder=""
            density="compact"
            dense
          ></v-text-field>
        </v-col>
      </v-row>
    </div>
  </v-col>
</template>

<script setup lang="ts">
import { defineProps } from 'vue'
import type { SpeciesGroup } from '@/interfaces/interfaces'

defineProps({
  speciesGroups: {
    type: Array as () => SpeciesGroup[],
    required: true,
  },
})
</script>

<style scoped></style>
