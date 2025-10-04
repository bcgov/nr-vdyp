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

import java.io.IOException;

public class VdypProjectionProcessor implements ItemProcessor<BatchRecord, BatchRecord> {

	private static final Logger logger = LoggerFactory.getLogger(VdypProjectionProcessor.class);

	private final BatchRetryPolicy retryPolicy;
	private final BatchMetricsCollector metricsCollector;

	// Partition context information
	private String partitionName = BatchConstants.Common.UNKNOWN;

	// Validation thresholds
	@Value("${batch.validation.max-data-length:50000}")
	private int maxDataLength;

	@Value("${batch.validation.min-polygon-id-length:1}")
	private int minPolygonIdLength;

	@Value("${batch.validation.max-polygon-id-length:50}")
	private int maxPolygonIdLength;

	public VdypProjectionProcessor(
			BatchRetryPolicy retryPolicy, BatchMetricsCollector metricsCollector) {
		this.retryPolicy = retryPolicy;
		this.metricsCollector = metricsCollector;
	}

	/**
	 * Initialize processor with step execution context.
	 */
	@BeforeStep
	public void beforeStep(StepExecution stepExecution) {
		Long jobExecutionId = stepExecution.getJobExecutionId();
		this.partitionName = stepExecution.getExecutionContext().getString(BatchConstants.Partition.NAME,
				BatchConstants.Common.UNKNOWN);

		if (metricsCollector != null) {
			metricsCollector.initializePartitionMetrics(jobExecutionId, partitionName);
		}

		logger.info(
				"[{}] VDYP Projection Processor initialized for job {}",
				partitionName, jobExecutionId);
	}

	@Override
	public BatchRecord process(@NonNull BatchRecord batchRecord) throws IOException, IllegalArgumentException {
		String featureId = batchRecord.getFeatureId();

		if (retryPolicy != null && featureId != null) {
			retryPolicy.registerRecord((long) featureId.hashCode(), batchRecord);
		}

		// Basic validation only - projection happens in ItemWriter for chunk processing
		if (featureId == null || featureId.trim().isEmpty()) {
			throw new IllegalArgumentException("FEATURE_ID is null or empty");
		}

		logger.debug("[{}] Prepared record for chunk processing: FEATURE_ID {}", partitionName, featureId);

		return batchRecord;
	}

}
