package ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(FIELD)
public @interface OptionalField {
	/**
	 * The yield table category that this field will appear in
	 */
	public YieldTable.Category category() default YieldTable.Category.NONE;

	/**
	 * The yield table categories that this field will appear in. This overrules the category() value if set
	 */
	public YieldTable.Category[] categories() default {};
}
