package ca.bc.gov.nrs.vdyp.vri.model;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;

import ca.bc.gov.nrs.vdyp.model.BaseVdypSite;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.PolygonIdentifier;

public class VriSite extends BaseVdypSite {

	private final String siteSpecies; // VRISIA/VR_SP64

	public VriSite(
			PolygonIdentifier polygonIdentifier, LayerType layer, String siteGenus, Optional<Integer> siteCurveNumber,
			Optional<Float> siteIndex, Optional<Float> height, Optional<Float> ageTotal,
			Optional<Float> yearsToBreastHeight, String siteSpecies, Optional<Float> breastHeightAge
	) {
		super(
				polygonIdentifier, layer, siteGenus, siteCurveNumber, siteIndex, height, ageTotal, yearsToBreastHeight,
				breastHeightAge
		);
		this.siteSpecies = siteSpecies;
	}

	public String getSiteSpecies() {
		return siteSpecies;
	}

	/**
	 * Accepts a configuration function that accepts a builder to configure.
	 *
	 * @param config The configuration function
	 * @return The object built by the configured builder.
	 * @throws IllegalStateException if any required properties have not been set by the configuration function.
	 */
	public static VriSite build(Consumer<Builder> config) {
		var builder = new Builder();
		config.accept(builder);
		return builder.build();
	}

	public static class Builder extends BaseVdypSite.Builder<VriSite> {

		protected Optional<String> siteSpecies = Optional.empty();

		public Builder siteSpecies(Optional<String> siteSpecies) {
			this.siteSpecies = siteSpecies;
			return this;
		}

		public Builder siteSpecies(String string) {
			return siteSpecies(Optional.of(string));
		}

		@Override
		protected void check(Collection<String> errors) {
			super.check(errors);
			requirePresent(siteSpecies, "siteSpecies", errors);
		}

		@Override
		public Builder copy(VriSite toCopy) {
			super.copy(toCopy);
			siteSpecies(toCopy.getSiteSpecies());
			return this;
		}

		@Override
		protected VriSite doBuild() {
			return new VriSite(
					this.polygonIdentifier.get(), //
					this.layerType.get(), //
					this.siteGenus.get(), //
					this.siteCurveNumber, //
					this.siteIndex, //
					this.height, //
					this.ageTotal, //
					this.yearsToBreastHeight, //
					this.siteSpecies.get(), //
					this.yearsAtBreastHeight
			);
		}
	}

}
