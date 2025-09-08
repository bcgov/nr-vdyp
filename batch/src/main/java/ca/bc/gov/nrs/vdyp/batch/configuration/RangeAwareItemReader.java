package ca.bc.gov.nrs.vdyp.batch.configuration;

import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.lang.NonNull;

import ca.bc.gov.nrs.vdyp.batch.model.BatchRecord;
import ca.bc.gov.nrs.vdyp.batch.service.BatchMetricsCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ItemReader that reads only records within a specified line range. Used for partitioned processing to ensure each
 * partition processes only its assigned range of data lines.
 */
public class RangeAwareItemReader implements ItemReader<BatchRecord>, ItemStream {

	private static final Logger logger = LoggerFactory.getLogger(RangeAwareItemReader.class);

	private FlatFileItemReader<BatchRecord> delegate;
	private long startLine;
	private long endLine;
	private String partitionName;
	private long processedCount = 0;
	private long skippedCount = 0;
	private long currentLine = 0;
	private boolean readerOpened = false;
	private boolean rangeExhausted = false;

	// Job execution context for metrics
	private Long jobExecutionId;

	// Metrics collector for skip tracking
	@Autowired
	private BatchMetricsCollector metricsCollector;

	@Autowired
	private BatchProperties batchProperties;

	// Skip tracking and statistics
	private final AtomicLong totalSkipsInReader = new AtomicLong(0);
	private final ConcurrentHashMap<String, AtomicLong> skipReasonCounts = new ConcurrentHashMap<>();

	private Resource inputResource;

	public RangeAwareItemReader(Resource resource) {
		this.inputResource = resource;
	}

	public void setInputResource(Resource resource) {
		this.inputResource = resource;
	}

	/**
	 * Extracts partition parameters from StepExecution context before step starts.
	 */
	@BeforeStep
	public void beforeStep(StepExecution stepExecution) {
		this.startLine = stepExecution.getExecutionContext().getLong("startLine", 2);
		this.endLine = stepExecution.getExecutionContext().getLong("endLine", Long.MAX_VALUE);
		this.partitionName = stepExecution.getExecutionContext().getString("partitionName", "unknown");
		this.jobExecutionId = stepExecution.getJobExecutionId();

		// Initialize current line tracker
		this.currentLine = 1; // Start at header line

		String inputFilePath = stepExecution.getJobExecution().getJobParameters().getString("inputFilePath");
		if (inputFilePath == null || inputFilePath.trim().isEmpty()) {
			inputFilePath = batchProperties.getInput().getFilePath();
		}
		if (inputFilePath == null || inputFilePath.trim().isEmpty()) {
			throw new IllegalStateException(
					"No input file path specified in job parameters or properties. Cannot initialize reader for partition: "
							+ partitionName
			);
		}

		// Create resource from file path
		if (inputFilePath.startsWith("classpath:")) {
			this.inputResource = new org.springframework.core.io.ClassPathResource(inputFilePath.substring(10));
		} else {
			this.inputResource = new org.springframework.core.io.FileSystemResource(inputFilePath);
		}

		// Check if the resource actually exists
		if (!inputResource.exists()) {
			throw new IllegalStateException(
					"VDYP input resource does not exist: " + inputFilePath
							+ ". Cannot initialize reader for partition: " + partitionName
			);
		}

		// Create a new, independent delegate reader for this VDYP partition
		String uniqueReaderName = "VdypRangeAwareItemReader-" + partitionName + "-" + System.currentTimeMillis();
		this.delegate = new FlatFileItemReaderBuilder<BatchRecord>().name(uniqueReaderName).resource(inputResource)
				.delimited().names("id", "data", "polygonId", "layerId").linesToSkip(1) // Skip header
				.targetType(BatchRecord.class).build();

		// Calculate dynamic logging intervals based on partition size
		long partitionSize = endLine - startLine + 1;

		logger.info(
				"[{}] VDYP Reader initialized with line range: {} - {} (size: {})", partitionName, startLine, endLine,
				partitionSize
		);

	}

