import { createRouter, createWebHistory } from 'vue-router'
import { routes } from './routes'
import { useAuthStore } from '@/stores/common/authStore'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

router.beforeEach((to, _from, next) => {
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
    return
  }

  if (to.name !== 'Unauthorized') {
    const authStore = useAuthStore()
    if (authStore.authenticated && authStore.getAllRoles().length === 0) {
      next({ name: 'Unauthorized' })
      return
    }
  }

  next()
})

export default router
