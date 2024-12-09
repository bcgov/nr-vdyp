<template>
  <v-container fluid min-height="600px">
    <v-card
      elevation="0"
      style="
        display: flex;
        justify-content: end;
        align-items: end;
        text-align: end;
      "
      class="mr-2"
    >
      <v-card-actions class="pr-0">
        <v-spacer></v-spacer>
        <v-btn class="white-btn" @click="download">Download</v-btn>
        <v-btn class="blue-btn ml-2" @click="print">Print</v-btn>
      </v-card-actions>
    </v-card>
    <div id="log-file-print-area" ref="logFilePrintAreaRef" class="ml-2 mr-2">
      <h2 class="mt-5 mb-10">Log Files</h2>
      <div
        v-for="(group, groupIndex) in chunkedLogData"
        :key="groupIndex"
        dense
        class="v-row v-row--dense"
      >
        <template v-for="(item, index) in group" :key="index">
          <!-- data column -->
          <div data-v-6e428c2f="" class="v-col-md-2 v-col-12 mb-5">
            <div style="border-bottom: 1px solid rgb(223, 220, 220)">
              <div
                class="mb-1 pl-3 pb-1 ml-3 readonly-label"
                :title="item.label"
                style="
                  white-space: nowrap;
                  overflow: hidden;
                  text-overflow: ellipsis;
                "
              >
                {{ item.label }}
                <div class="pl-0 readonly-text">{{ item.value }}</div>
              </div>
            </div>
          </div>
          <!-- empty column: Insert Except Last Column -->
          <div
            v-if="index < group.length - 1"
            class="v-col-md-1 v-col-12"
            style="max-width: 3%"
          ></div>
        </template>
      </div>
      <h2 class="mt-5">Species Parameters</h2>
      <div
        class="v-table v-theme--defaultTheme v-table--density-default styled-table"
        dense="true"
      >
        <div class="v-table__wrapper">
          <table>
            <thead>
              <tr>
                <th scope="col">Species</th>
                <th scope="col">% Comp</th>
                <th scope="col">Tot Age</th>
                <th scope="col">BH Age</th>
                <th scope="col">Height</th>
                <th scope="col">SI</th>
                <th scope="col">YTBH</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in speciesParams" :key="row.species">
                <td>{{ row.species }}</td>
                <td>{{ row.percentComp }}</td>
                <td>{{ row.totAge }}</td>
                <td>{{ row.bhAge }}</td>
                <td>{{ row.height }}</td>
                <td>{{ row.si }}</td>
                <td>{{ row.ytbh }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
      <h2 class="mt-5">Site Index Curves Used</h2>
      <div
        class="v-table v-theme--defaultTheme v-table--density-default styled-table"
        dense="true"
        style="width: 50% !important"
      >
        <div class="v-table__wrapper">
          <table>
            <thead>
              <tr>
                <th scope="col">Age Range</th>
                <th scope="col">Species</th>
                <th scope="col">SI Curve Name</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in siCurve" :key="row.species">
                <td>{{ row.ageRange }}</td>
                <td>{{ row.species }}</td>
                <td>{{ row.curveName }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
      <h2 class="mt-5">Additional Stand Attributes</h2>
      <div
        v-if="addtStandAttrs.length > 0"
        class="v-table v-theme--defaultTheme v-table--density-default styled-table mb-3"
        dense="true"
      >
        <div class="v-table__wrapper">
          <table>
            <thead>
              <tr>
                <th scope="col">Lorey Height (7.5cm+)</th>
                <th scope="col">Vol-Whole Stem (7.5cm+)</th>
                <th scope="col">Basal Area (12.5cm+)</th>
                <th scope="col">Vol-Whole Stem (12.5cm+)</th>
                <th scope="col">Vol-Close Util (12.5cm+)</th>
                <th scope="col">Vol-Decay Only (12.5cm+)</th>
                <th scope="col">Vol-Decay/Waste (12.5cm+)</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, index) in addtStandAttrs" :key="index">
                <td>{{ row.loreyHeight }}</td>
                <td>{{ row.wholeStemVol75 }}</td>
                <td>{{ row.basalArea125 }}</td>
                <td>{{ row.wholeStemVol125 }}</td>
                <td>{{ row.cuVol }}</td>
                <td>{{ row.cuNetDecayVol }}</td>
                <td>{{ row.cuNetDecayWasteVol }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
      <div
        v-else
        class="v-table v-theme--defaultTheme v-table--density-default styled-table"
        dense="true"
        style="width: 10% !important; text-align: center; font-weight: bold"
      >
        <div class="v-table__wrapper">
          <table>
            <thead>
              <tr>
                <th scope="col">None Applied</th>
              </tr>
            </thead>
          </table>
        </div>
      </div>
    </div>
  </v-container>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import printJS from 'print-js'
import { saveAs } from 'file-saver'
import AddtStandAttrs from '@/models/addtStandAttrs'
import SpeciesParam from '@/models/speciesParam'
import SICurve from '@/models/siCurve'
import { SPECIAL_INDICATORS } from '@/constants/constants'
import { Util } from '@/utils/util'

const logFilePrintAreaRef = ref<HTMLElement | null>(null)

const logData = [
  { label: '% Stockable Area Supplied', value: '55%' },
  { label: 'CFS Eco Zone', value: SPECIAL_INDICATORS.NOT_USED },
  { label: 'Trees Per Hectare', value: SPECIAL_INDICATORS.NOT_USED },
  { label: 'Measured Basal Area', value: SPECIAL_INDICATORS.NOT_USED },
  { label: 'Species 1', value: 'PL (30.0%)' },
  { label: 'Species 2', value: 'AC (30.0%)' },
  { label: 'Species 3', value: 'H (30.0%)' },
  { label: 'Species 4', value: 'S (10.0%)' },
  { label: 'Starting Total Age', value: '0' },
  { label: 'Finishing Total Age', value: '250' },
  { label: 'Age Increment', value: '25' },
  { label: 'Projected Values', value: 'Volume' },
  { label: 'FIP Calc Mode', value: '1' },
  { label: 'BEC Zone', value: 'IDF' },
  { label: 'Incl Second Species Ht', value: SPECIAL_INDICATORS.NA },
  { label: '% Crown Closure Supplied', value: '0' },
  { label: 'Min DBH Limit: PL', value: '7.5 cm+' },
  { label: 'Min DBH Limit: AC', value: '7.5 cm+' },
  { label: 'Min DBH Limit: H', value: '7.5 cm+' },
  { label: 'Min DBH Limit: S', value: '7.5 cm+' },
]

// Bundle data into groups of four
const chunkedLogData = logData.reduce(
  (acc, _, i) => {
    if (i % 4 === 0) acc.push(logData.slice(i, i + 4))
    return acc
  },
  [] as Array<Array<{ label: string; value: string }>>,
)

const speciesParams = [
  new SpeciesParam({
    species: 'PL',
    percentComp: '30.0',
    totAge: '60',
    bhAge: '54',
    height: '17.00',
    si: '16.30',
    ytbh: '6.80',
  }),
  new SpeciesParam({
    species: 'AC',
    percentComp: '30.0',
    totAge: 'N/A',
    bhAge: 'N/A',
    height: 'N/A',
    si: 'N/A',
    ytbh: 'N/A',
  }),
  new SpeciesParam({
    species: 'H',
    percentComp: '20.0',
    totAge: 'N/A',
    bhAge: 'N/A',
    height: '12.52',
    si: '8.80',
    ytbh: 'N/A',
  }),
  new SpeciesParam({
    species: 'S',
    percentComp: '10.0',
    totAge: 'N/A',
    bhAge: 'N/A',
    height: 'N/A',
    si: 'N/A',
    ytbh: 'N/A',
  }),
  new SpeciesParam({
    species: 'BL',
    percentComp: '5.0',
    totAge: 'N/A',
    bhAge: 'N/A',
    height: 'N/A',
    si: 'N/A',
    ytbh: 'N/A',
  }),
  new SpeciesParam({
    species: 'CW',
    percentComp: '5.0',
    totAge: 'N/A',
    bhAge: 'N/A',
    height: 'N/A',
    si: 'N/A',
    ytbh: 'N/A',
  }),
]

const siCurve = [
  new SICurve({
    ageRange: '50 - 250',
    species: 'Hwi',
    curveName: '37 - Nigh (1998)',
  }),
]

const addtStandAttrs = [
  new AddtStandAttrs({
    loreyHeight: '11.93',
    wholeStemVol75: '60.1',
    basalArea125: '4.8554',
    wholeStemVol125: '35.0',
    cuVol: '24.9',
    cuNetDecayVol: '23.8',
    cuNetDecayWasteVol: '23.4',
  }),
]

const download = () => {
  const content = 'Hello, this is your text file!'
  const blob = new Blob([content], { type: 'text/plain;charset=utf-8' })

  saveAs(blob, 'view-error-message.txt')
}

const print = () => {
  if (logFilePrintAreaRef.value) {
    const styles =
      Util.extractStylesFromDocument(document.styleSheets) +
      `
      @page {
        size: Letter portrait;
        margin: 7mm;
      }
      .v-col-md-2 { flex: 0 0 16.6666666667%; max-width: 16.6666666667%; }
      .readonly-label { font-size: 10px; }
      .readonly-text { font-size: 10px; }
      html, body { font-family: 'BCSans', 'Noto Sans', Verdana, Arial, sans-serif; font-size: 10px; font-weight: 400;}
    `

    printJS({
      printable: logFilePrintAreaRef.value.id,
      type: 'html',
      scanStyles: false,
      style: styles,
    })
  }
}
</script>
<style scoped>
.styled-table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 20px;
  border-bottom: 1px solid #dfdcdc;
}

/* table header */
.v-table > .v-table__wrapper > table > tbody > tr > th,
.v-table > .v-table__wrapper > table > thead > tr > th,
.v-table > .v-table__wrapper > table > tfoot > tr > th {
  height: 40px;
  font-weight: 700;
  user-select: all;
  text-align: center;
  border: 1px solid #dfdcdc;
}

.styled-table td {
  border: 1px solid #dfdcdc;
  padding: 0px;
  text-align: center;
}

/* table td */
.v-table > .v-table__wrapper > table > tbody > tr > td,
.v-table > .v-table__wrapper > table > thead > tr > td,
.v-table > .v-table__wrapper > table > tfoot > tr > td {
  height: 30px;
}

/* Make the line between thead and tbody bold */
.v-table > .v-table__wrapper > table > thead tr th {
  border-bottom: 2px solid #dfdcdc;
}
</style>
