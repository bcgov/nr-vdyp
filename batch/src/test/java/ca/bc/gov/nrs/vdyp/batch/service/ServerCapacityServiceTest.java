package ca.bc.gov.nrs.vdyp.batch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import ca.bc.gov.nrs.vdyp.batch.ownership.BatchWorkerIdentity;
import ca.bc.gov.nrs.vdyp.batch.persistence.repository.ServerCapacityRepository;

@ExtendWith(MockitoExtension.class)
class ServerCapacityServiceTest {

	@Mock
	ObjectProvider<ThreadPoolTaskExecutor> taskExecutorProvider;
	@Mock
	ThreadPoolTaskExecutor taskExecutor;
	@Mock
	BatchWorkerIdentity identity;
	@Mock
	ServerCapacityRepository repository;

	ServerCapacityService service;

	@BeforeEach
	void setUp() {
		lenient().when(taskExecutorProvider.getObject()).thenReturn(taskExecutor);
		service = new ServerCapacityService(taskExecutorProvider, identity, repository);
	}

	@Test
	void reportsExecutorUsageAndAvailableCapacity() {
		when(taskExecutor.getActiveCount()).thenReturn(3);
		when(taskExecutor.getMaxPoolSize()).thenReturn(8);

		assertEquals(3, service.activeThreads());
		assertEquals(8, service.maximumThreads());
		assertEquals(5, service.availableThreads());
		assertTrue(service.hasAvailableCapacity());
	}

	@Test
	void availableThreadsNeverFallsBelowZero() {
		when(taskExecutor.getActiveCount()).thenReturn(9);
		when(taskExecutor.getMaxPoolSize()).thenReturn(8);

		assertEquals(0, service.availableThreads());
		assertFalse(service.hasAvailableCapacity());
	}

	@Test
	void heartbeatRecordsOwnerAvailableThreadsAndAcceptanceState() {
		when(identity.ownerId()).thenReturn("worker-1");
		when(taskExecutor.getMaxPoolSize()).thenReturn(8);

		service.recordThreadCapacityHeartbeat(false);

		verify(repository).recordCapacityHeartbeat("worker-1", 8, false);
	}

	@Test
	void aggregateCapacityIsReturnedFromRepository() {
		when(repository.getAggregateCapacity(45)).thenReturn(12L);

		assertEquals(12L, service.getAllReplicaCapacity(45));
		verify(repository).getAggregateCapacity(45);
	}
}
