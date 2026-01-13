import { createVuetify, type ThemeDefinition } from 'vuetify'
import '@mdi/font/css/materialdesignicons.css'
import 'vuetify/styles'

const defaultTheme: ThemeDefinition = {
  dark: false,
  variables: {
    'disabled-opacity': 0.6,
  },
}

export default createVuetify({
  icons: {
    defaultSet: 'mdi',
  },
  theme: {
    defaultTheme: 'defaultTheme',
    themes: {
      defaultTheme,
    },
  },
})
