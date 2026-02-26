package ca.bc.gov.nrs.vdyp.batch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
		tasklet = new DownloadAndPartitionTasklet(comsFileService, inputPartitioner, vdypClient);

		when(chunkContext.getStepContext()).thenReturn(stepContext);
		when(stepContext.getStepExecution()).thenReturn(stepExecution);
		when(stepExecution.getJobExecution()).thenReturn(jobExecution);
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

		doNothing().when(comsFileService).fetchObjectToFile(any(UUID.class), any(Path.class));

		// Act
		RepeatStatus status = tasklet.execute(stepContribution, chunkContext);

		// Assert
		assertEquals(RepeatStatus.FINISHED, status);
		verify(comsFileService).fetchObjectToFile(eq(polygonComsObjectGuid), any(Path.class));
		verify(comsFileService).fetchObjectToFile(eq(layerComsObjectGuid), any(Path.class));
		verify(inputPartitioner).partitionCsvFiles(
				tempDir.resolve("input/polygon.csv"), tempDir.resolve("input/layer.csv"), 4, tempDir, "job-123"
		);
	}

}
