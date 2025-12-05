package ca.bc.gov.nrs.vdyp.batch.configuration;

import java.util.List;
import java.util.stream.Collectors;

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
import ca.bc.gov.nrs.vdyp.batch.model.BatchRecord;
import ca.bc.gov.nrs.vdyp.batch.service.VdypProjectionService;
import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters;

/**
 * Chunk-based ItemWriter that processes multiple BatchRecords together for improved performance. This writer implements
 * the efficient chunk-based projection strategy where multiple FEATURE_IDs are processed in a single VDYP projection
 * operation.
 */
public class VdypChunkProjectionWriter implements ItemWriter<BatchRecord>, StepExecutionListener {

	private static final Logger logger = LoggerFactory.getLogger(VdypChunkProjectionWriter.class);

	private final VdypProjectionService vdypProjectionService;
	private final ObjectMapper objectMapper;

	// Step execution context fields - initialized once in beforeStep() and never modified thereafter.
	// Note: These fields cannot be made final due to Spring Batch's StepExecutionListener contract,
	// which requires initialization after construction. They represent the step's immutable identity
	// and configuration once set.
	private String partitionName;
	private Long jobExecutionId;
	private String jobGuid;
	private Parameters projectionParameters;
	private String jobBaseDir;

	// Protects against multiple beforeStep() calls
	private boolean initialized = false;

	public VdypChunkProjectionWriter(VdypProjectionService vdypProjectionService, ObjectMapper objectMapper) {
		this.vdypProjectionService = vdypProjectionService;
		this.objectMapper = objectMapper;
	}

	@Override
	public void beforeStep(StepExecution stepExecution) {
		if (initialized) {
			// StepExecutionListener.beforeStep() interface does not support checked exceptions.
			// Wrap as RuntimeException to maintain interface contract
			BatchConfigurationException configException = BatchConfigurationException.handleConfigurationFailure(
					new IllegalStateException("VdypChunkProjectionWriter already initialized"),
					"beforeStep() called multiple times - should only be called once", "N/A", 0L, logger
			);
			throw new IllegalStateException(configException.getMessage(), configException);
		}

		this.jobExecutionId = stepExecution.getJobExecutionId();
		this.jobGuid = stepExecution.getJobExecution().getJobParameters().getString(BatchConstants.Job.GUID);
		this.partitionName = stepExecution.getExecutionContext().getString(BatchConstants.Partition.NAME);
		this.jobBaseDir = stepExecution.getJobParameters().getString(BatchConstants.Job.BASE_DIR);

		logger.info(
				"[GUID: {}, EXEID: {}, Partition: {}] VdypChunkProjectionWriter.beforeStep() called", jobGuid,
				jobExecutionId, partitionName
		);

		String projectionParametersJson = stepExecution.getJobParameters()
				.getString(BatchConstants.Projection.PARAMETERS_JSON);

		try {
			this.projectionParameters = objectMapper.readValue(projectionParametersJson, Parameters.class);
			logger.info(
					"[GUID: {}, EXEID: {}, Partition: {}] VdypChunkProjectionWriter initialized with projection parameters. Parameters null: {}",
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
		logger.info(
				"[GUID: {}, EXEID: {}, Partition: {}] VdypChunkProjectionWriter.afterStep() called", this.jobGuid,
				this.jobExecutionId, this.partitionName
		);
		return stepExecution.getExitStatus();
	}

	@Override
	public void write(@NonNull Chunk<? extends BatchRecord> chunk)
			throws BatchResultStorageException, BatchProjectionException, BatchConfigurationException {
		if (chunk.isEmpty()) {
			logger.debug(
					"[GUID: {}, EXEID: {}, Partition: {}] Empty chunk received, skipping", this.jobGuid,
					this.jobExecutionId, this.partitionName
			);
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

		List<BatchRecord> batchRecords = chunk.getItems().stream().collect(Collectors.toList());

		logger.info(
				"[GUID: {}, EXEID: {}, Partition: {}] Processing chunk of {} records using VdypProjectionService",
				this.jobGuid, this.jobExecutionId, this.partitionName, batchRecords.size()
		);

		// Perform chunk-based projection
		String chunkResult = vdypProjectionService.performProjectionForChunk(
				batchRecords, this.partitionName, this.projectionParameters, this.jobExecutionId, this.jobGuid,
				this.jobBaseDir
		);

		logger.info(
				"[GUID: {}, EXEID: {}, Partition: {}] Successfully processed chunk of {} records. Result: {}",
				this.jobGuid, this.jobExecutionId, this.partitionName, batchRecords.size(), chunkResult
		);
	}
}
