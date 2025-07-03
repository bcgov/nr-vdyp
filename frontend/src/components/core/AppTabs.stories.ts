import type { Meta, StoryObj } from '@storybook/vue3-vite'
import AppTabs from './AppTabs.vue'
import { MODEL_PARAM_TAB_NAME, REPORTING_TAB } from '@/constants/constants'
import { action } from 'storybook/actions'

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
  },
  tags: ['autodocs'],
}

export default meta

type Story = StoryObj<typeof AppTabs>

export const Default: Story = {
  render: (args) => ({
    components: { AppTabs },
    setup() {
      return {
        args,
        onUpdateCurrentTab: action('update:currentTab'),
      }
    },
    template: `
      <AppTabs
        v-bind="args"
        @update:currentTab="onUpdateCurrentTab"
      />
    `,
  }),
  args: {
    currentTab: 0,
    tabs: [
      {
        label: 'Tab 1',
        component: 'Component1',
        tabname: 'Tab1',
        disabled: false,
      },
      {
        label: 'Tab 2',
        component: 'Component2',
        tabname: 'Tab2',
        disabled: false,
      },
      {
        label: 'Tab 3',
        component: 'Component3',
        tabname: 'Tab3',
        disabled: false,
      },
      {
        label: 'Tab 4',
        component: 'Component4',
        tabname: 'Tab4',
        disabled: false,
      },
    ],
  },
}

export const WithInitialTab: Story = {
  render: (args) => ({
    components: { AppTabs },
    setup() {
      return {
        args,
        onUpdateCurrentTab: action('update:currentTab'),
      }
    },
    template: `
      <AppTabs
        v-bind="args"
        @update:currentTab="onUpdateCurrentTab"
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
        disabled: false,
      },
      {
        label: MODEL_PARAM_TAB_NAME.MODEL_REPORT,
        component: 'ReportingContainer',
        tabname: REPORTING_TAB.MODEL_REPORT,
        disabled: false,
      },
      {
        label: MODEL_PARAM_TAB_NAME.VIEW_LOG_FILE,
        component: 'ReportingContainer',
        tabname: REPORTING_TAB.VIEW_LOG_FILE,
        disabled: false,
      },
      {
        label: MODEL_PARAM_TAB_NAME.VIEW_ERROR_MESSAGES,
        component: 'ReportingContainer',
        tabname: REPORTING_TAB.VIEW_ERR_MSG,
        disabled: false,
      },
    ],
  },
}
