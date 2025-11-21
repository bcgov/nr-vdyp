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
}
