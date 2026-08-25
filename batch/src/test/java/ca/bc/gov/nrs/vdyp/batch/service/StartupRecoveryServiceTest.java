package ca.bc.gov.nrs.vdyp.batch.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

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
import org.springframework.batch.core.explore.JobExplorer;

import ca.bc.gov.nrs.vdyp.batch.client.vdyp.VdypClient;
import ca.bc.gov.nrs.vdyp.batch.configuration.BatchOwnershipProperties;
import ca.bc.gov.nrs.vdyp.batch.ownership.JobClaim;
import ca.bc.gov.nrs.vdyp.batch.ownership.JobOwnershipService;
import ca.bc.gov.nrs.vdyp.batch.ownership.ServerCapacityService;
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

	@Test
	void scan_whenNoRunningExecutions_doesNotLaunch() throws Exception {
		when(jobExplorer.findRunningJobExecutions("VdypFetchAndPartitionJob")).thenReturn(Set.of());

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

	private JobClaim claim(String projectionGuid, Instant acquiredTime, Instant leaseExpiryTime) {
		return new JobClaim(projectionGuid, "owner", UUID.randomUUID(), acquiredTime, leaseExpiryTime);
	}
}
