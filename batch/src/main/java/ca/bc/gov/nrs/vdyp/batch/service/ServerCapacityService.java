package ca.bc.gov.nrs.vdyp.batch.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import ca.bc.gov.nrs.vdyp.batch.ownership.BatchWorkerIdentity;
import ca.bc.gov.nrs.vdyp.batch.persistence.repository.ServerCapacityRepository;

/**
 * Reports the capacity of the shared Batch executor. Jobs are intentionally not assigned fixed slots because their
 * worker-thread requirements vary with input size.
 */
@Service
public class ServerCapacityService {

	private final ObjectProvider<ThreadPoolTaskExecutor> taskExecutorProvider;
	private final BatchWorkerIdentity identity;
	private final ServerCapacityRepository repository;

	public ServerCapacityService(
			@Qualifier("taskExecutor") ObjectProvider<ThreadPoolTaskExecutor> taskExecutorProvider,
			BatchWorkerIdentity identity, ServerCapacityRepository repository
	) {
		this.taskExecutorProvider = taskExecutorProvider;
		this.identity = identity;
		this.repository = repository;
	}

	public int activeThreads() {
		return taskExecutor().getActiveCount();
	}

	public int maximumThreads() {
		return taskExecutor().getMaxPoolSize();
	}

	public int availableThreads() {
		return Math.max(0, maximumThreads() - activeThreads());
	}

	public boolean hasAvailableCapacity() {
		return activeThreads() < maximumThreads();
	}

	public void recordThreadCapacityHeartbeat(boolean isAcceptingWork) {
		repository.recordCapacityHeartbeat(identity.ownerId(), availableThreads(), isAcceptingWork);
	}

	public Long getAllReplicaCapacity(long maxHeartBeatAgeSeconds) {
		return repository.getAggregateCapacity(maxHeartBeatAgeSeconds);
	}

	private ThreadPoolTaskExecutor taskExecutor() {
		return taskExecutorProvider.getObject();
	}
}
