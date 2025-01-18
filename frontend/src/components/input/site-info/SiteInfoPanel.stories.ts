import type { Meta, StoryObj } from '@storybook/vue3'
import SiteInfoPanel from './SiteInfoPanel.vue'

const meta: Meta<typeof SiteInfoPanel> = {
  title: 'components/input/site-info/SiteInfoPanel',
  component: SiteInfoPanel,
  argTypes: {
    panelOpenStates: {
      control: { type: 'object' },
      description: 'State of panel (open or closed)',
      defaultValue: {
        siteInfo: 1, // 1 for open, 0 for closed
      },
    },
    becZone: {
      control: { type: 'text' },
      description: 'BEC Zone selection.',
      defaultValue: 'BEC Zone A',
    },
    ecoZone: {
      control: { type: 'text' },
      description: 'Eco Zone selection.',
      defaultValue: 'Eco Zone A',
    },
    incSecondaryHeight: {
      control: { type: 'boolean' },
      description: 'Includes Secondary Dominant Height.',
      defaultValue: false,
    },
    selectedSiteSpecies: {
      control: { type: 'text' },
      description: 'Selected Site Species.',
      defaultValue: 'Species A',
    },
    siteSpeciesValues: {
      control: { type: 'text' },
      description: 'Site Species Value radio selection.',
      defaultValue: 'Value A',
    },
    bha50SiteIndex: {
      control: { type: 'number' },
      description: 'BHA 50 Site Index.',
      defaultValue: 25.5,
    },
    isConfirmEnabled: {
      control: { type: 'boolean' },
      description: 'Determines if the confirm actions are enabled.',
      defaultValue: true,
    },
    isConfirmed: {
      control: { type: 'boolean' },
      description: 'Indicates if the panel is confirmed.',
      defaultValue: false,
    },
  },
  tags: ['autodocs'],
}

export default meta

type Story = StoryObj<typeof SiteInfoPanel>

export const Default: Story = {
  render: (args) => ({
    components: { SiteInfoPanel },
    setup() {
      return { args }
    },
    template: `<SiteInfoPanel v-bind="args" />`,
  }),
  args: {
    panelOpenStates: { siteInfo: 1 },
    becZone: 'BEC Zone A',
    ecoZone: 'Eco Zone A',
    incSecondaryHeight: false,
    selectedSiteSpecies: 'Species A',
    siteSpeciesValues: 'Value A',
    bha50SiteIndex: 25.5,
    isConfirmEnabled: true,
    isConfirmed: false,
  },
}

export const Confirmed: Story = {
  render: (args) => ({
    components: { SiteInfoPanel },
    setup() {
      return { args }
    },
    template: `<SiteInfoPanel v-bind="args" />`,
  }),
  args: {
    panelOpenStates: { siteInfo: 0 },
    becZone: 'BEC Zone B',
    ecoZone: 'Eco Zone B',
    incSecondaryHeight: true,
    selectedSiteSpecies: 'Species B',
    siteSpeciesValues: 'Value B',
    bha50SiteIndex: 30.0,
    isConfirmEnabled: false,
    isConfirmed: true,
  },
}

export const EmptyState: Story = {
  render: (args) => ({
    components: { SiteInfoPanel },
    setup() {
      return { args }
    },
    template: `<SiteInfoPanel v-bind="args" />`,
  }),
  args: {
    panelOpenStates: { siteInfo: 1 },
    becZone: '',
    ecoZone: '',
    incSecondaryHeight: false,
    selectedSiteSpecies: '',
    siteSpeciesValues: '',
    bha50SiteIndex: null,
    isConfirmEnabled: true,
    isConfirmed: false,
  },
}
