package ca.bc.gov.nrs.vdyp.backend.services.upload;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import ca.bc.gov.nrs.vdyp.backend.config.CsvUploadConfig;
import ca.bc.gov.nrs.vdyp.backend.data.models.FileSetTypeCodeModel;
import ca.bc.gov.nrs.vdyp.backend.exceptions.ProjectionFileUploadException;
import ca.bc.gov.nrs.vdyp.backend.services.upload.CsvUploadValidator.CsvUploadValidationIOException;
import ca.bc.gov.nrs.vdyp.backend.services.upload.CsvUploadValidator.ValidatedUpload;
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
						validator(CSV_BYTES.length), CSV_BYTES, "input.csv", "text/csv", FileSetTypeCodeModel.RESULTS
				)
		);
	}

	@Test
	void oneByteOverConfiguredFileSizeIsRejectedFromStream() {
		ProjectionFileUploadException ex = assertThrows(
				ProjectionFileUploadException.class,
				() -> validate(
						validator(CSV_BYTES.length - 1L), CSV_BYTES, "input.csv", "text/csv",
						FileSetTypeCodeModel.RESULTS, CSV_BYTES.length - 1L
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
	void uppercaseCsvExtensionIsAccepted() {
		assertDoesNotThrow(
				() -> validate(validator(), CSV_BYTES, "INPUT.CSV", "text/csv", FileSetTypeCodeModel.RESULTS)
		);
	}

	@Test
	void filenameAndContentTypeAreNormalized() throws ProjectionFileUploadException {
		ValidatedUpload upload = validator()
				.validateMetadata(" cafe\u0301.csv ", " Text/CSV; charset=UTF-8 ", CSV_BYTES.length);

		assertEquals("caf\u00e9.csv", upload.filename());
		assertEquals(CSV_BYTES.length, upload.contentLength());
		assertEquals("text/csv", upload.contentType());
	}

	@ParameterizedTest
	@ValueSource(strings = { "text/csv", "application/csv", "application/vnd.ms-excel" })
	void allowedContentTypesAreAccepted(String contentType) {
		assertDoesNotThrow(() -> validator().validateMetadata("input.csv", contentType, CSV_BYTES.length));
	}

	@ParameterizedTest
	@ValueSource(
			strings = { "input", "input.txt", "file.csv.exe", "..\\input.csv", "../input.csv", "bad\u0001.csv",
					"payload.exe.csv", "", " ", ".csv" }
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
	void emptyDeclaredLengthIsRejectedEarly() {
		ProjectionFileUploadException ex = assertThrows(
				ProjectionFileUploadException.class, () -> validator().validateMetadata("input.csv", "text/csv", 0L)
		);

		assertEquals(Response.Status.BAD_REQUEST, ex.getStatus());
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
	void polygonHeaderWithBomQuotesAndCrLfIsAccepted() {
		CsvUploadSchemas.HeaderSchema schema = CsvUploadSchemas.expectedHeaders(FileSetTypeCodeModel.POLYGON)
				.orElseThrow();
		List<String> headers = new ArrayList<>(schema.requiredPrefix());
		headers.set(0, "\"" + headers.get(0) + "\"");
		String csv = "\uFEFF" + String.join(",", headers) + "\r\n";

		assertDoesNotThrow(
				() -> validate(
						validator(), csv.getBytes(StandardCharsets.UTF_8), "polygon.csv", "text/csv",
						FileSetTypeCodeModel.POLYGON
				)
		);
	}

	@Test
	void polygonHeaderWithExtraColumnIsRejected() {
		CsvUploadSchemas.HeaderSchema schema = CsvUploadSchemas.expectedHeaders(FileSetTypeCodeModel.POLYGON)
				.orElseThrow();
		String csv = String.join(",", concat(schema.requiredPrefix(), List.of("EXTRA_COLUMN"))) + "\n";

		ProjectionFileUploadException ex = assertThrows(
				ProjectionFileUploadException.class,
				() -> validate(
						validator(), csv.getBytes(StandardCharsets.UTF_8), "polygon.csv", "text/csv",
						FileSetTypeCodeModel.POLYGON
				)
		);

		assertEquals(Response.Status.BAD_REQUEST, ex.getStatus());
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
	void layerHeadersRejectDuplicateOptionalTailColumn() {
		CsvUploadSchemas.HeaderSchema schema = CsvUploadSchemas.expectedHeaders(FileSetTypeCodeModel.LAYER)
				.orElseThrow();
		String optionalColumn = schema.allowedOptionalTail().iterator().next();
		String csv = String.join(",", concat(schema.requiredPrefix(), List.of(optionalColumn, optionalColumn))) + "\n";

		ProjectionFileUploadException ex = assertThrows(
				ProjectionFileUploadException.class,
				() -> validate(
						validator(), csv.getBytes(StandardCharsets.UTF_8), "layer.csv", "text/csv",
						FileSetTypeCodeModel.LAYER
				)
		);

		assertEquals(Response.Status.BAD_REQUEST, ex.getStatus());
	}

	@Test
	void layerHeadersRejectUnknownOptionalTailColumn() {
		CsvUploadSchemas.HeaderSchema schema = CsvUploadSchemas.expectedHeaders(FileSetTypeCodeModel.LAYER)
				.orElseThrow();
		String csv = String.join(",", concat(schema.requiredPrefix(), List.of("UNKNOWN_OPTIONAL_COLUMN"))) + "\n";

		ProjectionFileUploadException ex = assertThrows(
				ProjectionFileUploadException.class,
				() -> validate(
						validator(), csv.getBytes(StandardCharsets.UTF_8), "layer.csv", "text/csv",
						FileSetTypeCodeModel.LAYER
				)
		);

		assertEquals(Response.Status.BAD_REQUEST, ex.getStatus());
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

	@Test
	void oversizedHeaderIsRejected() {
		ProjectionFileUploadException ex = assertThrows(
				ProjectionFileUploadException.class,
				() -> validate(
						validator(), "A".repeat(64 * 1024 + 1).getBytes(StandardCharsets.UTF_8), "polygon.csv",
						"text/csv", FileSetTypeCodeModel.POLYGON
				)
		);

		assertEquals(Response.Status.BAD_REQUEST, ex.getStatus());
	}

	@Test
	void singleByteReadPathFinishesIdempotently() throws IOException {
		try (
				InputStream validating = validator()
						.validatingStream(new ByteArrayInputStream(CSV_BYTES), FileSetTypeCodeModel.RESULTS)
		) {
			while (validating.read() >= 0) {
				// read to the end
			}

			assertEquals(-1, validating.read());
		}
	}

	@Test
	void singleByteReadPathWrapsValidationFailures() throws IOException {
		try (
				InputStream validating = validator()
						.validatingStream(new ByteArrayInputStream(new byte[] { 0 }), FileSetTypeCodeModel.RESULTS)
		) {
			CsvUploadValidationIOException ex = assertThrows(CsvUploadValidationIOException.class, validating::read);

			assertEquals(Response.Status.BAD_REQUEST, ex.validationException().getStatus());
		}
	}

	private void validate(CsvUploadValidator validator, byte[] bytes, String filename, String contentType, String type)
			throws ProjectionFileUploadException, IOException {
		validate(validator, bytes, filename, contentType, type, bytes.length);
	}

	private void validate(
			CsvUploadValidator validator, byte[] bytes, String filename, String contentType, String type,
			long declaredLength
	) throws ProjectionFileUploadException, IOException {
		validate(validator, new ByteArrayInputStream(bytes), filename, contentType, type, declaredLength);
	}

	private void validate(
			CsvUploadValidator validator, InputStream source, String filename, String contentType, String type,
			long declaredLength
	) throws ProjectionFileUploadException, IOException {
		validator.validateMetadata(filename, contentType, declaredLength);
		try (InputStream validating = validator.validatingStream(source, type)) {
			validating.transferTo(OutputStream.nullOutputStream());
		} catch (CsvUploadValidationIOException e) {
			throw e.validationException();
		}
	}

	private CsvUploadValidator validator() {
		return validator(1_000_000_000L);
	}

	private CsvUploadValidator validator(long maxBytes) {
		return new CsvUploadValidator(config(maxBytes));
	}

	private CsvUploadConfig config() {
		return config(1_000_000_000L);
	}

	private CsvUploadConfig config(long maxBytes) {
		return new CsvUploadConfig() {
			@Override
			public long maxFileSizeBytes() {
				return maxBytes;
			}

		};
	}

	private java.util.List<String> concat(java.util.List<String> first, java.util.List<String> second) {
		java.util.ArrayList<String> combined = new java.util.ArrayList<>(first);
		combined.addAll(second);
		return combined;
	}

}
