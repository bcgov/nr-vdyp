<template>
  <v-dialog v-model="getIsOpen" persistent :max-width="getDialogOptions.width">
    <v-card :style="computedDialogStyle">
      <v-card-title :style="computedHeaderStyle" class="popup-header">{{
        getDialogTitle
      }}</v-card-title>
      <v-card-text
        v-show="Boolean(getDialogMessage)"
        class="pa-4"
        style="
          font-size: 14px;
          padding-left: 35px !important;
          padding-right: 35px !important;
          white-space: pre-line;
        "
        >{{ getDialogMessage }}</v-card-text
      >
      <v-card-actions
        class="pt-3"
        :style="{
          backgroundColor: computedActionsBackground,
          borderTop: '1px solid #0000001f',
        }"
      >
        <v-spacer></v-spacer>
        <AppButton
          :label="computedNoBtnLabel"
          customClass="white-btn ml-2"
          @click="cancel"
        />
        <AppButton
          :label="computedYesBtnLabel"
          customClass="blue-btn ml-2"
          @click="agree"
        />
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script setup lang="ts">
import AppButton from '@/components/core/AppButton.vue'
import { CONSTANTS } from '@/constants'
import { computed } from 'vue'
import { storeToRefs } from 'pinia'
import { useConfirmDialogStore } from '@/stores/common/confirmDialogStore'

const confirmDialogStore = useConfirmDialogStore()

const { getIsOpen, getDialogTitle, getDialogMessage, getDialogOptions } =
  storeToRefs(confirmDialogStore)

const computedYesBtnLabel = computed(() => CONSTANTS.BUTTON_LABEL.CONF_YES)
const computedNoBtnLabel = computed(() => CONSTANTS.BUTTON_LABEL.CONF_NO)

const computedHeaderStyle = computed(() => ({
  fontWeight: '300',
  paddingLeft: '30px',
  padding: '1rem',
  background: '#003366',
  color: '#ffffff',
}))

const computedActionsBackground = computed(() => '#f6f6f6')

const computedDialogStyle = computed(() => {
  return {
    borderRadius: '8px',
  }
})

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
