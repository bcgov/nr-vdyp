import type { RouteRecordRaw } from 'vue-router'
import ModelParameterInput from '@/views/ModelParameterInput.vue'
import PageNotFound from '@/views/PageNotFound.vue'
import AuthInfo from '@/views/test/AuthInfo.vue'
import ParameterDetail from '@/views/test/ParameterDetail.vue'

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
  {
    path: '/param-detail',
    name: 'ParameterDetail',
    component: ParameterDetail,
  },
  { path: '/:pathMatch(.*)*', name: 'NotFound', component: PageNotFound },
]
