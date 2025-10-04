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

import ca.bc.gov.nrs.vdyp.batch.model.BatchRecord;
import ca.bc.gov.nrs.vdyp.batch.service.BatchMetricsCollector;
import ca.bc.gov.nrs.vdyp.batch.service.VdypProjectionService;
import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters;

/**
 * Chunk-based ItemWriter that processes multiple BatchRecords together for
 * improved performance. This writer implements the efficient chunk-based
 * projection strategy where multiple FEATURE_IDs are processed in a single VDYP
 * projection operation.
 */
public class VdypChunkProjectionWriter implements ItemWriter<BatchRecord>, StepExecutionListener {

	private static final Logger logger = LoggerFactory.getLogger(VdypChunkProjectionWriter.class);

	private final VdypProjectionService vdypProjectionService;
	private final BatchMetricsCollector metricsCollector;
	private final ObjectMapper objectMapper = new ObjectMapper();

	// Step execution context
	private String partitionName = BatchConstants.Common.UNKNOWN;
	private Long jobExecutionId;
	private Parameters projectionParameters;
	private String jobBaseDir;

	public VdypChunkProjectionWriter(VdypProjectionService vdypProjectionService,
			BatchMetricsCollector metricsCollector) {
		this.vdypProjectionService = vdypProjectionService;
		this.metricsCollector = metricsCollector;
	}

	@Override
	public void beforeStep(StepExecution stepExecution) {
		this.jobExecutionId = stepExecution.getJobExecutionId();
		this.partitionName = stepExecution.getExecutionContext().getString(BatchConstants.Partition.NAME,
				BatchConstants.Common.UNKNOWN);
		this.jobBaseDir = stepExecution.getJobParameters().getString(BatchConstants.Job.BASE_DIR);

		logger.info("[{}] VdypChunkProjectionWriter.beforeStep() called", partitionName);

		String projectionParametersJson = stepExecution.getJobParameters()
				.getString(BatchConstants.Projection.PARAMETERS_JSON);

		try {
			this.projectionParameters = objectMapper.readValue(projectionParametersJson, Parameters.class);
			logger.info("[{}] VdypChunkProjectionWriter initialized with projection parameters. Parameters null: {}",
					partitionName, this.projectionParameters == null);

			if (this.projectionParameters != null) {
				logger.debug("[{}] Projection parameters loaded successfully: selectedExecutionOptions={}",
						partitionName,
						this.projectionParameters.getSelectedExecutionOptions() != null
								? this.projectionParameters.getSelectedExecutionOptions().size()
								: "null");
			} else {
				logger.error("[{}] Projection parameters deserialized to null from JSON: {}",
						partitionName, projectionParametersJson);
				throw new IllegalStateException("Deserialized projection parameters are null");
			}
		} catch (JsonProcessingException jsonException) {
			throw handleParameterDeserializationFailure(
					projectionParametersJson, jsonException, "JSON parsing failed during parameter deserialization");
		} catch (Exception generalException) {
			throw handleParameterDeserializationFailure(
					projectionParametersJson, generalException, "Unexpected error during parameter deserialization");
		}
	}

	@Override
	public ExitStatus afterStep(StepExecution stepExecution) {
		logger.info("[{}] VdypChunkProjectionWriter.afterStep() called", this.partitionName);
		return stepExecution.getExitStatus();
	}

	@Override
	public void write(@NonNull Chunk<? extends BatchRecord> chunk) throws Exception {
		if (chunk.isEmpty()) {
			logger.debug("[{}] Empty chunk received, skipping", this.partitionName);
			return;
		}

		List<BatchRecord> batchRecords = chunk.getItems().stream()
				.collect(Collectors.toList());

		logger.info("[{}] Processing chunk of {} records using VdypProjectionService",
				this.partitionName, batchRecords.size());

		try {
			// Validate projection parameters before processing
			if (projectionParameters == null) {
				throw new IllegalStateException(
						"VDYP projection parameters are null. Cannot perform chunk projection.");
			}

			// Perform chunk-based projection
			String chunkResult = vdypProjectionService.performProjectionForChunk(
					batchRecords, this.partitionName, this.projectionParameters, this.jobExecutionId, this.jobBaseDir);

			// Record metrics for successful chunk processing
			recordChunkMetrics(batchRecords, this.partitionName, true, null);

			logger.info("[{}] Successfully processed chunk of {} records. Result: {}",
					this.partitionName, batchRecords.size(), chunkResult);

		} catch (RuntimeException runtimeException) {
			throw handleChunkProcessingFailure(
					batchRecords, this.partitionName, runtimeException, "Runtime error during chunk processing");
		} catch (Exception generalException) {
			throw handleChunkProcessingFailure(
					batchRecords, this.partitionName, generalException, "Unexpected error during chunk processing");
		}
	}

	/**
	 * Records metrics for chunk processing results.
	 */
	private void recordChunkMetrics(List<BatchRecord> batchRecords, String actualPartitionName, boolean success,
			Exception error) {
		if (metricsCollector != null && jobExecutionId != null) {
			for (BatchRecord batchRecord : batchRecords) {
				try {
					Long recordIdHash = batchRecord.getFeatureId() != null
							? (long) batchRecord.getFeatureId().hashCode()
							: 0L;

					if (success) {
						// Record successful processing
						logger.trace("[{}] Recording successful processing for FEATURE_ID: {}",
								actualPartitionName, batchRecord.getFeatureId());
					} else {
						// Record processing failure
						metricsCollector.recordSkip(jobExecutionId, recordIdHash, batchRecord,
								error, actualPartitionName, null);
					}
				} catch (Exception metricsException) {
					logger.warn("[{}] Failed to record metrics for FEATURE_ID: {} - {}",
							actualPartitionName, batchRecord.getFeatureId(), metricsException.getMessage());
				}
			}
		}
	}

	/**
	 * Handles parameter deserialization failures by logging and creating
	 * appropriate exception.
	 */
	private IllegalStateException handleParameterDeserializationFailure(
			String parametersJson, Exception cause, String errorDescription) {
		// Create enhanced contextual message
		String contextualMessage = String.format(
				"[%s] %s. JSON length: %d, Exception type: %s, Root cause: %s",
				partitionName, errorDescription,
				parametersJson != null ? parametersJson.length() : 0,
				cause.getClass().getSimpleName(),
				cause.getMessage() != null ? cause.getMessage() : BatchConstants.ErrorMessage.NO_ERROR_MESSAGE);

		// Log the failure with full context
		logger.error(contextualMessage, cause);

		return new IllegalStateException(contextualMessage, cause);
	}

	/**
	 * Handles chunk processing failures by logging, recording metrics, and creating
	 * appropriate exception.
	 */
	private RuntimeException handleChunkProcessingFailure(
			java.util.List<BatchRecord> batchRecords, String actualPartitionName,
			Exception cause, String errorDescription) {
		// Create enhanced contextual message
		String contextualMessage = String.format(
				"[%s] %s. Chunk size: %d, Exception type: %s, Root cause: %s",
				actualPartitionName, errorDescription, batchRecords.size(),
				cause.getClass().getSimpleName(),
				cause.getMessage() != null ? cause.getMessage() : BatchConstants.ErrorMessage.NO_ERROR_MESSAGE);

		// Log the failure with full context
		logger.error(contextualMessage, cause);

		// Record metrics for failed chunk processing
		recordChunkMetrics(batchRecords, actualPartitionName, false, cause);

		if (cause instanceof RuntimeException runtimeException) {
			return runtimeException;
		} else {
			return new RuntimeException(contextualMessage, cause);
		}
	}
}
