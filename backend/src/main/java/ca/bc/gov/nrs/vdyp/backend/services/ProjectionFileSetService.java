package ca.bc.gov.nrs.vdyp.backend.services;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import ca.bc.gov.nrs.vdyp.backend.data.assemblers.ProjectionFileSetResourceAssembler;
import ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionFileSetEntity;
import ca.bc.gov.nrs.vdyp.backend.data.entities.VDYPUserEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.FileSetTypeCodeModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.ProjectionFileSetModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.VDYPUserModel;
import ca.bc.gov.nrs.vdyp.backend.data.repositories.ProjectionFileSetRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

/**
 * Handles business rules for file sets, including creation updating and deletion
 */
@ApplicationScoped
public class ProjectionFileSetService {
	ProjectionFileSetRepository repository;
	ProjectionFileSetResourceAssembler assembler;
	FileSetTypeCodeLookup lookup;
	EntityManager em;

	public ProjectionFileSetService(
			EntityManager em, ProjectionFileSetRepository repository, ProjectionFileSetResourceAssembler assembler,
			FileSetTypeCodeLookup lookup
	) {
		this.em = em;
		this.repository = repository;
		this.assembler = assembler;
		this.lookup = lookup;
	}

	FileSetTypeCodeModel[] projectionFileSets;

	private void ensureFileSetTypeCodes() {
		if (projectionFileSets == null) {
			projectionFileSets = new FileSetTypeCodeModel[3];
			projectionFileSets[0] = lookup.requireModel(FileSetTypeCodeModel.POLYGON);
			projectionFileSets[1] = lookup.requireModel(FileSetTypeCodeModel.LAYER);
			projectionFileSets[2] = lookup.requireModel(FileSetTypeCodeModel.RESULTS);
		}
	}

	public Map<FileSetTypeCodeModel, ProjectionFileSetModel> createFileSetForNewProjection(VDYPUserModel actingUser) {
		ensureFileSetTypeCodes();
		var map = new HashMap<FileSetTypeCodeModel, ProjectionFileSetModel>();
		for (FileSetTypeCodeModel type : projectionFileSets) {
			map.put(type, createEmptyFileSet(type, actingUser));
		}
		return map;
	}

	@Transactional
	public ProjectionFileSetModel createEmptyFileSet(FileSetTypeCodeModel typeCodeModel, VDYPUserModel actingUser) {
		ProjectionFileSetEntity saveEntity = new ProjectionFileSetEntity();
		saveEntity.setOwnerUser(em.find(VDYPUserEntity.class, UUID.fromString(actingUser.getVdypUserGUID())));
		saveEntity.setFileSetTypeCode(lookup.requireEntity(typeCodeModel.getCode()));
		repository.persist(saveEntity);
		return assembler.toModel(saveEntity);
	}

	@Transactional
	public void deleteFileSetById(UUID polygonFileSetGuid) {
		repository.deleteById(polygonFileSetGuid);
		// TODO this needs to go further and delete the actual files but that is not implemented yet
	}
}
