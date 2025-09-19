package ca.bc.gov.nrs.vdyp.batch.controller;

import ca.bc.gov.nrs.vdyp.batch.model.BatchMetrics;
import ca.bc.gov.nrs.vdyp.batch.service.BatchMetricsCollector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.*;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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
	private JobExecution jobExecution;

	@Mock
	private JobInstance jobInstance;

	@Mock
	private StepExecution stepExecution;

	private BatchController batchController;

	@BeforeEach
	void setUp() {
		batchController = new BatchController(jobLauncher, partitionedJob, jobExplorer, metricsCollector);
	}

	@Test
	void testConstructor() {
		assertNotNull(batchController);
	}

	@Test
	void testStartBatchJob_WithValidJob_ReturnsSuccess() throws JobExecutionAlreadyRunningException,
			JobRestartException, JobInstanceAlreadyCompleteException, JobParametersInvalidException {
		BatchJobRequest request = new BatchJobRequest();
		request.setPartitionSize(4L);

		when(jobExecution.getId()).thenReturn(1L);
		when(jobExecution.getJobInstance()).thenReturn(jobInstance);
		when(jobInstance.getJobName()).thenReturn("testJob");
		when(jobExecution.getStatus()).thenReturn(BatchStatus.STARTED);
		when(jobExecution.getStartTime()).thenReturn(LocalDateTime.now());
		when(jobLauncher.run(eq(partitionedJob), any(JobParameters.class))).thenReturn(jobExecution);

		ResponseEntity<Map<String, Object>> response = batchController.startBatchJob(request);

		assertEquals(200, response.getStatusCode().value());
		assertNotNull(response.getBody());
		assertTrue(response.getBody().containsKey("jobExecutionId"));
		assertEquals(1L, response.getBody().get("jobExecutionId"));

		verify(jobLauncher).run(eq(partitionedJob), any(JobParameters.class));
	}

	@Test
	void testStartBatchJob_WithNullRequest_ReturnsSuccess() throws JobExecutionAlreadyRunningException,
			JobRestartException, JobInstanceAlreadyCompleteException, JobParametersInvalidException {
		when(jobExecution.getId()).thenReturn(1L);
		when(jobExecution.getJobInstance()).thenReturn(jobInstance);
		when(jobInstance.getJobName()).thenReturn("testJob");
		when(jobExecution.getStatus()).thenReturn(BatchStatus.STARTED);
		when(jobExecution.getStartTime()).thenReturn(LocalDateTime.now());
		when(jobLauncher.run(eq(partitionedJob), any(JobParameters.class))).thenReturn(jobExecution);

		ResponseEntity<Map<String, Object>> response = batchController.startBatchJob(null);

		assertEquals(200, response.getStatusCode().value());
		assertNotNull(response.getBody());
	}

	@Test
	void testStartBatchJob_WithNullJob_ReturnsJobNotAvailable() {
		batchController = new BatchController(jobLauncher, null, jobExplorer, metricsCollector);

		ResponseEntity<Map<String, Object>> response = batchController.startBatchJob(null);

		assertEquals(200, response.getStatusCode().value());
		assertNotNull(response.getBody());
		assertEquals("JOB_NOT_AVAILABLE", response.getBody().get("status"));
	}

	@Test
	void testStartBatchJob_JobLauncherThrowsException_ReturnsError() throws JobExecutionAlreadyRunningException,
			JobRestartException, JobInstanceAlreadyCompleteException, JobParametersInvalidException {
		BatchJobRequest request = new BatchJobRequest();
		when(jobLauncher.run(any(), any())).thenThrow(new JobExecutionAlreadyRunningException("Job already running"));

		ResponseEntity<Map<String, Object>> response = batchController.startBatchJob(request);

		assertEquals(500, response.getStatusCode().value());
		assertNotNull(response.getBody());
		assertTrue(response.getBody().containsKey("error"));
	}

	@Test
	void testGetJobStatus_WithValidJobExecutionId_ReturnsJobStatus() {
		when(jobExecution.getId()).thenReturn(1L);
		when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
		when(jobExecution.getExitStatus()).thenReturn(ExitStatus.COMPLETED);
		when(jobExecution.getStartTime()).thenReturn(LocalDateTime.now().minusMinutes(5));
		when(jobExecution.getEndTime()).thenReturn(LocalDateTime.now());
		when(jobExecution.getJobInstance()).thenReturn(jobInstance);
		when(jobInstance.getJobName()).thenReturn("testJob");
		when(jobExplorer.getJobExecution(1L)).thenReturn(jobExecution);

		ResponseEntity<Map<String, Object>> response = batchController.getJobStatus(1L);

		assertEquals(200, response.getStatusCode().value());
		assertNotNull(response.getBody());
		assertEquals(1L, response.getBody().get("jobExecutionId"));
		assertEquals("COMPLETED", response.getBody().get("status"));

		verify(jobExplorer).getJobExecution(1L);
	}

	@Test
	void testGetJobStatus_WithInvalidJobExecutionId_ReturnsNotFound() {
		when(jobExplorer.getJobExecution(999L)).thenReturn(null);

		ResponseEntity<Map<String, Object>> response = batchController.getJobStatus(999L);

		assertEquals(404, response.getStatusCode().value());
	}

	@Test
	void testGetJobStatus_ExceptionThrown_ReturnsError() {
		when(jobExplorer.getJobExecution(1L)).thenThrow(new RuntimeException("Database error"));

		ResponseEntity<Map<String, Object>> response = batchController.getJobStatus(1L);

		assertEquals(500, response.getStatusCode().value());
		assertNotNull(response.getBody());
		assertTrue(response.getBody().containsKey("error"));
	}

	@Test
	void testGetJobMetrics_WithValidJobExecutionId_ReturnsMetrics() {
		BatchMetrics metrics = new BatchMetrics();
		metrics.setStartTime(LocalDateTime.now().minusMinutes(10));
		metrics.setEndTime(LocalDateTime.now());
		metrics.setTotalRecordsRead(100L);
		metrics.setTotalRecordsWritten(95L);
		metrics.setTotalSkips(5);

		when(jobExecution.getJobInstance()).thenReturn(jobInstance);
		when(jobInstance.getJobName()).thenReturn("testJob");
		when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
		when(jobExplorer.getJobExecution(1L)).thenReturn(jobExecution);
		when(metricsCollector.getJobMetrics(1L)).thenReturn(metrics);

		ResponseEntity<Map<String, Object>> response = batchController.getJobMetrics(1L);

		assertEquals(200, response.getStatusCode().value());
		assertNotNull(response.getBody());
		assertEquals(1L, response.getBody().get("jobExecutionId"));
		assertEquals(100L, response.getBody().get("totalRecordsRead"));
	}

	@Test
	void testGetJobMetrics_WithNullMetrics_ReturnsFallbackData() {
		when(jobExecution.getJobInstance()).thenReturn(jobInstance);
		when(jobInstance.getJobName()).thenReturn("testJob");
		when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
		when(jobExecution.getStartTime()).thenReturn(LocalDateTime.now().minusMinutes(5));
		when(jobExecution.getEndTime()).thenReturn(LocalDateTime.now());
		when(jobExplorer.getJobExecution(1L)).thenReturn(jobExecution);
		when(metricsCollector.getJobMetrics(1L)).thenReturn(null);

		ResponseEntity<Map<String, Object>> response = batchController.getJobMetrics(1L);

		assertEquals(200, response.getStatusCode().value());
		assertNotNull(response.getBody());
		assertEquals("Detailed metrics not available for this job", response.getBody().get("message"));
	}

	@Test
	void testGetJobMetrics_WithInvalidJobExecutionId_ReturnsNotFound() {
		when(jobExplorer.getJobExecution(999L)).thenReturn(null);

		ResponseEntity<Map<String, Object>> response = batchController.getJobMetrics(999L);

		assertEquals(404, response.getStatusCode().value());
	}

	@Test
	void testListJobs_ReturnsJobsList() {
		List<String> jobNames = List.of("testJob");
		List<JobInstance> jobInstances = List.of(jobInstance);
		List<JobExecution> jobExecutions = List.of(jobExecution);

		when(jobExecution.getId()).thenReturn(1L);
		when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
		when(jobExecution.getExitStatus()).thenReturn(ExitStatus.COMPLETED);
		when(jobExecution.getStartTime()).thenReturn(LocalDateTime.now().minusMinutes(5));
		when(jobExecution.getEndTime()).thenReturn(LocalDateTime.now());
		when(jobExecution.getCreateTime()).thenReturn(LocalDateTime.now().minusMinutes(6));
		when(jobExecution.getStepExecutions()).thenReturn(Collections.singleton(stepExecution));
		when(jobInstance.getId()).thenReturn(10L);
		when(stepExecution.getStepName()).thenReturn("testStep");
		when(stepExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
		when(stepExecution.getReadCount()).thenReturn(100L);
		when(stepExecution.getWriteCount()).thenReturn(95L);
		when(stepExecution.getSkipCount()).thenReturn(5L);
		when(jobExplorer.getJobNames()).thenReturn(jobNames);
		when(jobExplorer.getJobInstances("testJob", 0, 50)).thenReturn(jobInstances);
		when(jobExplorer.getJobExecutions(jobInstance)).thenReturn(jobExecutions);

		ResponseEntity<Map<String, Object>> response = batchController.listJobs(50);

		assertEquals(200, response.getStatusCode().value());
		assertNotNull(response.getBody());
		assertTrue(response.getBody().containsKey("jobs"));

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> jobs = (List<Map<String, Object>>) response.getBody().get("jobs");
		assertEquals(1, jobs.size());
		assertEquals(1L, jobs.get(0).get("jobExecutionId"));
	}

	@Test
	void testListJobs_WithCustomLimit_ReturnsLimitedResults() {
		when(jobExplorer.getJobNames()).thenReturn(Collections.emptyList());

		ResponseEntity<Map<String, Object>> response = batchController.listJobs(10);

		assertEquals(200, response.getStatusCode().value());
		assertNotNull(response.getBody());
		assertEquals(10, response.getBody().get("limit"));
	}

	@Test
	void testHealth_ReturnsHealthStatus() {
		ResponseEntity<Map<String, Object>> response = batchController.health();

		assertEquals(200, response.getStatusCode().value());
		assertNotNull(response.getBody());
		assertEquals("UP", response.getBody().get("status"));
		assertEquals("VDYP Batch Processing Service", response.getBody().get("service"));
		assertTrue(response.getBody().containsKey("availableEndpoints"));
	}

	@Test
	void testStartBatchJob_WithAllRequestParameters_SetsAllParameters() throws JobExecutionAlreadyRunningException,
			JobRestartException, JobInstanceAlreadyCompleteException, JobParametersInvalidException {
		BatchJobRequest request = new BatchJobRequest();
		request.setPartitionSize(4L);
		request.setMaxRetryAttempts(3);
		request.setRetryBackoffPeriod(1000L);
		request.setMaxSkipCount(10);

		when(jobExecution.getId()).thenReturn(1L);
		when(jobExecution.getJobInstance()).thenReturn(jobInstance);
		when(jobInstance.getJobName()).thenReturn("testJob");
		when(jobExecution.getStatus()).thenReturn(BatchStatus.STARTED);
		when(jobExecution.getStartTime()).thenReturn(LocalDateTime.now());
		when(jobLauncher.run(eq(partitionedJob), any(JobParameters.class))).thenReturn(jobExecution);

		ResponseEntity<Map<String, Object>> response = batchController.startBatchJob(request);

		assertEquals(200, response.getStatusCode().value());
		assertNotNull(response.getBody());

		verify(jobLauncher).run(eq(partitionedJob), any(JobParameters.class));
	}
}