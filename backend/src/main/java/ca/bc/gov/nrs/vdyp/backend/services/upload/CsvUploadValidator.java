package ca.bc.gov.nrs.vdyp.backend.services.upload;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

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
	private static final Pattern NEGATIVE_NUMBER = Pattern.compile("-((\\d+(\\.\\d*)?)|(\\.\\d+))");

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
		StreamingCsvParser parser = new StreamingCsvParser(
				config.maxFileSizeBytes(), config.maxColumns(), config.maxFieldChars(), config.maxRecordBytes(),
				headerSchema
		);
		return new ValidatingInputStream(source, parser);
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
		private final StreamingCsvParser parser;
		private boolean finished;

		private ValidatingInputStream(InputStream in, StreamingCsvParser parser) {
			super(in);
			this.parser = parser;
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
				parser.process(b, off, read);
			} catch (ProjectionFileUploadException e) {
				throw new CsvUploadValidationIOException(e);
			}
			return read;
		}

		private void process(int value) throws IOException {
			try {
				parser.processByte(value & 0xFF);
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
				parser.finish();
			} catch (ProjectionFileUploadException e) {
				throw new CsvUploadValidationIOException(e);
			}
		}
	}

	private static final class StreamingCsvParser {
		private final long maxFileBytes;
		private final int maxColumns;
		private final int maxFieldChars;
		private final int maxRecordBytes;
		private final CsvUploadSchemas.HeaderSchema headerSchema;
		private final List<String> headerFields;
		private final StringBuilder currentField = new StringBuilder();
		private final byte[] prefix = new byte[16];

		private long bytesRead;
		private int utf8Remaining;
		private int utf8CodePoint;
		private int utf8MinCodePoint;
		private boolean seenChar;
		private boolean inQuotes;
		private boolean afterQuote;
		private boolean atFieldStart = true;
		private boolean previousCarriageReturn;
		private boolean recordOpen;
		private int fieldCount;
		private int expectedColumnCount = -1;
		private int recordBytes;
		private long recordNumber;

		private StreamingCsvParser(
				long maxFileBytes, int maxColumns, int maxFieldChars, int maxRecordBytes,
				CsvUploadSchemas.HeaderSchema headerSchema
		) {
			this.maxFileBytes = maxFileBytes;
			this.maxColumns = maxColumns;
			this.maxFieldChars = maxFieldChars;
			this.maxRecordBytes = maxRecordBytes;
			this.headerSchema = headerSchema;
			this.headerFields = headerSchema == null ? null : new ArrayList<>(headerSchema.requiredPrefix().size());
		}

		void process(byte[] buffer, int offset, int length) throws ProjectionFileUploadException {
			for (int i = offset; i < offset + length; i++) {
				processByte(buffer[i] & 0xFF);
			}
		}

		private void processByte(int b) throws ProjectionFileUploadException {
			bytesRead++;
			if (bytesRead > maxFileBytes) {
				throw ProjectionFileUploadException.payloadTooLarge("CSV upload exceeds the 1000000000 byte limit.");
			}
			recordBytes++;
			if (recordBytes > maxRecordBytes) {
				throw ProjectionFileUploadException.invalidCsv("CSV upload contains a record that is too large.");
			}
			if (b == 0) {
				throw ProjectionFileUploadException.invalidCsv("CSV upload contains binary content.");
			}
			capturePrefix(b);
			decodeUtf8(b);
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

		private void decodeUtf8(int b) throws ProjectionFileUploadException {
			if (utf8Remaining == 0) {
				if (b <= 0x7F) {
					emitCodePoint(b);
				} else if (b >= 0xC2 && b <= 0xDF) {
					utf8Remaining = 1;
					utf8CodePoint = b & 0x1F;
					utf8MinCodePoint = 0x80;
				} else if (b >= 0xE0 && b <= 0xEF) {
					utf8Remaining = 2;
					utf8CodePoint = b & 0x0F;
					utf8MinCodePoint = 0x800;
				} else if (b >= 0xF0 && b <= 0xF4) {
					utf8Remaining = 3;
					utf8CodePoint = b & 0x07;
					utf8MinCodePoint = 0x10000;
				} else {
					throw ProjectionFileUploadException.invalidCsv("CSV upload is not valid UTF-8.");
				}
				return;
			}

			if ( (b & 0xC0) != 0x80) {
				throw ProjectionFileUploadException.invalidCsv("CSV upload is not valid UTF-8.");
			}
			utf8CodePoint = (utf8CodePoint << 6) | (b & 0x3F);
			utf8Remaining--;
			if (utf8Remaining == 0) {
				if (utf8CodePoint < utf8MinCodePoint || utf8CodePoint > 0x10FFFF
						|| (utf8CodePoint >= 0xD800 && utf8CodePoint <= 0xDFFF)) {
					throw ProjectionFileUploadException.invalidCsv("CSV upload is not valid UTF-8.");
				}
				emitCodePoint(utf8CodePoint);
			}
		}

		private void emitCodePoint(int codePoint) throws ProjectionFileUploadException {
			if (!seenChar && codePoint == 0xFEFF) {
				seenChar = true;
				return;
			}
			seenChar = true;
			if (codePoint < 0x20 && codePoint != '\r' && codePoint != '\n' && codePoint != '\t') {
				throw ProjectionFileUploadException.invalidCsv("CSV upload contains disallowed control characters.");
			}
			for (char c : Character.toChars(codePoint)) {
				processChar(c);
			}
		}

		private void processChar(char c) throws ProjectionFileUploadException {
			if (previousCarriageReturn) {
				previousCarriageReturn = false;
				if (c == '\n') {
					recordBytes = 0;
					return;
				}
			}

			if (inQuotes) {
				if (c == '"') {
					inQuotes = false;
					afterQuote = true;
				} else {
					append(c);
				}
				return;
			}

			if (afterQuote) {
				if (c == '"') {
					append(c);
					inQuotes = true;
					afterQuote = false;
				} else if (c == ',') {
					endField();
					afterQuote = false;
				} else if (c == '\r' || c == '\n') {
					endRecord(c == '\r');
					afterQuote = false;
				} else {
					throw ProjectionFileUploadException.invalidCsv("CSV upload contains malformed quoting.");
				}
				return;
			}

			if (c == '\r' || c == '\n') {
				endRecord(c == '\r');
			} else if (c == ',') {
				endField();
				recordOpen = true;
			} else if (c == '"') {
				if (!atFieldStart) {
					throw ProjectionFileUploadException.invalidCsv("CSV upload contains malformed quoting.");
				}
				inQuotes = true;
				atFieldStart = false;
				recordOpen = true;
			} else {
				append(c);
			}
		}

		private void append(char c) throws ProjectionFileUploadException {
			if (currentField.length() + 1 > maxFieldChars) {
				throw ProjectionFileUploadException.invalidCsv("CSV upload contains a field that is too large.");
			}
			currentField.append(c);
			atFieldStart = false;
			recordOpen = true;
		}

		private void endField() throws ProjectionFileUploadException {
			String value = currentField.toString();
			validateFormulaPolicy(value);
			fieldCount++;
			if (fieldCount > maxColumns) {
				throw ProjectionFileUploadException.invalidCsv("CSV upload contains too many columns.");
			}
			if (recordNumber == 0 && headerFields != null) {
				headerFields.add(value);
			}
			currentField.setLength(0);
			atFieldStart = true;
		}

		private void validateFormulaPolicy(String value) throws ProjectionFileUploadException {
			String trimmed = value.stripLeading();
			if (trimmed.isEmpty()) {
				return;
			}
			char first = trimmed.charAt(0);
			if (first == '=' || first == '+' || first == '@'
					|| (first == '-' && !NEGATIVE_NUMBER.matcher(trimmed).matches())) {
				throw ProjectionFileUploadException.invalidCsv("CSV upload contains a spreadsheet formula-like cell.");
			}
		}

		private void endRecord(boolean carriageReturn) throws ProjectionFileUploadException {
			endField();
			if (expectedColumnCount < 0) {
				expectedColumnCount = fieldCount;
			} else if (fieldCount != expectedColumnCount) {
				throw ProjectionFileUploadException.invalidCsv("CSV upload has inconsistent column counts.");
			}
			if (recordNumber == 0 && headerSchema != null && !headerSchema.matches(headerFields)) {
				throw ProjectionFileUploadException.invalidCsv("CSV upload headers do not match the expected schema.");
			}
			recordNumber++;
			fieldCount = 0;
			currentField.setLength(0);
			atFieldStart = true;
			recordOpen = false;
			recordBytes = 0;
			previousCarriageReturn = carriageReturn;
		}

		void finish() throws ProjectionFileUploadException {
			if (bytesRead == 0) {
				throw ProjectionFileUploadException.invalidCsv("CSV upload must not be empty.");
			}
			if (utf8Remaining != 0) {
				throw ProjectionFileUploadException.invalidCsv("CSV upload is not valid UTF-8.");
			}
			if (inQuotes) {
				throw ProjectionFileUploadException.invalidCsv("CSV upload contains an incomplete quoted field.");
			}
			if (recordOpen || currentField.length() > 0 || fieldCount > 0) {
				endRecord(false);
			}
			if (recordNumber == 0) {
				throw ProjectionFileUploadException.invalidCsv("CSV upload must contain at least one CSV record.");
			}
		}
	}
}
