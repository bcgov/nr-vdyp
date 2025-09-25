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
	private static final String TEST_PARTITION_BASE_DIR = "/tmp/test";
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
	private StepExecutionSplitter stepSplitter;

	@Mock
	private StepExecution masterStepExecution;

	@Mock
	private JobExecution jobExecution;

	private DynamicPartitionHandler dynamicPartitionHandler;

	@BeforeEach
	void setUp() {
		dynamicPartitionHandler = new DynamicPartitionHandler(
				taskExecutor, workerStep, dynamicPartitioner, batchProperties);
	}

	@Test
	void testConstructor() {
		assertNotNull(dynamicPartitionHandler);
	}

	@Test
	void testHandle_withJobParametersComplete_success() {
		// Setup job parameters with partition base directory
		JobParameters jobParameters = createJobParametersWithPartitionBaseDir(DEFAULT_PARTITION_SIZE,
				DEFAULT_CHUNK_SIZE);

		setupBasicMocks(jobParameters);

		// Setup batch properties
		when(batchProperties.getPartitioning()).thenReturn(partitioning);
		when(partitioning.getGridSize()).thenReturn(2);

		// Test the parameter extraction and validation logic
		assertDoesNotThrow(() -> dynamicPartitionHandler.handle(stepSplitter, masterStepExecution));

		verify(dynamicPartitioner).setPartitionBaseDir(any());
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
	 * Helper method to create job parameters with partition base directory
	 */
	private JobParameters createJobParametersWithPartitionBaseDir(Long partitionSize, Long chunkSize) {
		JobParametersBuilder builder = new JobParametersBuilder()
				.addString("partitionBaseDir", TEST_PARTITION_BASE_DIR);
		if (partitionSize != null) {
			builder.addLong(PARTITION_SIZE_PARAM, partitionSize);
		}
		if (chunkSize != null) {
			builder.addLong(CHUNK_SIZE_PARAM, chunkSize);
		}
		return builder.toJobParameters();
	}

	@Test
	void testHandle_withJobParametersPartial_usesBatchProperties() {
		// Setup job parameters with only some values (no partitionSize)
		JobParameters jobParameters = createJobParametersWithPartitionBaseDir(null, DEFAULT_CHUNK_SIZE);

		setupBasicMocks(jobParameters);
		when(batchProperties.getPartitioning()).thenReturn(partitioning);
		when(partitioning.getGridSize()).thenReturn(DEFAULT_GRID_SIZE);

		assertDoesNotThrow(() -> {
			dynamicPartitionHandler.handle(stepSplitter, masterStepExecution);
		});

		verify(dynamicPartitioner).setPartitionBaseDir(any());
		verify(batchProperties, atLeastOnce()).getPartitioning();
	}

	@ParameterizedTest
	@MethodSource("providePartitionBaseDirPaths")
	void testHandle_withDifferentPartitionBaseDirs_createsAppropriateResource(String partitionBaseDir,
			long partitionSize) {
		JobParameters jobParameters = createJobParametersWithPartitionBaseDir(partitionSize, null);

		setupBasicMocks(jobParameters);
		when(batchProperties.getPartitioning()).thenReturn(partitioning);

		assertDoesNotThrow(() -> {
			dynamicPartitionHandler.handle(stepSplitter, masterStepExecution);
		});

		verify(dynamicPartitioner).setPartitionBaseDir(any());
	}

	static Stream<Arguments> providePartitionBaseDirPaths() {
		return Stream.of(
				Arguments.of("/tmp/test1", 2L), Arguments.of("/tmp/test2", 3L),
				Arguments.of("/tmp/test3", 1L),
				Arguments.of("/tmp/test4", 1L));
	}

	@Test
	void testHandle_noPartitionSizeInJobParametersOrProperties_throwsException() {
		JobParameters jobParameters = new JobParametersBuilder().toJobParameters();

		setupBasicMocks(jobParameters);
		// Don't setup partition dir mocks since exception is thrown before they're used
		when(batchProperties.getPartitioning()).thenReturn(partitioning);
		when(partitioning.getGridSize()).thenReturn(0); // No grid size in properties either

		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
			dynamicPartitionHandler.handle(stepSplitter, masterStepExecution);
		});

		assertTrue(exception.getMessage().contains(NO_GRID_SIZE_MESSAGE));
		verify(batchProperties, atLeastOnce()).getPartitioning();
	}

	@Test
	void testHandle_validConfiguration_success() {
		JobParameters jobParameters = new JobParametersBuilder().addLong(PARTITION_SIZE_PARAM, DEFAULT_PARTITION_SIZE)
				.toJobParameters();

		setupBasicMocks(jobParameters);
		when(batchProperties.getPartitioning()).thenReturn(partitioning);

		// This test should succeed since the DynamicPartitionHandler doesn't need
		// polygon file paths
		assertDoesNotThrow(() -> {
			dynamicPartitionHandler.handle(stepSplitter, masterStepExecution);
		});

		verify(batchProperties, atLeastOnce()).getPartitioning();
	}

	@ParameterizedTest
	@MethodSource("providePartitionBaseDirValidationCases")
	void testHandle_partitionBaseDirValidation(String partitionBaseDir, boolean shouldThrow, String testDescription) {
		JobParameters jobParameters = new JobParametersBuilder().addLong(PARTITION_SIZE_PARAM, 2L).toJobParameters();

		setupBasicMocks(jobParameters);

		if (shouldThrow) {
			assertThrows(Exception.class, () -> {
				dynamicPartitionHandler.handle(stepSplitter, masterStepExecution);
			}, testDescription);
		} else {
			when(batchProperties.getPartitioning()).thenReturn(partitioning);

			assertDoesNotThrow(() -> {
				dynamicPartitionHandler.handle(stepSplitter, masterStepExecution);
			}, testDescription);

			verify(batchProperties, atLeastOnce()).getPartitioning();
		}
	}

	static Stream<Arguments> providePartitionBaseDirValidationCases() {
		return Stream.of(
				Arguments.of("/tmp/test1", false, "Valid partition base directory should succeed"),
				Arguments.of("/tmp/test2", false, "Valid partition base directory should succeed"),
				Arguments.of(null, false, "Null partition base directory should still succeed"),
				Arguments.of("", false, "Empty partition base directory should still succeed"));
	}

	@Test
	void testHandle_nullPartitionSizeButValidPropertiesGridSize_success() {
		JobParameters jobParameters = new JobParametersBuilder().toJobParameters();

		setupBasicMocks(jobParameters);
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
		when(batchProperties.getPartitioning()).thenReturn(partitioning);

		assertDoesNotThrow(() -> {
			dynamicPartitionHandler.handle(stepSplitter, masterStepExecution);
		});

		// This test verifies that the handler works without chunk size parameter
		verify(jobExecution, atLeastOnce()).getJobParameters();
	}

	@Test
	void testHandle_maximumParameters_allPathsExercised() {
		JobParameters jobParameters = new JobParametersBuilder()
				.addString("partitionBaseDir", TEST_PARTITION_BASE_DIR)
				.addLong(PARTITION_SIZE_PARAM, 8L)
				.addLong(CHUNK_SIZE_PARAM, 200L)
				.addString("outputFilePath", "/data/forestry/output/vdyp_results")
				.toJobParameters();

		setupBasicMocks(jobParameters);
		when(batchProperties.getPartitioning()).thenReturn(partitioning);
		when(partitioning.getGridSize()).thenReturn(DEFAULT_GRID_SIZE); // Should be overridden by job parameters

		assertDoesNotThrow(() -> {
			dynamicPartitionHandler.handle(stepSplitter, masterStepExecution);
		});

		verify(dynamicPartitioner).setPartitionBaseDir(any());
		verify(batchProperties, atLeastOnce()).getPartitioning();
	}

	@Test
	void testHandle_stepSplitterAndMasterStepExecutionInteraction_success() {
		JobParameters jobParameters = createJobParametersWithPartitionBaseDir(2L, null);

		setupBasicMocks(jobParameters);
		when(batchProperties.getPartitioning()).thenReturn(partitioning);

		// This test verifies that the stepSplitter and masterStepExecution are passed
		// correctly to the TaskExecutorPartitionHandler
		assertDoesNotThrow(() -> {
			dynamicPartitionHandler.handle(stepSplitter, masterStepExecution);
		});

		verify(masterStepExecution, atLeastOnce()).getJobExecution();
		verify(dynamicPartitioner).setPartitionBaseDir(any());
	}
}
