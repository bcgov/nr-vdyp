package ca.bc.gov.nrs.vdyp.batch.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;

import ca.bc.gov.nrs.vdyp.batch.client.vdyp.FileMappingDetails;
import ca.bc.gov.nrs.vdyp.batch.client.vdyp.VdypClient;
import ca.bc.gov.nrs.vdyp.batch.client.vdyp.VdypProjectionDetails;
import ca.bc.gov.nrs.vdyp.batch.configuration.BatchProperties;
import ca.bc.gov.nrs.vdyp.batch.exception.BatchPartitionException;
import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;

@ExtendWith(MockitoExtension.class)
class DownloadAndPartitionTaskletTest {
	@Mock
	ComsFileService comsFileService;

	@Mock
	BatchInputPartitioner inputPartitioner;
	@Mock
	VdypClient vdypClient;
	@Mock
	BatchProperties batchProperties;
	@Mock
	BatchProperties.ReaderProperties readerProperties;
	@Mock
	BatchProperties.ThreadPoolProperties threadPoolProperties;

	@Mock
	ChunkContext chunkContext;
	@Mock
	StepContext stepContext;
	@Mock
	StepExecution stepExecution;
	@Mock
	JobExecution jobExecution;
	@Mock
	VdypProjectionDetails details;
	JobParameters jobParameters;

	@Mock
	StepContribution stepContribution;

	DownloadAndPartitionTasklet tasklet;
	UUID projectionGuid = UUID.randomUUID();
	UUID polygonComsObjectGuid = UUID.randomUUID();
	UUID layerComsObjectGuid = UUID.randomUUID();

	@TempDir
	Path tempDir;

	@BeforeEach
	void setup() {
		tasklet = new DownloadAndPartitionTasklet(comsFileService, inputPartitioner, vdypClient, batchProperties);

		lenient().when(chunkContext.getStepContext()).thenReturn(stepContext);
		lenient().when(stepContext.getStepExecution()).thenReturn(stepExecution);
		lenient().when(stepExecution.getJobExecution()).thenReturn(jobExecution);
	}

	@Test
	void testExecute_nullJobGuid_ThrowsException() {
		projectionGuid = UUID.randomUUID();
		polygonComsObjectGuid = UUID.randomUUID();
		layerComsObjectGuid = UUID.randomUUID();
		jobParameters = new JobParametersBuilder().addString(BatchConstants.Job.BASE_DIR, tempDir.toString())
				.addLong(BatchConstants.Partition.NUMBER, 4L)
				.addString(BatchConstants.GuidInput.PROJECTION_GUID, projectionGuid.toString()).toJobParameters();
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		assertThrows(BatchPartitionException.class, () -> {
			tasklet.execute(stepContribution, chunkContext);
		});
	}

	@Test
	void testExecute_nullBaseDir_ThrowsException() {
		projectionGuid = UUID.randomUUID();
		polygonComsObjectGuid = UUID.randomUUID();
		layerComsObjectGuid = UUID.randomUUID();
		jobParameters = new JobParametersBuilder().addString(BatchConstants.Job.GUID, "job-123")
				.addLong(BatchConstants.Partition.NUMBER, 4L)
				.addString(BatchConstants.GuidInput.PROJECTION_GUID, projectionGuid.toString()).toJobParameters();
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		assertThrows(BatchPartitionException.class, () -> {
			tasklet.execute(stepContribution, chunkContext);
		});
	}

	@Test
	void testExecute_nullPartitions_ThrowsException() {
		projectionGuid = UUID.randomUUID();
		polygonComsObjectGuid = UUID.randomUUID();
		layerComsObjectGuid = UUID.randomUUID();
		jobParameters = new JobParametersBuilder().addString(BatchConstants.Job.GUID, "job-123")
				.addString(BatchConstants.Job.BASE_DIR, tempDir.toString())
				.addString(BatchConstants.GuidInput.PROJECTION_GUID, projectionGuid.toString()).toJobParameters();
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		assertThrows(BatchPartitionException.class, () -> {
			tasklet.execute(stepContribution, chunkContext);
		});
	}

	@Test
	void testExecute_nullPolygonFile_ThrowsException() {
		layerComsObjectGuid = UUID.randomUUID();
		jobParameters = new JobParametersBuilder().addString(BatchConstants.Job.GUID, "job-123")
				.addString(BatchConstants.Job.BASE_DIR, tempDir.toString()).addLong(BatchConstants.Partition.NUMBER, 4L)
				.toJobParameters();
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		assertThrows(BatchPartitionException.class, () -> {
			tasklet.execute(stepContribution, chunkContext);
		});
	}

	@Test
	void testExecute_filesExist_partitioningDone() throws Exception {
		UUID polygonFileSetGuid = UUID.randomUUID();
		UUID layerFileSetGuid = UUID.randomUUID();
		polygonComsObjectGuid = UUID.randomUUID();
		layerComsObjectGuid = UUID.randomUUID();

		jobParameters = new JobParametersBuilder().addString(BatchConstants.Job.GUID, "job-123")
				.addString(BatchConstants.Job.BASE_DIR, tempDir.toString()).addLong(BatchConstants.Partition.NUMBER, 4L)
				.addString(BatchConstants.GuidInput.PROJECTION_GUID, projectionGuid.toString()).toJobParameters();
		ExecutionContext executionContext = new ExecutionContext();
		when(vdypClient.getProjectionDetails(any())).thenReturn(details);
		when(details.polygonFileSet())
				.thenReturn(new VdypProjectionDetails.VdypProjectionFileSet(polygonFileSetGuid.toString()));
		when(details.layerFileSet())
				.thenReturn(new VdypProjectionDetails.VdypProjectionFileSet(layerFileSetGuid.toString()));
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobExecution.getExecutionContext()).thenReturn(executionContext);
		when(vdypClient.getFileSetFiles(any(), matches(polygonFileSetGuid.toString()))).thenReturn(
				List.of(new FileMappingDetails(polygonFileSetGuid.toString(), polygonComsObjectGuid.toString()))
		);
		when(vdypClient.getFileSetFiles(any(), matches(layerFileSetGuid.toString()))).thenReturn(
				List.of(new FileMappingDetails(layerFileSetGuid.toString(), layerComsObjectGuid.toString()))
		);

