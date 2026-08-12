package ca.bc.gov.nrs.vdyp.backend.services.upload;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import ca.bc.gov.nrs.vdyp.backend.config.CsvUploadConfig;
import ca.bc.gov.nrs.vdyp.backend.data.models.FileSetTypeCodeModel;
import ca.bc.gov.nrs.vdyp.backend.exceptions.ProjectionFileUploadException;
import ca.bc.gov.nrs.vdyp.backend.services.upload.CsvUploadValidator.CsvUploadValidationIOException;
import jakarta.ws.rs.core.Response;

class CsvUploadValidatorTest {
	private static final String CSV = "A,B\n1,2\n";
	private static final byte[] CSV_BYTES = CSV.getBytes(StandardCharsets.UTF_8);

	@Test
	void maxFileSizeConfigurationIsExactlyOneBillionBytes() {
		assertEquals(1_000_000_000L, config().maxFileSizeBytes());
	}

	@Test
	void smallValidCsvIsAccepted() {
		assertDoesNotThrow(
				() -> validate(validator(), CSV_BYTES, "input.csv", "text/csv", FileSetTypeCodeModel.RESULTS)
		);
	}

	@Test
	void exactOneBillionByteDeclaredLengthIsAcceptedForEarlyMetadataValidation() {
		assertDoesNotThrow(() -> validator().validateMetadata("input.csv", "text/csv", 1_000_000_000L));
	}

	@Test
	void exactConfiguredFileSizeIsAcceptedFromStream() {
		assertDoesNotThrow(
				() -> validate(
						validator(CSV_BYTES.length, 512, 1_048_576, 10_485_760), CSV_BYTES, "input.csv", "text/csv",
						FileSetTypeCodeModel.RESULTS
				)
		);
	}

	@Test
	void oneByteOverConfiguredFileSizeIsRejectedFromStream() {
		ProjectionFileUploadException ex = assertThrows(
				ProjectionFileUploadException.class,
				() -> validate(
						validator(CSV_BYTES.length - 1L, 512, 1_048_576, 10_485_760), CSV_BYTES, "input.csv",
						"text/csv", FileSetTypeCodeModel.RESULTS, CSV_BYTES.length - 1L
				)
		);

		assertEquals(Response.Status.REQUEST_ENTITY_TOO_LARGE, ex.getStatus());
	}

	@Test
	void declaredLengthOneByteOverLimitIsRejectedEarly() {
		ProjectionFileUploadException ex = assertThrows(
				ProjectionFileUploadException.class,
				() -> validator().validateMetadata("input.csv", "text/csv", 1_000_000_001L)
		);

		assertEquals(Response.Status.REQUEST_ENTITY_TOO_LARGE, ex.getStatus());
	}

	@Test
	void missingDeclaredLengthIsRejectedForComsUpload() {
		ProjectionFileUploadException ex = assertThrows(
				ProjectionFileUploadException.class, () -> validator().validateMetadata("input.csv", "text/csv", -1L)
		);

		assertEquals(Response.Status.BAD_REQUEST, ex.getStatus());
	}

	@Test
	void smallerDeclaredLengthDoesNotAuthoritativelyLimitStreamBytes() {
		assertDoesNotThrow(
				() -> validate(validator(), CSV_BYTES, "input.csv", "text/csv", FileSetTypeCodeModel.RESULTS, 1L)
		);
	}

	@Test
	void largerDeclaredLengthUnderLimitDoesNotAuthoritativelyValidateStreamBytes() {
		assertDoesNotThrow(
				() -> validate(validator(), CSV_BYTES, "input.csv", "text/csv", FileSetTypeCodeModel.RESULTS, 10_000L)
		);
	}

	@Test
	void chunkedStreamingBodyIsValidatedWhenLengthIsKnownFromMultipartPart() {
		assertDoesNotThrow(
				() -> validate(
						validator(), new OneByteAtATimeInputStream(CSV_BYTES), CSV_BYTES.length, "input.csv",
						"text/csv", FileSetTypeCodeModel.RESULTS
				)
		);
	}

	@Test
	void uppercaseCsvExtensionIsAccepted() {
		assertDoesNotThrow(
				() -> validate(validator(), CSV_BYTES, "INPUT.CSV", "text/csv", FileSetTypeCodeModel.RESULTS)
		);
	}

