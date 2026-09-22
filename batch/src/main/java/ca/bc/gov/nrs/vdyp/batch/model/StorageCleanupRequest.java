package ca.bc.gov.nrs.vdyp.batch.model;

import java.util.List;

/**
 * Request body for the PVC storage cleanup endpoint.
 *
 * @param dryRun            when true (the default expected from callers), candidates are evaluated but nothing is
 *                          deleted
 * @param protectedJobGuids job GUIDs the caller has determined must not be deleted (e.g. because the projection is
 *                          Running, Stuck or Queued); null is treated as an empty list
 */
public record StorageCleanupRequest(boolean dryRun, List<String> protectedJobGuids) {
}
