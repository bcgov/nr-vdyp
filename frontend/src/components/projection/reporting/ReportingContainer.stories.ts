import type { Meta, StoryObj } from '@storybook/vue3-vite'
import ReportingContainer from './ReportingContainer.vue'
import { useProjectionStore } from '@/stores/projection/projectionStore'
import { REPORTING_TAB } from '@/constants/constants'
import type { ReportingTab } from '@/types/types'

const meta: Meta<typeof ReportingContainer> = {
  title: 'components/projection-reporting/ReportingContainer',
  component: ReportingContainer,
  tags: ['autodocs'],
  argTypes: {
    tabname: {
      control: { type: 'select' },
      options: Object.values(REPORTING_TAB),
      description:
        'Active reporting tab. Controls which store data is displayed and the Download button label.',
    },
  },
  parameters: {
    docs: {
      description: {
        component: `
Full-width container for a single reporting tab, composing \`ReportingActions\` and \`ReportingOutput\`.

**Data source:** All display data is read reactively from \`useProjectionStore\`:
- **Model Report** tab - displays \`txtYieldLines\`; download uses \`csvYieldLines\`
- **View Error Messages** tab - displays and downloads \`errorMessages\`
- **View Log File** tab - displays and downloads \`logMessages\`

**Button state:** Both Print and Download buttons are disabled while the relevant download dataset is empty.

**Events handled internally (not emitted upward):**
- \`@print\` -> calls \`printReport()\` via \`reportService\`
- \`@download\` -> calls \`downloadCSVFile\` (Model Report) or \`downloadTextFile\` (other tabs)
- \`@downloadrawresult\` -> calls \`downloadFile()\` with the raw result ZIP from the store
        `,
      },
    },
  },
}

export default meta

type Story = StoryObj<typeof ReportingContainer>

const sampleTxtYieldLines = [
  '                           A Sample Report Title',
  '',
  '                            VDYP Yield Table Report',
  '         Trembling Aspen (30.0%), Balsam (25.0%), Hemlock (20.0%),                 Limber Pine (15.0%), Engelmann Spruce (10.0%)',
  '',
  '               Quad                      |   Whole    |   Close    |    Net     | Net Decay  | Net Decay  ',
  '    Site Lorey Stnd                      |   Stem     |Utilization |   Decay    | and Waste  |Waste, Brkg ',
  'TOT  HT   HT   DIA     BA        TPH     |  VOLUME    |  VOLUME    |  VOLUME    |  VOLUME    |  VOLUME    ',
  'AGE (m)   (m)  (cm) (m**2/ha) (trees/ha) | (m**3/ha)  | (m**3/ha)  | (m**3/ha)  | (m**3/ha)  | (m**3/ha)  ',
  '----------------------------------------------------------------------------------------------------------',
  ' 10  3.6                                 |            |            |            |            |            ',
  '',
  'INFO: directly assigning estimated site index of 16.3 to species "AT" and recomputing input height',
  'WARN: CC was not supplied. Using a default CC of 52% for this (leading) species (Interior)',
  'WARN: projected data for the PRIMARY layer was not generated at calendar year 2032',
  'WARN: projected data for the PRIMARY layer was not generated at calendar year 2032',
  'WARN: projected data for the PRIMARY layer was not generated at calendar year 2032',
  'WARN: projected data for the PRIMARY layer was not generated at calendar year 2032',
  'WARN: projected data for the PRIMARY layer was not generated at calendar year 2032',
  'WARN: projected data for the PRIMARY layer was not generated at calendar year 2032',
  'WARN: projected data for the PRIMARY layer was not generated at calendar year 2032',
  'WARN: projected data for the PRIMARY layer was not generated at calendar year 2032',
  'WARN: projected data for the PRIMARY layer was not generated at calendar year 2032',
  '',
  '',
  'TABLE PROPERTIES...',
  '',
  'VDYP UI Version Number... 8.0            % Stockable Area Supplied 5.0',
  'VDYP SRVR Version Number. 8.0            CFS Eco Zone............. TaigaPlains',
  'VDYP SI Version Number... 8.0            Trees Per Hectare........ 1000.0',
  'SINDEX Version Number.... 8.0            Measured Basal Area...... 10.0',
  'Species 1................ AT  (30.0%)    Starting Total Age....... 10',
  'Species 2................ B   (25.0%)    Finishing Total Age...... 11',
  'Species 3................ H   (20.0%)    Age Increment............ 6',
  'Species 4................ PF  (15.0%)    Projected Values......... Volume',
  'Species 5................ SE  (10.0%)    Min DBH Limit: AT........ 7.5cm+',
  'VRI Calc Mode............ 1              Min DBH Limit: B......... 12.5cm+',
  'BEC Zone................. ESSF           Min DBH Limit: H......... 7.5cm+',
  'Incl Second Species Ht... No             Min DBH Limit: PA........ 7.5cm+',
  '% Crown Closure Supplied. <Not Used>     Min DBH Limit: S......... 7.5cm+',
  '',
  'Species Parameters...',
  'Species |  % Comp | Tot Age |  BH Age |  Height |    SI   |  YTBH   ',
  '--------+---------+---------+---------+---------+---------+---------',
  'AT      |   30.0  |      4  |      1  |   1.43  |  16.30  |   3.70  ',
  'B       |   25.0  |    N/A  |    N/A  |    N/A  |    N/A  |    N/A  ',
  'H       |   20.0  |    N/A  |    N/A  |    N/A  |    N/A  |    N/A  ',
  'PF      |   15.0  |    N/A  |    N/A  |    N/A  |    N/A  |    N/A  ',
  'SE      |   10.0  |    N/A  |    N/A  |    N/A  |    N/A  |    N/A  ',
  '',
  'Site Index Curves Used...',
  '  Age Range | Species | SI Curve Name                                     ',
  ' -----------+---------+-------------------------------------------------',
  '',
  'Additional Stand Attributes:',
  '----------------------------',
  '',
  '        None Applied.',
]

