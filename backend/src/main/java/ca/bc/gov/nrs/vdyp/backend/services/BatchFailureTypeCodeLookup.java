package ca.bc.gov.nrs.vdyp.backend.services;

import java.util.stream.Stream;

import ca.bc.gov.nrs.vdyp.backend.data.assemblers.BatchFailureTypeCodeResourceAssembler;
import ca.bc.gov.nrs.vdyp.backend.data.entities.BatchFailureTypeCodeEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.BatchFailureTypeCodeModel;
import ca.bc.gov.nrs.vdyp.backend.data.repositories.BatchFailureTypeCodeRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class BatchFailureTypeCodeLookup
		extends AbstractCodeTableLookup<BatchFailureTypeCodeModel, BatchFailureTypeCodeEntity> {

	BatchFailureTypeCodeRepository repository;
	BatchFailureTypeCodeResourceAssembler assembler;

	public BatchFailureTypeCodeLookup(
			BatchFailureTypeCodeRepository repository, BatchFailureTypeCodeResourceAssembler assembler
	) {
		this.repository = repository;
		this.assembler = assembler;
	}

	@Override
	protected Stream<BatchFailureTypeCodeModel> loadAllModels() {
		return repository.listAll().stream().map(assembler::toModel);
	}

	@Override
	protected Stream<BatchFailureTypeCodeEntity> loadAllEntities() {
		return repository.listAll().stream();
	}
}