	@Override
	public void open(@NonNull ExecutionContext executionContext) throws ItemStreamException {
		if (!readerOpened) {
			delegate.open(executionContext);
			readerOpened = true;
			logger.info(
					"[{}] VDYP Reader opened successfully for line range {}-{} (total range: {} lines)", partitionName,
					startLine, endLine, (endLine - startLine + 1)
			);
		}
	}

	@Override
	public void update(@NonNull ExecutionContext executionContext) throws ItemStreamException {
		delegate.update(executionContext);
	}

	@Override
	public void close() throws ItemStreamException {
		if (readerOpened) {
			delegate.close();
			readerOpened = false;

			long totalReaderSkips = totalSkipsInReader.get();
			logger.info("[{}] VDYP Reader closed. Final statistics:", partitionName);
			logger.info("  - VDYP records processed: {}", processedCount);
			logger.info("  - Partition boundary skips: {}", skippedCount);
			logger.info("  - Data quality skips: {}", totalReaderSkips);
			logger.info("  - Total VDYP records examined: {}", processedCount + skippedCount + totalReaderSkips);

			if (totalReaderSkips > 0) {
				logger.info("[{}] VDYP data quality skip breakdown:", partitionName);
				skipReasonCounts.forEach((reason, count) -> logger.info("  - {}: {}", reason, count.get()));
			}
		}
	}

	/**
	 * Reads the next BatchRecord that falls within the partition's line range.
	 */
	@Override
	public BatchRecord read() throws Exception {
		if (!readerOpened) {
			open(new ExecutionContext());
		}

		if (rangeExhausted) {
			return null;
		}

		BatchRecord batchRecord;

		while (true) {
			try {
				batchRecord = delegate.read();
				currentLine++; // Track current line position

				if (batchRecord == null) {
					logger.info("[{}] End of VDYP file reached at line {}", partitionName, currentLine - 1);
					rangeExhausted = true;
					logFinalStatistics();
					return null;
				}

				// Check if haven't reached start line yet
				if (currentLine < startLine) {
					skippedCount++;
					continue;
				}

				// Check if passed end line
				if (currentLine > endLine) {
					if (!rangeExhausted) {
						rangeExhausted = true;
						logger.info(
								"[{}] Reached end of VDYP partition line range at line {}. Stopping reading.",
								partitionName, currentLine
						);
						logFinalStatistics();
					}
					return null;
				}

				// within line range - validate and process data record
				BatchRecord processedRecord = processVdypRecord(batchRecord);

				if (processedRecord == null) {
					continue; // Continue reading next record (validation issues)
				}

				return processedRecord;

			} catch (FlatFileParseException e) {
				handleSkipEvent(e, "VDYP_FILE_PARSE_ERROR", (long) currentLine);
				continue;
			} catch (Exception e) {
				handleSkipEvent(e, "VDYP_READER_ERROR", (long) currentLine);
				continue;
			}
		}
	}

	/**
	 * Process a successfully read data record, applying data validation.
	 */
	private BatchRecord processVdypRecord(BatchRecord batchRecord) throws Exception {
		Long recordId = batchRecord.getId();

		if (recordId == null) {
			handleDataQualitySkip(batchRecord, "NULL_ID", "VDYP record has null ID");
			return null;
		}

		// Validate record data quality
		validateVdypRecordData(batchRecord);

		// Record is within line range and valid
		processedCount++;

		// Log first data record found in partition range
		if (processedCount == 1) {
			logger.info(
					"[{}] Found first VDYP record in partition range: line {}, ID {}", partitionName, currentLine,
					recordId
			);
		}

		return batchRecord;
	}

