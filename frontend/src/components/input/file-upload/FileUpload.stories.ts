import type { Meta, StoryObj } from '@storybook/vue3'
import FileUpload from './FileUpload.vue'

const meta: Meta<typeof FileUpload> = {
  title: 'components/input/file-upload/FileUpload',
  component: FileUpload,
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
}
