package ca.bc.gov.nrs.vdyp.backend.scheduled;

import ca.bc.gov.nrs.vdyp.backend.services.ProjectionService;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProjectionCleanupJob {
	ProjectionService projectionService;

	public ProjectionCleanupJob(ProjectionService projectionService) {
		this.projectionService = projectionService;
	}

	@Scheduled(cron = "0 0 2 * * ?")
	void run() {
		projectionService.cleanupExpiredProjections();
	}
}
