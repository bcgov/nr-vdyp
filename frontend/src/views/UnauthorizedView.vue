<template>
  <v-empty-state icon="mdi-lock" class="mt-10 empty-state-title" title="VDYP Unauthorized Page">
    <div class="mt-4 empty-state-content">
      <p>An authorization error has occurred.</p>
      <p>Required role does not exist.</p>
    </div>
  </v-empty-state>
</template>
<script setup lang="ts"></script>
<style scoped>
.empty-state-title {
  font-size: 1.5rem;
  font-weight: 600;
}

.empty-state-content {
  font-size: 1rem;
  font-weight: 500;
}
</style>
