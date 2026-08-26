package ca.bc.gov.nrs.vdyp.batch.service;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;

import ca.bc.gov.nrs.vdyp.batch.client.vdyp.VdypClient;
import ca.bc.gov.nrs.vdyp.batch.configuration.BatchOwnershipProperties;
import ca.bc.gov.nrs.vdyp.batch.ownership.JobOwnershipService;
import ca.bc.gov.nrs.vdyp.batch.persistence.model.JobClaim;
import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;

@ExtendWith(MockitoExtension.class)
class StartupRecoveryServiceTest {

	@Mock
	JobExplorer jobExplorer;
	@Mock
	Job fetchAndPartitionJob;
	@Mock
	BatchRecoveryMetadataService recoveryMetadataService;
	@Mock
	VdypClient vdypClient;
	@Mock
	BatchOwnershipProperties ownershipProperties;
	@Mock
	JobOwnershipService ownershipService;
	@Mock
	ServerCapacityService serverCapacityService;
	@Mock
	ClaimBoundJobLauncher claimBoundJobLauncher;

	@TempDir
	Path tempDir;

	StartupRecoveryService service;

	@BeforeEach
	void setUp() {
		service = new StartupRecoveryService(
				jobExplorer, fetchAndPartitionJob, recoveryMetadataService, vdypClient, ownershipProperties,
				ownershipService, serverCapacityService, claimBoundJobLauncher
		);
	}

	@AfterEach
	void stopService() {
		service.stop();
	}

	@Test
	void lifecycleStartsOnceAndStopsRecoveryThread() throws Exception {
		CountDownLatch scanned = new CountDownLatch(1);
		lenient().when(ownershipProperties.getRecoveryScanInterval()).thenReturn(Duration.ofHours(1));
		doAnswer(invocation -> {
			scanned.countDown();
			return Set.of();
		}).when(jobExplorer).findRunningJobExecutions("VdypFetchAndPartitionJob");

		assertFalse(service.isRunning());
		assertTrue(service.isAutoStartup());
		assertEquals(Integer.MIN_VALUE, service.getPhase());
		service.start();
		service.start();

		assertTrue(service.isRunning());
		assertTrue(scanned.await(2, TimeUnit.SECONDS));
		service.stop();
		assertFalse(service.isRunning());
	}

	@Test
	void recoveryLoopRetriesRuntimeFailureAndHandlesInterruptedRetrySleep() throws Exception {
		CountDownLatch retrySleepStarted = new CountDownLatch(1);
		AtomicInteger intervalRequests = new AtomicInteger();
		when(jobExplorer.findRunningJobExecutions("VdypFetchAndPartitionJob")).thenReturn(Set.of());
		doAnswer(invocation -> {
			int request = intervalRequests.getAndIncrement();
			if (request == 0) {
				return null;
			}
			if (request < 3) {
				return Duration.ZERO;
			}
			retrySleepStarted.countDown();
			return Duration.ofHours(1);
		}).when(ownershipProperties).getRecoveryScanInterval();

		service.start();
		assertTrue(retrySleepStarted.await(2, TimeUnit.SECONDS));
		service.stop();
		await().atMost(20, TimeUnit.MILLISECONDS).until(() -> !service.isRunning());

		assertFalse(service.isRunning());
	}

	@Test
	void stopBeforeStartIsSafe() {
		service.stop();
		assertFalse(service.isRunning());
	}

	@Test
	void scan_whenNoRunningExecutions_doesNotLaunch() throws Exception {
		when(jobExplorer.findRunningJobExecutions("VdypFetchAndPartitionJob")).thenReturn(Set.of());

		service.recoverClaimableExecutions();

		verify(claimBoundJobLauncher, never()).launch(any(), any(), any());
	}

	@Test
	void scan_whenExplorerFails_handlesFailure() throws Exception {
		when(jobExplorer.findRunningJobExecutions("VdypFetchAndPartitionJob"))
				.thenThrow(new IllegalStateException("repository unavailable"));

		service.recoverClaimableExecutions();

		verify(claimBoundJobLauncher, never()).launch(any(), any(), any());
	}

	@Test
	void scan_whenRunningExecutionHasNoClaim_skipsByDefault() throws Exception {
		String projectionGuid = UUID.randomUUID().toString();
		JobExecution execution = runningExecution(projectionGuid);
		when(jobExplorer.findRunningJobExecutions("VdypFetchAndPartitionJob")).thenReturn(Set.of(execution));
		when(ownershipService.findProjectionClaim(projectionGuid)).thenReturn(Optional.empty());
		when(ownershipProperties.isRecoverLegacyExecutionsWithoutClaim()).thenReturn(false);

		service.recoverClaimableExecutions();

		verify(recoveryMetadataService, never()).markStaleExecutionFailed(any());
		verify(claimBoundJobLauncher, never()).launch(any(), any(), any());
	}

