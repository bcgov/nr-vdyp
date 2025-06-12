package ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(FIELD)
public @interface OptionalField {
	public YieldTable.Category category();
}
