import type { Meta, StoryObj } from '@storybook/vue3-vite'
import TheFooter from './TheFooter.vue'

const meta = {
  title: 'components/layout/TheFooter',
  component: TheFooter,
  tags: ['autodocs'],
  argTypes: {
    hideAcknowledgement: {
      control: 'boolean',
      description: 'Hide the land acknowledgement section',
    },
    hideLogoAndLinks: {
      control: 'boolean',
      description: 'Hide the logo and links section',
    },
    hideCopyright: {
      control: 'boolean',
      description: 'Hide the copyright section',
    },
  },
} satisfies Meta<typeof TheFooter>

export default meta
type Story = StoryObj<typeof meta>

export const Default: Story = {
  args: {
    hideAcknowledgement: false,
    hideLogoAndLinks: false,
    hideCopyright: false,
  },
}

export const WithoutAcknowledgement: Story = {
  args: {
    hideAcknowledgement: true,
    hideLogoAndLinks: false,
    hideCopyright: false,
  },
}

export const WithoutLogoAndLinks: Story = {
  args: {
    hideAcknowledgement: false,
    hideLogoAndLinks: true,
    hideCopyright: false,
  },
}

export const CopyrightOnly: Story = {
  args: {
    hideAcknowledgement: true,
    hideLogoAndLinks: true,
    hideCopyright: false,
  },
}
