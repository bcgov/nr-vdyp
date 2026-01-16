package ca.bc.gov.nrs.vdyp.batch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
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
import org.springframework.batch.repeat.RepeatStatus;

import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;

@ExtendWith(MockitoExtension.class)
class DownloadAndPartitionTaskletTest {
	@Mock
	ComsFileService comsFileService;

	@Mock
	BatchInputPartitioner inputPartitioner;

	@Mock
	ChunkContext chunkContext;
	@Mock
	StepContext stepContext;
	@Mock
	StepExecution stepExecution;
	@Mock
	JobExecution jobExecution;

	JobParameters jobParameters;

	@Mock
	StepContribution stepContribution;

	DownloadAndPartitionTasklet tasklet;
	UUID polygonComsObjectGuid = UUID.randomUUID();
	UUID layerComsObjectGuid = UUID.randomUUID();

	@TempDir
	Path tempDir;

	@BeforeEach
	void setup() {
		tasklet = new DownloadAndPartitionTasklet(comsFileService, inputPartitioner);

		when(chunkContext.getStepContext()).thenReturn(stepContext);
		when(stepContext.getStepExecution()).thenReturn(stepExecution);
		when(stepExecution.getJobExecution()).thenReturn(jobExecution);
	}

	@Test
	void testExecute_nullJobGuid_ThrowsException() {
		polygonComsObjectGuid = UUID.randomUUID();
		layerComsObjectGuid = UUID.randomUUID();
		jobParameters = new JobParametersBuilder().addString(BatchConstants.Job.BASE_DIR, tempDir.toString())
				.addLong(BatchConstants.Partition.NUMBER, 4L)
				.addString(BatchConstants.ComsInput.POLYGON_COMS_OBJECT_GUID, polygonComsObjectGuid.toString())
				.addString(BatchConstants.ComsInput.LAYER_COMS_OBJECT_GUID, layerComsObjectGuid.toString())
				.toJobParameters();
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		assertThrows(IllegalArgumentException.class, () -> {
			tasklet.execute(stepContribution, chunkContext);
		});
	}

	@Test
	void testExecute_nullBaseDir_ThrowsException() {
		polygonComsObjectGuid = UUID.randomUUID();
		layerComsObjectGuid = UUID.randomUUID();
		jobParameters = new JobParametersBuilder().addString(BatchConstants.Job.GUID, "job-123")
				.addLong(BatchConstants.Partition.NUMBER, 4L)
				.addString(BatchConstants.ComsInput.POLYGON_COMS_OBJECT_GUID, polygonComsObjectGuid.toString())
				.addString(BatchConstants.ComsInput.LAYER_COMS_OBJECT_GUID, layerComsObjectGuid.toString())
				.toJobParameters();
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		assertThrows(IllegalArgumentException.class, () -> {
			tasklet.execute(stepContribution, chunkContext);
		});
	}

	@Test
	void testExecute_nullPartitions_ThrowsException() {
		polygonComsObjectGuid = UUID.randomUUID();
		layerComsObjectGuid = UUID.randomUUID();
		jobParameters = new JobParametersBuilder().addString(BatchConstants.Job.GUID, "job-123")
				.addString(BatchConstants.Job.BASE_DIR, tempDir.toString())
				.addString(BatchConstants.ComsInput.POLYGON_COMS_OBJECT_GUID, polygonComsObjectGuid.toString())
				.addString(BatchConstants.ComsInput.LAYER_COMS_OBJECT_GUID, layerComsObjectGuid.toString())
				.toJobParameters();
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		assertThrows(IllegalArgumentException.class, () -> {
			tasklet.execute(stepContribution, chunkContext);
		});
	}

	@Test
	void testExecute_nullPolygonFile_ThrowsException() {
		layerComsObjectGuid = UUID.randomUUID();
		jobParameters = new JobParametersBuilder().addString(BatchConstants.Job.GUID, "job-123")
				.addString(BatchConstants.Job.BASE_DIR, tempDir.toString()).addLong(BatchConstants.Partition.NUMBER, 4L)
				.addString(BatchConstants.ComsInput.LAYER_COMS_OBJECT_GUID, layerComsObjectGuid.toString())
				.toJobParameters();
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		assertThrows(IllegalArgumentException.class, () -> {
			tasklet.execute(stepContribution, chunkContext);
		});
	}

	@Test
	void testExecute_nullLaayerFile_ThrowsException() {
		polygonComsObjectGuid = UUID.randomUUID();
		jobParameters = new JobParametersBuilder().addString(BatchConstants.Job.GUID, "job-123")
				.addString(BatchConstants.Job.BASE_DIR, tempDir.toString()).addLong(BatchConstants.Partition.NUMBER, 4L)
				.addString(BatchConstants.ComsInput.POLYGON_COMS_OBJECT_GUID, polygonComsObjectGuid.toString())
				.toJobParameters();
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		assertThrows(IllegalArgumentException.class, () -> {
			tasklet.execute(stepContribution, chunkContext);
		});
	}

	@Test
	void testExecute_filesExist_partitioningDone() throws Exception {
		polygonComsObjectGuid = UUID.randomUUID();
		layerComsObjectGuid = UUID.randomUUID();
		jobParameters = new JobParametersBuilder().addString(BatchConstants.Job.GUID, "job-123")
				.addString(BatchConstants.Job.BASE_DIR, tempDir.toString()).addLong(BatchConstants.Partition.NUMBER, 4L)
				.addString(BatchConstants.ComsInput.POLYGON_COMS_OBJECT_GUID, polygonComsObjectGuid.toString())
				.addString(BatchConstants.ComsInput.LAYER_COMS_OBJECT_GUID, layerComsObjectGuid.toString())
				.toJobParameters();
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);

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
