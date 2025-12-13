package ca.bc.gov.nrs.vdyp.backend.services;

import java.util.Map;
import java.util.stream.Stream;

import ca.bc.gov.nrs.vdyp.backend.data.assemblers.CalculationEngineResourceAssembler;
import ca.bc.gov.nrs.vdyp.backend.data.entities.CalculationEngineCodeEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.CalculationEngineCodeModel;
import ca.bc.gov.nrs.vdyp.backend.data.repositories.CalculationEngineCodeRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CalculationEngineCodeLookup
		extends AbstractCodeTableLookup<CalculationEngineCodeModel, CalculationEngineCodeEntity> {

	CalculationEngineCodeRepository repository;
	CalculationEngineResourceAssembler assembler;

	private Map<String, String> mapExternalRolesToUserTypeCodes;

	public CalculationEngineCodeLookup(
			CalculationEngineCodeRepository repository, CalculationEngineResourceAssembler assembler
	) {
		this.repository = repository;
		this.assembler = assembler;
	}

	@Override
	protected Stream<CalculationEngineCodeModel> loadAllModels() {
		return repository.listAll().stream().map(assembler::toModel);
	}

	@Override
	protected Stream<CalculationEngineCodeEntity> loadAllEntities() {
		return repository.listAll().stream();
	}

}
