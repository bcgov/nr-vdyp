package ca.bc.gov.nrs.vdyp.backend.projection.input;

import java.util.Iterator;

public class CsvStreamIterator<T> implements Iterator<T> {

	private final Iterator<T> delegate;

	CsvStreamIterator(Iterator<T> delegate) {
		this.delegate = delegate;
	}

	@Override
	public boolean hasNext() {
		return delegate.hasNext();
	}

	@Override
	public T next() {
		T polygon = delegate.next();

		// Validate

		return polygon;
	}
}
