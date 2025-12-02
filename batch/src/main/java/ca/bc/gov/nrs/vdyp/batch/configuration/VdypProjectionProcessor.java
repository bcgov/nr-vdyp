package ca.bc.gov.nrs.vdyp.batch.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.lang.NonNull;

import ca.bc.gov.nrs.vdyp.batch.exception.BatchException;
import ca.bc.gov.nrs.vdyp.batch.model.BatchRecord;
import ca.bc.gov.nrs.vdyp.batch.service.BatchMetricsCollector;
import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;

public class VdypProjectionProcessor implements ItemProcessor<BatchRecord, BatchRecord> {

	private static final Logger logger = LoggerFactory.getLogger(VdypProjectionProcessor.class);

	private final BatchMetricsCollector metricsCollector;

	// Partition context information - initialized once in beforeStep() and never modified thereafter.
	// Note: These fields cannot be made final due to Spring Batch's @BeforeStep contract.
	private Long jobExecutionId;
	private String jobGuid;
	private String partitionName;

	// Protects against multiple beforeStep() calls
	private boolean initialized = false;

	public VdypProjectionProcessor(BatchMetricsCollector metricsCollector) {
		this.metricsCollector = metricsCollector;
	}

	/**
	 * Initialize processor with step execution context.
	 */
	@BeforeStep
	@SuppressWarnings("java:S2637") // jobGuid, jobExecutionId, partitionName cannot be null in batch context
	public void beforeStep(StepExecution stepExecution) throws BatchException {
		if (initialized) {
			throw new IllegalStateException(
					"VdypProjectionProcessor already initialized. beforeStep() should only be called once."
			);
		}

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

		// Mark as initialized to prevent subsequent calls
		this.initialized = true;
	}

	@Override
	public BatchRecord process(@NonNull BatchRecord batchRecord) {
		logger.trace(
				"[GUID: {}, EXEID: {} Partition: {}] Prepared record for chunk processing: FEATURE_ID {}", jobGuid,
				jobExecutionId, partitionName, batchRecord.getFeatureId()
		);

		return batchRecord;
	}

}