	@Test
	void scan_whenClaimIsStillLive_leavesExecutionUntouched() throws Exception {
		String projectionGuid = UUID.randomUUID().toString();
		JobExecution execution = runningExecution(projectionGuid);
		JobClaim claim = claim(projectionGuid, Instant.now(), Instant.now().plus(Duration.ofMinutes(1)));
		when(jobExplorer.findRunningJobExecutions("VdypFetchAndPartitionJob")).thenReturn(Set.of(execution));
		when(ownershipService.findProjectionClaim(projectionGuid)).thenReturn(Optional.of(claim));

		service.recoverClaimableExecutions();

		verify(recoveryMetadataService, never()).markStaleExecutionFailed(any());
		verify(claimBoundJobLauncher, never()).launch(any(), any(), any());
	}

	@Test
	void prioritizedRecoverySkipsLocallyOwnedExecution() {
		String projectionGuid = UUID.randomUUID().toString();
		JobExecution execution = runningExecution(projectionGuid);
		when(jobExplorer.findRunningJobExecutions("VdypFetchAndPartitionJob")).thenReturn(Set.of(execution));
		when(ownershipService.isOwnedLocally(projectionGuid)).thenReturn(true);

		assertFalse(service.recoverNextExpiredExecution());
		verify(ownershipService, never()).findProjectionClaim(any());
	}

	@Test
	void prioritizedRecoverySkipsWhenCapacityIsUnavailable() {
		String projectionGuid = UUID.randomUUID().toString();
		JobExecution execution = runningExecution(projectionGuid);
		JobClaim oldClaim = expiredClaim(projectionGuid);
		when(jobExplorer.findRunningJobExecutions("VdypFetchAndPartitionJob")).thenReturn(Set.of(execution));
		when(ownershipService.findProjectionClaim(projectionGuid)).thenReturn(Optional.of(oldClaim));
		when(serverCapacityService.hasAvailableCapacity()).thenReturn(false);

		assertFalse(service.recoverNextExpiredExecution());
		verify(ownershipService, never()).tryAcquire(any(), any());
	}

	@Test
	void prioritizedRecoverySkipsWhenClaimCannotBeAcquired() {
		String projectionGuid = UUID.randomUUID().toString();
		JobExecution execution = runningExecution(projectionGuid);
		when(jobExplorer.findRunningJobExecutions("VdypFetchAndPartitionJob")).thenReturn(Set.of(execution));
		when(ownershipService.findProjectionClaim(projectionGuid))
				.thenReturn(Optional.of(expiredClaim(projectionGuid)));
		when(serverCapacityService.hasAvailableCapacity()).thenReturn(true);
		when(ownershipService.tryAcquire(projectionGuid, "recovery")).thenReturn(Optional.empty());

		assertFalse(service.recoverNextExpiredExecution());
		verify(recoveryMetadataService, never()).markStaleExecutionFailed(any());
	}

	@Test
	void prioritizedRecoveryReturnsTrueAfterRestart() throws Exception {
		String projectionGuid = UUID.randomUUID().toString();
		JobExecution execution = runningExecution(projectionGuid);
		JobClaim newClaim = claim(projectionGuid, Instant.now(), Instant.now().plus(Duration.ofMinutes(2)));
		when(jobExplorer.findRunningJobExecutions("VdypFetchAndPartitionJob")).thenReturn(Set.of(execution));
		when(ownershipService.findProjectionClaim(projectionGuid))
				.thenReturn(Optional.of(expiredClaim(projectionGuid)));
		when(serverCapacityService.hasAvailableCapacity()).thenReturn(true);
		when(ownershipService.tryAcquire(projectionGuid, "recovery")).thenReturn(Optional.of(newClaim));
		when(claimBoundJobLauncher.launch(fetchAndPartitionJob, execution.getJobParameters(), newClaim))
				.thenReturn(runningExecution(projectionGuid));

		assertTrue(service.recoverNextExpiredExecution());
	}

	@Test
	void prioritizedRecoveryHandlesMalformedExecution() {
		JobExecution execution = executionWithoutProjectionGuid();
		when(jobExplorer.findRunningJobExecutions("VdypFetchAndPartitionJob")).thenReturn(Set.of(execution));

		assertFalse(service.recoverNextExpiredExecution());
	}

