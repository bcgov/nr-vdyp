package ca.bc.gov.nrs.vdyp.model.variables;

public interface Variable<P, T> extends Property<P, T> {
	/**
	 * Set the value of a variable field on a particular parent
	 * @param parent
	 * @param value
	 */
	void set(P parent, T value);
}
