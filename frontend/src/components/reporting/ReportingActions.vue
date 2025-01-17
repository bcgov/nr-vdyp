<template>
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
      <AppButton
        label="Print"
        :isDisabled="isButtonDisabled"
        customClass="blue-btn"
        @click="handlePrint"
      />
      <v-spacer></v-spacer>
      <AppButton
        label="Download"
        :isDisabled="isButtonDisabled"
        customClass="white-btn"
        @click="handleDownload"
      />
    </v-card-actions>
  </v-card>
</template>

<script setup lang="ts">
import AppButton from '@/components/core/AppButton.vue'

defineProps({
  isButtonDisabled: {
    type: Boolean,
    required: true,
  },
})

const emit = defineEmits<(e: 'print' | 'download') => void>()

const handlePrint = () => {
  emit('print')
}

const handleDownload = () => {
  emit('download')
}
</script>
