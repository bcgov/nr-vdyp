package ca.bc.gov.nrs.vdyp.model;

/**
 * A mutable object that can be made read only in place, so that it can be shared between runs without being copied.
 * <p>
 * Once {@link #makeReadOnly} has been called, every method that would change the object throws
 * {@link UnsupportedOperationException}. An object can not be made modifiable again.
 * <p>
 * Control map values that implement this interface are made read only by
 * {@link ca.bc.gov.nrs.vdyp.controlmap.SharedControlMapValues} when a control map is shared. Use it for types that must
 * keep their exact class (so they can not be replaced by a read only wrapper or copy).
 */
public interface ReadOnlyCapable {

	/**
	 * Make this object read only. Calling it again has no further effect.
	 */
	void makeReadOnly();

	/**
	 * @return whether {@link #makeReadOnly} has been called
	 */
	boolean isReadOnly();
}
