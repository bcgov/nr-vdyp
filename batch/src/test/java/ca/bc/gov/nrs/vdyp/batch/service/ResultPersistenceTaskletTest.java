package ca.bc.gov.nrs.vdyp.batch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.springframework.batch.repeat.RepeatStatus;

import ca.bc.gov.nrs.vdyp.batch.client.vdyp.FileMappingDetails;
import ca.bc.gov.nrs.vdyp.batch.client.vdyp.VdypClient;
import ca.bc.gov.nrs.vdyp.batch.client.vdyp.VdypProjectionDetails;
import ca.bc.gov.nrs.vdyp.batch.exception.BatchResultPersistenceException;
import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;
import ca.bc.gov.nrs.vdyp.batch.util.BatchUtils;

@ExtendWith(MockitoExtension.class)
class ResultPersistenceTaskletTest {
	@Mock
	ComsFileService comsFileService;
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

	ResultPersistenceTasklet tasklet;
	UUID projectionGuid = UUID.randomUUID();

	@TempDir
	Path tempDir;

	@BeforeEach
	void setup() {
		tasklet = new ResultPersistenceTasklet(comsFileService, vdypClient);

		when(chunkContext.getStepContext()).thenReturn(stepContext);
		when(stepContext.getStepExecution()).thenReturn(stepExecution);
		when(stepExecution.getJobExecution()).thenReturn(jobExecution);
	}

	@Test
	void testExecute_nullJobGuid_ThrowsException() {
		projectionGuid = UUID.randomUUID();
		jobParameters = new JobParametersBuilder().addString(BatchConstants.Job.BASE_DIR, tempDir.toString())
				.addLong(BatchConstants.Partition.NUMBER, 4L)
				.addString(BatchConstants.GuidInput.PROJECTION_GUID, projectionGuid.toString()).toJobParameters();
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		assertThrows(BatchResultPersistenceException.class, () -> {
			tasklet.execute(stepContribution, chunkContext);
		});
	}

	@Test
	void testExecute_nullBaseDir_ThrowsException() {
		projectionGuid = UUID.randomUUID();
		jobParameters = new JobParametersBuilder().addString(BatchConstants.Job.GUID, "job-123")
				.addLong(BatchConstants.Partition.NUMBER, 4L)
				.addString(BatchConstants.GuidInput.PROJECTION_GUID, projectionGuid.toString()).toJobParameters();
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		assertThrows(BatchResultPersistenceException.class, () -> {
			tasklet.execute(stepContribution, chunkContext);
		});
	}

	@Test
	void testExecute_nullPartitions_ThrowsException() {
		projectionGuid = UUID.randomUUID();
		jobParameters = new JobParametersBuilder().addString(BatchConstants.Job.GUID, "job-123")
				.addString(BatchConstants.Job.BASE_DIR, tempDir.toString())
				.addString(BatchConstants.GuidInput.PROJECTION_GUID, projectionGuid.toString()).toJobParameters();
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		assertThrows(BatchResultPersistenceException.class, () -> {
			tasklet.execute(stepContribution, chunkContext);
		});
	}

	@Test
	void testExecute_nullPolygonFile_ThrowsException() {
		jobParameters = new JobParametersBuilder().addString(BatchConstants.Job.GUID, "job-123")
				.addString(BatchConstants.Job.BASE_DIR, tempDir.toString()).addLong(BatchConstants.Partition.NUMBER, 4L)
				.toJobParameters();
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		assertThrows(BatchResultPersistenceException.class, () -> {
			tasklet.execute(stepContribution, chunkContext);
		});
	}

	@Test
	void testExecute_filesExist_comsFileExistsCallComsClient() throws Exception {
		UUID resultFileSetGuid = UUID.randomUUID();
		UUID resultFileComsObjectGuid = UUID.randomUUID();

		jobParameters = new JobParametersBuilder().addString(BatchConstants.Job.GUID, "job-123")
				.addString(BatchConstants.Job.BASE_DIR, tempDir.toString()) //
				.addString(BatchConstants.Job.TIMESTAMP, BatchUtils.createJobTimestamp()) //
				.addLong(BatchConstants.Partition.NUMBER, 4L) //
				.addString(BatchConstants.GuidInput.PROJECTION_GUID, projectionGuid.toString()) //
				.toJobParameters();
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(vdypClient.getProjectionDetails(any())).thenReturn(details);
		when(details.resultFileSet())
				.thenReturn(new VdypProjectionDetails.VdypProjectionFileSet(resultFileSetGuid.toString()));
		when(vdypClient.getFileSetFiles(any(), matches(resultFileSetGuid.toString()), eq(false))).thenReturn(
				List.of(
						new FileMappingDetails(
								UUID.randomUUID().toString(), resultFileComsObjectGuid.toString(), "http://test.com"
						)
				)
		);

		doNothing().when(comsFileService).updateStoredObject(any(UUID.class), any(Path.class));

		Path finalZipPath = BatchUtils.getFinalZipName(tempDir, jobParameters.getString(BatchConstants.Job.TIMESTAMP));
		Files.createFile(finalZipPath); // ← makes Files.exists(...) return true

		// Act
		RepeatStatus status = tasklet.execute(stepContribution, chunkContext);

		// Assert
		assertEquals(RepeatStatus.FINISHED, status);
		verify(comsFileService).updateStoredObject(eq(resultFileComsObjectGuid), any(Path.class));
	}

	@Test
	void testExecute_filesExist_emptyFileSetCallsVDYPClient() throws Exception {
		UUID resultFileSetGuid = UUID.randomUUID();

		jobParameters = new JobParametersBuilder() //
				.addString(BatchConstants.Job.GUID, "job-123") //
				.addString(BatchConstants.Job.BASE_DIR, tempDir.toString()) //
				.addLong(BatchConstants.Partition.NUMBER, 4L) //
				.addString(BatchConstants.GuidInput.PROJECTION_GUID, projectionGuid.toString()) //
				.addString(BatchConstants.Job.TIMESTAMP, BatchUtils.createJobTimestamp()) //
				.toJobParameters();
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(vdypClient.getProjectionDetails(any())).thenReturn(details);
		when(details.resultFileSet())
				.thenReturn(new VdypProjectionDetails.VdypProjectionFileSet(resultFileSetGuid.toString()));
		when(vdypClient.getFileSetFiles(any(), matches(resultFileSetGuid.toString()), eq(false))).thenReturn(List.of());

		doNothing().when(vdypClient).uploadFileToFileSet(
				matches(projectionGuid.toString()), matches(resultFileSetGuid.toString()), any(Path.class)
		);

		Path finalZipPath = BatchUtils.getFinalZipName(tempDir, jobParameters.getString(BatchConstants.Job.TIMESTAMP));
		Files.createFile(finalZipPath); // ← makes Files.exists(...) return true
		// Act
		RepeatStatus status = tasklet.execute(stepContribution, chunkContext);

		// Assert
		assertEquals(RepeatStatus.FINISHED, status);
		verify(vdypClient)
				.uploadFileToFileSet(eq(projectionGuid.toString()), eq(resultFileSetGuid.toString()), any(Path.class));
	}
}
