import type { Meta, StoryObj } from '@storybook/vue3-vite'
import { createPinia, setActivePinia } from 'pinia'
import ReportingContainer from './ReportingContainer.vue'
import { useProjectionStore } from '@/stores/projectionStore'
import { REPORTING_TAB } from '@/constants/constants'

const pinia = createPinia()
setActivePinia(pinia)

const meta: Meta<typeof ReportingContainer> = {
  title: 'components/reporting/ReportingContainer',
  component: ReportingContainer,
  decorators: [
    (story) => {
      const projectionStore = useProjectionStore()
      projectionStore.loadSampleData().then(() => {
        console.log('Sample data loaded:', {
          MODEL_REPORT: projectionStore.csvYieldLines,
          VIEW_ERR_MSG: projectionStore.errorMessages,
          VIEW_LOG_FILE: projectionStore.logMessages,
        })
      })
      return {
        components: { story },
        template: `<div><story /></div>`,
      }
    },
  ],
  tags: ['autodocs'],
  argTypes: {
    tabname: {
      control: 'select',
      options: Object.keys(REPORTING_TAB),
      description: 'Determines the type of data to display',
      defaultValue: REPORTING_TAB.MODEL_REPORT,
    },
  },
}

export default meta

type Story = StoryObj<typeof ReportingContainer>

export const ModelReport: Story = {
  args: {
    tabname: REPORTING_TAB.MODEL_REPORT,
  },
}

export const ViewErrorMessages: Story = {
  args: {
    tabname: REPORTING_TAB.VIEW_ERR_MSG,
  },
}

export const ViewLogFile: Story = {
  args: {
    tabname: REPORTING_TAB.VIEW_LOG_FILE,
  },
}
