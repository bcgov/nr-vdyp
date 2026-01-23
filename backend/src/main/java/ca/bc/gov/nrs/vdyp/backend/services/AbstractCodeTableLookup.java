package ca.bc.gov.nrs.vdyp.backend.services;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.backend.data.entities.CodeTableEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.CodeTableModel;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public abstract class AbstractCodeTableLookup<M extends CodeTableModel, E extends CodeTableEntity> {

	private static final Logger logger = LoggerFactory.getLogger(AbstractCodeTableLookup.class);
	private Map<String, M> modelByCode;
	private Map<String, E> entityByCode;

	@PostConstruct
	void init() {
		Stream<M> all = loadAllModels();
		this.modelByCode = all.collect(Collectors.toMap(t -> normalize(t.getCode()), Function.identity()));
		Stream<E> allE = loadAllEntities();
		this.entityByCode = allE.collect(Collectors.toMap(t -> normalize(t.getCode()), Function.identity()));
	}

	protected abstract Stream<M> loadAllModels();

	protected abstract Stream<E> loadAllEntities();

	protected String normalize(String code) {
		return code == null ? null : code.trim().toUpperCase();
	}

	public M requireModel(String input) {
		var norm = normalize(input);
		M value = modelByCode.get(norm);
		if (value == null) {
			throw new IllegalArgumentException("Unknown code: " + input);
		}
		return value;
	}

	public Optional<M> findModel(String input) {
		if (input == null)
			return Optional.empty();
		return Optional.ofNullable(modelByCode.get(normalize(input)));
	}

	public E requireEntity(String input) {
		var norm = normalize(input);
		E value = entityByCode.get(norm);
		if (value == null) {
			throw new IllegalArgumentException("Unknown code: " + input);
		}
		return value;
	}

	public Optional<E> findEntity(String input) {
		if (input == null)
			return Optional.empty();
		return Optional.ofNullable(entityByCode.get(normalize(input)));
	}
}
