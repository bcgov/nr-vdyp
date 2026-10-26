<template>
  <v-dialog v-model="isOpen" persistent max-width="560">
    <v-card class="cleanup-dialog">
      <div class="cleanup-dialog--header">
        <h2 class="cleanup-dialog--title">
          {{ resultReport ? STORAGE_CLEANUP_DIALOG.RESULT_TITLE : STORAGE_CLEANUP_DIALOG.TITLE }}
        </h2>
        <v-icon class="cleanup-dialog--close-icon" @click="handleClose">mdi-close</v-icon>
      </div>

      <div class="cleanup-dialog--body">
        <div v-if="phase === 'loading'" class="cleanup-dialog--status">
          <v-progress-circular indeterminate color="primary" size="24" class="mr-2" />
          <span>{{ STORAGE_CLEANUP_DIALOG.LOADING_PREVIEW }}</span>
        </div>

        <div v-else-if="phase === 'preview-error'" class="cleanup-dialog--status">
          <p>{{ STORAGE_CLEANUP_DIALOG.PREVIEW_ERROR }}</p>
        </div>

        <div v-else-if="phase === 'delete-error'" class="cleanup-dialog--status">
          <p>{{ STORAGE_CLEANUP_DIALOG.DELETE_ERROR }}</p>
        </div>

        <div v-else-if="phase === 'preview' && previewReport">
          <p v-if="deletableCount === 0" class="cleanup-dialog--line">
            {{ STORAGE_CLEANUP_DIALOG.NOTHING_TO_DELETE }}
          </p>
          <template v-else>
            <p class="cleanup-dialog--line cleanup-dialog--line-primary">
              {{ STORAGE_CLEANUP_DIALOG.WILL_DELETE(deletableCount, formatBytes(deletableBytes)) }}
            </p>
            <p class="cleanup-dialog--line">{{ STORAGE_CLEANUP_DIALOG.SAFE_TO_DELETE }}</p>
            <p class="cleanup-dialog--warning">{{ STORAGE_CLEANUP_DIALOG.WARNING }}</p>
          </template>
          <p class="cleanup-dialog--line">{{ STORAGE_CLEANUP_DIALOG.PROTECTED(protectedCount) }}</p>
          <p class="cleanup-dialog--line">{{ STORAGE_CLEANUP_DIALOG.SKIPPED(skippedCount) }}</p>
        </div>

        <div v-else-if="phase === 'deleting'" class="cleanup-dialog--status">
          <v-progress-circular indeterminate color="primary" size="24" class="mr-2" />
          <span>{{ STORAGE_CLEANUP_DIALOG.DELETING }}</span>
        </div>

        <div v-else-if="phase === 'result' && resultReport">
          <p class="cleanup-dialog--line cleanup-dialog--line-primary">
            {{
              STORAGE_CLEANUP_DIALOG.RESULT_SUMMARY(deletedCount, failedCount, formatBytes(resultReport.totalBytes))
            }}
          </p>
          <p class="cleanup-dialog--line">{{ STORAGE_CLEANUP_DIALOG.RESULT_NOTE }}</p>
        </div>
      </div>

      <div class="cleanup-dialog--actions">
        <v-spacer></v-spacer>
        <AppButton
          v-if="phase !== 'result'"
          :label="STORAGE_CLEANUP_DIALOG.CANCEL"
          variant="tertiary"
          @click="handleClose"
        />
        <AppButton
          v-if="phase === 'preview-error' || phase === 'delete-error'"
          :label="STORAGE_CLEANUP_DIALOG.RETRY"
          variant="primary"
          class="ml-2"
          @click="loadPreview"
        />
        <AppButton
          v-if="phase === 'preview' && deletableCount > 0"
          :label="STORAGE_CLEANUP_DIALOG.CONFIRM_DELETE(deletableCount, formatBytes(deletableBytes))"
          variant="danger"
          class="ml-2"
          @click="handleConfirmDelete"
        />
        <AppButton
          v-if="phase === 'result'"
          :label="STORAGE_CLEANUP_DIALOG.CLOSE"
          variant="primary"
          class="ml-2"
          @click="handleClose"
        />
      </div>
    </v-card>
  </v-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { AppButton } from '@/components'
