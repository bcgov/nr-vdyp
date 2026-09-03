package ca.bc.gov.nrs.vdyp.batch.controller;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobExecutionNotRunningException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ca.bc.gov.nrs.vdyp.batch.configuration.BatchOwnershipProperties;
import ca.bc.gov.nrs.vdyp.batch.messaging.message.PrioritizeReplyMessage;
import ca.bc.gov.nrs.vdyp.batch.ownership.JobOwnershipService;
import ca.bc.gov.nrs.vdyp.batch.service.BatchJobLaunchService;
import ca.bc.gov.nrs.vdyp.batch.service.BatchMetricsCollector;
import ca.bc.gov.nrs.vdyp.batch.service.BatchPrioritizationService;
import ca.bc.gov.nrs.vdyp.batch.service.BatchPrioritizationService.PrioritizeOutcome;
import ca.bc.gov.nrs.vdyp.batch.service.JobExecutionLookupService;
import ca.bc.gov.nrs.vdyp.batch.service.PrioritizeRemoteGateway;
import ca.bc.gov.nrs.vdyp.batch.service.ServerCapacityService;
import ca.bc.gov.nrs.vdyp.batch.service.StorageEstimationService;
import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;
import ca.bc.gov.nrs.vdyp.batch.util.BatchUtils;
import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.ProjectionRequestValidationException;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.ValidationMessage;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.ValidationMessageKind;

@RestController
@RequestMapping("/api/batch")
public class BatchController {

	private static final Logger logger = LoggerFactory.getLogger(BatchController.class);

	private final JobOperator jobOperator;
	@SuppressWarnings("unused")
	private final BatchMetricsCollector metricsCollector;
	private final StorageEstimationService storageEstimationService;
	private final BatchJobLaunchService batchJobLaunchService;
	private final ServerCapacityService serverCapacityService;
	private final BatchOwnershipProperties ownershipProperties;
	private final JobOwnershipService ownershipService;
	private final JobExecutionLookupService lookupService;
	private final BatchPrioritizationService prioritizationService;
	private final Optional<PrioritizeRemoteGateway> remoteGateway;

