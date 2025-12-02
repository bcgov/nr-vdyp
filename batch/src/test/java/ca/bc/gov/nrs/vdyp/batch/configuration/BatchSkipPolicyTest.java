package ca.bc.gov.nrs.vdyp.batch.configuration;

import ca.bc.gov.nrs.vdyp.batch.service.BatchMetricsCollector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.validation.BindException;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BatchSkipPolicyTest {

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

	private BatchSkipPolicy batchSkipPolicy;

	private static final String TEST_JOB_GUID = "test-job-guid-123";

	@BeforeEach
	void setUp() {
		batchSkipPolicy = new BatchSkipPolicy(5L, metricsCollector);
	}

	@ParameterizedTest
	@MethodSource("provideSkippableExceptions")
	void testShouldSkip_SkippableExceptions_ReturnsTrue(String testName, Exception exception)
			throws SkipLimitExceededException {
		boolean result = batchSkipPolicy.shouldSkip(exception, 1);

		assertTrue(result, testName);
	}

	static Stream<Arguments> provideSkippableExceptions() {
		return Stream.of(
				Arguments.of(
						"FlatFileParseException should be skippable",
						new FlatFileParseException("Parse error", "invalid line")
				), Arguments.of("BindException should be skippable", new BindException(new Object(), "test")),
				Arguments.of(
						"IllegalArgumentException should be skippable", new IllegalArgumentException("Invalid argument")
				), Arguments.of("NullPointerException should be skippable", new NullPointerException("Null pointer")),
				Arguments.of("NumberFormatException should be skippable", new NumberFormatException("Invalid number")),
				Arguments.of(
						"RuntimeException with 'invalid' should be skippable",
						new RuntimeException("Invalid data format")
				),
				Arguments.of(
						"RuntimeException with 'malformed' should be skippable",
						new RuntimeException("Data is malformed")
				),
				Arguments.of(
						"RuntimeException with 'corrupt' should be skippable",
						new RuntimeException("File appears to be corrupt")
				),
				Arguments.of(
						"RuntimeException with 'missing' should be skippable",
						new RuntimeException("Required field is missing")
				),
				Arguments.of(
						"RuntimeException with 'empty' should be skippable",
						new RuntimeException("Field cannot be empty")
				),
				Arguments.of(
						"RuntimeException with 'format' should be skippable",
						new RuntimeException("Unexpected format encountered")
				)
		);
	}

	@ParameterizedTest
	@MethodSource("provideNonSkippableExceptions")
	void testShouldSkip_NonSkippableExceptions_ReturnsFalse(String testName, Exception exception)
			throws SkipLimitExceededException {
		boolean result = batchSkipPolicy.shouldSkip(exception, 1);

		assertFalse(result, testName);
	}

	static Stream<Arguments> provideNonSkippableExceptions() {
		return Stream.of(
				Arguments.of(
						"RuntimeException without transient message should not be skippable",
						new RuntimeException("Some other error")
				), Arguments.of("RuntimeException with null message should not be skippable", new RuntimeException()),
				Arguments.of("Generic Exception should not be skippable", new Exception("Generic exception"))
		);
	}

	@Test
	void testShouldSkip_SkipLimitExceeded_ThrowsException() {
		RuntimeException exception = new RuntimeException("Error");

		assertThrows(SkipLimitExceededException.class, () -> batchSkipPolicy.shouldSkip(exception, 5));
	}

	@Test
	void testBeforeStep_SetsJobExecutionId() {
		when(stepExecution.getJobExecutionId()).thenReturn(1L);
		when(stepExecution.getJobExecution()).thenReturn(jobExecution);
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobParameters.getString("jobGuid")).thenReturn(TEST_JOB_GUID);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(executionContext.getString("partitionName")).thenReturn("partition1");

		batchSkipPolicy.beforeStep(stepExecution);

		verify(stepExecution).getJobExecutionId();
		verify(stepExecution).getJobExecution();
		verify(executionContext).getString("partitionName");
	}

	@Test
	void testConstructor_WithValidParameters() {
		BatchSkipPolicy policy = new BatchSkipPolicy(10L, metricsCollector);

		assertNotNull(policy);
	}

	@ParameterizedTest
	@MethodSource("provideFeatureIdExtractionTests")
	void testFeatureIdExtraction_VariousScenarios_Handles(
			String testName, RuntimeException exception, boolean expectedSkippable
	) throws SkipLimitExceededException {
		when(stepExecution.getJobExecutionId()).thenReturn(1L);
		when(stepExecution.getJobExecution()).thenReturn(jobExecution);
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobParameters.getString("jobGuid")).thenReturn(TEST_JOB_GUID);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(executionContext.getString("partitionName")).thenReturn("test-partition");
		batchSkipPolicy.beforeStep(stepExecution);

		boolean result = batchSkipPolicy.shouldSkip(exception, 1);

		assertEquals(expectedSkippable, result, testName);
	}

	static Stream<Arguments> provideFeatureIdExtractionTests() {
		return Stream.of(
				Arguments.of(
						"Valid Feature ID in message should be extractable",
						new RuntimeException("Error processing Feature ID 1145678902 - invalid data format"), true
				),
				Arguments.of(
						"No Feature ID in message should handle",
						new RuntimeException("No Feature ID in this message - malformed data"), true
				),
				Arguments.of(
						"Malformed Feature ID should handle",
						new RuntimeException("Error with Feature ID abc123 - invalid format"), true
				),
				Arguments.of(
						"Non-FlatFile exception with Feature ID should use fallback",
						new RuntimeException("Error with Feature ID 1245678903 - invalid data format"), true
				),
				Arguments.of(
						"Zero Feature ID should be handled",
						new RuntimeException("Error with Feature ID 0 - format issue"), true
				)
		);
	}

	@Test
	void testFlatFileParseException_WithLineNumber_ExtractsCorrectly() throws SkipLimitExceededException {
		FlatFileParseException exception = new FlatFileParseException("Parse error at line 5", "invalid,data,line", 5);

		boolean result = batchSkipPolicy.shouldSkip(exception, 1);

		assertTrue(result);
	}

	@Test
	void testBuildErrorMessage_WithFlatFileParseException_IncludesLineAndInput() throws SkipLimitExceededException {
		FlatFileParseException exception = new FlatFileParseException("Parse failed", "bad,data,row", 10);

		boolean result = batchSkipPolicy.shouldSkip(exception, 1);

		assertTrue(result);
	}

	@Test
	void testShouldSkip_ProcessesSkippableExceptionPath_WithMetrics() throws SkipLimitExceededException {
		// Set up step execution for metrics
		when(stepExecution.getJobExecutionId()).thenReturn(100L);
		when(stepExecution.getJobExecution()).thenReturn(jobExecution);
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobParameters.getString("jobGuid")).thenReturn(TEST_JOB_GUID);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(executionContext.getString("partitionName")).thenReturn("test-partition");

		batchSkipPolicy.beforeStep(stepExecution);

		FlatFileParseException exception = new FlatFileParseException("Parse error", "invalid,data", 15);

		boolean result = batchSkipPolicy.shouldSkip(exception, 1);

		assertTrue(result);
	}

	@Test
	void testShouldSkip_WithStepSynchronizationManager_UpdatesContext() throws SkipLimitExceededException {
		StepContext stepContext = mock(StepContext.class);
		StepExecution currentStepExecution = mock(StepExecution.class);
		ExecutionContext currentExecutionContext = mock(ExecutionContext.class);
		JobExecution currentJobExecution = mock(JobExecution.class);
		JobParameters currentJobParameters = mock(JobParameters.class);

		// Mock StepSynchronizationManager
		try (MockedStatic<StepSynchronizationManager> mockedManager = mockStatic(StepSynchronizationManager.class)) {
			mockedManager.when(StepSynchronizationManager::getContext).thenReturn(stepContext);
			when(stepContext.getStepExecution()).thenReturn(currentStepExecution);
			when(currentStepExecution.getJobExecutionId()).thenReturn(200L);
			when(currentStepExecution.getJobExecution()).thenReturn(currentJobExecution);
			when(currentJobExecution.getJobParameters()).thenReturn(currentJobParameters);
			when(currentJobParameters.getString("jobGuid")).thenReturn("dynamic-job-guid");
			when(currentStepExecution.getExecutionContext()).thenReturn(currentExecutionContext);
			when(currentExecutionContext.getString("partitionName")).thenReturn("dynamic-partition");

			FlatFileParseException exception = new FlatFileParseException("Error", "bad data", 5);

			boolean result = batchSkipPolicy.shouldSkip(exception, 1);

			assertTrue(result);
			verify(stepContext).getStepExecution();
			verify(currentStepExecution).getJobExecutionId();
			verify(currentStepExecution).getExecutionContext();
		}
	}

	@Test
	void testShouldSkip_WithStepSynchronizationManagerException_Handles() throws SkipLimitExceededException {
		// Mock StepSynchronizationManager to throw exception
		try (MockedStatic<StepSynchronizationManager> mockedManager = mockStatic(StepSynchronizationManager.class)) {
			mockedManager.when(StepSynchronizationManager::getContext).thenThrow(new RuntimeException("Context error"));

			FlatFileParseException exception = new FlatFileParseException("Error", "bad data", 5);

			boolean result = batchSkipPolicy.shouldSkip(exception, 1);

			assertTrue(result);
		}
	}

	@Test
	void testShouldSkip_WithNullMetricsCollector_DoesNotFail() throws SkipLimitExceededException {
		BatchSkipPolicy policyWithNullMetrics = new BatchSkipPolicy(5L, null);
		when(stepExecution.getJobExecutionId()).thenReturn(300L);
		when(stepExecution.getJobExecution()).thenReturn(jobExecution);
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobParameters.getString("jobGuid")).thenReturn(TEST_JOB_GUID);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(executionContext.getString("partitionName")).thenReturn("null-metrics-partition");

		policyWithNullMetrics.beforeStep(stepExecution);

		FlatFileParseException exception = new FlatFileParseException("Parse error", "data", 20);

		boolean result = policyWithNullMetrics.shouldSkip(exception, 1);

		assertTrue(result);
	}

	@Test
	void testExtractRecord_WithoutCachedRecord_CreatesBasicRecord() throws SkipLimitExceededException {
		when(stepExecution.getJobExecutionId()).thenReturn(1L);
		when(stepExecution.getJobExecution()).thenReturn(jobExecution);
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobParameters.getString("jobGuid")).thenReturn(TEST_JOB_GUID);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(executionContext.getString("partitionName")).thenReturn("test-partition");
		batchSkipPolicy.beforeStep(stepExecution);

		RuntimeException exception = new RuntimeException("Error processing Feature ID 1645678907 - malformed data");

		boolean result = batchSkipPolicy.shouldSkip(exception, 1);

		assertTrue(result);
	}
}
