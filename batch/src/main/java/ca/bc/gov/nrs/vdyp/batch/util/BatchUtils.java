package ca.bc.gov.nrs.vdyp.batch.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public final class BatchUtils {

	private BatchUtils() {
		//
	}

	// MDJ: Normally this would be private, and all uses of it would be isolated in this
	// class.	
	public static final DateTimeFormatter dateTimeFormatterForFilenames = DateTimeFormatter
			.ofPattern("yyyy_MM_dd_HH_mm_ss_SSSS");

	public static String createJobFolderName(String prefix, String timestamp) {
		return String.format("%s-%s", prefix, timestamp);
	}

	public static String createJobTimestamp() {
		return dateTimeFormatterForFilenames.format(LocalDateTime.now());
	}

	public static String createJobGuid() {
		return UUID.randomUUID().toString();
	}

	/**
	 * Builds the input partition folder name.
	 *
	 * @param partitionName the partition name (e.g., "partition0")
	 * @return the input partition folder name (e.g., "input-partition0")
	 */
	public static String buildInputPartitionFolderName(String partitionName) {
		return BatchConstants.Partition.INPUT_PREFIX + "-" + partitionName;
	}

	/**
	 * Builds the output partition folder name.
	 *
	 * @param partitionName the partition name (e.g., "partition0")
	 * @return the output partition folder name (e.g., "output-partition0")
	 */
	public static String buildOutputPartitionFolderName(String partitionName) {
		return BatchConstants.Partition.OUTPUT_PREFIX + "-" + partitionName;
	}
}
