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
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

	@SuppressWarnings("unused")
	private final JobExplorer jobExplorer;
	@SuppressWarnings("unused")
	private final BatchMetricsCollector metricsCollector;

	@Value("${batch.root-directory}")
	private String batchRootDirectory;

	@Value("${batch.partition.default-partition-size}")
	private Integer defaultParitionSize;

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
	 * Start a new batch job execution
	 */
	@PostMapping(value = "/start", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> startBatchJobWithFiles(
			@RequestParam("polygonFile") MultipartFile polygonFile,
			@RequestParam("layerFile") MultipartFile layerFile,
			@RequestParam(value = "partitionSize", required = false) Integer partitionSize,
			@RequestParam("parameters") String projectionParametersJson) {

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
			return ResponseEntity.badRequest()
					.header("content-type", "application/json")
					.body(createValidationErrorResponse(e));
		} catch (Exception e) {
			return buildErrorResponse(e);
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
						"/api/batch/start", "/api/batch/status/{id}", "/api/batch/metrics/{id}", "/api/batch/jobs",
						"/api/batch/health"));
		response.put(BatchConstants.Common.TIMESTAMP, System.currentTimeMillis());
		return ResponseEntity.ok(response);
	}

	private void logRequestDetails(MultipartFile polygonFile, MultipartFile layerFile,
			Integer partitionSize, String parametersJson) {
		if (logger.isInfoEnabled()) {
			logger.info("=== VDYP Batch Job Request ===");
			logger.info("Polygon file: {} ({} bytes)", BatchUtils.sanitizeForLogging(polygonFile.getOriginalFilename()),
					polygonFile.getSize());
			logger.info("Layer file: {} ({} bytes)", BatchUtils.sanitizeForLogging(layerFile.getOriginalFilename()),
					layerFile.getSize());
			logger.info("Partition size: {}", partitionSize);
			logger.info("Parameters provided: {}", parametersJson != null ? "yes" : "no");
		}
	}

	private JobExecution executeJob(MultipartFile polygonFile, MultipartFile layerFile,
			Integer partitionSize, String projectionParametersJson)
			throws JobExecutionAlreadyRunningException, JobRestartException,
			JobInstanceAlreadyCompleteException, JobParametersInvalidException, ProjectionRequestValidationException {

		if (projectionParametersJson == null || projectionParametersJson.trim().isEmpty()) {
			throw new ProjectionRequestValidationException(List.of(
					new ValidationMessage(ValidationMessageKind.GENERIC,
							"VDYP projection parameters are required but not provided in the request")));
		}

		try {
			Path batchRootDir = Paths.get(batchRootDirectory);

			if (!Files.exists(batchRootDir)) {
				Files.createDirectories(batchRootDir);
				logger.info("Created batch root directory: {}", batchRootDir);
			}

			String jobTimestamp = BatchUtils.createJobTimestamp();

			String jobBaseFolderName = BatchUtils.createJobFolderName(BatchConstants.Job.BASE_FOLDER_PREFIX,
					jobTimestamp);
			Path jobBaseDir = batchRootDir.resolve(jobBaseFolderName);
			Files.createDirectories(jobBaseDir);
			logger.info("Created job base directory: {}", jobBaseDir);

			Integer actualPartitionSize = partitionSize != null ? partitionSize : defaultParitionSize;
			logger.info("Actual using {} partitions (requested: {}, from properties: {})",
					actualPartitionSize, partitionSize, defaultParitionSize);

			// Partition CSV files using streaming approach BEFORE starting the job
			logger.info("Starting CSV partitioning...");
			int featureIdToPartitionSize = csvPartitioner.partitionCsvFiles(
					polygonFile, layerFile,
					actualPartitionSize,
					jobBaseDir);

			logger.info("CSV files partitioned successfully. Partitions: {}, Total FEATURE_IDs: {}",
					actualPartitionSize, featureIdToPartitionSize);

			// Now start the job with the partition directory included in parameters
			JobParameters jobParameters = buildJobParameters(projectionParametersJson, actualPartitionSize,
					jobTimestamp, jobBaseDir.toString());
			JobExecution jobExecution = jobLauncher.run(partitionedJob, jobParameters);

			logger.info("Started job! execution ID: {}, directory: {}, partitions: {}", jobExecution.getId(),
					jobBaseDir,
					actualPartitionSize);

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

	private JobParameters buildJobParameters(String projectionParametersJson, Integer partitionSize,
			String jobTimestamp,
			String jobBaseDir) {

		JobParametersBuilder parametersBuilder = new JobParametersBuilder()
				.addString(BatchConstants.Projection.PARAMETERS_JSON, projectionParametersJson)
				.addString(BatchConstants.Job.TIMESTAMP, jobTimestamp)
				.addString(BatchConstants.Job.BASE_DIR, jobBaseDir)
				.addLong(BatchConstants.Partition.SIZE, partitionSize.longValue());

		return parametersBuilder.toJobParameters();
	}

	private void buildSuccessResponse(Map<String, Object> response, JobExecution jobExecution) {
		response.put(BatchConstants.Job.EXECUTION_ID, jobExecution.getId());
		response.put(BatchConstants.Job.NAME, jobExecution.getJobInstance().getJobName());
		response.put(BatchConstants.Job.STATUS, jobExecution.getStatus().toString());
		response.put(BatchConstants.Job.START_TIME, jobExecution.getStartTime());
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
		errorResponse.put(BatchConstants.Job.MESSAGE,
				"Request validation failed - check validation messages for details");
		return errorResponse;
	}

	private ResponseEntity<Map<String, Object>> buildErrorResponse(Exception e) {
		Map<String, Object> errorResponse = new HashMap<>();
		errorResponse.put(BatchConstants.Job.ERROR, "Failed to start batch job");
		errorResponse.put(BatchConstants.Job.MESSAGE, e.getMessage() == null ? "unknown reason" : e.getMessage());
		return ResponseEntity.internalServerError().body(errorResponse);
	}
}
