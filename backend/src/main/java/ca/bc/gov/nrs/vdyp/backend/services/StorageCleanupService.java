package ca.bc.gov.nrs.vdyp.backend.services;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.backend.clients.VDYPBatchClient;
import ca.bc.gov.nrs.vdyp.backend.data.entities.StorageCleanupDeletedFolderEntity;
import ca.bc.gov.nrs.vdyp.backend.data.entities.StorageCleanupRunEntity;
import ca.bc.gov.nrs.vdyp.backend.data.entities.VDYPUserEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.CleanupOutcomeModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.CleanupSetResultModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.StorageCleanupReportModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.StorageCleanupRequestModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.VDYPUserModel;
import ca.bc.gov.nrs.vdyp.backend.data.repositories.ProjectionBatchMappingRepository;
import ca.bc.gov.nrs.vdyp.backend.data.repositories.StorageCleanupDeletedFolderRepository;
import ca.bc.gov.nrs.vdyp.backend.data.repositories.StorageCleanupRunRepository;
import ca.bc.gov.nrs.vdyp.backend.data.repositories.VDYPUserRepository;
import ca.bc.gov.nrs.vdyp.backend.exceptions.StorageCleanupException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

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
	private final VDYPUserRepository userRepository;
	private final StorageCleanupRunRepository runRepository;
	private final StorageCleanupDeletedFolderRepository deletedFolderRepository;
	private final VDYPBatchClient batchClient;

	public StorageCleanupService(
			ProjectionBatchMappingRepository mappingRepository, VDYPUserRepository userRepository,
			StorageCleanupRunRepository runRepository, StorageCleanupDeletedFolderRepository deletedFolderRepository,
			@RestClient VDYPBatchClient batchClient
	) {
		this.mappingRepository = mappingRepository;
		this.userRepository = userRepository;
		this.runRepository = runRepository;
		this.deletedFolderRepository = deletedFolderRepository;
		this.batchClient = batchClient;
	}

	/**
	 * Scans the batch PVC for leftover job folders and, unless dryRun is true, deletes the ones that are not protected.
	 * Never deletes anything if the protected job list or the batch call itself cannot be obtained.
	 */
	@Transactional
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
			recordRun(actingUser, report);
		}

		return report;
	}

	private void recordRun(VDYPUserModel actingUser, StorageCleanupReportModel report) {
		VDYPUserEntity actingUserEntity = userRepository.findById(UUID.fromString(actingUser.getVdypUserGUID()));

		int deletedCount = 0;
		int failedCount = 0;
		int protectedCount = 0;
		long bytesFreed = 0;
		for (CleanupSetResultModel set : report.sets()) {
			if (set.outcome() == CleanupOutcomeModel.DELETED) {
				deletedCount++;
				bytesFreed += set.bytes();
			} else if (set.outcome() == CleanupOutcomeModel.FAILED) {
				failedCount++;
			} else if (set.outcome() == CleanupOutcomeModel.PROTECTED) {
				protectedCount++;
			}
		}

		StorageCleanupRunEntity run = new StorageCleanupRunEntity();
		run.setRunByUser(actingUserEntity);
		run.setRunDate(OffsetDateTime.now());
		run.setDeletedCount(deletedCount);
		run.setFailedCount(failedCount);
		run.setProtectedCount(protectedCount);
		run.setBytesFreed(bytesFreed);
		runRepository.persist(run);

		for (CleanupSetResultModel set : report.sets()) {
			if (set.outcome() != CleanupOutcomeModel.DELETED) {
				continue;
			}
			StorageCleanupDeletedFolderEntity deletedFolder = new StorageCleanupDeletedFolderEntity();
			deletedFolder.setRun(run);
			deletedFolder.setFolderName(set.folderName());
			deletedFolder.setSizeBytes(set.bytes());
			deletedFolderRepository.persist(deletedFolder);
		}

		logger.info(
				"PVC storage cleanup run by {}: deleted={}, failed={}, protected={}, bytesFreed={}",
				actingUser.getVdypUserGUID(), deletedCount, failedCount, protectedCount, bytesFreed
		);
	}

}
