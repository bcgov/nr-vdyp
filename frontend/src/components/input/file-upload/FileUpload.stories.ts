import type { Meta, StoryObj } from '@storybook/vue3'
import FileUpload from './FileUpload.vue'

const meta: Meta<typeof FileUpload> = {
  title: 'components/input/file-upload/FileUpload',
  component: FileUpload,
  argTypes: {
    startingAge: {
      control: { type: 'number' },
      description: 'Starting age for the report.',
      defaultValue: 10,
    },
    finishingAge: {
      control: { type: 'number' },
      description: 'Finishing age for the report.',
      defaultValue: 50,
    },
    ageIncrement: {
      control: { type: 'number' },
      description: 'Age increment value.',
      defaultValue: 5,
    },
    volumeReported: {
      control: { type: 'array' },
      description: 'Volume reported options.',
      defaultValue: ['Option 1', 'Option 2'],
    },
    includeInReport: {
      control: { type: 'array' },
      description: 'Items to include in the report.',
      defaultValue: ['Include 1', 'Include 2'],
    },
    projectionType: {
      control: { type: 'text' },
      description: 'Projection type for the report.',
      defaultValue: 'Projection 1',
    },
    reportTitle: {
      control: { type: 'text' },
      description: 'Report title.',
      defaultValue: 'Sample Report',
    },
    isProgressVisible: {
      control: { type: 'boolean' },
      description: 'Whether the progress indicator is visible.',
      defaultValue: false,
    },
    progressMessage: {
      control: { type: 'text' },
      description: 'Message displayed in the progress indicator.',
      defaultValue: 'Running model...',
    },
    layerFile: {
      control: { type: 'file' },
      description: 'Layer file to upload.',
      defaultValue: null,
    },
    polygonFile: {
      control: { type: 'file' },
      description: 'Polygon file to upload.',
      defaultValue: null,
    },
  },
  tags: ['autodocs'],
}

export default meta

type Story = StoryObj<typeof FileUpload>

export const Default: Story = {
  render: (args) => ({
    components: { FileUpload },
    setup() {
      return { args }
    },
    template: `<FileUpload v-bind="args" />`,
  }),
  args: {
    startingAge: 10,
    finishingAge: 50,
    ageIncrement: 5,
    volumeReported: ['Option 1', 'Option 2'],
    includeInReport: ['Include 1', 'Include 2'],
    projectionType: 'Projection 1',
    reportTitle: 'Sample Report',
    isProgressVisible: false,
    progressMessage: 'Running model...',
    layerFile: null,
    polygonFile: null,
  },
}

export const WithProgress: Story = {
  render: (args) => ({
    components: { FileUpload },
    setup() {
      return { args }
    },
    template: `<FileUpload v-bind="args" />`,
  }),
  args: {
    ...Default.args,
    isProgressVisible: true,
    progressMessage: 'Uploading files...',
  },
}

export const WithFiles: Story = {
  render: (args) => ({
    components: { FileUpload },
    setup() {
      return { args }
    },
    template: `<FileUpload v-bind="args" />`,
  }),
  args: {
    ...Default.args,
    layerFile: new File(['Layer file content'], 'layer.csv', {
      type: 'text/csv',
    }),
    polygonFile: new File(['Polygon file content'], 'polygon.csv', {
      type: 'text/csv',
    }),
  },
}

export const InvalidData: Story = {
  render: (args) => ({
    components: { FileUpload },
    setup() {
      return { args }
    },
    template: `<FileUpload v-bind="args" />`,
  }),
  args: {
    ...Default.args,
    startingAge: 5,
    finishingAge: 100,
    ageIncrement: -1, // Invalid increment
  },
}