	public BatchController(
			BatchMetricsCollector metricsCollector, JobOperator jobOperator,
			StorageEstimationService storageEstimationService, BatchJobLaunchService batchJobLaunchService,
			ServerCapacityService serverCapacityService, BatchOwnershipProperties ownershipProperties,
			JobOwnershipService ownershipService, JobExecutionLookupService lookupService,
			BatchPrioritizationService prioritizationService, Optional<PrioritizeRemoteGateway> remoteGateway
	) {
		this.metricsCollector = metricsCollector;
		this.jobOperator = jobOperator;
		this.storageEstimationService = storageEstimationService;
		this.batchJobLaunchService = batchJobLaunchService;
		this.serverCapacityService = serverCapacityService;
		this.ownershipProperties = ownershipProperties;
		this.ownershipService = ownershipService;
		this.lookupService = lookupService;
		this.prioritizationService = prioritizationService;
		this.remoteGateway = remoteGateway;
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

			JobExecution jobExecution = lookupService.findJobExecutionByJobParameter(
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

	/**
	 * Prioritizes a running job by pausing every other currently running job owned by the same instance as the target
	 * (freeing that instance's shared thread-pool capacity for the target job's already-queued partitions) and, once
	 * they've actually stopped, resuming them in the order they were originally started. If this instance isn't the
	 * owner, the request is broadcast over NATS to find the instance that is (see PrioritizeRemoteGateway), so the
	 * outcome doesn't depend on which replica happened to receive the initial HTTP call.
	 */
	@PostMapping(value = "/prioritize/{jobGuid}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> prioritizeBatchJob(@PathVariable UUID jobGuid) {
		Map<String, Object> response = new HashMap<>();

		JobExecution targetExecution;
		try {
			logger.debug("Attempting to prioritize job with GUID: {}", jobGuid);
			targetExecution = lookupService
					.findJobExecutionByJobParameter(BatchConstants.Job.GUID, jobGuid.toString(), true);
		} catch (NoSuchJobExecutionException e) {
			response.put(BatchConstants.Job.GUID, jobGuid);
			response.put(BatchConstants.Job.ERROR, "Job execution not found");
			response.put(
					BatchConstants.Job.MESSAGE,
					"No job execution found with GUID: " + jobGuid + ". Please verify the GUID is correct."
			);
			response.put(BatchConstants.Common.TIMESTAMP, System.currentTimeMillis());
			logger.error("Job execution not found with GUID: {}", jobGuid);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
		}

		if (!targetExecution.getStatus().isRunning()) {
			response.put(BatchConstants.Job.GUID, jobGuid);
			response.put(BatchConstants.Job.EXECUTION_ID, targetExecution.getId());
			response.put(BatchConstants.Job.ERROR, "Job is not currently running");
			response.put(BatchConstants.Job.MESSAGE, "Only a currently running job can be prioritized.");
			response.put(BatchConstants.Common.TIMESTAMP, System.currentTimeMillis());
			logger.warn("[GUID: {}] Refusing to prioritize job that is not currently running.", jobGuid);
			return ResponseEntity.badRequest().body(response);
		}

		String projectionGuid = targetExecution.getJobParameters().getString(BatchConstants.GuidInput.PROJECTION_GUID);
		Optional<PrioritizeOutcome> outcome;
		if (ownershipService.isOwnedLocally(projectionGuid)) {
			outcome = Optional.of(prioritizationService.prioritizeLocally(jobGuid.toString(), targetExecution));
		} else if (remoteGateway.isPresent()) {
			outcome = remoteGateway.get().requestPrioritize(jobGuid.toString(), projectionGuid).map(this::toOutcome);
		} else {
			response.put(BatchConstants.Job.GUID, jobGuid);
			response.put(BatchConstants.Job.EXECUTION_ID, targetExecution.getId());
			response.put(BatchConstants.Job.ERROR, "Job is not running on this instance");
			response.put(
					BatchConstants.Job.MESSAGE,
					"This replica does not own the job execution, so it has no local capacity to free for it."
			);
			response.put(BatchConstants.Common.TIMESTAMP, System.currentTimeMillis());
			logger.warn("[GUID: {}] Refusing to prioritize job not owned by this instance.", jobGuid);
			return ResponseEntity.badRequest().body(response);
		}

		if (outcome.isEmpty()) {
			response.put(BatchConstants.Job.GUID, jobGuid);
			response.put(BatchConstants.Job.EXECUTION_ID, targetExecution.getId());
			response.put(BatchConstants.Job.ERROR, "Could not locate the replica running this job");
			response.put(
					BatchConstants.Job.MESSAGE,
					"No replica responded as the owner of this job execution within the timeout. It may have just "
							+ "finished; please retry."
			);
			response.put(BatchConstants.Common.TIMESTAMP, System.currentTimeMillis());
			logger.warn("[GUID: {}] No replica claimed ownership of this job when asked to prioritize it.", jobGuid);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
		}

		response.put(BatchConstants.Job.GUID, jobGuid);
		response.put(BatchConstants.Prioritize.TARGET_EXECUTION_ID, outcome.get().targetExecutionId());
		response.put(BatchConstants.Prioritize.OTHERS_PAUSED_COUNT, outcome.get().othersPausedCount());
		response.put(BatchConstants.Job.STATUS, outcome.get().status());
		response.put(BatchConstants.Job.MESSAGE, outcome.get().message());
		response.put(BatchConstants.Common.TIMESTAMP, System.currentTimeMillis());

		HttpStatus httpStatus = "PRIORITIZE_REQUESTED".equals(outcome.get().status()) ? HttpStatus.ACCEPTED
				: HttpStatus.OK;
		return ResponseEntity.status(httpStatus).body(response);
	}

	private PrioritizeOutcome toOutcome(PrioritizeReplyMessage reply) {
		return new PrioritizeOutcome(
				reply.status(), reply.message(), reply.othersPausedCount(), reply.targetExecutionId()
		);
	}

	@GetMapping(value = "/status/{jobGuid}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> getJobStatus(@PathVariable UUID jobGuid) {
		Map<String, Object> response = new HashMap<>();

		JobExecution jobExecution;
		Long executionId;

		try {
			logger.debug("Getting status for job with GUID: {}", jobGuid);

			jobExecution = lookupService
					.findJobExecutionByJobParameter(BatchConstants.Job.GUID, jobGuid.toString(), false);
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
		int workers = BatchUtils.calculateThreadsInUse(jobExecution, isRunning);

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
						"/api/batch/startWithGUIDs", "/api/batch/stop/{jobGuid}", "/api/batch/prioritize/{jobGuid}",
						"/api/batch/status/{jobGuid}", "/api/batch/health", "/api/batch/capacity", "/api/batch/storage"
				)
		);
		response.put(BatchConstants.Common.TIMESTAMP, System.currentTimeMillis());
		return ResponseEntity.ok(response);
	}

	/**
	 * Exposes this instance's shared Batch executor usage and maximum capacity.
	 */
	@GetMapping(value = "/capacity", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> capacity() {
		Map<String, Object> response = new HashMap<>();
		response.put(
				BatchConstants.Capacity.THREAD_CAPACITY,
				serverCapacityService.getAllReplicaCapacity(ownershipProperties.getHeartbeatInterval().getSeconds() * 2)
		);
		response.put(BatchConstants.Common.TIMESTAMP, System.currentTimeMillis());
		return ResponseEntity.ok(response);
	}

	/**
	 * Exposes the estimated PVC storage footprint of currently running projections alongside actual filesystem usage,
	 * so callers can display storage percent-full and flag 'out of spec' conditions
	 */
	@GetMapping(value = "/storage", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> storage() {
		StorageEstimationService.StorageStatus status = storageEstimationService.computeStorageStatus();

		Map<String, Object> response = new HashMap<>();
		response.put(BatchConstants.Storage.PERCENT_FULL, status.percentFull());
		response.put(BatchConstants.Storage.USED_BYTES, status.usedBytes());
		response.put(BatchConstants.Storage.TOTAL_BYTES, status.totalBytes());
		response.put(BatchConstants.Storage.EXPECTED_BYTES, status.expectedBytes());
		response.put(BatchConstants.Storage.OUT_OF_SPEC, status.outOfSpec());
		response.put(BatchConstants.Storage.THRESHOLD_PERCENT, status.thresholdPercent());
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
			JobExecution jobExecution = batchJobLaunchService.launchNewJob(projectionGuid, projectionParametersJson);

			logger.info(
					"[GUID: {}] Batch job started - Execution ID: {}",
					jobExecution.getJobParameters().getString(BatchConstants.Job.GUID), jobExecution.getId()
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
}
