package ca.bc.gov.nrs.vdyp.batch.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
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
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

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

	private BatchController batchController;

	@BeforeEach
	void setUp() {
		batchController = new BatchController(
				jobLauncher, partitionedJob, jobExplorer, metricsCollector, csvPartitioner
		);
		// Set @Value annotated fields using reflection for unit testing
		ReflectionTestUtils.setField(batchController, "batchRootDirectory", "/tmp/batch");
		ReflectionTestUtils.setField(batchController, "defaultParitionSize", 4);
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
		when(jobLauncher.run(any(), any())).thenReturn(jobExecution);

		ResponseEntity<Map<String, Object>> response = batchController
				.startBatchJobWithFiles(polygonFile, layerFile, 4, "{}");

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
				.startBatchJobWithFiles(polygonFile, layerFile, 4, "{}");

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
				.startBatchJobWithFiles(polygonFile, layerFile, 4, null);

		assertEquals(400, response.getStatusCode().value());
		assertTrue(response.getBody().containsKey("error"));
	}

	@Test
	void testStartBatchJobWithFiles_WithNullPartitionedJob_ReturnsJobNotAvailable() {
		// Create controller with null job
		BatchController controllerWithNullJob = new BatchController(
				jobLauncher, null, jobExplorer, metricsCollector, csvPartitioner
		);
		ReflectionTestUtils.setField(controllerWithNullJob, "batchRootDirectory", "/tmp/batch");
		ReflectionTestUtils.setField(controllerWithNullJob, "defaultParitionSize", 4);

		MockMultipartFile polygonFile = new MockMultipartFile(
				"polygonFile", "polygon.csv", "text/csv", "FEATURE_ID\n123".getBytes()
		);
		MockMultipartFile layerFile = new MockMultipartFile(
				"layerFile", "layer.csv", "text/csv", "FEATURE_ID\n123".getBytes()
		);

		ResponseEntity<Map<String, Object>> response = controllerWithNullJob
				.startBatchJobWithFiles(polygonFile, layerFile, 4, "{}");

		assertEquals(200, response.getStatusCode().value());
		assertEquals("VDYP Batch job not available", response.getBody().get("message"));
		assertEquals("JOB_NOT_AVAILABLE", response.getBody().get("status"));
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
				.startBatchJobWithFiles(polygonFile, layerFile, 4, "   ");

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
		when(jobLauncher.run(any(), any())).thenReturn(jobExecution);

		// Call without partition size to use default
		ResponseEntity<Map<String, Object>> response = batchController
				.startBatchJobWithFiles(polygonFile, layerFile, null, "{}");

		assertEquals(200, response.getStatusCode().value());
		assertTrue(response.getBody().containsKey("jobExecutionId"));
	}
}
