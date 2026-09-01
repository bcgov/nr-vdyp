package ca.bc.gov.nrs.vdyp.batch.service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobExecutionNotRunningException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import ca.bc.gov.nrs.vdyp.batch.configuration.BatchProperties;
import ca.bc.gov.nrs.vdyp.batch.ownership.JobOwnershipService;
import ca.bc.gov.nrs.vdyp.batch.persistence.model.JobClaim;
import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;

/**
 * Performs the prioritize action for a job execution that this instance owns: pauses every other locally-owned running
 * job and resumes them afterward. Callable directly by the REST endpoint when the target is owned locally, or by
 * PrioritizeRequestListener when this instance is found to be the owner via the NATS broadcast.
 */
@Service
public class BatchPrioritizationService {

	private static final Logger logger = LoggerFactory.getLogger(BatchPrioritizationService.class);

	private final JobExplorer jobExplorer;
	private final JobOperator jobOperator;
	private final BatchProperties batchProperties;
	private final JobOwnershipService ownershipService;
	private final ClaimBoundJobLauncher claimBoundJobLauncher;
	private final Job fetchAndPartitionJob;
	private final TaskExecutor prioritizationExecutor;
	private final TaskExecutor resumeRetryExecutor;
	private final PrioritizationPauseTracker pauseTracker;

	public BatchPrioritizationService(
			JobExplorer jobExplorer, JobOperator jobOperator, BatchProperties batchProperties,
			JobOwnershipService ownershipService, ClaimBoundJobLauncher claimBoundJobLauncher,
			@Qualifier("fetchAndPartitionJob") Job fetchAndPartitionJob,
			@Qualifier("prioritizationExecutor") TaskExecutor prioritizationExecutor,
			@Qualifier("resumeRetryExecutor") TaskExecutor resumeRetryExecutor, PrioritizationPauseTracker pauseTracker
	) {
		this.jobExplorer = jobExplorer;
		this.jobOperator = jobOperator;
		this.batchProperties = batchProperties;
		this.ownershipService = ownershipService;
		this.claimBoundJobLauncher = claimBoundJobLauncher;
		this.fetchAndPartitionJob = fetchAndPartitionJob;
		this.prioritizationExecutor = prioritizationExecutor;
		this.resumeRetryExecutor = resumeRetryExecutor;
		this.pauseTracker = pauseTracker;
	}

	/**
	 * Pauses every other locally-owned running job (freeing this instance's shared thread-pool capacity for the target
	 * job's already-queued partitions) and resumes them afterward, in the order they were originally started. Assumes
	 * the caller has already verified the target is running and owned by this instance.
	 */
	public PrioritizeOutcome prioritizeLocally(String jobGuid, JobExecution targetExecution) {
		List<JobExecution> others = findOtherRunningExecutions(targetExecution);

		if (others.isEmpty()) {
			logger.info("[GUID: {}] No other running jobs found; nothing to pause.", jobGuid);
			return new PrioritizeOutcome(
					"ALREADY_PRIORITIZED",
					"No other running jobs to pause; this job already has full thread capacity available.", 0,
					targetExecution.getId()
			);
		}

		logger.info("[GUID: {}] Prioritizing job. Pausing {} other running job(s).", jobGuid, others.size());
		prioritizationExecutor.execute(() -> prioritizeAsync(jobGuid, others));

		return new PrioritizeOutcome(
				"PRIORITIZE_REQUESTED",
				"Prioritization requested. " + others.size()
						+ " other running job(s) will be paused and resumed in their original start order.",
				others.size(), targetExecution.getId()
		);
	}

	/**
	 * Finds all other currently running executions of the fetch-and-partition job (BatchConstants.Job.JOB_NAME) that
	 * are owned by this instance, excluding the given target, ordered by start time ascending - the order they should
	 * later be resumed in. A job owned by another replica is left untouched: it does not compete with the target job
	 * for this replica's thread-pool capacity, and this replica cannot reliably stop it anyway.
	 */
	private List<JobExecution> findOtherRunningExecutions(JobExecution target) {
		Set<JobExecution> runningExecutions = jobExplorer.findRunningJobExecutions(BatchConstants.Job.JOB_NAME);

		return runningExecutions.stream().filter(execution -> !execution.getId().equals(target.getId()))
				.filter(execution -> ownershipService.isOwnedLocally(projectionGuid(execution)))
				.sorted(
						Comparator
								.comparing(JobExecution::getStartTime, Comparator.nullsLast(Comparator.naturalOrder()))
				).toList();
	}