	@Test
	void prioritizedRecoveryHandlesBlankProjectionGuid() {
		JobExecution execution = executionWithProjectionGuid(" ");
		when(jobExplorer.findRunningJobExecutions("VdypFetchAndPartitionJob")).thenReturn(Set.of(execution));

		assertFalse(service.recoverNextExpiredExecution());
	}

	@Test
	void legacyExecutionCanBeRecoveredWhenExplicitlyEnabled() throws Exception {
		String projectionGuid = UUID.randomUUID().toString();
		JobExecution execution = runningExecution(projectionGuid);
		JobClaim newClaim = claim(projectionGuid, Instant.now(), Instant.now().plus(Duration.ofMinutes(2)));
		when(jobExplorer.findRunningJobExecutions("VdypFetchAndPartitionJob")).thenReturn(Set.of(execution));
		when(ownershipService.findProjectionClaim(projectionGuid)).thenReturn(Optional.empty());
		when(ownershipProperties.isRecoverLegacyExecutionsWithoutClaim()).thenReturn(true);
		when(serverCapacityService.hasAvailableCapacity()).thenReturn(true);
		when(ownershipService.tryAcquire(projectionGuid, "recovery")).thenReturn(Optional.of(newClaim));
		when(claimBoundJobLauncher.launch(fetchAndPartitionJob, execution.getJobParameters(), newClaim))
				.thenReturn(runningExecution(projectionGuid));

		assertTrue(service.recoverNextExpiredExecution());
	}

	@Test
	void scan_whenExpiredClaimIsAcquired_marksFailedRestartsAndBindsNewExecution() throws Exception {
		String projectionGuid = UUID.randomUUID().toString();
		JobExecution execution = runningExecution(projectionGuid);
		JobExecution restarted = runningExecution(projectionGuid);
		JobClaim oldClaim = claim(
				projectionGuid, Instant.now().minus(Duration.ofMinutes(5)), Instant.now().minus(Duration.ofMinutes(5))
		);
		JobClaim newClaim = claim(projectionGuid, Instant.now(), Instant.now().plus(Duration.ofMinutes(2)));
		when(jobExplorer.findRunningJobExecutions("VdypFetchAndPartitionJob")).thenReturn(Set.of(execution));
		when(ownershipService.findProjectionClaim(projectionGuid)).thenReturn(Optional.of(oldClaim));
		when(serverCapacityService.hasAvailableCapacity()).thenReturn(true);
		when(ownershipService.tryAcquire(projectionGuid, "recovery")).thenReturn(Optional.of(newClaim));
		when(claimBoundJobLauncher.launch(fetchAndPartitionJob, execution.getJobParameters(), newClaim))
				.thenReturn(restarted);

		service.recoverClaimableExecutions();

		verify(recoveryMetadataService).markStaleExecutionFailed(execution.getId());
		verify(claimBoundJobLauncher).launch(fetchAndPartitionJob, execution.getJobParameters(), newClaim);
	}

	@Test
	void completedFetchStepWithoutBaseDirectoryIsFailedAndBackendIsNotified() throws Exception {
		String projectionGuid = UUID.randomUUID().toString();
		JobExecution execution = completedFetchExecution(projectionGuid, " ", 2L, false);
		JobClaim newClaim = claim(projectionGuid, Instant.now(), Instant.now().plus(Duration.ofMinutes(2)));
		when(jobExplorer.findRunningJobExecutions("VdypFetchAndPartitionJob")).thenReturn(Set.of(execution));
		when(ownershipService.findProjectionClaim(projectionGuid))
				.thenReturn(Optional.of(expiredClaim(projectionGuid)));
		when(serverCapacityService.hasAvailableCapacity()).thenReturn(true);
		when(ownershipService.tryAcquire(projectionGuid, "recovery")).thenReturn(Optional.of(newClaim));
		when(recoveryMetadataService.markStaleExecutionFailed(eq(execution.getId()), any())).thenReturn(execution);

		assertFalse(service.recoverNextExpiredExecution());

		verify(vdypClient).markComplete(eq(projectionGuid), eq(false), any());
		verify(ownershipService).releaseUnboundClaim(newClaim);
		verify(claimBoundJobLauncher, never()).launch(any(), any(), any());
	}

