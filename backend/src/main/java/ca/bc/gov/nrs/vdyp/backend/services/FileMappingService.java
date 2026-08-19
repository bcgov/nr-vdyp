package ca.bc.gov.nrs.vdyp.backend.services;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.backend.clients.COMSClient;
import ca.bc.gov.nrs.vdyp.backend.data.assemblers.FileMappingResourceAssembler;
import ca.bc.gov.nrs.vdyp.backend.data.entities.FileMappingEntity;
import ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionFileSetEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.FileMappingModel;
import ca.bc.gov.nrs.vdyp.backend.data.repositories.FileMappingRepository;
import ca.bc.gov.nrs.vdyp.backend.exceptions.ProjectionFileUploadException;
import ca.bc.gov.nrs.vdyp.backend.exceptions.ProjectionServiceException;
import ca.bc.gov.nrs.vdyp.backend.model.COMSObject;
import ca.bc.gov.nrs.vdyp.backend.model.COMSObjectVersion;
import ca.bc.gov.nrs.vdyp.backend.services.upload.CsvUploadValidator;
import ca.bc.gov.nrs.vdyp.backend.services.upload.CsvUploadValidator.CsvUploadValidationIOException;
import ca.bc.gov.nrs.vdyp.backend.services.upload.CsvUploadValidator.ValidatedUpload;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class FileMappingService {
	private static final Logger logger = LoggerFactory.getLogger(FileMappingService.class);
	private final FileMappingRepository repository;
	private final FileMappingResourceAssembler assembler;

	private final HttpClient httpClient;
	private final COMSClient comsClient;
	private final FileMappingPersistenceService persistenceService;
	private final CsvUploadValidator csvUploadValidator;

	public FileMappingService(
			FileMappingRepository repository, FileMappingResourceAssembler assembler, @RestClient COMSClient comsClient,
			HttpClient httpClient, FileMappingPersistenceService persistenceService,
			CsvUploadValidator csvUploadValidator
	) {
		this.repository = repository;
		this.assembler = assembler;
		this.comsClient = comsClient;
		this.httpClient = httpClient;
		this.persistenceService = persistenceService;
		this.csvUploadValidator = csvUploadValidator;
	}

	public FileMappingModel
			createNewFile(String comsBucketGUID, ProjectionFileSetEntity projectionFileSetEntity, FileUpload file)
					throws ProjectionServiceException {
		if (file == null) {
			throw ProjectionFileUploadException.invalidCsv("CSV upload file is required.");
		}

		logger.info(
				"Attempting validation of upload of file.fileName {}, file.contentType {}, file.size {}",
				file.fileName(), file.contentType(), file.size()
		);
		String fileSetTypeCode = projectionFileSetEntity.getFileSetTypeCode() == null ? null
				: projectionFileSetEntity.getFileSetTypeCode().getCode();
		ValidatedUpload upload = csvUploadValidator.validateMetadata(file.fileName(), file.contentType(), file.size());
		UUID objectGUID = null;
		try {
			String contentDisposition = buildContentDisposition(upload.filename());
			try (
					InputStream source = Files.newInputStream(file.uploadedFile());
					InputStream fileStream = csvUploadValidator.validatingStream(source, fileSetTypeCode)
			) {
				logger.debug(
						"Data for object bucketId {}, contentDisposistion {}, contentLength {}, contentType {}",
						comsBucketGUID, contentDisposition, upload.contentLength(), upload.contentType()
				);
				COMSObject createObjectResponse = comsClient.createObject(
						comsBucketGUID, contentDisposition, upload.contentLength(), upload.contentType(), fileStream

				);
				objectGUID = UUID.fromString(createObjectResponse.id());

				// persist a record here for the file
				return persistenceService.persistFileMapping(
						objectGUID, projectionFileSetEntity.getProjectionFileSetGUID(), upload.filename()
				);
			}
		} catch (CsvUploadValidationIOException e) {
			throw e.validationException();
		} catch (Exception e) {
			if (objectGUID != null) {
				deleteComsObjectQuietly(objectGUID, e);
			}
			throw new ProjectionServiceException("Error uploading file to COMS", e);
		}
	}

	private static String buildContentDisposition(String filename) {
		// Minimal safe implementation. COMS spec wants RFC6266 with filename/filename*.
		// Start with this; add RFC8187 encoding later if you have non-ASCII names.
		String safe = filename == null ? "upload.csv" : filename.replace("\\", "").replace("\"", "");
		return "attachment; filename=\"" + safe + "\"";
	}

	private void deleteComsObjectQuietly(UUID objectGUID, Exception originalException) {
		try {
			deleteComsObject(objectGUID);
		} catch (Exception cleanupException) {
			originalException.addSuppressed(cleanupException);
			logger.warn("Failed to clean up COMS object after upload failure");
		}
	}

	private void deleteComsObject(UUID objectGUID) throws ProjectionServiceException {
		String comsObjectID = objectGUID.toString();
		List<COMSObjectVersion> versions = comsClient.getObjectVersions(comsObjectID);
		if (versions != null && !versions.isEmpty()) {
			for (COMSObjectVersion version : versions) {
				try (Response response = comsClient.deleteObjectVersion(comsObjectID, version.s3VersionId())) {
					if (response.getStatusInfo().getStatusCode() != Response.Status.OK.getStatusCode()) {
						throw new ProjectionServiceException("Could not delete object in COMS after upload failure");
					}
				}
			}
		} else {
			try (Response response = comsClient.deleteObject(comsObjectID)) {
				if (response.getStatusInfo().getStatusCode() >= 400) {
					throw new ProjectionServiceException("Could not delete object in COMS after upload failure");
				}
			}
		}
	}

	private FileMappingEntity getFileMappingEntity(UUID fileMappingGUID) throws ProjectionServiceException {
		var entity = repository.findByIdOptional(fileMappingGUID);
		if (entity.isEmpty()) {
			throw new ProjectionServiceException(String.format("File %s does not exist in VDYP", fileMappingGUID));
		}
		return entity.get();
	}

	public FileMappingModel getFileById(UUID fileMappingGUID, boolean isDownload) throws ProjectionServiceException {
		var entity = getFileMappingEntity(fileMappingGUID);
		return getFileDetails(entity, isDownload);
	}

	public List<FileMappingModel> getFilesForFileSet(UUID fileSetGUID, boolean isDownload) {
		List<FileMappingEntity> entities = repository.listForFileSet(fileSetGUID);
		return entities.stream().map(e -> getFileDetails(e, isDownload)).toList();
	}

	private FileMappingModel getFileDetails(FileMappingEntity entity, boolean isDownload) {
		var model = assembler.toModel(entity);
		if (isDownload) {
			URL url = null;
			try {
				url = new URL(
						comsClient.getObject(model.getComsObjectGUID(), COMSClient.FileDownloadMode.URL.getParamValue())
								.getString()
				);
			} catch (MalformedURLException e) {
				logger.error("Malformed URL received from COMS for object {}", model.getComsObjectGUID(), e);
			}
			model.setDownloadURL(url);
		}
		return model;
	}

	public void deleteFileMapping(UUID fileMappingGUID) throws ProjectionServiceException {
		var entity = getFileMappingEntity(fileMappingGUID);
		deleteFile(entity);
	}

	public void deleteFilesForSet(UUID polygonFileSetGuid) throws ProjectionServiceException {
		List<FileMappingEntity> entities = repository.listForFileSet(polygonFileSetGuid);
		for (FileMappingEntity entity : entities) {
			deleteFile(entity);
		}
	}

	private void deleteFile(FileMappingEntity entity) throws ProjectionServiceException {
		String comsObjectID = entity.getComsObjectGUID().toString();

		List<COMSObjectVersion> versions = comsClient.getObjectVersions(comsObjectID);

		if (versions != null && !versions.isEmpty()) {
			// Hard delete: remove each version individually
			for (COMSObjectVersion version : versions) {
				try (Response response = comsClient.deleteObjectVersion(comsObjectID, version.s3VersionId())) {
					if (response.getStatusInfo().getStatusCode() != Response.Status.OK.getStatusCode()) {
						throw new ProjectionServiceException(
								String.format(
										"Could not delete version %s of object %s in COMS", version.s3VersionId(),
										comsObjectID
								)
						);
					}
				}
			}
		}

		repository.delete(entity);
	}

	public FileMappingModel
			duplicateFile(FileMappingModel file, ProjectionFileSetEntity fileSetEntity, String comsBucketGUID)
					throws ProjectionServiceException {
		HttpRequest getReq = HttpRequest.newBuilder(URI.create(file.getDownloadURL().toString())).GET().build();

		try {
			HttpResponse<InputStream> resp = httpClient.send(getReq, HttpResponse.BodyHandlers.ofInputStream());

			if (resp.statusCode() >= 400) {
				throw new ProjectionServiceException("Upstream GET failed: HTTP " + resp.statusCode());
			}

			long len = resp.headers().firstValueAsLong("Content-Length").orElseThrow(
					() -> new ProjectionServiceException(
							"Upstream did not send Content-Length; COMS PUT requires Content-Length."
					)
			);

			try (InputStream in = resp.body()) {
				COMSObject comsObj = comsClient.createObject(
						comsBucketGUID, buildContentDisposition(file.getFilename()), len,
						jakarta.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM, // String constant
						in
				);
				return persistenceService.persistFileMapping(
						UUID.fromString(comsObj.id()), fileSetEntity.getProjectionFileSetGUID(), file.getFilename()
				);
			}
		} catch (ProjectionServiceException e) {
			throw e;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt(); // restore interrupt status
			throw new ProjectionServiceException("Thread interrupted during COMS duplication", e);
		} catch (Exception e) {
			throw new ProjectionServiceException("Error duplicating file in COMS", e);
		}
	}

	public FileMappingModel createPlaceholderFile(
			String comsBucketGUID, ProjectionFileSetEntity projectionFileSetEntity, String filename
	) throws ProjectionServiceException {
		try {
			String contentDisposition = buildContentDisposition(filename);
			byte[] placeholder = new byte[] { 0 };
			InputStream placeholderStream = new ByteArrayInputStream(placeholder);
			COMSObject createObjectResponse = comsClient.createObject(
					comsBucketGUID, contentDisposition, placeholder.length, MediaType.APPLICATION_OCTET_STREAM,
					placeholderStream
			);
			UUID objectGUID = UUID.fromString(createObjectResponse.id());
			return persistenceService
					.persistFileMapping(objectGUID, projectionFileSetEntity.getProjectionFileSetGUID(), filename);
		} catch (Exception e) {
			throw new ProjectionServiceException("Error creating placeholder file in COMS", e);
		}
	}
}
