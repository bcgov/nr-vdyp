<template>
  <v-dialog
    v-model="dialog"
    persistent
    :max-width="options && options.width ? options.width : '400px'"
  >
    <v-card>
      <v-card-title
        style="font-weight: 300 !important; padding-left: 30px !important"
        class="popup-header"
        >{{ title || 'VDYP Confirm Dialog' }}</v-card-title
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
        >{{ message || 'No message available.' }}</v-card-text
      >
      <v-card-actions
        class="pt-3"
        style="background-color: #f6f6f6; border-top: 1px solid #0000001f"
      >
        <v-spacer></v-spacer>
        <v-btn
          v-if="options && !options.noconfirm"
          class="white-btn"
          @click="cancel"
          >No</v-btn
        >
        <v-btn class="blue-btn ml-2" @click="agree">Yes</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useConfirmDialogStore } from '@/stores/common/confirmDialogStore'

const confirmDialogStore = useConfirmDialogStore()

const dialog = computed(() => confirmDialogStore.isOpen)
const title = computed(
  () => confirmDialogStore.dialogTitle || 'VDYP Confirm Dialog',
)
const message = computed(() => confirmDialogStore.dialogMessage || '')
const options = computed(
  () =>
    confirmDialogStore.dialogOptions || { width: '600px', noconfirm: false },
)

const agree = () => {
  confirmDialogStore.agree()
}

const cancel = () => {
  confirmDialogStore.cancel()
}
</script>

<style scoped>
.v-card-text {
  padding: 1rem !important;
}
</style>
