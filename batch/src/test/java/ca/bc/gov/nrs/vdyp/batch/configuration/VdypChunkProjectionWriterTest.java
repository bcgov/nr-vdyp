package ca.bc.gov.nrs.vdyp.batch.configuration;

import ca.bc.gov.nrs.vdyp.batch.model.BatchRecord;
import ca.bc.gov.nrs.vdyp.batch.service.BatchMetricsCollector;
import ca.bc.gov.nrs.vdyp.batch.service.VdypProjectionService;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VdypChunkProjectionWriterTest {

	private static final Long TEST_JOB_EXECUTION_ID = 12345L;
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
	private JobParameters jobParameters;

	private VdypChunkProjectionWriter writer;

	@BeforeEach
	void setUp() {
		writer = new VdypChunkProjectionWriter(vdypProjectionService, metricsCollector);
	}

	@Test
	void testConstructor() {
		assertNotNull(writer);
	}

	@Test
	void testBeforeStep_success() {
		JobParameters params = new JobParametersBuilder().addString("projectionParametersJson", VALID_PARAMETERS_JSON)
				.toJobParameters();

		when(stepExecution.getJobExecutionId()).thenReturn(TEST_JOB_EXECUTION_ID);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(stepExecution.getJobParameters()).thenReturn(params);
		when(executionContext.getString("partitionName", "unknown")).thenReturn(TEST_PARTITION_NAME);

		assertDoesNotThrow(() -> writer.beforeStep(stepExecution));

		verify(stepExecution).getJobExecutionId();
		verify(stepExecution).getExecutionContext();
		verify(stepExecution, atLeastOnce()).getJobParameters();
	}

	@Test
	void testBeforeStep_missingParameters_throwsException() {
		JobParameters params = new JobParametersBuilder().toJobParameters();

		when(stepExecution.getJobExecutionId()).thenReturn(TEST_JOB_EXECUTION_ID);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(stepExecution.getJobParameters()).thenReturn(params);
		when(executionContext.getString("partitionName", "unknown")).thenReturn(TEST_PARTITION_NAME);

		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
			writer.beforeStep(stepExecution);
		});

		// Error message contains partition name
		assertTrue(exception.getMessage().contains(TEST_PARTITION_NAME));
	}

	@Test
	void testBeforeStep_emptyParameters_throwsException() {
		JobParameters params = new JobParametersBuilder().addString("projectionParametersJson", "").toJobParameters();

		when(stepExecution.getJobExecutionId()).thenReturn(TEST_JOB_EXECUTION_ID);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(stepExecution.getJobParameters()).thenReturn(params);
		when(executionContext.getString("partitionName", "unknown")).thenReturn(TEST_PARTITION_NAME);

		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
			writer.beforeStep(stepExecution);
		});

		// Error message contains partition name
		assertTrue(exception.getMessage().contains(TEST_PARTITION_NAME));
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

		verify(vdypProjectionService, never()).performProjectionForChunk(any(), any(), any(), any(), any());
	}

	@Test
	void testWrite_successfulProcessing() throws Exception {
		setupWriterWithValidParameters();
		List<BatchRecord> records = Arrays.asList(createMockBatchRecord("feature1"), createMockBatchRecord("feature2"));
		Chunk<BatchRecord> chunk = new Chunk<>(records);

		when(vdypProjectionService.performProjectionForChunk(any(), any(), any(), any(), any()))
				.thenReturn("projection result");

		assertDoesNotThrow(() -> writer.write(chunk));

		verify(vdypProjectionService).performProjectionForChunk(
				eq(records), eq(TEST_PARTITION_NAME), any(Parameters.class), eq(TEST_JOB_EXECUTION_ID), any()
		);
	}

	@Test
	void testWrite_failedProcessing_rethrowsException() throws Exception {
		setupWriterWithValidParameters();
		List<BatchRecord> records = Arrays.asList(createMockBatchRecord("feature1"));
		Chunk<BatchRecord> chunk = new Chunk<>(records);

		Exception testException = new RuntimeException("Test projection failure");
		when(vdypProjectionService.performProjectionForChunk(any(), any(), any(), any(), any()))
				.thenThrow(testException);

		Exception thrownException = assertThrows(RuntimeException.class, () -> {
			writer.write(chunk);
		});

		assertEquals(testException, thrownException);
		verify(vdypProjectionService).performProjectionForChunk(any(), any(), any(), any(), any());
		verify(metricsCollector).recordSkip(
				eq(TEST_JOB_EXECUTION_ID), anyLong(), any(BatchRecord.class), eq(testException),
				eq(TEST_PARTITION_NAME), isNull()
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

		when(vdypProjectionService.performProjectionForChunk(any(), any(), any(), any(), any()))
				.thenReturn("projection result");

		writer.write(chunk);

		// Verifies that the step partition name is used when record partition is null
		verify(vdypProjectionService).performProjectionForChunk(
				eq(records), eq(TEST_PARTITION_NAME), any(Parameters.class), eq(TEST_JOB_EXECUTION_ID), any()
		);
	}

	private void setupWriterWithValidParameters() {
		JobParameters params = new JobParametersBuilder().addString("projectionParametersJson", VALID_PARAMETERS_JSON)
				.toJobParameters();

		when(stepExecution.getJobExecutionId()).thenReturn(TEST_JOB_EXECUTION_ID);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(stepExecution.getJobParameters()).thenReturn(params);
		when(executionContext.getString("partitionName", "unknown")).thenReturn(TEST_PARTITION_NAME);

		writer.beforeStep(stepExecution);
	}

	private BatchRecord createMockBatchRecord(String featureId) {
		BatchRecord batchRecord = mock(BatchRecord.class);
		when(batchRecord.getFeatureId()).thenReturn(featureId);
		when(batchRecord.getPartitionName()).thenReturn(null);
		return batchRecord;
	}
}
