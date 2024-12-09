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
    <div id="mdl-rpt-print-area" ref="mdlRptPrintAreaRef" class="ml-2 mr-2">
      <h3>Job ID 2024-04-07</h3>
      <p class="mt-3" style="font-size: 18px">
        Lodgepole Pine 30.0%, Poplar 30.0%, Hemlock 30.0%, Spruce 10.0%
      </p>
      <v-table :dense="true" class="styled-table">
        <thead>
          <tr>
            <th colspan="6" scope="colgroup">Quad</th>
            <th rowspan="2" scope="col">
              Whole Stem Volume (m<sup>3</sup>/ha)
            </th>
          </tr>
          <tr>
            <th scope="col">Total Age</th>
            <th scope="col">Site HT (m)</th>
            <th scope="col">Lorey HT (m)</th>
            <th scope="col">Stand DIA (cm)</th>
            <th scope="col">BA (m<sup>2</sup>/ha)</th>
            <th scope="col">TPH (trees/ha)</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in tableData" :key="row.age">
            <td>{{ row.age }}</td>
            <td :style="{ color: row.age === 25 ? 'red' : 'inherit' }">
              {{ row.loreyHt }}
            </td>
            <td>{{ row.standHt }}</td>
            <td>{{ row.standDia }}</td>
            <td>{{ row.ba }}</td>
            <td>{{ row.tph }}</td>
            <td>{{ row.volume }}</td>
          </tr>
        </tbody>
      </v-table>
      <p class="mt-3">
        NOTE: Height 7.7 at Projection Age 25.0 is too short to generate yields
        for species 'PL'
      </p>
      <p>
        NOTE: Projected data for species 'PL' was not generated at stand age
        25.0
      </p>
      <p>NOTE: Basal Area and Trees per HA computed using Default CC of 50%</p>
      <p>NOTE: Yields are not predicted prior to age 50.</p>
    </div>
  </v-container>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import printJS from 'print-js'
import { saveAs } from 'file-saver'
import { Util } from '@/utils/util'

const mdlRptPrintAreaRef = ref<HTMLElement | null>(null)

const tableData = ref([
  {
    age: 0,
    loreyHt: '0.0',
    standHt: '',
    standDia: '',
    ba: '',
    tph: '',
    volume: '',
  },
  {
    age: 25,
    loreyHt: '7.7',
    standHt: '',
    standDia: '',
    ba: '',
    tph: '',
    volume: '',
  },
  {
    age: 50,
    loreyHt: '15.0',
    standHt: 10.9,
    standDia: 12.3,
    ba: 10.6,
    tph: 895,
    volume: 44.2,
  },
  {
    age: 75,
    loreyHt: '19.4',
    standHt: 16.3,
    standDia: 17.8,
    ba: 26.1,
    tph: 1054,
    volume: 165,
  },
  {
    age: 100,
    loreyHt: '22.0',
    standHt: 20.1,
    standDia: 20.6,
    ba: 32.0,
    tph: 959,
    volume: 253.2,
  },
  {
    age: 125,
    loreyHt: '23.8',
    standHt: 23.0,
    standDia: 23.6,
    ba: 35.8,
    tph: 815,
    volume: 325.7,
  },
  {
    age: 150,
    loreyHt: '25.0',
    standHt: 25.3,
    standDia: 27.4,
    ba: 37.1,
    tph: 631,
    volume: 369.5,
  },
  {
    age: 175,
    loreyHt: '25.8',
    standHt: 26.6,
    standDia: 30.4,
    ba: 36.7,
    tph: 507,
    volume: 382.2,
  },
  {
    age: 200,
    loreyHt: '26.5',
    standHt: 27.2,
    standDia: 32.4,
    ba: 36.5,
    tph: 444,
    volume: 385.7,
  },
  {
    age: 225,
    loreyHt: '26.9',
    standHt: 27.2,
    standDia: 33.5,
    ba: 36.6,
    tph: 416,
    volume: 385.8,
  },
  {
    age: 250,
    loreyHt: '27.3',
    standHt: 27.3,
    standDia: 34.4,
    ba: 36.7,
    tph: 394,
    volume: 386.3,
  },
])

const download = () => {
  const content = 'Hello, this is your text file!'
  const blob = new Blob([content], { type: 'text/plain;charset=utf-8' })

  saveAs(blob, 'model-report.txt')
}

const print = () => {
  if (mdlRptPrintAreaRef.value) {
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
      printable: mdlRptPrintAreaRef.value.id,
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
.styled-table tbody tr:first-child td {
  border-top: 2px solid #dfdcdc !important;
}
</style>
