import type { Meta, StoryObj } from '@storybook/vue3-vite'
import BCLogo from './BCLogo.vue'

const meta = {
  title: 'components/layout/BCLogo',
  component: BCLogo,
  tags: ['autodocs'],
  decorators: [
    () => ({
      template: `
        <div style="background-color: rgb(0, 51, 102); padding: 20px; display: flex; justify-content: center;">
          <story />
        </div>
      `,
    }),
  ],
  argTypes: {
    maxHeight: {
      control: { type: 'range', min: 10, max: 300, step: 10 },
      description: 'Maximum height of the logo (in pixels)',
    },
    maxWidth: {
      control: { type: 'range', min: 50, max: 600, step: 10 },
      description: 'Maximum width of the logo (in pixels)',
    },
    marginLeft: {
      control: { type: 'range', min: 0, max: 50, step: 5 },
      description: 'Margin left of the logo (in pixels)',
    },
  },
} satisfies Meta<typeof BCLogo>

export default meta
type Story = StoryObj<typeof meta>

export const Default: Story = {
  args: {
    maxHeight: 50,
    maxWidth: 150,
    marginLeft: 15,
  },
}
