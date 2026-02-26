import type { Meta, StoryObj } from '@storybook/vue3-vite'
import ReportingOutput from './ReportingOutput.vue'
import { REPORTING_TAB } from '@/constants/constants'

const meta: Meta<typeof ReportingOutput> = {
  title: 'components/projection-reporting/ReportingOutput',
  component: ReportingOutput,
  tags: ['autodocs'],
  argTypes: {
    data: {
      control: { type: 'object' },
      description: 'Array of text lines to display. Lines are joined with newlines.',
    },
    tabname: {
      control: { type: 'select' },
      options: Object.values(REPORTING_TAB),
      description:
        'Active reporting tab. The MODEL_REPORT tab uses `min-height` instead of a fixed `height` to fill available space.',
    },
  },
  parameters: {
    docs: {
      description: {
        component: `
Monospace pre-formatted text output panel for reporting data.

- Joins the \`data\` array with newlines and renders it in a scrollable, monospace box.
- **MODEL_REPORT** tab: uses \`min-height: 420px\` so the panel can grow with content.
- **All other tabs**: uses a fixed \`height: 420px\`.
        `,
      },
    },
  },
}

export default meta

type Story = StoryObj<typeof ReportingOutput>

const sampleYieldLines = [
  '                           A Sample Report Title',
  '',
  '                            VDYP Yield Table Report',
  '         Trembling Aspen (30.0%), Balsam (25.0%), Hemlock (20.0%)',
  '',
  'TOT  HT   HT   DIA     BA        TPH     |  VOLUME    |  VOLUME    ',
  'AGE (m)   (m)  (cm) (m**2/ha) (trees/ha) | (m**3/ha)  | (m**3/ha)  ',
  '--------------------------------------------------------------------',
  ' 10  3.6                                 |            |            ',
  ' 20  8.2  7.5  9.4    12.3       580     |   85.4     |   72.1     ',
  ' 30 13.4 12.1 14.6    22.8       430     |  190.3     |  165.8     ',
  ' 40 17.8 15.9 18.9    30.5       340     |  295.6     |  261.2     ',
  ' 50 21.3 19.0 22.7    37.2       290     |  388.0     |  345.7     ',
]

const sampleErrorMessages = [
  'Polygon 17811400: no primary layer found for any projection type',
  'Polygon 17811402: no primary layer found for any projection type',
  'Polygon 17811404: no primary layer found for any projection type',
  'Polygon 17821464: encountered error in FIP_START when running polygon: Estimated base area of 0 was lower than expected 0.05',
  'Polygon 19233926: no primary layer found for any projection type',
]

const sampleLogMessages = [
  'batch-7-partition0-projection-HCSV-2026_02_25_21_33_37_3874: starting projection (type HCSV)',
  'Processed 1 polygons...',
  'Processing summary: 1 polygons processed + 0 skipped = 1 seen',
  'batch-7-partition0-projection-HCSV-2026_02_25_21_33_37_3874: completing projection (type HCSV); duration: 1064ms',
]

export const ModelReport: Story = {
  args: {
    data: sampleYieldLines,
    tabname: REPORTING_TAB.MODEL_REPORT,
  },
  parameters: {
    docs: {
      description: {
        story:
          'Model Report tab with yield table data. Uses `min-height: 420px` so the panel expands with content.',
      },
    },
  },
}

export const ViewErrorMessages: Story = {
  args: {
    data: sampleErrorMessages,
    tabname: REPORTING_TAB.VIEW_ERR_MSG,
  },
  parameters: {
    docs: {
      description: {
        story: 'View Error Messages tab showing projection errors. Uses a fixed height of 420px.',
      },
    },
  },
}

export const ViewLogFile: Story = {
  args: {
    data: sampleLogMessages,
    tabname: REPORTING_TAB.VIEW_LOG_FILE,
  },
  parameters: {
    docs: {
      description: {
        story: 'View Log File tab showing batch processing log lines. Uses a fixed height of 420px.',
      },
    },
  },
}

export const Empty: Story = {
  args: {
    data: [],
    tabname: REPORTING_TAB.MODEL_REPORT,
  },
  parameters: {
    docs: {
      description: {
        story: 'Empty state - no data lines. The panel still renders with the correct dimensions.',
      },
    },
  },
}
