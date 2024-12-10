<template>
  <v-container>
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
    <div
      id="err-msg-rslt-print-area"
      ref="errMsgRsltPrintAreaRef"
      class="ml-2 mr-2"
    >
      <v-virtual-scroll :items="items" :item-height="50" height="430px">
        <template #default="{ item }">
          <div
            style="
              display: flex;
              align-items: center;
              height: 30px;
              padding: 0 16px;
            "
          >
            {{ item }}
          </div>
        </template>
      </v-virtual-scroll>
    </div>
  </v-container>
</template>
<script setup lang="ts">
import { ref, computed } from 'vue'
import { useProjectionStore } from '@/stores/projectionStore'
import printJS from 'print-js'
import { saveAs } from 'file-saver'
import { Util } from '@/utils/util'

const projectionStore = useProjectionStore()
const items = computed(() => projectionStore.errorMessages)

const errMsgRsltPrintAreaRef = ref<HTMLElement | null>(null)

const download = () => {
  const content = items.value.join('\n')
  const blob = new Blob([content], { type: 'text/plain;charset=utf-8' })
  saveAs(blob, 'Output_Error.txt')
}

const print = () => {
  if (errMsgRsltPrintAreaRef.value) {
    // Create a temporary DOM
    const container = document.createElement('div')
    container.style.fontFamily =
      "'BCSans', 'Noto Sans', Verdana, Arial, sans-serif"
    container.style.fontSize = '12px'

    // Add a print-only header
    const header = document.createElement('h2')
    header.style.textAlign = 'center'
    header.style.marginBottom = '20px'
    header.textContent = 'View Error Message Results'
    container.appendChild(header)

    // traverse the v-virtual-scroll item directly and add it to the DOM
    items.value.forEach((item: any) => {
      const itemDiv = document.createElement('div')
      itemDiv.style.padding = '4px 16px'
      itemDiv.style.height = '30px'
      itemDiv.style.display = 'flex'
      itemDiv.style.alignItems = 'center'
      itemDiv.style.border = 'none'
      itemDiv.textContent = item
      container.appendChild(itemDiv)
    })

    // Define a print style
    const styles =
      Util.extractStylesFromDocument(document.styleSheets) +
      `
      @page {
        size: Letter portrait;
        margin: 7mm;
      }
      h2 {
        font-size: 16px;
        margin-bottom: 10px;
      }
      div {
        font-size: 12px;
      }
    `

    // Print a temporary DOM
    printJS({
      printable: container.innerHTML,
      type: 'raw-html',
      style: styles,
    })
  }
}
</script>
<style scoped></style>
