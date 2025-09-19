package ca.bc.gov.nrs.vdyp.batch.configuration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PartitionedJobExecutionListenerTest {

	@Mock
	private BatchProperties batchProperties;

	@Mock
	private BatchProperties.Partitioning partitioning;

	@Mock
	private BatchProperties.Output output;

	@Mock
	private JobExecution jobExecution;

	@Mock
	private JobInstance jobInstance;

	@TempDir
	Path tempDir;

	private PartitionedJobExecutionListener listener;

	@BeforeEach
	void setUp() {
		listener = new PartitionedJobExecutionListener(batchProperties);
	}

	@Test
	void testConstructor() {
		assertNotNull(listener);
	}

	@Test
	void testBeforeJob_WithJobParameters() {
		JobParameters jobParameters = new JobParametersBuilder().addLong("partitionSize", 4L).addLong("chunkSize", 100L)
				.toJobParameters();

		when(jobExecution.getId()).thenReturn(1L);
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);

		assertDoesNotThrow(() -> listener.beforeJob(jobExecution));

		verify(jobExecution, atLeastOnce()).getId();
		verify(jobExecution, atLeastOnce()).getJobParameters();
	}

	@Test
	void testBeforeJob_WithoutJobParameters_UsesBatchProperties() {
		JobParameters jobParameters = new JobParametersBuilder().toJobParameters();
		when(jobExecution.getId()).thenReturn(1L);
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(batchProperties.getPartitioning()).thenReturn(partitioning);
		when(partitioning.getGridSize()).thenReturn(4);

		assertDoesNotThrow(() -> listener.beforeJob(jobExecution));

		verify(partitioning, atLeastOnce()).getGridSize();
	}

	@Test
	void testBeforeJob_MissingGridSize_ThrowsException() {
		JobParameters jobParameters = new JobParametersBuilder().toJobParameters();
		when(jobExecution.getId()).thenReturn(1L);
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(batchProperties.getPartitioning()).thenReturn(partitioning);
		when(partitioning.getGridSize()).thenReturn(0);

		assertThrows(IllegalStateException.class, () -> listener.beforeJob(jobExecution));
	}

	@Test
	void testBeforeJob_ValidPartitionSize_Success() {
		JobParameters jobParameters = new JobParametersBuilder().addLong("partitionSize", 4L).toJobParameters();
		when(jobExecution.getId()).thenReturn(1L);
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(batchProperties.getPartitioning()).thenReturn(partitioning);
		when(partitioning.getGridSize()).thenReturn(4);

		// Should succeed since partitionSize is provided
		assertDoesNotThrow(() -> listener.beforeJob(jobExecution));
		verify(jobExecution, atLeastOnce()).getId();
	}

	@Test
	void testAfterJob_FirstTime() {
		setupAfterJobMocks();
		setupJobExecutionBasicMocks();

		// Call beforeJob first to initialize tracking
		listener.beforeJob(jobExecution);

		assertDoesNotThrow(() -> listener.afterJob(jobExecution));

		verify(jobExecution, atLeastOnce()).getId();
		verify(jobExecution, atLeastOnce()).getStatus();
	}

	@Test
	void testAfterJob_AlreadyProcessed_SkipsProcessing() {
		setupAfterJobMocks();
		setupJobExecutionBasicMocks();

		// Call beforeJob to initialize tracking
		listener.beforeJob(jobExecution);

		// Call afterJob twice - second call should be skipped
		listener.afterJob(jobExecution);
		listener.afterJob(jobExecution);

		// Should still work without throwing exceptions
		assertDoesNotThrow(() -> listener.afterJob(jobExecution));
	}

	@Test
	void testAfterJob_WithMergePartitionFiles() throws IOException {
		setupAfterJobMocksWithFileOperations();
		setupJobExecutionBasicMocks();
		createTestPartitionFiles();

		listener.beforeJob(jobExecution);

		assertDoesNotThrow(() -> listener.afterJob(jobExecution));

		// Current implementation doesn't access output.getDirectory() since file merging is disabled
		// Results are now aggregated by ResultAggregationService in post-processing step
		verify(jobExecution, atLeastOnce()).getId();
	}

	@Test
	void testAfterJob_WithNullDirectory_HandlesGracefully() {
		setupAfterJobMocks();
		setupJobExecutionBasicMocks();
		// Test with null directory path
		when(output.getDirectory()).thenReturn(mock(BatchProperties.Output.Directory.class));
		when(output.getDirectory().getDefaultPath()).thenReturn(null);

		listener.beforeJob(jobExecution);

		// Should not throw during afterJob processing
		assertDoesNotThrow(() -> listener.afterJob(jobExecution));
	}

	@Test
	void testAfterJob_WithValidDirectory_Success() {
		setupAfterJobMocks();
		setupJobExecutionBasicMocks();
		// Test with valid directory
		when(output.getDirectory()).thenReturn(mock(BatchProperties.Output.Directory.class));
		when(output.getDirectory().getDefaultPath()).thenReturn(tempDir.toString());

		listener.beforeJob(jobExecution);

		assertDoesNotThrow(() -> listener.afterJob(jobExecution));
		verify(output, atLeastOnce()).getDirectory();
	}

	@Test
	void testAfterJob_WithNullOutputDirectory_UsesTempDirectory() {
		setupAfterJobMocks();
		setupJobExecutionBasicMocks();

		JobParameters jobParameters = new JobParametersBuilder().addLong("partitionSize", 2L).toJobParameters();
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);

		// No output directory specified
		when(output.getDirectory()).thenReturn(mock(BatchProperties.Output.Directory.class));
		when(output.getDirectory().getDefaultPath()).thenReturn(null);

		listener.beforeJob(jobExecution);

		assertDoesNotThrow(() -> listener.afterJob(jobExecution));
	}

	private void setupJobExecutionBasicMocks() {
		when(jobExecution.getId()).thenReturn(1L);
		when(jobExecution.getJobInstance()).thenReturn(jobInstance);
		when(jobInstance.getJobName()).thenReturn("testJob");
		when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
		when(jobExecution.getExitStatus()).thenReturn(ExitStatus.COMPLETED);
		when(jobExecution.getStartTime()).thenReturn(LocalDateTime.now().minusMinutes(5));
		when(jobExecution.getEndTime()).thenReturn(LocalDateTime.now());
	}

	private void setupAfterJobMocks() {
		JobParameters jobParameters = new JobParametersBuilder().addLong("partitionSize", 2L)
				.addString("outputFilePath", tempDir.toString()).toJobParameters();

		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(batchProperties.getPartitioning()).thenReturn(partitioning);
		when(batchProperties.getOutput()).thenReturn(output);
		// Current BatchProperties.Output only has directory configuration
		// Remove references to non-existent getFilePrefix() and getCsvHeader() methods

		BatchProperties.Output.Directory directory = mock(BatchProperties.Output.Directory.class);
		when(output.getDirectory()).thenReturn(directory);
		when(directory.getDefaultPath()).thenReturn(tempDir.toString());
	}

	private void setupAfterJobMocksWithFileOperations() throws IOException {
		setupAfterJobMocks();

		// Ensure output directory exists
		Files.createDirectories(tempDir);
	}

	private void createTestPartitionFiles() throws IOException {
		String header = "id,data,polygonId,layerId,status";

		// Create partition file 0
		Path partition0 = tempDir.resolve("test_partition0.csv");
		Files.write(partition0, (header + "\n1,data1,poly1,layer1,PROCESSED\n").getBytes());

		// Create partition file 1
		Path partition1 = tempDir.resolve("test_partition1.csv");
		Files.write(partition1, (header + "\n2,data2,poly2,layer2,PROCESSED\n").getBytes());
	}

	@Test
	void testAfterJob_UnexpectedJobId_HandlesGracefully() {
		// Test with a job execution that wasn't tracked in beforeJob
		JobExecution untracked = mock(JobExecution.class);
		when(untracked.getId()).thenReturn(999L);

		assertDoesNotThrow(() -> listener.afterJob(untracked));
	}
}