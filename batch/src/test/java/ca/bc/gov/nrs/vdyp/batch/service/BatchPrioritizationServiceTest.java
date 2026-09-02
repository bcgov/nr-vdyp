package ca.bc.gov.nrs.vdyp.batch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobExecutionNotRunningException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.core.task.TaskExecutor;

import ca.bc.gov.nrs.vdyp.batch.configuration.BatchProperties;
import ca.bc.gov.nrs.vdyp.batch.ownership.JobOwnershipService;
import ca.bc.gov.nrs.vdyp.batch.persistence.model.JobClaim;
import ca.bc.gov.nrs.vdyp.batch.service.BatchPrioritizationService.PrioritizeOutcome;
import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BatchPrioritizationServiceTest {

	@Mock
	private JobExplorer jobExplorer;

	@Mock
	private JobOperator jobOperator;

	@Mock
	private JobOwnershipService ownershipService;

	@Mock
	private ClaimBoundJobLauncher claimBoundJobLauncher;

	@Mock
	private Job fetchAndPartitionJob;

	@Mock
	private TaskExecutor prioritizationExecutor;

	@Mock
	private TaskExecutor resumeRetryExecutor;

	private PrioritizationPauseTracker pauseTracker;
	private BatchProperties batchProperties;
	private BatchPrioritizationService service;

	@BeforeEach
	void setUp() {
		batchProperties = new BatchProperties();
		batchProperties.getPrioritize().setStopWaitTimeoutSeconds(1);
		batchProperties.getPrioritize().setStopPollIntervalMillis(5);
		pauseTracker = new PrioritizationPauseTracker();

		service = new BatchPrioritizationService(
				jobExplorer, jobOperator, batchProperties, ownershipService, claimBoundJobLauncher,
				fetchAndPartitionJob, prioritizationExecutor, resumeRetryExecutor, pauseTracker
		);

		when(ownershipService.isOwnedLocally(any())).thenReturn(true);

		doAnswer(invocation -> {
			((Runnable) invocation.getArgument(0)).run();
			return null;
		}).when(prioritizationExecutor).execute(any());

		doAnswer(invocation -> {
			((Runnable) invocation.getArgument(0)).run();
			return null;
		}).when(resumeRetryExecutor).execute(any());
	}

	@Test
	void testPrioritizeLocally_NoOtherRunningJobs_ReturnsAlreadyPrioritized() {
		JobExecution target = targetExecution();
		when(jobExplorer.findRunningJobExecutions(BatchConstants.Job.JOB_NAME)).thenReturn(Set.of(target));

		PrioritizeOutcome outcome = service.prioritizeLocally("guid", target);

		assertEquals("ALREADY_PRIORITIZED", outcome.status());
		assertEquals(0, outcome.othersPausedCount());
		verify(prioritizationExecutor, never()).execute(any());
	}

	@Test
	void testPrioritizeLocally_IgnoresRunningJobsOwnedByOtherReplicas() throws Exception {
		JobExecution target = targetExecution();
		JobExecution remoteOther = runningExecution(200L, LocalDateTime.now().minusMinutes(10));
		when(jobExplorer.findRunningJobExecutions(BatchConstants.Job.JOB_NAME)).thenReturn(Set.of(target, remoteOther));
		when(
				ownershipService.isOwnedLocally(
						remoteOther.getJobParameters().getString(BatchConstants.GuidInput.PROJECTION_GUID)
				)
		).thenReturn(false);

		PrioritizeOutcome outcome = service.prioritizeLocally("guid", target);

		assertEquals("ALREADY_PRIORITIZED", outcome.status());
		assertEquals(0, outcome.othersPausedCount());
		verify(jobOperator, never()).stop(200L);
	}

	@Test
	void testPrioritizeLocally_PausesOthersInStartTimeOrder_AndResumesWithSameParameters() throws Exception {
		JobExecution target = targetExecution();
		JobExecution firstOther = runningExecution(200L, LocalDateTime.now().minusMinutes(10));
		JobExecution secondOther = runningExecution(300L, LocalDateTime.now().minusMinutes(5));
		when(jobExplorer.findRunningJobExecutions(BatchConstants.Job.JOB_NAME))
				.thenReturn(Set.of(target, firstOther, secondOther));

		when(jobOperator.stop(200L)).thenReturn(true);
		when(jobOperator.stop(300L)).thenReturn(true);
		when(ownershipService.tryAcquire(anyString(), anyString())).thenReturn(Optional.of(claim("resume")));
		when(claimBoundJobLauncher.launch(any(), any(), any())).thenReturn(new JobExecution(999L));

		PrioritizeOutcome outcome = service.prioritizeLocally("guid", target);

		assertEquals("PRIORITIZE_REQUESTED", outcome.status());
		assertEquals(2, outcome.othersPausedCount());

		InOrder order = inOrder(jobOperator, claimBoundJobLauncher);
		order.verify(jobOperator).stop(200L);
		order.verify(claimBoundJobLauncher).launch(eq(fetchAndPartitionJob), eq(firstOther.getJobParameters()), any());
		order.verify(jobOperator).stop(300L);
		order.verify(claimBoundJobLauncher).launch(eq(fetchAndPartitionJob), eq(secondOther.getJobParameters()), any());

		verify(jobOperator, never()).stop(100L);
	}

	@Test
	void testPrioritizeLocally_StopThrowsAlreadyNotRunning_StillResumes() throws Exception {
		JobExecution target = targetExecution();
		JobExecution other = runningExecution(200L, LocalDateTime.now().minusMinutes(1));
		when(jobExplorer.findRunningJobExecutions(BatchConstants.Job.JOB_NAME)).thenReturn(Set.of(target, other));

		when(jobOperator.stop(200L)).thenThrow(new JobExecutionNotRunningException("already stopped"));
		when(ownershipService.tryAcquire(anyString(), anyString())).thenReturn(Optional.of(claim("resume")));
		when(claimBoundJobLauncher.launch(any(), any(), any())).thenReturn(new JobExecution(999L));

		service.prioritizeLocally("guid", target);

		verify(claimBoundJobLauncher).launch(eq(fetchAndPartitionJob), eq(other.getJobParameters()), any());
	}

	@Test
	void testPrioritizeLocally_ResumeAlreadyRunning_RetriesThenSucceeds() throws Exception {
		JobExecution target = targetExecution();
		JobExecution other = runningExecution(200L, LocalDateTime.now().minusMinutes(1));
		when(jobExplorer.findRunningJobExecutions(BatchConstants.Job.JOB_NAME)).thenReturn(Set.of(target, other));

		when(jobOperator.stop(200L)).thenReturn(true);
		when(ownershipService.tryAcquire(anyString(), anyString())).thenReturn(Optional.of(claim("resume")));
		when(claimBoundJobLauncher.launch(any(), any(), any()))
				.thenThrow(new JobExecutionAlreadyRunningException("still stopping"))
				.thenReturn(new JobExecution(999L));

		service.prioritizeLocally("guid", target);

		verify(claimBoundJobLauncher, times(2)).launch(eq(fetchAndPartitionJob), eq(other.getJobParameters()), any());
	}

	@Test
	void testPrioritizeLocally_ResumeAlreadyRunning_GivesUpAfterTimeout() throws Exception {
		JobExecution target = targetExecution();
		JobExecution other = runningExecution(200L, LocalDateTime.now().minusMinutes(1));
		when(jobExplorer.findRunningJobExecutions(BatchConstants.Job.JOB_NAME)).thenReturn(Set.of(target, other));

		when(jobOperator.stop(200L)).thenReturn(true);
		when(ownershipService.tryAcquire(anyString(), anyString())).thenReturn(Optional.of(claim("resume")));
		when(claimBoundJobLauncher.launch(any(), any(), any()))
				.thenThrow(new JobExecutionAlreadyRunningException("still stopping"));

		service.prioritizeLocally("guid", target);

		verify(claimBoundJobLauncher, atLeast(2)).launch(eq(fetchAndPartitionJob), eq(other.getJobParameters()), any());
	}

	@Test
	void testPrioritizeLocally_OneStopFails_OthersStillProcessed() throws Exception {
		JobExecution target = targetExecution();
		JobExecution firstOther = runningExecution(200L, LocalDateTime.now().minusMinutes(10));
		JobExecution secondOther = runningExecution(300L, LocalDateTime.now().minusMinutes(5));
		when(jobExplorer.findRunningJobExecutions(BatchConstants.Job.JOB_NAME))
				.thenReturn(Set.of(target, firstOther, secondOther));

		when(jobOperator.stop(200L)).thenThrow(new RuntimeException("boom"));
		when(jobOperator.stop(300L)).thenReturn(true);
		when(ownershipService.tryAcquire(anyString(), anyString())).thenReturn(Optional.of(claim("resume")));
		when(claimBoundJobLauncher.launch(any(), any(), any())).thenReturn(new JobExecution(999L));

		service.prioritizeLocally("guid", target);

		verify(claimBoundJobLauncher, never())
				.launch(eq(fetchAndPartitionJob), eq(firstOther.getJobParameters()), any());
		verify(claimBoundJobLauncher).launch(eq(fetchAndPartitionJob), eq(secondOther.getJobParameters()), any());
	}

	@Test
	void testPrioritizeLocally_ClaimUnavailable_RetriesUntilAcquired() throws Exception {
		JobExecution target = targetExecution();
		JobExecution other = runningExecution(200L, LocalDateTime.now().minusMinutes(1));
		when(jobExplorer.findRunningJobExecutions(BatchConstants.Job.JOB_NAME)).thenReturn(Set.of(target, other));

		when(jobOperator.stop(200L)).thenReturn(true);
		when(ownershipService.tryAcquire(anyString(), anyString())).thenReturn(Optional.empty())
				.thenReturn(Optional.of(claim("resume")));
		when(claimBoundJobLauncher.launch(any(), any(), any())).thenReturn(new JobExecution(999L));

		service.prioritizeLocally("guid", target);

		verify(claimBoundJobLauncher).launch(eq(fetchAndPartitionJob), eq(other.getJobParameters()), any());
	}

	private JobExecution targetExecution() {
		JobInstance instance = new JobInstance(1L, "testJob");
		JobParameters params = new JobParametersBuilder().addString(BatchConstants.Job.GUID, "guid").toJobParameters();
		JobExecution execution = new JobExecution(instance, 100L, params);
		execution.setStatus(BatchStatus.STARTED);
		return execution;
	}

	private JobExecution runningExecution(long executionId, LocalDateTime startTime) {
		JobInstance instance = new JobInstance(executionId, "testJob");
		JobParameters params = new JobParametersBuilder()
				.addString(BatchConstants.Job.GUID, UUID.randomUUID().toString())
				.addString(BatchConstants.GuidInput.PROJECTION_GUID, UUID.randomUUID().toString()).toJobParameters();
		JobExecution execution = new JobExecution(instance, executionId, params);
		execution.setStatus(BatchStatus.STARTED);
		execution.setStartTime(startTime);
		return execution;
	}

	private JobClaim claim(String reason) {
		Instant now = Instant.now();
		return new JobClaim(UUID.randomUUID().toString(), reason, UUID.randomUUID(), now, now.plusSeconds(120));
	}
}
