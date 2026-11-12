package ca.bc.gov.nrs.vdyp.batch.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import ca.bc.gov.nrs.vdyp.batch.configuration.WorkerRegistryCleanupProperties;

/**
 * Periodically removes batch worker registry entries for replicas that have not sent a heartbeat within the configured
 * retention period. Every replica runs this job; the delete is idempotent, so concurrent runs are harmless. A missed or
 * failed run is not retried until the next scheduled time.
 */
@Component
public class WorkerRegistryCleanupScheduler {
	private static final Logger logger = LoggerFactory.getLogger(WorkerRegistryCleanupScheduler.class);

	private final ServerCapacityService capacityService;
	private final WorkerRegistryCleanupProperties properties;

	public WorkerRegistryCleanupScheduler(
			ServerCapacityService capacityService, WorkerRegistryCleanupProperties properties
	) {
		this.capacityService = capacityService;
		this.properties = properties;
	}

	@Scheduled(cron = "${batch.worker-registry-cleanup.cron:0 0 3 * * *}")
	public void removeStaleWorkers() {
		if (!properties.isEnabled()) {
			return;
		}

		try {
			int removed = capacityService.removeStaleWorkers(properties.getRetention().toSeconds());
			if (removed > 0) {
				logger.info(
						"Removed {} stale batch worker registry entries older than {}", removed,
						properties.getRetention()
				);
			}
		} catch (Exception e) {
			logger.warn("Failed to remove stale batch worker registry entries; will retry at next scheduled run", e);
		}
	}
}
