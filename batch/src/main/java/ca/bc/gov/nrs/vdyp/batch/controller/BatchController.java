package ca.bc.gov.nrs.vdyp.batch.controller;

import ca.bc.gov.nrs.vdyp.batch.model.BatchMetrics;
import ca.bc.gov.nrs.vdyp.batch.service.BatchMetricsCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.*;

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

	public BatchController(JobLauncher jobLauncher, Job partitionedJob, JobExplorer jobExplorer, BatchMetricsCollector metricsCollector) {
		this.jobLauncher = jobLauncher;
		this.partitionedJob = partitionedJob;
		this.jobExplorer = jobExplorer;
		this.metricsCollector = metricsCollector;
	}

	/**
	 * Start a new batch job execution with configuration options.
	 *
	 * @param request Optional configuration parameters for the batch job
	 * @return ResponseEntity containing job execution details and metrics endpoint
	 */
	@PostMapping("/start")
	public ResponseEntity<Map<String, Object>> startBatchJob(@RequestBody(required = false) BatchJobRequest request) {
		try {
			long startTime = System.currentTimeMillis();
			logRequestDetails(request);

			Map<String, Object> response = new HashMap<>();

			if (partitionedJob != null) {
				JobExecution jobExecution = executeJob(request, startTime);
				buildSuccessResponse(response, jobExecution);
			} else {
				buildJobNotAvailableResponse(response, startTime);
			}

			return ResponseEntity.ok(response);

		} catch (Exception e) {
			return buildErrorResponse(e);
		}
	}

	/**
	 * Get current job execution status with step-level details.
	 *
	 * @param jobExecutionId The unique identifier of the job execution
	 * @return ResponseEntity containing job status and step details or 404 if not found
	 */
	@GetMapping("/status/{jobExecutionId}")
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
	 * Get detailed job metrics including partition-level data, retry/skip statistics.
	 *
	 * @param jobExecutionId The unique identifier of the job execution
	 * @return ResponseEntity containing job metrics or 404 if not found
	 */
	@GetMapping("/metrics/{jobExecutionId}")
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
	@GetMapping("/jobs")
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

	/**
	 * Get aggregated batch processing statistics and system overview.
	 *
	 * @return ResponseEntity containing overall system statistics and metrics
	 */
	@GetMapping("/statistics")
	public ResponseEntity<Map<String, Object>> getBatchStatistics() {
		try {
			List<String> jobNames = jobExplorer.getJobNames();
			BatchStatistics statistics = collectBatchStatistics(jobNames);
			Map<String, Object> response = buildStatisticsResponse(statistics, jobNames);
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put(JOB_ERROR, "Failed to retrieve batch statistics");
			errorResponse.put(JOB_MESSAGE, e.getMessage());
			return ResponseEntity.internalServerError().body(errorResponse);
		}
	}

	private BatchStatistics collectBatchStatistics(List<String> jobNames) {
		BatchStatistics statistics = new BatchStatistics();

		for (String jobName : jobNames) {
			List<JobInstance> jobInstances = jobExplorer.getJobInstances(jobName, 0, 1000);
			for (JobInstance jobInstance : jobInstances) {
				List<JobExecution> jobExecutions = jobExplorer.getJobExecutions(jobInstance);
				for (JobExecution jobExecution : jobExecutions) {
					processJobExecution(jobExecution, statistics);
				}
			}
		}

		return statistics;
	}

	private void processJobExecution(JobExecution jobExecution, BatchStatistics statistics) {
		statistics.totalJobs++;

		BatchStatus status = jobExecution.getStatus();
		if (status == BatchStatus.COMPLETED) {
			statistics.completedJobs++;
		} else if (status == BatchStatus.FAILED) {
			statistics.failedJobs++;
		} else if (status == BatchStatus.STARTED || status == BatchStatus.STARTING) {
			statistics.runningJobs++;
		}

		processStepExecutions(jobExecution, statistics);
		processJobMetrics(jobExecution, statistics);
	}

	private void processStepExecutions(JobExecution jobExecution, BatchStatistics statistics) {
		for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
			statistics.totalRecordsProcessed += stepExecution.getWriteCount();
			statistics.totalSkippedRecords += stepExecution.getSkipCount();
		}
	}

	private void processJobMetrics(JobExecution jobExecution, BatchStatistics statistics) {
		BatchMetrics metrics = metricsCollector.getJobMetrics(jobExecution.getId());
		if (metrics != null) {
			statistics.totalRetryAttempts += metrics.getTotalRetryAttempts();
		}
	}

	private Map<String, Object> buildStatisticsResponse(BatchStatistics statistics, List<String> jobNames) {
		Map<String, Object> response = new HashMap<>();

		response.put("systemOverview", buildSystemOverview(statistics));
		response.put("processingStatistics", buildProcessingStatistics(statistics));
		response.put("availableJobTypes", jobNames);
		response.put(JOB_TIMESTAMP, System.currentTimeMillis());
		response.put("serviceCapabilities", buildServiceCapabilities());

		return response;
	}

	private Map<String, Object> buildSystemOverview(BatchStatistics statistics) {
		return Map.of(
				"totalJobs", statistics.totalJobs, "completedJobs", statistics.completedJobs, "failedJobs",
				statistics.failedJobs, "runningJobs", statistics.runningJobs, "successRate",
				statistics.totalJobs > 0 ? (double) statistics.completedJobs / statistics.totalJobs * 100 : 0.0
		);
	}

	private Map<String, Object> buildProcessingStatistics(BatchStatistics statistics) {
		return Map.of(
				"totalRecordsProcessed", statistics.totalRecordsProcessed, "totalRetryAttempts",
				statistics.totalRetryAttempts, "totalSkippedRecords", statistics.totalSkippedRecords,
				"averageRecordsPerJob",
				statistics.totalJobs > 0 ? statistics.totalRecordsProcessed / statistics.totalJobs : 0
		);
	}

	private Map<String, Object> buildServiceCapabilities() {
		return Map.of(
				"partitioningEnabled", true, "retryPolicyEnabled", true, "skipPolicyEnabled", true,
				"metricsCollectionEnabled", true, "vdypIntegrationReady", false, "nativeImageSupport", true
		);
	}

	/**
	 * Logs request details with sanitized paths.
	 */
	private void logRequestDetails(BatchJobRequest request) {
		logger.info("=== VDYP Batch Job Start Request ===");
		if (request != null) {
			String sanitizedInputPath = sanitizePathForLogging(request.getInputFilePath());
			String sanitizedOutputPath = sanitizePathForLogging(request.getOutputFilePath());
			logger.info(
					"Request details - inputFilePath: {}, outputFilePath: {}, partitionSize: {}, maxRetryAttempts: {}, retryBackoffPeriod: {}, maxSkipCount: {}",
					sanitizedInputPath, sanitizedOutputPath, request.getPartitionSize(), request.getMaxRetryAttempts(),
					request.getRetryBackoffPeriod(), request.getMaxSkipCount()
			);
		}
	}

	/**
	 * Sanitizes file paths for safe logging.
	 */
	private String sanitizePathForLogging(String path) {
		return path != null ? path.replaceAll("[\n\r]", "_") : null;
	}

	/**
	 * Executes the batch job with given parameters.
	 */
	private JobExecution executeJob(BatchJobRequest request, long startTime) throws JobExecutionAlreadyRunningException,
			JobRestartException, JobInstanceAlreadyCompleteException, JobParametersInvalidException {
		JobParameters jobParameters = buildJobParameters(request, startTime);
		return jobLauncher.run(partitionedJob, jobParameters);
	}

	/**
	 * Builds job parameters from request and start time.
	 */
	private JobParameters buildJobParameters(BatchJobRequest request, long startTime) {
		JobParametersBuilder parametersBuilder = new JobParametersBuilder().addLong(JOB_TIMESTAMP, startTime)
				.addString(JOB_TYPE, "vdyp-projection");

		if (request != null) {
			addRequestParametersToBuilder(parametersBuilder, request);
		}

		return parametersBuilder.toJobParameters();
	}

	/**
	 * Adds request parameters to the job parameters builder.
	 */
	private void addRequestParametersToBuilder(JobParametersBuilder builder, BatchJobRequest request) {
		if (request.getInputFilePath() != null) {
			builder.addString("inputFilePath", request.getInputFilePath());
		}
		if (request.getOutputFilePath() != null) {
			builder.addString("outputFilePath", request.getOutputFilePath());
		}
		if (request.getPartitionSize() != null) {
			builder.addLong("partitionSize", request.getPartitionSize());
		}
		if (request.getMaxRetryAttempts() != null) {
			builder.addLong("maxRetryAttempts", request.getMaxRetryAttempts().longValue());
		}
		if (request.getRetryBackoffPeriod() != null) {
			builder.addLong("retryBackoffPeriod", request.getRetryBackoffPeriod());
		}
		if (request.getMaxSkipCount() != null) {
			builder.addLong("maxSkipCount", request.getMaxSkipCount().longValue());
		}
	}

	/**
	 * Builds successful job execution response.
	 */
	private void buildSuccessResponse(Map<String, Object> response, JobExecution jobExecution) {
		response.put(JOB_EXECUTION_ID, jobExecution.getId());
		response.put(JOB_NAME, jobExecution.getJobInstance().getJobName());
		response.put(JOB_STATUS, jobExecution.getStatus().toString());
		response.put(JOB_START_TIME, jobExecution.getStartTime());
		response.put(JOB_MESSAGE, "VDYP Batch job started successfully");
	}

	/**
	 * Builds response when job is not available.
	 */
	private void buildJobNotAvailableResponse(Map<String, Object> response, long startTime) {
		response.put(JOB_MESSAGE, "VDYP Batch job not available - Job auto-creation is disabled");
		response.put(JOB_TIMESTAMP, startTime);
		response.put(JOB_STATUS, "JOB_NOT_AVAILABLE");
		response.put(NOTE, "Set 'batch.job.auto-create=true' to enable job creation");
	}

	/**
	 * Builds error response for exceptions.
	 */
	private ResponseEntity<Map<String, Object>> buildErrorResponse(Exception e) {
		Map<String, Object> errorResponse = new HashMap<>();
		errorResponse.put(JOB_ERROR, "Failed to start batch job");
		errorResponse.put(JOB_MESSAGE, e.getMessage());
		return ResponseEntity.internalServerError().body(errorResponse);
	}

	private static class BatchStatistics {
		int totalJobs = 0;
		int completedJobs = 0;
		int failedJobs = 0;
		int runningJobs = 0;
		long totalRecordsProcessed = 0L;
		long totalRetryAttempts = 0L;
		long totalSkippedRecords = 0L;
	}

	/**
	 * Service health check endpoint for monitoring and load balancer integration.
	 *
	 * @return ResponseEntity containing service health status and feature list
	 */
	@GetMapping("/health")
	public ResponseEntity<Map<String, Object>> health() {
		Map<String, Object> response = new HashMap<>();
		response.put(JOB_STATUS, "UP");
		response.put("service", "VDYP Batch Processing Service");
		response.put(
				"availableEndpoints",
				Arrays.asList(
						"/api/batch/start", "/api/batch/status/{id}", "/api/batch/metrics/{id}", "/api/batch/jobs",
						"/api/batch/statistics", "/api/batch/health"
				)
		);
		response.put(JOB_TIMESTAMP, System.currentTimeMillis());
		return ResponseEntity.ok(response);
	}
}