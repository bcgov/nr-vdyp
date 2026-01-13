import { createApp } from 'vue'
import { registerPlugins } from '@/plugins'
import App from './App.vue'
import { initializeKeycloak } from '@/services/keycloak'
import '@bcgov/bc-sans/css/BC_Sans.css'
import '@bcgov/design-tokens/css/variables.css'
import '@/styles/style.scss'

const bootstrap = async () => {
  const app = createApp(App)

  registerPlugins(app)

  try {
    const keycloak = await initializeKeycloak()

    if (keycloak) {
      if (keycloak.authenticated) {
        app.mount('#app')
      } else {
        keycloak.login()
      }
    }
  } catch (error) {
    console.error('Failed to initialize Keycloak:', error)
  }
}

bootstrap()
