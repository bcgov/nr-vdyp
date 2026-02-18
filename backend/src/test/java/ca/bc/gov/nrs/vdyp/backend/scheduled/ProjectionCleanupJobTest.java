package ca.bc.gov.nrs.vdyp.backend.scheduled;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ca.bc.gov.nrs.vdyp.backend.services.ProjectionService;

@ExtendWith(MockitoExtension.class)
class ProjectionCleanupJobTest {
	@Mock
	ProjectionService projectionService;

	@Test
	void testRun() {
		ProjectionCleanupJob job = new ProjectionCleanupJob(projectionService);
		job.run();

		// Verify that the projectionService's cleanup method was called
		verify(projectionService).cleanupExpiredProjections();
	}
}
