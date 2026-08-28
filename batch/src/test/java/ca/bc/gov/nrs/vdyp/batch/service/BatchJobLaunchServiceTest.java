package ca.bc.gov.nrs.vdyp.batch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;

import ca.bc.gov.nrs.vdyp.batch.configuration.BatchProperties;
import ca.bc.gov.nrs.vdyp.batch.ownership.JobOwnershipService;
import ca.bc.gov.nrs.vdyp.batch.persistence.model.JobClaim;
import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;

@ExtendWith(MockitoExtension.class)
class BatchJobLaunchServiceTest {

	@Mock
	Job vdypBatchJob;
	@Mock
	ServerCapacityService serverCapacityService;
	@Mock
	JobOwnershipService ownershipService;
	@Mock
	JobExplorer jobExplorer;
	@Mock
	ClaimBoundJobLauncher claimBoundJobLauncher;

	@TempDir
	Path tempDir;

	BatchProperties batchProperties;
	BatchJobLaunchService service;

	@BeforeEach
	void setUp() {
		batchProperties = new BatchProperties();
		batchProperties.setRootDirectory(tempDir.toString());
		batchProperties.getPartition().setDefaultNumberOfPartitions(3);
		batchProperties.getPartition().setJobSearchChunkSize(2);
		batchProperties.getReader().setDefaultChunkSize(25);
		service = new BatchJobLaunchService(
				vdypBatchJob, batchProperties, serverCapacityService, ownershipService, jobExplorer,
				claimBoundJobLauncher
		);
	}

	@Test
	void hasCapacityRequiresBothThreadsAndOwnershipIntake() {
		when(serverCapacityService.hasAvailableCapacity()).thenReturn(false, true, true);
		when(ownershipService.isAcceptingNewWork()).thenReturn(false, true);

		assertFalse(service.hasCapacity());
		assertFalse(service.hasCapacity());
		assertTrue(service.hasCapacity());
	}

	@Test
	void launchRejectsWorkWithoutThreadCapacity() {
		when(serverCapacityService.hasAvailableCapacity()).thenReturn(false);

		assertThrows(IllegalStateException.class, () -> service.launch(UUID.randomUUID(), "{}"));
		verifyNoInteractions(jobExplorer);
		verify(ownershipService, never()).tryAcquire(any(), any());
	}

	@Test
	void duplicateProjectionReturnsExistingExecution() throws Exception {
		UUID projectionId = UUID.randomUUID();
		JobInstance instance = new JobInstance(1L, "VdypFetchAndPartitionJob");
		JobExecution existing = execution(instance, projectionId.toString());
		allowLaunch();
		when(jobExplorer.getJobInstanceCount("VdypFetchAndPartitionJob")).thenReturn(1L);
		when(jobExplorer.getJobInstances("VdypFetchAndPartitionJob", 0, 2)).thenReturn(List.of(instance));
		when(jobExplorer.getJobExecutions(instance)).thenReturn(List.of(existing));

		assertEquals(existing, service.launch(projectionId, "{}"));
		verify(ownershipService, never()).tryAcquire(any(), any());
		verify(claimBoundJobLauncher, never()).launch(any(), any(), any());
	}

	@Test
	void duplicateSearchFindsProjectionOnLaterPage() throws Exception {
		UUID projectionId = UUID.randomUUID();
		JobInstance first = new JobInstance(1L, "VdypFetchAndPartitionJob");
		JobInstance second = new JobInstance(2L, "VdypFetchAndPartitionJob");
		JobExecution other = execution(first, UUID.randomUUID().toString());
		JobExecution existing = execution(second, projectionId.toString());
		allowLaunch();
		when(jobExplorer.getJobInstanceCount("VdypFetchAndPartitionJob")).thenReturn(3L);
		when(jobExplorer.getJobInstances("VdypFetchAndPartitionJob", 0, 2)).thenReturn(List.of(first));
		when(jobExplorer.getJobInstances("VdypFetchAndPartitionJob", 2, 2)).thenReturn(List.of(second));
		when(jobExplorer.getJobExecutions(first)).thenReturn(List.of(other));
		when(jobExplorer.getJobExecutions(second)).thenReturn(List.of(existing));

		assertEquals(existing, service.launch(projectionId, "{}"));
		verify(jobExplorer).getJobInstances("VdypFetchAndPartitionJob", 2, 2);
	}

