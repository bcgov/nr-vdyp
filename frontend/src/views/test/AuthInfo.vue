<template>
  <div>
    <!-- ACCESS TOKEN Section -->
    <p style="margin-left: 50px; margin-top: 20px">ACCESS TOKEN</p>
    <v-row
      justify="center"
      class="ml-10 auth-output-container"
      style="margin-top: 0px"
    >
      <v-col cols="12" sm="12" md="12">
        <v-table class="bordered-table">
          <tbody>
            <tr>
              <td class="field-column">exp</td>
              <td>
                {{
                  accessTokenInfo && accessTokenInfo.exp
                    ? accessTokenInfo.exp
                    : 'N/A'
                }}
                ({{ formattedAccessExpTime }})
                <v-btn @click="validateAndRefreshToken"
                  >Validate And Refresh Token</v-btn
                >
              </td>
            </tr>
            <tr>
              <td class="field-column">aud</td>
              <td>
                {{
                  accessTokenInfo && accessTokenInfo.aud
                    ? accessTokenInfo.aud
                    : 'N/A'
                }}
              </td>
            </tr>
            <tr>
              <td class="field-column">azp</td>
              <td>
                {{
                  accessTokenInfo && accessTokenInfo.azp
                    ? accessTokenInfo.azp
                    : 'N/A'
                }}
              </td>
            </tr>
            <tr>
              <td class="field-column">client_roles</td>
              <td>
                {{
                  accessTokenInfo &&
                  accessTokenInfo.client_roles &&
                  accessTokenInfo.client_roles.length > 0
                    ? accessTokenInfo.client_roles.join(', ')
                    : 'No roles assigned'
                }}
              </td>
            </tr>
            <tr>
              <td class="field-column">display_name</td>
              <td>
                {{
                  idTokenInfo && idTokenInfo.display_name
                    ? idTokenInfo.display_name
                    : 'N/A'
                }}
              </td>
            </tr>
            <tr>
              <td class="field-column">iss</td>
              <td>
                {{
                  accessTokenInfo && accessTokenInfo.iss
                    ? accessTokenInfo.iss
                    : 'N/A'
                }}
              </td>
            </tr>
            <tr>
              <td class="field-column">sub</td>
              <td>
                {{
                  accessTokenInfo && accessTokenInfo.sub
                    ? accessTokenInfo.sub
                    : 'N/A'
                }}
              </td>
            </tr>
            <tr>
              <td class="field-column">sub</td>
              <td>
                {{
                  accessTokenInfo && accessTokenInfo.sub
                    ? accessTokenInfo.sub
                    : 'N/A'
                }}
              </td>
            </tr>
            <tr>
              <td class="field-column">Access Token</td>
              <td>{{ accessToken }}</td>
            </tr>
          </tbody>
        </v-table>
      </v-col>
    </v-row>

    <!-- ID TOKEN Section -->
    <p style="margin-left: 50px; margin-top: 20px">ID TOKEN</p>
    <v-row
      justify="center"
      class="ml-10 auth-output-container"
      style="margin-top: 0px"
    >
      <v-col cols="12" sm="12" md="12">
        <v-table class="bordered-table">
          <tbody>
            <tr>
              <td class="field-column">exp</td>
              <td>
                {{ idTokenInfo && idTokenInfo.exp ? idTokenInfo.exp : 'N/A' }}
                ({{ formattedIdExpTime }})
              </td>
            </tr>
            <tr>
              <td class="field-column">client_roles</td>
              <td>
                {{
                  idTokenInfo &&
                  idTokenInfo.client_roles &&
                  idTokenInfo.client_roles.length > 0
                    ? idTokenInfo.client_roles.join(', ')
                    : 'No roles assigned'
                }}
              </td>
            </tr>
            <tr>
              <td class="field-column">display_name</td>
              <td>
                {{
                  idTokenInfo && idTokenInfo.display_name
                    ? idTokenInfo.display_name
                    : 'N/A'
                }}
              </td>
            </tr>
            <tr>
              <td class="field-column">email</td>
              <td>
                {{
                  idTokenInfo && idTokenInfo.email ? idTokenInfo.email : 'N/A'
                }}
              </td>
            </tr>
            <tr>
              <td class="field-column">family_name</td>
              <td>
                {{
                  idTokenInfo && idTokenInfo.family_name
                    ? idTokenInfo.family_name
                    : 'N/A'
                }}
              </td>
            </tr>
            <tr>
              <td class="field-column">given_name</td>
              <td>
                {{
                  idTokenInfo && idTokenInfo.given_name
                    ? idTokenInfo.given_name
                    : 'N/A'
                }}
              </td>
            </tr>
            <tr>
              <td class="field-column">idir_username</td>
              <td>
                {{
                  idTokenInfo && idTokenInfo.idir_username
                    ? idTokenInfo.idir_username
                    : 'N/A'
                }}
              </td>
            </tr>
            <tr>
              <td class="field-column">name</td>
              <td>
                {{ idTokenInfo && idTokenInfo.name ? idTokenInfo.name : 'N/A' }}
              </td>
            </tr>
            <tr>
              <td class="field-column">preferred_username</td>
              <td>
                {{
                  idTokenInfo && idTokenInfo.preferred_username
                    ? idTokenInfo.preferred_username
                    : 'N/A'
                }}
              </td>
            </tr>
            <tr>
              <td class="field-column">user_principal_name</td>
              <td>
                {{
                  idTokenInfo && idTokenInfo.user_principal_name
                    ? idTokenInfo.user_principal_name
                    : 'N/A'
                }}
              </td>
            </tr>
            <tr>
              <td class="field-column">ID Token</td>
              <td>{{ idToken }}</td>
            </tr>
          </tbody>
        </v-table>
      </v-col>
    </v-row>

    <!-- ref Token Section -->
    <p style="margin-left: 50px; margin-top: 20px">ref TOKEN</p>
    <v-row
      justify="center"
      class="ml-10 auth-output-container"
      style="margin-top: 0px"
    >
      <v-col cols="12" sm="12" md="12">
        <v-table class="bordered-table">
          <tbody>
            <tr>
              <td class="field-column">exp</td>
              <td>
                {{
                  refTokenInfo && refTokenInfo.exp ? refTokenInfo.exp : 'N/A'
                }}
                ({{ formattedRefExpTime }})
              </td>
            </tr>
            <tr>
              <td class="field-column">ref Token</td>
              <td>{{ refToken }}</td>
            </tr>
          </tbody>
        </v-table>
      </v-col>
    </v-row>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useAuthStore } from '@/stores/common/authStore'
