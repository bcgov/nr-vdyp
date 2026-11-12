package ca.bc.gov.nrs.vdyp.batch.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ca.bc.gov.nrs.vdyp.batch.configuration.WorkerRegistryCleanupProperties;

@ExtendWith(MockitoExtension.class)
class WorkerRegistryCleanupSchedulerTest {

	@Mock
	ServerCapacityService capacityService;

	WorkerRegistryCleanupProperties properties;
	WorkerRegistryCleanupScheduler scheduler;

	@BeforeEach
	void setUp() {
		properties = new WorkerRegistryCleanupProperties();
		scheduler = new WorkerRegistryCleanupScheduler(capacityService, properties);
	}

	@Test
	void removesWorkersOlderThanRetention() {
		properties.setRetention(Duration.ofHours(36));
		when(capacityService.removeStaleWorkers(36L * 60 * 60)).thenReturn(2);

		scheduler.removeStaleWorkers();

		verify(capacityService).removeStaleWorkers(36L * 60 * 60);
	}

	@Test
	void defaultRetentionIsOneDay() {
		scheduler.removeStaleWorkers();

		verify(capacityService).removeStaleWorkers(86400L);
	}

	@Test
	void disabledCleanupDoesNothing() {
		properties.setEnabled(false);

		scheduler.removeStaleWorkers();

		verify(capacityService, never()).removeStaleWorkers(anyLong());
	}

	@Test
	void failureIsLoggedAndSwallowed() {
		when(capacityService.removeStaleWorkers(anyLong())).thenThrow(new RuntimeException("database unavailable"));

		assertDoesNotThrow(scheduler::removeStaleWorkers);
	}
}
