package ca.bc.gov.nrs.vdyp.batch.configuration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.bc.gov.nrs.vdyp.batch.model.BatchRecord;
import ca.bc.gov.nrs.vdyp.batch.service.BatchMetricsCollector;
import ca.bc.gov.nrs.vdyp.batch.service.VdypProjectionService;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VdypChunkProjectionWriterTest {

	private static final Long TEST_JOB_EXECUTION_ID = 12345L;
	private static final String TEST_JOB_GUID = "7c26643a-50cb-497e-a539-afac6966ecea";
	private static final String TEST_PARTITION_NAME = "partition0";
	private static final String VALID_PARAMETERS_JSON = "{\"selectedExecutionOptions\":[]}";

	@Mock
	private VdypProjectionService vdypProjectionService;

	@Mock
	private BatchMetricsCollector metricsCollector;

	@Mock
	private StepExecution stepExecution;

	@Mock
	private ExecutionContext executionContext;

	@Mock
	private JobExecution jobExecution;

	@Mock
	private JobParameters jobParameters;

	@Mock
	private ObjectMapper objectMapper;

	private VdypChunkProjectionWriter writer;
	private Parameters mockParameters;

	@BeforeEach
	void setUp() throws Exception {
		// Create a mock Parameters object to be returned by ObjectMapper
		mockParameters = mock(Parameters.class);

		// Configure ObjectMapper to return the mock Parameters when reading the valid JSON
		when(objectMapper.readValue(VALID_PARAMETERS_JSON, Parameters.class)).thenReturn(mockParameters);

		writer = new VdypChunkProjectionWriter(vdypProjectionService, metricsCollector, objectMapper);
	}

	@Test
	void testConstructor() {
		assertNotNull(writer);
	}

	@Test
	void testBeforeStep_success() {
		JobParameters params = new JobParametersBuilder().addString("projectionParametersJson", VALID_PARAMETERS_JSON)
				.addString("jobGuid", TEST_JOB_GUID).addString("jobBaseDir", "/tmp/test").toJobParameters();

		when(stepExecution.getJobExecutionId()).thenReturn(TEST_JOB_EXECUTION_ID);
		when(stepExecution.getJobExecution()).thenReturn(jobExecution);
		when(jobExecution.getJobParameters()).thenReturn(params);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(stepExecution.getJobParameters()).thenReturn(params);
		when(executionContext.getString("partitionName")).thenReturn(TEST_PARTITION_NAME);

		assertDoesNotThrow(() -> writer.beforeStep(stepExecution));

		verify(stepExecution).getJobExecutionId();
		verify(stepExecution).getJobExecution();
		verify(stepExecution).getExecutionContext();
		verify(stepExecution, atLeastOnce()).getJobParameters();
	}

	@Test
	void testBeforeStep_missingParameters_throwsException() {
		JobParameters params = new JobParametersBuilder().addString("jobGuid", TEST_JOB_GUID)
				.addString("jobBaseDir", "/tmp/test").toJobParameters();

		when(stepExecution.getJobExecutionId()).thenReturn(TEST_JOB_EXECUTION_ID);
		when(stepExecution.getJobExecution()).thenReturn(jobExecution);
		when(jobExecution.getJobParameters()).thenReturn(params);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(stepExecution.getJobParameters()).thenReturn(params);
		when(executionContext.getString("partitionName")).thenReturn(TEST_PARTITION_NAME);

		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
			writer.beforeStep(stepExecution);
		});

		assertTrue(exception.getMessage().contains(TEST_JOB_GUID));
	}

	@Test
	void testBeforeStep_emptyParameters_throwsException() {
		JobParameters params = new JobParametersBuilder().addString("projectionParametersJson", "")
				.addString("jobGuid", TEST_JOB_GUID).addString("jobBaseDir", "/tmp/test").toJobParameters();

		when(stepExecution.getJobExecutionId()).thenReturn(TEST_JOB_EXECUTION_ID);
		when(stepExecution.getJobExecution()).thenReturn(jobExecution);
		when(jobExecution.getJobParameters()).thenReturn(params);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(stepExecution.getJobParameters()).thenReturn(params);
		when(executionContext.getString("partitionName")).thenReturn(TEST_PARTITION_NAME);

		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
			writer.beforeStep(stepExecution);
		});

		assertTrue(exception.getMessage().contains(TEST_JOB_GUID));
	}

	@Test
	void testAfterStep() {
		ExitStatus mockExitStatus = ExitStatus.COMPLETED;
		when(stepExecution.getExitStatus()).thenReturn(mockExitStatus);

		ExitStatus result = writer.afterStep(stepExecution);

		assertEquals(mockExitStatus, result);
		verify(stepExecution).getExitStatus();
	}

	@Test
	void testWrite_emptyChunk() throws Exception {
		setupWriterWithValidParameters();
		Chunk<BatchRecord> emptyChunk = new Chunk<>();

		assertDoesNotThrow(() -> writer.write(emptyChunk));

		verify(vdypProjectionService, never()).performProjectionForChunk(any(), any(), any(), any(), any(), any());
	}

	@Test
	void testWrite_successfulProcessing() throws Exception {
		setupWriterWithValidParameters();
		List<BatchRecord> records = Arrays.asList(createMockBatchRecord("feature1"), createMockBatchRecord("feature2"));
		Chunk<BatchRecord> chunk = new Chunk<>(records);

		when(vdypProjectionService.performProjectionForChunk(any(), any(), any(), any(), any(), any()))
				.thenReturn("projection result");

		assertDoesNotThrow(() -> writer.write(chunk));

		verify(vdypProjectionService).performProjectionForChunk(
				eq(records), eq(TEST_PARTITION_NAME), any(Parameters.class), eq(TEST_JOB_EXECUTION_ID),
				eq(TEST_JOB_GUID), any()
		);
	}

	@Test
	void testWrite_failedProcessing_rethrowsException() throws Exception {
		setupWriterWithValidParameters();
		List<BatchRecord> records = Arrays.asList(createMockBatchRecord("feature1"));
		Chunk<BatchRecord> chunk = new Chunk<>(records);

		Exception testException = new RuntimeException("Test projection failure");
		when(vdypProjectionService.performProjectionForChunk(any(), any(), any(), any(), any(), any()))
				.thenThrow(testException);

		Exception thrownException = assertThrows(RuntimeException.class, () -> {
			writer.write(chunk);
		});

		assertEquals(testException, thrownException);
		verify(vdypProjectionService).performProjectionForChunk(any(), any(), any(), any(), any(), any());
		verify(metricsCollector).recordSkip(
				eq(TEST_JOB_EXECUTION_ID), eq(TEST_JOB_GUID), any(), eq(testException), eq(TEST_PARTITION_NAME)
		);
	}

	@Test
	void testWrite_nullProjectionParameters_throwsException() {
		List<BatchRecord> records = Arrays.asList(createMockBatchRecord("feature1"));
		Chunk<BatchRecord> chunk = new Chunk<>(records);

		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
			writer.write(chunk);
		});

		assertTrue(exception.getMessage().contains("VDYP projection parameters are null"));
	}

	@Test
	void testWrite_usesRecordPartitionName() throws Exception {
		setupWriterWithValidParameters();
		BatchRecord recordWithPartition = createMockBatchRecord("feature1");
		// Record partition name is null, so it uses the step partition name (TEST_PARTITION_NAME)

		List<BatchRecord> records = Arrays.asList(recordWithPartition);
		Chunk<BatchRecord> chunk = new Chunk<>(records);

		when(vdypProjectionService.performProjectionForChunk(any(), any(), any(), any(), any(), any()))
				.thenReturn("projection result");

		writer.write(chunk);

		// Verifies that the step partition name is used when record partition is null
		verify(vdypProjectionService).performProjectionForChunk(
				eq(records), eq(TEST_PARTITION_NAME), any(Parameters.class), eq(TEST_JOB_EXECUTION_ID),
				eq(TEST_JOB_GUID), any()
		);
	}

	private void setupWriterWithValidParameters() {
		JobParameters params = new JobParametersBuilder().addString("projectionParametersJson", VALID_PARAMETERS_JSON)
				.addString("jobGuid", TEST_JOB_GUID).addString("jobBaseDir", "/tmp/test").toJobParameters();

		when(stepExecution.getJobExecutionId()).thenReturn(TEST_JOB_EXECUTION_ID);
		when(stepExecution.getJobExecution()).thenReturn(jobExecution);
		when(jobExecution.getJobParameters()).thenReturn(params);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(stepExecution.getJobParameters()).thenReturn(params);
		when(executionContext.getString("partitionName")).thenReturn(TEST_PARTITION_NAME);

		writer.beforeStep(stepExecution);
	}

	private BatchRecord createMockBatchRecord(String featureId) {
		BatchRecord batchRecord = mock(BatchRecord.class);
		when(batchRecord.getFeatureId()).thenReturn(featureId);
		when(batchRecord.getPartitionName()).thenReturn(null);
		return batchRecord;
	}
}
