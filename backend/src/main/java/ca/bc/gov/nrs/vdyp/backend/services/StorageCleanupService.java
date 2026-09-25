package ca.bc.gov.nrs.vdyp.backend.services;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.backend.clients.VDYPBatchClient;
import ca.bc.gov.nrs.vdyp.backend.data.models.StorageCleanupReportModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.StorageCleanupRequestModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.VDYPUserModel;
import ca.bc.gov.nrs.vdyp.backend.exceptions.StorageCleanupException;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Coordinates admin-triggered PVC storage cleanup: delegates the scan (and, for an actual delete run, the deletion) to
 * the batch service, which is the sole authority on which job folders are currently in use, and records the outcome for
 * audit purposes.
 */
@ApplicationScoped
public class StorageCleanupService {

	private static final Logger logger = LoggerFactory.getLogger(StorageCleanupService.class);

	private final StorageCleanupRecorder recorder;
	private final VDYPBatchClient batchClient;

	public StorageCleanupService(StorageCleanupRecorder recorder, @RestClient VDYPBatchClient batchClient) {
		this.recorder = recorder;
		this.batchClient = batchClient;
	}

	/**
	 * Scans the batch PVC for leftover job folders and, unless dryRun is true, deletes the ones that are not protected.
	 *
	 * Deliberately not transactional: the batch call can take a while, and recording the run happens in its own
	 * transaction. Once files are deleted that cannot be undone, so a failure to record the run is logged rather than
	 * reported to the caller as a failed cleanup.
	 */
	public StorageCleanupReportModel cleanup(VDYPUserModel actingUser, boolean dryRun) {
		StorageCleanupReportModel report;
		try {
			report = batchClient.cleanupStorage(new StorageCleanupRequestModel(dryRun));
		} catch (Exception e) {
			throw new StorageCleanupException("Unable to complete PVC storage cleanup via the batch service", e);
		}

		if (!dryRun) {
			try {
				recorder.recordCleanupRun(actingUser, report);
			} catch (Exception e) {
				logger.error(
						"PVC storage cleanup run by {} deleted files but its run record could not be saved",
						actingUser == null ? null : actingUser.getVdypUserGUID(), e
				);
			}
		}

		return report;
	}

}