	// Stops each job (in start-time order) then hands off its resume to resumeRetryExecutor, so one
	// slow-to-stop job can't delay stopping the rest or block the next prioritize request.
	private void prioritizeAsync(String prioritizedJobGuid, List<JobExecution> others) {
		for (JobExecution execution : others) {
			Long executionId = execution.getId();
			// Mark before stop() so VDYPJobMetricListener's afterJob skips deleting this job's interim
			// partition directories, which it still needs to resume.
			pauseTracker.markPausedForResume(executionId);
			try {
				jobOperator.stop(executionId);
			} catch (JobExecutionNotRunningException e) {
				logger.debug(
						"[GUID: {}] Job execution {} was already stopping/stopped.", prioritizedJobGuid, executionId
				);
			} catch (Exception e) {
				logger.warn(
						"[GUID: {}] Failed to stop job execution {} while prioritizing; skipping it. {}",
						prioritizedJobGuid, executionId, e.getMessage(), e
				);
				pauseTracker.unmark(executionId);
				continue;
			}

			resumeRetryExecutor.execute(() -> resumeWithRetry(prioritizedJobGuid, execution));
		}
	}

	// Retries relaunching a paused job until it succeeds or the timeout elapses - Spring Batch's graceful
	// stop can still report STOPPING (and reject a relaunch) well after the job was signalled to stop.
	// Stopping a job releases its ownership claim (see VDYPJobMetricListener.afterJob), so resuming must
	// reacquire one and relaunch through claimBoundJobLauncher - the same pattern as StartupRecoveryService -
	// rather than launching directly, or the resumed execution would be fenced out by ownership checks.
	private void resumeWithRetry(String prioritizedJobGuid, JobExecution execution) {
		Long executionId = execution.getId();
		String projectionGuid = projectionGuid(execution);
		int timeoutSeconds = batchProperties.getPrioritize().getStopWaitTimeoutSeconds();
		int pollIntervalMillis = batchProperties.getPrioritize().getStopPollIntervalMillis();
		long deadline = System.currentTimeMillis() + (timeoutSeconds * 1000L);

		try {
			while (true) {
				Optional<JobClaim> claim = ownershipService.tryAcquire(projectionGuid, "prioritize-resume");
				if (claim.isPresent()) {
					try {
						JobExecution resumed = claimBoundJobLauncher
								.launch(fetchAndPartitionJob, execution.getJobParameters(), claim.get());
						logger.info(
								"[GUID: {}] Resumed paused job execution {} as new execution {}.", prioritizedJobGuid,
								executionId, resumed.getId()
						);
						return;
					} catch (JobExecutionAlreadyRunningException e) {
						// Still stopping (Spring Batch only halts at the next chunk boundary) - fall through to retry.
					} catch (JobExecutionException e) {
						logger.error(
								"[GUID: {}] Failed to resume paused job execution {}. {}", prioritizedJobGuid,
								executionId, e.getMessage(), e
						);
						return;
					}
				}

				if (System.currentTimeMillis() >= deadline) {
					logger.error(
							"[GUID: {}] Gave up resuming job execution {} after {}s; it is still reported as "
									+ "running (likely still STOPPING). It will remain stopped until manually restarted.",
							prioritizedJobGuid, executionId, timeoutSeconds
					);
					return;
				}
				try {
					Thread.sleep(pollIntervalMillis);
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					return;
				}
			}
		} finally {
			pauseTracker.unmark(executionId);
		}
	}

	private String projectionGuid(JobExecution jobExecution) {
		return jobExecution.getJobParameters().getString(BatchConstants.GuidInput.PROJECTION_GUID);
	}

	public record PrioritizeOutcome(String status, String message, int othersPausedCount, Long targetExecutionId) {
	}
}
