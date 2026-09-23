import type { Meta, StoryObj } from '@storybook/vue3-vite'
import { ref, onMounted } from 'vue'
import AdminStorageCleanupDialog from './AdminStorageCleanupDialog.vue'
import { apiClient } from '@/services/apiClient'

const MB_5 = 5 * 1024 * 1024

const orphanPreview = {
  dryRun: true,
  scanned: 1,
  totalBytes: MB_5,
  sets: [{ jobGuid: 'a', folderName: 'vdyp-batch-a', bytes: MB_5, outcome: 'DELETABLE', detail: null }],
  skippedNames: ['lost+found'],
}

const inUsePreview = {
  dryRun: true,
  scanned: 1,
  totalBytes: 0,
  sets: [{ jobGuid: 'b', folderName: 'vdyp-batch-b', bytes: 1024, outcome: 'PROTECTED', detail: 'Running' }],
  skippedNames: [],
}

const deleteReport = {
  dryRun: false,
  scanned: 1,
  totalBytes: MB_5,
  sets: [{ jobGuid: 'a', folderName: 'vdyp-batch-a', bytes: MB_5, outcome: 'DELETED', detail: null }],
  skippedNames: [],
}

// Replaces the API call with hardcoded reports, then opens the dialog after mount
// because the dialog loads its preview only when modelValue turns true.
const renderWithPreview = (preview: object) => () => ({
  components: { AdminStorageCleanupDialog },
  setup() {
    apiClient.cleanupPvcStorage = (dryRun?: boolean) =>
      Promise.resolve({ data: dryRun === false ? deleteReport : preview }) as any
    const isOpen = ref(false)
    onMounted(() => {
      isOpen.value = true
    })
    return { isOpen }
  },
  template: `<AdminStorageCleanupDialog v-model="isOpen" />`,
})

const meta: Meta<typeof AdminStorageCleanupDialog> = {
  title: 'components/projection/list/AdminStorageCleanupDialog',
  component: AdminStorageCleanupDialog,
  tags: ['autodocs'],
}

export default meta
type Story = StoryObj<typeof AdminStorageCleanupDialog>

/** Orphan files can be deleted. Click the Delete button to see the Cleanup Complete result. */
export const Default: Story = {
  render: renderWithPreview(orphanPreview),
}

/** All batch storage folders are held by running projections, so nothing can be deleted. */
export const NothingToDelete: Story = {
  render: renderWithPreview(inUsePreview),
}
