import { createVuetify, type ThemeDefinition } from 'vuetify'
import { VFileUpload } from 'vuetify/labs/VFileUpload'
import '@mdi/font/css/materialdesignicons.css'
import 'vuetify/styles'

const defaultTheme: ThemeDefinition = {
  dark: false,
  variables: {
    'disabled-opacity': 0.6,
  },
}

export default createVuetify({
  components: {
    VFileUpload,
  },
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
