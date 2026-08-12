package ca.bc.gov.nrs.vdyp.backend.services.upload;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import ca.bc.gov.nrs.vdyp.backend.config.CsvUploadConfig;
import ca.bc.gov.nrs.vdyp.backend.exceptions.ProjectionFileUploadException;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CsvUploadValidator {
	private static final String CSV_EXTENSION = ".csv";
	private static final int MAX_FILENAME_LENGTH = 255;
	private static final Set<String> ALLOWED_CONTENT_TYPES = Set
			.of("text/csv", "application/csv", "application/vnd.ms-excel");
	private static final Set<String> DANGEROUS_DOUBLE_EXTENSIONS = Set.of(
			"bat", "cmd", "com", "dll", "elf", "exe", "gif", "htm", "html", "jar", "jpeg", "jpg", "js", "msi", "pdf",
			"php", "png", "ps1", "scr", "sh", "svg", "vbs", "xls", "xlsx", "zip"
	);
	private static final int MAX_HEADER_BYTES = 64 * 1024;

	private final CsvUploadConfig config;

	public CsvUploadValidator(CsvUploadConfig config) {
		this.config = config;
	}

	public ValidatedUpload validateMetadata(String filename, String contentType, long declaredContentLength)
			throws ProjectionFileUploadException {
		String safeFilename = validateFilename(filename);
		String normalizedContentType = validateContentType(contentType);
		if (declaredContentLength < 0) {
			throw ProjectionFileUploadException.invalidCsv("CSV upload content length is required for object storage.");
		}
		if (declaredContentLength == 0) {
			throw ProjectionFileUploadException.invalidCsv("CSV upload must not be empty.");
		}
		if (declaredContentLength > config.maxFileSizeBytes()) {
			throw ProjectionFileUploadException.payloadTooLarge("CSV upload exceeds the 1000000000 byte limit.");
		}
		return new ValidatedUpload(safeFilename, declaredContentLength, normalizedContentType);
	}

	public InputStream validatingStream(InputStream source, String fileSetTypeCode) {
		CsvUploadSchemas.HeaderSchema headerSchema = CsvUploadSchemas.expectedHeaders(fileSetTypeCode).orElse(null);
		UploadSniffer sniffer = new UploadSniffer(config.maxFileSizeBytes(), headerSchema);
		return new ValidatingInputStream(source, sniffer);
	}

	public long maxFileSizeBytes() {
		return config.maxFileSizeBytes();
	}

	private String validateFilename(String filename) throws ProjectionFileUploadException {
		if (filename == null || filename.isBlank()) {
			throw ProjectionFileUploadException.unsupportedMediaType("CSV upload filename is required.");
		}
		if (filename.length() > MAX_FILENAME_LENGTH) {
			throw ProjectionFileUploadException.unsupportedMediaType("CSV upload filename is too long.");
		}
		for (int i = 0; i < filename.length(); i++) {
			char c = filename.charAt(i);
			if (c == 0 || Character.isISOControl(c)) {
				throw ProjectionFileUploadException
						.unsupportedMediaType("CSV upload filename contains invalid characters.");
			}
		}
		if (filename.contains("/") || filename.contains("\\") || filename.contains(":") || filename.contains("..")) {
			throw ProjectionFileUploadException.unsupportedMediaType("CSV upload filename is invalid.");
		}

		String normalized = Normalizer.normalize(filename, Normalizer.Form.NFC).trim();
		if (normalized.isBlank() || normalized.length() > MAX_FILENAME_LENGTH) {
			throw ProjectionFileUploadException.unsupportedMediaType("CSV upload filename is invalid.");
		}

		String lower = normalized.toLowerCase(Locale.ROOT);
		if (!lower.endsWith(CSV_EXTENSION)) {
			throw ProjectionFileUploadException.unsupportedMediaType("Only CSV uploads are supported.");
		}

		String base = lower.substring(0, lower.length() - CSV_EXTENSION.length());
		int previousDot = base.lastIndexOf('.');
		if (previousDot >= 0 && DANGEROUS_DOUBLE_EXTENSIONS.contains(base.substring(previousDot + 1))) {
			throw ProjectionFileUploadException.unsupportedMediaType("CSV upload filename has a misleading extension.");
		}
		if (base.isBlank()) {
			throw ProjectionFileUploadException.unsupportedMediaType("CSV upload filename is invalid.");
		}
		return normalized;
	}

	private String validateContentType(String contentType) throws ProjectionFileUploadException {
		String normalized = normalizedContentType(contentType);
		if (!ALLOWED_CONTENT_TYPES.contains(normalized)) {
			throw ProjectionFileUploadException.unsupportedMediaType("CSV upload content type is not supported.");
		}
		return normalized;
	}

	private String normalizedContentType(String contentType) {
		if (contentType == null) {
			return "";
		}
		int semicolon = contentType.indexOf(';');
		String value = semicolon >= 0 ? contentType.substring(0, semicolon) : contentType;
		return value.trim().toLowerCase(Locale.ROOT);
	}

	public record ValidatedUpload(String filename, long contentLength, String contentType) {
	}

	public static final class CsvUploadValidationIOException extends IOException {
		private final ProjectionFileUploadException validationException;

		private CsvUploadValidationIOException(ProjectionFileUploadException validationException) {
			super(validationException.getMessage(), validationException);
			this.validationException = validationException;
		}

		public ProjectionFileUploadException validationException() {
			return validationException;
		}
	}

	private static final class ValidatingInputStream extends FilterInputStream {
		private final UploadSniffer sniffer;
		private boolean finished;

		private ValidatingInputStream(InputStream in, UploadSniffer sniffer) {
			super(in);
			this.sniffer = sniffer;
		}

		@Override
		public int read() throws IOException {
			int value = super.read();
			if (value < 0) {
				finish();
				return -1;
			}
			process(value);
			return value;
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			int read = super.read(b, off, len);
			if (read < 0) {
				finish();
				return -1;
			}
			try {
				sniffer.process(b, off, read);
			} catch (ProjectionFileUploadException e) {
				throw new CsvUploadValidationIOException(e);
			}
			return read;
		}

		private void process(int value) throws IOException {
			try {
				sniffer.processByte(value & 0xFF);
			} catch (ProjectionFileUploadException e) {
				throw new CsvUploadValidationIOException(e);
			}
		}

		private void finish() throws IOException {
			if (finished) {
				return;
			}
			finished = true;
			try {
				sniffer.finish();
			} catch (ProjectionFileUploadException e) {
				throw new CsvUploadValidationIOException(e);
			}
		}
	}

	private static final class UploadSniffer {
		private final long maxFileBytes;
		private final CsvUploadSchemas.HeaderSchema headerSchema;
		private final byte[] prefix = new byte[16];
		private final byte[] headerBytes = new byte[MAX_HEADER_BYTES];

		private long bytesRead;
		private int headerLength;
		private boolean headerComplete;
		private boolean previousCarriageReturn;

		private UploadSniffer(long maxFileBytes, CsvUploadSchemas.HeaderSchema headerSchema) {
			this.maxFileBytes = maxFileBytes;
			this.headerSchema = headerSchema;
		}

		void process(byte[] buffer, int offset, int length) throws ProjectionFileUploadException {
			if (length <= 0) {
				return;
			}
			if (headerComplete) {
				countBytes(length);
				return;
			}

			int end = offset + length;
			int i = offset;
			for (; i < end && !headerComplete; i++) {
				processByte(buffer[i] & 0xFF);
			}
			if (i < end) {
				countBytes(end - i);
			}
		}

		private void processByte(int b) throws ProjectionFileUploadException {
			countBytes(1);
			if (headerComplete) {
				return;
			}
			if (b == 0) {
				throw ProjectionFileUploadException.invalidCsv("CSV upload contains binary content.");
			}
			capturePrefix(b);
			if (headerSchema == null) {
				if (bytesRead >= prefix.length) {
					headerComplete = true;
				}
				return;
			}
			if (b == '\n') {
				if (previousCarriageReturn && headerLength > 0) {
					headerLength--;
				}
				validateHeader();
				return;
			}
			if (headerLength >= headerBytes.length) {
				throw ProjectionFileUploadException.invalidCsv("CSV upload header is too large.");
			}
			headerBytes[headerLength++] = (byte) b;
			previousCarriageReturn = b == '\r';
		}

		private void countBytes(int length) throws ProjectionFileUploadException {
			bytesRead += length;
			if (bytesRead > maxFileBytes) {
				throw ProjectionFileUploadException.payloadTooLarge("CSV upload exceeds the 1000000000 byte limit.");
			}
		}

		private void capturePrefix(int b) throws ProjectionFileUploadException {
			if (bytesRead <= prefix.length) {
				prefix[(int) bytesRead - 1] = (byte) b;
				rejectKnownBinaryTypes((int) bytesRead);
			}
		}

		private void rejectKnownBinaryTypes(int prefixLength) throws ProjectionFileUploadException {
			if (startsWith(prefix, prefixLength, new byte[] { 'P', 'K', 3, 4 })
					|| startsWith(prefix, prefixLength, new byte[] { 'P', 'K', 5, 6 })
					|| startsWith(prefix, prefixLength, new byte[] { '%', 'P', 'D', 'F', '-' })
					|| startsWith(
							prefix, prefixLength, new byte[] { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A }
					) || startsWith(prefix, prefixLength, new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF })
					|| startsWith(prefix, prefixLength, new byte[] { 'G', 'I', 'F', '8' })
					|| startsWith(prefix, prefixLength, new byte[] { 'M', 'Z' })
					|| startsWith(prefix, prefixLength, new byte[] { 0x7F, 'E', 'L', 'F' })) {
				throw ProjectionFileUploadException.invalidCsv("CSV upload content is not valid CSV text.");
			}
		}

		private boolean startsWith(byte[] value, int valueLength, byte[] expectedPrefix) {
			if (valueLength < expectedPrefix.length) {
				return false;
			}
			for (int i = 0; i < expectedPrefix.length; i++) {
				if (value[i] != expectedPrefix[i]) {
					return false;
				}
			}
			return true;
		}

		private void validateHeader() throws ProjectionFileUploadException {
			headerComplete = true;
			if (headerSchema == null) {
				return;
			}

			String headerLine = new String(headerBytes, 0, headerLength, StandardCharsets.UTF_8);
			if (!headerLine.isEmpty() && headerLine.charAt(0) == '\uFEFF') {
				headerLine = headerLine.substring(1);
			}
			if (!headerSchema.matches(parseCsvHeader(headerLine))) {
				throw ProjectionFileUploadException.invalidCsv("CSV upload headers do not match the expected schema.");
			}
		}

		void finish() throws ProjectionFileUploadException {
			if (bytesRead == 0) {
				throw ProjectionFileUploadException.invalidCsv("CSV upload must not be empty.");
			}
			if (!headerComplete) {
				validateHeader();
			}
		}

		private List<String> parseCsvHeader(String headerLine) {
			List<String> headers = new ArrayList<>();
			StringBuilder current = new StringBuilder();
			boolean inQuotes = false;

			for (int i = 0; i < headerLine.length(); i++) {
				char c = headerLine.charAt(i);
				if (inQuotes) {
					if (c == '"') {
						if (i + 1 < headerLine.length() && headerLine.charAt(i + 1) == '"') {
							current.append('"');
							i++;
						} else {
							inQuotes = false;
						}
					} else {
						current.append(c);
					}
				} else if (c == '"') {
					inQuotes = true;
				} else if (c == ',') {
					headers.add(current.toString().trim());
					current.setLength(0);
				} else {
					current.append(c);
				}
			}

			headers.add(current.toString().trim());
			return headers;
		}
	}
}
