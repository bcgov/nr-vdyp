import type { Meta, StoryObj } from '@storybook/vue3-vite'
import ReportingOutput from './ReportingOutput.vue'

const meta: Meta<typeof ReportingOutput> = {
  title: 'components/reporting/ReportingOutput',
  component: ReportingOutput,
  tags: ['autodocs'],
  argTypes: {
    data: {
      control: 'object',
      description: 'The data array to be displayed in the output',
      defaultValue: ['Line 1', 'Line 2', 'Line 3'],
    },
  },
}

export default meta
type Story = StoryObj<typeof ReportingOutput>

const fetchFileData = async (url: string): Promise<string[]> => {
  const response = await fetch(url)
  const text = await response.text()
  return text.split('\n') // Split by line breaks
}

export const ErrorLog: Story = {
  args: {
    data: await fetchFileData('/test-data/ErrorLog.txt'),
  },
}

export const ProgressLog: Story = {
  args: {
    data: await fetchFileData('/test-data/ProgressLog.txt'),
  },
}

export const YieldTable: Story = {
  args: {
    data: await fetchFileData('/test-data/YieldTable.csv'),
  },
}
