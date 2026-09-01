package ca.bc.gov.nrs.vdyp.batch.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.JobExecutionNotRunningException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.http.ResponseEntity;

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

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BatchControllerTest {

	@Mock
	private BatchMetricsCollector metricsCollector;

	@Mock
	private JobExecution jobExecution;

	@Mock
	private JobInstance jobInstance;

	@Mock
	private JobOperator jobOperator;

	@Mock
	private JobParameters jobParameters;

	@Mock
	private BatchJobLaunchService batchJobLaunchService;

	@Mock
	private ServerCapacityService serverCapacityService;

	@Mock
	private StorageEstimationService storageEstimationService;

	@Mock
	private JobOwnershipService ownershipService;

	@Mock
	private JobExecutionLookupService lookupService;

	@Mock
	private BatchPrioritizationService prioritizationService;

	@Mock
	private PrioritizeRemoteGateway remoteGateway;

	private BatchOwnershipProperties ownershipProperties;

	private BatchController batchController;

	@BeforeEach
	void setUp() {
		ownershipProperties = new BatchOwnershipProperties();
		ownershipProperties.setHeartbeatInterval(Duration.of(300, ChronoUnit.SECONDS));

		batchController = new BatchController(
				metricsCollector, jobOperator, storageEstimationService, batchJobLaunchService, serverCapacityService,
				ownershipProperties, ownershipService, lookupService, prioritizationService, Optional.of(remoteGateway)
		);
	}

	@Test
	void testHealth_ReturnsHealthStatus() {
		ResponseEntity<Map<String, Object>> response = batchController.health();

		assertEquals(200, response.getStatusCode().value());
		assertNotNull(response.getBody());
		assertEquals("UP", response.getBody().get("status"));
		assertEquals("VDYP Batch Processing Service", response.getBody().get("service"));
	}

	@Test
	void testHealth_ListsPrioritizeEndpoint() {
		ResponseEntity<Map<String, Object>> response = batchController.health();

		assertNotNull(response.getBody());
		@SuppressWarnings("unchecked")
		List<String> endpoints = (List<String>) response.getBody().get("availableEndpoints");
		assertTrue(endpoints.contains("/api/batch/prioritize/{jobGuid}"));
	}

	@Test
	void testCapacity_ReturnsExecutorThreadUsage() {
		when(serverCapacityService.getAllReplicaCapacity(anyLong())).thenReturn(21L);
		ResponseEntity<Map<String, Object>> response = batchController.capacity();

		assertEquals(200, response.getStatusCode().value());
		assertNotNull(response.getBody());
		assertEquals(21L, response.getBody().get(BatchConstants.Capacity.THREAD_CAPACITY));
		verify(serverCapacityService).getAllReplicaCapacity(anyLong());
	}

	@Test
	void testStorage_ReturnsStorageStatusFromEstimationService() {
		StorageEstimationService.StorageStatus status = new StorageEstimationService.StorageStatus(
				42.5, 1000L, 2000L, 900L, true, 115
		);
		when(storageEstimationService.computeStorageStatus()).thenReturn(status);

		ResponseEntity<Map<String, Object>> response = batchController.storage();

		assertEquals(200, response.getStatusCode().value());
		assertNotNull(response.getBody());
		assertEquals(42.5, response.getBody().get(BatchConstants.Storage.PERCENT_FULL));
		assertEquals(1000L, response.getBody().get(BatchConstants.Storage.USED_BYTES));
		assertEquals(2000L, response.getBody().get(BatchConstants.Storage.TOTAL_BYTES));
		assertEquals(900L, response.getBody().get(BatchConstants.Storage.EXPECTED_BYTES));
		assertEquals(true, response.getBody().get(BatchConstants.Storage.OUT_OF_SPEC));
		assertEquals(115, response.getBody().get(BatchConstants.Storage.THRESHOLD_PERCENT));
	}

	@Test
	void testHealth_ListsStorageEndpoint() {
		ResponseEntity<Map<String, Object>> response = batchController.health();

		assertNotNull(response.getBody());
		@SuppressWarnings("unchecked")
		List<String> endpoints = (List<String>) response.getBody().get("availableEndpoints");
		assertTrue(endpoints.contains("/api/batch/storage"));
	}

	@Test
	void testStartBatchJob_WithValidGUIDs_ReturnsSuccessResponse() throws IOException, JobExecutionException {
		UUID projectionGUID = UUID.randomUUID();

		when(jobExecution.getId()).thenReturn(1L);
		when(jobExecution.getStatus()).thenReturn(BatchStatus.STARTED);
		when(jobExecution.getJobInstance()).thenReturn(jobInstance);
		when(jobInstance.getJobName()).thenReturn("testJob");
		when(jobExecution.getStartTime()).thenReturn(LocalDateTime.now());
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobParameters.getString(BatchConstants.Job.GUID)).thenReturn("test-guid");
		when(batchJobLaunchService.launchNewJob(projectionGUID, "{}")).thenReturn(jobExecution);

		ResponseEntity<Map<String, Object>> response = batchController.startBatchJobPersistedID(projectionGUID, "{}");

		assertEquals(200, response.getStatusCode().value());
		assertNotNull(response.getBody());
		assertTrue(response.getBody().containsKey("jobExecutionId"));

		verify(batchJobLaunchService).launchNewJob(projectionGUID, "{}");
	}

	@Test
	void testStopBatchJob_WithValidJobGuid_StopsJob()
			throws NoSuchJobExecutionException, JobExecutionNotRunningException {
		UUID jobGuid = UUID.randomUUID();
		Long executionId = 123L;

		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobParameters.getString(BatchConstants.Job.GUID)).thenReturn(jobGuid.toString());
		when(jobExecution.getId()).thenReturn(executionId);
		when(lookupService.findJobExecutionByJobParameter(BatchConstants.Job.GUID, jobGuid.toString(), false))
				.thenReturn(jobExecution);
		when(jobOperator.stop(executionId)).thenReturn(true);

		ResponseEntity<Map<String, Object>> response = batchController.stopBatchJob(jobGuid);

		assertEquals(200, response.getStatusCode().value());
		assertNotNull(response.getBody());
		assertEquals("STOP_REQUESTED", response.getBody().get(BatchConstants.Job.STATUS));
		assertEquals(jobGuid.toString(), response.getBody().get(BatchConstants.Job.GUID));
		assertEquals(executionId, response.getBody().get(BatchConstants.Job.EXECUTION_ID));
	}

	@Test
	void testStopBatchJobByProjectionGuid_WithValidProjectionGuid_StopsJob()
			throws NoSuchJobExecutionException, JobExecutionNotRunningException {
		UUID projectionGuid = UUID.randomUUID();
		UUID jobGuid = UUID.randomUUID();
		Long executionId = 123L;

		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobParameters.getString(BatchConstants.GuidInput.PROJECTION_GUID)).thenReturn(projectionGuid.toString());
		when(jobParameters.getString(BatchConstants.Job.GUID)).thenReturn(jobGuid.toString());
		when(jobExecution.getId()).thenReturn(executionId);
		when(
				lookupService.findJobExecutionByJobParameter(
						BatchConstants.GuidInput.PROJECTION_GUID, projectionGuid.toString(), true
				)
		).thenReturn(jobExecution);
		when(jobOperator.stop(executionId)).thenReturn(true);

		ResponseEntity<Map<String, Object>> response = batchController.stopBatchJobByProjectionGuid(projectionGuid);

		assertEquals(200, response.getStatusCode().value());
		assertNotNull(response.getBody());
		assertEquals("STOP_REQUESTED", response.getBody().get(BatchConstants.Job.STATUS));
		assertEquals(jobGuid.toString(), response.getBody().get(BatchConstants.Job.GUID));
		assertEquals(projectionGuid.toString(), response.getBody().get(BatchConstants.GuidInput.PROJECTION_GUID));
		assertEquals(executionId, response.getBody().get(BatchConstants.Job.EXECUTION_ID));
		verify(jobOperator).stop(executionId);
	}

	@Test
	void testStopBatchJob_WhenStopFails_ReturnsBadRequest()
			throws NoSuchJobExecutionException, JobExecutionNotRunningException {
		UUID jobGuid = UUID.randomUUID();
		Long executionId = 123L;

		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobParameters.getString(BatchConstants.Job.GUID)).thenReturn(jobGuid.toString());
		when(jobExecution.getId()).thenReturn(executionId);
		when(lookupService.findJobExecutionByJobParameter(BatchConstants.Job.GUID, jobGuid.toString(), false))
				.thenReturn(jobExecution);
		when(jobOperator.stop(executionId)).thenReturn(false);

		ResponseEntity<Map<String, Object>> response = batchController.stopBatchJob(jobGuid);

		assertEquals(400, response.getStatusCode().value());
		assertNotNull(response.getBody());
		assertEquals("STOP_FAILED", response.getBody().get(BatchConstants.Job.STATUS));
	}

	@Test
	void testStopBatchJob_WhenJobAlreadyStopping_ReturnsAccepted()
			throws NoSuchJobExecutionException, JobExecutionNotRunningException {
		UUID jobGuid = UUID.randomUUID();
		Long executionId = 123L;

		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobParameters.getString(BatchConstants.Job.GUID)).thenReturn(jobGuid.toString());
		when(jobExecution.getId()).thenReturn(executionId);
		when(lookupService.findJobExecutionByJobParameter(BatchConstants.Job.GUID, jobGuid.toString(), false))
				.thenReturn(jobExecution);
		when(jobOperator.stop(executionId)).thenThrow(new JobExecutionNotRunningException("Already stopping"));

		ResponseEntity<Map<String, Object>> response = batchController.stopBatchJob(jobGuid);

		assertEquals(202, response.getStatusCode().value());
		assertNotNull(response.getBody());
		assertEquals("ALREADY_STOPPING", response.getBody().get(BatchConstants.Job.STATUS));
	}

	@Test
	void testStopBatchJob_WithNonExistentJobGuid_ReturnsNotFound() throws NoSuchJobExecutionException {
		UUID jobGuid = UUID.randomUUID();

		when(lookupService.findJobExecutionByJobParameter(BatchConstants.Job.GUID, jobGuid.toString(), false))
				.thenThrow(new NoSuchJobExecutionException("not found"));

		ResponseEntity<Map<String, Object>> response = batchController.stopBatchJob(jobGuid);

		assertEquals(404, response.getStatusCode().value());
		assertNotNull(response.getBody());
		assertEquals("Job execution not found", response.getBody().get(BatchConstants.Job.ERROR));
	}

	@Test
	void testStopBatchJob_WhenUnexpectedError_ReturnsInternalServerError()
			throws NoSuchJobExecutionException, JobExecutionNotRunningException {
		UUID jobGuid = UUID.randomUUID();
		Long executionId = 123L;

		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobParameters.getString(BatchConstants.Job.GUID)).thenReturn(jobGuid.toString());
		when(jobExecution.getId()).thenReturn(executionId);
		when(lookupService.findJobExecutionByJobParameter(BatchConstants.Job.GUID, jobGuid.toString(), false))
				.thenReturn(jobExecution);
		when(jobOperator.stop(executionId)).thenThrow(new RuntimeException("Unexpected error"));

		ResponseEntity<Map<String, Object>> response = batchController.stopBatchJob(jobGuid);

		assertEquals(500, response.getStatusCode().value());
		assertNotNull(response.getBody());
		assertEquals("Failed to stop job execution", response.getBody().get(BatchConstants.Job.ERROR));
	}

	@Test
	void testGetJobStatus_WithValidJobGuid_ReturnsStatus() throws NoSuchJobExecutionException {
		UUID jobGuid = UUID.randomUUID();
		Long executionId = 123L;

		JobInstance realInstance = new JobInstance(1L, "testJob");
		JobParameters realParams = new JobParametersBuilder().addString(BatchConstants.Job.GUID, jobGuid.toString())
				.toJobParameters();
		JobExecution realExecution = new JobExecution(realInstance, executionId, realParams);
		realExecution.setStatus(BatchStatus.STARTED);
		realExecution.setStartTime(LocalDateTime.now());
		ExecutionContext jobContext = new ExecutionContext();
		jobContext.putInt(BatchConstants.Job.TOTAL_POLYGONS, 10);
		realExecution.setExecutionContext(jobContext);

		StepExecution step1 = new StepExecution("workerStep:partition0", realExecution);
		StepExecution step2 = new StepExecution("workerStep:partition1", realExecution);
		step1.setStatus(BatchStatus.COMPLETED);
		step2.setStatus(BatchStatus.STARTED);
		ExecutionContext stepContext1 = new ExecutionContext();
		ExecutionContext stepContext2 = new ExecutionContext();
		stepContext1.putInt(BatchConstants.Job.PROJECTION_ERRORS, 1);
		stepContext1.putInt(BatchConstants.Job.POLYGONS_PROCESSED, 2);
		stepContext1.putInt(BatchConstants.Job.POLYGONS_SKIPPED, 3);
		step1.setExecutionContext(stepContext1);
		stepContext2.putInt(BatchConstants.Job.PROJECTION_ERRORS, 1);
		stepContext2.putInt(BatchConstants.Job.POLYGONS_PROCESSED, 2);
		stepContext2.putInt(BatchConstants.Job.POLYGONS_SKIPPED, 3);
		step2.setExecutionContext(stepContext2);
		realExecution.addStepExecutions(List.of(step1, step2));

		when(lookupService.findJobExecutionByJobParameter(BatchConstants.Job.GUID, jobGuid.toString(), false))
				.thenReturn(realExecution);

		ResponseEntity<Map<String, Object>> response = batchController.getJobStatus(jobGuid);

		assertEquals(200, response.getStatusCode().value());
		assertNotNull(response.getBody());
		assertEquals(jobGuid, response.getBody().get(BatchConstants.Job.GUID));
		assertEquals(executionId, response.getBody().get(BatchConstants.Job.EXECUTION_ID));
		assertEquals("STARTED", response.getBody().get(BatchConstants.Job.STATUS));
		assertEquals(true, response.getBody().get("isRunning"));
		assertEquals(10, response.getBody().get(BatchConstants.Job.TOTAL_POLYGONS));
		assertEquals(2, response.getBody().get(BatchConstants.Job.PROJECTION_ERRORS));
		assertEquals(4, response.getBody().get(BatchConstants.Job.POLYGONS_PROCESSED));
		assertEquals(6, response.getBody().get(BatchConstants.Job.POLYGONS_SKIPPED));
	}

	@Test
	void testGetJobStatus_WithCompletedJob_ReturnsNotRunning() throws NoSuchJobExecutionException {
		UUID jobGuid = UUID.randomUUID();
		Long executionId = 123L;

		ExecutionContext executionContext = new ExecutionContext();

		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobParameters.getString(BatchConstants.Job.GUID)).thenReturn(jobGuid.toString());
		when(jobExecution.getId()).thenReturn(executionId);
		when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
		when(jobExecution.getJobInstance()).thenReturn(jobInstance);
		when(jobInstance.getJobName()).thenReturn("testJob");
		when(jobExecution.getStartTime()).thenReturn(LocalDateTime.now());
		when(jobExecution.getEndTime()).thenReturn(LocalDateTime.now());
		when(jobExecution.getStepExecutions()).thenReturn(Collections.emptySet());
		when(jobExecution.getExecutionContext()).thenReturn(executionContext);
		when(lookupService.findJobExecutionByJobParameter(BatchConstants.Job.GUID, jobGuid.toString(), false))
				.thenReturn(jobExecution);

		ResponseEntity<Map<String, Object>> response = batchController.getJobStatus(jobGuid);

		assertEquals(200, response.getStatusCode().value());
		assertNotNull(response.getBody());
		assertEquals(false, response.getBody().get("isRunning"));
		assertTrue(response.getBody().containsKey("endTime"));
	}

	@Test
	void testGetJobStatus_WithNonExistentJobGuid_ReturnsNotFound() throws NoSuchJobExecutionException {
		UUID jobGuid = UUID.randomUUID();

		when(lookupService.findJobExecutionByJobParameter(BatchConstants.Job.GUID, jobGuid.toString(), false))
				.thenThrow(new NoSuchJobExecutionException("not found"));

		ResponseEntity<Map<String, Object>> response = batchController.getJobStatus(jobGuid);

		assertEquals(404, response.getStatusCode().value());
		assertNotNull(response.getBody());
		assertEquals("Job execution not found", response.getBody().get(BatchConstants.Job.ERROR));
	}

	@Test
	void testGetJobStatus_WhenUnexpectedError_ReturnsInternalServerError() throws NoSuchJobExecutionException {
		UUID jobGuid = UUID.randomUUID();

		when(lookupService.findJobExecutionByJobParameter(BatchConstants.Job.GUID, jobGuid.toString(), false))
				.thenThrow(new RuntimeException("Database error"));

		ResponseEntity<Map<String, Object>> response = batchController.getJobStatus(jobGuid);

		assertEquals(500, response.getStatusCode().value());
		assertNotNull(response.getBody());
		assertEquals("Failed to get job status", response.getBody().get(BatchConstants.Job.ERROR));
	}

	@Test
	void testPrioritizeBatchJob_TargetNotFound_ReturnsNotFound() throws NoSuchJobExecutionException {
		UUID jobGuid = UUID.randomUUID();

		when(lookupService.findJobExecutionByJobParameter(BatchConstants.Job.GUID, jobGuid.toString(), true))
				.thenThrow(new NoSuchJobExecutionException("not found"));

		ResponseEntity<Map<String, Object>> response = batchController.prioritizeBatchJob(jobGuid);

		assertEquals(404, response.getStatusCode().value());
		assertNotNull(response.getBody());
		assertEquals("Job execution not found", response.getBody().get(BatchConstants.Job.ERROR));
		verifyNoInteractions(prioritizationService, remoteGateway);
	}

	@Test
	void testPrioritizeBatchJob_TargetNotRunning_ReturnsBadRequest() throws NoSuchJobExecutionException {
		UUID jobGuid = UUID.randomUUID();
		JobInstance instance = new JobInstance(1L, "testJob");
		JobParameters params = new JobParametersBuilder().addString(BatchConstants.Job.GUID, jobGuid.toString())
				.toJobParameters();
		JobExecution execution = new JobExecution(instance, 100L, params);
		execution.setStatus(BatchStatus.COMPLETED);

		when(lookupService.findJobExecutionByJobParameter(BatchConstants.Job.GUID, jobGuid.toString(), true))
				.thenReturn(execution);

		ResponseEntity<Map<String, Object>> response = batchController.prioritizeBatchJob(jobGuid);

		assertEquals(400, response.getStatusCode().value());
		assertNotNull(response.getBody());
		assertEquals("Job is not currently running", response.getBody().get(BatchConstants.Job.ERROR));
		verifyNoInteractions(prioritizationService, remoteGateway);
	}

	@Test
	void testPrioritizeBatchJob_OwnedLocally_DelegatesToPrioritizationService() throws NoSuchJobExecutionException {
		UUID jobGuid = UUID.randomUUID();
		UUID projectionGuid = UUID.randomUUID();
		JobInstance instance = new JobInstance(1L, "testJob");
		JobParameters params = new JobParametersBuilder().addString(BatchConstants.Job.GUID, jobGuid.toString())
				.addString(BatchConstants.GuidInput.PROJECTION_GUID, projectionGuid.toString()).toJobParameters();
		JobExecution execution = new JobExecution(instance, 100L, params);
		execution.setStatus(BatchStatus.STARTED);

		when(lookupService.findJobExecutionByJobParameter(BatchConstants.Job.GUID, jobGuid.toString(), true))
				.thenReturn(execution);
		when(ownershipService.isOwnedLocally(projectionGuid.toString())).thenReturn(true);
		when(prioritizationService.prioritizeLocally(jobGuid.toString(), execution))
				.thenReturn(new PrioritizeOutcome("PRIORITIZE_REQUESTED", "2 other job(s) paused.", 2, 100L));

		ResponseEntity<Map<String, Object>> response = batchController.prioritizeBatchJob(jobGuid);

		assertEquals(202, response.getStatusCode().value());
		assertNotNull(response.getBody());
		assertEquals("PRIORITIZE_REQUESTED", response.getBody().get(BatchConstants.Job.STATUS));
		assertEquals(2, response.getBody().get(BatchConstants.Prioritize.OTHERS_PAUSED_COUNT));
		verifyNoInteractions(remoteGateway);
	}

	@Test
	void testPrioritizeBatchJob_OwnedLocally_AlreadyPrioritized_ReturnsOk() throws NoSuchJobExecutionException {
		UUID jobGuid = UUID.randomUUID();
		UUID projectionGuid = UUID.randomUUID();
		JobInstance instance = new JobInstance(1L, "testJob");
		JobParameters params = new JobParametersBuilder().addString(BatchConstants.Job.GUID, jobGuid.toString())
				.addString(BatchConstants.GuidInput.PROJECTION_GUID, projectionGuid.toString()).toJobParameters();
		JobExecution execution = new JobExecution(instance, 100L, params);
		execution.setStatus(BatchStatus.STARTED);

		when(lookupService.findJobExecutionByJobParameter(BatchConstants.Job.GUID, jobGuid.toString(), true))
				.thenReturn(execution);
		when(ownershipService.isOwnedLocally(projectionGuid.toString())).thenReturn(true);
		when(prioritizationService.prioritizeLocally(jobGuid.toString(), execution))
				.thenReturn(new PrioritizeOutcome("ALREADY_PRIORITIZED", "Nothing to pause.", 0, 100L));

		ResponseEntity<Map<String, Object>> response = batchController.prioritizeBatchJob(jobGuid);

		assertEquals(200, response.getStatusCode().value());
		assertNotNull(response.getBody());
		assertEquals("ALREADY_PRIORITIZED", response.getBody().get(BatchConstants.Job.STATUS));
	}

	@Test
	void testPrioritizeBatchJob_NotOwnedLocally_DelegatesToRemoteGateway() throws NoSuchJobExecutionException {
		UUID jobGuid = UUID.randomUUID();
		UUID projectionGuid = UUID.randomUUID();
		JobInstance instance = new JobInstance(1L, "testJob");
		JobParameters params = new JobParametersBuilder().addString(BatchConstants.Job.GUID, jobGuid.toString())
				.addString(BatchConstants.GuidInput.PROJECTION_GUID, projectionGuid.toString()).toJobParameters();
		JobExecution execution = new JobExecution(instance, 100L, params);
		execution.setStatus(BatchStatus.STARTED);

		when(lookupService.findJobExecutionByJobParameter(BatchConstants.Job.GUID, jobGuid.toString(), true))
				.thenReturn(execution);
		when(ownershipService.isOwnedLocally(projectionGuid.toString())).thenReturn(false);
		when(remoteGateway.requestPrioritize(jobGuid.toString(), projectionGuid.toString())).thenReturn(
				Optional.of(new PrioritizeReplyMessage(true, "PRIORITIZE_REQUESTED", "Paused elsewhere.", 1, 100L))
		);

		ResponseEntity<Map<String, Object>> response = batchController.prioritizeBatchJob(jobGuid);

		assertEquals(202, response.getStatusCode().value());
		assertNotNull(response.getBody());
		assertEquals("PRIORITIZE_REQUESTED", response.getBody().get(BatchConstants.Job.STATUS));
		assertEquals(1, response.getBody().get(BatchConstants.Prioritize.OTHERS_PAUSED_COUNT));
		verifyNoInteractions(prioritizationService);
	}

	@Test
	void testPrioritizeBatchJob_NotOwnedLocally_NoReplicaResponds_ReturnsNotFound() throws NoSuchJobExecutionException {
		UUID jobGuid = UUID.randomUUID();
		UUID projectionGuid = UUID.randomUUID();
		JobInstance instance = new JobInstance(1L, "testJob");
		JobParameters params = new JobParametersBuilder().addString(BatchConstants.Job.GUID, jobGuid.toString())
				.addString(BatchConstants.GuidInput.PROJECTION_GUID, projectionGuid.toString()).toJobParameters();
		JobExecution execution = new JobExecution(instance, 100L, params);
		execution.setStatus(BatchStatus.STARTED);

		when(lookupService.findJobExecutionByJobParameter(BatchConstants.Job.GUID, jobGuid.toString(), true))
				.thenReturn(execution);
		when(ownershipService.isOwnedLocally(projectionGuid.toString())).thenReturn(false);
		when(remoteGateway.requestPrioritize(jobGuid.toString(), projectionGuid.toString()))
				.thenReturn(Optional.empty());

		ResponseEntity<Map<String, Object>> response = batchController.prioritizeBatchJob(jobGuid);

		assertEquals(404, response.getStatusCode().value());
		assertNotNull(response.getBody());
		assertEquals("Could not locate the replica running this job", response.getBody().get(BatchConstants.Job.ERROR));
	}

	@Test
	void testPrioritizeBatchJob_NotOwnedLocally_NoGatewayAvailable_ReturnsBadRequest()
			throws NoSuchJobExecutionException {
		UUID jobGuid = UUID.randomUUID();
		UUID projectionGuid = UUID.randomUUID();
		JobInstance instance = new JobInstance(1L, "testJob");
		JobParameters params = new JobParametersBuilder().addString(BatchConstants.Job.GUID, jobGuid.toString())
				.addString(BatchConstants.GuidInput.PROJECTION_GUID, projectionGuid.toString()).toJobParameters();
		JobExecution execution = new JobExecution(instance, 100L, params);
		execution.setStatus(BatchStatus.STARTED);

		BatchController controllerWithoutNats = new BatchController(
				metricsCollector, jobOperator, storageEstimationService, batchJobLaunchService, serverCapacityService,
				ownershipProperties, ownershipService, lookupService, prioritizationService, Optional.empty()
		);

		when(lookupService.findJobExecutionByJobParameter(BatchConstants.Job.GUID, jobGuid.toString(), true))
				.thenReturn(execution);
		when(ownershipService.isOwnedLocally(projectionGuid.toString())).thenReturn(false);

		ResponseEntity<Map<String, Object>> response = controllerWithoutNats.prioritizeBatchJob(jobGuid);

		assertEquals(400, response.getStatusCode().value());
		assertNotNull(response.getBody());
		assertEquals("Job is not running on this instance", response.getBody().get(BatchConstants.Job.ERROR));
		verifyNoInteractions(prioritizationService);
	}
}
