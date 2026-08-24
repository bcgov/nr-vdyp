package ca.bc.gov.nrs.vdyp.batch.service;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;

import ca.bc.gov.nrs.vdyp.batch.configuration.BatchProperties;
import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;
import ca.bc.gov.nrs.vdyp.batch.util.BatchUtils;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters.ExecutionOption;

/**
 * Estimates PVC storage footprint of currently running projections using the algorithm defined in the VDYP-1274 spike,
 * and compares it against actual filesystem usage to determine whether storage is 'out of spec'
 */
@Service
public class StorageEstimationService {

	private static final Logger logger = LoggerFactory.getLogger(StorageEstimationService.class);
	private static final String FETCH_AND_PARTITION_JOB_NAME = "VdypFetchAndPartitionJob";

	private final JobExplorer jobExplorer;
	private final BatchProperties batchProperties;
	private final ObjectMapper objectMapper;

	public StorageEstimationService(
			JobExplorer jobExplorer, BatchProperties batchProperties, ObjectMapper objectMapper
	) {
		this.jobExplorer = jobExplorer;
		this.batchProperties = batchProperties;
		this.objectMapper = objectMapper;
	}

	public StorageStatus computeStorageStatus() {
		long expectedBytes = estimateExpectedFootprintBytes();
		StorageUsage usage = readActualUsage();
		int thresholdPercent = batchProperties.getStorage().getThresholdPercent();
		double percentFull = usage.totalBytes() == 0 ? 0.0 : (usage.usedBytes() * 100.0) / usage.totalBytes();
		boolean outOfSpec = isOutOfSpec(expectedBytes, usage.usedBytes(), thresholdPercent);

		return new StorageStatus(
				percentFull, usage.usedBytes(), usage.totalBytes(), expectedBytes, outOfSpec, thresholdPercent
		);
	}

	/**
	 * expectedBytes only covers currently running jobs, so with none running, any actual usage is unexplained (e.g.
	 * leftover files) and flagged directly - proving that with the real numbers is the point of this check.
	 */
	boolean isOutOfSpec(long expectedBytes, long actualUsedBytes, int thresholdPercent) {
		if (expectedBytes <= 0) {
			return actualUsedBytes > 0;
		}
		return actualUsedBytes > (expectedBytes * thresholdPercent) / 100.0;
	}

	private long estimateExpectedFootprintBytes() {
		long expectedBytes = 0;
		for (JobExecution job : jobExplorer.findRunningJobExecutions(FETCH_AND_PARTITION_JOB_NAME)) {
			expectedBytes += outputBytesPerPolygonForJob(job) * numPolygonsForOutput(job)
					+ batchProperties.getStorage().getBytesPerInputLine() * numPolygonsForInput(job);
		}
		return expectedBytes;
	}

	private long outputBytesPerPolygonForJob(JobExecution job) {
		BatchProperties.StorageProperties storage = batchProperties.getStorage();
		Optional<Parameters> parameters = parseParameters(job);

		RangeSpan rangeSpan = parameters.flatMap(this::resolveRangeSpan)
				.orElseGet(() -> new RangeSpan(storage.getFallbackYearRange(), storage.getFallbackAgeIncrement()));
		long increments = rangeSpan.ageIncrement() <= 0 ? 0 : rangeSpan.yearRange() / rangeSpan.ageIncrement();

		long outputBytes = storage.getBytesPerCompleteLine() * increments;
		if (parameters.map(p -> hasExecutionOption(p, ExecutionOption.DO_ENABLE_ERROR_LOGGING)).orElse(false)) {
			outputBytes += storage.getReasonableErrorBytesPerPolygon();
		}
		if (parameters.map(p -> hasExecutionOption(p, ExecutionOption.DO_ENABLE_DEBUG_LOGGING)).orElse(false)) {
			outputBytes += storage.getOptionalDebugLogBytesPerPolygon();
		}
		return outputBytes;
	}

	private boolean hasExecutionOption(Parameters parameters, ExecutionOption option) {
		return parameters.getSelectedExecutionOptions().contains(option.toString());
	}

	private Optional<Parameters> parseParameters(JobExecution job) {
		String parametersJson = job.getJobParameters().getString(BatchConstants.Projection.PARAMETERS_JSON);
		if (Strings.isNullOrEmpty(parametersJson)) {
			return Optional.empty();
		}
		try {
			return Optional.of(objectMapper.readValue(parametersJson, Parameters.class));
		} catch (IOException e) {
			logger.warn(
					"Unable to parse projection parameters for storage estimation on job {}: {}", job.getId(),
					e.getMessage()
			);
			return Optional.empty();
		}
	}

