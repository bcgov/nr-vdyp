package ca.bc.gov.nrs.vdyp.batch.configuration;

import ca.bc.gov.nrs.vdyp.batch.model.BatchRecord;
import ca.bc.gov.nrs.vdyp.batch.service.BatchMetricsCollector;
import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;

public class VdypProjectionProcessor implements ItemProcessor<BatchRecord, BatchRecord> {

	private static final Logger logger = LoggerFactory.getLogger(VdypProjectionProcessor.class);

	private final BatchMetricsCollector metricsCollector;

	// Partition context information
	private Long jobExecutionId;
	private String jobGuid;
	private String partitionName;

	// Validation thresholds
	@Value("${batch.validation.max-data-length:50000}")
	private int maxDataLength;

	@Value("${batch.validation.min-polygon-id-length:1}")
	private int minPolygonIdLength;

	@Value("${batch.validation.max-polygon-id-length:50}")
	private int maxPolygonIdLength;

	public VdypProjectionProcessor(BatchMetricsCollector metricsCollector) {
		this.metricsCollector = metricsCollector;
	}

	/**
	 * Initialize processor with step execution context.
	 */
	@BeforeStep
	@SuppressWarnings("java:S2637") // jobGuid, jobExecutionId, partitionName cannot be null in batch context
	public void beforeStep(StepExecution stepExecution) {
		this.jobExecutionId = stepExecution.getJobExecutionId();
		this.jobGuid = stepExecution.getJobExecution().getJobParameters().getString(BatchConstants.Job.GUID);
		this.partitionName = stepExecution.getExecutionContext().getString(BatchConstants.Partition.NAME);

		if (metricsCollector != null) {
			metricsCollector.initializePartitionMetrics(jobExecutionId, jobGuid, partitionName);
		}

		logger.info(
				"[GUID: {}, Partition: {}] VDYP Projection Processor initialized for job {}", jobGuid, partitionName,
				jobExecutionId
		);
	}

	@Override
	public BatchRecord process(@NonNull BatchRecord batchRecord) throws IllegalArgumentException {
		String featureId = batchRecord.getFeatureId();

		// Basic validation only - projection happens in ItemWriter for chunk processing
		if (featureId == null || featureId.trim().isEmpty()) {
			throw new IllegalArgumentException("FEATURE_ID is null or empty");
		}

		logger.trace(
				"[GUID: {}, EXEID: {} Partition: {}] Prepared record for chunk processing: FEATURE_ID {}", jobGuid,
				jobExecutionId, partitionName, featureId
		);

		return batchRecord;
	}

}
