<template>
  <v-dialog
    v-model="dialog"
    persistent
    :max-width="options && options.width ? options.width : '400'"
  >
    <v-card>
      <v-card-title
        style="font-weight: 300 !important; padding-left: 30px !important"
        class="popup-header"
        >{{ title || 'VDYP Message' }}</v-card-title
      >
      <v-card-text
        v-show="Boolean(message)"
        class="pa-4"
        style="
          font-size: 14px;
          padding-left: 35px !important;
          padding-right: 35px !important;
          white-space: pre-line;
        "
      >
        {{ message }}
      </v-card-text>
      <v-card-actions
        class="pt-3"
        style="background-color: #f6f6f6; border-top: 1px solid #0000001f"
      >
        <v-spacer></v-spacer>
        <v-btn class="blue-btn ml-2" @click="agree">{{
          btnLabel || 'OK'
        }}</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useMessageDialogStore } from '@/stores/common/messageDialogStore'

const messageDialogStore = useMessageDialogStore()

const dialog = computed(() => messageDialogStore.isOpen)
const title = computed(() => messageDialogStore.dialogTitle || 'VDYP Message')
const message = computed(() => messageDialogStore.dialogMessage || '')
const btnLabel = computed(() => messageDialogStore.dialogBtnLabel || 'OK')
const options = computed(
  () => messageDialogStore.dialogOptions || { width: '400px' },
)

const agree = () => {
  messageDialogStore.agree()
}
</script>

<style scoped>
.v-card-text {
  padding: 1rem !important;
}
</style>
