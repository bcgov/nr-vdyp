package ca.bc.gov.nrs.vdyp.backend.services;

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
import ca.bc.gov.nrs.vdyp.backend.exceptions.ProjectionServiceException;
import ca.bc.gov.nrs.vdyp.backend.model.COMSObject;
import ca.bc.gov.nrs.vdyp.backend.model.COMSObjectVersion;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class FileMappingService {
	private static final Logger logger = LoggerFactory.getLogger(FileMappingService.class);
	private final FileMappingRepository repository;
	private final FileMappingResourceAssembler assembler;

	private final HttpClient httpClient;
	private final COMSClient comsClient;

	public FileMappingService(
			FileMappingRepository repository, FileMappingResourceAssembler assembler, @RestClient COMSClient comsClient,
			HttpClient httpClient
	) {
		this.repository = repository;
		this.assembler = assembler;
		this.comsClient = comsClient;
		this.httpClient = httpClient;
	}

	public FileMappingModel
			createNewFile(String comsBucketGUID, ProjectionFileSetEntity projectionFileSetEntity, FileUpload file)
					throws ProjectionServiceException {
		try {
			String contentDisposition = buildContentDisposition(file.fileName());
			long contentLength = Files.size(file.uploadedFile());

			String contentType = file.contentType() != null ? file.contentType() : MediaType.APPLICATION_OCTET_STREAM;
			try (InputStream fileStream = Files.newInputStream(file.uploadedFile())) {
				logger.debug(
						"Data for object bucketId {}, contentDisposistion {}, contentLength {}, contentType {}",
						comsBucketGUID, contentDisposition, contentLength, contentType
				);
				COMSObject createObjectResponse = comsClient.createObject(
						comsBucketGUID, contentDisposition, contentLength, contentType, fileStream

				);
				UUID objectGUID = UUID.fromString(createObjectResponse.id());

				// persist a record here for the file
				FileMappingEntity entity = persistFileMapping(objectGUID, projectionFileSetEntity, file.fileName());

				// return the data from COMS up the chain
				return assembler.toModel(entity);
			}
		} catch (Exception e) {
			throw new ProjectionServiceException("Error uploading file to COMS", e);
		}
	}

	private static String buildContentDisposition(String filename) {
		// Minimal safe implementation. COMS spec wants RFC6266 with filename/filename*.
		// Start with this; add RFC8187 encoding later if you have non-ASCII names.
		String safe = filename == null ? "upload.bin" : filename.replace("\"", "");
		return "attachment; filename=\"" + safe + "\"";
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
				FileMappingEntity newEntity = persistFileMapping(
						UUID.fromString(comsObj.id()), fileSetEntity, file.getFilename()
				);
				return assembler.toModel(newEntity);
			}
		} catch (ProjectionServiceException e) {
			throw e;
		} catch (Exception e) {
			throw new ProjectionServiceException("Error duplicating file in COMS", e);
		}
	}

	@Transactional
	protected FileMappingEntity
			persistFileMapping(UUID objectGUID, ProjectionFileSetEntity fileSetEntity, String fileName) {
		FileMappingEntity entity = new FileMappingEntity();
		entity.setComsObjectGUID(objectGUID);
		entity.setProjectionFileSet(fileSetEntity);
		entity.setFilename(fileName);
		repository.persist(entity);
		return entity;
	}
}
