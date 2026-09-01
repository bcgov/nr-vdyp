package ca.bc.gov.nrs.vdyp.batch.service;

import java.util.List;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Finds job executions by an arbitrary job parameter (job GUID or projection GUID), searching across all job names and
 * instances via the shared JobExplorer. Used by REST endpoints and by the prioritize NATS listener, which only has a
 * job GUID to work with.
 */
@Service
public class JobExecutionLookupService {

	private static final Logger logger = LoggerFactory.getLogger(JobExecutionLookupService.class);

	private final JobExplorer jobExplorer;

	@Value("${batch.partition.job-search-chunk-size}")
	private int jobSearchChunkSize;

	public JobExecutionLookupService(JobExplorer jobExplorer) {
		this.jobExplorer = jobExplorer;
	}

	public JobExecution
			findJobExecutionByJobParameter(String parameterName, String expectedValue, boolean preferRunning)
					throws NoSuchJobExecutionException {
		List<String> jobNames = jobExplorer.getJobNames();
		JobExecution fallbackMatch = null;

		for (String jobName : jobNames) {
			JobExecutionSearchResult result = searchJobExecutionsByName(
					jobName, parameterName, expectedValue, preferRunning
			);
			if (result.primaryMatch() != null) {
				return result.primaryMatch();
			}
			if (fallbackMatch == null) {
				fallbackMatch = result.fallbackMatch();
			}
		}

		if (fallbackMatch != null) {
			return fallbackMatch;
		}

		throw new NoSuchJobExecutionException("No job execution found with " + parameterName + ": " + expectedValue);
	}

	/**
	 * Searches for a job execution by job name and job parameter value.
	 *
	 * @param jobName The name of the job to search
	 * @return matching executions, preferring a running match when requested
	 */
	private JobExecutionSearchResult searchJobExecutionsByName(
			String jobName, String parameterName, String expectedValue, boolean preferRunning
	) {
		JobExecution fallbackMatch = null;
		try {
			long totalInstances = jobExplorer.getJobInstanceCount(jobName);

			for (long start = 0; start < totalInstances; start += jobSearchChunkSize) {
				JobExecutionSearchResult result = searchJobExecutionsInChunk(
						jobName, parameterName, expectedValue, start, preferRunning
				);
				if (result.primaryMatch() != null) {
					return result;
				}
				if (fallbackMatch == null) {
					fallbackMatch = result.fallbackMatch();
				}
			}
		} catch (NoSuchJobException e) {
			logger.error(
					"Job {} not found while searching for {} {}: {}", jobName, parameterName, expectedValue,
					e.getMessage()
			);
		}

		return new JobExecutionSearchResult(null, fallbackMatch);
	}

	/**
	 * Searches for a job execution within a chunk of job instances.
	 *
	 * @param jobName The name of the job
	 * @param start   The starting index for the chunk
	 * @return matching executions, preferring a running match when requested
	 */
	private JobExecutionSearchResult searchJobExecutionsInChunk(
			String jobName, String parameterName, String expectedValue, long start, boolean preferRunning
	) {
		List<JobInstance> jobInstances = jobExplorer.getJobInstances(jobName, (int) start, jobSearchChunkSize);
		JobExecution fallbackMatch = null;

		for (JobInstance jobInstance : jobInstances) {
			JobExecutionSearchResult result = findMatchingExecution(
					jobInstance, parameterName, expectedValue, preferRunning
			);
			if (result.primaryMatch() != null) {
				return result;
			}
			if (fallbackMatch == null) {
				fallbackMatch = result.fallbackMatch();
			}
		}

		return new JobExecutionSearchResult(null, fallbackMatch);
	}

	/**
	 * Finds a job execution matching the given job parameter within a job instance.
	 *
	 * @param jobInstance The job instance to search
	 * @return matching executions, preferring a running match when requested
	 */
	private JobExecutionSearchResult findMatchingExecution(
			JobInstance jobInstance, String parameterName, String expectedValue, boolean preferRunning
	) {
		List<JobExecution> jobExecutions = jobExplorer.getJobExecutions(jobInstance);
		JobExecution fallbackMatch = null;

		for (JobExecution execution : jobExecutions) {
			String parameterValue = execution.getJobParameters().getString(parameterName);
			if (expectedValue.equals(parameterValue)) {
				if (!preferRunning || (execution.getStatus() != null && execution.getStatus().isRunning())) {
					return new JobExecutionSearchResult(execution, null);
				}
				if (fallbackMatch == null) {
					fallbackMatch = execution;
				}
			}
		}

		return new JobExecutionSearchResult(null, fallbackMatch);
	}

	private record JobExecutionSearchResult(JobExecution primaryMatch, JobExecution fallbackMatch) {
	}
}