		when(batchProperties.getReader()).thenReturn(readerProperties);
		when(readerProperties.getDefaultChunkSize()).thenReturn(150);
		when(batchProperties.getThreadPool()).thenReturn(threadPoolProperties);
		when(threadPoolProperties.getMaxJobThreads()).thenReturn(4);

		// fetchObjectToFile is a no-op in tests; create the input files manually so the
		// tasklet can open them to count polygons before partitioning.
		Path inputDir = tempDir.resolve("input");
		Files.createDirectories(inputDir);
		Files.writeString(inputDir.resolve("polygon.csv"), "FEATURE_ID\n");
		Files.writeString(inputDir.resolve("layer.csv"), "LAYER_ID\n");
		doNothing().when(comsFileService).fetchObjectToFile(any(UUID.class), any(Path.class));

		// Act
		RepeatStatus status = tasklet.execute(stepContribution, chunkContext);

		// Assert
		assertEquals(RepeatStatus.FINISHED, status);
		verify(comsFileService).fetchObjectToFile(eq(polygonComsObjectGuid), any(Path.class));
		verify(comsFileService).fetchObjectToFile(eq(layerComsObjectGuid), any(Path.class));
		verify(inputPartitioner).partitionCsvFiles(
				eq(tempDir.resolve("input/polygon.csv")), eq(tempDir.resolve("input/layer.csv")), anyInt(), eq(tempDir),
				eq("job-123"), anyInt()
		);
		verify(vdypClient).pushProgress(eq(projectionGuid.toString()), any());

		// Verify original input files are deleted after partitioning
		assertFalse(Files.exists(inputDir.resolve("polygon.csv")), "polygon.csv should be deleted after partitioning");
		assertFalse(Files.exists(inputDir.resolve("layer.csv")), "layer.csv should be deleted after partitioning");
		assertFalse(Files.exists(inputDir), "input directory should be deleted after partitioning");
	}

	@Test
	void testDeleteOriginalInputDirectory_ioExceptionIsSwallowedAsWarning() {
		DownloadAndPartitionTasklet testTasklet = new DownloadAndPartitionTasklet(
				comsFileService, inputPartitioner, vdypClient, batchProperties
		) {
			@Override
			protected void deleteDirectory(Path dir) throws IOException {
				throw new IOException("simulated disk error");
			}
		};

		assertDoesNotThrow(() -> testTasklet.deleteOriginalInputDirectory(tempDir));
	}

	@Test
	void testExecute_inputFilesDeletedEvenWhenPartitionerReturnsZero() throws Exception {
		UUID polygonFileSetGuid = UUID.randomUUID();
		UUID layerFileSetGuid = UUID.randomUUID();
		polygonComsObjectGuid = UUID.randomUUID();
		layerComsObjectGuid = UUID.randomUUID();

		jobParameters = new JobParametersBuilder().addString(BatchConstants.Job.GUID, "job-456")
				.addString(BatchConstants.Job.BASE_DIR, tempDir.toString()).addLong(BatchConstants.Partition.NUMBER, 2L)
				.addString(BatchConstants.GuidInput.PROJECTION_GUID, projectionGuid.toString()).toJobParameters();
		ExecutionContext executionContext = new ExecutionContext();
		when(vdypClient.getProjectionDetails(any())).thenReturn(details);
		when(details.polygonFileSet())
				.thenReturn(new VdypProjectionDetails.VdypProjectionFileSet(polygonFileSetGuid.toString()));
		when(details.layerFileSet())
				.thenReturn(new VdypProjectionDetails.VdypProjectionFileSet(layerFileSetGuid.toString()));
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobExecution.getExecutionContext()).thenReturn(executionContext);
		when(vdypClient.getFileSetFiles(any(), matches(polygonFileSetGuid.toString()))).thenReturn(
				List.of(new FileMappingDetails(polygonFileSetGuid.toString(), polygonComsObjectGuid.toString()))
		);
		when(vdypClient.getFileSetFiles(any(), matches(layerFileSetGuid.toString()))).thenReturn(
				List.of(new FileMappingDetails(layerFileSetGuid.toString(), layerComsObjectGuid.toString()))
		);

		when(batchProperties.getReader()).thenReturn(readerProperties);
		when(readerProperties.getDefaultChunkSize()).thenReturn(150);
		when(batchProperties.getThreadPool()).thenReturn(threadPoolProperties);
		when(threadPoolProperties.getMaxJobThreads()).thenReturn(4);

		Path inputDir = tempDir.resolve("input");
		Files.createDirectories(inputDir);
		Files.writeString(inputDir.resolve("polygon.csv"), "FEATURE_ID\n12345\n");
		Files.writeString(inputDir.resolve("layer.csv"), "LAYER_ID\n");
		doNothing().when(comsFileService).fetchObjectToFile(any(UUID.class), any(Path.class));

		RepeatStatus status = tasklet.execute(stepContribution, chunkContext);

		assertEquals(RepeatStatus.FINISHED, status);
		assertFalse(Files.exists(inputDir), "input directory should be cleaned up after partitioning");
	}

}
