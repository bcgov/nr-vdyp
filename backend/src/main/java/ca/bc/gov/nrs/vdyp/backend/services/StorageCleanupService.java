package ca.bc.gov.nrs.vdyp.backend.services;

import java.util.List;
import java.util.UUID;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.backend.clients.VDYPBatchClient;
import ca.bc.gov.nrs.vdyp.backend.data.models.StorageCleanupReportModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.StorageCleanupRequestModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.VDYPUserModel;
import ca.bc.gov.nrs.vdyp.backend.data.repositories.ProjectionBatchMappingRepository;
import ca.bc.gov.nrs.vdyp.backend.exceptions.StorageCleanupException;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Coordinates admin-triggered PVC storage cleanup: tells the batch service which job GUIDs must not be touched (because
 * their projection is still Running or Stuck), and, for an actual delete run, records the outcome for audit purposes. A
 * projection that is only Queued needs no protection here - see
 * {@link ProjectionBatchMappingRepository#findProtectedBatchJobGuids()}.
 */
@ApplicationScoped
public class StorageCleanupService {

	private static final Logger logger = LoggerFactory.getLogger(StorageCleanupService.class);

	private final ProjectionBatchMappingRepository mappingRepository;
	private final StorageCleanupRecorder recorder;
	private final VDYPBatchClient batchClient;

	public StorageCleanupService(
			ProjectionBatchMappingRepository mappingRepository, StorageCleanupRecorder recorder,
			@RestClient VDYPBatchClient batchClient
	) {
		this.mappingRepository = mappingRepository;
		this.recorder = recorder;
		this.batchClient = batchClient;
	}

	/**
	 * Scans the batch PVC for leftover job folders and, unless dryRun is true, deletes the ones that are not protected.
	 * Never deletes anything if the protected job list or the batch call itself cannot be obtained.
	 *
	 * Deliberately not transactional: the batch call can take a while, and recording the run happens in its own
	 * transaction. Once files are deleted that cannot be undone, so a failure to record the run is logged rather than
	 * reported to the caller as a failed cleanup.
	 */
	public StorageCleanupReportModel cleanup(VDYPUserModel actingUser, boolean dryRun) {
		List<String> protectedJobGuids = mappingRepository.findProtectedBatchJobGuids().stream().map(UUID::toString)
				.toList();

		StorageCleanupReportModel report;
		try {
			report = batchClient.cleanupStorage(new StorageCleanupRequestModel(dryRun, protectedJobGuids));
		} catch (Exception e) {
			throw new StorageCleanupException("Unable to complete PVC storage cleanup via the batch service", e);
		}

		if (!dryRun) {
			try {
				recorder.record(actingUser, report);
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
