package ca.bc.gov.nrs.vdyp.batch.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ca.bc.gov.nrs.vdyp.batch.configuration.BatchOwnershipProperties;
import ca.bc.gov.nrs.vdyp.batch.ownership.JobOwnershipService;
import ca.bc.gov.nrs.vdyp.batch.ownership.OwnedJobRegistry;

@ExtendWith(MockitoExtension.class)
class BatchShutdownDrainServiceTest {

	@Mock
	JobOwnershipService ownershipService;
	@Mock
	OwnedJobRegistry registry;

	BatchOwnershipProperties properties;
	BatchShutdownDrainService service;

	@BeforeEach
	void setUp() {
		properties = new BatchOwnershipProperties();
		service = new BatchShutdownDrainService(properties, ownershipService, registry);
	}

	@Test
	void lifecycleStartsAndStopsImmediatelyWhenRegistryIsEmpty() {
		properties.setShutdownWait(Duration.ZERO);
		when(registry.size()).thenReturn(0);

		assertFalse(service.isRunning());
		service.start();
		assertTrue(service.isRunning());
		assertTrue(service.getPhase() > 0);

		service.stop();

		assertFalse(service.isRunning());
		verify(ownershipService).beginShutdownDrain();
	}

	@Test
	void stopWaitsUntilOwnedJobsDrain() {
		properties.setShutdownWait(Duration.ofSeconds(1));
		properties.setHeartbeatInterval(Duration.ofMillis(1));
		when(registry.size()).thenReturn(1, 1, 0, 0);

		service.stop();

		verify(ownershipService).beginShutdownDrain();
	}

	@Test
	void stopReturnsWhenDeadlineExpiresWithActiveJobs() {
		properties.setShutdownWait(Duration.ZERO);
		when(registry.size()).thenReturn(1);

		service.stop();

		verify(ownershipService).beginShutdownDrain();
	}

	@Test
	void interruptedDrainRestoresInterruptFlag() throws Exception {
		properties.setShutdownWait(Duration.ofSeconds(1));
		properties.setHeartbeatInterval(Duration.ofMillis(10));
		when(registry.size()).thenReturn(1);
		AtomicBoolean interrupted = new AtomicBoolean(false);

		Thread worker = new Thread(() -> {
			Thread.currentThread().interrupt();
			service.stop();
			interrupted.set(Thread.currentThread().isInterrupted());
		});
		worker.start();
		worker.join(Duration.ofSeconds(2).toMillis());

		assertFalse(worker.isAlive());
		assertTrue(interrupted.get());
	}
}
