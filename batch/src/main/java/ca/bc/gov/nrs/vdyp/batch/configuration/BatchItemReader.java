package ca.bc.gov.nrs.vdyp.batch.configuration;

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

	// Job base directory
	private String jobBaseDir;

	// Byte offset metadata for efficient streaming
	private BatchUtils.RecordByteOffsets polygonOffsets;
	private BatchUtils.RecordByteOffsets layerOffsets;

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
		int totalRecords = polygonOffsets.getTotalRecords();
		if (currentIndex >= totalRecords) {
			logger.trace(
					"[GUID: {}, EXEID: {}, Partition: {}] All records processed (currentIndex={}, totalRecords={})",
					jobGuid, jobExecutionId, partitionName, currentIndex, totalRecords
			);
			return null; // End of data - signals Spring Batch that reading is complete
		}

		// Calculate how many records to include in this chunk
		int remainingRecords = totalRecords - currentIndex;
		int recordsInThisChunk = Math.min(chunkSize, remainingRecords);

		// Calculate byte offsets for polygon file
		long polygonStartByte = BatchUtils.getChunkStartByte(polygonOffsets, currentIndex);

		// Calculate byte offsets for layer file by finding matching FEATURE_IDs
		BatchUtils.LayerByteRange layerRange = calculateLayerByteRange(currentIndex, recordsInThisChunk);

		BatchChunkMetadata metadata = new BatchChunkMetadata(
				partitionName, jobBaseDir, polygonStartByte, recordsInThisChunk, layerRange.getStartByte(),
				layerRange.getRecordCount()
		);

		logger.trace(
				"[GUID: {}, EXEID: {}, Partition: {}] Created chunk metadata: polygonStartByte={}, polygonRecordCount={}, layerStartByte={}, layerRecordCount={}, progress={}/{}",
				jobGuid, jobExecutionId, partitionName, polygonStartByte, recordsInThisChunk, layerRange.getStartByte(),
				layerRange.getRecordCount(), currentIndex + recordsInThisChunk, totalRecords
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
			Path partitionDir = Paths.get(this.jobBaseDir, inputPartitionFolderName);
			if (!Files.exists(partitionDir)) {
				BatchDataReadException dataReadException = BatchDataReadException.handleDataReadFailure(
						new FileNotFoundException("Partition directory does not exist: " + partitionDir),
						"Partition directory does not exist", jobGuid, jobExecutionId, partitionName, logger
				);
				throw new ItemStreamException(dataReadException.getMessage(), dataReadException);
			}

			logger.trace(
					"[GUID: {}, EXEID: {}, Partition: {}] Reading from partition directory: {}", jobGuid,
					jobExecutionId, partitionName, partitionDir
			);

			// Calculate byte offsets for polygon and layer files
			Path polygonFilePath = partitionDir.resolve(BatchConstants.Partition.INPUT_POLYGON_FILE_NAME);
			Path layerFilePath = partitionDir.resolve(BatchConstants.Partition.INPUT_LAYER_FILE_NAME);

			this.polygonOffsets = BatchUtils.calculateRecordByteOffsets(polygonFilePath);
			this.layerOffsets = BatchUtils.calculateRecordByteOffsets(layerFilePath);

			readerOpened = true;
			logger.trace(
					"[GUID: {}, EXEID: {}, Partition: {}] BatchItemReader opened successfully. Total polygon records: {}, total layer records: {}",
					jobGuid, jobExecutionId, partitionName, polygonOffsets.getTotalRecords(),
					layerOffsets.getTotalRecords()
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
				"[GUID: {}, EXEID: {}, Partition: {}] Closing BatchItemReader. Total polygon records: {}", jobGuid,
				jobExecutionId, partitionName, polygonOffsets != null ? polygonOffsets.getTotalRecords() : 0
		);

		readerOpened = false;
	}

	/**
	 * Calculates the byte range for layer records that match the FEATURE_IDs from a polygon chunk.
	 *
	 * @param polygonStartIndex  The starting record index in the polygon file
	 * @param polygonRecordCount The number of polygon records in the chunk
	 * @return LayerByteRange containing start byte and record count for matching layer records
	 */
	private BatchUtils.LayerByteRange calculateLayerByteRange(int polygonStartIndex, int polygonRecordCount) {
		return BatchUtils.calculateLayerByteRange(polygonOffsets, polygonStartIndex, polygonRecordCount, layerOffsets);
	}
}
