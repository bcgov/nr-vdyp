package ca.bc.gov.nrs.vdyp.batch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersValidator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.core.task.TaskExecutor;

import ca.bc.gov.nrs.vdyp.batch.ownership.JobOwnershipService;
import ca.bc.gov.nrs.vdyp.batch.persistence.model.JobClaim;
import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;

@ExtendWith(MockitoExtension.class)
class ClaimBoundJobLauncherTest {

	@Mock
	JobRepository jobRepository;
	@Mock
	TaskExecutor taskExecutor;
	@Mock
	JobOwnershipService ownershipService;
	@Mock
	ThreadReservationService threadReservationService;
	@Mock
	Job job;
	@Mock
	JobParametersValidator jobParametersValidator;

	@Test
	void launch_registersClaimBeforeSubmittingExecution() throws Exception {
		JobParameters parameters = new JobParametersBuilder().addString("projectionGUID", UUID.randomUUID().toString())
				.toJobParameters();
		JobExecution execution = new JobExecution(new JobInstance(1L, "VdypFetchAndPartitionJob"), 2L, parameters);
		JobClaim claim = new JobClaim(
				UUID.randomUUID().toString(), "owner", UUID.randomUUID(), Instant.now(), Instant.now()
		);
		ArgumentCaptor<Runnable> submittedTask = ArgumentCaptor.forClass(Runnable.class);

		when(job.getName()).thenReturn("VdypFetchAndPartitionJob");
		when(job.getJobParametersValidator()).thenReturn(jobParametersValidator);
		when(jobRepository.createJobExecution(job.getName(), parameters)).thenReturn(execution);
		new ClaimBoundJobLauncher(jobRepository, taskExecutor, ownershipService, threadReservationService)
				.launch(job, parameters, claim, 5);

		InOrder inOrder = inOrder(jobRepository, ownershipService, taskExecutor);
		inOrder.verify(jobRepository).createJobExecution(job.getName(), parameters);
		inOrder.verify(ownershipService).registerClaim(claim);
		inOrder.verify(taskExecutor).execute(submittedTask.capture());
		verify(job, never()).execute(execution);
		assertEquals(5, execution.getExecutionContext().getInt(BatchConstants.Job.RESERVED_THREADS));
		verifyNoInteractions(threadReservationService);

		submittedTask.getValue().run();

		verify(job).execute(execution);
	}

	@Test
	void launch_releasesReservedThreadsWhenSubmissionFails() throws Exception {
		JobParameters parameters = new JobParametersBuilder().addString("projectionGUID", UUID.randomUUID().toString())
				.toJobParameters();
		JobExecution execution = new JobExecution(new JobInstance(1L, "VdypFetchAndPartitionJob"), 2L, parameters);
		JobClaim claim = new JobClaim(
				UUID.randomUUID().toString(), "owner", UUID.randomUUID(), Instant.now(), Instant.now()
		);

		when(job.getName()).thenReturn("VdypFetchAndPartitionJob");
		when(job.getJobParametersValidator()).thenReturn(jobParametersValidator);
		when(jobRepository.createJobExecution(job.getName(), parameters)).thenReturn(execution);
		doThrow(new IllegalStateException("pool saturated")).when(taskExecutor).execute(any());

		ClaimBoundJobLauncher launcher = new ClaimBoundJobLauncher(
				jobRepository, taskExecutor, ownershipService, threadReservationService
		);

		assertThrows(IllegalStateException.class, () -> launcher.launch(job, parameters, claim, 5));

		verify(threadReservationService).release(5);
	}

	@Test
	void launch_releasesReservedThreadsWhenExecutionCreationFails() throws Exception {
		JobParameters parameters = new JobParametersBuilder().addString("projectionGUID", UUID.randomUUID().toString())
				.toJobParameters();
		JobClaim claim = new JobClaim(
				UUID.randomUUID().toString(), "owner", UUID.randomUUID(), Instant.now(), Instant.now()
		);

		when(job.getName()).thenReturn("VdypFetchAndPartitionJob");
		when(job.getJobParametersValidator()).thenReturn(jobParametersValidator);
		when(jobRepository.createJobExecution(job.getName(), parameters))
				.thenThrow(new org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException("done"));

		ClaimBoundJobLauncher launcher = new ClaimBoundJobLauncher(
				jobRepository, taskExecutor, ownershipService, threadReservationService
		);

		assertThrows(
				org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException.class,
				() -> launcher.launch(job, parameters, claim, 5)
		);

		verify(ownershipService).releaseUnboundClaim(claim);
		verify(threadReservationService).release(5);
	}
}
