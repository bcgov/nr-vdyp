<template>
  <v-col
    cols="6"
    v-if="speciesGroups.length > 0"
    class="species-groups-container"
    data-testid="species-groups-container"
  >
    <div
      v-for="(group, index) in speciesGroups"
      :key="index"
      data-testid="species-group-row"
    >
      <v-row>
        <v-col cols="4" sm="4" md="4" data-testid="species-group-column">
          <label class="bcds-text-field-label" :for="`species-group-${index}`">Species Group</label>
          <v-text-field
            :id="`species-group-${index}`"
            label="Species Group"
            :model-value="group.group"
            variant="underlined"
            disabled
          ></v-text-field>
        </v-col>
        <v-col
          cols="4"
          sm="4"
          md="4"
          data-testid="species-group-percent-column"
        >
          <label class="bcds-text-field-label" :for="`species-group-percent-${index}`">Species Group Percent</label>
          <v-text-field
            :id="`species-group-percent-${index}`"
            label="Species Group Percent"
            :model-value="group.percent"
            variant="underlined"
            disabled
          ></v-text-field>
        </v-col>
        <v-col cols="4" sm="4" md="4" data-testid="site-species-column">
          <label class="bcds-text-field-label" :for="`site-species-${index}`">Site Species</label>
          <v-text-field
            :id="`site-species-${index}`"
            label="Site Species"
            :model-value="group.siteSpecies"
            variant="underlined"
            disabled
          ></v-text-field>
        </v-col>
      </v-row>
      <div class="hr-line"></div>
    </div>
  </v-col>
  <v-col class="species-groups-no-data-container" cols="6" v-else>
    <div class="mt-2">
      <v-row>
        <v-col cols="4" sm="4" md="4">
          <label class="bcds-text-field-label" for="species-group-no-data">Species Group</label>
          <v-text-field
            id="species-group-no-data"
            label="Species Group"
            variant="underlined"
            disabled
            persistent-placeholder
            placeholder=""
          ></v-text-field>
        </v-col>
        <v-col cols="4" sm="4" md="4">
          <label class="bcds-text-field-label" for="species-group-percent-no-data">Species Group Percent</label>
          <v-text-field
            id="species-group-percent-no-data"
            label="Species Group Percent"
            variant="underlined"
            disabled
            persistent-placeholder
            placeholder=""
          ></v-text-field>
        </v-col>
        <v-col cols="4" sm="4" md="4">
          <label class="bcds-text-field-label" for="site-species-no-data">Site Species</label>
          <v-text-field
            id="site-species-no-data"
            label="Site Species"
            variant="underlined"
            disabled
            persistent-placeholder
            placeholder=""
          ></v-text-field>
        </v-col>
      </v-row>
    </div>
  </v-col>
</template>

<script setup lang="ts">
import type { SpeciesGroup } from '@/interfaces/interfaces'

defineProps({
  speciesGroups: {
    type: Array as () => SpeciesGroup[],
    required: true,
  },
})
</script>

<style scoped>
.species-groups-container {
  padding-top: 8px;
}
.species-groups-no-data-container {
  padding-top: 0px;
}
</style>

