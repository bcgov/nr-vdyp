package ca.bc.gov.nrs.vdyp.batch.exception;

import java.io.IOException;
import java.nio.file.Path;

import org.slf4j.Logger;

import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;

// FIXME VDYP-839
public class BatchIOException extends IOException {

	private static final long serialVersionUID = 2563311795099971052L;

	public BatchIOException(String message) {
		super(message);
	}

	public BatchIOException(String message, Throwable cause) {
		super(message, cause);
	}

	public BatchIOException(Throwable cause) {
		super(cause);
	}

	public static IOException
			handleIOException(Object context, IOException cause, String errorDescription, Logger logger) {
		String rootCause = cause.getMessage() != null ? cause.getMessage()
				: BatchConstants.ErrorMessage.NO_ERROR_MESSAGE;
		String exceptionType = cause.getClass().getSimpleName();

		String contextualMessage = context != null ? String.format(
				"%s: %s. Exception type: %s, Root cause: %s", errorDescription, context, exceptionType, rootCause
		) : String.format("%s. Exception type: %s, Root cause: %s", errorDescription, exceptionType, rootCause);

		logger.error(contextualMessage, cause);
		return new IOException(contextualMessage, cause);
	}

	public static IOException
			handleDirectoryWalkFailure(Path directory, IOException cause, String errorDescription, Logger logger) {
		return handleIOException(directory, cause, errorDescription, logger);
	}

	public static IOException
			handleFileReadFailure(Path filePath, IOException cause, String errorDescription, Logger logger) {
		return handleIOException(filePath, cause, errorDescription, logger);
	}

	public static IOException
			handleFileWriteFailure(Path filePath, IOException cause, String errorDescription, Logger logger) {
		return handleIOException(filePath, cause, errorDescription, logger);
	}

	public static IOException
			handleFileCopyFailure(Path filePath, IOException cause, String errorDescription, Logger logger) {
		return handleIOException(filePath, cause, errorDescription, logger);
	}

	public static IOException
			handleStreamFailure(String streamName, IOException cause, String errorDescription, Logger logger) {
		return handleIOException(streamName, cause, errorDescription, logger);
	}
}
