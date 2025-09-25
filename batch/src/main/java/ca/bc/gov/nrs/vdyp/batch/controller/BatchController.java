package ca.bc.gov.nrs.vdyp.batch.controller;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import ca.bc.gov.nrs.vdyp.batch.model.BatchMetrics;
import ca.bc.gov.nrs.vdyp.batch.service.BatchMetricsCollector;
import ca.bc.gov.nrs.vdyp.batch.service.StreamingCsvPartitioner;
import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.ProjectionRequestValidationException;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.ValidationMessage;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.ValidationMessageKind;

@RestController
@RequestMapping("/api/batch")
public class BatchController {

	private static final Logger logger = LoggerFactory.getLogger(BatchController.class);
	private static final String JOB_EXECUTION_ID = "jobExecutionId";
	private static final String JOB_NAME = "jobName";
	private static final String JOB_STATUS = "status";
	private static final String JOB_MESSAGE = "message";
	private static final String JOB_ERROR = "error";
	private static final String JOB_START_TIME = "startTime";
	private static final String JOB_END_TIME = "endTime";
	private static final String JOB_TIMESTAMP = "timestamp";
	private static final String JOB_EXIT_STATUS = "exitStatus";
	private static final String JOB_TYPE = "jobType";
	private static final String NOTE = "note";

	private final JobLauncher jobLauncher;
	private final Job partitionedJob;
	private final JobExplorer jobExplorer;
	private final BatchMetricsCollector metricsCollector;
	private final StreamingCsvPartitioner csvPartitioner;

	@Value("${batch.input.directory.default-path}")
	private String inputBasePath;

	@Value("${batch.output.directory.default-path}")
	private String outputBasePath;

	public BatchController(
			JobLauncher jobLauncher, Job partitionedJob, JobExplorer jobExplorer,
			BatchMetricsCollector metricsCollector, StreamingCsvPartitioner csvPartitioner) {
		this.jobLauncher = jobLauncher;
		this.partitionedJob = partitionedJob;
		this.jobExplorer = jobExplorer;
		this.metricsCollector = metricsCollector;
		this.csvPartitioner = csvPartitioner;
	}

