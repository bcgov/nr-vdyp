package ca.bc.gov.nrs.vdyp.batch.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.springframework.lang.NonNull;

/**
 * InputStream that reads a specific range of records from a CSV file using FileChannel for efficient streaming. This
 * stream uses byte offsets to jump directly to the required data without loading entire chunks into memory.
 *
 * Two modes of operation: - Polygon mode: Reads records by byte offset (startByte, recordCount) - Layer mode: Reads ALL
 * records matching a set of FEATURE_IDs (requires sorted input)
 *
 * Assumption: Input CSV files are sorted by FEATURE_ID for efficient layer filtering.
 */
public class BatchRangeInputStream extends InputStream {

	private final InputStream delegate;
	private final FileChannel channel;
	private boolean closed = false;

	private BatchRangeInputStream(InputStream delegate, FileChannel channel) {
		this.delegate = delegate;
		this.channel = channel;
	}

	/**
	 * Creates an InputStream that reads a specific range of records from a CSV file using FileChannel. This method
	 * reads the specified number of records starting from the given byte offset.
	 *
	 * Note: This method does NOT perform header detection or skipping. The caller (BatchItemReader) is responsible for
	 * calculating byte offsets that point to data records only, excluding any headers.
	 *
	 * @param filePath    The path to the CSV file
	 * @param startByte   The byte offset where the chunk starts (should point to a data record, not a header)
	 * @param recordCount The number of records to read
	 * @return An InputStream containing the specified range of records
	 * @throws IOException if file reading fails
	 */
	public static BatchRangeInputStream create(@NonNull Path filePath, long startByte, int recordCount)
			throws IOException {
		if (startByte < 0) {
			throw new IllegalArgumentException("Start byte must be non-negative, got: " + startByte);
		}
		if (recordCount <= 0) {
			throw new IllegalArgumentException("Record count must be positive, got: " + recordCount);
		}

		// Open FileChannel and position it at startByte
		FileChannel channel = null;
		try {
			channel = FileChannel.open(filePath, StandardOpenOption.READ);
			channel.position(startByte);

			// Create a buffered stream that reads from the positioned channel
			InputStream channelStream = new RecordLimitedInputStream(Channels.newInputStream(channel), recordCount);

			return new BatchRangeInputStream(channelStream, channel);
		} catch (IOException e) {
			if (channel != null) {
				try {
					channel.close();
				} catch (IOException closeException) {
					e.addSuppressed(closeException);
				}
			}
			throw e;
		}
	}

	/**
	 * Inner class that wraps an InputStream and limits it to reading a specific number of complete lines (records).
	 */
	private static class RecordLimitedInputStream extends InputStream {
		private final BufferedReader reader;
		private final int maxRecords;
		private int recordsRead = 0;
		private byte[] currentLineBytes = null;
		private int currentLinePos = 0;
		private boolean endOfRecords = false;

		RecordLimitedInputStream(InputStream source, int maxRecords) {
			this.reader = new BufferedReader(new InputStreamReader(source, StandardCharsets.UTF_8));
			this.maxRecords = maxRecords;
		}

		@Override
		public int read() throws IOException {
			if (endOfRecords) {
				return -1;
			}

			// Load next line if needed
			if (currentLineBytes == null || currentLinePos >= currentLineBytes.length) {
				if (recordsRead >= maxRecords) {
					endOfRecords = true;
					return -1;
				}

				String line = reader.readLine();
				if (line == null) {
					endOfRecords = true;
					return -1;
				}

				// Restore line terminator removed by readLine()
				currentLineBytes = (line + "\n").getBytes(StandardCharsets.UTF_8);
				currentLinePos = 0;
				recordsRead++;
			}

			// Convert signed byte to unsigned int (0-255)
			return currentLineBytes[currentLinePos++] & 0xFF;
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			if (endOfRecords) {
				return -1;
			}

			int totalRead = 0;
			while (totalRead < len) {
				// Load next line if needed
				if ( (currentLineBytes == null || currentLinePos >= currentLineBytes.length) && !loadNextLine()) {
					return totalRead > 0 ? totalRead : -1;
				}

				// Copy bytes from current line
				int available = currentLineBytes.length - currentLinePos;
				int toCopy = Math.min(available, len - totalRead);
				System.arraycopy(currentLineBytes, currentLinePos, b, off + totalRead, toCopy);
				currentLinePos += toCopy;
				totalRead += toCopy;
			}

			return totalRead;
		}

		private boolean loadNextLine() throws IOException {
			if (recordsRead >= maxRecords) {
				endOfRecords = true;
				return false;
			}

			String line = reader.readLine();
			if (line == null) {
				endOfRecords = true;
				return false;
			}

			// Restore line terminator removed by readLine()
			currentLineBytes = (line + "\n").getBytes(StandardCharsets.UTF_8);
			currentLinePos = 0;
			recordsRead++;
			return true;
		}

		@Override
		public void close() throws IOException {
			reader.close();
		}
	}

	@Override
	public int read() throws IOException {
		if (closed) {
			throw new IOException("Stream closed");
		}
		return delegate.read();
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		if (closed) {
			throw new IOException("Stream closed");
		}
		return delegate.read(b, off, len);
	}

	@Override
	public void close() throws IOException {
		if (!closed) {
			closed = true;
			try {
				delegate.close();
			} finally {
				if (channel != null) {
					channel.close();
				}
			}
		}
	}
}
