import type { RouteRecordRaw } from 'vue-router'
import ProjectionListView from '@/views/ProjectionListView.vue'
import ProjectionDetail from '@/views/ProjectionDetail.vue'
import PageNotFound from '@/views/PageNotFound.vue'
import AuthInfo from '@/views/test/AuthInfo.vue'
import ParameterDetail from '@/views/test/ParameterDetail.vue'
import { ROUTE_PATH } from '@/constants/constants'

export const routes: Array<RouteRecordRaw> = [
  {
    path: ROUTE_PATH.PROJECTION_LIST,
    name: 'ProjectionListView',
    component: ProjectionListView,
  },
  {
    path: ROUTE_PATH.PROJECTION_DETAIL,
    name: 'ProjectionDetail',
    component: ProjectionDetail,
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
