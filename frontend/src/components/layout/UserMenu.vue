<template>
  <v-menu offset-y>
    <template v-slot:activator="{ props }">
      <v-btn v-bind="props" class="d-flex align-center header-user-button">
        <v-icon class="header-user-icon">{{ userIcon }}</v-icon>
        <span class="header-user-name">
          {{ displayName }}
        </span>
      </v-btn>
    </template>
    <v-list>
      <v-list-item @click="logout">
        <v-list-item-title>{{ logoutText }}</v-list-item-title>
      </v-list-item>
    </v-list>
  </v-menu>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useAuthStore } from '@/stores/common/authStore'

const props = defineProps({
  userIcon: {
    type: String,
    default: 'mdi-account-circle',
  },
  givenName: {
    type: String,
    default: null,
  },
  familyName: {
    type: String,
    default: null,
  },
  guestName: {
    type: String,
    default: 'Guest',
  },
  logoutText: {
    type: String,
    default: 'Logout',
  },
})

const authStore = useAuthStore()
const userInfo = computed(() => authStore.getParsedIdToken())

const displayName = computed(() => {
  if (userInfo.value || props.givenName || props.familyName) {
    const givenName = props.givenName ?? userInfo.value?.given_name ?? ''
    const familyName = props.familyName ?? userInfo.value?.family_name ?? ''
    if (givenName || familyName) {
      return `${givenName} ${familyName}`.trim()
    }
  }
  return props.guestName || 'Guest'
})

const logout = () => {
  authStore.logout()
}
</script>

<style scoped>
.header-user-button {
  padding: 0.25rem 0.5rem;
  min-width: auto;
  background-color: transparent;
  color: var(--typography-color-primary);
  box-shadow: none;
  font-family: var(--typography-font-families-bc-sans);
  font-weight: var(--typography-font-weights-regular);
  font-size: var(--typography-font-size-body);
  line-height: var(--typography-line-heights-xdense);
}

.header-user-button:hover {
  background-color: rgba(0, 0, 0, 0.04);
}

.header-user-icon {
  margin-right: 0.25rem;
  color: var(--typography-color-primary);
}

.header-user-name {
  text-transform: none;
  font-family: var(--typography-font-families-bc-sans);
  font-weight: var(--typography-font-weights-regular);
  font-size: var(--typography-font-size-body);
  line-height: var(--typography-line-heights-xdense);
  letter-spacing: normal;
  white-space: nowrap;
}
</style>
