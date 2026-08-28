package ca.bc.gov.nrs.vdyp.batch.ownership;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;

import ca.bc.gov.nrs.vdyp.batch.configuration.BatchOwnershipProperties;
import ca.bc.gov.nrs.vdyp.batch.persistence.model.JobClaim;
import ca.bc.gov.nrs.vdyp.batch.persistence.repository.JobOwnershipRepository;
import ca.bc.gov.nrs.vdyp.batch.service.ServerCapacityService;
import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@ExtendWith(MockitoExtension.class)
class JobOwnershipServiceTest {

	private static final String OWNER_ID = "worker-1";
	private static final Duration LEASE_DURATION = Duration.ofMinutes(2);

	@Mock
	BatchWorkerIdentity identity;
	@Mock
	JobOwnershipRepository repository;
	@Mock
	ServerCapacityService serverCapacityService;

	BatchOwnershipProperties properties;
	OwnedJobRegistry registry;
	SimpleMeterRegistry meterRegistry;
	JobOwnershipService service;

	@BeforeEach
	void setUp() {
		properties = new BatchOwnershipProperties();
		properties.setLeaseDuration(LEASE_DURATION);
		registry = new OwnedJobRegistry();
		meterRegistry = new SimpleMeterRegistry();
		service = new JobOwnershipService(
				properties, identity, repository, registry, meterRegistry, serverCapacityService
		);
	}

	@Test
	void constructor_registersCapacityAndClaimGauges() {
		when(serverCapacityService.activeThreads()).thenReturn(3);
		when(serverCapacityService.maximumThreads()).thenReturn(8);
		when(serverCapacityService.availableThreads()).thenReturn(5);
		when(repository.countActiveClaims()).thenReturn(4L);

		assertEquals(3.0, meterRegistry.get("vdyp.batch.threads.active.local").gauge().value());
		assertEquals(8.0, meterRegistry.get("vdyp.batch.threads.capacity.local").gauge().value());
		assertEquals(5.0, meterRegistry.get("vdyp.batch.threads.capacity.available").gauge().value());
		assertEquals(4.0, meterRegistry.get("vdyp.batch.jobs.claims.active.global").gauge().value());
	}

	@Test
	void intakeCanResumeUntilShutdownDrainBegins() {
		assertTrue(service.isAcceptingNewWork());

		service.stopAcceptingNewWork();
		assertFalse(service.isAcceptingNewWork());

		service.resumeAcceptingNewWork();
		assertTrue(service.isAcceptingNewWork());

		service.beginShutdownDrain();
		service.resumeAcceptingNewWork();
		assertFalse(service.isAcceptingNewWork());
	}

	@Test
	void disabledOwnershipAlwaysAcceptsWorkAndReturnsSyntheticClaim() {
		properties.setEnabled(false);
		when(identity.ownerId()).thenReturn(OWNER_ID);
		service.beginShutdownDrain();

		Optional<JobClaim> result = service.tryAcquire("projection-1", "submission");

		assertTrue(service.isAcceptingNewWork());
		assertTrue(result.isPresent());
		assertEquals("projection-1", result.orElseThrow().projectionGuid());
		assertEquals(OWNER_ID, result.orElseThrow().ownerId());
		assertEquals(new UUID(0, 0), result.orElseThrow().leaseToken());
		assertEquals(Instant.EPOCH, result.orElseThrow().acquiredTime());
		assertEquals(Instant.MAX, result.orElseThrow().leaseExpiryTime());
		verify(identity).ownerId();
		verifyNoInteractions(repository);
	}

