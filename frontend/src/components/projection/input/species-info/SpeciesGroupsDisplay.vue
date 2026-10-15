<template>
  <v-col
    v-if="groupedSummary.length > 0"
    cols="12"
    lg="4"
    class="species-summary-container d-flex flex-column"
    data-testid="species-groups-container"
  >
    <div class="summary-box">
    <div class="summary-title text-body-2 font-weight-bold mb-2">Species Summary</div>
    <table class="summary-table" aria-label="Species Summary">
      <thead>
        <tr>
          <th class="text-caption">Species Group ({{ groupCount }})</th>
          <th class="text-caption">Site Species ({{ siteSpeciesCount }})</th>
          <th class="text-caption text-right">Percentage</th>
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="row in groupedSummary"
          :key="row.group"
          data-testid="species-group-row"
        >
          <td class="text-body-2" data-testid="species-group-column">{{ row.group }}</td>
          <td class="text-body-2" data-testid="site-species-column">{{ row.siteSpecies.join(', ') }}</td>
          <td class="text-body-2 text-right" data-testid="species-group-percent-column">
            {{ row.percent.toFixed(1) }}%
          </td>
        </tr>
      </tbody>
      <tfoot>
        <tr class="summary-total-row">
          <td colspan="2" class="text-body-2 text-right pr-3">Total:</td>
          <td class="text-body-2 text-right font-weight-bold">{{ totalPercent }}</td>
        </tr>
      </tfoot>
    </table>
    </div>
  </v-col>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { SpeciesGroupSummary } from '@/interfaces/interfaces'

const props = defineProps({
  groupedSummary: {
    type: Array as () => SpeciesGroupSummary[],
    required: true,
  },
})

const groupCount = computed(() => props.groupedSummary.length)

const siteSpeciesCount = computed(() =>
  props.groupedSummary.reduce((count, row) => count + row.siteSpecies.length, 0),
)

const totalPercent = computed(() => {
  const total = props.groupedSummary.reduce(
    (acc, item) => acc + item.percent,
    0,
  )
  return total.toFixed(1) + '%'
})
</script>

<style scoped>
.species-summary-container {
  padding-top: 0;
}

.summary-box {
  border: 1px solid rgba(0, 0, 0, 0.2);
  border-radius: 8px;
  padding: 12px 16px;
  flex: 1;
}

.summary-title {
  color: rgba(0, 0, 0, 0.87);
}

.summary-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.summary-table th {
  color: rgba(0, 0, 0, 0.6);
  font-weight: 500;
  text-align: left;
  padding: 4px 8px 6px 4px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.15);
}

.summary-table th:last-child,
.summary-table td:last-child {
  text-align: right;
  padding-right: 4px;
}

.summary-table td {
  padding: 5px 8px 5px 4px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  vertical-align: top;
}

.summary-table tbody tr:last-child td {
  border-bottom: 1px solid rgba(0, 0, 0, 0.15);
}

.summary-total-row td {
  border-bottom: none;
  padding-top: 6px;
}
</style>
