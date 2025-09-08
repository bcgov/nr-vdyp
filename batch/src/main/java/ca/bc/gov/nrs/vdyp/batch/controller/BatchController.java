package ca.bc.gov.nrs.vdyp.batch.controller;

import ca.bc.gov.nrs.vdyp.batch.model.BatchMetrics;
import ca.bc.gov.nrs.vdyp.batch.service.BatchMetricsCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/batch")
public class BatchController {

	private static final Logger logger = LoggerFactory.getLogger(BatchController.class);

	@Autowired
	private JobLauncher jobLauncher;

	@Autowired(required = false)
	private Job partitionedJob;

	@Autowired
	private JobExplorer jobExplorer;

	@Autowired
	private BatchMetricsCollector metricsCollector;

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

			logger.info("=== VDYP Batch Job Start Request ===");
			if (request != null) {
				logger.info("Request details: {}", request.toString());
			}

			Map<String, Object> response = new HashMap<>();

			if (partitionedJob != null) {
				JobParametersBuilder parametersBuilder = new JobParametersBuilder().addLong("timestamp", startTime)
						.addString("jobType", "vdyp-projection");

				// Add request parameters to job parameters
				if (request != null) {
					if (request.getInputFilePath() != null) {
						parametersBuilder.addString("inputFilePath", request.getInputFilePath());
					}
					if (request.getOutputFilePath() != null) {
						parametersBuilder.addString("outputFilePath", request.getOutputFilePath());
					}
					if (request.getPartitionSize() != null) {
						parametersBuilder.addLong("partitionSize", request.getPartitionSize());
					}
					if (request.getMaxRetryAttempts() != null) {
						parametersBuilder.addLong("maxRetryAttempts", request.getMaxRetryAttempts().longValue());
					}
					if (request.getRetryBackoffPeriod() != null) {
						parametersBuilder.addLong("retryBackoffPeriod", request.getRetryBackoffPeriod());
					}
					if (request.getMaxSkipCount() != null) {
						parametersBuilder.addLong("maxSkipCount", request.getMaxSkipCount().longValue());
					}
				}

				JobParameters jobParameters = parametersBuilder.toJobParameters();
				JobExecution jobExecution = jobLauncher.run(partitionedJob, jobParameters);

				response.put("jobExecutionId", jobExecution.getId());
				response.put("jobName", jobExecution.getJobInstance().getJobName());
				response.put("status", jobExecution.getStatus().toString());
				response.put("startTime", jobExecution.getStartTime());
				response.put("message", "VDYP Batch job started successfully");
			} else {
				response.put("message", "VDYP Batch job not available - Job auto-creation is disabled");
				response.put("timestamp", startTime);
				response.put("status", "JOB_NOT_AVAILABLE");
				response.put("note", "Set 'batch.job.auto-create=true' to enable job creation");
			}

