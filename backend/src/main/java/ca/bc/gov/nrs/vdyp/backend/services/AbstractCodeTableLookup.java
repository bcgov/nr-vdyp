package ca.bc.gov.nrs.vdyp.backend.services;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ca.bc.gov.nrs.vdyp.backend.data.models.CodeTableModel;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public abstract class AbstractCodeTableLookup<T extends CodeTableModel> {

	private Map<String, T> byCode;

	@PostConstruct
	void init() {
		Stream<T> all = loadAll(); // implemented by subclass or injected repo
		this.byCode = all.collect(Collectors.toMap(t -> normalize(t.getCode()), Function.identity()));
	}

	protected abstract Stream<T> loadAll();

	protected String normalize(String code) {
		return code == null ? null : code.trim().toUpperCase();
	}

	public T require(String input) {
		var norm = normalize(input);
		T value = byCode.get(norm);
		if (value == null) {
			throw new IllegalArgumentException("Unknown code: " + input);
		}
		return value;
	}

	public Optional<T> find(String input) {
		if (input == null)
			return Optional.empty();
		return Optional.ofNullable(byCode.get(normalize(input)));
	}
}