import { STORAGE_CLEANUP_DIALOG } from '@/constants/message'
import { cleanupPvcStorage } from '@/services/adminService'
import { CleanupOutcomeModel, type StorageCleanupReportModel } from '@/services/vdyp-api'
import { formatBytes } from '@/utils/util'

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  completed: [report: StorageCleanupReportModel]
}>()

type Phase = 'loading' | 'preview' | 'preview-error' | 'deleting' | 'delete-error' | 'result'

const phase = ref<Phase>('loading')
const previewReport = ref<StorageCleanupReportModel | null>(null)
const resultReport = ref<StorageCleanupReportModel | null>(null)

const isOpen = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

const countByOutcome = (report: StorageCleanupReportModel | null, outcome: CleanupOutcomeModel) =>
  report?.sets.filter((set) => set.outcome === outcome).length ?? 0

const deletableCount = computed(() => countByOutcome(previewReport.value, CleanupOutcomeModel.Deletable))
const deletableBytes = computed(
  () =>
    previewReport.value?.sets
      .filter((set) => set.outcome === CleanupOutcomeModel.Deletable)
      .reduce((total, set) => total + set.bytes, 0) ?? 0,
)
const protectedCount = computed(() => countByOutcome(previewReport.value, CleanupOutcomeModel.Protected))
const skippedCount = computed(() => previewReport.value?.skippedNames.length ?? 0)

const deletedCount = computed(() => countByOutcome(resultReport.value, CleanupOutcomeModel.Deleted))
const failedCount = computed(() => countByOutcome(resultReport.value, CleanupOutcomeModel.Failed))

const loadPreview = async () => {
  phase.value = 'loading'
  try {
    previewReport.value = await cleanupPvcStorage(true)
    phase.value = 'preview'
  } catch (err) {
    console.error('Error previewing PVC storage cleanup:', err)
    phase.value = 'preview-error'
  }
}

const handleConfirmDelete = async () => {
  phase.value = 'deleting'
  try {
    resultReport.value = await cleanupPvcStorage(false)
    phase.value = 'result'
    emit('completed', resultReport.value)
  } catch (err) {
    console.error('Error deleting unnecessary PVC storage files:', err)
    phase.value = 'delete-error'
  }
}

const handleClose = () => {
  isOpen.value = false
}

watch(
  () => props.modelValue,
  (open) => {
    if (open) {
      previewReport.value = null
      resultReport.value = null
      loadPreview()
    }
  },
)
</script>

<style scoped>
.cleanup-dialog {
  display: flex;
  flex-direction: column;
}

.cleanup-dialog--header {
  display: inline-flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: flex-start;
  padding: var(--layout-padding-medium) var(--layout-padding-large);
  border-bottom: var(--layout-border-width-small) solid var(--surface-color-border-default);
}

.cleanup-dialog--title {
  flex-grow: 1;
  font: var(--typography-bold-h5);
  color: var(--typography-color-primary);
  margin: 0;
}

.cleanup-dialog--close-icon {
  justify-self: flex-end;
  color: var(--icons-color-primary);
  cursor: pointer;
}

.cleanup-dialog--body {
  padding: var(--layout-padding-medium) var(--layout-padding-large);
}

.cleanup-dialog--status {
  display: flex;
  align-items: center;
}

.cleanup-dialog--line {
  font: var(--typography-regular-body);
  color: var(--typography-color-primary);
  margin: 0 0 var(--layout-margin-small) 0;
}

.cleanup-dialog--line-primary {
  font: var(--typography-bold-body);
}

.cleanup-dialog--warning {
  font: var(--typography-regular-body);
  color: var(--support-color-danger, #d32f2f);
  margin: 0 0 var(--layout-margin-medium) 0;
}

.cleanup-dialog--actions {
  display: flex;
  padding: var(--layout-padding-medium) var(--layout-padding-large);
  gap: var(--layout-padding-small);
}
</style>
