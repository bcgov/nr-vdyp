package ca.bc.gov.nrs.vdyp.model.variables;

/**
 * Represents a property of a model object in a reflection-like way without using reflection
 * @param <P>
 * @param <T>
 */
public interface Property<P, T> {

	/**
	 * Get the name of the property
	 * @return
	 */
	String getName();
	
	/**
	 * Get the value of the property from a particular parent object
	 * @param parent
	 * @return
	 */
	T get(P parent);
	
}
