package ca.bc.gov.nrs.vdyp.batch.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.launch.JobExecutionNotRunningException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.stereotype.Service;

/**
 * Stops a locally owned job execution. Used by BatchController (stop received by the owning replica) and
 * StopRequestListener (stop received by another replica, routed here over NATS).
 */
@Service
public class BatchStopService {

	private static final Logger logger = LoggerFactory.getLogger(BatchStopService.class);

	private final JobOperator jobOperator;

	public BatchStopService(JobOperator jobOperator) {
		this.jobOperator = jobOperator;
	}

	public record StopOutcome(String status, String message, Long executionId) {
	}

	public StopOutcome stopLocally(String jobGuid, JobExecution jobExecution) throws NoSuchJobExecutionException {
		Long executionId = jobExecution.getId();
		try {
			boolean stopped = jobOperator.stop(executionId);
			if (stopped) {
				logger.info("[GUID: {}] Stop request sent successfully for JobExecution ID: {}", jobGuid, executionId);
				return new StopOutcome(
						"STOP_REQUESTED",
						"Stop request sent successfully. Job will stop after completing current chunk.", executionId
				);
			}

			logger.warn("[GUID: {}] Failed to stop JobExecution ID: {}. Job may not be running.", jobGuid, executionId);
			return new StopOutcome(
					"STOP_FAILED", "Job execution could not be stopped. It may not be running.", executionId
			);
		} catch (JobExecutionNotRunningException e) {
			logger.debug("[GUID: {}] Job is already stopping or stopped", jobGuid);
			return new StopOutcome(
					"ALREADY_STOPPING",
					"Job is already in the process of stopping or has already been stopped. "
							+ "Please check job status for current state.",
					executionId
			);
		}
	}
}