	@ParameterizedTest
	@ValueSource(
			strings = { "input", "input.txt", "file.csv.exe", "..\\input.csv", "../input.csv", "bad\u0001.csv",
					"payload.exe.csv" }
	)
	void unsafeFilenamesAreRejected(String filename) {
		ProjectionFileUploadException ex = assertThrows(
				ProjectionFileUploadException.class,
				() -> validate(validator(), CSV_BYTES, filename, "text/csv", FileSetTypeCodeModel.RESULTS)
		);

		assertEquals(Response.Status.UNSUPPORTED_MEDIA_TYPE, ex.getStatus());
	}

	@Test
	void unsupportedContentTypeIsRejected() {
		ProjectionFileUploadException ex = assertThrows(
				ProjectionFileUploadException.class,
				() -> validate(validator(), CSV_BYTES, "input.csv", "application/pdf", FileSetTypeCodeModel.RESULTS)
		);

		assertEquals(Response.Status.UNSUPPORTED_MEDIA_TYPE, ex.getStatus());
	}

	@Test
	void spoofedCsvContentTypeDoesNotBypassContentValidation() {
		ProjectionFileUploadException ex = assertThrows(
				ProjectionFileUploadException.class,
				() -> validate(
						validator(), "%PDF-1.7\n".getBytes(StandardCharsets.UTF_8), "input.csv", "text/csv",
						FileSetTypeCodeModel.RESULTS
				)
		);

		assertEquals(Response.Status.BAD_REQUEST, ex.getStatus());
	}

	@ParameterizedTest
	@ValueSource(strings = { "%PDF-1.7\n", "PK\u0003\u0004file", "MZ....", "\u007fELF....", "GIF89a...." })
	void binarySignaturesRenamedToCsvAreRejected(String prefix) {
		ProjectionFileUploadException ex = assertThrows(
				ProjectionFileUploadException.class,
				() -> validate(
						validator(), prefix.getBytes(StandardCharsets.UTF_8), "input.csv", "text/csv",
						FileSetTypeCodeModel.RESULTS
				)
		);

		assertEquals(Response.Status.BAD_REQUEST, ex.getStatus());
	}

	@Test
	void pngSignatureRenamedToCsvIsRejected() {
		ProjectionFileUploadException ex = assertThrows(
				ProjectionFileUploadException.class,
				() -> validate(
						validator(), new byte[] { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A }, "input.csv",
						"text/csv", FileSetTypeCodeModel.RESULTS
				)
		);

		assertEquals(Response.Status.BAD_REQUEST, ex.getStatus());
	}

	@Test
	void jpegSignatureRenamedToCsvIsRejected() {
		ProjectionFileUploadException ex = assertThrows(
				ProjectionFileUploadException.class,
				() -> validate(
						validator(), new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00 }, "input.csv",
						"text/csv", FileSetTypeCodeModel.RESULTS
				)
		);

