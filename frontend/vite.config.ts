import { fileURLToPath, URL } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import Vue from '@vitejs/plugin-vue'
import Vuetify from 'vite-plugin-vuetify'
import packageVersion from 'vite-plugin-package-version'

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  process.env = { ...process.env, ...loadEnv(mode, process.cwd()) }
  return {
    plugins: [
      {
        name: 'build-html',
        apply: 'build',
        transformIndexHtml: (html) => {
          return {
            html,
            tags: [
              {
                tag: 'script',
                attrs: {
                  src: '/env.js',
                },
                injectTo: 'head',
              },
            ],
          }
        },
      },
      Vue(),
      packageVersion(),
      Vuetify({
        autoImport: true,
      }),
    ],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },
    server: {
      proxy: {
        // Proxy API requests to the backend
        '/api': {
          target: process.env.VITE_API_URL,
          changeOrigin: true,
        },
      },
    },
  }
})
