import type { Preview } from '@storybook/vue3'
import { setup } from '@storybook/vue3'
import { registerPlugins } from '../src/plugins'
import { withVuetifyTheme } from './withVeutifyTheme.decorator'
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