	@Test
	void missingPartitionDirectoryPreventsRestartUsingComputedPartitionCount() throws Exception {
		String projectionGuid = UUID.randomUUID().toString();
		JobExecution execution = completedFetchExecution(projectionGuid, tempDir.toString(), 1L, false);
		execution.getExecutionContext().putInt(BatchConstants.Job.COMPUTED_PARTITIONS, 2);
		Files.createDirectories(
				tempDir.resolve(BatchConstants.Partition.INPUT_PREFIX + "-" + BatchConstants.Partition.PREFIX + "0")
		);
		JobClaim newClaim = claim(projectionGuid, Instant.now(), Instant.now().plus(Duration.ofMinutes(2)));
		when(jobExplorer.findRunningJobExecutions("VdypFetchAndPartitionJob")).thenReturn(Set.of(execution));
		when(ownershipService.findProjectionClaim(projectionGuid))
				.thenReturn(Optional.of(expiredClaim(projectionGuid)));
		when(serverCapacityService.hasAvailableCapacity()).thenReturn(true);
		when(ownershipService.tryAcquire(projectionGuid, "recovery")).thenReturn(Optional.of(newClaim));
		when(recoveryMetadataService.markStaleExecutionFailed(eq(execution.getId()), any()))
				.thenReturn(executionWithoutProjectionGuid());

		assertFalse(service.recoverNextExpiredExecution());

		verify(vdypClient, never()).markComplete(any(), eq(false), any());
		verify(ownershipService).releaseUnboundClaim(newClaim);
	}

	@Test
	void completedFetchStepWithoutBaseDirectoryHandlesBlankFailureProjection() {
		String projectionGuid = UUID.randomUUID().toString();
		JobExecution execution = completedFetchExecution(projectionGuid, null, 1L, false);
		JobClaim newClaim = claim(projectionGuid, Instant.now(), Instant.now().plus(Duration.ofMinutes(2)));
		when(jobExplorer.findRunningJobExecutions("VdypFetchAndPartitionJob")).thenReturn(Set.of(execution));
		when(ownershipService.findProjectionClaim(projectionGuid))
				.thenReturn(Optional.of(expiredClaim(projectionGuid)));
		when(serverCapacityService.hasAvailableCapacity()).thenReturn(true);
		when(ownershipService.tryAcquire(projectionGuid, "recovery")).thenReturn(Optional.of(newClaim));
		when(recoveryMetadataService.markStaleExecutionFailed(eq(execution.getId()), any()))
				.thenReturn(executionWithProjectionGuid(" "));

		assertFalse(service.recoverNextExpiredExecution());

		verify(vdypClient, never()).markComplete(any(), eq(false), any());
		verify(ownershipService).releaseUnboundClaim(newClaim);
	}

	@Test
	void completedFetchStepRestartsWhenAllPartitionDirectoriesExist() throws Exception {
		String projectionGuid = UUID.randomUUID().toString();
		JobExecution execution = completedFetchExecution(projectionGuid, tempDir.toString(), 2L, false);
		for (int i = 0; i < 2; i++) {
			Files.createDirectories(
					tempDir.resolve(BatchConstants.Partition.INPUT_PREFIX + "-" + BatchConstants.Partition.PREFIX + i)
			);
		}
		stubSuccessfulRecovery(execution, projectionGuid);

		assertTrue(service.recoverNextExpiredExecution());
		verify(claimBoundJobLauncher).launch(eq(fetchAndPartitionJob), eq(execution.getJobParameters()), any());
	}

	@Test
	void completedMasterStepCanRestartWithoutPartitionDirectories() throws Exception {
		String projectionGuid = UUID.randomUUID().toString();
		JobExecution execution = completedFetchExecution(projectionGuid, tempDir.toString(), 2L, true);
		stubSuccessfulRecovery(execution, projectionGuid);

		assertTrue(service.recoverNextExpiredExecution());
	}

	@Test
	void absentPartitionCountTreatsCompletedFetchAsHavingNoExpectedDirectories() throws Exception {
		String projectionGuid = UUID.randomUUID().toString();
		JobExecution execution = completedFetchExecution(projectionGuid, tempDir.toString(), null, false);
		stubSuccessfulRecovery(execution, projectionGuid);

		assertTrue(service.recoverNextExpiredExecution());
	}

	@Test
	void backendNotificationFailureDoesNotPreventClaimRelease() {
		String projectionGuid = UUID.randomUUID().toString();
		JobExecution execution = completedFetchExecution(projectionGuid, "", 1L, false);
		JobClaim newClaim = claim(projectionGuid, Instant.now(), Instant.now().plus(Duration.ofMinutes(2)));
		when(jobExplorer.findRunningJobExecutions("VdypFetchAndPartitionJob")).thenReturn(Set.of(execution));
		when(ownershipService.findProjectionClaim(projectionGuid))
				.thenReturn(Optional.of(expiredClaim(projectionGuid)));
		when(serverCapacityService.hasAvailableCapacity()).thenReturn(true);
		when(ownershipService.tryAcquire(projectionGuid, "recovery")).thenReturn(Optional.of(newClaim));
		when(recoveryMetadataService.markStaleExecutionFailed(eq(execution.getId()), any())).thenReturn(execution);
		doThrow(new IllegalStateException("backend unavailable")).when(vdypClient)
				.markComplete(eq(projectionGuid), eq(false), any());

		assertFalse(service.recoverNextExpiredExecution());
		verify(ownershipService).releaseUnboundClaim(newClaim);
	}