	@Test
	void unavailableClaimRaisesAlreadyRunningException() throws Exception {
		UUID projectionId = UUID.randomUUID();
		allowLaunch();
		when(jobExplorer.getJobInstanceCount("VdypFetchAndPartitionJob")).thenReturn(0L);
		when(ownershipService.tryAcquire(projectionId.toString(), "new-launch")).thenReturn(Optional.empty());

		assertThrows(JobExecutionAlreadyRunningException.class, () -> service.launch(projectionId, "{}"));
		verify(claimBoundJobLauncher, never()).launch(any(), any(), any());
	}

	@Test
	void launchBuildsParametersCreatesDirectoryAndBindsClaim() throws Exception {
		UUID projectionId = UUID.randomUUID();
		JobClaim claim = claim(projectionId.toString());
		JobExecution launched = execution(new JobInstance(3L, "VdypFetchAndPartitionJob"), projectionId.toString());
		ArgumentCaptor<JobParameters> parameters = ArgumentCaptor.forClass(JobParameters.class);
		allowLaunch();
		when(jobExplorer.getJobInstanceCount("VdypFetchAndPartitionJob")).thenReturn(0L);
		when(ownershipService.tryAcquire(projectionId.toString(), "new-launch")).thenReturn(Optional.of(claim));
		when(claimBoundJobLauncher.launch(any(), any(), any())).thenReturn(launched);

		assertEquals(launched, service.launchNewJob(projectionId, "{\"ageStart\": 10}"));

		verify(claimBoundJobLauncher).launch(
				org.mockito.ArgumentMatchers.eq(vdypBatchJob), parameters.capture(),
				org.mockito.ArgumentMatchers.eq(claim)
		);
		JobParameters value = parameters.getValue();
		assertEquals(projectionId.toString(), value.getString(BatchConstants.GuidInput.PROJECTION_GUID));
		assertEquals("{\"ageStart\": 10}", value.getString(BatchConstants.Projection.PARAMETERS_JSON));
		assertEquals(3L, value.getLong(BatchConstants.Partition.NUMBER));
		assertEquals(25L, value.getLong(BatchConstants.Chunk.SIZE));
		assertTrue(Files.isDirectory(Path.of(value.getString(BatchConstants.Job.BASE_DIR))));
		assertNotNull(value.getString(BatchConstants.Job.GUID));
		assertNotNull(value.getString(BatchConstants.Job.TIMESTAMP));
	}

	@Test
	void idempotencyLookupFailureFallsBackToClaimAndLaunch() throws Exception {
		UUID projectionId = UUID.randomUUID();
		JobClaim claim = claim(projectionId.toString());
		JobExecution launched = execution(new JobInstance(3L, "VdypFetchAndPartitionJob"), projectionId.toString());
		allowLaunch();
		when(jobExplorer.getJobInstanceCount("VdypFetchAndPartitionJob"))
				.thenThrow(new IllegalStateException("repository unavailable"));
		when(ownershipService.tryAcquire(projectionId.toString(), "new-launch")).thenReturn(Optional.of(claim));
		when(claimBoundJobLauncher.launch(any(), any(), any())).thenReturn(launched);

		assertEquals(launched, service.launch(projectionId, "{}"));
		verify(claimBoundJobLauncher).launch(any(), any(), any());
	}

	private void allowLaunch() {
		when(serverCapacityService.hasAvailableCapacity()).thenReturn(true);
		when(ownershipService.isAcceptingNewWork()).thenReturn(true);
	}

	private JobExecution execution(JobInstance instance, String projectionGuid) {
		JobParameters parameters = new JobParametersBuilder()
				.addString(BatchConstants.GuidInput.PROJECTION_GUID, projectionGuid).toJobParameters();
		return new JobExecution(instance, instance.getInstanceId() + 10, parameters);
	}

	private JobClaim claim(String projectionGuid) {
		return new JobClaim(projectionGuid, "owner", UUID.randomUUID(), Instant.now(), Instant.now().plusSeconds(120));
	}
}
