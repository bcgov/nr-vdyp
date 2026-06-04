package ca.bc.gov.nrs.vdyp.backend.services;

import java.util.UUID;

import ca.bc.gov.nrs.vdyp.backend.data.assemblers.FileMappingResourceAssembler;
import ca.bc.gov.nrs.vdyp.backend.data.entities.FileMappingEntity;
import ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionFileSetEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.FileMappingModel;
import ca.bc.gov.nrs.vdyp.backend.data.repositories.FileMappingRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class FileMappingPersistenceService {
	private final EntityManager em;
	private final FileMappingRepository repository;
	private final FileMappingResourceAssembler assembler;

	public FileMappingPersistenceService(
			EntityManager em, FileMappingRepository repository, FileMappingResourceAssembler assembler
	) {
		this.em = em;
		this.repository = repository;
		this.assembler = assembler;
	}

	@Transactional
	public FileMappingModel persistFileMapping(UUID objectGUID, UUID fileSetGUID, String fileName) {
		FileMappingEntity entity = new FileMappingEntity();
		entity.setComsObjectGUID(objectGUID);
		entity.setProjectionFileSet(em.find(ProjectionFileSetEntity.class, fileSetGUID));
		entity.setFilename(fileName);
		repository.persist(entity);
		return assembler.toModel(entity);
	}
}
