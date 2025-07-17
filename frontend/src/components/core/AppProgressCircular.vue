<template>
  <div
    class="centered-progress progress-wrapper"
    v-if="isShow"
    :style="{
      backgroundColor: hasBackground ? backgroundColor : 'transparent',
      boxShadow: hasBackground ? '0 2px 4px rgba(0, 0, 0, 0.1)' : 'none',
      borderRadius: `${borderRadius}px`,
      padding: `${padding}px`,
    }"
  >
    <v-progress-circular
      indeterminate
      :size="circleSize"
      :width="circleWidth"
      :color="circleColor"
    ></v-progress-circular>
    <div v-if="showMessage" class="message">
      {{ message }}
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps({
  isShow: { type: Boolean, default: false },
  showMessage: { type: Boolean, default: true },
  message: { type: String, default: 'Loading...' },
  circleSize: { type: Number, default: 70 },
  circleWidth: { type: Number, default: 5 },
  circleColor: { type: String, default: 'primary' },
  backgroundColor: { type: String, default: 'rgba(255, 255, 255, 0.8)' },
  hasBackground: { type: Boolean, default: true },
  padding: { type: Number, default: 20 },
  borderRadius: { type: Number, default: 10 },
})
</script>

<style scoped>
.centered-progress {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 9999;
}
.progress-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}
.message {
  font-weight: bold;
  color: rgba(0, 0, 0, 0.87);
  margin-top: 10px;
}
</style>
