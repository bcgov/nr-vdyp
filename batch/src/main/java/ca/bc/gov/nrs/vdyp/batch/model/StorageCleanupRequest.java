package ca.bc.gov.nrs.vdyp.batch.model;

/**
 * Request body for the PVC storage cleanup endpoint.
 *
 * @param dryRun when true (the default expected from callers), candidates are evaluated but nothing is deleted
 */
public record StorageCleanupRequest(boolean dryRun) {
}
