package ca.bc.gov.nrs.vdyp.batch.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Job execution listener for partitioned VDYP batch job.
 */
@Component
public class PartitionedJobExecutionListener implements JobExecutionListener {

	private static final Logger logger = LoggerFactory.getLogger(PartitionedJobExecutionListener.class);

	@Autowired
	private BatchProperties batchProperties;

	// Thread safety for afterJob execution - using job execution ID as key
	private final ConcurrentHashMap<Long, Boolean> jobCompletionTracker = new ConcurrentHashMap<>();
	private final Object lock = new Object();

	@Override
	public void beforeJob(@NonNull JobExecution jobExecution) {
		// Initialize tracking for this job execution
		jobCompletionTracker.put(jobExecution.getId(), false);

		String separator = "============================================================";
		logger.info(separator);
		logger.info("VDYP PARTITIONED JOB STARTING");

		Long partitionSize = jobExecution.getJobParameters().getLong("partitionSize");
		Long chunkSize = jobExecution.getJobParameters().getLong("chunkSize");

		int actualPartitionSize;
		if (partitionSize != null) {
			actualPartitionSize = partitionSize.intValue();
		} else if (batchProperties.getPartitioning().getGridSize() > 0) {
			actualPartitionSize = batchProperties.getPartitioning().getGridSize();
		} else {
			throw new IllegalStateException(
					"batch.partitioning.grid-size must be configured in application.properties"
			);
		}

		int actualChunkSize;
		if (chunkSize != null) {
			actualChunkSize = chunkSize.intValue();
		} else if (batchProperties.getPartitioning().getChunkSize() > 0) {
			actualChunkSize = batchProperties.getPartitioning().getChunkSize();
		} else {
			throw new IllegalStateException(
					"batch.partitioning.chunk-size must be configured in application.properties"
			);
		}

		logger.info("VDYP Grid Size: {}", actualPartitionSize);
		logger.info("VDYP Chunk Size: {}", actualChunkSize);
		logger.info("Expected Partitions: {}", actualPartitionSize);
		logger.info("Job Execution ID: {}", jobExecution.getId());
		logger.info(separator);
	}

	@Override
	public void afterJob(@NonNull JobExecution jobExecution) {
		Long jobExecutionId = jobExecution.getId();

		// Check if this specific job execution has already been processed
		Boolean alreadyProcessed = jobCompletionTracker.get(jobExecutionId);
		if (alreadyProcessed == null || alreadyProcessed) {
			logger.info("VDYP Job {} already processed or not tracked, skipping afterJob processing", jobExecutionId);
			return;
		}

		// Use synchronization to ensure only one thread processes this job completion
		synchronized (lock) {
			// Double-check after acquiring lock
			if (jobCompletionTracker.get(jobExecutionId)) {
				logger.info("VDYP Job {} already processed by another thread, skipping", jobExecutionId);
				return;
			}

			// Mark this job as processed
			jobCompletionTracker.put(jobExecutionId, true);

			String separator = "============================================================";
			logger.info(separator);
			logger.info("VDYP PARTITIONED JOB COMPLETED");
			logger.info("Job Execution ID: {}", jobExecutionId);
			logger.info("Status: {}", jobExecution.getStatus());

			LocalDateTime startTime = jobExecution.getStartTime();
			LocalDateTime endTime = jobExecution.getEndTime();
			if (startTime != null && endTime != null) {
				Duration duration = Duration.between(startTime, endTime);
				long millis = duration.toMillis();
				logger.info("Duration: {}ms", millis);
			} else {
				logger.warn("Duration: Unable to calculate (missing time information)");
			}

			// Merge partition files
			try {
				Long partitionSize = jobExecution.getJobParameters().getLong("partitionSize");
				String outputDirectory = jobExecution.getJobParameters().getString("outputFilePath");

				String actualOutputDirectory = outputDirectory;
				if (actualOutputDirectory == null) {
					actualOutputDirectory = batchProperties.getOutput().getDirectory().getDefaultPath();
				}
				if (actualOutputDirectory == null) {
					actualOutputDirectory = System.getProperty("java.io.tmpdir");
					logger.warn(
							"No output directory specified, using system temp directory: {}", actualOutputDirectory
					);
				}

				int actualPartitionSize;
				if (partitionSize != null) {
					actualPartitionSize = partitionSize.intValue();
				} else if (batchProperties.getPartitioning().getGridSize() > 0) {
					actualPartitionSize = batchProperties.getPartitioning().getGridSize();
				} else {
					throw new IllegalStateException(
							"batch.partitioning.grid-size must be configured in application.properties"
					);
				}
				mergePartitionFiles(actualPartitionSize, jobExecutionId, actualOutputDirectory);
			} catch (Exception e) {
				logger.error("Failed to merge VDYP partition files: {}", e.getMessage());
				e.printStackTrace();
			}

			logger.info(separator);
		}

		cleanupOldJobTracker(jobExecutionId);
	}

