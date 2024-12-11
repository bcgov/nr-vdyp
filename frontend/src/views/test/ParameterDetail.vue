<template>
  <div>
    <p>Root:</p>
    <ul v-if="rootDetails">
      <li v-for="link in rootDetails.links" :key="link.href">
        <strong>{{ link.rel }}:</strong> {{ link.method }} - {{ link.href }}
      </li>
    </ul>
    <p v-else>No data available.</p>
    <p class="mt-10">Parameter Details:</p>
    <pre v-if="helpDetails">{{ helpDetails }}</pre>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import type { ParameterDetailsMessage, RootResource } from '@/services/vdyp-api'
import { helpGet, rootGet } from '@/services/apiActions'

const helpDetails = ref<ParameterDetailsMessage[] | null>(null)
const rootDetails = ref<RootResource | null>(null)

onMounted(async () => {
  try {
    rootDetails.value = await rootGet()
    console.log('Root Details:', rootDetails.value)

    const response = await helpGet()
    // console.log('Received help details:', response)
    helpDetails.value = response

    if (helpDetails.value && 'helpMessages' in helpDetails.value) {
      // console.log('helpDetails:', JSON.stringify(helpDetails.value, null, 2))
      const messages = (helpDetails.value as any).helpMessages
      if (Array.isArray(messages)) {
        console.log('Iterating over Help Details:')
        for (const detail of messages) {
          console.log(
            `Field: ${detail.field}, Short-Description: ${detail.shortDescription}, Parameter-Value: ${detail.parameterValue}, Long-Description: ${detail.longDescription}, Default-Value: ${detail.defaultValue}`,
          )
        }
      } else {
        console.error('helpMessages is not an array:', messages)
      }
    } else {
      console.error('helpMessages property does not exist on helpDetails.value')
    }
  } catch (error) {
    console.error('Error fetching help details:', error)
  }
})
</script>
<style scoped>
.file-upload-run-model-card {
  padding-bottom: 16px !important;
  background-color: #f6f6f6;
  border-top: 1px solid #0000001f;
  border-top-left-radius: 0px;
  border-top-right-radius: 0px;
  border-bottom-left-radius: 3px;
  border-bottom-right-radius: 3px;
  display: flex;
  justify-content: end;
  align-items: end;
  text-align: end;
}
</style>
