package ca.bc.gov.nrs.vdyp.backend.data.assemblers;

import java.util.function.Supplier;

import ca.bc.gov.nrs.vdyp.backend.data.entities.CodeTableEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.CodeTableModel;

public class CodeTableResourceAssembler<T extends CodeTableEntity, U extends CodeTableModel> {
	private final Supplier<T> entityFactory;
	private final Supplier<U> modelFactory;

	public CodeTableResourceAssembler(Supplier<T> entityFactory, Supplier<U> modelFactory) {
		this.entityFactory = entityFactory;
		this.modelFactory = modelFactory;
	}

	public T toEntity(U model) {
		if (model == null) {
			return null;
		}

		var entity = entityFactory.get();
		entity.setCode(model.getCode());
		entity.setDescription(model.getDescription());
		entity.setDisplayOrder(model.getDisplayOrder());
		return entity;
	}

	public U toModel(T entity) {
		if (entity == null) {
			return null;
		}
		var model = modelFactory.get();
		model.setCode(entity.getCode());
		model.setDescription(entity.getDescription());
		model.setDisplayOrder(entity.getDisplayOrder());
		return model;
	}
}