	/**
	 * Cleans up old job execution tracking to prevent memory leaks.
	 */
	private void cleanupOldJobTracker(Long currentJobId) {
		if (jobCompletionTracker.size() > 10) {
			jobCompletionTracker.entrySet().removeIf(entry -> entry.getKey() < currentJobId - 5);
		}
	}

	/**
	 * Merges all VDYP partition output files into a single file.
	 */
	private void mergePartitionFiles(int partitionCount, Long jobExecutionId, String outputDirectory) throws Exception {
		String filePrefix = batchProperties.getOutput().getFilePrefix();
		if (filePrefix == null) {
			throw new IllegalStateException("batch.output.file-prefix must be configured in application.properties");
		}

		String csvHeader = batchProperties.getOutput().getCsvHeader();
		if (csvHeader == null || csvHeader.trim().isEmpty()) {
			throw new IllegalStateException("batch.output.csv-header must be configured in application.properties");
		}

		String finalOutputPath = outputDirectory + "/" + filePrefix + "_merged.csv";

		// Add job execution ID to avoid conflicts in concurrent executions
		String tempMergeFile = outputDirectory + "/" + filePrefix + "_merged_temp_" + jobExecutionId + ".csv";

		logger.info("Starting VDYP file merge for {} partitions...", partitionCount);

		try (java.io.BufferedWriter writer = Files.newBufferedWriter(Paths.get(tempMergeFile))) {
			// Write VDYP header
			writer.write(csvHeader);
			writer.newLine();

			int mergedFiles = 0;
			long totalLines = 0;

			// Merge partition files
			for (int i = 0; i < partitionCount; i++) {
				String partitionFile = outputDirectory + "/" + filePrefix + "_partition" + i + ".csv";
				if (Files.exists(Paths.get(partitionFile))) {
					long partitionLines = Files.lines(Paths.get(partitionFile)).skip(1) // Skip header
							.peek(line -> {
								try {
									writer.write(line);
									writer.newLine();
								} catch (Exception e) {
									logger.error("Error writing VDYP line: {}", e.getMessage());
								}
							}).count();

					totalLines += partitionLines;
					mergedFiles++;
					logger.info("Merged VDYP partition file: {} ({} records)", partitionFile, partitionLines);
				} else {
					logger.warn("VDYP partition file not found: {}", partitionFile);
				}
			}

			logger.info("Merged {} VDYP partition files with total {} data records", mergedFiles, totalLines);
		}

		// Atomically move temp file to final location
		Files.move(
				Paths.get(tempMergeFile), Paths.get(finalOutputPath), java.nio.file.StandardCopyOption.REPLACE_EXISTING
		);

		logger.info("Final merged VDYP output created: {}", finalOutputPath);
		long lineCount = Files.lines(Paths.get(finalOutputPath)).count();
		logger.info("Total lines in merged VDYP file: {} (including header)", lineCount);
	}
}