package ca.bc.gov.nrs.vdyp.batch.service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import ca.bc.gov.nrs.vdyp.batch.configuration.BatchOwnershipProperties;
import ca.bc.gov.nrs.vdyp.batch.ownership.JobOwnershipService;
import ca.bc.gov.nrs.vdyp.batch.ownership.OwnedJobRegistry;

@Component
public class BatchShutdownDrainService implements SmartLifecycle {

	private static final Logger logger = LoggerFactory.getLogger(BatchShutdownDrainService.class);

	private final BatchOwnershipProperties properties;
	private final JobOwnershipService ownershipService;
	private final OwnedJobRegistry registry;
	private final AtomicBoolean running = new AtomicBoolean(false);

	public BatchShutdownDrainService(
			BatchOwnershipProperties properties, JobOwnershipService ownershipService, OwnedJobRegistry registry
	) {
		this.properties = properties;
		this.ownershipService = ownershipService;
		this.registry = registry;
	}

	@Override
	public void start() {
		running.set(true);
	}

	@Override
	public void stop() {
		running.set(false);
		ownershipService.beginShutdownDrain();
		Instant deadline = Instant.now().plus(properties.getShutdownWait());
		logger.info(
				"Batch shutdown drain started. New work is disabled; waiting for {} locally owned job(s) to finish.",
				registry.size()
		);
		waitForDrain(deadline);
	}

	private void waitForDrain(Instant deadline) {
		while (Instant.now().isBefore(deadline) && registry.size() > 0) {
			try {
				Thread.sleep(Math.min(Duration.ofSeconds(2).toMillis(), properties.getHeartbeatInterval().toMillis()));
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
		}
		if (registry.size() > 0) {
			logger.warn(
					"Shutdown drain deadline reached with local jobs still running. activeJobs={}. "
							+ "The process may now terminate; claims will be recovered after lease expiry.",
					registry.size()
			);
		} else {
			logger.info("Batch shutdown drain completed; no locally owned executions remain.");
		}
	}

	@Override
	public boolean isRunning() {
		return running.get();
	}

	@Override
	public int getPhase() {
		return 900;
	}
}
