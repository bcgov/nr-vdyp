package ca.bc.gov.nrs.vdyp.batch.service;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import ca.bc.gov.nrs.vdyp.batch.ownership.JobOwnershipService;
import ca.bc.gov.nrs.vdyp.batch.persistence.model.JobClaim;
import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;

/**
 * Creates a Spring Batch execution, registers its already-acquired claim locally, then submits it for execution.
 */
@Service
public class ClaimBoundJobLauncher {

	private final JobRepository jobRepository;
	private final TaskExecutor taskExecutor;
	private final JobOwnershipService ownershipService;
	private final ThreadReservationService threadReservationService;

	public ClaimBoundJobLauncher(
			JobRepository jobRepository, @Qualifier("taskExecutor") TaskExecutor taskExecutor,
			JobOwnershipService ownershipService, ThreadReservationService threadReservationService
	) {
		this.jobRepository = jobRepository;
		this.taskExecutor = taskExecutor;
		this.ownershipService = ownershipService;
		this.threadReservationService = threadReservationService;
	}

	/**
	 * @param reservedThreads threads the caller already reserved for this launch (0 if none). Recorded on the execution
	 *                        for later release, or released immediately if the launch fails.
	 */
	public JobExecution launch(Job job, JobParameters jobParameters, JobClaim claim, int reservedThreads)
			throws JobExecutionException {
		JobExecution jobExecution;
		try {
			job.getJobParametersValidator().validate(jobParameters);
			jobExecution = jobRepository.createJobExecution(job.getName(), jobParameters);
		} catch (JobExecutionException e) {
			ownershipService.releaseUnboundClaim(claim);
			releaseReservedThreads(reservedThreads);
			throw e;
		}
		ownershipService.registerClaim(claim);
		// Lets finalizeOwnedExecution later tell this claim apart from one a pause/resume has since replaced it with.
		jobExecution.getExecutionContext()
				.putString(JobOwnershipService.LEASE_TOKEN_CONTEXT_KEY, claim.leaseToken().toString());
		if (reservedThreads > 0) {
			jobExecution.getExecutionContext().putInt(BatchConstants.Job.RESERVED_THREADS, reservedThreads);
		}

		try {
			taskExecutor.execute(() -> job.execute(jobExecution));
		} catch (RuntimeException e) {
			failUnstartedExecution(jobExecution, "Could not submit the batch execution: " + e.getMessage());
			ownershipService.finalizeOwnedClaim(claim.projectionGuid(), claim.leaseToken());
			releaseReservedThreads(reservedThreads);
			throw e;
		}

		return jobExecution;
	}

	private void releaseReservedThreads(int reservedThreads) {
		if (reservedThreads > 0) {
			threadReservationService.release(reservedThreads);
		}
	}

	private void failUnstartedExecution(JobExecution jobExecution, String exitDescription) {
		jobExecution.upgradeStatus(BatchStatus.FAILED);
		jobExecution.setExitStatus(ExitStatus.FAILED.addExitDescription(exitDescription));
		jobExecution.setEndTime(LocalDateTime.now(ZoneId.systemDefault()));
		jobRepository.update(jobExecution);
	}
}