			return ResponseEntity.ok(response);

		} catch (Exception e) {
			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("error", "Failed to start batch job");
			errorResponse.put("message", e.getMessage());
			return ResponseEntity.internalServerError().body(errorResponse);
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
			response.put("jobExecutionId", jobExecution.getId());
			response.put("jobName", jobExecution.getJobInstance().getJobName());
			response.put("status", jobExecution.getStatus().toString());
			response.put("exitStatus", jobExecution.getExitStatus().getExitCode());
			response.put("startTime", jobExecution.getStartTime());
			response.put("endTime", jobExecution.getEndTime());

			return ResponseEntity.ok(response);

		} catch (Exception e) {
			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("error", "Failed to retrieve job status");
			errorResponse.put("message", e.getMessage());
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
				response.put("jobExecutionId", jobExecutionId);
				response.put("jobName", jobExecution.getJobInstance().getJobName());
				response.put("jobStatus", jobExecution.getStatus().toString());
				response.put("startTime", metrics.getStartTime());
				response.put("endTime", metrics.getEndTime());

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
				response.put("jobExecutionId", jobExecutionId);
				response.put("jobName", jobExecution.getJobInstance().getJobName());
				response.put("jobStatus", jobExecution.getStatus().toString());
				response.put("startTime", jobExecution.getStartTime());
				response.put("endTime", jobExecution.getEndTime());
				response.put("message", "Detailed metrics not available for this job");
			}

			return ResponseEntity.ok(response);

		} catch (Exception e) {
			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("error", "Failed to retrieve job metrics");
			errorResponse.put("message", e.getMessage());
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
			Map<String, Object> response = new HashMap<>();
			List<Map<String, Object>> jobsList = new ArrayList<>();

			int count = 0;
			for (String jobName : jobNames) {
				if (count >= limit)
					break;

				List<JobInstance> jobInstances = jobExplorer.getJobInstances(jobName, 0, limit - count);

				for (JobInstance jobInstance : jobInstances) {
					if (count >= limit)
						break;

					List<JobExecution> jobExecutions = jobExplorer.getJobExecutions(jobInstance);

					for (JobExecution jobExecution : jobExecutions) {
						if (count >= limit)
							break;

						Map<String, Object> jobInfo = new HashMap<>();
						jobInfo.put("jobExecutionId", jobExecution.getId());
						jobInfo.put("jobInstanceId", jobInstance.getId());
						jobInfo.put("jobName", jobName);
						jobInfo.put("status", jobExecution.getStatus().toString());
						jobInfo.put("exitStatus", jobExecution.getExitStatus().getExitCode());
						jobInfo.put("startTime", jobExecution.getStartTime());
						jobInfo.put("endTime", jobExecution.getEndTime());
						jobInfo.put("createTime", jobExecution.getCreateTime());

						// Add step summary
						List<Map<String, Object>> stepSummaries = jobExecution.getStepExecutions().stream()
								.map(stepExecution -> {
									Map<String, Object> stepInfo = new HashMap<>();
									stepInfo.put("stepName", stepExecution.getStepName());
									stepInfo.put("status", stepExecution.getStatus().toString());
									stepInfo.put("readCount", stepExecution.getReadCount());
									stepInfo.put("writeCount", stepExecution.getWriteCount());
									stepInfo.put("skipCount", stepExecution.getSkipCount());
									return stepInfo;
								}).collect(Collectors.toList());
						jobInfo.put("stepSummaries", stepSummaries);

						jobsList.add(jobInfo);
						count++;
					}
				}
			}

			response.put("jobs", jobsList);
			response.put("totalCount", count);
			response.put("limit", limit);
			response.put("timestamp", System.currentTimeMillis());

			return ResponseEntity.ok(response);

		} catch (Exception e) {
			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("error", "Failed to retrieve job list");
			errorResponse.put("message", e.getMessage());
			return ResponseEntity.internalServerError().body(errorResponse);
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
			Map<String, Object> response = new HashMap<>();
			List<String> jobNames = jobExplorer.getJobNames();

			// Overall job statistics
			int totalJobs = 0;
			int completedJobs = 0;
			int failedJobs = 0;
			int runningJobs = 0;
			long totalRecordsProcessed = 0L;
			long totalRetryAttempts = 0L;
			long totalSkippedRecords = 0L;

			for (String jobName : jobNames) {
				List<JobInstance> jobInstances = jobExplorer.getJobInstances(jobName, 0, 1000); // reasonable limit

				for (JobInstance jobInstance : jobInstances) {
					List<JobExecution> jobExecutions = jobExplorer.getJobExecutions(jobInstance);

					for (JobExecution jobExecution : jobExecutions) {
						totalJobs++;

						BatchStatus status = jobExecution.getStatus();
						if (status == BatchStatus.COMPLETED) {
							completedJobs++;
						} else if (status == BatchStatus.FAILED) {
							failedJobs++;
						} else if (status == BatchStatus.STARTED || status == BatchStatus.STARTING) {
							runningJobs++;
						}

						// Aggregate step statistics
						for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
							totalRecordsProcessed += stepExecution.getWriteCount();
							totalSkippedRecords += stepExecution.getSkipCount();
						}

						// Get metrics if available
						BatchMetrics metrics = metricsCollector.getJobMetrics(jobExecution.getId());
						if (metrics != null) {
							totalRetryAttempts += metrics.getTotalRetryAttempts();
						}
					}
				}
			}

			// System statistics
			response.put(
					"systemOverview",
					Map.of(
							"totalJobs", totalJobs, "completedJobs", completedJobs, "failedJobs", failedJobs,
							"runningJobs", runningJobs, "successRate",
							totalJobs > 0 ? (double) completedJobs / totalJobs * 100 : 0.0
					)
			);

			// Processing statistics
			response.put(
					"processingStatistics",
					Map.of(
							"totalRecordsProcessed", totalRecordsProcessed, "totalRetryAttempts", totalRetryAttempts,
							"totalSkippedRecords", totalSkippedRecords, "averageRecordsPerJob",
							totalJobs > 0 ? totalRecordsProcessed / totalJobs : 0
					)
			);

			// Recent job names for reference
			response.put("availableJobTypes", jobNames);
			response.put("timestamp", System.currentTimeMillis());

			// Add service capabilities
			response.put(
					"serviceCapabilities",
					Map.of(
							"partitioningEnabled", true, "retryPolicyEnabled", true, "skipPolicyEnabled", true,
							"metricsCollectionEnabled", true, "vdypIntegrationReady", false, "nativeImageSupport", true
					)
			);

			return ResponseEntity.ok(response);

		} catch (Exception e) {
			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("error", "Failed to retrieve batch statistics");
			errorResponse.put("message", e.getMessage());
			return ResponseEntity.internalServerError().body(errorResponse);
		}
	}

	/**
	 * Service health check endpoint for monitoring and load balancer integration.
	 *
	 * @return ResponseEntity containing service health status and feature list
	 */
	@GetMapping("/health")
	public ResponseEntity<Map<String, Object>> health() {
		Map<String, Object> response = new HashMap<>();
		response.put("status", "UP");
		response.put("service", "VDYP Batch Processing Service");
		response.put(
				"availableEndpoints",
				Arrays.asList(
						"/api/batch/start", "/api/batch/status/{id}", "/api/batch/metrics/{id}", "/api/batch/jobs",
						"/api/batch/statistics", "/api/batch/health"
				)
		);
		response.put("timestamp", System.currentTimeMillis());
		return ResponseEntity.ok(response);
	}
}