const sampleCsvYieldLines = [
  'Polygon,Layer,Species,Age,Height,SI,BA,WSV,CW,MV,DW',
  '1001,1,Fir,80,28.5,22,42.5,320.4,48.2,280.3,240.1',
  '1001,1,Spruce,75,25.0,19,18.3,120.5,18.0,98.4,82.0',
  '1002,1,Cedar,95,30.2,24,55.0,410.0,61.5,355.0,300.0',
  '1002,1,Hemlock,90,28.8,21,30.2,225.0,33.8,192.0,160.0',
  '1003,1,Pine,60,22.0,18,25.0,185.0,27.8,160.0,140.0',
]

const sampleErrorMessages = [
  'Polygon 17811400: no primary layer found for any projection type',
  'Polygon 17811402: no primary layer found for any projection type',
  'Polygon 17811404: no primary layer found for any projection type',
  'Polygon 17811406: no primary layer found for any projection type',
  'Polygon 17811407: no primary layer found for any projection type',
  'Polygon 17811410: no primary layer found for any projection type',
  'Polygon 17811411: no primary layer found for any projection type',
  'Polygon 17811412: no primary layer found for any projection type',
  'Polygon 17811417: no primary layer found for any projection type',
  'Polygon 17811420: no primary layer found for any projection type',
  'Polygon 17811426: no primary layer found for any projection type',
  'Polygon 17811431: no primary layer found for any projection type',
  'Polygon 17811432: no primary layer found for any projection type',
  'Polygon 17821464: encountered error in FIP_START when running polygon: Estimated base area of 0 was lower than expected 0.05',
  'Estimated base area of 0 was lower than expected 0.05',
  'Polygon 19233926: no primary layer found for any projection type',
]

const sampleLogMessages = [
  'batch-7-partition0-projection-HCSV-2026_02_25_21_33_37_3874: starting projection (type HCSV)',
  'Processed 1 polygons...',
  'Processing summary: 1 polygons processed + 0 skipped = 1 seen',
  'batch-7-partition0-projection-HCSV-2026_02_25_21_33_37_3874: completing projection (type HCSV); duration: 1064ms',
]

