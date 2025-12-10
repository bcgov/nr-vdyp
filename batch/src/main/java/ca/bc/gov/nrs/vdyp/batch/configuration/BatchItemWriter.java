package ca.bc.gov.nrs.vdyp.batch.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.lang.NonNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ca.bc.gov.nrs.vdyp.batch.exception.BatchConfigurationException;
import ca.bc.gov.nrs.vdyp.batch.exception.BatchProjectionException;
import ca.bc.gov.nrs.vdyp.batch.exception.BatchResultStorageException;
import ca.bc.gov.nrs.vdyp.batch.model.BatchChunkMetadata;
import ca.bc.gov.nrs.vdyp.batch.service.BatchProjectionService;
import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters;

/**
 * Memory-efficient ItemWriter that processes chunk metadata by streaming data directly from partition files. This
 * writer implements the efficient streaming strategy where file data is read on-demand rather than being held in
 * memory.
 */
public class BatchItemWriter implements ItemWriter<BatchChunkMetadata>, StepExecutionListener {

	private static final Logger logger = LoggerFactory.getLogger(BatchItemWriter.class);

	private final BatchProjectionService batchProjectionService;
	private final ObjectMapper objectMapper;

	// Step execution context fields - initialized once in beforeStep() and never modified thereafter.
	// Note: These fields cannot be made final due to Spring Batch's StepExecutionListener contract,
	// which requires initialization after construction. They represent the step's immutable identity
	// and configuration once set.
	private Long jobExecutionId;
	private String jobGuid;
	private Parameters projectionParameters;

	// Protects against multiple beforeStep() calls
	private boolean initialized = false;

	public BatchItemWriter(BatchProjectionService batchProjectionService, ObjectMapper objectMapper) {
		this.batchProjectionService = batchProjectionService;
		this.objectMapper = objectMapper;
	}

	@Override
	public void beforeStep(StepExecution stepExecution) {
		if (initialized) {
			// StepExecutionListener.beforeStep() interface does not support checked exceptions.
			// Wrap as RuntimeException to maintain interface contract
			BatchConfigurationException configException = BatchConfigurationException.handleConfigurationFailure(
					new IllegalStateException("BatchItemWriter already initialized"),
					"beforeStep() called multiple times - should only be called once", "N/A", 0L, logger
			);
			throw new IllegalStateException(configException.getMessage(), configException);
		}

		this.jobExecutionId = stepExecution.getJobExecutionId();
		this.jobGuid = stepExecution.getJobExecution().getJobParameters().getString(BatchConstants.Job.GUID);

		String partitionName = stepExecution.getExecutionContext().getString(BatchConstants.Partition.NAME);

		logger.info(
				"[GUID: {}, EXEID: {}, Partition: {}] BatchItemWriter.beforeStep() called", jobGuid, jobExecutionId,
				partitionName
		);

		String projectionParametersJson = stepExecution.getJobParameters()
				.getString(BatchConstants.Projection.PARAMETERS_JSON);

		try {
			this.projectionParameters = objectMapper.readValue(projectionParametersJson, Parameters.class);
			logger.info(
					"[GUID: {}, EXEID: {}, Partition: {}] BatchItemWriter initialized with projection parameters. Parameters null: {}",
					jobGuid, jobExecutionId, partitionName, this.projectionParameters == null
			);

			if (this.projectionParameters != null) {
				logger.debug(
						"[GUID: {}, EXEID: {}, Partition: {}] Projection parameters loaded successfully: selectedExecutionOptions={}",
						jobGuid, jobExecutionId, partitionName,
						this.projectionParameters.getSelectedExecutionOptions() != null
								? this.projectionParameters.getSelectedExecutionOptions().size() : "null"
				);
			} else {
				logger.error(
						"[GUID: {}, EXEID: {}, Partition: {}] Deserialized projection parameters are null", jobGuid,
						jobExecutionId, partitionName
				);

				// StepExecutionListener.beforeStep() interface does not support checked exceptions.
				// Wrap as RuntimeException to maintain interface contract
				BatchConfigurationException configException = BatchConfigurationException.handleConfigurationFailure(
						new IllegalStateException("Deserialized projection parameters are null"),
						"Parameter deserialization resulted in null", jobGuid, jobExecutionId, logger
				);
				throw new IllegalStateException(configException.getMessage(), configException);
			}

			// Mark as initialized to prevent subsequent calls
			this.initialized = true;
		} catch (JsonProcessingException jpe) {
			// StepExecutionListener.beforeStep() does not support checked exceptions
			// Wrap BatchConfigurationException as RuntimeException to maintain interface contract
			BatchConfigurationException configException = BatchConfigurationException.handleConfigurationFailure(
					jpe, "JSON parsing failed during parameter deserialization", jobGuid, jobExecutionId, logger
			);
			throw new IllegalStateException(configException.getMessage(), configException);
		}
	}

	@Override
	public ExitStatus afterStep(StepExecution stepExecution) {
		String partitionName = stepExecution.getExecutionContext().getString(BatchConstants.Partition.NAME);
		logger.info(
				"[GUID: {}, EXEID: {}, Partition: {}] BatchItemWriter.afterStep() called", this.jobGuid,
				this.jobExecutionId, partitionName
		);
		return stepExecution.getExitStatus();
	}

	@Override
	public void write(@NonNull Chunk<? extends BatchChunkMetadata> chunk)
			throws BatchResultStorageException, BatchProjectionException, BatchConfigurationException {
		if (chunk.isEmpty()) {
			logger.debug("[GUID: {}, EXEID: {}] Empty chunk received, skipping", this.jobGuid, this.jobExecutionId);
			return;
		}

		// Validate that projection parameters were initialized in beforeStep()
		if (this.projectionParameters == null) {
			throw BatchConfigurationException.handleConfigurationFailure(
					new IllegalStateException("VDYP projection parameters are null"),
					"Projection parameters not initialized - beforeStep() was not called or failed", this.jobGuid,
					this.jobExecutionId, logger
			);
		}

		// Spring Batch chunk size is 1, so chunk contains exactly one BatchChunkMetadata
		BatchChunkMetadata chunkMetadata = chunk.getItems().get(0);
		String partitionName = chunkMetadata.getPartitionName();

		logger.info(
				"[GUID: {}, EXEID: {}, Partition: {}] Processing chunk metadata (startIndex={}, recordCount={}) using BatchProjectionService",
				this.jobGuid, this.jobExecutionId, partitionName, chunkMetadata.getStartIndex(),
				chunkMetadata.getRecordCount()
		);

		// Perform chunk-based projection with streaming
		String chunkResult = batchProjectionService
				.performProjectionForChunk(chunkMetadata, this.projectionParameters, this.jobExecutionId, this.jobGuid);

		logger.info(
				"[GUID: {}, EXEID: {}, Partition: {}] Successfully processed chunk of {} records. Result: {}",
				this.jobGuid, this.jobExecutionId, partitionName, chunkMetadata.getRecordCount(), chunkResult
		);
	}
}
