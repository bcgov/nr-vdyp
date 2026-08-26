package ca.bc.gov.nrs.vdyp.batch.service;

import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;

import ca.bc.gov.nrs.vdyp.batch.configuration.BatchOwnershipProperties;
import ca.bc.gov.nrs.vdyp.batch.ownership.JobOwnershipService;

@Service
public class JobOwnershipHeartbeatService implements SmartLifecycle {

	private final BatchOwnershipProperties properties;
	private final JobOwnershipService ownershipService;
	private final ServerCapacityService capacityService;
	private final AtomicBoolean running = new AtomicBoolean(false);
	private Thread heartbeatThread;

	public JobOwnershipHeartbeatService(
			BatchOwnershipProperties properties, JobOwnershipService ownershipService,
			ServerCapacityService capacityService
	) {
		this.properties = properties;
		this.ownershipService = ownershipService;
		this.capacityService = capacityService;
	}

	@Override
	public void start() {
		if (!properties.isEnabled() || !running.compareAndSet(false, true)) {
			return;
		}
		heartbeatThread = new Thread(this::heartbeatLoop, "vdyp-batch-ownership-heartbeat");
		heartbeatThread.start();
	}

	private void heartbeatLoop() {
		while (running.get()) {
			try {
				Thread.sleep(properties.getHeartbeatInterval().toMillis());
				ownershipService.heartbeatOwnedJobs();
				capacityService.recordThreadCapacityHeartbeat(ownershipService.isAcceptingNewWork());
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				running.set(false);
			}
		}
	}

	@Override
	public void stop() {
		running.set(false);
		if (heartbeatThread != null) {
			heartbeatThread.interrupt();
		}
	}

	@Override
	public boolean isRunning() {
		return running.get();
	}

	@Override
	public int getPhase() {
		return Integer.MIN_VALUE + 100;
	}
}
