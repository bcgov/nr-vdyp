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
      <v-list-item v-if="isAdmin" @click="goToMyProjections">
        <v-list-item-title>My Projections</v-list-item-title>
      </v-list-item>
      <v-list-item v-if="isAdmin" @click="goToAdminDashboard">
        <v-list-item-title>Admin Dashboard</v-list-item-title>
      </v-list-item>
      <v-list-item @click="logout">
        <v-list-item-title>{{ logoutText }}</v-list-item-title>
      </v-list-item>
    </v-list>
  </v-menu>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/common/authStore'
import { useAlertDialogStore } from '@/stores/common/alertDialogStore'
import { useUnsavedChangesStore } from '@/stores/common/unsavedChangesStore'
import { ROUTE_PATH, USER_ROLE } from '@/constants/constants'
import { MESSAGE } from '@/constants'

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

const router = useRouter()
const authStore = useAuthStore()
const alertDialogStore = useAlertDialogStore()
const unsavedChangesStore = useUnsavedChangesStore()
const userInfo = computed(() => authStore.getParsedIdToken())
const isAdmin = computed(() => authStore.hasRole(USER_ROLE.ADMIN))

const confirmDiscardUnsavedChanges = async (): Promise<boolean> => {
  if (!(await unsavedChangesStore.hasUnsavedChanges())) return true
  return await alertDialogStore.openDialog(
    MESSAGE.UNSAVED_CHANGES_DIALOG.TITLE,
    MESSAGE.UNSAVED_CHANGES_DIALOG.MESSAGE,
    { variant: 'warning' },
  )
}

const goToAdminDashboard = () => {
  router.push(ROUTE_PATH.ADMIN_DASHBOARD)
}

const goToMyProjections = () => {
  router.push(ROUTE_PATH.PROJECTION_LIST)
}

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

const logout = async () => {
  // Logout redirects the whole page rather than navigating via vue-router, so it bypasses
  // ProjectionDetail's onBeforeRouteLeave guard and needs its own unsaved-changes check.
  if (!(await confirmDiscardUnsavedChanges())) return
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
  overflow: hidden;
  flex-shrink: 1;
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
  overflow: hidden;
  text-overflow: ellipsis;
}

@media (max-width: 480px) {
  .header-user-icon {
    display: none;
  }
}
</style>
