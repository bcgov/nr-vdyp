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

// The dialog loads its preview when modelValue turns true, so mount it closed and then open it,
// the same way the Admin Dashboard does when "Free up Storage" is clicked.
const openDialog = (props: Record<string, unknown> = {}) => {
  cy.mountWithVuetify(AdminStorageCleanupDialog, { props: { modelValue: false, ...props } }).then(({ wrapper }) => {
    wrapper.setProps({ modelValue: true })
  })
}

describe('AdminStorageCleanupDialog.vue', () => {
  beforeEach(() => {
    cy.viewport(1280, 1000)
  })

  it('shows orphan files to delete in the preview', () => {
    cy.stub(apiClient, 'cleanupPvcStorage').resolves({ data: orphanPreview })

    openDialog()

    cy.contains('Will be deleted: 1 item(s) (5.00 MB)').should('be.visible')
    cy.contains('Kept (in use): 0 item(s)').should('be.visible')
    cy.contains('Ignored (unrecognized): 1 item(s)').should('be.visible')
    cy.contains('button', 'Delete 1 Item(s) (5.00 MB)').should('exist')
  })

  it('shows nothing to delete when all folders are in use', () => {
    cy.stub(apiClient, 'cleanupPvcStorage').resolves({ data: inUsePreview })

    openDialog()

    cy.contains('Nothing to delete. All batch storage folders are in use.').should('be.visible')
    cy.contains('Kept (in use): 1 item(s)').should('be.visible')
    cy.contains('button', 'Delete').should('not.exist')
  })

  it('deletes after confirming and shows the result', () => {
    const stub = cy.stub(apiClient, 'cleanupPvcStorage')
    stub.withArgs(true).resolves({ data: orphanPreview })
    stub.withArgs(false).resolves({ data: deleteReport })

    openDialog()

    cy.contains('button', 'Delete 1 Item(s) (5.00 MB)').click()
    cy.contains('Cleanup Complete').should('be.visible')
    cy.contains('Deleted 1 item(s), freeing 5.00 MB.').should('be.visible')
    cy.contains('button', 'Close').should('exist')
  })

  it('closes without deleting when Cancel is clicked', () => {
    const stub = cy.stub(apiClient, 'cleanupPvcStorage').resolves({ data: orphanPreview })
    const onUpdate = cy.spy().as('updateSpy')

    openDialog({ 'onUpdate:modelValue': onUpdate })

    cy.contains('button', 'Cancel').click()
    cy.get('@updateSpy').should('have.been.calledWith', false)
    cy.wrap(stub).should('not.have.been.calledWith', false)
  })
})
