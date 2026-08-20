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
	 * expectedBytes only accounts for currently running jobs. If none are running, any actual usage is unexplained
	 * (e.g. leftover files from a job that didn't clean up) and is flagged directly, since proving that possibility
	 * with the real numbers - not compensating for it in the calculation - is the point of this check.
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
			expectedBytes += perPolygonBytesForJob(job) * numPolygonsForJob(job);
		}
		return expectedBytes;
	}

	private long perPolygonBytesForJob(JobExecution job) {
		BatchProperties.StorageProperties storage = batchProperties.getStorage();
		Optional<Parameters> parameters = parseParameters(job);

		RangeSpan rangeSpan = parameters.flatMap(this::resolveRangeSpan)
				.orElseGet(() -> new RangeSpan(storage.getFallbackYearRange(), storage.getFallbackAgeIncrement()));
		long increments = rangeSpan.ageIncrement() <= 0 ? 0 : rangeSpan.yearRange() / rangeSpan.ageIncrement();

		long bytes = storage.getBytesPerCompleteLine() * increments;
		if (parameters.map(p -> hasExecutionOption(p, ExecutionOption.DO_ENABLE_ERROR_LOGGING)).orElse(false)) {
			bytes += storage.getReasonableErrorBytesPerPolygon();
		}
		if (parameters.map(p -> hasExecutionOption(p, ExecutionOption.DO_ENABLE_DEBUG_LOGGING)).orElse(false)) {
			bytes += storage.getOptionalDebugLogBytesPerPolygon();
		}
		return bytes;
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

	private int numPolygonsForJob(JobExecution job) {
		int totalPolygons = job.getExecutionContext().getInt(BatchConstants.Job.TOTAL_POLYGONS, 0);
		if (totalPolygons > 0) {
			return totalPolygons;
		}
		return job.getStepExecutions().stream()
				.filter(se -> se.getStepName().startsWith(BatchConstants.Job.WORKER_STEP_NAME))
				.mapToInt(se -> se.getExecutionContext().getInt(BatchConstants.Job.POLYGONS_PROCESSED, 0)).sum();
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
