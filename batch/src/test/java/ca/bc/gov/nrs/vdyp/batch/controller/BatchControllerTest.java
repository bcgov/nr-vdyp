package ca.bc.gov.nrs.vdyp.batch.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import ca.bc.gov.nrs.vdyp.batch.model.BatchMetrics;
import ca.bc.gov.nrs.vdyp.batch.service.BatchMetricsCollector;
import ca.bc.gov.nrs.vdyp.batch.service.StreamingCsvPartitioner;

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
	private StepExecution stepExecution;

	private BatchController batchController;

	@BeforeEach
	void setUp() {
		batchController = new BatchController(jobLauncher, partitionedJob, jobExplorer, metricsCollector,
				csvPartitioner);
		// Set @Value annotated fields using reflection for unit testing
		ReflectionTestUtils.setField(batchController, "inputBasePath", "/tmp/input");
		ReflectionTestUtils.setField(batchController, "outputBasePath", "/tmp/output");
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
	void testGetJobStatus_WithValidJobExecutionId_ReturnsJobStatus() {
		when(jobExecution.getId()).thenReturn(1L);
		when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
		when(jobExecution.getExitStatus()).thenReturn(ExitStatus.COMPLETED);
		when(jobExecution.getJobInstance()).thenReturn(jobInstance);
		when(jobInstance.getJobName()).thenReturn("testJob");
		when(jobExplorer.getJobExecution(1L)).thenReturn(jobExecution);

		ResponseEntity<Map<String, Object>> response = batchController.getJobStatus(1L);

		assertEquals(200, response.getStatusCode().value());
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
	void testGetJobMetrics_WithInvalidJobExecutionId_ReturnsNotFound() {
		when(jobExplorer.getJobExecution(999L)).thenReturn(null);

		ResponseEntity<Map<String, Object>> response = batchController.getJobMetrics(999L);

		assertEquals(404, response.getStatusCode().value());
	}

	@Test
	void testListJobs_ReturnsJobsList() {
		when(jobExplorer.getJobNames()).thenReturn(List.of());

		ResponseEntity<Map<String, Object>> response = batchController.listJobs(50);

		assertEquals(200, response.getStatusCode().value());
		assertTrue(response.getBody().containsKey("jobs"));
	}

	@Test
	void testStartBatchJobWithFiles_WithValidInput_CallsPartitioner() throws Exception {
		MockMultipartFile polygonFile = new MockMultipartFile("polygonFile", "polygon.csv", "text/csv",
				"FEATURE_ID\n123".getBytes());
		MockMultipartFile layerFile = new MockMultipartFile("layerFile", "layer.csv", "text/csv",
				"FEATURE_ID\n123".getBytes());

		// Mock partition result to prevent NullPointerException
		StreamingCsvPartitioner.PartitionResult mockResult = mock(StreamingCsvPartitioner.PartitionResult.class);
		when(mockResult.getGridSize()).thenReturn(4);
		when(mockResult.getTotalFeatureIds()).thenReturn(1);
		when(mockResult.getBaseOutputDir()).thenReturn(java.nio.file.Paths.get("/tmp/test"));
		when(csvPartitioner.partitionCsvFiles(any(), any(), anyInt(), any())).thenReturn(mockResult);

		// Mock job execution
		when(jobExecution.getId()).thenReturn(1L);
		when(jobExecution.getStatus()).thenReturn(BatchStatus.STARTED);
		when(jobExecution.getJobInstance()).thenReturn(jobInstance);
		when(jobInstance.getJobName()).thenReturn("testJob");
		when(jobExecution.getStartTime()).thenReturn(LocalDateTime.now());
		when(jobLauncher.run(any(), any())).thenReturn(jobExecution);

		ResponseEntity<Map<String, Object>> response = batchController.startBatchJobWithFiles(polygonFile, layerFile,
				4L, "{}");

		assertEquals(200, response.getStatusCode().value());
		assertTrue(response.getBody().containsKey("jobExecutionId"));
		verify(csvPartitioner).partitionCsvFiles(any(), any(), eq(4), any());
	}

	@Test
	void testStartBatchJobWithFiles_WithPartitionerException_ReturnsBadRequest() throws Exception {
		MockMultipartFile polygonFile = new MockMultipartFile("polygonFile", "polygon.csv", "text/csv",
				"data".getBytes());
		MockMultipartFile layerFile = new MockMultipartFile("layerFile", "layer.csv", "text/csv", "data".getBytes());

		// Mock partitioner to throw exception (simulating empty file or other
		// processing error)
		when(csvPartitioner.partitionCsvFiles(any(), any(), anyInt(), any()))
				.thenThrow(new RuntimeException("Empty file or invalid CSV data"));

		ResponseEntity<Map<String, Object>> response = batchController.startBatchJobWithFiles(polygonFile, layerFile,
				4L, "{}");

		assertEquals(400, response.getStatusCode().value());
		assertTrue(response.getBody().containsKey("validationMessages"));
	}

	@Test
	void testStartBatchJobWithFiles_WithNullParameters_ReturnsBadRequest() {
		MockMultipartFile polygonFile = new MockMultipartFile("polygonFile", "polygon.csv", "text/csv",
				"data".getBytes());
		MockMultipartFile layerFile = new MockMultipartFile("layerFile", "layer.csv", "text/csv", "data".getBytes());

		ResponseEntity<Map<String, Object>> response = batchController.startBatchJobWithFiles(polygonFile, layerFile,
				4L, null);

		assertEquals(400, response.getStatusCode().value());
		assertTrue(response.getBody().containsKey("error"));
	}

	@Test
	void testGetJobStatus_ExceptionThrown_ReturnsError() {
		when(jobExplorer.getJobExecution(1L)).thenThrow(new RuntimeException("Database error"));

		ResponseEntity<Map<String, Object>> response = batchController.getJobStatus(1L);

		assertEquals(500, response.getStatusCode().value());
		assertTrue(response.getBody().containsKey("error"));
	}

	@Test
	void testGetJobMetrics_ExceptionThrown_ReturnsError() {
		when(jobExplorer.getJobExecution(1L)).thenThrow(new RuntimeException("Database error"));

		ResponseEntity<Map<String, Object>> response = batchController.getJobMetrics(1L);

		assertEquals(500, response.getStatusCode().value());
		assertTrue(response.getBody().containsKey("error"));
	}

	@Test
	void testListJobs_ExceptionThrown_ReturnsError() {
		when(jobExplorer.getJobNames()).thenThrow(new RuntimeException("Database error"));

		ResponseEntity<Map<String, Object>> response = batchController.listJobs(50);

		assertEquals(500, response.getStatusCode().value());
		assertTrue(response.getBody().containsKey("error"));
	}

	@Test
	void testGetJobMetrics_WithNullMetrics_ReturnsFallbackData() {
		when(jobExecution.getId()).thenReturn(1L);
		when(jobExecution.getJobInstance()).thenReturn(jobInstance);
		when(jobInstance.getJobName()).thenReturn("testJob");
		when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
		when(jobExecution.getStartTime()).thenReturn(LocalDateTime.now());
		when(jobExecution.getEndTime()).thenReturn(LocalDateTime.now());
		when(jobExplorer.getJobExecution(1L)).thenReturn(jobExecution);
		when(metricsCollector.getJobMetrics(1L)).thenReturn(null);

		ResponseEntity<Map<String, Object>> response = batchController.getJobMetrics(1L);

		assertEquals(200, response.getStatusCode().value());
		assertEquals("Detailed metrics not available for this job", response.getBody().get("message"));
	}

	@Test
	void testStartBatchJobWithFiles_WithJobLauncherException_ReturnsValidationError() throws Exception {
		MockMultipartFile polygonFile = new MockMultipartFile("polygonFile", "polygon.csv", "text/csv",
				"FEATURE_ID\n123".getBytes());
		MockMultipartFile layerFile = new MockMultipartFile("layerFile", "layer.csv", "text/csv",
				"FEATURE_ID\n123".getBytes());

		// Mock partitioner to throw exception (simulating processing error)
		when(csvPartitioner.partitionCsvFiles(any(), any(), anyInt(), any()))
				.thenThrow(new RuntimeException("Job launcher error"));

		ResponseEntity<Map<String, Object>> response = batchController.startBatchJobWithFiles(polygonFile, layerFile,
				4L, "{}");

		assertEquals(400, response.getStatusCode().value());
		assertTrue(response.getBody().containsKey("validationMessages"));
	}

	@Test
	void testStartBatchJobWithFiles_WithNullPartitionedJob_ReturnsJobNotAvailable() throws Exception {
		// Create controller with null job
		BatchController controllerWithNullJob = new BatchController(jobLauncher, null, jobExplorer, metricsCollector,
				csvPartitioner);
		ReflectionTestUtils.setField(controllerWithNullJob, "inputBasePath", "/tmp/input");
		ReflectionTestUtils.setField(controllerWithNullJob, "outputBasePath", "/tmp/output");

		MockMultipartFile polygonFile = new MockMultipartFile("polygonFile", "polygon.csv", "text/csv",
				"FEATURE_ID\n123".getBytes());
		MockMultipartFile layerFile = new MockMultipartFile("layerFile", "layer.csv", "text/csv",
				"FEATURE_ID\n123".getBytes());

		ResponseEntity<Map<String, Object>> response = controllerWithNullJob.startBatchJobWithFiles(polygonFile,
				layerFile, 4L, "{}");

		assertEquals(200, response.getStatusCode().value());
		assertEquals("VDYP Batch job not available - Job auto-creation is disabled", response.getBody().get("message"));
		assertEquals("JOB_NOT_AVAILABLE", response.getBody().get("status"));
	}

	@Test
	void testStartBatchJobWithFiles_WithEmptyParametersJson_ReturnsBadRequest() {
		MockMultipartFile polygonFile = new MockMultipartFile("polygonFile", "polygon.csv", "text/csv",
				"FEATURE_ID\n123".getBytes());
		MockMultipartFile layerFile = new MockMultipartFile("layerFile", "layer.csv", "text/csv",
				"FEATURE_ID\n123".getBytes());

		ResponseEntity<Map<String, Object>> response = batchController.startBatchJobWithFiles(polygonFile, layerFile,
				4L, "   ");

		assertEquals(400, response.getStatusCode().value());
		assertTrue(response.getBody().containsKey("validationMessages"));
	}

	@Test 
	void testListJobs_WithJobInstances_ReturnsJobsList() {
		when(jobExplorer.getJobNames()).thenReturn(List.of("testJob"));
		when(jobExplorer.getJobInstances("testJob", 0, 50)).thenReturn(List.of(jobInstance));
		when(jobInstance.getId()).thenReturn(1L);
		when(jobExplorer.getJobExecutions(jobInstance)).thenReturn(List.of(jobExecution));
		
		when(jobExecution.getId()).thenReturn(1L);
		when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
		when(jobExecution.getExitStatus()).thenReturn(ExitStatus.COMPLETED);
		when(jobExecution.getStartTime()).thenReturn(LocalDateTime.now());
		when(jobExecution.getEndTime()).thenReturn(LocalDateTime.now());
		when(jobExecution.getCreateTime()).thenReturn(LocalDateTime.now());
		when(jobExecution.getStepExecutions()).thenReturn(List.of(stepExecution));
		
		when(stepExecution.getStepName()).thenReturn("testStep");
		when(stepExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
		when(stepExecution.getReadCount()).thenReturn(100L);
		when(stepExecution.getWriteCount()).thenReturn(100L);
		when(stepExecution.getSkipCount()).thenReturn(0L);

		ResponseEntity<Map<String, Object>> response = batchController.listJobs(50);

		assertEquals(200, response.getStatusCode().value());
		assertTrue(response.getBody().containsKey("jobs"));
		assertEquals(1, ((List<?>) response.getBody().get("jobs")).size());
	}

	@Test
	void testGetJobMetrics_WithValidMetrics_ReturnsFullMetrics() {
		BatchMetrics metrics = mock(BatchMetrics.class);
		when(metrics.getStartTime()).thenReturn(LocalDateTime.now().minusMinutes(5));
		when(metrics.getEndTime()).thenReturn(LocalDateTime.now());
		when(metrics.getTotalRecordsRead()).thenReturn(100L);
		when(metrics.getTotalRecordsWritten()).thenReturn(95L);
		when(metrics.getTotalSkips()).thenReturn(5);
		when(metrics.getTotalRetryAttempts()).thenReturn(2);
		when(metrics.getPartitionMetrics()).thenReturn(Map.of());
		when(metrics.getRetryDetails()).thenReturn(List.of());
		when(metrics.getSkipDetails()).thenReturn(List.of());

		when(jobExecution.getId()).thenReturn(1L);
		when(jobExecution.getJobInstance()).thenReturn(jobInstance);
		when(jobInstance.getJobName()).thenReturn("testJob");
		when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
		when(jobExplorer.getJobExecution(1L)).thenReturn(jobExecution);
		when(metricsCollector.getJobMetrics(1L)).thenReturn(metrics);

		ResponseEntity<Map<String, Object>> response = batchController.getJobMetrics(1L);

		assertEquals(200, response.getStatusCode().value());
		assertTrue(response.getBody().containsKey("duration"));
		assertEquals(100L, response.getBody().get("totalRecordsRead"));
		assertEquals(95L, response.getBody().get("totalRecordsWritten"));
		assertEquals(5, response.getBody().get("totalRecordsSkipped"));
		assertEquals(2, response.getBody().get("totalRetryAttempts"));
	}

	@Test
	void testGetJobMetrics_WithRunningJob_ShowsRunningDuration() {
		BatchMetrics metrics = mock(BatchMetrics.class);
		when(metrics.getStartTime()).thenReturn(LocalDateTime.now().minusMinutes(5));
		when(metrics.getEndTime()).thenReturn(null); // Job still running
		when(metrics.getTotalRecordsRead()).thenReturn(50L);

		when(jobExecution.getId()).thenReturn(1L);
		when(jobExecution.getJobInstance()).thenReturn(jobInstance);
		when(jobInstance.getJobName()).thenReturn("testJob");
		when(jobExecution.getStatus()).thenReturn(BatchStatus.STARTED);
		when(jobExplorer.getJobExecution(1L)).thenReturn(jobExecution);
		when(metricsCollector.getJobMetrics(1L)).thenReturn(metrics);

		ResponseEntity<Map<String, Object>> response = batchController.getJobMetrics(1L);

		assertEquals(200, response.getStatusCode().value());
		assertEquals("Job still running", response.getBody().get("duration"));
		assertEquals(50L, response.getBody().get("totalRecordsRead"));
	}

}
