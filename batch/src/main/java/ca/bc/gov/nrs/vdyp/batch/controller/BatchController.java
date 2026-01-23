package ca.bc.gov.nrs.vdyp.batch.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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
import org.springframework.web.multipart.MultipartFile;

import ca.bc.gov.nrs.vdyp.batch.exception.BatchPartitionException;
import ca.bc.gov.nrs.vdyp.batch.service.BatchInputPartitioner;
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
	private final Job partitionedJob;
	private final Job fetchAndPartitionJob;
	private final BatchInputPartitioner inputPartitioner;
	private final JobOperator jobOperator;

	private final JobExplorer jobExplorer;
	@SuppressWarnings("unused")
	private final BatchMetricsCollector metricsCollector;

	@Value("${batch.root-directory}")
	private String batchRootDirectory;

	@Value("${batch.partition.default-number-of-partitions}")
	private Integer defaultNumPartitions;

	@Value("${batch.partition.job-search-chunk-size}")
	private int jobSearchChunkSize;

	// This class is instantiated by Spring's dependency injection container during application startup.
	// As a @RestController, Spring automatically creates a singleton instance and injects the required dependencies.
	// It's available at runtime for handling HTTP requests to /api/batch endpoints, not just in tests.
	public BatchController(
			@Qualifier("asyncJobLauncher") JobLauncher jobLauncher, @Qualifier("partitionedJob") Job partitionedJob,
			@Qualifier("fetchAndPartitionJob") Job fetchAndPartitionJob, JobExplorer jobExplorer,
			BatchMetricsCollector metricsCollector, BatchInputPartitioner inputPartitioner, JobOperator jobOperator
	) {
		this.jobLauncher = jobLauncher;
		this.partitionedJob = partitionedJob;
		this.fetchAndPartitionJob = fetchAndPartitionJob;
		this.jobExplorer = jobExplorer;
		this.metricsCollector = metricsCollector;
		this.inputPartitioner = inputPartitioner;
		this.jobOperator = jobOperator;
	}

	/**
	 * Start a new batch job execution
	 */
	@PostMapping(
			value = "/start", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE
	)
	public ResponseEntity<Map<String, Object>> startBatchJobWithFiles(
			@RequestParam("polygonFile") MultipartFile polygonFile, @RequestParam("layerFile") MultipartFile layerFile,
			@RequestParam("parameters") String projectionParametersJson
	) {

		try {
			logRequestDetails(polygonFile, layerFile, projectionParametersJson);

			Map<String, Object> response = new HashMap<>();

			JobExecution jobExecution = executeJob(polygonFile, layerFile, projectionParametersJson);
			buildSuccessResponse(response, jobExecution);

			return ResponseEntity.ok(response);

		} catch (ProjectionRequestValidationException e) {
			return ResponseEntity.badRequest().header("content-type", "application/json")
					.body(createValidationErrorResponse(e));
		} catch (Exception e) {
			return buildErrorResponse(e);
		}
	}

	/**
	 * Start a new batch job execution
	 */
	@PostMapping(
			value = "/startWithGUIDs", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE
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
		Map<String, Object> response = new HashMap<>();
		Long executionId = null;

		try {
			logger.debug("Attempting to stop job with GUID: {}", jobGuid);

			JobExecution jobExecution = findJobExecutionByGuid(jobGuid.toString());
			executionId = jobExecution.getId();

			logger.debug("[GUID: {}] Found JobExecution ID: {}, attempting to stop...", jobGuid, executionId);

			// Stop the job execution - this sends a stop signal to the running job
			boolean stopped = jobOperator.stop(executionId);

			if (stopped) {
				response.put(BatchConstants.Job.GUID, jobGuid);
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
				response.put(BatchConstants.Job.GUID, jobGuid);
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
			response.put(BatchConstants.Job.GUID, jobGuid);
			response.put(BatchConstants.Job.EXECUTION_ID, executionId);
			response.put(BatchConstants.Job.STATUS, "ALREADY_STOPPING");
			response.put(
					BatchConstants.Job.MESSAGE,
					"Job is already in the process of stopping or has already been stopped. "
							+ "Please check job status for current state."
			);
			response.put(BatchConstants.Common.TIMESTAMP, System.currentTimeMillis());

			logger.debug("[GUID: {}] Job is already stopping or stopped", jobGuid);
			// Return 202 Accepted - the stop request was already accepted and is being processed
			return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);

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

			logger.error("[GUID: {}] Error stopping job execution: {}", jobGuid, e.getMessage(), e);
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

			jobExecution = findJobExecutionByGuid(jobGuid.toString());
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

		// Count total partitions and completed partitions

		long totalPartitions = jobExecution.getStepExecutions().stream()
				.filter(stepExecution -> stepExecution.getStepName().startsWith("workerStep:")).count();

		long completedPartitions = jobExecution.getStepExecutions().stream()
				.filter(stepExecution -> stepExecution.getStepName().startsWith("workerStep:"))
				.filter(
						stepExecution -> stepExecution.getStatus().isUnsuccessful()
								|| stepExecution.getStatus() == org.springframework.batch.core.BatchStatus.COMPLETED
				).count();

		response.put(BatchConstants.Job.GUID, jobGuid);
		response.put(BatchConstants.Job.EXECUTION_ID, executionId);
		response.put(BatchConstants.Job.NAME, jobExecution.getJobInstance().getJobName());
		response.put(BatchConstants.Job.STATUS, jobExecution.getStatus().toString());
		response.put(BatchConstants.Job.IS_RUNNING, isRunning);
		response.put(BatchConstants.Job.TOTAL_PARTITIONS, totalPartitions);
		response.put(BatchConstants.Job.COMPLETED_PARTITIONS, completedPartitions);

		if (jobExecution.getStartTime() != null) {
			response.put(BatchConstants.Job.START_TIME, jobExecution.getStartTime());
		}
		if (jobExecution.getEndTime() != null) {
			response.put(BatchConstants.Job.END_TIME, jobExecution.getEndTime());
		}

		response.put(BatchConstants.Common.TIMESTAMP, System.currentTimeMillis());

		logger.debug(
				"[GUID: {}] Job status: {}, Running: {}, Total Partitions: {}, Completed Partitions: {}", jobGuid,
				jobExecution.getStatus(), isRunning, totalPartitions, completedPartitions
		);

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
						"/api/batch/start", "/api/batch/stop/{jobGuid}", "/api/batch/status/{jobGuid}",
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

	private void logRequestDetails(MultipartFile polygonFile, MultipartFile layerFile, String parametersJson) {
		logger.debug("=== VDYP Batch Job Request ===");
		logger.debug("Polygon file: {} ({} bytes)", polygonFile.getOriginalFilename(), polygonFile.getSize());
		logger.debug("Layer file: {} ({} bytes)", layerFile.getOriginalFilename(), layerFile.getSize());
		logger.debug(
				"Parameters provided: {}", (parametersJson != null && !parametersJson.trim().isEmpty()) ? "yes" : "no"
		);
	}

	private JobExecution executeJob(UUID projectionGuid, String projectionParametersJson)
			throws ProjectionRequestValidationException {

		validateParametersJSON(projectionParametersJson);

		try {
			String jobGuid = BatchUtils.createJobGuid();
			String jobTimestamp = BatchUtils.createJobTimestamp();

			Path jobBaseDir = ensureProjectionDirectoryExists(jobGuid);

			logger.debug("[GUID: {}] Using {} partitions", jobGuid, defaultNumPartitions);

			// Now start the job with the partition directory included in parameters
			JobParameters jobParameters = buildJobParameters(
					projectionParametersJson, defaultNumPartitions, jobGuid, jobTimestamp, jobBaseDir.toString(),
					projectionGuid
			);
			JobExecution jobExecution = jobLauncher.run(fetchAndPartitionJob, jobParameters);

			logger.info(
					"[GUID: {}] Batch job started - Execution ID: {}, Partitions: {}", jobGuid, jobExecution.getId(),
					defaultNumPartitions
			);

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

	private JobExecution executeJob(MultipartFile polygonFile, MultipartFile layerFile, String projectionParametersJson)
			throws ProjectionRequestValidationException {
		validateParametersJSON(projectionParametersJson);
		validateProjectionParameters(polygonFile, layerFile);

		try {
			String jobGuid = BatchUtils.createJobGuid();
			String jobTimestamp = BatchUtils.createJobTimestamp();

			Path jobBaseDir = ensureProjectionDirectoryExists(jobGuid);

			logger.debug("[GUID: {}] Using {} partitions", jobGuid, defaultNumPartitions);

			// Partition CSV files using streaming approach BEFORE starting the job
			logger.debug("[GUID: {}] Starting CSV partitioning...", jobGuid);
			int featureIdToPartition = inputPartitioner
					.partitionCsvFiles(polygonFile, layerFile, defaultNumPartitions, jobBaseDir, jobGuid);

			logger.debug(
					"[GUID: {}] CSV files partitioned successfully. Partitions: {}, Total FEATURE_IDs: {}", jobGuid,
					defaultNumPartitions, featureIdToPartition
			);

			// Now start the job with the partition directory included in parameters
			JobParameters jobParameters = buildJobParameters(
					projectionParametersJson, defaultNumPartitions, jobGuid, jobTimestamp, jobBaseDir.toString()
			);
			JobExecution jobExecution = jobLauncher.run(partitionedJob, jobParameters);

			logger.info(
					"[GUID: {}] Batch job started - Execution ID: {}, Partitions: {}", jobGuid, jobExecution.getId(),
					defaultNumPartitions
			);

			return jobExecution;

		} catch (BatchPartitionException e) {
			throw new ProjectionRequestValidationException(
					List.of(new ValidationMessage(ValidationMessageKind.GENERIC, e.getMessage()))
			);

		} catch (Exception e) {
			logger.error("Failed to process uploaded CSV files", e);

			String errorMessage = e.getMessage() != null ? e.getMessage()
					: "Unknown error (" + e.getClass().getSimpleName() + ")";

			throw new ProjectionRequestValidationException(
					List.of(
							new ValidationMessage(
									ValidationMessageKind.GENERIC,
									"Failed to process uploaded CSV files: " + errorMessage
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

	/**
	 * Validates that projection parameters are provided and not empty.
	 */
	private void validateProjectionParameters(MultipartFile polygonFile, MultipartFile layerFile)
			throws ProjectionRequestValidationException {

		// Validate polygon file
		if (polygonFile == null || polygonFile.isEmpty()) {
			throw new ProjectionRequestValidationException(
					List.of(
							new ValidationMessage(
									ValidationMessageKind.GENERIC,
									"Polygon file is required but not provided in the request"
							)
					)
			);
		}

		if (polygonFile.getSize() == 0) {
			throw new ProjectionRequestValidationException(
					List.of(
							new ValidationMessage(
									ValidationMessageKind.GENERIC,
									"Polygon file is empty. Please provide a valid CSV file with polygon data"
							)
					)
			);
		}

		validateFileHasContent(polygonFile, "Polygon");

		// Validate layer file
		if (layerFile == null || layerFile.isEmpty()) {
			throw new ProjectionRequestValidationException(
					List.of(
							new ValidationMessage(
									ValidationMessageKind.GENERIC,
									"Layer file is required but not provided in the request"
							)
					)
			);
		}

		if (layerFile.getSize() == 0) {
			throw new ProjectionRequestValidationException(
					List.of(
							new ValidationMessage(
									ValidationMessageKind.GENERIC,
									"Layer file is empty. Please provide a valid CSV file with layer data"
							)
					)
			);
		}

		validateFileHasContent(layerFile, "Layer");
	}

	/**
	 * Validates that a file contains actual data (not just whitespace/empty lines). Does NOT parse the file, only
	 * checks if there is any non-whitespace content.
	 *
	 * @param file     The multipart file to validate
	 * @param fileType The type of file (e.g., "Polygon", "Layer") for error messages
	 * @throws ProjectionRequestValidationException if file contains only whitespace/empty lines
	 */
	private void validateFileHasContent(MultipartFile file, String fileType)
			throws ProjectionRequestValidationException {

		try (
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)
				)
		) {
			String line;
			boolean headerChecked = false;

			while ( (line = reader.readLine()) != null) {
				// Skip blank lines and optionally skip header (only checked once)
				boolean shouldSkip = line.isBlank() || (!headerChecked && BatchUtils.isHeaderLine(line));

				if (!headerChecked && !line.isBlank()) {
					headerChecked = true;
				}

				if (!shouldSkip) {
					// Found at least one data line with content - file is valid
					return;
				}
			}

			// If we reach here, file contains only empty lines/whitespace/headers
			throw new ProjectionRequestValidationException(
					List.of(
							new ValidationMessage(
									ValidationMessageKind.GENERIC,
									fileType + " file contains only empty lines or whitespace. "
											+ "Please provide a valid CSV file with " + fileType.toLowerCase() + " data"
							)
					)
			);

		} catch (IOException e) {
			throw new ProjectionRequestValidationException(
					List.of(
							new ValidationMessage(
									ValidationMessageKind.GENERIC,
									"Failed to read " + fileType.toLowerCase() + " file: " + e.getMessage()
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

	private JobParameters buildJobParameters(
			String projectionParametersJson, Integer numPartitions, String jobGuid, String jobTimestamp,
			String jobBaseDir
	) {

		JobParametersBuilder parametersBuilder = new JobParametersBuilder().addString(BatchConstants.Job.GUID, jobGuid)
				.addString(BatchConstants.Projection.PARAMETERS_JSON, projectionParametersJson)
				.addString(BatchConstants.Job.TIMESTAMP, jobTimestamp)
				.addString(BatchConstants.Job.BASE_DIR, jobBaseDir)
				.addLong(BatchConstants.Partition.NUMBER, numPartitions.longValue());

		return parametersBuilder.toJobParameters();
	}

	private JobParameters buildJobParameters(
			String projectionParametersJson, Integer numPartitions, String jobGuid, String jobTimestamp,
			String jobBaseDir, UUID projectionGUID
	) {

		JobParametersBuilder parametersBuilder = new JobParametersBuilder().addString(BatchConstants.Job.GUID, jobGuid)
				.addString(BatchConstants.Projection.PARAMETERS_JSON, projectionParametersJson)
				.addString(BatchConstants.Job.TIMESTAMP, jobTimestamp)
				.addString(BatchConstants.Job.BASE_DIR, jobBaseDir) //
				.addString(BatchConstants.GuidInput.PROJECTION_GUID, projectionGUID.toString())
				.addString(BatchConstants.Job.BASE_DIR, jobBaseDir)
				.addLong(BatchConstants.Partition.NUMBER, numPartitions.longValue());

		return parametersBuilder.toJobParameters();
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

	private JobExecution findJobExecutionByGuid(String jobGuid) throws NoSuchJobExecutionException {
		List<String> jobNames = jobExplorer.getJobNames();

		for (String jobName : jobNames) {
			JobExecution execution = searchJobExecutionsByName(jobName, jobGuid);
			if (execution != null) {
				return execution;
			}
		}

		throw new NoSuchJobExecutionException("No job execution found with GUID: " + jobGuid);
	}

	/**
	 * Searches for a job execution by job name and GUID.
	 *
	 * @param jobName The name of the job to search
	 * @param jobGuid The GUID to match
	 * @return The JobExecution if found, or null if not found in this job name. Callers MUST check for null before
	 *         using the returned value.
	 */
	private JobExecution searchJobExecutionsByName(String jobName, String jobGuid) {
		try {
			long totalInstances = jobExplorer.getJobInstanceCount(jobName);

			for (long start = 0; start < totalInstances; start += jobSearchChunkSize) {
				JobExecution execution = searchJobExecutionsInChunk(jobName, jobGuid, start);
				if (execution != null) {
					return execution;
				}
			}
		} catch (NoSuchJobException e) {
			logger.error("Job {} not found while searching for GUID {}: {}", jobName, jobGuid, e.getMessage());
		}

		return null;
	}

	/**
	 * Searches for a job execution within a chunk of job instances.
	 *
	 * @param jobName The name of the job
	 * @param jobGuid The GUID to match
	 * @param start   The starting index for the chunk
	 * @return The JobExecution if found in this chunk, or null if not found. Callers MUST check for null before using
	 *         the returned value.
	 */
	private JobExecution searchJobExecutionsInChunk(String jobName, String jobGuid, long start) {
		List<JobInstance> jobInstances = jobExplorer.getJobInstances(jobName, (int) start, jobSearchChunkSize);

		for (JobInstance jobInstance : jobInstances) {
			JobExecution execution = findMatchingExecution(jobInstance, jobGuid);
			if (execution != null) {
				return execution;
			}
		}

		return null;
	}

	/**
	 * Finds a job execution matching the given GUID within a job instance.
	 *
	 * @param jobInstance The job instance to search
	 * @param jobGuid     The GUID to match
	 * @return The JobExecution if found, or null if no execution in this instance matches the GUID. Callers MUST check
	 *         for null before using the returned value.
	 */
	private JobExecution findMatchingExecution(JobInstance jobInstance, String jobGuid) {
		List<JobExecution> jobExecutions = jobExplorer.getJobExecutions(jobInstance);

		for (JobExecution execution : jobExecutions) {
			String executionGuid = execution.getJobParameters().getString(BatchConstants.Job.GUID);
			if (jobGuid.equals(executionGuid)) {
				return execution;
			}
		}

		return null;
	}
}
