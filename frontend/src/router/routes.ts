import type { RouteRecordRaw } from 'vue-router'
import ProjectionListView from '@/views/ProjectionListView.vue'
import ProjectionDetail from '@/views/ProjectionDetail.vue'
import PageNotFound from '@/views/PageNotFound.vue'
import UnauthorizedView from '@/views/UnauthorizedView.vue'
import AuthInfo from '@/views/test/AuthInfo.vue'
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
    path: ROUTE_PATH.UNAUTHORIZED,
    name: 'Unauthorized',
    component: UnauthorizedView,
  },
  {
    path: '/auth-info',
    name: 'AuthInfo',
    component: AuthInfo,
  },
  { path: '/:pathMatch(.*)*', name: 'NotFound', component: PageNotFound },
]
