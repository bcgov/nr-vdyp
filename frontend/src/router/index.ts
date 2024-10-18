import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

import JobList from '@/views/JobList.vue'
import ModelParameterInput from '@/views/input-model-parameters/ModelParameterInput.vue'
import ModelParameterSelection from '@/views/input-model-parameters/ModelParameterSelection.vue'
import PageNotFound from '@/views/PageNotFound.vue'

const routes: Array<RouteRecordRaw> = [
  {
    path: '/',
    name: 'Home',
    component: JobList,
  },
  {
    path: '/input-model-parameters',
    name: 'ModelParameterInput',
    component: ModelParameterInput,
    children: [
      {
        path: 'model-parameter-selection',
        name: 'ModelParameterSelection',
        component: ModelParameterSelection,
      },
    ],
  },
  { path: '/:pathMatch(.*)*', name: 'NotFound', component: PageNotFound },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

router.beforeEach((to, from, next) => {
  if (
    to.hash.includes('code=') &&
    to.hash.includes('state=') &&
    to.hash.includes('session_state=') &&
    to.hash.includes('iss=')
  ) {
    const hashParams = new URLSearchParams(to.hash.slice(1))
    hashParams.delete('code')
    hashParams.delete('state')
    hashParams.delete('session_state')
    hashParams.delete('iss')

    const newHash = hashParams.toString() ? `#${hashParams.toString()}` : ''

    next({
      path: to.path + newHash,
      replace: true,
    })
  } else {
    next()
  }
})

export default router
