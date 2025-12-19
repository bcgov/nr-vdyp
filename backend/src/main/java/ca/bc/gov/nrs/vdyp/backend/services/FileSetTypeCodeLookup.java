package ca.bc.gov.nrs.vdyp.backend.services;

import java.util.stream.Stream;

import ca.bc.gov.nrs.vdyp.backend.data.assemblers.FileSetTypeCodeResourceAssembler;
import ca.bc.gov.nrs.vdyp.backend.data.entities.FileSetTypeCodeEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.FileSetTypeCodeModel;
import ca.bc.gov.nrs.vdyp.backend.data.repositories.FileSetTypeCodeRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class FileSetTypeCodeLookup extends AbstractCodeTableLookup<FileSetTypeCodeModel, FileSetTypeCodeEntity> {

	FileSetTypeCodeRepository repository;
	FileSetTypeCodeResourceAssembler assembler;

	public FileSetTypeCodeLookup(FileSetTypeCodeRepository repository, FileSetTypeCodeResourceAssembler assembler) {
		this.repository = repository;
		this.assembler = assembler;
	}

	@Override
	protected Stream<FileSetTypeCodeModel> loadAllModels() {
		return repository.listAll().stream().map(assembler::toModel);
	}

	protected Stream<FileSetTypeCodeEntity> loadAllEntities() {
		return repository.listAll().stream();
	}

}
