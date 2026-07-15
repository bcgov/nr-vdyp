package ca.bc.gov.nrs.vdyp.batch.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobExecutionNotRunningException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ca.bc.gov.nrs.vdyp.batch.service.BatchMetricsCollector;
import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;
import ca.bc.gov.nrs.vdyp.batch.util.BatchUtils;
import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.ProjectionRequestValidationException;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.ValidationMessage;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.ValidationMessageKind;

@RestController
@RequestMapping("/api/batch")
public class BatchController {

	private static final Logger logger = LoggerFactory.getLogger(BatchController.class);

	private final JobLauncher jobLauncher;
	private final Job fetchAndPartitionJob;
	private final JobExplorer jobExplorer;
	private final JobOperator jobOperator;
	@SuppressWarnings("unused")
	private final BatchMetricsCollector metricsCollector;

	@Value("${batch.root-directory}")
	private String batchRootDirectory;

	@Value("${batch.partition.default-number-of-partitions}")
	private Integer defaultNumPartitions;

	@Value("${batch.partition.job-search-chunk-size}")
	private int jobSearchChunkSize;

	@Value("${batch.reader.default-chunk-size}")
	private Integer defaultChunkSize;

	public BatchController(
			@Qualifier("asyncJobLauncher") JobLauncher jobLauncher,
			@Qualifier("fetchAndPartitionJob") Job fetchAndPartitionJob, JobExplorer jobExplorer,
			BatchMetricsCollector metricsCollector, JobOperator jobOperator
	) {
		this.jobLauncher = jobLauncher;
		this.fetchAndPartitionJob = fetchAndPartitionJob;
		this.jobExplorer = jobExplorer;
		this.metricsCollector = metricsCollector;
		this.jobOperator = jobOperator;
	}

	/**
	 * Start a new batch job execution using pre-uploaded files identified by projection GUID.
	 */
	@PostMapping(value = "/startWithGUIDs", //
			consumes = MediaType.MULTIPART_FORM_DATA_VALUE, //
			produces = MediaType.APPLICATION_JSON_VALUE

	)
	public ResponseEntity<Map<String, Object>> startBatchJobPersistedID(
			@RequestParam("projectionGUID") UUID projectionGUID,
			@RequestParam("projectionParametersJson") String projectionParametersJson
	) {

		try {

			logRequestDetails(projectionGUID, projectionParametersJson);

			Map<String, Object> response = new HashMap<>();

			JobExecution jobExecution = executeJob(projectionGUID, projectionParametersJson);
			buildSuccessResponse(response, jobExecution);

			return ResponseEntity.ok(response);

		} catch (ProjectionRequestValidationException e) {
			return ResponseEntity.badRequest().header("content-type", "application/json")
					.body(createValidationErrorResponse(e));
		} catch (Exception e) {
			return buildErrorResponse(e);
		}
	}

	@PostMapping(value = "/stop/{jobGuid}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> stopBatchJob(@PathVariable UUID jobGuid) {
		return stopBatchJob(jobGuid, BatchConstants.Job.GUID, "GUID");
	}

	@PostMapping(value = "/stop/projection/{projectionGuid}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> stopBatchJobByProjectionGuid(@PathVariable UUID projectionGuid) {
		return stopBatchJob(projectionGuid, BatchConstants.GuidInput.PROJECTION_GUID, "projection GUID");
	}

