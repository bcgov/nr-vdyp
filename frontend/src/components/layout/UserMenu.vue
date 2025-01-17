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
import { computed, defineProps } from 'vue'
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
const userInfo = computed(() => authStore.parseIdToken())

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
  padding-left: 0px;
  padding-right: 0px;
  min-width: auto;
  background-color: transparent;
  color: rgb(255, 255, 255);
  box-shadow: none;
}

/* user-icon*/
i.mdi-account-circle.mdi.v-icon.notranslate.v-theme--defaultTheme.v-icon--size-default.header-user-icon {
  margin-top: 3px;
}

span.header-user-icon {
  margin-right: 4px;
  padding-top: 1px;
}

span.header-user-name {
  padding: 0px 0px 0px 3px;
  text-transform: none;
  font-size: 14px;
  letter-spacing: normal;
  line-height: 21px;
}

/* user-icon and user-name whole box */
button.v-btn.v-theme--defaultTheme.v-btn--density-default.v-btn--size-default.v-btn--variant-text.d-flex.align-center {
  padding-left: 5px;
  padding-right: 5px;
}
</style>