	private JobExecution runningExecution(String projectionGuid) {
		JobParameters parameters = new JobParametersBuilder()
				.addString(BatchConstants.Job.GUID, UUID.randomUUID().toString())
				.addString(BatchConstants.GuidInput.PROJECTION_GUID, projectionGuid)
				.addString(BatchConstants.Job.BASE_DIR, tempDir.toString()).addLong(BatchConstants.Partition.NUMBER, 2L)
				.toJobParameters();
		JobInstance jobInstance = new JobInstance(10L, "VdypFetchAndPartitionJob");
		JobExecution execution = new JobExecution(jobInstance, 1L, parameters);
		execution.setStatus(BatchStatus.STARTED);
		return execution;
	}

	private JobExecution completedFetchExecution(
			String projectionGuid, String baseDirectory, Long partitionCount, boolean masterCompleted
	) {
		JobParametersBuilder parameters = new JobParametersBuilder()
				.addString(BatchConstants.Job.GUID, UUID.randomUUID().toString())
				.addString(BatchConstants.GuidInput.PROJECTION_GUID, projectionGuid);
		if (baseDirectory != null) {
			parameters.addString(BatchConstants.Job.BASE_DIR, baseDirectory);
		}
		if (partitionCount != null) {
			parameters.addLong(BatchConstants.Partition.NUMBER, partitionCount);
		}
		JobExecution execution = new JobExecution(
				new JobInstance(20L, "VdypFetchAndPartitionJob"), 21L, parameters.toJobParameters()
		);
		execution.setStatus(BatchStatus.STARTED);
		StepExecution fetch = new StepExecution(BatchConstants.Job.FETCH_AND_PARTITION_FILES_STEP_NAME, execution);
		fetch.setStatus(BatchStatus.COMPLETED);
		execution.addStepExecutions(List.of(fetch));
		if (masterCompleted) {
			StepExecution master = new StepExecution(BatchConstants.Job.MASTER_STEP_NAME, execution);
			master.setStatus(BatchStatus.COMPLETED);
			execution.addStepExecutions(List.of(master));
		}
		return execution;
	}

	private JobExecution executionWithoutProjectionGuid() {
		return executionWithProjectionGuid(null);
	}

	private JobExecution executionWithProjectionGuid(String projectionGuid) {
		JobParametersBuilder parameters = new JobParametersBuilder()
				.addString(BatchConstants.Job.GUID, UUID.randomUUID().toString());
		if (projectionGuid != null) {
			parameters.addString(BatchConstants.GuidInput.PROJECTION_GUID, projectionGuid);
		}
		JobParameters jobParameters = parameters.toJobParameters();
		return new JobExecution(new JobInstance(30L, "VdypFetchAndPartitionJob"), 31L, jobParameters);
	}

	private void stubSuccessfulRecovery(JobExecution execution, String projectionGuid) throws Exception {
		JobClaim newClaim = claim(projectionGuid, Instant.now(), Instant.now().plus(Duration.ofMinutes(2)));
		when(jobExplorer.findRunningJobExecutions("VdypFetchAndPartitionJob")).thenReturn(Set.of(execution));
		when(ownershipService.findProjectionClaim(projectionGuid))
				.thenReturn(Optional.of(expiredClaim(projectionGuid)));
		when(serverCapacityService.hasAvailableCapacity()).thenReturn(true);
		when(ownershipService.tryAcquire(projectionGuid, "recovery")).thenReturn(Optional.of(newClaim));
		when(claimBoundJobLauncher.launch(fetchAndPartitionJob, execution.getJobParameters(), newClaim))
				.thenReturn(runningExecution(projectionGuid));
	}

	private JobClaim expiredClaim(String projectionGuid) {
		return claim(
				projectionGuid, Instant.now().minus(Duration.ofMinutes(5)), Instant.now().minus(Duration.ofMinutes(1))
		);
	}

	private JobClaim claim(String projectionGuid, Instant acquiredTime, Instant leaseExpiryTime) {
		return new JobClaim(projectionGuid, "owner", UUID.randomUUID(), acquiredTime, leaseExpiryTime);
	}
}
