<template>
  <v-snackbar
    v-model="isVisibleLocal"
    :timeout="computedAutoTimeout ? computedTimeout : -1"
    :color="computedColor"
    :rounded="computedRounded"
    :location="computedLocation"
    :show-timer="showTimer"
    class="elevation-12"
    @update:model-value="onClose"
    @click:outside="onClose"
  >
    <div class="d-flex align-center">
      <v-icon class="mr-2">{{ getIcon(computedType) }}</v-icon>
      <span>{{ computedMessage }}</span>
    </div>

    <template #actions>
      <v-btn icon variant="text" @click="onClose">
        <v-icon>mdi-close</v-icon>
      </v-btn>
    </template>
  </v-snackbar>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'

const props = defineProps<{
  isVisible?: boolean
  message?: string
  type?: 'info' | 'success' | 'error' | 'warning' | ''
  color?: string
  location?: 'top' | 'center' | 'bottom' | 'right' | 'left'
  timeout?: number
  autoTimeout?: boolean
  showTimer?: boolean
  rounded?: boolean | string
}>()

const emit = defineEmits(['update:isVisible', 'close'])

const isVisibleLocal = ref(props.isVisible)

watch(
  () => props.isVisible,
  (newVal) => {
    isVisibleLocal.value = newVal
  },
)

const computedMessage = computed(() => props.message ?? 'Notification message')
const computedType = computed(() => props.type ?? 'info')
const computedColor = computed(() => props.color ?? 'info')
const computedLocation = computed(() => props.location ?? 'top')
const computedTimeout = computed(() => props.timeout ?? 5000)
const computedAutoTimeout = computed(() => props.autoTimeout ?? true)
const computedRounded = computed(() => props.rounded ?? true)
const showTimer = computed(() => props.showTimer ?? false)

const onClose = () => {
  isVisibleLocal.value = false
  emit('update:isVisible', false)
  emit('close')
}

const getIcon = (type: string): string => {
  const iconMap: { [key: string]: string } = {
    info: 'mdi-information',
    success: 'mdi-check-circle',
    error: 'mdi-alert-circle',
    warning: 'mdi-alert',
    '': 'mdi-information',
  }
  return iconMap[type] ?? 'mdi-information'
}
</script>

<style scoped>
.elevation-12 {
  border-radius: 8px;
}
</style>
