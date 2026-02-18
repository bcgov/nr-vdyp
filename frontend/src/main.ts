import { createApp } from 'vue'
import { registerPlugins } from '@/plugins'
import App from './App.vue'
import { initializeKeycloak } from '@/services/keycloak'
import '@bcgov/bc-sans/css/BC_Sans.css'
import '@bcgov/design-tokens/css/variables.css'
import '@/styles/style.scss'

const showBootstrapError = (message: string) => {
  const spinner = document.querySelector('.app-loader-spinner')
  const text = document.querySelector('.app-loader-text')
  if (spinner) {
    spinner.remove()
  }
  if (text) {
    text.textContent = message
    ;(text as HTMLElement).style.color = '#d8292f'
  }
}

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
    showBootstrapError('Authentication service is unavailable. Please try refreshing the page.')
  }
}

bootstrap()
