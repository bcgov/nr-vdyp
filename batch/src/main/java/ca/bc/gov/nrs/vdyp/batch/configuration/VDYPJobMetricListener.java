package ca.bc.gov.nrs.vdyp.batch.configuration;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.lang.NonNull;

import ca.bc.gov.nrs.vdyp.batch.exception.BatchMetricsException;
import ca.bc.gov.nrs.vdyp.batch.service.BatchMetricsCollector;
import ca.bc.gov.nrs.vdyp.batch.service.BatchResultAggregationService;
import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;

public class VDYPJobMetricListener implements JobExecutionListener {
	private static final Logger logger = LoggerFactory.getLogger(VDYPJobMetricListener.class);
	private final BatchMetricsCollector metricsCollector;
	private final BatchProperties batchProperties;
	private final BatchResultAggregationService resultAggregationService;

	public VDYPJobMetricListener(
			BatchMetricsCollector metricsCollector, BatchProperties batchProperties,
			BatchResultAggregationService resultAggregationService
	) {
		this.metricsCollector = metricsCollector;
		this.batchProperties = batchProperties;
		this.resultAggregationService = resultAggregationService;
	}

	@Override
	@SuppressWarnings("java:S2637") // jobGuid, jobExecutionId, partitionName cannot be null in batch
	// context
	public void beforeJob(@NonNull JobExecution jobExecution) {
		// Initialize job metrics
		String jobGuid = jobExecution.getJobParameters().getString(BatchConstants.Job.GUID);
		try {
			metricsCollector.initializeMetrics(jobExecution.getId(), jobGuid);
		} catch (BatchMetricsException e) {
			logger.error("Failed to initialize job metrics: {}", e.getMessage());
		}
		logger.info("[GUID: {}] === VDYP Batch Job Starting === Execution ID: {}", jobGuid, jobExecution.getId());
	}

	@Override
	@SuppressWarnings("java:S2637") // jobGuid, jobExecutionId, partitionName cannot be null in batch
	// context
	public void afterJob(@NonNull JobExecution jobExecution) {
		String jobGuid = jobExecution.getJobParameters().getString(BatchConstants.Job.GUID);

		// Finalize job metrics - only count worker steps (partitioned steps)
		long totalRead = jobExecution.getStepExecutions().stream()
				.filter(stepExecution -> stepExecution.getStepName().startsWith("workerStep:"))
				.mapToLong(StepExecution::getReadCount).sum();
		long totalWritten = jobExecution.getStepExecutions().stream()
				.filter(stepExecution -> stepExecution.getStepName().startsWith("workerStep:"))
				.mapToLong(StepExecution::getWriteCount).sum();

		logger.debug(
				"[GUID: {}] [VDYP Metrics Debug] Job execution ID: {} - All steps: [{}]", jobGuid, jobExecution.getId(),
				jobExecution.getStepExecutions().stream().map(StepExecution::getStepName)
						.collect(Collectors.joining(", "))
		);

		try {
			metricsCollector.finalizeJobMetrics(
					jobExecution.getId(), jobGuid, jobExecution.getStatus().toString(), totalRead, totalWritten
			);
		} catch (BatchMetricsException e) {
			logger.error("Failed to finalize job metrics: {}", e.getMessage());
		}

		if (jobExecution.getStatus() == BatchStatus.STOPPED
				&& batchProperties.getPartition().getInterimDirsCleanupEnabled()) {
			try {
				String jobBaseDir = jobExecution.getJobParameters().getString(BatchConstants.Job.BASE_DIR);
				if (jobBaseDir != null) {
					Path jobBasePath = Paths.get(jobBaseDir);
					resultAggregationService.cleanupPartitionDirectories(jobBasePath);
					logger.info(
							"[GUID: {}] Job execution ID: {} was stopped. Interim partition directories cleanup completed",
							jobGuid, jobExecution.getId()
					);
				}
			} catch (Exception e) {
				logger.error(
						"[GUID: {}] Failed to cleanup interim directories for stopped job execution ID: {}: {}",
						jobGuid, jobExecution.getId(), e.getMessage()
				);
			}
		}

		try {
			metricsCollector.cleanupOldMetrics(20);
		} catch (BatchMetricsException e) {
			logger.error("Failed to cleanup old metrics: {}", e.getMessage());
		}

		logger.info("[GUID: {}] === VDYP Batch Job Completed === Execution ID: {}", jobGuid, jobExecution.getId());
	}
}
