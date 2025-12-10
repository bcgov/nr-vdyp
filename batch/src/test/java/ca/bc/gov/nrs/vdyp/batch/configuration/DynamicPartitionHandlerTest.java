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

	private static final String PARTITION_SIZE_PARAM = "partitionSize";
	private static final String TEST_JOB_BASE_DIR = "/tmp/test";
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
	private BatchProperties.PartitionProperties partition;

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
	void testHandle_WithPartitionSizeAndJobBaseDir() {
		JobParameters jobParameters = new JobParametersBuilder().addLong(PARTITION_SIZE_PARAM, 4L)
				.addString("jobBaseDir", TEST_JOB_BASE_DIR).toJobParameters();

		setupBasicMocks(jobParameters);

		assertDoesNotThrow(() -> dynamicPartitionHandler.handle(stepSplitter, masterStepExecution));

		verify(dynamicPartitioner).setJobBaseDir(TEST_JOB_BASE_DIR);
	}

	@Test
	void testHandle_WithNullPartitionSize() {
		JobParameters jobParameters = new JobParametersBuilder().toJobParameters();

		setupBasicMocks(jobParameters);
		when(batchProperties.getPartition()).thenReturn(partition);
		when(partition.getDefaultPartitionSize()).thenReturn(DEFAULT_GRID_SIZE);

		assertDoesNotThrow(() -> {
			dynamicPartitionHandler.handle(stepSplitter, masterStepExecution);
		});

		verify(batchProperties, atLeastOnce()).getPartition();
		verify(partition, atLeastOnce()).getDefaultPartitionSize();
	}

	@Test
	void testHandle_WithNullJobBaseDir() {
		JobParameters jobParameters = new JobParametersBuilder().addLong(PARTITION_SIZE_PARAM, 2L).toJobParameters();

		setupBasicMocks(jobParameters);

		assertDoesNotThrow(() -> {
			dynamicPartitionHandler.handle(stepSplitter, masterStepExecution);
		});

		verify(dynamicPartitioner, never()).setJobBaseDir(any());
	}

	@Test
	void testHandle_WithEmptyJobBaseDir() {
		JobParameters jobParameters = new JobParametersBuilder().addLong(PARTITION_SIZE_PARAM, 2L)
				.addString("jobBaseDir", "").toJobParameters();

		setupBasicMocks(jobParameters);

		assertDoesNotThrow(() -> {
			dynamicPartitionHandler.handle(stepSplitter, masterStepExecution);
		});

		verify(dynamicPartitioner).setJobBaseDir("");
	}

	@Test
	void testHandle_WithZeroPartitionSize() {
		JobParameters jobParameters = new JobParametersBuilder().addLong(PARTITION_SIZE_PARAM, 0L).toJobParameters();

		setupBasicMocks(jobParameters);

		assertDoesNotThrow(() -> {
			dynamicPartitionHandler.handle(stepSplitter, masterStepExecution);
		});
	}

	@Test
	void testHandle_WithNegativePartitionSize() {
		JobParameters jobParameters = new JobParametersBuilder().addLong(PARTITION_SIZE_PARAM, -1L).toJobParameters();

		setupBasicMocks(jobParameters);

		assertThrows(IllegalArgumentException.class, () -> {
			dynamicPartitionHandler.handle(stepSplitter, masterStepExecution);
		});
	}
}
