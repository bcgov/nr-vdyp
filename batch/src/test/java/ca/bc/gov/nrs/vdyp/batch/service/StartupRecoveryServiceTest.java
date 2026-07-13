package ca.bc.gov.nrs.vdyp.batch.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.item.ExecutionContext;

import ca.bc.gov.nrs.vdyp.batch.client.vdyp.VdypClient;
import ca.bc.gov.nrs.vdyp.batch.model.VDYPProjectionProgressUpdate;
import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;

@ExtendWith(MockitoExtension.class)
class StartupRecoveryServiceTest {

	@Mock
	JobExplorer jobExplorer;

	@Mock
	JobLauncher jobLauncher;

	@Mock
	Job fetchAndPartitionJob;

	@Mock
	BatchRecoveryMetadataService recoveryMetadataService;

	@Mock
	VdypClient vdypClient;

	@TempDir
	Path tempDir;

	StartupRecoveryService service;

	@BeforeEach
	void setUp() {
		service = new StartupRecoveryService(
				jobExplorer, jobLauncher, fetchAndPartitionJob, recoveryMetadataService, vdypClient
		);
	}

	@Test
	void start_whenNoStaleExecutions_marksServiceRunningWithoutLaunchingJob() throws Exception {
		when(jobExplorer.findRunningJobExecutions("VdypFetchAndPartitionJob")).thenReturn(Set.of());

		service.start();

		assertTrue(service.isRunning());
		verifyNoInteractions(jobLauncher, recoveryMetadataService);
	}

	@Test
	void start_whenStaleExecutionExists_marksItFailedThenRestartsWithAsyncLauncher() throws Exception {
		JobParameters parameters = new JobParametersBuilder()
				.addString(BatchConstants.Job.GUID, "7c26643a-50cb-497e-a539-afac6966ecea").toJobParameters();
		JobExecution staleExecution = new JobExecution(1L, parameters);
		staleExecution.setStatus(BatchStatus.STARTED);
		JobExecution restartedExecution = new JobExecution(2L, parameters);

		when(jobExplorer.findRunningJobExecutions("VdypFetchAndPartitionJob")).thenReturn(Set.of(staleExecution));
		when(jobLauncher.run(fetchAndPartitionJob, parameters)).thenReturn(restartedExecution);

		service.start();

		assertTrue(service.isRunning());
		InOrder inOrder = inOrder(recoveryMetadataService, jobLauncher);
		inOrder.verify(recoveryMetadataService).markStaleExecutionFailed(1L);
		inOrder.verify(jobLauncher).run(fetchAndPartitionJob, parameters);
	}

	@Test
	void start_whenProcessingRestartNeedsMissingPartitions_failsWithoutRestarting() throws Exception {
		JobExecution staleExecution = staleExecutionWithBaseDir(tempDir.resolve("missing-base"));
		staleExecution.addStepExecutions(
				List.of(
						stepExecution(
								BatchConstants.Job.FETCH_AND_PARTITION_FILES_STEP_NAME, staleExecution,
								BatchStatus.COMPLETED
						), stepExecution(BatchConstants.Job.MASTER_STEP_NAME, staleExecution, BatchStatus.STARTED)
				)
		);
		JobExecution failedExecution = staleExecutionWithBaseDir(tempDir.resolve("missing-base"));
		failedExecution.setStatus(BatchStatus.FAILED);

		when(jobExplorer.findRunningJobExecutions("VdypFetchAndPartitionJob")).thenReturn(Set.of(staleExecution));
		when(recoveryMetadataService.markStaleExecutionFailed(eq(1L), any())).thenReturn(failedExecution);

		service.start();

		assertTrue(service.isRunning());
		verify(recoveryMetadataService).markStaleExecutionFailed(
				1L, "Marked FAILED during startup recovery because partition input directories are missing"
		);
		verify(vdypClient).markComplete(eq("projection-guid"), eq(false), any(VDYPProjectionProgressUpdate.class));
		verify(jobLauncher, never()).run(eq(fetchAndPartitionJob), any());
	}

	@Test
	void start_whenAggregationRestartHasMissingPartitions_stillRestarts() throws Exception {
		JobExecution staleExecution = staleExecutionWithBaseDir(tempDir.resolve("missing-base"));
		staleExecution.addStepExecutions(
				List.of(
						stepExecution(
								BatchConstants.Job.FETCH_AND_PARTITION_FILES_STEP_NAME, staleExecution,
								BatchStatus.COMPLETED
						), stepExecution(BatchConstants.Job.MASTER_STEP_NAME, staleExecution, BatchStatus.COMPLETED),
						stepExecution(BatchConstants.Job.POST_PROCESSING_STEP_NAME, staleExecution, BatchStatus.STARTED)
				)
		);
		JobExecution restartedExecution = new JobExecution(2L, staleExecution.getJobParameters());

		when(jobExplorer.findRunningJobExecutions("VdypFetchAndPartitionJob")).thenReturn(Set.of(staleExecution));
		when(jobLauncher.run(fetchAndPartitionJob, staleExecution.getJobParameters())).thenReturn(restartedExecution);

		service.start();

		assertTrue(service.isRunning());
		verify(recoveryMetadataService).markStaleExecutionFailed(1L);
		verify(jobLauncher).run(fetchAndPartitionJob, staleExecution.getJobParameters());
	}

	@Test
	void stop_marksServiceNotRunning() {
		service.stop();

		assertFalse(service.isRunning());
	}

	private JobExecution staleExecutionWithBaseDir(Path baseDir) {
		JobParameters parameters = new JobParametersBuilder()
				.addString(BatchConstants.Job.GUID, "7c26643a-50cb-497e-a539-afac6966ecea")
				.addString(BatchConstants.GuidInput.PROJECTION_GUID, "projection-guid")
				.addString(BatchConstants.Job.BASE_DIR, baseDir.toString()).addLong(BatchConstants.Partition.NUMBER, 2L)
				.toJobParameters();
		JobExecution execution = new JobExecution(1L, parameters);
		execution.setStatus(BatchStatus.STARTED);
		ExecutionContext executionContext = execution.getExecutionContext();
		executionContext.putInt(BatchConstants.Job.COMPUTED_PARTITIONS, 2);
		return execution;
	}

	private StepExecution stepExecution(String stepName, JobExecution jobExecution, BatchStatus status) {
		StepExecution stepExecution = new StepExecution(stepName, jobExecution);
		stepExecution.setStatus(status);
		return stepExecution;
	}
}
