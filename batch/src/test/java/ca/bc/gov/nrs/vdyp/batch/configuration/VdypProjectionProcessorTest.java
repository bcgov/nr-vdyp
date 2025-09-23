package ca.bc.gov.nrs.vdyp.batch.configuration;

import ca.bc.gov.nrs.vdyp.batch.model.BatchRecord;
import ca.bc.gov.nrs.vdyp.batch.model.Polygon;
import ca.bc.gov.nrs.vdyp.batch.model.Layer;
import ca.bc.gov.nrs.vdyp.batch.service.BatchMetricsCollector;
import ca.bc.gov.nrs.vdyp.batch.service.VdypProjectionService;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VdypProjectionProcessorTest {

	@Mock
	private BatchRetryPolicy retryPolicy;

	@Mock
	private BatchMetricsCollector metricsCollector;

	@Mock
	private VdypProjectionService vdypProjectionService;

	@Mock
	private Parameters parameters;

	@Mock
	private StepExecution stepExecution;

	@Mock
	private JobParameters jobParameters;

	@Mock
	private ExecutionContext executionContext;

	private VdypProjectionProcessor processor;

	@BeforeEach
	void setUp() {
		processor = new VdypProjectionProcessor(retryPolicy, metricsCollector, vdypProjectionService);

		// Set validation thresholds using reflection
		ReflectionTestUtils.setField(processor, "maxDataLength", 50000);
		ReflectionTestUtils.setField(processor, "minPolygonIdLength", 1);
		ReflectionTestUtils.setField(processor, "maxPolygonIdLength", 50);

		setupStepExecution();
	}

	private void setupStepExecution() {
		when(stepExecution.getJobExecutionId()).thenReturn(1L);
		when(executionContext.getString("partitionName", "unknown")).thenReturn("test-partition");
		when(executionContext.getLong("startLine", 0)).thenReturn(1L);
		when(executionContext.getLong("endLine", 0)).thenReturn(100L);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(stepExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobParameters.getString("projectionParametersJson")).thenReturn("{\"selectedExecutionOptions\": []}");
	}

	@Test
	void testConstructor() {
		assertNotNull(processor);
	}

	@Test
	void testBeforeStep_InitializesProcessor() {
		processor.beforeStep(stepExecution);

		verify(stepExecution).getJobExecutionId();
		verify(executionContext).getString("partitionName", "unknown");
		verify(executionContext).getLong("startLine", 0);
		verify(executionContext).getLong("endLine", 0);
		verify(metricsCollector).initializePartitionMetrics(1L, "test-partition", 1, 100);
	}

	@Test
	void testProcess_ValidRecord_ReturnsProcessedRecord() throws Exception {
		processor.beforeStep(stepExecution);

		// Mock VdypProjectionService to return a test result
		when(vdypProjectionService.performProjectionForRecord(any(BatchRecord.class), anyString(), any(Parameters.class)))
				.thenReturn("Test projection result");

		BatchRecord batchRecord = createValidBatchRecord();

		BatchRecord result = processor.process(batchRecord);

		assertNotNull(result);
		assertNotNull(result.getProjectionResult());
		assertEquals("Test projection result", result.getProjectionResult());
		verify(retryPolicy).registerRecord(anyLong(), eq(batchRecord));
	}

	@ParameterizedTest
	@MethodSource("provideInvalidRecords")
	void testProcess_ValidationErrors_ThrowIllegalArgumentException(
			String testName, String data, String featureId, String layerId
	) {
		processor.beforeStep(stepExecution);

		BatchRecord batchRecord = new BatchRecord();
		batchRecord.setFeatureId(featureId);

		// Only create polygon/layers if featureId is valid
		if (featureId != null && !featureId.trim().isEmpty()) {
			if (testName.contains("Missing polygon data")) {
				// Don't set polygon data
				batchRecord.setPolygon(null);
			} else if (testName.contains("Missing layer data")) {
				// Set polygon but no layers
				Polygon polygon = new Polygon();
				polygon.setFeatureId(featureId);
				polygon.setMapId("082G055");
				polygon.setPolygonNumber(1234L);
				batchRecord.setPolygon(polygon);
				batchRecord.setLayers(null);
			} else {
				// Create valid data structures
				Polygon polygon = new Polygon();
				polygon.setFeatureId(featureId);
				polygon.setMapId("082G055");
				polygon.setPolygonNumber(1234L);
				batchRecord.setPolygon(polygon);

				Layer layer = new Layer();
				layer.setFeatureId(featureId);
				layer.setMapId("082G055");
				layer.setPolygonNumber(1234L);
				batchRecord.setLayers(java.util.List.of(layer));
			}
		}

		assertThrows(IllegalArgumentException.class, () -> processor.process(batchRecord), testName);
	}

	static Stream<Arguments> provideInvalidRecords() {
		return Stream.of(
				Arguments.of("Missing Feature ID", null, null, null),
				Arguments.of("Missing Feature ID field", "test-data", null, "layer1"),
				Arguments.of("Empty Feature ID", "test-data", "   ", "layer1"),
				Arguments.of("Missing polygon data", "test-data", "12345678901", "layer1"),
				Arguments.of("Missing layer data", "test-data", "12345678901", "layer1")
		);
	}

	@Test
	void testProcess_ValidRecordWithNullRetryPolicy_ProcessesSuccessfully() throws Exception {
		processor = new VdypProjectionProcessor(null, metricsCollector, vdypProjectionService);
		ReflectionTestUtils.setField(processor, "maxDataLength", 50000);
		ReflectionTestUtils.setField(processor, "minPolygonIdLength", 1);
		ReflectionTestUtils.setField(processor, "maxPolygonIdLength", 50);
		processor.beforeStep(stepExecution);

		// Mock VdypProjectionService to return a test result
		when(vdypProjectionService.performProjectionForRecord(any(BatchRecord.class), anyString(), any(Parameters.class)))
				.thenReturn("Test projection result");

		BatchRecord batchRecord = createValidBatchRecord();

		BatchRecord result = processor.process(batchRecord);

		assertNotNull(result);
		assertNotNull(result.getProjectionResult());
		assertEquals("Test projection result", result.getProjectionResult());
	}

	@Test
	void testProcess_ValidRecordWithNullMetricsCollector_ProcessesSuccessfully() throws Exception {
		processor = new VdypProjectionProcessor(retryPolicy, null, vdypProjectionService);
		ReflectionTestUtils.setField(processor, "maxDataLength", 50000);
		ReflectionTestUtils.setField(processor, "minPolygonIdLength", 1);
		ReflectionTestUtils.setField(processor, "maxPolygonIdLength", 50);
		processor.beforeStep(stepExecution);

		// Mock VdypProjectionService to return a test result
		when(vdypProjectionService.performProjectionForRecord(any(BatchRecord.class), anyString(), any(Parameters.class)))
				.thenReturn("Test projection result");

		BatchRecord batchRecord = createValidBatchRecord();

		BatchRecord result = processor.process(batchRecord);

		assertNotNull(result);
		assertNotNull(result.getProjectionResult());
		assertEquals("Test projection result", result.getProjectionResult());
	}

	@Test
	void testBeforeStep_WithNullMetricsCollector_DoesNotThrowException() {
		processor = new VdypProjectionProcessor(retryPolicy, null, vdypProjectionService);

		processor.beforeStep(stepExecution);

		verify(stepExecution).getJobExecutionId();
		verify(executionContext).getString("partitionName", "unknown");
		verify(executionContext).getLong("startLine", 0);
		verify(executionContext).getLong("endLine", 0);
	}

	@Test
	void testProcess_RetrySuccessScenario_RemovesFromRetriedRecords() throws Exception {
		processor.beforeStep(stepExecution);

		// Mock VdypProjectionService to return a test result
		when(vdypProjectionService.performProjectionForRecord(any(BatchRecord.class), anyString(), any(Parameters.class)))
				.thenReturn("Test projection result");

		// Use reflection to access the static retriedRecords field and add an entry
		java.lang.reflect.Field retriedRecordsField = processor.getClass().getDeclaredField("retriedRecords");
		retriedRecordsField.setAccessible(true);
		@SuppressWarnings("unchecked")
		Set<String> retriedRecords = (Set<String>) retriedRecordsField.get(null);
		retriedRecords.add("test-partition_12345678901");

		BatchRecord batchRecord = createValidBatchRecord();

		BatchRecord result = processor.process(batchRecord);

		assertNotNull(result);
		verify(retryPolicy).onRetrySuccess(anyLong(), eq(batchRecord));

		// Clean up
		retriedRecords.clear();
	}

	@Test
	void testProcess_InterruptedException_ThrowsIOException() {
		processor.beforeStep(stepExecution);

		// Create a processor that will be interrupted
		VdypProjectionProcessor interruptedProcessor = new VdypProjectionProcessor(
				retryPolicy, metricsCollector, vdypProjectionService
		) {
			@Override
			public BatchRecord process(BatchRecord batchRecord) throws IOException, IllegalArgumentException {
				Thread.currentThread().interrupt();
				return super.process(batchRecord);
			}
		};
		ReflectionTestUtils.setField(interruptedProcessor, "maxDataLength", 50000);
		ReflectionTestUtils.setField(interruptedProcessor, "minPolygonIdLength", 1);
		ReflectionTestUtils.setField(interruptedProcessor, "maxPolygonIdLength", 50);

		BatchRecord batchRecord = createValidBatchRecord();

		assertThrows(IOException.class, () -> interruptedProcessor.process(batchRecord));
	}

	@Test
	void testProcess_EmptyProjectionResult_ThrowsIOException() throws NoSuchMethodException {
		// Test validateProjectionResult method directly with empty string
		processor.beforeStep(stepExecution);

		java.lang.reflect.Method validateMethod = processor.getClass()
				.getDeclaredMethod("validateProjectionResult", String.class, String.class);
		validateMethod.setAccessible(true);

		// Test empty string
		IOException thrown = assertThrows(IOException.class, () -> {
			invokeValidationMethod(validateMethod, "", "12345678901");
		});
		assertTrue(thrown.getMessage().contains("VDYP projection returned empty result"));
	}

	@ParameterizedTest
	@MethodSource("provideTransientErrors")
	void testIsTransientError_VariousExceptions_ReturnsTrue(Exception exception)
			throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		processor.beforeStep(stepExecution);

		// Test using reflection to access private method
		java.lang.reflect.Method isTransientErrorMethod = processor.getClass()
				.getDeclaredMethod("isTransientError", Exception.class);
		isTransientErrorMethod.setAccessible(true);

		boolean result = (boolean) isTransientErrorMethod.invoke(processor, exception);

		assertTrue(result);
	}

	static Stream<Arguments> provideTransientErrors() {
		return Stream.of(
				Arguments.of(new RuntimeException("Connection timeout occurred")),
				Arguments.of(new RuntimeException("Network unavailable")),
				Arguments.of(new RuntimeException("Service temporarily unavailable")),
				Arguments.of(new RuntimeException("Connection refused")), Arguments.of(new Exception("timeout") {
					@Override
					public String toString() {
						return "TimeoutException";
					}
				}), Arguments.of(new Exception("connection") {
					@Override
					public String toString() {
						return "ConnectionException";
					}
				})
		);
	}

	@Test
	void testIsTransientError_NonTransientError_ReturnsFalse()
			throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		processor.beforeStep(stepExecution);

		java.lang.reflect.Method isTransientErrorMethod = processor.getClass()
				.getDeclaredMethod("isTransientError", Exception.class);
		isTransientErrorMethod.setAccessible(true);

		Exception nonTransientException = new RuntimeException("Invalid data format");
		boolean result = (boolean) isTransientErrorMethod.invoke(processor, nonTransientException);

		assertFalse(result);
	}

	@Test
	void testReclassifyAndThrowException_IOException_RethrowsIOException() throws NoSuchMethodException {
		processor.beforeStep(stepExecution);

		java.lang.reflect.Method reclassifyMethod = processor.getClass()
				.getDeclaredMethod("reclassifyAndThrowException", Exception.class, String.class);
		reclassifyMethod.setAccessible(true);

		IOException ioException = new IOException("Original IO error");

		IOException thrown = assertThrows(IOException.class, () -> {
			invokeReclassifyMethod(reclassifyMethod, ioException, "12345678901");
		});
		assertEquals("Original IO error", thrown.getMessage());
	}

	@Test
	void testReclassifyAndThrowException_IllegalArgumentException_RethrowsIllegalArgumentException()
			throws NoSuchMethodException {
		processor.beforeStep(stepExecution);

		java.lang.reflect.Method reclassifyMethod = processor.getClass()
				.getDeclaredMethod("reclassifyAndThrowException", Exception.class, String.class);
		reclassifyMethod.setAccessible(true);

		IllegalArgumentException illegalArgException = new IllegalArgumentException("Invalid argument");

		IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
			invokeReclassifyMethod(reclassifyMethod, illegalArgException, "12345678901");
		});
		assertEquals("Invalid argument", thrown.getMessage());
	}

	@Test
	void testReclassifyAndThrowException_TransientRuntimeException_ThrowsIOException() throws NoSuchMethodException {
		processor.beforeStep(stepExecution);

		java.lang.reflect.Method reclassifyMethod = processor.getClass()
				.getDeclaredMethod("reclassifyAndThrowException", Exception.class, String.class);
		reclassifyMethod.setAccessible(true);

		RuntimeException transientException = new RuntimeException("Connection timeout");

		IOException thrown = assertThrows(IOException.class, () -> {
			invokeReclassifyMethod(reclassifyMethod, transientException, "12345678901");
		});
		assertTrue(thrown.getMessage().contains("Transient error during VDYP projection for Feature ID"));
	}

	@Test
	void testReclassifyAndThrowException_UnknownException_ThrowsIllegalArgumentException()
			throws NoSuchMethodException {
		processor.beforeStep(stepExecution);

		java.lang.reflect.Method reclassifyMethod = processor.getClass()
				.getDeclaredMethod("reclassifyAndThrowException", Exception.class, String.class);
		reclassifyMethod.setAccessible(true);

		Exception unknownException = new Exception("Unknown error");

		IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
			invokeReclassifyMethod(reclassifyMethod, unknownException, "12345678901");
		});
		assertTrue(thrown.getMessage().contains("VDYP projection failed for Feature ID"));
	}

	@Test
	void testIsRetryableException_IOExceptions_ReturnsTrue()
			throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		processor.beforeStep(stepExecution);

		java.lang.reflect.Method isRetryableMethod = processor.getClass()
				.getDeclaredMethod("isRetryableException", Exception.class);
		isRetryableMethod.setAccessible(true);

		IOException ioException = new IOException("IO error");
		boolean result = (boolean) isRetryableMethod.invoke(processor, ioException);

		assertTrue(result);
	}

	@Test
	void testIsRetryableException_TransientRuntimeException_ReturnsTrue()
			throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		processor.beforeStep(stepExecution);

		java.lang.reflect.Method isRetryableMethod = processor.getClass()
				.getDeclaredMethod("isRetryableException", Exception.class);
		isRetryableMethod.setAccessible(true);

		RuntimeException transientException = new RuntimeException("Connection timeout");
		boolean result = (boolean) isRetryableMethod.invoke(processor, transientException);

		assertTrue(result);
	}

	@Test
	void testIsRetryableException_NonTransientRuntimeException_ReturnsFalse()
			throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		processor.beforeStep(stepExecution);

		java.lang.reflect.Method isRetryableMethod = processor.getClass()
				.getDeclaredMethod("isRetryableException", Exception.class);
		isRetryableMethod.setAccessible(true);

		RuntimeException nonTransientException = new RuntimeException("Invalid data");
		boolean result = (boolean) isRetryableMethod.invoke(processor, nonTransientException);

		assertFalse(result);
	}

	@Test
	void testHandleProjectionException_RetryableException_RecordsRetryAttempt()
			throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		processor.beforeStep(stepExecution);

		java.lang.reflect.Method handleExceptionMethod = processor.getClass()
				.getDeclaredMethod("handleProjectionException", Exception.class, BatchRecord.class);
		handleExceptionMethod.setAccessible(true);

		BatchRecord batchRecord = createValidBatchRecord();
		IOException retryableException = new IOException("Retryable error");

		handleExceptionMethod.invoke(processor, retryableException, batchRecord);

		verify(metricsCollector).recordRetryAttempt(
				eq(1L), anyLong(), eq(batchRecord), eq(1), eq(retryableException), eq(false), eq("test-partition")
		);
	}

	@Test
	void testHandleProjectionException_NonRetryableException_RecordsSkip()
			throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		processor.beforeStep(stepExecution);

		java.lang.reflect.Method handleExceptionMethod = processor.getClass()
				.getDeclaredMethod("handleProjectionException", Exception.class, BatchRecord.class);
		handleExceptionMethod.setAccessible(true);

		BatchRecord batchRecord = createValidBatchRecord();
		IllegalArgumentException nonRetryableException = new IllegalArgumentException("Non-retryable error");

		handleExceptionMethod.invoke(processor, nonRetryableException, batchRecord);

		verify(metricsCollector).recordSkip(
				eq(1L), anyLong(), eq(batchRecord), eq(nonRetryableException), eq("test-partition"), isNull()
		);
	}

	@ParameterizedTest
	@MethodSource("provideValidationResults")
	void testValidateProjectionResult(String testName, String input, boolean shouldThrow, String expectedResult)
			throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		processor.beforeStep(stepExecution);

		java.lang.reflect.Method validateMethod = processor.getClass()
				.getDeclaredMethod("validateProjectionResult", String.class, String.class);
		validateMethod.setAccessible(true);

		if (shouldThrow) {
			assertThrows(IOException.class, () -> {
				invokeValidationMethod(validateMethod, input, "12345678901");
			}, testName);
		} else {
			String result = (String) validateMethod.invoke(processor, input, "12345678901");
			assertEquals(expectedResult, result, testName);
		}
	}

	static Stream<Arguments> provideValidationResults() {
		return Stream.of(
				Arguments.of("Null result throws IOException", null, true, null),
				Arguments.of("Empty result throws IOException", "   ", true, null),
				Arguments.of("Valid result returns result", "Valid projection result", false, "Valid projection result")
		);
	}

	private void invokeReclassifyMethod(java.lang.reflect.Method method, Exception exception, String featureId)
			throws Exception {
		try {
			method.invoke(processor, exception, featureId);
		} catch (java.lang.reflect.InvocationTargetException e) {
			throw (Exception) e.getCause();
		}
	}

	private String invokeValidationMethod(java.lang.reflect.Method method, String input, String featureId)
			throws Exception {
		try {
			return (String) method.invoke(processor, input, featureId);
		} catch (java.lang.reflect.InvocationTargetException e) {
			throw (Exception) e.getCause();
		}
	}

	private BatchRecord createValidBatchRecord() {
		BatchRecord batchRecord = new BatchRecord();
		batchRecord.setFeatureId("12345678901"); // Realistic 11-digit Feature ID

		// Create valid polygon data
		Polygon polygon = new Polygon();
		polygon.setFeatureId("12345678901");
		polygon.setMapId("082G055");
		polygon.setPolygonNumber(1234L);
		polygon.setOrgUnit("DCR");
		batchRecord.setPolygon(polygon);

		// Create valid layer data
		Layer layer = new Layer();
		layer.setFeatureId("12345678901");
		layer.setMapId("082G055");
		layer.setPolygonNumber(1234L);
		layer.setLayerLevelCode("P");
		batchRecord.setLayers(java.util.List.of(layer));

		return batchRecord;
	}
}
