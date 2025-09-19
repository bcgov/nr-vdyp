package ca.bc.gov.nrs.vdyp.batch.configuration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.partition.StepExecutionSplitter;
import org.springframework.core.task.TaskExecutor;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DynamicPartitionHandlerTest {

	private static final String NO_GRID_SIZE_MESSAGE = "No grid size specified";
	private static final String PARTITION_SIZE_PARAM = "partitionSize";
	private static final String CHUNK_SIZE_PARAM = "chunkSize";
	private static final String TEST_POLYGON_PATH = "classpath:VDYP7_INPUT_POLY.csv";
	private static final String CLASSPATH_POLYGON_PATH = "classpath:VDYP7_INPUT_POLY.csv";
	private static final String FILESYSTEM_POLYGON_PATH = "/data/forestry/vdyp/input/BC_Interior_Forest_Inventory_VDYP_Polygons.csv";
	private static final String NESTED_POLYGON_PATH = "classpath:test/data/forestry/BC_Coast_Forest_VDYP_Polygons.csv";
	private static final long DEFAULT_PARTITION_SIZE = 4L;
	private static final long DEFAULT_CHUNK_SIZE = 1000L;
	private static final int DEFAULT_GRID_SIZE = 4;

	@Mock
	private TaskExecutor taskExecutor;

	@Mock
	private Step workerStep;

	@Mock
	private DynamicPartitioner dynamicPartitioner;

	@Mock
	private BatchProperties batchProperties;

	@Mock
	private BatchProperties.Partitioning partitioning;

	@Mock
	private BatchProperties.Vdyp vdyp;

	@Mock
	private BatchProperties.Vdyp.Projection projection;

	@Mock
	private StepExecutionSplitter stepSplitter;

	@Mock
	private StepExecution masterStepExecution;

	@Mock
	private JobExecution jobExecution;

	private DynamicPartitionHandler dynamicPartitionHandler;

	@BeforeEach
	void setUp() {
		dynamicPartitionHandler = new DynamicPartitionHandler(
				taskExecutor, workerStep, dynamicPartitioner, batchProperties
		);
	}

	@Test
	void testConstructor() {
		assertNotNull(dynamicPartitionHandler);
	}

	@Test
	void testHandle_withJobParametersComplete_success() {
		// Setup job parameters
		JobParameters jobParameters = new JobParametersBuilder().addLong(PARTITION_SIZE_PARAM, DEFAULT_PARTITION_SIZE)
				.addLong(CHUNK_SIZE_PARAM, DEFAULT_CHUNK_SIZE).toJobParameters();

		setupBasicMocks(jobParameters);
		setupPolygonFileMocks(CLASSPATH_POLYGON_PATH);

		// Setup batch properties
		when(batchProperties.getPartitioning()).thenReturn(partitioning);
		when(partitioning.getGridSize()).thenReturn(2);

		// Test the parameter extraction and validation logic
		assertDoesNotThrow(() -> dynamicPartitionHandler.handle(stepSplitter, masterStepExecution));

		verify(dynamicPartitioner).setPolygonResource(any());
		verify(masterStepExecution, atLeastOnce()).getJobExecution();
		verify(jobExecution, atLeastOnce()).getJobParameters();
	}

	/**
	 * Helper method to setup basic mocks for successful test execution.
	 *
	 * @param jobParameters The job parameters to use for the test
	 */
	private void setupBasicMocks(JobParameters jobParameters) {
		when(masterStepExecution.getJobExecution()).thenReturn(jobExecution);
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
	}

	/**
	 * Helper method to setup polygon file path mocks.
	 *
	 * @param polygonFilePath The polygon file path to use
	 */
	private void setupPolygonFileMocks(String polygonFilePath) {
		when(batchProperties.getVdyp()).thenReturn(vdyp);
		when(vdyp.getProjection()).thenReturn(projection);
		when(projection.getPolygonFile()).thenReturn(polygonFilePath);
	}

	@Test
	void testHandle_withJobParametersPartial_usesBatchProperties() {
		// Setup job parameters with only some values (no partitionSize)
		JobParameters jobParameters = new JobParametersBuilder().addLong(CHUNK_SIZE_PARAM, DEFAULT_CHUNK_SIZE)
				.toJobParameters();

		setupBasicMocks(jobParameters);
		setupPolygonFileMocks(TEST_POLYGON_PATH);
		when(batchProperties.getPartitioning()).thenReturn(partitioning);
		when(partitioning.getGridSize()).thenReturn(DEFAULT_GRID_SIZE);

		assertDoesNotThrow(() -> {
			dynamicPartitionHandler.handle(stepSplitter, masterStepExecution);
		});

		verify(dynamicPartitioner).setPolygonResource(any());
		verify(batchProperties, atLeastOnce()).getPartitioning();
		verify(batchProperties, atLeastOnce()).getVdyp();
	}

	@ParameterizedTest
	@MethodSource("providePolygonResourcePaths")
	void testHandle_withDifferentPolygonPaths_createsAppropriateResource(String polygonPath, long partitionSize) {
		JobParameters jobParameters = new JobParametersBuilder().addLong(PARTITION_SIZE_PARAM, partitionSize)
				.toJobParameters();

		setupBasicMocks(jobParameters);
		setupPolygonFileMocks(polygonPath);
		when(batchProperties.getPartitioning()).thenReturn(partitioning);

		assertDoesNotThrow(() -> {
			dynamicPartitionHandler.handle(stepSplitter, masterStepExecution);
		});

		verify(dynamicPartitioner).setPolygonResource(any());
	}

	static Stream<Arguments> providePolygonResourcePaths() {
		return Stream.of(
				Arguments.of(CLASSPATH_POLYGON_PATH, 2L), Arguments.of(FILESYSTEM_POLYGON_PATH, 3L),
				Arguments.of(NESTED_POLYGON_PATH, 1L),
				Arguments.of("relative/forestry/data/BC_Northern_Interior_VDYP_Polygons.csv", 1L)
		);
	}

	@Test
	void testHandle_noPartitionSizeInJobParametersOrProperties_throwsException() {
		JobParameters jobParameters = new JobParametersBuilder().toJobParameters();

		setupBasicMocks(jobParameters);
		// Don't setup polygon file mocks since exception is thrown before they're used
		when(batchProperties.getPartitioning()).thenReturn(partitioning);
		when(partitioning.getGridSize()).thenReturn(0); // No grid size in properties either

		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
			dynamicPartitionHandler.handle(stepSplitter, masterStepExecution);
		});

		assertTrue(exception.getMessage().contains(NO_GRID_SIZE_MESSAGE));
		verify(batchProperties, atLeastOnce()).getPartitioning();
	}

	@Test
	void testHandle_noPolygonFileInProperties_throwsNullPointerException() {
		JobParameters jobParameters = new JobParametersBuilder().addLong(PARTITION_SIZE_PARAM, DEFAULT_PARTITION_SIZE)
				.toJobParameters();

		setupBasicMocks(jobParameters);
		// Only set up the mocks that are actually called before the exception
		when(batchProperties.getVdyp()).thenReturn(vdyp);
		when(vdyp.getProjection()).thenReturn(projection);
		when(projection.getPolygonFile()).thenReturn(null); // No polygon file path in properties

		// This should throw NullPointerException when trying to call startsWith on null
		// polygon file
		assertThrows(NullPointerException.class, () -> {
			dynamicPartitionHandler.handle(stepSplitter, masterStepExecution);
		});

		verify(batchProperties, atLeastOnce()).getVdyp();
	}

	@ParameterizedTest
	@MethodSource("providePolygonPathValidationCases")
	void testHandle_polygonPathValidation(String polygonPath, boolean shouldThrow, String testDescription) {
		JobParameters jobParameters = new JobParametersBuilder().addLong(PARTITION_SIZE_PARAM, 2L).toJobParameters();

		setupBasicMocks(jobParameters);

		if (shouldThrow) {
			setupPolygonFileMocks(polygonPath);

			assertThrows(Exception.class, () -> {
				dynamicPartitionHandler.handle(stepSplitter, masterStepExecution);
			}, testDescription);
		} else {
			when(batchProperties.getPartitioning()).thenReturn(partitioning);
			setupPolygonFileMocks(polygonPath);

			assertDoesNotThrow(() -> {
				dynamicPartitionHandler.handle(stepSplitter, masterStepExecution);
			}, testDescription);

			verify(batchProperties, atLeastOnce()).getVdyp();
			verify(vdyp).getProjection();
		}
	}

	static Stream<Arguments> providePolygonPathValidationCases() {
		return Stream.of(
				Arguments.of(CLASSPATH_POLYGON_PATH, false, "Valid classpath polygon file should succeed"),
				Arguments.of(FILESYSTEM_POLYGON_PATH, false, "Valid filesystem polygon file should succeed"),
				Arguments.of(null, true, "Null polygon file path should throw exception"),
				Arguments.of("", true, "Empty polygon file path should throw exception")
		);
	}

	@Test
	void testHandle_nullPartitionSizeButValidPropertiesGridSize_success() {
		JobParameters jobParameters = new JobParametersBuilder().toJobParameters();

		setupBasicMocks(jobParameters);
		setupPolygonFileMocks(TEST_POLYGON_PATH);
		when(batchProperties.getPartitioning()).thenReturn(partitioning);
		when(partitioning.getGridSize()).thenReturn(5); // Valid grid size in properties

		assertDoesNotThrow(() -> {
			dynamicPartitionHandler.handle(stepSplitter, masterStepExecution);
		});

		verify(batchProperties, atLeastOnce()).getPartitioning();
		verify(partitioning, atLeastOnce()).getGridSize();
	}

	@Test
	void testHandle_zeroPartitionSizeInJobParameters_usesBatchProperties() {
		JobParameters jobParameters = new JobParametersBuilder().addLong(PARTITION_SIZE_PARAM, 0L).toJobParameters();

		setupBasicMocks(jobParameters);
		setupPolygonFileMocks(TEST_POLYGON_PATH);
		when(batchProperties.getPartitioning()).thenReturn(partitioning);
		when(partitioning.getGridSize()).thenReturn(3);

		assertDoesNotThrow(() -> {
			dynamicPartitionHandler.handle(stepSplitter, masterStepExecution);
		});
	}

	@Test
	void testHandle_negativePartitionSizeInJobParameters_throwsException() {
		JobParameters jobParameters = new JobParametersBuilder().addLong(PARTITION_SIZE_PARAM, -1L).toJobParameters();

		setupBasicMocks(jobParameters);
		setupPolygonFileMocks(TEST_POLYGON_PATH);
		when(batchProperties.getPartitioning()).thenReturn(partitioning);

		// TaskExecutorPartitionHandler throws IllegalArgumentException for negative
		// grid size
		assertThrows(IllegalArgumentException.class, () -> {
			dynamicPartitionHandler.handle(stepSplitter, masterStepExecution);
		});
	}

	@Test
	void testHandle_withChunkSizeParameter_processesSuccessfully() {
		JobParameters jobParameters = new JobParametersBuilder().addLong(PARTITION_SIZE_PARAM, 2L)
				.addLong(CHUNK_SIZE_PARAM, 150L).toJobParameters();

		setupBasicMocks(jobParameters);
		setupPolygonFileMocks(TEST_POLYGON_PATH);
		when(batchProperties.getPartitioning()).thenReturn(partitioning);

		assertDoesNotThrow(() -> {
			dynamicPartitionHandler.handle(stepSplitter, masterStepExecution);
		});

		// This test verifies that chunk size parameter is processed successfully
		verify(jobExecution, atLeastOnce()).getJobParameters();
	}

	@Test
	void testHandle_withoutChunkSize_processesSuccessfully() {
		JobParameters jobParameters = new JobParametersBuilder().addLong(PARTITION_SIZE_PARAM, 2L).toJobParameters();

		setupBasicMocks(jobParameters);
		setupPolygonFileMocks(TEST_POLYGON_PATH);
		when(batchProperties.getPartitioning()).thenReturn(partitioning);

		assertDoesNotThrow(() -> {
			dynamicPartitionHandler.handle(stepSplitter, masterStepExecution);
		});

		// This test verifies that the handler works without chunk size parameter
		verify(jobExecution, atLeastOnce()).getJobParameters();
	}

	@Test
	void testHandle_maximumParameters_allPathsExercised() {
		JobParameters jobParameters = new JobParametersBuilder().addLong(PARTITION_SIZE_PARAM, 8L)
				.addLong(CHUNK_SIZE_PARAM, 200L).addString("outputFilePath", "/data/forestry/output/vdyp_results")
				.toJobParameters();

		setupBasicMocks(jobParameters);
		setupPolygonFileMocks(FILESYSTEM_POLYGON_PATH);
		when(batchProperties.getPartitioning()).thenReturn(partitioning);
		when(partitioning.getGridSize()).thenReturn(DEFAULT_GRID_SIZE); // Should be overridden by job parameters

		assertDoesNotThrow(() -> {
			dynamicPartitionHandler.handle(stepSplitter, masterStepExecution);
		});

		verify(dynamicPartitioner).setPolygonResource(any());
		verify(batchProperties, atLeastOnce()).getPartitioning();
	}

	@Test
	void testHandle_stepSplitterAndMasterStepExecutionInteraction_success() {
		JobParameters jobParameters = new JobParametersBuilder().addLong(PARTITION_SIZE_PARAM, 2L).toJobParameters();

		setupBasicMocks(jobParameters);
		setupPolygonFileMocks(TEST_POLYGON_PATH);
		when(batchProperties.getPartitioning()).thenReturn(partitioning);

		// This test verifies that the stepSplitter and masterStepExecution are passed
		// correctly to the TaskExecutorPartitionHandler
		assertDoesNotThrow(() -> {
			dynamicPartitionHandler.handle(stepSplitter, masterStepExecution);
		});

		verify(masterStepExecution, atLeastOnce()).getJobExecution();
		verify(dynamicPartitioner).setPolygonResource(any());
	}
}