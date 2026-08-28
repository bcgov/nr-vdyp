package ca.bc.gov.nrs.vdyp.batch.ownership;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import ca.bc.gov.nrs.vdyp.batch.service.ServerCapacityService;

@Component("batchReadiness")
public class BatchReadinessHealthIndicator implements HealthIndicator {

	private final JobOwnershipService ownershipService;
	private final ServerCapacityService serverCapacityService;

	public BatchReadinessHealthIndicator(
			JobOwnershipService ownershipService, ServerCapacityService serverCapacityService
	) {
		this.ownershipService = ownershipService;
		this.serverCapacityService = serverCapacityService;
	}

	@Override
	public Health health() {
		if (!ownershipService.isAcceptingNewWork()) {
			return Health.down().withDetail("reason", "ownership-renewal-unavailable").build();
		}
		return Health.up().withDetail("activeThreads", serverCapacityService.activeThreads())
				.withDetail("availableThreads", serverCapacityService.availableThreads()).build();
	}
}
