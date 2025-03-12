<template>
  <v-row justify="center" class="ma-10 auth-output-container">
    <v-col cols="12" sm="12" md="12">
      <v-table class="bordered-table">
        <tbody>
          <tr>
            <td class="field-column">auth_time</td>
            <td>
              {{ userInfo && userInfo.auth_time ? userInfo.auth_time : 'N/A' }}
              ({{ formattedAuthTime }})
              <v-btn @click="validateAndRefreshToken"
                >Validate And Refresh Token</v-btn
              >
            </td>
          </tr>
          <tr>
            <td class="field-column">exp</td>
            <td>
              {{ userInfo && userInfo.exp ? userInfo.exp : 'N/A' }} ({{
                formattedExpTime
              }})
            </td>
          </tr>
          <tr>
            <td class="field-column">client_roles</td>
            <td>
              {{
                userInfo &&
                userInfo.client_roles &&
                userInfo.client_roles.length > 0
                  ? userInfo.client_roles.join(', ')
                  : 'No roles assigned'
              }}
            </td>
          </tr>
          <tr>
            <td class="field-column">display_name</td>
            <td>
              {{
                userInfo && userInfo.display_name
                  ? userInfo.display_name
                  : 'N/A'
              }}
            </td>
          </tr>
          <tr>
            <td class="field-column">email</td>
            <td>{{ userInfo && userInfo.email ? userInfo.email : 'N/A' }}</td>
          </tr>
          <tr>
            <td class="field-column">family_name</td>
            <td>
              {{
                userInfo && userInfo.family_name ? userInfo.family_name : 'N/A'
              }}
            </td>
          </tr>
          <tr>
            <td class="field-column">given_name</td>
            <td>
              {{
                userInfo && userInfo.given_name ? userInfo.given_name : 'N/A'
              }}
            </td>
          </tr>
          <tr>
            <td class="field-column">idir_username</td>
            <td>
              {{
                userInfo && userInfo.idir_username
                  ? userInfo.idir_username
                  : 'N/A'
              }}
            </td>
          </tr>
          <tr>
            <td class="field-column">name</td>
            <td>{{ userInfo && userInfo.name ? userInfo.name : 'N/A' }}</td>
          </tr>
          <tr>
            <td class="field-column">preferred_username</td>
            <td>
              {{
                userInfo && userInfo.preferred_username
                  ? userInfo.preferred_username
                  : 'N/A'
              }}
            </td>
          </tr>
          <tr>
            <td class="field-column">user_principal_name</td>
            <td>
              {{
                userInfo && userInfo.user_principal_name
                  ? userInfo.user_principal_name
                  : 'N/A'
              }}
            </td>
          </tr>

          <tr>
            <td class="field-column">Access Token</td>
            <td>{{ accessToken }}</td>
          </tr>
          <tr>
            <td class="field-column">ID Token</td>
            <td>{{ idToken }}</td>
          </tr>
          <tr>
            <td class="field-column">Refresh Token</td>
            <td>{{ refToken }}</td>
          </tr>
        </tbody>
      </v-table>
    </v-col>
  </v-row>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useAuthStore } from '@/stores/common/authStore'
import { formatUnixTimestampToDate } from '@/utils/util'
import { handleTokenValidation } from '@/services/keycloak'

const authStore = useAuthStore()

const userInfo = computed(() => authStore.parseIdToken())

const accessToken = computed(() => {
  return authStore.user && authStore.user.accessToken
    ? authStore.user.accessToken
    : 'No Access Token'
})

const idToken = computed(() => {
  return authStore.user && authStore.user.idToken
    ? authStore.user.idToken
    : 'No ID Token'
})

const refToken = computed(() => {
  return authStore.user && authStore.user.refToken
    ? authStore.user.refToken
    : 'No Refresh Token'
})

const formattedAuthTime = computed(() => {
  return userInfo.value && userInfo.value.auth_time
    ? formatUnixTimestampToDate(userInfo.value.auth_time)
    : 'No Auth Time'
})

const formattedExpTime = computed(() => {
  return userInfo.value && userInfo.value.exp
    ? formatUnixTimestampToDate(userInfo.value.exp)
    : 'No Expiration Time'
})

const validateAndRefreshToken = async () => {
  await handleTokenValidation()
}
</script>
<style scoped>
.auth-output-container {
  max-width: 100%;
  word-wrap: break-word;
  overflow: hidden;
  white-space: pre-wrap;
}

.bordered-table {
  border-collapse: separate;
  border-spacing: 0;
  border-radius: 4px;
  overflow: hidden;
  box-shadow: none;
  border-bottom: 1px solid #ddd !important;
}

.bordered-table .field-column {
  width: 10%;
}

.bordered-table td {
  white-space: normal;
  word-break: break-word;
}
</style>
