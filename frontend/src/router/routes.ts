import type { RouteRecordRaw } from 'vue-router'
import ModelParameterInput from '@/views/input-model-parameters/ModelParameterInput.vue'
import PageNotFound from '@/views/PageNotFound.vue'
import AuthInfo from '@/views/test/AuthInfo.vue'

export const routes: Array<RouteRecordRaw> = [
  {
    path: '/',
    name: 'ModelParameterInput',
    component: ModelParameterInput,
  },
  {
    path: '/auth-info',
    name: 'AuthInfo',
    component: AuthInfo,
  },
  { path: '/:pathMatch(.*)*', name: 'NotFound', component: PageNotFound },
]