	private ResponseEntity<Map<String, Object>>
			stopBatchJob(UUID requestedGuid, String jobParameterName, String guidDescription) {
		Map<String, Object> response = new HashMap<>();
		Long executionId = null;

		try {
			logger.debug("Attempting to stop job with {}: {}", guidDescription, requestedGuid);

			JobExecution jobExecution = findJobExecutionByJobParameter(
					jobParameterName, requestedGuid.toString(),
					BatchConstants.GuidInput.PROJECTION_GUID.equals(jobParameterName)
			);
			executionId = jobExecution.getId();
			addJobIdentifiers(response, jobExecution, requestedGuid, jobParameterName);

			String jobGuid = jobExecution.getJobParameters().getString(BatchConstants.Job.GUID);
			logger.debug("[GUID: {}] Found JobExecution ID: {}, attempting to stop...", jobGuid, executionId);

			// Stop the job execution - this sends a stop signal to the running job
			boolean stopped = jobOperator.stop(executionId);

			if (stopped) {
				response.put(BatchConstants.Job.EXECUTION_ID, executionId);
				response.put(BatchConstants.Job.STATUS, "STOP_REQUESTED");
				response.put(
						BatchConstants.Job.MESSAGE,
						"Stop request sent successfully. Job will stop after completing current chunk."
				);
				response.put(BatchConstants.Common.TIMESTAMP, System.currentTimeMillis());

				logger.info("[GUID: {}] Stop request sent successfully for JobExecution ID: {}", jobGuid, executionId);

				return ResponseEntity.ok(response);
			} else {
				response.put(BatchConstants.Job.EXECUTION_ID, executionId);
				response.put(BatchConstants.Job.STATUS, "STOP_FAILED");
				response.put(BatchConstants.Job.MESSAGE, "Job execution could not be stopped. It may not be running.");
				response.put(BatchConstants.Common.TIMESTAMP, System.currentTimeMillis());

				logger.warn(
						"[GUID: {}] Failed to stop JobExecution ID: {}. Job may not be running.", jobGuid, executionId
				);

				return ResponseEntity.badRequest().body(response);
			}

		} catch (JobExecutionNotRunningException e) {
			// Job is already stopping or has stopped - this is not an error, just inform the user
			response.put(BatchConstants.Job.EXECUTION_ID, executionId);
			response.put(BatchConstants.Job.STATUS, "ALREADY_STOPPING");
			response.put(
					BatchConstants.Job.MESSAGE,
					"Job is already in the process of stopping or has already been stopped. "
							+ "Please check job status for current state."
			);
			response.put(BatchConstants.Common.TIMESTAMP, System.currentTimeMillis());

			logger.debug("[{}: {}] Job is already stopping or stopped", guidDescription, requestedGuid);
			// Return 202 Accepted - the stop request was already accepted and is being processed
			return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);

		} catch (NoSuchJobExecutionException e) {
			addRequestedGuid(response, requestedGuid, jobParameterName);
			response.put(BatchConstants.Job.ERROR, "Job execution not found");
			response.put(
					BatchConstants.Job.MESSAGE,
					"No job execution found with " + guidDescription + ": " + requestedGuid + ". "
							+ "Please verify the GUID is correct."
			);
			response.put(BatchConstants.Common.TIMESTAMP, System.currentTimeMillis());

			logger.error("Job execution not found with {}: {}", guidDescription, requestedGuid);
			return ResponseEntity.status(404).body(response);

		} catch (Exception e) {
			addRequestedGuid(response, requestedGuid, jobParameterName);
			if (executionId != null) {
				response.put(BatchConstants.Job.EXECUTION_ID, executionId);
			}
			response.put(BatchConstants.Job.ERROR, "Failed to stop job execution");
			response.put(
					BatchConstants.Job.MESSAGE,
					"An error occurred while stopping the job: "
							+ (e.getMessage() != null ? e.getMessage() : "Unknown error")
			);
			response.put(BatchConstants.Common.TIMESTAMP, System.currentTimeMillis());

			logger.error(
					"[{}: {}] Error stopping job execution: {}", guidDescription, requestedGuid, e.getMessage(), e
			);
			return ResponseEntity.internalServerError().body(response);
		}
	}

	@GetMapping(value = "/status/{jobGuid}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> getJobStatus(@PathVariable UUID jobGuid) {
		Map<String, Object> response = new HashMap<>();

		JobExecution jobExecution;
		Long executionId;

		try {
			logger.debug("Getting status for job with GUID: {}", jobGuid);

			jobExecution = findJobExecutionByJobParameter(BatchConstants.Job.GUID, jobGuid.toString(), false);
			executionId = jobExecution.getId();

			logger.debug("[GUID: {}] Found JobExecution ID: {}", jobGuid, executionId);
		} catch (NoSuchJobExecutionException e) {
			response.put(BatchConstants.Job.GUID, jobGuid);
			response.put(BatchConstants.Job.ERROR, "Job execution not found");
			response.put(
					BatchConstants.Job.MESSAGE,
					"No job execution found with GUID: " + jobGuid + ". " + "Please verify the GUID is correct."
			);
			response.put(BatchConstants.Common.TIMESTAMP, System.currentTimeMillis());

			logger.error("Job execution not found with GUID: {}", jobGuid);
			return ResponseEntity.status(404).body(response);

		} catch (Exception e) {
			response.put(BatchConstants.Job.GUID, jobGuid);
			response.put(BatchConstants.Job.ERROR, "Failed to get job status");
			response.put(
					BatchConstants.Job.MESSAGE,
					"An error occurred while retrieving job status: "
							+ (e.getMessage() != null ? e.getMessage() : "Unknown error")
			);
			response.put(BatchConstants.Common.TIMESTAMP, System.currentTimeMillis());

			logger.error("[GUID: {}] Error getting job status: {}", jobGuid, e.getMessage(), e);
			return ResponseEntity.internalServerError().body(response);
		}

		boolean isRunning = jobExecution.getStatus().isRunning();

		int polygonsProcessed = jobExecution.getStepExecutions().stream() //
				.filter(se -> se.getStepName().startsWith(BatchConstants.Job.WORKER_STEP_NAME)) //
				.mapToInt(se -> se.getExecutionContext().getInt(BatchConstants.Job.POLYGONS_PROCESSED, 0)) //
				.sum();
		int polygonsSkipped = jobExecution.getStepExecutions().stream() //
				.filter(se -> se.getStepName().startsWith(BatchConstants.Job.WORKER_STEP_NAME)) //
				.mapToInt(se -> se.getExecutionContext().getInt(BatchConstants.Job.POLYGONS_SKIPPED, 0)) //
				.sum();
		int projectionErrors = jobExecution.getStepExecutions().stream() //
				.filter(se -> se.getStepName().startsWith(BatchConstants.Job.WORKER_STEP_NAME)) //
				.mapToInt(se -> se.getExecutionContext().getInt(BatchConstants.Job.PROJECTION_ERRORS, 0)) //
				.sum();
		int totalPolygons = jobExecution.getExecutionContext().getInt(BatchConstants.Job.TOTAL_POLYGONS, 0);
		int workers = BatchUtils.calculateActiveWorkers(jobExecution, isRunning);

		response.put(BatchConstants.Job.GUID, jobGuid);
		response.put(BatchConstants.Job.EXECUTION_ID, executionId);
		response.put(BatchConstants.Job.NAME, jobExecution.getJobInstance().getJobName());
		response.put(BatchConstants.Job.STATUS, jobExecution.getStatus().toString());
		response.put(BatchConstants.Job.IS_RUNNING, isRunning);
		response.put(BatchConstants.Job.PROJECTION_ERRORS, projectionErrors);
		response.put(BatchConstants.Job.POLYGONS_PROCESSED, polygonsProcessed);
		response.put(BatchConstants.Job.POLYGONS_SKIPPED, polygonsSkipped);
		response.put(BatchConstants.Job.TOTAL_POLYGONS, totalPolygons);
		response.put(BatchConstants.Job.WORKERS, workers);

		if (jobExecution.getStartTime() != null) {
			response.put(BatchConstants.Job.START_TIME, jobExecution.getStartTime());
		}
		if (jobExecution.getEndTime() != null) {
			response.put(BatchConstants.Job.END_TIME, jobExecution.getEndTime());
		}

		response.put(BatchConstants.Common.TIMESTAMP, System.currentTimeMillis());

		logger.debug("[GUID: {}] Job status: {}, Running: {}", jobGuid, jobExecution.getStatus(), isRunning);

		return ResponseEntity.ok(response);
	}

	@GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> health() {
		Map<String, Object> response = new HashMap<>();
		response.put(BatchConstants.Job.STATUS, "UP");
		response.put("service", "VDYP Batch Processing Service");
		response.put(
				"availableEndpoints",
				Arrays.asList(
						"/api/batch/startWithGUIDs", "/api/batch/stop/{jobGuid}", "/api/batch/status/{jobGuid}",
						"/api/batch/health"
				)
		);
		response.put(BatchConstants.Common.TIMESTAMP, System.currentTimeMillis());
		return ResponseEntity.ok(response);
	}

	private void logRequestDetails(UUID projectionGUID, String parametersJson) {
		logger.debug("=== VDYP Batch Job Request ===");
		logger.debug("projectionGUID: {} ", projectionGUID);
		logger.debug("parametersJson: {} ", parametersJson);
	}

	private JobExecution executeJob(UUID projectionGuid, String projectionParametersJson)
			throws ProjectionRequestValidationException {

		validateParametersJSON(projectionParametersJson);

		try {
			String jobGuid = BatchUtils.createJobGuid();
			String jobTimestamp = BatchUtils.createJobTimestamp();

			Path jobBaseDir = ensureProjectionDirectoryExists(jobGuid);

			logger.debug("[GUID: {}] Starting GUID-based job", jobGuid);

			// Thread count will be computed dynamically by DownloadAndPartitionTasklet after files are fetched.
			// Pass defaultNumPartitions as a safe fallback in case the tasklet cannot determine the count.
			JobParameters jobParameters = buildJobParameters(
					projectionParametersJson, defaultNumPartitions, jobGuid, jobTimestamp, jobBaseDir.toString(),
					projectionGuid, defaultChunkSize
			);
			JobExecution jobExecution = jobLauncher.run(fetchAndPartitionJob, jobParameters);

			logger.info("[GUID: {}] Batch job started - Execution ID: {}", jobGuid, jobExecution.getId());

			return jobExecution;

		} catch (Exception e) {
			logger.error("Failed to start GUID based job", e);

			String errorMessage = e.getMessage() != null ? e.getMessage()
					: "Unknown error (" + e.getClass().getSimpleName() + ")";

			throw new ProjectionRequestValidationException(
					List.of(
							new ValidationMessage(
									ValidationMessageKind.GENERIC, "Failed to start GUID based job: " + errorMessage
							)
					)
			);
		}
	}

	private void validateParametersJSON(String projectionParametersJson) throws ProjectionRequestValidationException {

		if (projectionParametersJson == null || projectionParametersJson.trim().isEmpty()) {
			throw new ProjectionRequestValidationException(
					List.of(
							new ValidationMessage(
									ValidationMessageKind.GENERIC,
									"VDYP projection parameters are required but not provided in the request"
							)
					)
			);
		}
	}

	private Path ensureProjectionDirectoryExists(String jobGuid) throws IOException {
		Path batchRootDir = Paths.get(batchRootDirectory);

		if (!Files.exists(batchRootDir)) {
			Files.createDirectories(batchRootDir);
			logger.debug("Created batch root directory: {}", batchRootDir);
		}

		String jobBaseFolderName = BatchUtils.createJobFolderName(BatchConstants.Job.BASE_FOLDER_PREFIX, jobGuid);
		Path jobBaseDir = batchRootDir.resolve(jobBaseFolderName);
		Files.createDirectories(jobBaseDir);

		logger.debug("Created job base directory: {} (GUID: {})", jobBaseDir, jobGuid);
		return jobBaseDir;
	}

	/** Builds job parameters for GUID-based flow (files fetched from COMS by DownloadAndPartitionTasklet). */
	private JobParameters buildJobParameters(
			String projectionParametersJson, Integer numPartitions, String jobGuid, String jobTimestamp,
			String jobBaseDir, UUID projectionGUID, Integer chunkSize
	) {
		return new JobParametersBuilder().addString(BatchConstants.Job.GUID, jobGuid)
				.addString(BatchConstants.Projection.PARAMETERS_JSON, projectionParametersJson)
				.addString(BatchConstants.Job.TIMESTAMP, jobTimestamp)
				.addString(BatchConstants.Job.BASE_DIR, jobBaseDir)
				.addString(BatchConstants.GuidInput.PROJECTION_GUID, projectionGUID.toString())
				.addLong(BatchConstants.Partition.NUMBER, numPartitions.longValue())
				.addLong(BatchConstants.Chunk.SIZE, chunkSize.longValue(), false).toJobParameters();
	}

	private void buildSuccessResponse(Map<String, Object> response, JobExecution jobExecution) {
		String jobGuid = jobExecution.getJobParameters().getString(BatchConstants.Job.GUID);

		response.put(BatchConstants.Job.GUID, jobGuid);
		response.put(BatchConstants.Job.EXECUTION_ID, jobExecution.getId());
		response.put(BatchConstants.Job.NAME, jobExecution.getJobInstance().getJobName());
		response.put(BatchConstants.Job.STATUS, jobExecution.getStatus().toString());

		if (jobExecution.getStartTime() != null) {
			response.put(BatchConstants.Job.START_TIME, jobExecution.getStartTime());
		} else {
			response.put(BatchConstants.Job.START_TIME, java.time.LocalDateTime.now());
		}

		response.put(BatchConstants.Common.TIMESTAMP, System.currentTimeMillis());
		response.put(BatchConstants.Job.MESSAGE, "VDYP Batch job started successfully");
	}

	private Map<String, Object> createValidationErrorResponse(ProjectionRequestValidationException e) {
		Map<String, Object> errorResponse = new HashMap<>();
		errorResponse.put("validationMessages", e.getValidationMessages());
		errorResponse.put(BatchConstants.Job.ERROR, "Validation failed");
		errorResponse
				.put(BatchConstants.Job.MESSAGE, "Request validation failed - check validation messages for details");
		return errorResponse;
	}

	private ResponseEntity<Map<String, Object>> buildErrorResponse(Exception e) {
		Map<String, Object> errorResponse = new HashMap<>();
		errorResponse.put(BatchConstants.Job.ERROR, "Failed to start batch job");
		errorResponse.put(BatchConstants.Job.MESSAGE, e.getMessage() == null ? "unknown reason" : e.getMessage());
		return ResponseEntity.internalServerError().body(errorResponse);
	}

	private JobExecution
			findJobExecutionByJobParameter(String parameterName, String expectedValue, boolean preferRunning)
					throws NoSuchJobExecutionException {
		List<String> jobNames = jobExplorer.getJobNames();
		JobExecution fallbackMatch = null;

		for (String jobName : jobNames) {
			JobExecutionSearchResult result = searchJobExecutionsByName(
					jobName, parameterName, expectedValue, preferRunning
			);
			if (result.primaryMatch() != null) {
				return result.primaryMatch();
			}
			if (fallbackMatch == null) {
				fallbackMatch = result.fallbackMatch();
			}
		}

		if (fallbackMatch != null) {
			return fallbackMatch;
		}

		throw new NoSuchJobExecutionException("No job execution found with " + parameterName + ": " + expectedValue);
	}

	/**
	 * Searches for a job execution by job name and job parameter value.
	 *
	 * @param jobName The name of the job to search
	 * @return matching executions, preferring a running match when requested
	 */
	private JobExecutionSearchResult searchJobExecutionsByName(
			String jobName, String parameterName, String expectedValue, boolean preferRunning
	) {
		JobExecution fallbackMatch = null;
		try {
			long totalInstances = jobExplorer.getJobInstanceCount(jobName);

			for (long start = 0; start < totalInstances; start += jobSearchChunkSize) {
				JobExecutionSearchResult result = searchJobExecutionsInChunk(
						jobName, parameterName, expectedValue, start, preferRunning
				);
				if (result.primaryMatch() != null) {
					return result;
				}
				if (fallbackMatch == null) {
					fallbackMatch = result.fallbackMatch();
				}
			}
		} catch (NoSuchJobException e) {
			logger.error(
					"Job {} not found while searching for {} {}: {}", jobName, parameterName, expectedValue,
					e.getMessage()
			);
		}

		return new JobExecutionSearchResult(null, fallbackMatch);
	}

	/**
	 * Searches for a job execution within a chunk of job instances.
	 *
	 * @param jobName The name of the job
	 * @param start   The starting index for the chunk
	 * @return matching executions, preferring a running match when requested
	 */
	private JobExecutionSearchResult searchJobExecutionsInChunk(
			String jobName, String parameterName, String expectedValue, long start, boolean preferRunning
	) {
		List<JobInstance> jobInstances = jobExplorer.getJobInstances(jobName, (int) start, jobSearchChunkSize);
		JobExecution fallbackMatch = null;

		for (JobInstance jobInstance : jobInstances) {
			JobExecutionSearchResult result = findMatchingExecution(
					jobInstance, parameterName, expectedValue, preferRunning
			);
			if (result.primaryMatch() != null) {
				return result;
			}
			if (fallbackMatch == null) {
				fallbackMatch = result.fallbackMatch();
			}
		}

		return new JobExecutionSearchResult(null, fallbackMatch);
	}

	/**
	 * Finds a job execution matching the given job parameter within a job instance.
	 *
	 * @param jobInstance The job instance to search
	 * @return matching executions, preferring a running match when requested
	 */
	private JobExecutionSearchResult findMatchingExecution(
			JobInstance jobInstance, String parameterName, String expectedValue, boolean preferRunning
	) {
		List<JobExecution> jobExecutions = jobExplorer.getJobExecutions(jobInstance);
		JobExecution fallbackMatch = null;

		for (JobExecution execution : jobExecutions) {
			String parameterValue = execution.getJobParameters().getString(parameterName);
			if (expectedValue.equals(parameterValue)) {
				if (!preferRunning || (execution.getStatus() != null && execution.getStatus().isRunning())) {
					return new JobExecutionSearchResult(execution, null);
				}
				if (fallbackMatch == null) {
					fallbackMatch = execution;
				}
			}
		}

		return new JobExecutionSearchResult(null, fallbackMatch);
	}

	private void addJobIdentifiers(
			Map<String, Object> response, JobExecution jobExecution, UUID requestedGuid, String jobParameterName
	) {
		JobParameters parameters = jobExecution.getJobParameters();
		response.put(BatchConstants.Job.GUID, parameters.getString(BatchConstants.Job.GUID));

		String projectionGuid = parameters.getString(BatchConstants.GuidInput.PROJECTION_GUID);
		if (projectionGuid != null) {
			response.put(BatchConstants.GuidInput.PROJECTION_GUID, projectionGuid);
		} else if (BatchConstants.GuidInput.PROJECTION_GUID.equals(jobParameterName)) {
			response.put(BatchConstants.GuidInput.PROJECTION_GUID, requestedGuid);
		}
	}

	private void addRequestedGuid(Map<String, Object> response, UUID requestedGuid, String jobParameterName) {
		response.put(jobParameterName, requestedGuid);
	}

	private record JobExecutionSearchResult(JobExecution primaryMatch, JobExecution fallbackMatch) {
	}
}
