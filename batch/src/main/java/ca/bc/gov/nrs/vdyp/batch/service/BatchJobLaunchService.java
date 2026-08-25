package ca.bc.gov.nrs.vdyp.batch.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import ca.bc.gov.nrs.vdyp.batch.configuration.BatchProperties;
import ca.bc.gov.nrs.vdyp.batch.ownership.JobClaim;
import ca.bc.gov.nrs.vdyp.batch.ownership.JobOwnershipService;
import ca.bc.gov.nrs.vdyp.batch.ownership.ServerCapacityService;
import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;
import ca.bc.gov.nrs.vdyp.batch.util.BatchUtils;

@Service
public class BatchJobLaunchService {

	private static final Logger logger = LoggerFactory.getLogger(BatchJobLaunchService.class);
	private static final String JOB_NAME = "VdypFetchAndPartitionJob";

	private final Job vdypBatchJob;
	private final BatchProperties batchProperties;
	private final ServerCapacityService serverCapacityService;
	private final JobOwnershipService ownershipService;
	private final JobExplorer jobExplorer;
	private final ClaimBoundJobLauncher claimBoundJobLauncher;

	public BatchJobLaunchService(
			@Qualifier("fetchAndPartitionJob") Job vdypBatchJob, BatchProperties batchProperties,
			ServerCapacityService serverCapacityService, JobOwnershipService ownershipService, JobExplorer jobExplorer,
			ClaimBoundJobLauncher claimBoundJobLauncher
	) {
		this.vdypBatchJob = vdypBatchJob;
		this.batchProperties = batchProperties;
		this.serverCapacityService = serverCapacityService;
		this.ownershipService = ownershipService;
		this.jobExplorer = jobExplorer;
		this.claimBoundJobLauncher = claimBoundJobLauncher;
	}

	public boolean hasCapacity() {
		return serverCapacityService.hasAvailableCapacity() && ownershipService.isAcceptingNewWork();
	}

	public JobExecution launch(UUID projectionId, String parametersJson) throws IOException, JobExecutionException {
		if (!hasCapacity()) {
			throw new IllegalStateException("No local batch thread capacity is available");
		}
		String projectionGuid = projectionId.toString();
		Optional<JobExecution> existingExecution = findExistingExecution(projectionGuid);
		if (existingExecution.isPresent()) {
			JobExecution execution = existingExecution.get();
			logger.info(
					"Treating duplicate batch launch as idempotent success. projectionGuid={}, executionId={}, status={}",
					projectionGuid, execution.getId(), execution.getStatus()
			);
			return execution;
		}

		JobClaim claim = ownershipService.tryAcquire(projectionGuid, "new-launch").orElseThrow(
				() -> new JobExecutionAlreadyRunningException(
						"Batch job claim is currently owned by another worker: " + projectionGuid
				)
		);

		return claimBoundJobLauncher.launch(vdypBatchJob, buildJobParameters(projectionId, parametersJson), claim);
	}

	public JobExecution launchNewJob(UUID projectionId, String parametersJson)
			throws IOException, JobExecutionException {
		return launch(projectionId, parametersJson);
	}

	private JobParameters buildJobParameters(UUID projectionId, String parametersJson) throws IOException {
		String jobGuid = BatchUtils.createJobGuid();
		String jobTimestamp = BatchUtils.createJobTimestamp();
		Integer numPartitions = batchProperties.getPartition().getDefaultNumberOfPartitions();
		Integer chunkSize = batchProperties.getReader().getDefaultChunkSize();
		Path jobBaseDir = createJobBaseDirectory(jobGuid);

		return new JobParametersBuilder().addString(BatchConstants.Job.GUID, jobGuid)
				.addString(BatchConstants.Projection.PARAMETERS_JSON, parametersJson)
				.addString(BatchConstants.Job.TIMESTAMP, jobTimestamp)
				.addString(BatchConstants.Job.BASE_DIR, jobBaseDir.toString())
				.addString(BatchConstants.GuidInput.PROJECTION_GUID, projectionId.toString())
				.addLong(BatchConstants.Partition.NUMBER, numPartitions.longValue())
				.addLong(BatchConstants.Chunk.SIZE, chunkSize.longValue(), false).toJobParameters();
	}

	private Path createJobBaseDirectory(String jobGuid) throws IOException {
		Path batchRootDir = Paths.get(batchProperties.getRootDirectory());
		String jobBaseFolderName = BatchUtils.createJobFolderName(BatchConstants.Job.BASE_FOLDER_PREFIX, jobGuid);
		Path jobBaseDir = batchRootDir.resolve(jobBaseFolderName);
		Files.createDirectories(jobBaseDir);
		return jobBaseDir;
	}

	private Optional<JobExecution> findExistingExecution(String projectionGuid) {
		try {
			int chunkSize = batchProperties.getPartition().getJobSearchChunkSize();
			long totalInstances = jobExplorer.getJobInstanceCount(JOB_NAME);
			for (long start = 0; start < totalInstances; start += chunkSize) {
				Optional<JobExecution> match = jobExplorer.getJobInstances(JOB_NAME, (int) start, chunkSize).stream()
						.flatMap(jobInstance -> jobExplorer.getJobExecutions(jobInstance).stream())
						.filter(
								execution -> projectionGuid.equals(
										execution.getJobParameters().getString(BatchConstants.GuidInput.PROJECTION_GUID)
								)
						).findFirst();
				if (match.isPresent()) {
					return match;
				}
			}
			return Optional.empty();
		} catch (Exception e) {
			logger.warn(
					"Could not inspect existing executions for idempotency. projectionGuid={}, error={}",
					projectionGuid, e.getMessage()
			);
			return Optional.empty();
		}
	}
}