	/**
	 * Start a new batch job execution with uploaded CSV files.
	 *
	 * @param polygonFile    CSV file containing polygon data
	 * @param layerFile      CSV file containing layer data
	 * @param partitionSize  Number of partitions (optional, default from config)
	 * @param parametersJson JSON string containing VDYP projection parameters
	 * @return ResponseEntity containing job execution details and metrics endpoint
	 */
	@PostMapping(value = "/start", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> startBatchJobWithFiles(
			@RequestParam("polygonFile") MultipartFile polygonFile,
			@RequestParam("layerFile") MultipartFile layerFile,
			@RequestParam(value = "partitionSize", required = false) Long partitionSize,
			@RequestParam("parameters") String parametersJson) {

		try {
			long startTime = System.currentTimeMillis();
			logRequestDetails(polygonFile, layerFile, partitionSize, parametersJson);

			Map<String, Object> response = new HashMap<>();

			if (partitionedJob != null) {
				JobExecution jobExecution = executeJob(polygonFile, layerFile, partitionSize, parametersJson,
						startTime);
				buildSuccessResponse(response, jobExecution);
			} else {
				buildJobNotAvailableResponse(response, startTime);
			}

			return ResponseEntity.ok(response);

		} catch (ProjectionRequestValidationException e) {
			return ResponseEntity.badRequest()
					.header("content-type", "application/json")
					.body(createValidationErrorResponse(e));
		} catch (Exception e) {
			return buildErrorResponse(e);
		}
	}

	/**
	 * Get current job execution status with step-level details.
	 *
	 * @param jobExecutionId The unique identifier of the job execution
	 * @return ResponseEntity containing job status and step details or 404 if not
	 *         found
	 */
	@GetMapping(value = "/status/{jobExecutionId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> getJobStatus(@PathVariable Long jobExecutionId) {
		try {
			JobExecution jobExecution = jobExplorer.getJobExecution(jobExecutionId);

			if (jobExecution == null) {
				return ResponseEntity.notFound().build();
			}

			Map<String, Object> response = new HashMap<>();
			response.put(JOB_EXECUTION_ID, jobExecution.getId());
			response.put(JOB_NAME, jobExecution.getJobInstance().getJobName());
			response.put(JOB_STATUS, jobExecution.getStatus().toString());
			response.put(JOB_EXIT_STATUS, jobExecution.getExitStatus().getExitCode());
			response.put(JOB_START_TIME, jobExecution.getStartTime());
			response.put(JOB_END_TIME, jobExecution.getEndTime());

			return ResponseEntity.ok(response);

		} catch (Exception e) {
			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put(JOB_ERROR, "Failed to retrieve job status");
			errorResponse.put(JOB_MESSAGE, e.getMessage());
			return ResponseEntity.internalServerError().body(errorResponse);
		}
	}

	/**
	 * Get detailed job metrics including partition-level data, retry/skip
	 * statistics.
	 *
	 * @param jobExecutionId The unique identifier of the job execution
	 * @return ResponseEntity containing job metrics or 404 if not found
	 */
	@GetMapping(value = "/metrics/{jobExecutionId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> getJobMetrics(@PathVariable Long jobExecutionId) {
		try {
			JobExecution jobExecution = jobExplorer.getJobExecution(jobExecutionId);

			if (jobExecution == null) {
				return ResponseEntity.notFound().build();
			}

			BatchMetrics metrics = metricsCollector.getJobMetrics(jobExecutionId);
			Map<String, Object> response = new HashMap<>();

			if (metrics != null) {
				response.put(JOB_EXECUTION_ID, jobExecutionId);
				response.put(JOB_NAME, jobExecution.getJobInstance().getJobName());
				response.put(JOB_STATUS, jobExecution.getStatus().toString());
				response.put(JOB_START_TIME, metrics.getStartTime());
				response.put(JOB_END_TIME, metrics.getEndTime());

				// Calculate duration if both start and end times are available
				if (metrics.getStartTime() != null && metrics.getEndTime() != null) {
					long durationSeconds = Duration.between(metrics.getStartTime(), metrics.getEndTime()).getSeconds();
					response.put("duration", durationSeconds + " seconds");
				} else {
					response.put("duration", "Job still running");
				}

				response.put("totalRecordsRead", metrics.getTotalRecordsRead());
				response.put("totalRecordsWritten", metrics.getTotalRecordsWritten());
				response.put("totalRecordsSkipped", metrics.getTotalSkips());
				response.put("totalRetryAttempts", metrics.getTotalRetryAttempts());
				response.put("partitionMetrics", metrics.getPartitionMetrics());
				response.put("retryEvents", metrics.getRetryDetails());
				response.put("skipEvents", metrics.getSkipDetails());
			} else {
				// Fallback to basic job execution data if detailed metrics not available
				response.put(JOB_EXECUTION_ID, jobExecutionId);
				response.put(JOB_NAME, jobExecution.getJobInstance().getJobName());
				response.put(JOB_STATUS, jobExecution.getStatus().toString());
				response.put(JOB_START_TIME, jobExecution.getStartTime());
				response.put(JOB_END_TIME, jobExecution.getEndTime());
				response.put(JOB_MESSAGE, "Detailed metrics not available for this job");
			}

			return ResponseEntity.ok(response);

		} catch (Exception e) {
			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put(JOB_ERROR, "Failed to retrieve job metrics");
			errorResponse.put(JOB_MESSAGE, e.getMessage());
			return ResponseEntity.internalServerError().body(errorResponse);
		}
	}

	/**
	 * List all batch jobs with basic information and status.
	 *
	 * @param limit Optional limit for number of jobs to return (default: 50)
	 * @return ResponseEntity containing list of job instances and executions
	 */
	@GetMapping(value = "/jobs", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> listJobs(@RequestParam(defaultValue = "50") int limit) {
		try {
			List<String> jobNames = jobExplorer.getJobNames();
			List<Map<String, Object>> jobsList = collectJobsList(jobNames, limit);
			Map<String, Object> response = buildJobsListResponse(jobsList, limit);
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put(JOB_ERROR, "Failed to retrieve job list");
			errorResponse.put(JOB_MESSAGE, e.getMessage());
			return ResponseEntity.internalServerError().body(errorResponse);
		}
	}

	/**
	 * Service health check endpoint for monitoring and load balancer integration.
	 *
	 * @return ResponseEntity containing service health status and feature list
	 */
	@GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> health() {
		Map<String, Object> response = new HashMap<>();
		response.put(JOB_STATUS, "UP");
		response.put("service", "VDYP Batch Processing Service");
		response.put(
				"availableEndpoints",
				Arrays.asList(
						"/api/batch/start", "/api/batch/status/{id}", "/api/batch/metrics/{id}", "/api/batch/jobs",
						"/api/batch/health"));
		response.put(JOB_TIMESTAMP, System.currentTimeMillis());
		return ResponseEntity.ok(response);
	}

	private void logRequestDetails(MultipartFile polygonFile, MultipartFile layerFile,
			Long partitionSize, String parametersJson) {
		logger.info("=== VDYP Batch Job File Upload Request ===");
		logger.info("Polygon file: {} ({} bytes)", polygonFile.getOriginalFilename(), polygonFile.getSize());
		logger.info("Layer file: {} ({} bytes)", layerFile.getOriginalFilename(), layerFile.getSize());
		logger.info("Partition size: {}", partitionSize);
		logger.info("Parameters provided: {}", parametersJson != null ? "yes" : "no");
	}

	private JobExecution executeJob(MultipartFile polygonFile, MultipartFile layerFile,
			Long partitionSize, String parametersJson, long startTime)
			throws JobExecutionAlreadyRunningException, JobRestartException,
			JobInstanceAlreadyCompleteException, JobParametersInvalidException, ProjectionRequestValidationException {

		// Validate parameters
		if (parametersJson == null || parametersJson.trim().isEmpty()) {
			throw new ProjectionRequestValidationException(List.of(
					new ValidationMessage(ValidationMessageKind.GENERIC,
							"VDYP projection parameters are required but not provided in the request")));
		}

		try {
			// Debug logging
			logger.info("Processing files: polygon={}, layer={}, partitionSize={}",
					polygonFile.getOriginalFilename(), layerFile.getOriginalFilename(), partitionSize);
			logger.info("Parameters JSON length: {}", parametersJson.length());
			logger.debug("Parameters JSON content: {}", parametersJson);

			// Create partition directory for input CSV files in configured input directory
			Path baseInputDir = Paths.get(inputBasePath);
			Path partitionDir = baseInputDir.resolve("vdyp-batch-" + startTime);
			logger.info("Using input partition directory: {}", partitionDir);
			logger.info("Output results will be stored in: {}", outputBasePath);

			// Partition CSV files using streaming approach
			logger.info("Starting CSV partitioning...");
			StreamingCsvPartitioner.PartitionResult partitionResult = csvPartitioner.partitionCsvFiles(
					polygonFile, layerFile,
					partitionSize != null ? partitionSize.intValue() : 4, // default grid size
					partitionDir);

			logger.info("CSV files partitioned successfully. Partitions: {}, Total FEATURE_IDs: {}",
					partitionResult.getGridSize(), partitionResult.getTotalFeatureIds());

			// Build job parameters
			JobParameters jobParameters = buildJobParameters(partitionResult, parametersJson, startTime, partitionSize);

			// Start the job
			JobExecution jobExecution = jobLauncher.run(partitionedJob, jobParameters);

			logger.info("Started VDYP batch job {} with uploaded files - Polygons: {}, Layers: {}",
					jobExecution.getId(), polygonFile.getOriginalFilename(), layerFile.getOriginalFilename());

			return jobExecution;

		} catch (Exception e) {
			logger.error("Failed to process uploaded CSV files", e);

			String errorMessage = e.getMessage() != null ? e.getMessage()
					: "Unknown error (" + e.getClass().getSimpleName() + ")";

			throw new ProjectionRequestValidationException(List.of(
					new ValidationMessage(ValidationMessageKind.GENERIC,
							"Failed to process uploaded CSV files: " + errorMessage)));
		}
	}

	private JobParameters buildJobParameters(StreamingCsvPartitioner.PartitionResult partitionResult,
			String parametersJson, long startTime, Long partitionSize) {

		JobParametersBuilder parametersBuilder = new JobParametersBuilder()
				.addLong(JOB_TIMESTAMP, startTime)
				.addString(JOB_TYPE, "vdyp-projection-files")
				.addString("partitionBaseDir", partitionResult.getBaseOutputDir().toString())
				.addLong("gridSize", (long) partitionResult.getGridSize())
				.addString("projectionParametersJson", parametersJson);

		// Add partitionSize if provided
		if (partitionSize != null) {
			parametersBuilder.addLong("partitionSize", partitionSize);
		}

		return parametersBuilder.toJobParameters();
	}

	private void buildSuccessResponse(Map<String, Object> response, JobExecution jobExecution) {
		response.put(JOB_EXECUTION_ID, jobExecution.getId());
		response.put(JOB_NAME, jobExecution.getJobInstance().getJobName());
		response.put(JOB_STATUS, jobExecution.getStatus().toString());
		response.put(JOB_START_TIME, jobExecution.getStartTime());
		response.put(JOB_MESSAGE, "VDYP Batch job started successfully");
	}

	private void buildJobNotAvailableResponse(Map<String, Object> response, long startTime) {
		response.put(JOB_MESSAGE, "VDYP Batch job not available - Job auto-creation is disabled");
		response.put(JOB_TIMESTAMP, startTime);
		response.put(JOB_STATUS, "JOB_NOT_AVAILABLE");
		response.put(NOTE, "Set 'batch.job.auto-create=true' to enable job creation");
	}

	private Map<String, Object> createValidationErrorResponse(ProjectionRequestValidationException e) {
		Map<String, Object> errorResponse = new HashMap<>();
		errorResponse.put("validationMessages", e.getValidationMessages());
		errorResponse.put(JOB_ERROR, "Validation failed");
		errorResponse.put(JOB_MESSAGE, "Request validation failed - check validation messages for details");
		return errorResponse;
	}

	private ResponseEntity<Map<String, Object>> buildErrorResponse(Exception e) {
		Map<String, Object> errorResponse = new HashMap<>();
		errorResponse.put(JOB_ERROR, "Failed to start batch job");
		errorResponse.put(JOB_MESSAGE, e.getMessage() == null ? "unknown reason" : e.getMessage());
		return ResponseEntity.internalServerError().body(errorResponse);
	}

	private List<Map<String, Object>> collectJobsList(List<String> jobNames, int limit) {
		List<Map<String, Object>> jobsList = new ArrayList<>();
		JobsCollector collector = new JobsCollector(jobsList, limit);

		for (String jobName : jobNames) {
			if (collector.isLimitReached()) {
				break;
			}
			processJobName(jobName, collector);
		}

		return jobsList;
	}

	private void processJobName(String jobName, JobsCollector collector) {
		List<JobInstance> jobInstances = jobExplorer.getJobInstances(jobName, 0, collector.getRemainingLimit());

		for (JobInstance jobInstance : jobInstances) {
			if (collector.isLimitReached()) {
				break;
			}
			processJobInstance(jobName, jobInstance, collector);
		}
	}

	private void processJobInstance(String jobName, JobInstance jobInstance, JobsCollector collector) {
		List<JobExecution> jobExecutions = jobExplorer.getJobExecutions(jobInstance);

		for (JobExecution jobExecution : jobExecutions) {
			if (collector.isLimitReached()) {
				break;
			}
			Map<String, Object> jobInfo = createJobInfo(jobName, jobInstance, jobExecution);
			collector.addJob(jobInfo);
		}
	}

	private Map<String, Object> createJobInfo(String jobName, JobInstance jobInstance, JobExecution jobExecution) {
		Map<String, Object> jobInfo = new HashMap<>();
		jobInfo.put(JOB_EXECUTION_ID, jobExecution.getId());
		jobInfo.put("jobInstanceId", jobInstance.getId());
		jobInfo.put(JOB_NAME, jobName);
		jobInfo.put(JOB_STATUS, jobExecution.getStatus().toString());
		jobInfo.put(JOB_EXIT_STATUS, jobExecution.getExitStatus().getExitCode());
		jobInfo.put(JOB_START_TIME, jobExecution.getStartTime());
		jobInfo.put(JOB_END_TIME, jobExecution.getEndTime());
		jobInfo.put("createTime", jobExecution.getCreateTime());
		jobInfo.put("stepSummaries", createStepSummaries(jobExecution));
		return jobInfo;
	}

	private List<Map<String, Object>> createStepSummaries(JobExecution jobExecution) {
		return jobExecution.getStepExecutions().stream().map(stepExecution -> {
			Map<String, Object> stepInfo = new HashMap<>();
			stepInfo.put("stepName", stepExecution.getStepName());
			stepInfo.put(JOB_STATUS, stepExecution.getStatus().toString());
			stepInfo.put("readCount", stepExecution.getReadCount());
			stepInfo.put("writeCount", stepExecution.getWriteCount());
			stepInfo.put("skipCount", stepExecution.getSkipCount());
			return stepInfo;
		}).toList();
	}

	private Map<String, Object> buildJobsListResponse(List<Map<String, Object>> jobsList, int limit) {
		Map<String, Object> response = new HashMap<>();
		response.put("jobs", jobsList);
		response.put("totalCount", jobsList.size());
		response.put("limit", limit);
		response.put(JOB_TIMESTAMP, System.currentTimeMillis());
		return response;
	}

	private static class JobsCollector {
		private final List<Map<String, Object>> jobsList;
		private final int limit;
		private int count = 0;

		public JobsCollector(List<Map<String, Object>> jobsList, int limit) {
			this.jobsList = jobsList;
			this.limit = limit;
		}

		public boolean isLimitReached() {
			return count >= limit;
		}

		public int getRemainingLimit() {
			return limit - count;
		}

		public void addJob(Map<String, Object> jobInfo) {
			if (!isLimitReached()) {
				jobsList.add(jobInfo);
				count++;
			}
		}
	}
}
