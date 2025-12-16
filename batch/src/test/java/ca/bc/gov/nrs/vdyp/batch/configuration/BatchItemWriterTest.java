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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ca.bc.gov.nrs.vdyp.batch.exception.BatchConfigurationException;
import ca.bc.gov.nrs.vdyp.batch.exception.BatchException;
import ca.bc.gov.nrs.vdyp.batch.model.BatchChunkMetadata;
import ca.bc.gov.nrs.vdyp.batch.service.BatchProjectionService;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BatchItemWriterTest {

	private static final Long TEST_JOB_EXECUTION_ID = 12345L;
	private static final String TEST_JOB_GUID = "7c26643a-50cb-497e-a539-afac6966ecea";
	private static final String TEST_PARTITION_NAME = "partition0";
	private static final String VALID_PARAMETERS_JSON = "{\"selectedExecutionOptions\":[]}";

	@Mock
	private BatchProjectionService batchProjectionService;

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

	private BatchItemWriter writer;
	private Parameters mockParameters;

	@BeforeEach
	void setUp() throws JsonProcessingException {
		// Create a mock Parameters object to be returned by ObjectMapper
		mockParameters = mock(Parameters.class);

		// Configure ObjectMapper to return the mock Parameters when reading the valid JSON
		when(objectMapper.readValue(VALID_PARAMETERS_JSON, Parameters.class)).thenReturn(mockParameters);

		writer = new BatchItemWriter(batchProjectionService, objectMapper);
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
	void testBeforeStep_missingParameters_throwsException() throws JsonProcessingException {
		JobParameters params = new JobParametersBuilder().addString("jobGuid", TEST_JOB_GUID)
				.addString("jobBaseDir", "/tmp/test").toJobParameters();

		when(objectMapper.readValue((String) null, Parameters.class)).thenReturn(null);

		when(stepExecution.getJobExecutionId()).thenReturn(TEST_JOB_EXECUTION_ID);
		when(stepExecution.getJobExecution()).thenReturn(jobExecution);
		when(jobExecution.getJobParameters()).thenReturn(params);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(stepExecution.getJobParameters()).thenReturn(params);
		when(executionContext.getString("partitionName")).thenReturn(TEST_PARTITION_NAME);

		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
			writer.beforeStep(stepExecution);
		});

		assertTrue(exception.getMessage().contains("Deserialized projection parameters are null"));
	}

	@Test
	void testAfterStep() {
		ExitStatus mockExitStatus = ExitStatus.COMPLETED;
		when(stepExecution.getExitStatus()).thenReturn(mockExitStatus);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(executionContext.getString("partitionName")).thenReturn(TEST_PARTITION_NAME);

		ExitStatus result = writer.afterStep(stepExecution);

		assertEquals(mockExitStatus, result);
		verify(stepExecution).getExitStatus();
		verify(stepExecution).getExecutionContext();
	}

	@Test
	void testWrite_emptyChunk() throws BatchException {
		setupWriterWithValidParameters();
		Chunk<BatchChunkMetadata> emptyChunk = new Chunk<>();

		assertDoesNotThrow(() -> writer.write(emptyChunk));

		verify(batchProjectionService, never()).performProjectionForChunk(any(), any(), any(), any());
	}

	@Test
	void testWrite_successfulProcessing() throws BatchException {
		setupWriterWithValidParameters();
		BatchChunkMetadata chunkMetadata = createMockChunkMetadata(TEST_PARTITION_NAME, 2);
		Chunk<BatchChunkMetadata> chunk = new Chunk<>(Arrays.asList(chunkMetadata));

		when(batchProjectionService.performProjectionForChunk(any(), any(), any(), any()))
				.thenReturn("projection result");

		assertDoesNotThrow(() -> writer.write(chunk));

		verify(batchProjectionService).performProjectionForChunk(
				eq(chunkMetadata), any(Parameters.class), eq(TEST_JOB_EXECUTION_ID), eq(TEST_JOB_GUID)
		);
	}

	@Test
	void testWrite_WhenProjectionServiceThrows_PropagatesException() throws BatchException {
		setupWriterWithValidParameters();
		BatchChunkMetadata chunkMetadata = createMockChunkMetadata(TEST_PARTITION_NAME, 1);
		Chunk<BatchChunkMetadata> chunk = new Chunk<>(Arrays.asList(chunkMetadata));

		RuntimeException testException = new RuntimeException("Test projection failure");
		when(batchProjectionService.performProjectionForChunk(any(), any(), any(), any())).thenThrow(testException);

		RuntimeException thrownException = assertThrows(RuntimeException.class, () -> {
			writer.write(chunk);
		});

		assertEquals(testException, thrownException);
		verify(batchProjectionService).performProjectionForChunk(any(), any(), any(), any());
	}

	@Test
	void testWrite_nullProjectionParameters_throwsException() {
		BatchChunkMetadata chunkMetadata = createMockChunkMetadata(TEST_PARTITION_NAME, 1);
		Chunk<BatchChunkMetadata> chunk = new Chunk<>(Arrays.asList(chunkMetadata));

		BatchConfigurationException exception = assertThrows(BatchConfigurationException.class, () -> {
			writer.write(chunk);
		});

		assertTrue(exception.getMessage().contains("VDYP projection parameters are null"));
	}

	@Test
	void testBeforeStep_calledTwice_throwsIllegalStateException() {
		JobParameters params = new JobParametersBuilder().addString("projectionParametersJson", VALID_PARAMETERS_JSON)
				.addString("jobGuid", TEST_JOB_GUID).addString("jobBaseDir", "/tmp/test").toJobParameters();

		when(stepExecution.getJobExecutionId()).thenReturn(TEST_JOB_EXECUTION_ID);
		when(stepExecution.getJobExecution()).thenReturn(jobExecution);
		when(jobExecution.getJobParameters()).thenReturn(params);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(stepExecution.getJobParameters()).thenReturn(params);
		when(executionContext.getString("partitionName")).thenReturn(TEST_PARTITION_NAME);

		assertDoesNotThrow(() -> writer.beforeStep(stepExecution));

		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
			writer.beforeStep(stepExecution);
		});

		assertTrue(exception.getMessage().contains("already initialized"));
		assertTrue(exception.getMessage().contains("beforeStep() called multiple times"));
	}

	@Test
	void testBeforeStep_jsonProcessingException_throwsIllegalStateException() throws JsonProcessingException {
		String invalidJson = "{invalid json}";
		JobParameters params = new JobParametersBuilder().addString("projectionParametersJson", invalidJson)
				.addString("jobGuid", TEST_JOB_GUID).addString("jobBaseDir", "/tmp/test").toJobParameters();

		when(objectMapper.readValue(invalidJson, Parameters.class))
				.thenThrow(new JsonProcessingException("Unexpected character") {
				});

		when(stepExecution.getJobExecutionId()).thenReturn(TEST_JOB_EXECUTION_ID);
		when(stepExecution.getJobExecution()).thenReturn(jobExecution);
		when(jobExecution.getJobParameters()).thenReturn(params);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(stepExecution.getJobParameters()).thenReturn(params);
		when(executionContext.getString("partitionName")).thenReturn(TEST_PARTITION_NAME);

		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
			writer.beforeStep(stepExecution);
		});

		assertTrue(exception.getMessage().contains("JSON parsing failed during parameter deserialization"));
		assertTrue(exception.getMessage().contains(TEST_JOB_GUID));
		assertTrue(exception.getMessage().contains("Exception type:"));
		assertNotNull(exception.getCause());
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

	private BatchChunkMetadata createMockChunkMetadata(String partitionName, int recordCount) {
		return new BatchChunkMetadata(partitionName, "/tmp/test-job", 0L, recordCount, 0L, 0);
	}
}
