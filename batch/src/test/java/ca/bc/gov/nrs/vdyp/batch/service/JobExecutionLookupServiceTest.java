package ca.bc.gov.nrs.vdyp.batch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.test.util.ReflectionTestUtils;

import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JobExecutionLookupServiceTest {

	@Mock
	private JobExplorer jobExplorer;

	private JobExecutionLookupService lookupService;

	@BeforeEach
	void setUp() {
		lookupService = new JobExecutionLookupService(jobExplorer);
		ReflectionTestUtils.setField(lookupService, "jobSearchChunkSize", 1000);
	}

	@Test
	void testFindJobExecutionByJobParameter_WithNonExistentGuid_ThrowsException() throws Exception {
		String jobGuid = UUID.randomUUID().toString();

		when(jobExplorer.getJobNames()).thenReturn(List.of("testJob"));
		when(jobExplorer.getJobInstanceCount("testJob")).thenReturn(1L);
		JobInstance instance = new JobInstance(1L, "testJob");
		when(jobExplorer.getJobInstances("testJob", 0, 1000)).thenReturn(List.of(instance));
		JobParameters params = new JobParametersBuilder().addString(BatchConstants.Job.GUID, "different-guid")
				.toJobParameters();
		JobExecution execution = new JobExecution(instance, 1L, params);
		when(jobExplorer.getJobExecutions(instance)).thenReturn(List.of(execution));

		assertThrows(
				NoSuchJobExecutionException.class,
				() -> lookupService.findJobExecutionByJobParameter(BatchConstants.Job.GUID, jobGuid, false)
		);
	}

	@Test
	void testFindJobExecutionByJobParameter_WithEmptyJobNames_ThrowsException() {
		when(jobExplorer.getJobNames()).thenReturn(Collections.emptyList());

		assertThrows(
				NoSuchJobExecutionException.class,
				() -> lookupService
						.findJobExecutionByJobParameter(BatchConstants.Job.GUID, UUID.randomUUID().toString(), false)
		);
	}

	@Test
	void testFindJobExecutionByJobParameter_WithMultipleJobInstances_FindsCorrectOne() throws Exception {
		String jobGuid = UUID.randomUUID().toString();
		Long executionId = 123L;

		JobInstance instance1 = new JobInstance(1L, "testJob");
		JobInstance instance2 = new JobInstance(2L, "testJob");

		JobParameters params1 = new JobParametersBuilder().addString(BatchConstants.Job.GUID, "wrong-guid")
				.toJobParameters();
		JobParameters params2 = new JobParametersBuilder().addString(BatchConstants.Job.GUID, jobGuid)
				.toJobParameters();

		JobExecution execution1 = new JobExecution(instance1, 1L, params1);
		JobExecution execution2 = new JobExecution(instance2, executionId, params2);
		execution2.setStatus(BatchStatus.STARTED);

		when(jobExplorer.getJobNames()).thenReturn(List.of("testJob"));
		when(jobExplorer.getJobInstanceCount("testJob")).thenReturn(2L);
		when(jobExplorer.getJobInstances("testJob", 0, 1000)).thenReturn(List.of(instance1, instance2));
		when(jobExplorer.getJobExecutions(instance1)).thenReturn(List.of(execution1));
		when(jobExplorer.getJobExecutions(instance2)).thenReturn(List.of(execution2));

		JobExecution found = lookupService.findJobExecutionByJobParameter(BatchConstants.Job.GUID, jobGuid, false);

		assertEquals(executionId, found.getId());
	}

	@Test
	void testFindJobExecutionByJobParameter_PreferRunning_FallsBackToNonRunningWhenNoneRunning() throws Exception {
		String jobGuid = UUID.randomUUID().toString();
		JobInstance instance = new JobInstance(1L, "testJob");
		JobParameters params = new JobParametersBuilder().addString(BatchConstants.Job.GUID, jobGuid).toJobParameters();
		JobExecution completed = new JobExecution(instance, 1L, params);
		completed.setStatus(BatchStatus.COMPLETED);

		when(jobExplorer.getJobNames()).thenReturn(List.of("testJob"));
		when(jobExplorer.getJobInstanceCount("testJob")).thenReturn(1L);
		when(jobExplorer.getJobInstances("testJob", 0, 1000)).thenReturn(List.of(instance));
		when(jobExplorer.getJobExecutions(instance)).thenReturn(List.of(completed));

		JobExecution found = lookupService.findJobExecutionByJobParameter(BatchConstants.Job.GUID, jobGuid, true);

		assertEquals(completed.getId(), found.getId());
	}
}
