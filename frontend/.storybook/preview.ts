import type { Preview } from '@storybook/vue3-vite'
import { setup } from '@storybook/vue3-vite'
import { registerPlugins } from '../src/plugins'
import { withVuetifyTheme } from './withVeutifyTheme.decorator'
import '@bcgov/bc-sans/css/BC_Sans.css'
import '@bcgov/design-tokens/css/variables.css'
import '../src/styles/style.scss'

setup((app) => {
  registerPlugins(app)
})

export const decorators = [withVuetifyTheme]

const preview: Preview = {
  parameters: {
    controls: {
      matchers: {
        color: /(background|color)$/i,
        date: /Date$/i,
      },
    },
  },
}

export default preview
