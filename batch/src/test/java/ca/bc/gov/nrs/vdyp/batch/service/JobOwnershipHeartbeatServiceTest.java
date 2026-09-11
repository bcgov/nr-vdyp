package ca.bc.gov.nrs.vdyp.batch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ca.bc.gov.nrs.vdyp.batch.configuration.BatchOwnershipProperties;
import ca.bc.gov.nrs.vdyp.batch.ownership.JobOwnershipService;

@ExtendWith(MockitoExtension.class)
class JobOwnershipHeartbeatServiceTest {

	@Mock
	JobOwnershipService ownershipService;
	@Mock
	ServerCapacityService capacityService;

	BatchOwnershipProperties properties;
	JobOwnershipHeartbeatService service;

	@BeforeEach
	void setUp() {
		properties = new BatchOwnershipProperties();
		service = new JobOwnershipHeartbeatService(properties, ownershipService, capacityService);
	}

	@AfterEach
	void stopService() {
		service.stop();
	}

	@Test
	void disabledOwnershipDoesNotStartHeartbeatThread() {
		properties.setEnabled(false);

		service.start();

		assertFalse(service.isRunning());
		verify(ownershipService, never()).heartbeatOwnedJobs();
	}

	@Test
	void heartbeatRenewsClaimsAndRecordsAcceptance() throws Exception {
		properties.setHeartbeatInterval(Duration.ofMillis(1));
		when(ownershipService.isAcceptingNewWork()).thenReturn(true);
		CountDownLatch heartbeatRecorded = new CountDownLatch(1);
		AtomicReference<String> threadName = new AtomicReference<>();
		doAnswer(invocation -> {
			threadName.set(Thread.currentThread().getName());
			heartbeatRecorded.countDown();
			return null;
		}).when(capacityService).recordThreadCapacityHeartbeat(true);

		service.start();
		service.start();

		assertTrue(service.isRunning());
		assertTrue(heartbeatRecorded.await(2, TimeUnit.SECONDS));
		service.stop();
		assertFalse(service.isRunning());
		assertEquals("vdyp-batch-ownership-heartbeat", threadName.get());
		assertEquals(Integer.MIN_VALUE + 100, service.getPhase());
		verify(ownershipService, atLeastOnce()).heartbeatOwnedJobs();
		verify(capacityService, atLeastOnce()).recordThreadCapacityHeartbeat(true);
	}

	@Test
	void stopBeforeStartIsSafe() {
		service.stop();
		assertFalse(service.isRunning());
	}
}
