package ca.bc.gov.nrs.vdyp.batch.configuration;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.lang.NonNull;

import ca.bc.gov.nrs.vdyp.batch.exception.BatchDataReadException;
import ca.bc.gov.nrs.vdyp.batch.model.BatchChunkMetadata;
import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;
import ca.bc.gov.nrs.vdyp.batch.util.BatchUtils;

/**
 * Memory-efficient ItemReader that returns chunk metadata instead of loading entire CSV data. The actual file streaming
 * is performed in the ItemWriter.
 */
public class BatchItemReader implements ItemStreamReader<BatchChunkMetadata> {

	private static final Logger logger = LoggerFactory.getLogger(BatchItemReader.class);

	@NonNull
	private final String partitionName;
	@NonNull
	private final Long jobExecutionId;
	@NonNull
	private final String jobGuid;
	private final int chunkSize;

	// Partition directory and job base directory
	private String jobBaseDir;
	private Path partitionDir;

	// Total record count for this partition
	private int totalRecords = 0;

	// Current processing state
	private int currentIndex = 0;
	private boolean readerOpened = false;

	public BatchItemReader(
			@NonNull String partitionName, @NonNull Long jobExecutionId, @NonNull String jobGuid, @NonNull int chunkSize
	) {
		this.partitionName = partitionName;
		this.jobExecutionId = jobExecutionId;
		this.jobGuid = jobGuid;
		this.chunkSize = Math.max(chunkSize, 1);
	}

	/**
	 * Reads the next ChunkMetadata for processing.
	 *
	 * @return The next ChunkMetadata to process, or null when all data has been read. Note: Returning null signals
	 *         end-of-data to Spring Batch framework - this is the standard contract.
	 * @throws BatchDataReadException if reading fails
	 */
	@Override
	public BatchChunkMetadata read() throws BatchDataReadException {
		if (!readerOpened) {
			throw BatchDataReadException.handleDataReadFailure(
					new IllegalStateException("Reader not opened. Call open() first."),
					"Reader not opened. Call open() first.", jobGuid, jobExecutionId, partitionName, logger
			);
		}

		// Check if we've read all records
		if (currentIndex >= totalRecords) {
			logger.debug(
					"[GUID: {}, EXEID: {}, Partition: {}] All records processed (currentIndex={}, totalRecords={})",
					jobGuid, jobExecutionId, partitionName, currentIndex, totalRecords
			);
			return null; // End of data - signals Spring Batch that reading is complete
		}

		// Calculate how many records to include in this chunk
		int remainingRecords = totalRecords - currentIndex;
		int recordsInThisChunk = Math.min(chunkSize, remainingRecords);

		BatchChunkMetadata metadata = new BatchChunkMetadata(
				partitionName, jobBaseDir, currentIndex, recordsInThisChunk
		);

		logger.trace(
				"[GUID: {}, EXEID: {}, Partition: {}] Created chunk metadata: startIndex={}, recordCount={}, progress={}/{}",
				jobGuid, jobExecutionId, partitionName, currentIndex, recordsInThisChunk,
				currentIndex + recordsInThisChunk, totalRecords
		);

		// Advance current index for next read
		currentIndex += recordsInThisChunk;

		return metadata;
	}

	/**
	 * @throws ItemStreamException Required by Spring Batch ItemStreamReader interface contract.
	 */
	@Override
	public void open(@NonNull ExecutionContext executionContext) throws ItemStreamException {
		logger.debug(
				"[GUID: {}, EXEID: {}, Partition: {}] Opening BatchItemReader with chunk size: {}", jobGuid,
				jobExecutionId, partitionName, chunkSize
		);

		try {
			// Get partition directory from job parameters
			this.jobBaseDir = executionContext.getString(BatchConstants.Job.BASE_DIR);
			if (this.jobBaseDir.trim().isEmpty()) {
				BatchDataReadException dataReadException = BatchDataReadException.handleDataReadFailure(
						new IllegalArgumentException("jobBaseDir is empty"), "jobBaseDir is empty in ExecutionContext",
						jobGuid, jobExecutionId, partitionName, logger
				);
				throw new ItemStreamException(dataReadException.getMessage(), dataReadException);
			}

			String inputPartitionFolderName = BatchUtils.buildInputPartitionFolderName(partitionName);
			this.partitionDir = Paths.get(this.jobBaseDir, inputPartitionFolderName);
			if (!Files.exists(this.partitionDir)) {
				BatchDataReadException dataReadException = BatchDataReadException.handleDataReadFailure(
						new FileNotFoundException("Partition directory does not exist: " + this.partitionDir),
						"Partition directory does not exist", jobGuid, jobExecutionId, partitionName, logger
				);
				throw new ItemStreamException(dataReadException.getMessage(), dataReadException);
			}

			logger.debug(
					"[GUID: {}, EXEID: {}, Partition: {}] Reading from partition directory: {}", jobGuid,
					jobExecutionId, partitionName, this.partitionDir
			);

			// Count total records in polygon file
			this.totalRecords = countTotalRecords();

			readerOpened = true;
			logger.debug(
					"[GUID: {}, EXEID: {}, Partition: {}] BatchItemReader opened successfully. Total records: {}",
					jobGuid, jobExecutionId, partitionName, totalRecords
			);

		} catch (IOException e) {
			BatchDataReadException dataReadException = BatchDataReadException.handleDataReadFailure(
					e, "Failed to initialize partition reader", jobGuid, jobExecutionId, partitionName, logger
			);
			throw new ItemStreamException(dataReadException.getMessage(), dataReadException);
		}
	}

	@Override
	public void update(@NonNull ExecutionContext executionContext) {
		// No state to persist
	}

	/**
	 * @throws ItemStreamException Required by Spring Batch ItemStreamReader interface contract.
	 */
	@Override
	public void close() throws ItemStreamException {
		logger.debug(
				"[GUID: {}, EXEID: {}, Partition: {}] Closing BatchItemReader. Total records: {}", jobGuid,
				jobExecutionId, partitionName, totalRecords
		);

		readerOpened = false;
	}

	/**
	 * Count total data records in polygon file (excluding headers).
	 *
	 * @return Total number of data records
	 * @throws IOException if file reading fails
	 */
	private int countTotalRecords() throws IOException {
		Path polygonFile = partitionDir.resolve(BatchConstants.Partition.INPUT_POLYGON_FILE_NAME);

		if (!Files.exists(polygonFile)) {
			throw new FileNotFoundException("Polygon file not found: " + polygonFile);
		}

		int count;
		try (BufferedReader reader = Files.newBufferedReader(polygonFile)) {
			count = BatchUtils.countDataRecords(reader);
		}

		logger.debug(
				"[GUID: {}, EXEID: {}, Partition: {}] Counted {} total records in polygon file", jobGuid,
				jobExecutionId, partitionName, count
		);

		return count;
	}
}
