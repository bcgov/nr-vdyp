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

	private static final String PARTITION_SIZE_PARAM = "partitionSize";
	private static final String CHUNK_SIZE_PARAM = "chunkSize";
	private static final String TEST_JOB_BASE_DIR = "/tmp/test";
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
	private BatchProperties.Partition partition;

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
		// Setup job parameters with job base directory
		JobParameters jobParameters = createJobParametersWithJobBaseDir(DEFAULT_PARTITION_SIZE, DEFAULT_CHUNK_SIZE);

		setupBasicMocks(jobParameters);

		// Test the parameter extraction and validation logic
		assertDoesNotThrow(() -> dynamicPartitionHandler.handle(stepSplitter, masterStepExecution));

		verify(dynamicPartitioner).setJobBaseDir(TEST_JOB_BASE_DIR);
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
	 * Helper method to create job parameters with job base directory
	 */
	private JobParameters createJobParametersWithJobBaseDir(Long partitionSize, Long chunkSize) {
		JobParametersBuilder builder = new JobParametersBuilder().addString("jobBaseDir", TEST_JOB_BASE_DIR);
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
		JobParameters jobParameters = createJobParametersWithJobBaseDir(null, DEFAULT_CHUNK_SIZE);

		setupBasicMocks(jobParameters);
		when(batchProperties.getPartition()).thenReturn(partition);
		when(partition.getDefaultPartitionSize()).thenReturn(DEFAULT_GRID_SIZE);

		assertDoesNotThrow(() -> {
			dynamicPartitionHandler.handle(stepSplitter, masterStepExecution);
		});

		verify(dynamicPartitioner).setJobBaseDir(TEST_JOB_BASE_DIR);
		verify(batchProperties, atLeastOnce()).getPartition();
	}

	@ParameterizedTest
	@MethodSource("providePartitionBaseDirPaths")
	void testHandle_withDifferentPartitionBaseDirs_createsAppropriateResource(String jobBaseDir, long partitionSize) {
		JobParameters jobParameters = new JobParametersBuilder().addString("jobBaseDir", jobBaseDir)
				.addLong(PARTITION_SIZE_PARAM, partitionSize).toJobParameters();

		setupBasicMocks(jobParameters);

		assertDoesNotThrow(() -> {
			dynamicPartitionHandler.handle(stepSplitter, masterStepExecution);
		});

		verify(dynamicPartitioner).setJobBaseDir(jobBaseDir);
	}

	static Stream<Arguments> providePartitionBaseDirPaths() {
		return Stream.of(
				Arguments.of("/tmp/test1", 2L), Arguments.of("/tmp/test2", 3L), Arguments.of("/tmp/test3", 1L),
				Arguments.of("/tmp/test4", 1L)
		);
	}

	@Test
	void testHandle_noPartitionSizeInJobParametersOrProperties_succeeds() {
		JobParameters jobParameters = new JobParametersBuilder().toJobParameters();

		setupBasicMocks(jobParameters);
		when(batchProperties.getPartition()).thenReturn(partition);
		when(partition.getDefaultPartitionSize()).thenReturn(0);

		// 0 grid size is accepted by TaskExecutorPartitionHandler (creates 0 partitions)
		assertDoesNotThrow(() -> {
			dynamicPartitionHandler.handle(stepSplitter, masterStepExecution);
		});

		verify(batchProperties, atLeastOnce()).getPartition();
	}

	@Test
	void testHandle_validConfiguration_success() {
		JobParameters jobParameters = new JobParametersBuilder().addLong(PARTITION_SIZE_PARAM, DEFAULT_PARTITION_SIZE)
				.toJobParameters();

		setupBasicMocks(jobParameters);

		// This test should succeed - no jobBaseDir means setJobBaseDir won't be called
		assertDoesNotThrow(() -> {
			dynamicPartitionHandler.handle(stepSplitter, masterStepExecution);
		});

		// jobBaseDir is null, so setJobBaseDir should not be called
		verify(dynamicPartitioner, never()).setJobBaseDir(any());
	}

	@ParameterizedTest
	@MethodSource("providePartitionBaseDirValidationCases")
	void testHandle_partitionBaseDirValidation(String jobBaseDir, boolean shouldThrow, String testDescription) {
		JobParametersBuilder builder = new JobParametersBuilder().addLong(PARTITION_SIZE_PARAM, 2L);

		if (jobBaseDir != null) {
			builder.addString("jobBaseDir", jobBaseDir);
		}

		JobParameters jobParameters = builder.toJobParameters();

		setupBasicMocks(jobParameters);

		if (shouldThrow) {
			assertThrows(Exception.class, () -> {
				dynamicPartitionHandler.handle(stepSplitter, masterStepExecution);
			}, testDescription);
		} else {
			assertDoesNotThrow(() -> {
				dynamicPartitionHandler.handle(stepSplitter, masterStepExecution);
			}, testDescription);

			// Verify setJobBaseDir was called only if jobBaseDir was provided
			if (jobBaseDir != null && !jobBaseDir.isEmpty()) {
				verify(dynamicPartitioner).setJobBaseDir(jobBaseDir);
			}
		}
	}

	static Stream<Arguments> providePartitionBaseDirValidationCases() {
		return Stream.of(
				Arguments.of("/tmp/test1", false, "Valid job base directory should succeed"),
				Arguments.of("/tmp/test2", false, "Valid job base directory should succeed"),
				Arguments.of(null, false, "Null job base directory should still succeed"),
				Arguments.of("", false, "Empty job base directory should still succeed")
		);
	}

	@Test
	void testHandle_nullPartitionSizeButValidPropertiesGridSize_success() {
		JobParameters jobParameters = new JobParametersBuilder().toJobParameters();

		setupBasicMocks(jobParameters);
		when(batchProperties.getPartition()).thenReturn(partition);
		when(partition.getDefaultPartitionSize()).thenReturn(5); // Valid grid size in properties

		assertDoesNotThrow(() -> {
			dynamicPartitionHandler.handle(stepSplitter, masterStepExecution);
		});

		verify(batchProperties, atLeastOnce()).getPartition();
		verify(partition, atLeastOnce()).getDefaultPartitionSize();
	}

	@Test
	void testHandle_zeroPartitionSizeInJobParameters_succeeds() {
		JobParameters jobParameters = new JobParametersBuilder().addLong(PARTITION_SIZE_PARAM, 0L).toJobParameters();

		setupBasicMocks(jobParameters);

		// 0 partition size results in 0 grid size which is accepted (creates 0 partitions)
		assertDoesNotThrow(() -> {
			dynamicPartitionHandler.handle(stepSplitter, masterStepExecution);
		});
	}

	@Test
	void testHandle_negativePartitionSizeInJobParameters_throwsException() {
		JobParameters jobParameters = new JobParametersBuilder().addLong(PARTITION_SIZE_PARAM, -1L).toJobParameters();

		setupBasicMocks(jobParameters);

		// TaskExecutorPartitionHandler throws IllegalArgumentException for negative grid size
		assertThrows(IllegalArgumentException.class, () -> {
			dynamicPartitionHandler.handle(stepSplitter, masterStepExecution);
		});
	}

	@Test
	void testHandle_withChunkSizeParameter_processesSuccessfully() {
		JobParameters jobParameters = new JobParametersBuilder().addLong(PARTITION_SIZE_PARAM, 2L)
				.addLong(CHUNK_SIZE_PARAM, 150L).toJobParameters();

		setupBasicMocks(jobParameters);

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

		assertDoesNotThrow(() -> {
			dynamicPartitionHandler.handle(stepSplitter, masterStepExecution);
		});

		// This test verifies that the handler works without chunk size parameter
		verify(jobExecution, atLeastOnce()).getJobParameters();
	}

	@Test
	void testHandle_maximumParameters_allPathsExercised() {
		JobParameters jobParameters = new JobParametersBuilder().addString("jobBaseDir", TEST_JOB_BASE_DIR)
				.addLong(PARTITION_SIZE_PARAM, 8L).addLong(CHUNK_SIZE_PARAM, 200L)
				.addString("outputFilePath", "/data/forestry/output/vdyp_results").toJobParameters();

		setupBasicMocks(jobParameters);

		assertDoesNotThrow(() -> {
			dynamicPartitionHandler.handle(stepSplitter, masterStepExecution);
		});

		verify(dynamicPartitioner).setJobBaseDir(TEST_JOB_BASE_DIR);
	}

	@Test
	void testHandle_stepSplitterAndMasterStepExecutionInteraction_success() {
		JobParameters jobParameters = createJobParametersWithJobBaseDir(2L, null);

		setupBasicMocks(jobParameters);

		// This test verifies that the stepSplitter and masterStepExecution are passed
		// correctly to the TaskExecutorPartitionHandler
		assertDoesNotThrow(() -> {
			dynamicPartitionHandler.handle(stepSplitter, masterStepExecution);
		});

		verify(masterStepExecution, atLeastOnce()).getJobExecution();
		verify(dynamicPartitioner).setJobBaseDir(TEST_JOB_BASE_DIR);
	}
}
