import type { Meta, StoryObj } from '@storybook/vue3-vite'
import ModelSelectionContainer from './ModelSelectionContainer.vue'
import { createVuetify } from 'vuetify'
import { VApp } from 'vuetify/components'
import 'vuetify/styles'

const vuetify = createVuetify()

const meta: Meta<typeof ModelSelectionContainer> = {
  title: 'components/projection-setup/ModelSelectionContainer',
  component: ModelSelectionContainer,
  decorators: [
    () => ({
      components: { VApp },
      template: '<v-app><story /></v-app>',
      setup() {
        return { vuetify }
      },
    }),
  ],
  argTypes: {
    'onUpdate:modelSelection': {
      action: 'update:modelSelection',
      description: 'Emits the selected model value to the parent component.',
    },
  },
  tags: ['autodocs'],
}

export default meta

type Story = StoryObj<typeof ModelSelectionContainer>

export const Default: Story = {
  render: (args, { argTypes }) => ({
    components: { ModelSelectionContainer },
    props: Object.keys(argTypes),
    template: `
      <ModelSelectionContainer @update:modelSelection="args['onUpdate:modelSelection']" />
    `,
    setup() {
      return { args, vuetify }
    },
  }),
}
