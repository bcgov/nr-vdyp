package ca.bc.gov.nrs.vdyp.backend.scheduled;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ca.bc.gov.nrs.vdyp.backend.services.ProjectionBatchMappingService;

@ExtendWith(MockitoExtension.class)
class ProjectionStuckDetectionJobTest {
	@Mock
	ProjectionBatchMappingService batchMappingService;

	@Test
	void testRun() {
		ProjectionStuckDetectionJob job = new ProjectionStuckDetectionJob(batchMappingService);
		job.run();

		verify(batchMappingService).updateStuckProjectionStatuses();
	}
}
