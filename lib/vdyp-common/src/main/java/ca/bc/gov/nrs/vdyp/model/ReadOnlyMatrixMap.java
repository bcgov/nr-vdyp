package ca.bc.gov.nrs.vdyp.model;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A read only view of a {@link MatrixMap}, for matrices that are shared between runs. Reads are passed to the wrapped
 * matrix; any attempt to change a cell throws {@link UnsupportedOperationException}. The view implements the same
 * {@link MatrixMap2}, {@link MatrixMap3} or {@link MatrixMap4} interface as the matrix it wraps.
 * <p>
 * The view only stops changes made through it. The wrapped matrix must not be reachable by any other route, and its
 * values must themselves be read only.
 *
 * @param <T> the cell value type
 */
public class ReadOnlyMatrixMap<T> implements MatrixMap<T> {

	private final MatrixMap<T> delegate;

	private ReadOnlyMatrixMap(MatrixMap<T> delegate) {
		this.delegate = delegate;
	}

	/**
	 * @return a read only view of {@code source} with the same MatrixMapN interface, or {@code source} itself if it is
	 *         already read only
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T> MatrixMap<T> of(MatrixMap<T> source) {
		if (source instanceof ReadOnlyMatrixMap) {
			return source;
		}
		if (source instanceof MatrixMap2) {
			return new ReadOnlyMatrixMap2((MatrixMap2) source);
		}
		if (source instanceof MatrixMap3) {
			return new ReadOnlyMatrixMap3((MatrixMap3) source);
		}
		if (source instanceof MatrixMap4) {
			return new ReadOnlyMatrixMap4((MatrixMap4) source);
		}
		return new ReadOnlyMatrixMap<>(source);
	}

	private static UnsupportedOperationException readOnlyException() {
		return new UnsupportedOperationException(
				"This MatrixMap is shared between runs and is read only; copy it before modifying"
		);
	}

	@Override
	public T getM(Object... params) {
		return delegate.getM(params);
	}

	@Override
	public boolean hasM(Object... params) {
		return delegate.hasM(params);
	}

	@Override
	public void putM(T value, Object... params) {
		throw readOnlyException();
	}

	@Override
	public T remove(Object... params) {
		throw readOnlyException();
	}

	@Override
	public void setAll(T value) {
		throw readOnlyException();
	}

	@Override
	public void setAll(Function<Object[], T> generator) {
		throw readOnlyException();
	}

	@Override
	public boolean all(Predicate<T> pred) {
		return delegate.all(pred);
	}

	@Override
	public boolean any(Predicate<T> pred) {
		return delegate.any(pred);
	}

	@Override
	public void eachKey(Consumer<Object[]> body) {
		delegate.eachKey(body);
	}

	@Override
	public List<Set<?>> getDimensions() {
		return delegate.getDimensions();
	}

	@Override
	public int getNumDimensions() {
		return delegate.getNumDimensions();
	}

	@Override
	public boolean isFull() {
		return delegate.isFull();
	}

	@Override
	public boolean isEmpty() {
		return delegate.isEmpty();
	}

	@Override
	public String toString() {
		return "ReadOnly" + delegate.toString();
	}

	static class ReadOnlyMatrixMap2<K1, K2, V> extends ReadOnlyMatrixMap<V> implements MatrixMap2<K1, K2, V> {
		ReadOnlyMatrixMap2(MatrixMap2<K1, K2, V> delegate) {
			super(delegate);
		}
	}

	static class ReadOnlyMatrixMap3<K1, K2, K3, V> extends ReadOnlyMatrixMap<V> implements MatrixMap3<K1, K2, K3, V> {
		ReadOnlyMatrixMap3(MatrixMap3<K1, K2, K3, V> delegate) {
			super(delegate);
		}
	}

	static class ReadOnlyMatrixMap4<K1, K2, K3, K4, V> extends ReadOnlyMatrixMap<V>
			implements MatrixMap4<K1, K2, K3, K4, V> {
		ReadOnlyMatrixMap4(MatrixMap4<K1, K2, K3, K4, V> delegate) {
			super(delegate);
		}
	}
}
