package ca.bc.gov.nrs.vdyp.backend.test;

import java.util.UUID;

import ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionEntity;
import ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionFileSetEntity;
import ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionStatusCodeEntity;
import ca.bc.gov.nrs.vdyp.backend.data.entities.VDYPUserEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.FileMappingModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.FileSetTypeCodeModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.ProjectionBatchMappingModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.ProjectionFileSetModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.VDYPUserModel;

public class TestUtils {
	// -----------------------------
	// Helpers
	// -----------------------------

	public static VDYPUserModel user(UUID id) {
		VDYPUserModel u = new VDYPUserModel();
		u.setVdypUserGUID(id.toString());
		return u;
	}

	public static VDYPUserEntity userEntity(UUID id) {
		VDYPUserEntity e = new VDYPUserEntity();
		e.setVdypUserGUID(id);
		return e;
	}

	public static ProjectionEntity projectionEntity(UUID projectionId, UUID ownerId) {
		ProjectionEntity e = new ProjectionEntity();
		e.setProjectionGUID(projectionId);
		e.setOwnerUser(userEntity(ownerId));
		return e;
	}

	public static ProjectionEntity projectionEntity(UUID projectionId, UUID ownerId, String projectionStatus) {
		ProjectionEntity e = projectionEntity(projectionId, ownerId);
		e.setProjectionStatusCode(statusCode(projectionStatus));
		return e;
	}

	public static ProjectionStatusCodeEntity statusCode(String status) {
		var e = new ProjectionStatusCodeEntity();
		e.setCode(status);
		return e;
	}

	public static ProjectionFileSetModel fileSetModel(UUID id, UUID ownerId, String fileSetTypeCode) {
		ProjectionFileSetModel m = new ProjectionFileSetModel();
		m.setProjectionFileSetGUID(id.toString());
		m.setOwnerModel(user(ownerId));
		m.setFileSetTypeCode(fileSetTypeCodeModel(fileSetTypeCode));
		return m;
	}

	public static FileSetTypeCodeModel fileSetTypeCodeModel(String code) {
		FileSetTypeCodeModel m = new FileSetTypeCodeModel();
		m.setCode(code);
		return m;
	}

	public static ProjectionFileSetEntity fileSetEntity(UUID id) {
		ProjectionFileSetEntity e = new ProjectionFileSetEntity();
		e.setProjectionFileSetGUID(id);
		return e;
	}

	public static ProjectionFileSetEntity fileSetEntity(UUID id, UUID ownerId) {
		ProjectionFileSetEntity e = fileSetEntity(id);
		e.setOwnerUser(userEntity(ownerId));
		return e;
	}

	public static FileMappingModel fileMappingModel(UUID id, UUID comsId) {
		FileMappingModel m = new FileMappingModel();
		m.setFileMappingGUID(id.toString());
		m.setComsObjectGUID(comsId.toString());
		return m;
	}

	public static ProjectionBatchMappingModel batchMappingModel(UUID id) {
		ProjectionBatchMappingModel m = new ProjectionBatchMappingModel();
		m.setBatchJobGUID(id.toString());
		return m;
	}
}
