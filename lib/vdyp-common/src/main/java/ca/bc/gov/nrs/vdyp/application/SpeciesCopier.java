package ca.bc.gov.nrs.vdyp.application;

import java.util.function.Consumer;

import ca.bc.gov.nrs.vdyp.model.BaseVdypSite;
import ca.bc.gov.nrs.vdyp.model.BaseVdypSpecies;

public interface SpeciesCopier<S extends BaseVdypSpecies<I>, I extends BaseVdypSite> {
	S copySpecies(S toCopy, Consumer<BaseVdypSpecies.Builder<S, I, ?>> config);
}
