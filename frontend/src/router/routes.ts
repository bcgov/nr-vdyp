import type { RouteRecordRaw } from 'vue-router'
import ProjectionListView from '@/views/ProjectionListView.vue'
import ProjectionDetail from '@/views/ProjectionDetail.vue'
import ModelParameterInput from '@/views/ModelParameterInput.vue'
import PageNotFound from '@/views/PageNotFound.vue'
import AuthInfo from '@/views/test/AuthInfo.vue'
import ParameterDetail from '@/views/test/ParameterDetail.vue'

export const routes: Array<RouteRecordRaw> = [
  {
    path: '/',
    name: 'ProjectionListView',
    component: ProjectionListView,
  },
  {
    path: '/projection-detail',
    name: 'ProjectionDetail',
    component: ProjectionDetail,
  },
  {
    path: '/model-parameter-input',
    name: 'ModelParameterInput',
    component: ModelParameterInput, // NON-Persistent Projection Run Version
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