import { formatUnixTimestampToDate } from '@/utils/util'
import { handleTokenValidation } from '@/services/keycloak'

interface AccessTokenInfo {
  exp: number | null
  aud: string | null
  azp: string | null
  client_roles: string[]
  display_name: string | null
  email: string | null
  email_verified: boolean | null
  family_name: string | null
  given_name: string | null
  iat: string | null
  identity_provider: string | null
  idir_user_guid: string | null
  idir_username: string | null
  iss: string | null
  jti: string | null
  name: string | null
  nonce: string | null
  preferred_username: string | null
  scope: string | null
  session_state: string | null
  sid: string | null
  sub: string | null
  typ: string | null
  user_principal_name: string | null
}

interface IdTokenInfo {
  client_roles: string[]
  display_name: string | null
  email: string | null
  exp: number | null
  family_name: string | null
  given_name: string | null
  idir_username: string | null
  name: string | null
  preferred_username: string | null
  user_principal_name: string | null
}

interface RefTokenInfo {
  exp: number | null
}

const authStore = useAuthStore()

const accessTokenInfo = computed<AccessTokenInfo | null>(() =>
  authStore.getParsedAccessToken(),
)
const idTokenInfo = computed<IdTokenInfo | null>(() =>
  authStore.getParsedIdToken(),
)

const refTokenInfo = computed<RefTokenInfo | null>(() =>
  authStore.getParsedRefreshToken(),
)

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

const formattedAccessExpTime = computed(() => {
  return accessTokenInfo.value && accessTokenInfo.value.exp
    ? formatUnixTimestampToDate(accessTokenInfo.value.exp)
    : 'No Expiration Time'
})

const formattedIdExpTime = computed(() => {
  return idTokenInfo.value && idTokenInfo.value.exp
    ? formatUnixTimestampToDate(idTokenInfo.value.exp)
    : 'No Expiration Time'
})

const formattedRefExpTime = computed(() => {
  return refTokenInfo.value && refTokenInfo.value.exp
    ? formatUnixTimestampToDate(refTokenInfo.value.exp)
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
