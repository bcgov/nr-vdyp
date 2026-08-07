package ca.bc.gov.nrs.vdyp.backend.scheduled;

import ca.bc.gov.nrs.vdyp.backend.services.ProjectionBatchMappingService;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProjectionStuckDetectionJob {
	ProjectionBatchMappingService batchMappingService;

	public ProjectionStuckDetectionJob(ProjectionBatchMappingService batchMappingService) {
		this.batchMappingService = batchMappingService;
	}

	@Scheduled(every = "${vdyp.projection.stuck.check-interval:5m}")
	void run() {
		batchMappingService.updateStuckProjectionStatuses();
	}
}