		assertEquals(Response.Status.BAD_REQUEST, ex.getStatus());
	}

	@Test
	void emptyFileIsRejected() {
		assertThrows(
				ProjectionFileUploadException.class,
				() -> validate(validator(), new byte[0], "input.csv", "text/csv", FileSetTypeCodeModel.RESULTS)
		);
	}

	@Test
	void nulByteIsRejected() {
		assertThrows(
				ProjectionFileUploadException.class,
				() -> validate(
						validator(), new byte[] { 'A', '\n', 0, '\n' }, "input.csv", "text/csv",
						FileSetTypeCodeModel.RESULTS
				)
		);
	}

	@Test
	void incorrectPolygonHeadersAreRejected() {
		assertThrows(
				ProjectionFileUploadException.class,
				() -> validate(validator(), CSV_BYTES, "input.csv", "text/csv", FileSetTypeCodeModel.POLYGON)
		);
	}

	@Test
	void uploadValidationDoesNotRunPerRowProjectionDomainValidation() {
		CsvUploadSchemas.HeaderSchema schema = CsvUploadSchemas.expectedHeaders(FileSetTypeCodeModel.POLYGON)
				.orElseThrow();
		String csv = String.join(",", schema.requiredPrefix()) + "\n" + "not-a-number,"
				+ String.join(",", java.util.Collections.nCopies(schema.requiredPrefix().size() - 1, "1")) + "\n";

		assertDoesNotThrow(
				() -> validate(
						validator(), csv.getBytes(StandardCharsets.UTF_8), "polygon.csv", "text/csv",
						FileSetTypeCodeModel.POLYGON
				)
		);
	}

	@Test
	void layerHeadersAllowOptionalTailColumns() {
		CsvUploadSchemas.HeaderSchema schema = CsvUploadSchemas.expectedHeaders(FileSetTypeCodeModel.LAYER)
				.orElseThrow();
		String minimalHeader = String.join(",", schema.requiredPrefix()) + "\n";
		String withOptionalTail = String.join(
				",",
				concat(
						schema.requiredPrefix(),
						java.util.List.of("EST_SITE_INDEX_SPP6", "EST_AGE_SPP3", "EST_HEIGHT_SPP3")
				)
		) + "\n";

		assertDoesNotThrow(
				() -> validate(
						validator(), minimalHeader.getBytes(StandardCharsets.UTF_8), "layer.csv", "text/csv",
						FileSetTypeCodeModel.LAYER
				)
		);
		assertDoesNotThrow(
				() -> validate(
						validator(), withOptionalTail.getBytes(StandardCharsets.UTF_8), "layer.csv", "text/csv",
						FileSetTypeCodeModel.LAYER
				)
		);
	}

	@Test
	void uploadValidationStopsInspectingAfterHeader() {
		assertDoesNotThrow(
				() -> validate(
						validator(), "A,B\n1234567890123456,\u0000\n".getBytes(StandardCharsets.UTF_8), "input.csv",
						"text/csv", FileSetTypeCodeModel.RESULTS
				)
		);
	}

	private void validate(CsvUploadValidator validator, byte[] bytes, String filename, String contentType, String type)
			throws ProjectionFileUploadException, IOException {
		validate(validator, bytes, filename, contentType, type, bytes.length);
	}

	private void validate(
			CsvUploadValidator validator, byte[] bytes, String filename, String contentType, String type,
			long declaredLength
	) throws ProjectionFileUploadException, IOException {
		validate(validator, new ByteArrayInputStream(bytes), bytes.length, filename, contentType, type, declaredLength);
	}

	private void validate(
			CsvUploadValidator validator, InputStream source, long actualLength, String filename, String contentType,
			String type
	) throws ProjectionFileUploadException, IOException {
		validate(validator, source, actualLength, filename, contentType, type, actualLength);
	}

	private void validate(
			CsvUploadValidator validator, InputStream source, long actualLength, String filename, String contentType,
			String type, long declaredLength
	) throws ProjectionFileUploadException, IOException {
		validator.validateMetadata(filename, contentType, declaredLength);
		try (InputStream validating = validator.validatingStream(source, type)) {
			validating.transferTo(OutputStream.nullOutputStream());
		} catch (CsvUploadValidationIOException e) {
			throw e.validationException();
		}
	}

	private CsvUploadValidator validator() {
		return validator(1_000_000_000L, 512, 1_048_576, 10_485_760);
	}

	private CsvUploadValidator validator(long maxBytes, int maxColumns, int maxFieldChars, int maxRecordBytes) {
		return new CsvUploadValidator(config(maxBytes, maxColumns, maxFieldChars, maxRecordBytes));
	}

	private CsvUploadConfig config() {
		return config(1_000_000_000L, 512, 1_048_576, 10_485_760);
	}

	private CsvUploadConfig config(long maxBytes, int maxColumns, int maxFieldChars, int maxRecordBytes) {
		return new CsvUploadConfig() {
			@Override
			public long maxFileSizeBytes() {
				return maxBytes;
			}

			@Override
			public int maxColumns() {
				return maxColumns;
			}

			@Override
			public int maxFieldChars() {
				return maxFieldChars;
			}

			@Override
			public int maxRecordBytes() {
				return maxRecordBytes;
			}
		};
	}

	private java.util.List<String> concat(java.util.List<String> first, java.util.List<String> second) {
		java.util.ArrayList<String> combined = new java.util.ArrayList<>(first);
		combined.addAll(second);
		return combined;
	}

	private static final class OneByteAtATimeInputStream extends InputStream {
		private final byte[] bytes;
		private int offset;

		private OneByteAtATimeInputStream(byte[] bytes) {
			this.bytes = bytes;
		}

		@Override
		public int read() {
			if (offset >= bytes.length) {
				return -1;
			}
			return bytes[offset++] & 0xFF;
		}

		@Override
		public int read(byte[] b, int off, int len) {
			int value = read();
			if (value < 0) {
				return -1;
			}
			b[off] = (byte) value;
			return 1;
		}
	}
}
