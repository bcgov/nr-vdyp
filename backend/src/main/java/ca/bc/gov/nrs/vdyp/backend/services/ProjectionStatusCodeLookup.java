package ca.bc.gov.nrs.vdyp.backend.services;

import java.util.stream.Stream;

import ca.bc.gov.nrs.vdyp.backend.data.assemblers.ProjectionStatusCodeResourceAssembler;
import ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionStatusCodeEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.ProjectionStatusCodeModel;
import ca.bc.gov.nrs.vdyp.backend.data.repositories.ProjectionStatusCodeRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProjectionStatusCodeLookup
		extends AbstractCodeTableLookup<ProjectionStatusCodeModel, ProjectionStatusCodeEntity> {

	ProjectionStatusCodeRepository repository;
	ProjectionStatusCodeResourceAssembler assembler;

	public ProjectionStatusCodeLookup(
			ProjectionStatusCodeRepository repository, ProjectionStatusCodeResourceAssembler assembler
	) {
		this.repository = repository;
		this.assembler = assembler;
	}

	@Override
	protected Stream<ProjectionStatusCodeModel> loadAllModels() {
		return repository.listAll().stream().map(assembler::toModel);
	}

	@Override
	protected Stream<ProjectionStatusCodeEntity> loadAllEntities() {
		return repository.listAll().stream();
	}

}