	private Optional<RangeSpan> resolveRangeSpan(Parameters parameters) {
		Integer ageIncrement = parseIntOrNull(parameters.getAgeIncrement());
		if (ageIncrement == null) {
			return Optional.empty();
		}

		Optional<RangeSpan> ageRange = rangeSpanFrom(parameters.getAgeStart(), parameters.getAgeEnd(), ageIncrement);
		if (ageRange.isPresent()) {
			return ageRange;
		}
		return rangeSpanFrom(parameters.getYearStart(), parameters.getYearEnd(), ageIncrement);
	}

	private Optional<RangeSpan> rangeSpanFrom(String startText, String endText, int ageIncrement) {
		Integer start = parseIntOrNull(startText);
		Integer end = parseIntOrNull(endText);
		if (start == null || end == null || end <= start) {
			return Optional.empty();
		}
		return Optional.of(new RangeSpan(end - start, ageIncrement));
	}

	private Integer parseIntOrNull(String value) {
		if (Strings.isNullOrEmpty(value)) {
			return null;
		}
		try {
			return Integer.valueOf(value.trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/**
	 * Tracks polygons completed so far plus one chunk in flight per active worker, so the estimate keeps pace with
	 * progress instead of lagging behind or pinning to the job's final total from the start.
	 */
	private int numPolygonsForOutput(JobExecution job) {
		int polygonsProcessed = job.getStepExecutions().stream()
				.filter(se -> se.getStepName().startsWith(BatchConstants.Job.WORKER_STEP_NAME))
				.mapToInt(se -> se.getExecutionContext().getInt(BatchConstants.Job.POLYGONS_PROCESSED, 0)).sum();
		int chunkSize = Math.max(batchProperties.getReader().getDefaultChunkSize(), 1);
		int inFlightPolygons = BatchUtils.calculateActiveWorkers(job, true) * chunkSize;

		int estimatedPolygons = polygonsProcessed + inFlightPolygons;
		if (estimatedPolygons > 0) {
			return estimatedPolygons;
		}
		return fallbackPolygonCount(job);
	}

	/**
	 * Input-partition files are wiped in one bulk pass (postProcessingStep) only after every worker finishes, so the
	 * full input stays until all polygons are processed or skipped - not a fraction per active worker.
	 */
	private int numPolygonsForInput(JobExecution job) {
		int totalPolygons = job.getExecutionContext().getInt(BatchConstants.Job.TOTAL_POLYGONS, 0);
		if (totalPolygons <= 0) {
			// Still downloading/partitioning - its total isn't known yet.
			return batchProperties.getStorage().getUnknownPolygonCountPlaceholder();
		}
		int polygonsAccountedFor = job.getStepExecutions().stream()
				.filter(se -> se.getStepName().startsWith(BatchConstants.Job.WORKER_STEP_NAME))
				.mapToInt(
						se -> se.getExecutionContext().getInt(BatchConstants.Job.POLYGONS_PROCESSED, 0)
								+ se.getExecutionContext().getInt(BatchConstants.Job.POLYGONS_SKIPPED, 0)
				).sum();
		return polygonsAccountedFor >= totalPolygons ? 0 : totalPolygons;
	}

	private int fallbackPolygonCount(JobExecution job) {
		// No progress/in-flight work yet - use the declared total so a just-started job isn't estimated at zero.
		int totalPolygons = job.getExecutionContext().getInt(BatchConstants.Job.TOTAL_POLYGONS, 0);
		if (totalPolygons > 0) {
			return totalPolygons;
		}
		// Total isn't known yet either (still downloading/partitioning) - use a placeholder so this isn't
		// mistaken for unexplained leftover usage while legitimately downloading its own input.
		return batchProperties.getStorage().getUnknownPolygonCountPlaceholder();
	}

	private StorageUsage readActualUsage() {
		try {
			Path root = Paths.get(batchProperties.getRootDirectory());
			if (!Files.exists(root)) {
				return new StorageUsage(0, 0);
			}
			FileStore store = Files.getFileStore(root);
			long total = store.getTotalSpace();
			long usable = store.getUsableSpace();
			return new StorageUsage(total - usable, total);
		} catch (IOException e) {
			logger.warn(
					"Unable to determine PVC storage usage for {}: {}", batchProperties.getRootDirectory(),
					e.getMessage()
			);
			return new StorageUsage(0, 0);
		}
	}

	public record StorageStatus(
			double percentFull, long usedBytes, long totalBytes, long expectedBytes, boolean outOfSpec,
			int thresholdPercent
	) {
	}

	private record StorageUsage(long usedBytes, long totalBytes) {
	}

	private record RangeSpan(int yearRange, int ageIncrement) {
	}
}
