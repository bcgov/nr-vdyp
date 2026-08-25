package ca.bc.gov.nrs.vdyp.batch.ownership;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

/**
 * Reports the capacity of the shared Batch executor. Jobs are intentionally not assigned fixed slots because their
 * worker-thread requirements vary with input size.
 */
@Component
public class ServerCapacityService {

	private final ObjectProvider<ThreadPoolTaskExecutor> taskExecutorProvider;

	public ServerCapacityService(
			@Qualifier("taskExecutor") ObjectProvider<ThreadPoolTaskExecutor> taskExecutorProvider
	) {
		this.taskExecutorProvider = taskExecutorProvider;
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

	private ThreadPoolTaskExecutor taskExecutor() {
		return taskExecutorProvider.getObject();
	}
}
