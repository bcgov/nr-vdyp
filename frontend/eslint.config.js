import js from '@eslint/js'
import globals from 'globals'
import tseslint from 'typescript-eslint'
import pluginVue from 'eslint-plugin-vue'
import storybook from 'eslint-plugin-storybook'
import pluginCypress from 'eslint-plugin-cypress'
import prettierConfig from 'eslint-config-prettier'
import { defineConfig, globalIgnores } from 'eslint/config'
import { includeIgnoreFile } from '@eslint/compat'
import { fileURLToPath } from 'node:url'

const gitignorePath = fileURLToPath(new URL('.gitignore', import.meta.url))

export default defineConfig([
  includeIgnoreFile(gitignorePath),
  globalIgnores([
    '.husky/',
    '.vscode/',
    '.yarn/',
    'coverage/',
    'dist/',
    'public/assets/',
    'tsconfig.*.json',
    '!.storybook',
  ]),
  {
    files: ['**/*.{js,mjs,cjs,ts,mts,cts,vue}'],
    plugins: {
      js,
    },
  },
  js.configs.recommended,
  ...storybook.configs['flat/recommended'],
  {
    files: ['**/*.{js,mjs,cjs,ts,mts,cts,vue}'],
    languageOptions: { globals: globals.browser },
  },
  ...tseslint.configs.recommended,
  ...pluginVue.configs['flat/essential'],
  pluginCypress.configs.recommended,
  prettierConfig,
  {
    files: ['**/*.vue'],
    languageOptions: { parserOptions: { parser: tseslint.parser } },
  },
  {
    files: ['**/*.{ts,tsx,mts,cts,vue}'],
    rules: {
      '@typescript-eslint/no-explicit-any': 'off',
      'spaced-comment': ['error', 'always', { markers: ['/'] }], // custom rule
    },
  },
  {
    files: ['**/*.cy.ts'],
    rules: {
      '@typescript-eslint/no-unused-expressions': 'off',
    },
  },
  {
    files: ['cypress/support/component.ts'],
    rules: {
      '@typescript-eslint/no-namespace': 'off',
    },
  },
  {
    files: [
      'src/services/vdyp-api/models/file-upload.ts',
      'src/shims-vue.d.ts',
    ],
    rules: {
      '@typescript-eslint/no-empty-object-type': 'off',
    },
  },
])