export const ModelReport_WithData: Story = {
  render: (args) => ({
    components: { ReportingContainer },
    setup() {
      const store = useProjectionStore()
      store.txtYieldLines = sampleTxtYieldLines
      store.csvYieldLines = sampleCsvYieldLines
      const tabname = args.tabname as ReportingTab
      return { tabname }
    },
    template: `<ReportingContainer :tabname="tabname" />`,
  }),
  args: {
    tabname: REPORTING_TAB.MODEL_REPORT as ReportingTab,
  },
  parameters: {
    docs: {
      description: {
        story:
          'Model Report tab with yield table data loaded. Print and "Download Yield Table" buttons are enabled.',
      },
    },
  },
}

export const ModelReport_Empty: Story = {
  render: (args) => ({
    components: { ReportingContainer },
    setup() {
      const store = useProjectionStore()
      store.txtYieldLines = []
      store.csvYieldLines = []
      const tabname = args.tabname as ReportingTab
      return { tabname }
    },
    template: `<ReportingContainer :tabname="tabname" />`,
  }),
  args: {
    tabname: REPORTING_TAB.MODEL_REPORT as ReportingTab,
  },
  parameters: {
    docs: {
      description: {
        story:
          'Model Report tab before a projection has been run. Both buttons are disabled and the output area is blank.',
      },
    },
  },
}

export const ViewErrorMessages_WithData: Story = {
  render: (args) => ({
    components: { ReportingContainer },
    setup() {
      const store = useProjectionStore()
      store.errorMessages = sampleErrorMessages
      const tabname = args.tabname as ReportingTab
      return { tabname }
    },
    template: `<ReportingContainer :tabname="tabname" />`,
  }),
  args: {
    tabname: REPORTING_TAB.VIEW_ERR_MSG as ReportingTab,
  },
  parameters: {
    docs: {
      description: {
        story:
          'View Error Messages tab with validation errors and warnings. Print and Download buttons are enabled.',
      },
    },
  },
}

export const ViewErrorMessages_Empty: Story = {
  render: (args) => ({
    components: { ReportingContainer },
    setup() {
      const store = useProjectionStore()
      store.errorMessages = []
      const tabname = args.tabname as ReportingTab
      return { tabname }
    },
    template: `<ReportingContainer :tabname="tabname" />`,
  }),
  args: {
    tabname: REPORTING_TAB.VIEW_ERR_MSG as ReportingTab,
  },
  parameters: {
    docs: {
      description: {
        story: 'View Error Messages tab with no errors - buttons disabled, output area is blank.',
      },
    },
  },
}

export const ViewLogFile_WithData: Story = {
  render: (args) => ({
    components: { ReportingContainer },
    setup() {
      const store = useProjectionStore()
      store.logMessages = sampleLogMessages
      const tabname = args.tabname as ReportingTab
      return { tabname }
    },
    template: `<ReportingContainer :tabname="tabname" />`,
  }),
  args: {
    tabname: REPORTING_TAB.VIEW_LOG_FILE as ReportingTab,
  },
  parameters: {
    docs: {
      description: {
        story: 'View Log File tab with a full batch processing log. Print and Download buttons are enabled.',
      },
    },
  },
}

export const ViewLogFile_Empty: Story = {
  render: (args) => ({
    components: { ReportingContainer },
    setup() {
      const store = useProjectionStore()
      store.logMessages = []
      const tabname = args.tabname as ReportingTab
      return { tabname }
    },
    template: `<ReportingContainer :tabname="tabname" />`,
  }),
  args: {
    tabname: REPORTING_TAB.VIEW_LOG_FILE as ReportingTab,
  },
  parameters: {
    docs: {
      description: {
        story: 'View Log File tab with no log output - buttons disabled, output area is blank.',
      },
    },
  },
}