	@Test
	void tryAcquireRecordsSuccessAndTakeoverMetrics() {
		JobClaim previous = claim("projection-1");
		JobClaim acquired = claim("projection-1");
		when(identity.ownerId()).thenReturn(OWNER_ID);
		when(repository.findByProjectionGuid("projection-1")).thenReturn(Optional.of(previous));
		when(repository.acquire(any(), any(), any(), any())).thenReturn(Optional.of(acquired));

		assertEquals(Optional.of(acquired), service.tryAcquire("projection-1", "recovery"));
		assertEquals(1.0, counter("vdyp.batch.claim.attempts", "result", "success"));
		assertEquals(0.0, counter("vdyp.batch.claim.attempts", "result", "failed"));
		assertEquals(1.0, counter("vdyp.batch.claim.takeovers", "result", "success"));

		ArgumentCaptor<UUID> token = ArgumentCaptor.forClass(UUID.class);
		verify(repository).acquire(eq("projection-1"), eq(OWNER_ID), token.capture(), eq(LEASE_DURATION));
		Assertions.assertNotEquals(new UUID(0, 0), token.getValue());
	}

	@Test
	void tryAcquireRecordsFailedAttemptWithoutTakeover() {
		when(identity.ownerId()).thenReturn(OWNER_ID);
		when(repository.findByProjectionGuid("projection-1")).thenReturn(Optional.empty());
		when(repository.acquire(any(), any(), any(), any())).thenReturn(Optional.empty());

		assertTrue(service.tryAcquire("projection-1", "submission").isEmpty());
		assertEquals(0.0, counter("vdyp.batch.claim.attempts", "result", "success"));
		assertEquals(1.0, counter("vdyp.batch.claim.attempts", "result", "failed"));
		assertEquals(0.0, counter("vdyp.batch.claim.takeovers", "result", "success"));
	}

	@Test
	void tryAcquireRecordsFirstClaimAsSuccessWithoutTakeover() {
		JobClaim acquired = claim("projection-1");
		when(identity.ownerId()).thenReturn(OWNER_ID);
		when(repository.findByProjectionGuid("projection-1")).thenReturn(Optional.empty());
		when(repository.acquire(any(), any(), any(), any())).thenReturn(Optional.of(acquired));

		assertEquals(Optional.of(acquired), service.tryAcquire("projection-1", "submission"));
		assertEquals(1.0, counter("vdyp.batch.claim.attempts", "result", "success"));
		assertEquals(0.0, counter("vdyp.batch.claim.takeovers", "result", "success"));
	}

	@Test
	void assertCurrentOwnerRequiresRegisteredCurrentClaim() {
		JobExecution execution = execution("projection-1");
		IllegalStateException missing = assertThrows(
				IllegalStateException.class, () -> service.assertCurrentOwner(execution)
		);
		assertTrue(missing.getMessage().contains("does not own"));

		JobClaim claim = claim("projection-1");
		service.registerClaim(claim);
		when(repository.isCurrent(claim)).thenReturn(true);
		service.assertCurrentOwner(execution);

		when(repository.isCurrent(claim)).thenReturn(false);
		IllegalStateException lost = assertThrows(
				IllegalStateException.class, () -> service.assertCurrentOwner(execution)
		);
		assertTrue(lost.getMessage().contains("no longer owns"));
		assertTrue(registry.findByProjectionGuid("projection-1").orElseThrow().leaseLost());
	}

	@Test
	void assertCurrentOwnerDoesNothingWhenOwnershipIsDisabled() {
		properties.setEnabled(false);
		service.assertCurrentOwner(execution(null));
		verifyNoInteractions(repository);
	}

	@Test
	void executionOperationsRejectMissingProjectionGuid() {
		JobExecution missing = execution(null);
		JobExecution blank = execution("   ");

		assertThrows(IllegalStateException.class, () -> service.assertCurrentOwner(missing));
		assertThrows(IllegalStateException.class, () -> service.finalizeOwnedExecution(blank));
	}

	@Test
	void successfulHeartbeatRenewsEveryClaimAndResumesIntake() {
		JobClaim first = claim("projection-1");
		JobClaim second = claim("projection-2");
		service.registerClaim(first);
		service.registerClaim(second);
		service.stopAcceptingNewWork();
		when(repository.renew(any(), any())).thenReturn(true);

		service.heartbeatOwnedJobs();

		verify(repository).renew(first, LEASE_DURATION);
		verify(repository).renew(second, LEASE_DURATION);
		assertTrue(service.isAcceptingNewWork());
	}