	/**
	 * Validate record data quality and handle data-related skip events.
	 */
	private void validateVdypRecordData(BatchRecord batchRecord) throws Exception {
		Long recordId = batchRecord.getId();

		if (batchRecord.getData() == null || batchRecord.getData().trim().isEmpty()) {
			handleDataQualitySkip(batchRecord, "MISSING_VDYP_DATA", "VDYP data field is missing or empty");
			throw new IllegalArgumentException("Missing required VDYP data field for record ID " + recordId);
		}

		if (batchRecord.getPolygonId() == null || batchRecord.getPolygonId().trim().isEmpty()) {
			handleDataQualitySkip(batchRecord, "MISSING_POLYGON_ID", "Polygon ID is missing or empty");
			throw new IllegalArgumentException("Missing required polygon ID for record ID " + recordId);
		}

		if (batchRecord.getLayerId() == null || batchRecord.getLayerId().trim().isEmpty()) {
			handleDataQualitySkip(batchRecord, "MISSING_LAYER_ID", "Layer ID is missing or empty");
			throw new IllegalArgumentException("Missing required layer ID for record ID " + recordId);
		}
	}

	/**
	 * Handle skip events from file parsing errors.
	 */
	private void handleSkipEvent(Exception exception, String skipReason, Long lineNumber) {
		totalSkipsInReader.incrementAndGet();
		skipReasonCounts.computeIfAbsent(skipReason, k -> new AtomicLong(0)).incrementAndGet();

		if (metricsCollector != null && jobExecutionId != null) {
			BatchRecord errorRecord = new BatchRecord();
			if (lineNumber != null) {
				errorRecord.setId(lineNumber);
			}

			metricsCollector
					.recordSkip(jobExecutionId, errorRecord.getId(), errorRecord, exception, partitionName, lineNumber);
		}

		logger.warn(
				"[{}] VDYP Skip event: {} at line {} - {}", partitionName, skipReason,
				lineNumber != null ? lineNumber.toString() : "unknown", exception.getMessage()
		);
	}

	/**
	 * Handle skip events from VDYP data quality issues.
	 */
	private void handleDataQualitySkip(BatchRecord batchRecord, String skipReason, String description) {
		totalSkipsInReader.incrementAndGet();
		skipReasonCounts.computeIfAbsent(skipReason, k -> new AtomicLong(0)).incrementAndGet();

		if (batchRecord != null) {
			BatchSkipPolicy.cacheRecordData(batchRecord.getId(), batchRecord, Thread.currentThread().getName());
		}

		if (metricsCollector != null && jobExecutionId != null && batchRecord != null) {
			IllegalArgumentException dataQualityException = new IllegalArgumentException(description);
			Long lineNumber = batchRecord.getId() != null ? batchRecord.getId() + 1 : null;

			metricsCollector.recordSkip(
					jobExecutionId, batchRecord.getId(), batchRecord, dataQualityException, partitionName, lineNumber
			);
		}

		logger.warn(
				"[{}] VDYP Data quality skip: {} for record ID {} - {}", partitionName, skipReason,
				batchRecord != null ? batchRecord.getId() : "unknown", description
		);
	}

	/**
	 * Log final statistics when reading is complete.
	 */
	private void logFinalStatistics() {
		long totalReaderSkips = totalSkipsInReader.get();
		logger.info(
				"[{}] VDYP Reader completed. Processed: {}, Partition boundary skips: {}, Data quality skips: {}",
				partitionName, processedCount, skippedCount, totalReaderSkips
		);

		if (totalReaderSkips > 0) {
			logger.info("[{}] VDYP Skip breakdown by reason:", partitionName);
			skipReasonCounts.forEach((reason, count) -> logger.info("  - {}: {}", reason, count.get()));
		}
	}

	public ConcurrentHashMap<String, AtomicLong> getSkipStatistics() {
		return new ConcurrentHashMap<>(skipReasonCounts);
	}

	public long getTotalDataSkips() {
		return totalSkipsInReader.get();
	}

	public long getTotalRangeSkips() {
		return skippedCount;
	}

	public long getTotalProcessed() {
		return processedCount;
	}

	public String getPartitionName() {
		return partitionName;
	}
}