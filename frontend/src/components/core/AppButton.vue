<template>
  <v-btn :class="customClass" :disabled="isDisabled" @click="onClick">
    {{ label }}
  </v-btn>
</template>

<script setup lang="ts">
const props = defineProps({
  label: {
    type: String,
    required: true,
  },
  customClass: {
    type: String,
    default: '',
  },
  isDisabled: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits<(e: 'click', id: number) => void>()
const onClick = (event: Event) => {
  if (!props.isDisabled) {
    event.preventDefault() // preventing default behavior
    event.stopPropagation() // preventing event propagation
    emit('click', 1)
  } else {
    console.debug('Button is disabled, onClick event not triggered')
  }
}
</script>

<style scoped>
.blue-btn {
  background: #003366 !important;
  color: #ffffff !important;
  letter-spacing: 0.25px;
  padding-left: 25px;
  padding-right: 25px;
  font-weight: 500;
  box-shadow: none !important;
  text-transform: none;
}

.white-btn {
  background: #ffffff !important;
  color: #003366 !important;
  letter-spacing: 0.25px;
  border: 1px solid #003366;
  padding-left: 25px;
  padding-right: 25px;
  font-weight: 500;
  box-shadow: none !important;
  text-transform: none;
}

.v-btn--disabled.v-btn--variant-elevated,
.v-btn--disabled.v-btn--variant-flat {
  opacity: 0.26;
}
</style>
