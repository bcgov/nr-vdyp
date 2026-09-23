package ca.bc.gov.nrs.vdyp.backend.services;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.backend.data.entities.StorageCleanupDeletedFolderEntity;
import ca.bc.gov.nrs.vdyp.backend.data.entities.StorageCleanupRunEntity;
import ca.bc.gov.nrs.vdyp.backend.data.entities.VDYPUserEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.CleanupOutcomeModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.CleanupSetResultModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.StorageCleanupReportModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.VDYPUserModel;
import ca.bc.gov.nrs.vdyp.backend.data.repositories.StorageCleanupDeletedFolderRepository;
import ca.bc.gov.nrs.vdyp.backend.data.repositories.StorageCleanupRunRepository;
import ca.bc.gov.nrs.vdyp.backend.data.repositories.VDYPUserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/**
 * Records a completed storage cleanup delete run (who ran it and what was deleted) in its own transaction, so a failure
 * to record it can be handled by the caller without affecting anything else: by the time a run is recorded, the files
 * are already deleted and that cannot be rolled back.
 */
@ApplicationScoped
public class StorageCleanupRecorder {

	private static final Logger logger = LoggerFactory.getLogger(StorageCleanupRecorder.class);

	private final VDYPUserRepository userRepository;
	private final StorageCleanupRunRepository runRepository;
	private final StorageCleanupDeletedFolderRepository deletedFolderRepository;

	public StorageCleanupRecorder(
			VDYPUserRepository userRepository, StorageCleanupRunRepository runRepository,
			StorageCleanupDeletedFolderRepository deletedFolderRepository
	) {
		this.userRepository = userRepository;
		this.runRepository = runRepository;
		this.deletedFolderRepository = deletedFolderRepository;
	}

	@Transactional(Transactional.TxType.REQUIRES_NEW)
	public void record(VDYPUserModel actingUser, StorageCleanupReportModel report) {
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
