package ca.bc.gov.nrs.vdyp.model;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * A mapping from the cartesian product of a set of arbitrary identifiers to a
 * value.
 *
 * @author Kevin Smith, Vivid Solutions
 *
 * @param <T>
 */
public interface MatrixMap<T> {

	public Optional<T> getM(Object... params);

	public void putM(T value, Object... params);

	public boolean all(Predicate<T> pred);

	default public boolean isFull() {
		return all(x -> x != null);
	}

	default public boolean isEmpty() {
		return all(x -> x == null);
	}
}
