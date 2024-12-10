import { createRouter, createWebHistory } from 'vue-router'
import { routes } from './routes'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

router.beforeEach((to, from, next) => {
  const hash = to.hash || ''
  if (
    hash.includes('code=') &&
    hash.includes('state=') &&
    hash.includes('session_state=') &&
    hash.includes('iss=')
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
