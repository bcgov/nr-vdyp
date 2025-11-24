package ca.bc.gov.nrs.vdyp.batch.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobExecutionNotRunningException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import ca.bc.gov.nrs.vdyp.batch.service.BatchMetricsCollector;
import ca.bc.gov.nrs.vdyp.batch.service.StreamingCsvPartitioner;
import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BatchControllerTest {

	@Mock
	private JobLauncher jobLauncher;

	@Mock
	private Job partitionedJob;

	@Mock
	private JobExplorer jobExplorer;

	@Mock
	private BatchMetricsCollector metricsCollector;

	@Mock
	private StreamingCsvPartitioner csvPartitioner;

	@Mock
	private JobExecution jobExecution;

	@Mock
	private JobInstance jobInstance;

	@Mock
	private JobOperator jobOperator;

	@Mock
	private JobParameters jobParameters;

	private BatchController batchController;

	@BeforeEach
	void setUp() {
		batchController = new BatchController(
				jobLauncher, partitionedJob, jobExplorer, metricsCollector, csvPartitioner, jobOperator
		);

		// Use system temp directory for cross-platform compatibility
		String tempDir = System.getProperty("java.io.tmpdir");
		ReflectionTestUtils.setField(batchController, "batchRootDirectory", tempDir);
		ReflectionTestUtils.setField(batchController, "defaultPartitionSize", 4);
		ReflectionTestUtils.setField(batchController, "jobSearchChunkSize", 1000);
	}

	@Test
	void testConstructor() {
		assertNotNull(batchController);
	}

	@Test
	void testHealth_ReturnsHealthStatus() {
		ResponseEntity<Map<String, Object>> response = batchController.health();

		assertEquals(200, response.getStatusCode().value());
		assertEquals("UP", response.getBody().get("status"));
		assertEquals("VDYP Batch Processing Service", response.getBody().get("service"));
	}

	@Test
	void testStartBatchJobWithFiles_WithValidInput_CallsPartitioner() throws Exception {
		MockMultipartFile polygonFile = new MockMultipartFile(
				"polygonFile", "polygon.csv", "text/csv", "FEATURE_ID\n123".getBytes()
		);
		MockMultipartFile layerFile = new MockMultipartFile(
				"layerFile", "layer.csv", "text/csv", "FEATURE_ID\n123".getBytes()
		);

		// Mock partitioner to return grid size
		when(csvPartitioner.partitionCsvFiles(any(), any(), anyInt(), any())).thenReturn(4);

		// Mock job execution
		when(jobExecution.getId()).thenReturn(1L);
		when(jobExecution.getStatus()).thenReturn(BatchStatus.STARTED);
		when(jobExecution.getJobInstance()).thenReturn(jobInstance);
		when(jobInstance.getJobName()).thenReturn("testJob");
		when(jobExecution.getStartTime()).thenReturn(LocalDateTime.now());
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobParameters.getString("jobGuid")).thenReturn("test-guid");
		when(jobLauncher.run(any(), any())).thenReturn(jobExecution);

		ResponseEntity<Map<String, Object>> response = batchController
				.startBatchJobWithFiles(polygonFile, layerFile, "{}");

		assertEquals(200, response.getStatusCode().value());
		assertTrue(response.getBody().containsKey("jobExecutionId"));
	}

	@Test
	void testStartBatchJobWithFiles_WithPartitionerException_ReturnsBadRequest() throws Exception {
		MockMultipartFile polygonFile = new MockMultipartFile(
				"polygonFile", "polygon.csv", "text/csv", "data".getBytes()
		);
		MockMultipartFile layerFile = new MockMultipartFile("layerFile", "layer.csv", "text/csv", "data".getBytes());

		// Mock partitioner to throw exception
		when(csvPartitioner.partitionCsvFiles(any(), any(), anyInt(), any()))
				.thenThrow(new RuntimeException("Empty file or invalid CSV data"));

		ResponseEntity<Map<String, Object>> response = batchController
				.startBatchJobWithFiles(polygonFile, layerFile, "{}");

		assertEquals(400, response.getStatusCode().value());
		assertTrue(response.getBody().containsKey("validationMessages"));
	}

	@Test
	void testStartBatchJobWithFiles_WithNullParameters_ReturnsBadRequest() {
		MockMultipartFile polygonFile = new MockMultipartFile(
				"polygonFile", "polygon.csv", "text/csv", "data".getBytes()
		);
		MockMultipartFile layerFile = new MockMultipartFile("layerFile", "layer.csv", "text/csv", "data".getBytes());

		ResponseEntity<Map<String, Object>> response = batchController
				.startBatchJobWithFiles(polygonFile, layerFile, null);

		assertEquals(400, response.getStatusCode().value());
		assertTrue(response.getBody().containsKey("error"));
	}

	@Test
	void testStartBatchJobWithFiles_WithNullPartitionedJob_ReturnsBadRequest() throws Exception {
		// Create controller with null job
		BatchController controllerWithNullJob = new BatchController(
				jobLauncher, null, jobExplorer, metricsCollector, csvPartitioner, jobOperator
		);

		String tempDir = System.getProperty("java.io.tmpdir");
		ReflectionTestUtils.setField(controllerWithNullJob, "batchRootDirectory", tempDir);
		ReflectionTestUtils.setField(controllerWithNullJob, "defaultPartitionSize", 4);
		ReflectionTestUtils.setField(controllerWithNullJob, "jobSearchChunkSize", 1000);

		MockMultipartFile polygonFile = new MockMultipartFile(
				"polygonFile", "polygon.csv", "text/csv", "FEATURE_ID\n123".getBytes()
		);
		MockMultipartFile layerFile = new MockMultipartFile(
				"layerFile", "layer.csv", "text/csv", "FEATURE_ID\n123".getBytes()
		);

		// Mock partitioner to return grid size
		when(csvPartitioner.partitionCsvFiles(any(), any(), anyInt(), any())).thenReturn(4);

		ResponseEntity<Map<String, Object>> response = controllerWithNullJob
				.startBatchJobWithFiles(polygonFile, layerFile, "{}");

		// With null partitionedJob, the jobLauncher.run() will fail, resulting in a validation error
		assertEquals(400, response.getStatusCode().value());
		assertTrue(response.getBody().containsKey("validationMessages"));
	}

	@Test
	void testStartBatchJobWithFiles_WithEmptyParametersJson_ReturnsBadRequest() {
		MockMultipartFile polygonFile = new MockMultipartFile(
				"polygonFile", "polygon.csv", "text/csv", "FEATURE_ID\n123".getBytes()
		);
		MockMultipartFile layerFile = new MockMultipartFile(
				"layerFile", "layer.csv", "text/csv", "FEATURE_ID\n123".getBytes()
		);

		ResponseEntity<Map<String, Object>> response = batchController
				.startBatchJobWithFiles(polygonFile, layerFile, "   ");

		assertEquals(400, response.getStatusCode().value());
		assertTrue(response.getBody().containsKey("validationMessages"));
	}

	@Test
	void testStartBatchJobWithFiles_WithDefaultPartitionSize_UsesDefault() throws Exception {
		MockMultipartFile polygonFile = new MockMultipartFile(
				"polygonFile", "polygon.csv", "text/csv", "FEATURE_ID\n123".getBytes()
		);
		MockMultipartFile layerFile = new MockMultipartFile(
				"layerFile", "layer.csv", "text/csv", "FEATURE_ID\n123".getBytes()
		);

		// Mock partitioner to return grid size
		when(csvPartitioner.partitionCsvFiles(any(), any(), eq(4), any())).thenReturn(4);

		// Mock job execution
		when(jobExecution.getId()).thenReturn(1L);
		when(jobExecution.getStatus()).thenReturn(BatchStatus.STARTED);
		when(jobExecution.getJobInstance()).thenReturn(jobInstance);
		when(jobInstance.getJobName()).thenReturn("testJob");
		when(jobExecution.getStartTime()).thenReturn(LocalDateTime.now());
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobParameters.getString("jobGuid")).thenReturn("test-guid");
		when(jobLauncher.run(any(), any())).thenReturn(jobExecution);

		// Always uses default partition size from configuration
		ResponseEntity<Map<String, Object>> response = batchController
				.startBatchJobWithFiles(polygonFile, layerFile, "{}");

		assertEquals(200, response.getStatusCode().value());
		assertTrue(response.getBody().containsKey("jobExecutionId"));
	}

	@Test
	void testStartBatchJobWithFiles_WithNullStartTime_UsesCurrentTime() throws Exception {
		MockMultipartFile polygonFile = new MockMultipartFile(
				"polygonFile", "polygon.csv", "text/csv", "FEATURE_ID\n123".getBytes()
		);
		MockMultipartFile layerFile = new MockMultipartFile(
				"layerFile", "layer.csv", "text/csv", "FEATURE_ID\n123".getBytes()
		);

		// Mock partitioner
		when(csvPartitioner.partitionCsvFiles(any(), any(), anyInt(), any())).thenReturn(4);

		// Mock job execution with null start time
		when(jobExecution.getId()).thenReturn(1L);
		when(jobExecution.getStatus()).thenReturn(BatchStatus.STARTED);
		when(jobExecution.getJobInstance()).thenReturn(jobInstance);
		when(jobInstance.getJobName()).thenReturn("testJob");
		when(jobExecution.getStartTime()).thenReturn(null);
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobParameters.getString("jobGuid")).thenReturn("test-guid");
		when(jobLauncher.run(any(), any())).thenReturn(jobExecution);

		ResponseEntity<Map<String, Object>> response = batchController
				.startBatchJobWithFiles(polygonFile, layerFile, "{}");

		assertEquals(200, response.getStatusCode().value());
		assertTrue(response.getBody().containsKey("startTime"));
		assertTrue(response.getBody().containsKey("jobGuid"));
	}

	@Test
	void testStopBatchJob_WithValidJobGuid_StopsJob() throws Exception {
		String jobGuid = "test-job-guid";
		Long executionId = 123L;

		// Mock finding job execution
		when(jobExplorer.getJobNames()).thenReturn(Arrays.asList("testJob"));
		when(jobExplorer.getJobInstanceCount("testJob")).thenReturn(1L);
		when(jobExplorer.getJobInstances("testJob", 0, 1000)).thenReturn(Arrays.asList(jobInstance));
		when(jobExplorer.getJobExecutions(jobInstance)).thenReturn(Arrays.asList(jobExecution));
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobParameters.getString(BatchConstants.Job.GUID)).thenReturn(jobGuid);
		when(jobExecution.getId()).thenReturn(executionId);

		// Mock stopping the job
		when(jobOperator.stop(executionId)).thenReturn(true);

		ResponseEntity<Map<String, Object>> response = batchController.stopBatchJob(jobGuid);

		assertEquals(200, response.getStatusCode().value());
		assertEquals("STOP_REQUESTED", response.getBody().get(BatchConstants.Job.STATUS));
		assertEquals(jobGuid, response.getBody().get(BatchConstants.Job.GUID));
		assertEquals(executionId, response.getBody().get(BatchConstants.Job.EXECUTION_ID));
	}

	@Test
	void testStopBatchJob_WhenStopFails_ReturnsBadRequest() throws Exception {
		String jobGuid = "test-job-guid";
		Long executionId = 123L;

		// Mock finding job execution
		when(jobExplorer.getJobNames()).thenReturn(Arrays.asList("testJob"));
		when(jobExplorer.getJobInstanceCount("testJob")).thenReturn(1L);
		when(jobExplorer.getJobInstances("testJob", 0, 1000)).thenReturn(Arrays.asList(jobInstance));
		when(jobExplorer.getJobExecutions(jobInstance)).thenReturn(Arrays.asList(jobExecution));
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobParameters.getString(BatchConstants.Job.GUID)).thenReturn(jobGuid);
		when(jobExecution.getId()).thenReturn(executionId);

		// Mock stopping fails
		when(jobOperator.stop(executionId)).thenReturn(false);

		ResponseEntity<Map<String, Object>> response = batchController.stopBatchJob(jobGuid);

		assertEquals(400, response.getStatusCode().value());
		assertEquals("STOP_FAILED", response.getBody().get(BatchConstants.Job.STATUS));
	}

	@Test
	void testStopBatchJob_WhenJobAlreadyStopping_ReturnsAccepted() throws Exception {
		String jobGuid = "test-job-guid";
		Long executionId = 123L;

		// Mock finding job execution
		when(jobExplorer.getJobNames()).thenReturn(Arrays.asList("testJob"));
		when(jobExplorer.getJobInstanceCount("testJob")).thenReturn(1L);
		when(jobExplorer.getJobInstances("testJob", 0, 1000)).thenReturn(Arrays.asList(jobInstance));
		when(jobExplorer.getJobExecutions(jobInstance)).thenReturn(Arrays.asList(jobExecution));
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobParameters.getString(BatchConstants.Job.GUID)).thenReturn(jobGuid);
		when(jobExecution.getId()).thenReturn(executionId);

		// Mock job execution not running exception
		when(jobOperator.stop(executionId)).thenThrow(new JobExecutionNotRunningException("Already stopping"));

		ResponseEntity<Map<String, Object>> response = batchController.stopBatchJob(jobGuid);

		assertEquals(202, response.getStatusCode().value());
		assertEquals("ALREADY_STOPPING", response.getBody().get(BatchConstants.Job.STATUS));
	}

	@Test
	void testStopBatchJob_WithNonExistentJobGuid_ReturnsNotFound() throws NoSuchJobException {
		String jobGuid = "non-existent-guid";

		// Mock no job found
		when(jobExplorer.getJobNames()).thenReturn(Arrays.asList("testJob"));
		when(jobExplorer.getJobInstanceCount("testJob")).thenReturn(1L);
		when(jobExplorer.getJobInstances("testJob", 0, 1000)).thenReturn(Arrays.asList(jobInstance));
		when(jobExplorer.getJobExecutions(jobInstance)).thenReturn(Arrays.asList(jobExecution));
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobParameters.getString(BatchConstants.Job.GUID)).thenReturn("different-guid");

		ResponseEntity<Map<String, Object>> response = batchController.stopBatchJob(jobGuid);

		assertEquals(404, response.getStatusCode().value());
		assertEquals("Job execution not found", response.getBody().get(BatchConstants.Job.ERROR));
	}

	@Test
	void testStopBatchJob_WhenUnexpectedError_ReturnsInternalServerError() throws Exception {
		String jobGuid = "test-job-guid";
		Long executionId = 123L;

		// Mock finding job execution
		when(jobExplorer.getJobNames()).thenReturn(Arrays.asList("testJob"));
		when(jobExplorer.getJobInstanceCount("testJob")).thenReturn(1L);
		when(jobExplorer.getJobInstances("testJob", 0, 1000)).thenReturn(Arrays.asList(jobInstance));
		when(jobExplorer.getJobExecutions(jobInstance)).thenReturn(Arrays.asList(jobExecution));
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobParameters.getString(BatchConstants.Job.GUID)).thenReturn(jobGuid);
		when(jobExecution.getId()).thenReturn(executionId);

		// Mock unexpected exception
		when(jobOperator.stop(executionId)).thenThrow(new RuntimeException("Unexpected error"));

		ResponseEntity<Map<String, Object>> response = batchController.stopBatchJob(jobGuid);

		assertEquals(500, response.getStatusCode().value());
		assertEquals("Failed to stop job execution", response.getBody().get(BatchConstants.Job.ERROR));
	}

	@Test
	void testGetJobStatus_WithValidJobGuid_ReturnsStatus() throws NoSuchJobException {
		String jobGuid = "test-job-guid";
		Long executionId = 123L;

		// Create real job execution with instance and params using builder
		JobInstance realInstance = new JobInstance(1L, "testJob");
		JobParameters realParams = new JobParametersBuilder().addString(BatchConstants.Job.GUID, jobGuid)
				.toJobParameters();
		JobExecution realExecution = new JobExecution(realInstance, executionId, realParams);
		realExecution.setStatus(BatchStatus.STARTED);
		realExecution.setStartTime(LocalDateTime.now());

		// Create step executions
		StepExecution step1 = new StepExecution("workerStep:partition0", realExecution);
		StepExecution step2 = new StepExecution("workerStep:partition1", realExecution);
		step1.setStatus(BatchStatus.COMPLETED);
		step2.setStatus(BatchStatus.STARTED);
		realExecution.addStepExecutions(Arrays.asList(step1, step2));

		// Mock finding job execution
		when(jobExplorer.getJobNames()).thenReturn(Arrays.asList("testJob"));
		when(jobExplorer.getJobInstanceCount("testJob")).thenReturn(1L);
		when(jobExplorer.getJobInstances("testJob", 0, 1000)).thenReturn(Arrays.asList(realInstance));
		when(jobExplorer.getJobExecutions(realInstance)).thenReturn(Arrays.asList(realExecution));

		ResponseEntity<Map<String, Object>> response = batchController.getJobStatus(jobGuid);

		assertEquals(200, response.getStatusCode().value());
		assertEquals(jobGuid, response.getBody().get(BatchConstants.Job.GUID));
		assertEquals(executionId, response.getBody().get(BatchConstants.Job.EXECUTION_ID));
		assertEquals("STARTED", response.getBody().get(BatchConstants.Job.STATUS));
		assertEquals(true, response.getBody().get("isRunning"));
		assertEquals(2L, response.getBody().get("totalPartitions"));
		assertEquals(1L, response.getBody().get("completedPartitions"));
	}

	@Test
	void testGetJobStatus_WithCompletedJob_ReturnsNotRunning() throws NoSuchJobException {
		String jobGuid = "test-job-guid";
		Long executionId = 123L;

		when(jobExplorer.getJobNames()).thenReturn(Arrays.asList("testJob"));
		when(jobExplorer.getJobInstanceCount("testJob")).thenReturn(1L);
		when(jobExplorer.getJobInstances("testJob", 0, 1000)).thenReturn(Arrays.asList(jobInstance));
		when(jobExplorer.getJobExecutions(jobInstance)).thenReturn(Arrays.asList(jobExecution));
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobParameters.getString(BatchConstants.Job.GUID)).thenReturn(jobGuid);
		when(jobExecution.getId()).thenReturn(executionId);
		when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
		when(jobExecution.getJobInstance()).thenReturn(jobInstance);
		when(jobInstance.getJobName()).thenReturn("testJob");
		when(jobExecution.getStartTime()).thenReturn(LocalDateTime.now());
		when(jobExecution.getEndTime()).thenReturn(LocalDateTime.now());
		when(jobExecution.getStepExecutions()).thenReturn(Collections.emptySet());

		ResponseEntity<Map<String, Object>> response = batchController.getJobStatus(jobGuid);

		assertEquals(200, response.getStatusCode().value());
		assertEquals(false, response.getBody().get("isRunning"));
		assertTrue(response.getBody().containsKey("endTime"));
	}

	@Test
	void testGetJobStatus_WithFailedPartitions_CountsAsFailed() throws NoSuchJobException {
		String jobGuid = "test-job-guid";
		Long executionId = 123L;

		// Create real job execution with instance and params using builder
		JobInstance realInstance = new JobInstance(1L, "testJob");
		JobParameters realParams = new JobParametersBuilder().addString(BatchConstants.Job.GUID, jobGuid)
				.toJobParameters();
		JobExecution realExecution = new JobExecution(realInstance, executionId, realParams);
		realExecution.setStatus(BatchStatus.FAILED);
		realExecution.setStartTime(LocalDateTime.now());

		// Create failed step executions
		StepExecution step1 = new StepExecution("workerStep:partition0", realExecution);
		StepExecution step2 = new StepExecution("workerStep:partition1", realExecution);
		step1.setStatus(BatchStatus.FAILED);
		step2.setStatus(BatchStatus.COMPLETED);
		realExecution.addStepExecutions(Arrays.asList(step1, step2));

		// Mock finding job execution
		when(jobExplorer.getJobNames()).thenReturn(Arrays.asList("testJob"));
		when(jobExplorer.getJobInstanceCount("testJob")).thenReturn(1L);
		when(jobExplorer.getJobInstances("testJob", 0, 1000)).thenReturn(Arrays.asList(realInstance));
		when(jobExplorer.getJobExecutions(realInstance)).thenReturn(Arrays.asList(realExecution));

		ResponseEntity<Map<String, Object>> response = batchController.getJobStatus(jobGuid);

		assertEquals(200, response.getStatusCode().value());
		assertEquals(2L, response.getBody().get("totalPartitions"));
		assertEquals(2L, response.getBody().get("completedPartitions")); // Both failed and completed count
	}

	@Test
	void testGetJobStatus_WithNonExistentJobGuid_ReturnsNotFound() throws NoSuchJobException {
		String jobGuid = "non-existent-guid";

		// Mock no job found
		when(jobExplorer.getJobNames()).thenReturn(Arrays.asList("testJob"));
		when(jobExplorer.getJobInstanceCount("testJob")).thenReturn(1L);
		when(jobExplorer.getJobInstances("testJob", 0, 1000)).thenReturn(Arrays.asList(jobInstance));
		when(jobExplorer.getJobExecutions(jobInstance)).thenReturn(Arrays.asList(jobExecution));
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobParameters.getString(BatchConstants.Job.GUID)).thenReturn("different-guid");

		ResponseEntity<Map<String, Object>> response = batchController.getJobStatus(jobGuid);

		assertEquals(404, response.getStatusCode().value());
		assertEquals("Job execution not found", response.getBody().get(BatchConstants.Job.ERROR));
	}

	@Test
	void testGetJobStatus_WhenUnexpectedError_ReturnsInternalServerError() {
		String jobGuid = "test-job-guid";

		// Mock unexpected exception
		when(jobExplorer.getJobNames()).thenThrow(new RuntimeException("Database error"));

		ResponseEntity<Map<String, Object>> response = batchController.getJobStatus(jobGuid);

		assertEquals(500, response.getStatusCode().value());
		assertEquals("Failed to get job status", response.getBody().get(BatchConstants.Job.ERROR));
	}

	@Test
	void testGetJobStatus_WithEmptyJobNames_ReturnsNotFound() {
		String jobGuid = "test-job-guid";

		// Mock empty job names
		when(jobExplorer.getJobNames()).thenReturn(Collections.emptyList());

		ResponseEntity<Map<String, Object>> response = batchController.getJobStatus(jobGuid);

		assertEquals(404, response.getStatusCode().value());
		assertEquals("Job execution not found", response.getBody().get(BatchConstants.Job.ERROR));
	}

	@Test
	void testGetJobStatus_WithMultipleJobInstances_FindsCorrectOne() throws NoSuchJobException {
		String jobGuid = "test-job-guid";
		Long executionId = 123L;

		JobInstance instance1 = new JobInstance(1L, "testJob");
		JobInstance instance2 = new JobInstance(2L, "testJob");

		// Create real JobParameters using builder
		JobParameters params1 = new JobParametersBuilder().addString(BatchConstants.Job.GUID, "wrong-guid")
				.toJobParameters();
		JobParameters params2 = new JobParametersBuilder().addString(BatchConstants.Job.GUID, jobGuid)
				.toJobParameters();

		JobExecution execution1 = new JobExecution(instance1, 1L, params1);
		JobExecution execution2 = new JobExecution(instance2, executionId, params2);
		execution2.setStatus(BatchStatus.STARTED);
		execution2.setStartTime(LocalDateTime.now());

		// Mock finding job execution across multiple instances
		when(jobExplorer.getJobNames()).thenReturn(Arrays.asList("testJob"));
		when(jobExplorer.getJobInstanceCount("testJob")).thenReturn(2L);
		when(jobExplorer.getJobInstances("testJob", 0, 1000)).thenReturn(Arrays.asList(instance1, instance2));

		// First instance doesn't have matching GUID
		when(jobExplorer.getJobExecutions(instance1)).thenReturn(Arrays.asList(execution1));

		// Second instance has matching GUID
		when(jobExplorer.getJobExecutions(instance2)).thenReturn(Arrays.asList(execution2));

		ResponseEntity<Map<String, Object>> response = batchController.getJobStatus(jobGuid);

		assertEquals(200, response.getStatusCode().value());
		assertEquals(jobGuid, response.getBody().get(BatchConstants.Job.GUID));
		assertEquals(executionId, response.getBody().get(BatchConstants.Job.EXECUTION_ID));
	}
}
