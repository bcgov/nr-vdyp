package ca.bc.gov.nrs.vdyp.batch.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.lang.NonNull;

import ca.bc.gov.nrs.vdyp.batch.exception.BatchConfigurationException;
import ca.bc.gov.nrs.vdyp.batch.exception.BatchMetricsException;
import ca.bc.gov.nrs.vdyp.batch.model.BatchChunkMetadata;
import ca.bc.gov.nrs.vdyp.batch.service.BatchMetricsCollector;
import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;

public class BatchItemProcessor implements ItemProcessor<BatchChunkMetadata, BatchChunkMetadata> {

	private static final Logger logger = LoggerFactory.getLogger(BatchItemProcessor.class);

	private final BatchMetricsCollector metricsCollector;

	// Partition context information - initialized once in beforeStep() and never modified thereafter.
	// Note: These fields cannot be made final due to Spring Batch's @BeforeStep contract.
	private Long jobExecutionId;
	private String jobGuid;
	private String partitionName;

	// Protects against multiple beforeStep() calls
	private boolean initialized = false;

	public BatchItemProcessor(BatchMetricsCollector metricsCollector) {
		this.metricsCollector = metricsCollector;
	}

	/**
	 * Initialize processor with step execution context.
	 */
	@BeforeStep
	@SuppressWarnings("java:S2637") // jobGuid, jobExecutionId, partitionName cannot be null in batch context
	public void beforeStep(StepExecution stepExecution) throws BatchConfigurationException, BatchMetricsException {
		if (initialized) {
			throw BatchConfigurationException.handleConfigurationFailure(
					new IllegalStateException("Multiple initialization attempts"),
					"BatchItemProcessor already initialized. beforeStep() should only be called once",
					stepExecution.getJobExecution().getJobParameters().getString(BatchConstants.Job.GUID),
					stepExecution.getJobExecutionId(), logger
			);
		}

		this.jobExecutionId = stepExecution.getJobExecutionId();
		this.jobGuid = stepExecution.getJobExecution().getJobParameters().getString(BatchConstants.Job.GUID);
		this.partitionName = stepExecution.getExecutionContext().getString(BatchConstants.Partition.NAME);

		if (metricsCollector != null) {
			metricsCollector.initializePartitionMetrics(jobExecutionId, jobGuid, partitionName);
		}

		logger.trace(
				"[GUID: {}, Partition: {}] VDYP Projection Processor initialized for job {}", jobGuid, partitionName,
				jobExecutionId
		);

		// Mark as initialized to prevent subsequent calls
		this.initialized = true;
	}

	@Override
	public BatchChunkMetadata process(@NonNull BatchChunkMetadata chunkMetadata) {
		logger.trace(
				"[GUID: {}, EXEID: {}, Partition: {}] Pass-through chunk metadata for processing: startIndex={}, recordCount={}",
				jobGuid, jobExecutionId, partitionName, chunkMetadata.getStartIndex(), chunkMetadata.getRecordCount()
		);

		return chunkMetadata;
	}

}
