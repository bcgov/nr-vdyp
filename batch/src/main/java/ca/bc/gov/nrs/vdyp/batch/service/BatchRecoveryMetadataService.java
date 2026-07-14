package ca.bc.gov.nrs.vdyp.batch.service;

import java.time.LocalDateTime;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BatchRecoveryMetadataService {

	private final JobExplorer jobExplorer;
	private final JobRepository jobRepository;

	public BatchRecoveryMetadataService(JobExplorer jobExplorer, JobRepository jobRepository) {
		this.jobExplorer = jobExplorer;
		this.jobRepository = jobRepository;
	}

	@Transactional
	public void markStaleExecutionFailed(Long jobExecutionId) {
		markStaleExecutionFailedInternal(jobExecutionId, "Marked FAILED during startup recovery after abrupt shutdown");
	}

	@Transactional
	public JobExecution markStaleExecutionFailed(Long jobExecutionId, String exitDescription) {
		return markStaleExecutionFailedInternal(jobExecutionId, exitDescription);
	}

	private JobExecution markStaleExecutionFailedInternal(Long jobExecutionId, String exitDescription) {
		JobExecution jobExecution = jobExplorer.getJobExecution(jobExecutionId);

		if (jobExecution == null) {
			throw new IllegalStateException("Could not find JobExecution " + jobExecutionId);
		}

		if (!isRunningOrStopping(jobExecution.getStatus())) {
			return jobExecution;
		}

		LocalDateTime now = LocalDateTime.now();

		for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
			if (isRunningOrStopping(stepExecution.getStatus())) {
				stepExecution.setStatus(BatchStatus.FAILED);
				stepExecution.setExitStatus(ExitStatus.FAILED.addExitDescription(exitDescription));
				stepExecution.setEndTime(now);
				jobRepository.update(stepExecution);
			}
		}

		jobExecution.setStatus(BatchStatus.FAILED);
		jobExecution.setExitStatus(ExitStatus.FAILED.addExitDescription(exitDescription));
		jobExecution.setEndTime(now);

		jobRepository.update(jobExecution);
		return jobExecution;
	}

	private boolean isRunningOrStopping(BatchStatus status) {
		return status == BatchStatus.STARTING || status == BatchStatus.STARTED || status == BatchStatus.STOPPING;
	}
}