	@Test
	void confirmedLeaseLossStopsIntakeAndIsCountedOnce() {
		JobClaim claim = claim("projection-1");
		service.registerClaim(claim);
		when(repository.renew(claim, LEASE_DURATION)).thenReturn(false);

		service.heartbeatOwnedJobs();
		service.heartbeatOwnedJobs();

		assertFalse(service.isAcceptingNewWork());
		assertTrue(registry.findByProjectionGuid("projection-1").orElseThrow().leaseLost());
		assertEquals(1.0, meterRegistry.get("vdyp.batch.lease.lost").counter().count());
		verify(repository, times(2)).renew(claim, LEASE_DURATION);
	}

	@Test
	void renewalExceptionStopsIntakeAndRecordsFailure() {
		JobClaim failed = claim("projection-1");
		JobClaim renewed = claim("projection-2");
		service.registerClaim(failed);
		service.registerClaim(renewed);
		doThrow(new IllegalStateException("database unavailable")).when(repository).renew(failed, LEASE_DURATION);
		when(repository.renew(renewed, LEASE_DURATION)).thenReturn(true);

		service.heartbeatOwnedJobs();

		assertFalse(service.isAcceptingNewWork());
		assertEquals(1.0, meterRegistry.get("vdyp.batch.lease.renewals").tag("result", "failed").counter().count());
		assertFalse(registry.findByProjectionGuid("projection-1").orElseThrow().leaseLost());
		verify(repository).renew(renewed, LEASE_DURATION);
	}

	@Test
	void disabledHeartbeatDoesNotRenewClaims() {
		properties.setEnabled(false);
		service.registerClaim(claim("projection-1"));

		service.heartbeatOwnedJobs();

		verify(repository, never()).renew(any(), any());
	}

	@Test
	void finalizeReleasesCurrentClaimAndRemovesItFromRegistry() {
		JobClaim claim = claim("projection-1");
		service.registerClaim(claim);

		service.finalizeOwnedExecution(execution("projection-1"));

		verify(repository).release(claim);
		assertFalse(service.isOwnedLocally("projection-1"));
		assertEquals(0, service.activeLocalJobs());
	}

	@Test
	void finalizeDoesNotReleaseLostOrDisabledClaims() {
		JobClaim lostClaim = claim("projection-1");
		service.registerClaim(lostClaim);
		registry.findByProjectionGuid("projection-1").orElseThrow().markLeaseLost();
		service.finalizeOwnedClaim("projection-1");

		JobClaim disabledClaim = claim("projection-2");
		service.registerClaim(disabledClaim);
		properties.setEnabled(false);
		service.finalizeOwnedClaim("projection-2");
		service.finalizeOwnedClaim("not-registered");

		verify(repository, never()).release(any());
		assertEquals(0, service.activeLocalJobs());
	}

	@Test
	void releaseAndLookupDelegateOnlyWhenEnabled() {
		JobClaim claim = claim("projection-1");
		when(repository.findByProjectionGuid("projection-1")).thenReturn(Optional.of(claim));

		service.releaseUnboundClaim(claim);
		assertEquals(Optional.of(claim), service.findProjectionClaim("projection-1"));

		properties.setEnabled(false);
		service.releaseUnboundClaim(claim);
		assertTrue(service.findProjectionClaim("projection-1").isEmpty());
		verify(repository).release(claim);
		verify(repository).findByProjectionGuid("projection-1");
	}

	private double counter(String name, String tagName, String tagValue) {
		return meterRegistry.get(name).tag(tagName, tagValue).counter().count();
	}

	private JobClaim claim(String projectionGuid) {
		return new JobClaim(
				projectionGuid, OWNER_ID, UUID.randomUUID(), Instant.now(), Instant.now().plus(LEASE_DURATION)
		);
	}

	private JobExecution execution(String projectionGuid) {
		JobParametersBuilder parameters = new JobParametersBuilder();
		if (projectionGuid != null) {
			parameters.addString(BatchConstants.GuidInput.PROJECTION_GUID, projectionGuid);
		}
		JobParameters jobParameters = parameters.toJobParameters();
		return new JobExecution(new JobInstance(1L, "job"), 2L, jobParameters);
	}
}
