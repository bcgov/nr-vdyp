package ca.bc.gov.nrs.vdyp.batch.controller;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobExecutionNotRunningException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
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
import org.springframework.batch.core.JobInstance;

import ca.bc.gov.nrs.vdyp.batch.service.BatchMetricsCollector;
import ca.bc.gov.nrs.vdyp.batch.service.StreamingCsvPartitioner;
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
	private final StreamingCsvPartitioner csvPartitioner;
	private final JobOperator jobOperator;

	private final JobExplorer jobExplorer;
	@SuppressWarnings("unused")
	private final BatchMetricsCollector metricsCollector;

	@Value("${batch.root-directory}")
	private String batchRootDirectory;

	@Value("${batch.partition.default-partition-size}")
	private Integer defaultParitionSize;

	public BatchController(
			@Qualifier("asyncJobLauncher") JobLauncher jobLauncher, Job partitionedJob, JobExplorer jobExplorer,
			BatchMetricsCollector metricsCollector, StreamingCsvPartitioner csvPartitioner, JobOperator jobOperator
	) {
		this.jobLauncher = jobLauncher;
		this.partitionedJob = partitionedJob;
		this.jobExplorer = jobExplorer;
		this.metricsCollector = metricsCollector;
		this.csvPartitioner = csvPartitioner;
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
			@RequestParam(value = "partitionSize", required = false) Integer partitionSize,
			@RequestParam("parameters") String projectionParametersJson
	) {

		try {
			logRequestDetails(polygonFile, layerFile, partitionSize, projectionParametersJson);

			Map<String, Object> response = new HashMap<>();

			if (partitionedJob != null) {
				JobExecution jobExecution = executeJob(polygonFile, layerFile, partitionSize, projectionParametersJson);
				buildSuccessResponse(response, jobExecution);
			} else {
				buildJobNotAvailableResponse(response);
			}

			return ResponseEntity.ok(response);

		} catch (ProjectionRequestValidationException e) {
			return ResponseEntity.badRequest().header("content-type", "application/json")
					.body(createValidationErrorResponse(e));
		} catch (Exception e) {
			return buildErrorResponse(e);
		}
	}

	@PostMapping(value = "/stop/{jobGuid}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> stopBatchJob(@PathVariable String jobGuid) {
		Map<String, Object> response = new HashMap<>();
		Long executionId = null;

		try {
			logger.info("Attempting to stop job with GUID: {}", jobGuid);

			JobExecution jobExecution = findJobExecutionByGuid(jobGuid);
			executionId = jobExecution.getId();

			logger.info("[GUID: {}] Found JobExecution ID: {}, attempting to stop...", jobGuid, executionId);

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

			logger.info("[GUID: {}] Job is already stopping or stopped", jobGuid);
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
	public ResponseEntity<Map<String, Object>> getJobStatus(@PathVariable String jobGuid) {
		Map<String, Object> response = new HashMap<>();

		try {
			logger.info("Getting status for job with GUID: {}", jobGuid);

			JobExecution jobExecution = findJobExecutionByGuid(jobGuid);
			Long executionId = jobExecution.getId();

			logger.info("[GUID: {}] Found JobExecution ID: {}", jobGuid, executionId);

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
			response.put("isRunning", isRunning);
			response.put("totalPartitions", totalPartitions);
			response.put("completedPartitions", completedPartitions);

			if (jobExecution.getStartTime() != null) {
				response.put(BatchConstants.Job.START_TIME, jobExecution.getStartTime());
			}
			if (jobExecution.getEndTime() != null) {
				response.put("endTime", jobExecution.getEndTime());
			}

			response.put(BatchConstants.Common.TIMESTAMP, System.currentTimeMillis());

			logger.info(
					"[GUID: {}] Job status: {}, Running: {}, Total Partitions: {}, Completed Partitions: {}", jobGuid,
					jobExecution.getStatus(), isRunning, totalPartitions, completedPartitions
			);

			return ResponseEntity.ok(response);

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

	private void logRequestDetails(
			MultipartFile polygonFile, MultipartFile layerFile, Integer partitionSize, String parametersJson
	) {
		if (logger.isInfoEnabled()) {
			logger.info("=== VDYP Batch Job Request ===");
			logger.info(
					"Polygon file: {} ({} bytes)", BatchUtils.sanitizeForLogging(polygonFile.getOriginalFilename()),
					polygonFile.getSize()
			);
			logger.info(
					"Layer file: {} ({} bytes)", BatchUtils.sanitizeForLogging(layerFile.getOriginalFilename()),
					layerFile.getSize()
			);
			logger.info("Partition size: {}", partitionSize);
			logger.info("Parameters provided: {}", parametersJson != null ? "yes" : "no");
		}
	}

	private JobExecution executeJob(
			MultipartFile polygonFile, MultipartFile layerFile, Integer partitionSize, String projectionParametersJson
	) throws JobExecutionAlreadyRunningException, JobRestartException, JobInstanceAlreadyCompleteException,
			JobParametersInvalidException, ProjectionRequestValidationException {

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

		try {
			Path batchRootDir = Paths.get(batchRootDirectory);

			if (!Files.exists(batchRootDir)) {
				Files.createDirectories(batchRootDir);
				logger.info("Created batch root directory: {}", batchRootDir);
			}

			String jobGuid = BatchUtils.createJobGuid();
			String jobTimestamp = BatchUtils.createJobTimestamp();

			String jobBaseFolderName = BatchUtils.createJobFolderName(BatchConstants.Job.BASE_FOLDER_PREFIX, jobGuid);
			Path jobBaseDir = batchRootDir.resolve(jobBaseFolderName);
			Files.createDirectories(jobBaseDir);
			logger.info("Created job base directory: {} (GUID: {})", jobBaseDir, jobGuid);

			Integer actualPartitionSize = partitionSize != null ? partitionSize : defaultParitionSize;
			logger.info(
					"[GUID: {}] Actual using {} partitions (requested: {}, from properties: {})", jobGuid,
					actualPartitionSize, partitionSize, defaultParitionSize
			);

			// Partition CSV files using streaming approach BEFORE starting the job
			logger.info("[GUID: {}] Starting CSV partitioning...", jobGuid);
			int featureIdToPartitionSize = csvPartitioner
					.partitionCsvFiles(polygonFile, layerFile, actualPartitionSize, jobBaseDir);

			logger.info(
					"[GUID: {}] CSV files partitioned successfully. Partitions: {}, Total FEATURE_IDs: {}", jobGuid,
					actualPartitionSize, featureIdToPartitionSize
			);

			// Now start the job with the partition directory included in parameters
			JobParameters jobParameters = buildJobParameters(
					projectionParametersJson, actualPartitionSize, jobGuid, jobTimestamp, jobBaseDir.toString()
			);
			JobExecution jobExecution = jobLauncher.run(partitionedJob, jobParameters);

			logger.info(
					"[GUID: {}] Started job! Execution ID: {}, Directory: {}, Partitions: {}", jobGuid,
					jobExecution.getId(), jobBaseDir, actualPartitionSize
			);

			return jobExecution;

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

	private JobParameters buildJobParameters(
			String projectionParametersJson, Integer partitionSize, String jobGuid, String jobTimestamp,
			String jobBaseDir
	) {

		JobParametersBuilder parametersBuilder = new JobParametersBuilder().addString(BatchConstants.Job.GUID, jobGuid)
				.addString(BatchConstants.Projection.PARAMETERS_JSON, projectionParametersJson)
				.addString(BatchConstants.Job.TIMESTAMP, jobTimestamp)
				.addString(BatchConstants.Job.BASE_DIR, jobBaseDir)
				.addLong(BatchConstants.Partition.SIZE, partitionSize.longValue());

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

	private void buildJobNotAvailableResponse(Map<String, Object> response) {
		response.put(BatchConstants.Job.MESSAGE, "VDYP Batch job not available");
		response.put(BatchConstants.Common.TIMESTAMP, System.currentTimeMillis());
		response.put(BatchConstants.Job.STATUS, "JOB_NOT_AVAILABLE");
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
			List<JobInstance> jobInstances = jobExplorer.getJobInstances(jobName, 0, 1000);

			for (JobInstance jobInstance : jobInstances) {
				List<JobExecution> jobExecutions = jobExplorer.getJobExecutions(jobInstance);

				for (JobExecution execution : jobExecutions) {
					String executionGuid = execution.getJobParameters().getString(BatchConstants.Job.GUID);
					if (jobGuid.equals(executionGuid)) {
						return execution;
					}
				}
			}
		}

		throw new NoSuchJobExecutionException("No job execution found with GUID: " + jobGuid);
	}
}
