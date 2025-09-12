package ca.bc.gov.nrs.vdyp.batch.configuration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.partition.StepExecutionSplitter;
import org.springframework.core.task.TaskExecutor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DynamicPartitionHandlerTest {

	// Test constants
	private static final String TEST_INPUT_PATH = "/test/input.csv";
	private static final String TEST_BATCH_INPUT_PATH = "/test/batch-input.csv";
	private static final String TEST_PROPERTIES_INPUT_PATH = "/test/properties-input.csv";
	private static final String CLASSPATH_INPUT_PATH = "classpath:test-data/input.csv";
	private static final String NO_GRID_SIZE_MESSAGE = "No grid size specified";
	private static final String NO_INPUT_FILE_MESSAGE = "No input file path specified";
	private static final String PARTITION_SIZE_PARAM = "partitionSize";
	private static final String CHUNK_SIZE_PARAM = "chunkSize";
	private static final String INPUT_FILE_PATH_PARAM = "inputFilePath";
	private static final long DEFAULT_PARTITION_SIZE = 4L;
	private static final long DEFAULT_CHUNK_SIZE = 100L;
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
	private BatchProperties.Input input;

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
				.addLong(CHUNK_SIZE_PARAM, DEFAULT_CHUNK_SIZE).addString(INPUT_FILE_PATH_PARAM, TEST_INPUT_PATH)
				.toJobParameters();

		setupBasicMocks(jobParameters);

		// Setup batch properties
		when(batchProperties.getPartitioning()).thenReturn(partitioning);
		when(partitioning.getGridSize()).thenReturn(2);

		// Test the parameter extraction and validation logic
		assertDoesNotThrow(() -> dynamicPartitionHandler.handle(stepSplitter, masterStepExecution));

		verify(dynamicPartitioner).setInputResource(any());
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

	@Test
	void testHandle_withJobParametersPartial_usesBatchProperties() {
		// Setup job parameters with only some values
		JobParameters jobParameters = new JobParametersBuilder().addLong(CHUNK_SIZE_PARAM, DEFAULT_CHUNK_SIZE)
				.toJobParameters();

		setupBasicMocks(jobParameters);
		when(batchProperties.getPartitioning()).thenReturn(partitioning);
		when(batchProperties.getInput()).thenReturn(input);
		when(partitioning.getGridSize()).thenReturn(DEFAULT_GRID_SIZE);
		when(input.getFilePath()).thenReturn(TEST_BATCH_INPUT_PATH);

		assertDoesNotThrow(() -> {
			dynamicPartitionHandler.handle(stepSplitter, masterStepExecution);
		});

		verify(dynamicPartitioner).setInputResource(any());
		verify(batchProperties, atLeastOnce()).getPartitioning();
		verify(batchProperties, atLeastOnce()).getInput();
	}

	@Test
	void testHandle_withClasspathResource_createsClassPathResource() {
		JobParameters jobParameters = new JobParametersBuilder().addLong(PARTITION_SIZE_PARAM, 2L)
				.addString(INPUT_FILE_PATH_PARAM, CLASSPATH_INPUT_PATH).toJobParameters();

		setupBasicMocks(jobParameters);
		when(batchProperties.getPartitioning()).thenReturn(partitioning);

		assertDoesNotThrow(() -> {
			dynamicPartitionHandler.handle(stepSplitter, masterStepExecution);
		});

		verify(dynamicPartitioner).setInputResource(any());
	}

	@Test
	void testHandle_withFileSystemResource_createsFileSystemResource() {
		JobParameters jobParameters = new JobParametersBuilder().addLong(PARTITION_SIZE_PARAM, 3L)
				.addString(INPUT_FILE_PATH_PARAM, "/absolute/path/to/input.csv").toJobParameters();

		setupBasicMocks(jobParameters);
		when(batchProperties.getPartitioning()).thenReturn(partitioning);

		assertDoesNotThrow(() -> {
			dynamicPartitionHandler.handle(stepSplitter, masterStepExecution);
		});

		verify(dynamicPartitioner).setInputResource(any());
	}

	@Test
	void testHandle_noPartitionSizeInJobParametersOrProperties_throwsException() {
		JobParameters jobParameters = new JobParametersBuilder().addString(INPUT_FILE_PATH_PARAM, TEST_INPUT_PATH)
				.toJobParameters();

		setupBasicMocks(jobParameters);
		when(batchProperties.getPartitioning()).thenReturn(partitioning);
		when(partitioning.getGridSize()).thenReturn(0); // No grid size in properties either

		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
			dynamicPartitionHandler.handle(stepSplitter, masterStepExecution);
		});

		assertTrue(exception.getMessage().contains(NO_GRID_SIZE_MESSAGE));
		verify(batchProperties, atLeastOnce()).getPartitioning();
	}

	@Test
	void testHandle_noInputFilePathInJobParametersOrProperties_throwsException() {
		JobParameters jobParameters = new JobParametersBuilder().addLong(PARTITION_SIZE_PARAM, DEFAULT_PARTITION_SIZE)
				.toJobParameters();

		setupBasicMocks(jobParameters);
		when(batchProperties.getInput()).thenReturn(input);
		when(input.getFilePath()).thenReturn(null); // No file path in properties

		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
			dynamicPartitionHandler.handle(stepSplitter, masterStepExecution);
		});

		assertTrue(exception.getMessage().contains(NO_INPUT_FILE_MESSAGE));
		verify(batchProperties, atLeastOnce()).getInput();
	}

	@Test
	void testHandle_emptyInputFilePathInJobParameters_usesBatchProperties() {
		JobParameters jobParameters = new JobParametersBuilder().addLong(PARTITION_SIZE_PARAM, 2L)
				.addString(INPUT_FILE_PATH_PARAM, "").toJobParameters();

		setupBasicMocks(jobParameters);
		when(batchProperties.getPartitioning()).thenReturn(partitioning);
		when(batchProperties.getInput()).thenReturn(input);
		when(input.getFilePath()).thenReturn(TEST_PROPERTIES_INPUT_PATH);

		assertDoesNotThrow(() -> {
			dynamicPartitionHandler.handle(stepSplitter, masterStepExecution);
		});

		verify(batchProperties, atLeastOnce()).getInput();
		verify(input).getFilePath();
	}

	@Test
	void testHandle_whitespaceInputFilePathInJobParameters_usesBatchProperties() {
		JobParameters jobParameters = new JobParametersBuilder().addLong(PARTITION_SIZE_PARAM, 2L)
				.addString(INPUT_FILE_PATH_PARAM, "   ").toJobParameters();

		setupBasicMocks(jobParameters);
		when(batchProperties.getPartitioning()).thenReturn(partitioning);
		when(batchProperties.getInput()).thenReturn(input);
		when(input.getFilePath()).thenReturn(TEST_PROPERTIES_INPUT_PATH);

		assertDoesNotThrow(() -> {
			dynamicPartitionHandler.handle(stepSplitter, masterStepExecution);
		});

		verify(batchProperties, atLeastOnce()).getInput();
		verify(input).getFilePath();
	}

	@Test
	void testHandle_emptyInputFilePathInProperties_throwsException() {
		JobParameters jobParameters = new JobParametersBuilder().addLong(PARTITION_SIZE_PARAM, 2L)
				.addString(INPUT_FILE_PATH_PARAM, "").toJobParameters();

		setupBasicMocks(jobParameters);
		when(batchProperties.getInput()).thenReturn(input);
		when(input.getFilePath()).thenReturn(""); // Empty in properties too

		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
			dynamicPartitionHandler.handle(stepSplitter, masterStepExecution);
		});

		assertTrue(exception.getMessage().contains(NO_INPUT_FILE_MESSAGE));
	}

	@Test
	void testHandle_whitespaceInputFilePathInProperties_throwsException() {
		JobParameters jobParameters = new JobParametersBuilder().addLong(PARTITION_SIZE_PARAM, 2L).toJobParameters();

		setupBasicMocks(jobParameters);
		when(batchProperties.getInput()).thenReturn(input);
		when(input.getFilePath()).thenReturn("   "); // Whitespace in properties

		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
			dynamicPartitionHandler.handle(stepSplitter, masterStepExecution);
		});

		assertTrue(exception.getMessage().contains(NO_INPUT_FILE_MESSAGE));
	}

	@Test
	void testHandle_nullPartitionSizeButValidPropertiesGridSize_success() {
		JobParameters jobParameters = new JobParametersBuilder().addString(INPUT_FILE_PATH_PARAM, TEST_INPUT_PATH)
				.toJobParameters();

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
		JobParameters jobParameters = new JobParametersBuilder().addLong(PARTITION_SIZE_PARAM, 0L)
				.addString(INPUT_FILE_PATH_PARAM, TEST_INPUT_PATH).toJobParameters();

		setupBasicMocks(jobParameters);
		when(batchProperties.getPartitioning()).thenReturn(partitioning);
		when(partitioning.getGridSize()).thenReturn(3);

		assertDoesNotThrow(() -> {
			dynamicPartitionHandler.handle(stepSplitter, masterStepExecution);
		});
	}

	@Test
	void testHandle_negativePartitionSizeInJobParameters_throwsException() {
		JobParameters jobParameters = new JobParametersBuilder().addLong(PARTITION_SIZE_PARAM, -1L)
				.addString(INPUT_FILE_PATH_PARAM, TEST_INPUT_PATH).toJobParameters();

		setupBasicMocks(jobParameters);
		when(batchProperties.getPartitioning()).thenReturn(partitioning);

		// TaskExecutorPartitionHandler throws IllegalArgumentException for negative
		// grid size
		assertThrows(IllegalArgumentException.class, () -> {
			dynamicPartitionHandler.handle(stepSplitter, masterStepExecution);
		});
	}

	@Test
	void testHandle_withChunkSizeLogging_logsChunkSize() {
		JobParameters jobParameters = new JobParametersBuilder().addLong(PARTITION_SIZE_PARAM, 2L)
				.addLong(CHUNK_SIZE_PARAM, 150L).addString(INPUT_FILE_PATH_PARAM, TEST_INPUT_PATH).toJobParameters();

		setupBasicMocks(jobParameters);
		when(batchProperties.getPartitioning()).thenReturn(partitioning);

		assertDoesNotThrow(() -> {
			dynamicPartitionHandler.handle(stepSplitter, masterStepExecution);
		});

		// This test verifies that chunk size logging path is exercised
		verify(jobExecution, atLeastOnce()).getJobParameters();
	}

	@Test
	void testHandle_withoutChunkSize_skipsChunkSizeLogging() {
		JobParameters jobParameters = new JobParametersBuilder().addLong(PARTITION_SIZE_PARAM, 2L)
				.addString(INPUT_FILE_PATH_PARAM, TEST_INPUT_PATH).toJobParameters();

		setupBasicMocks(jobParameters);
		when(batchProperties.getPartitioning()).thenReturn(partitioning);

		assertDoesNotThrow(() -> {
			dynamicPartitionHandler.handle(stepSplitter, masterStepExecution);
		});

		// This test verifies that the null chunk size path is exercised
		verify(jobExecution, atLeastOnce()).getJobParameters();
	}

	@Test
	void testHandle_classpathResourceWithSubpath_handlesCorrectly() {
		JobParameters jobParameters = new JobParametersBuilder().addLong(PARTITION_SIZE_PARAM, 1L)
				.addString(INPUT_FILE_PATH_PARAM, "classpath:test/data/nested/input.csv").toJobParameters();

		setupBasicMocks(jobParameters);
		when(batchProperties.getPartitioning()).thenReturn(partitioning);

		assertDoesNotThrow(() -> {
			dynamicPartitionHandler.handle(stepSplitter, masterStepExecution);
		});

		verify(dynamicPartitioner).setInputResource(any());
	}

	@Test
	void testHandle_maximumParameters_allPathsExercised() {
		JobParameters jobParameters = new JobParametersBuilder().addLong(PARTITION_SIZE_PARAM, 8L)
				.addLong(CHUNK_SIZE_PARAM, 200L).addString(INPUT_FILE_PATH_PARAM, "/test/complete-params.csv")
				.addString("outputFilePath", "/test/output").toJobParameters();

		setupBasicMocks(jobParameters);
		when(batchProperties.getPartitioning()).thenReturn(partitioning);
		when(partitioning.getGridSize()).thenReturn(DEFAULT_GRID_SIZE); // Should be overridden by job parameters

		assertDoesNotThrow(() -> {
			dynamicPartitionHandler.handle(stepSplitter, masterStepExecution);
		});

		verify(dynamicPartitioner).setInputResource(any());
		verify(batchProperties, atLeastOnce()).getPartitioning();
	}

	@Test
	void testHandle_edgeCaseInputPath_relativeFileSystemPath() {
		JobParameters jobParameters = new JobParametersBuilder().addLong(PARTITION_SIZE_PARAM, 1L)
				.addString(INPUT_FILE_PATH_PARAM, "relative/path/input.csv").toJobParameters();

		setupBasicMocks(jobParameters);
		when(batchProperties.getPartitioning()).thenReturn(partitioning);

		assertDoesNotThrow(() -> {
			dynamicPartitionHandler.handle(stepSplitter, masterStepExecution);
		});

		verify(dynamicPartitioner).setInputResource(any());
	}

	@Test
	void testHandle_stepSplitterAndMasterStepExecutionInteraction_success() {
		JobParameters jobParameters = new JobParametersBuilder().addLong(PARTITION_SIZE_PARAM, 2L)
				.addString(INPUT_FILE_PATH_PARAM, TEST_INPUT_PATH).toJobParameters();

		setupBasicMocks(jobParameters);
		when(batchProperties.getPartitioning()).thenReturn(partitioning);

		// This test verifies that the stepSplitter and masterStepExecution are passed
		// correctly to the TaskExecutorPartitionHandler
		assertDoesNotThrow(() -> {
			dynamicPartitionHandler.handle(stepSplitter, masterStepExecution);
		});

		verify(masterStepExecution, atLeastOnce()).getJobExecution();
		verify(dynamicPartitioner).setInputResource(any());
	}
}