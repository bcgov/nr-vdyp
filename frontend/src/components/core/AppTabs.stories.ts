import type { Meta, StoryObj } from '@storybook/vue3'
import AppTabs from './AppTabs.vue'
import { MODEL_PARAM_TAB_NAME, REPORTING_TAB } from '@/constants/constants'

const meta: Meta<typeof AppTabs> = {
  title: 'components/core/AppTabs',
  component: AppTabs,
  argTypes: {
    currentTab: {
      control: { type: 'number' },
      description: 'The index of the currently selected tab.',
    },
    tabs: {
      control: 'object',
      description:
        'An array of tab objects with label, component, and tabname.',
    },
    'update:currentTab': {
      action: 'update:currentTab',
      description: 'Emits the updated tab index to the parent.',
    },
  },
  tags: ['autodocs'],
}

export default meta

type Story = StoryObj<typeof AppTabs>

export const Default: Story = {
  render: (args) => ({
    components: { AppTabs },
    setup() {
      return { args }
    },
    template: `
      <AppTabs
        v-bind="args"
        @update:currentTab="args['update:currentTab']"
      />
    `,
  }),
  args: {
    currentTab: 0,
    tabs: [
      { label: 'Tab 1', component: 'Component1', tabname: 'Tab1' },
      { label: 'Tab 2', component: 'Component2', tabname: 'Tab2' },
      { label: 'Tab 3', component: 'Component3', tabname: 'Tab3' },
      { label: 'Tab 4', component: 'Component4', tabname: 'Tab4' },
    ],
  },
}

export const WithInitialTab: Story = {
  render: (args) => ({
    components: { AppTabs },
    setup() {
      return { args }
    },
    template: `
      <AppTabs
        v-bind="args"
        @update:currentTab="args['update:currentTab']"
      />
    `,
  }),
  args: {
    currentTab: 0,
    tabs: [
      {
        label: MODEL_PARAM_TAB_NAME.MODEL_PARAM_SELECTION,
        component: 'SpeciesInfoPanel',
        tabname: null,
      },
      {
        label: MODEL_PARAM_TAB_NAME.MODEL_REPORT,
        component: 'ReportingContainer',
        tabname: REPORTING_TAB.MODEL_REPORT,
      },
      {
        label: MODEL_PARAM_TAB_NAME.VIEW_LOG_FILE,
        component: 'ReportingContainer',
        tabname: REPORTING_TAB.VIEW_LOG_FILE,
      },
      {
        label: MODEL_PARAM_TAB_NAME.VIEW_ERROR_MESSAGES,
        component: 'ReportingContainer',
        tabname: REPORTING_TAB.VIEW_ERR_MSG,
      },
    ],
  },
}
