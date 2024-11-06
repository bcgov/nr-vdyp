package ca.bc.gov.nrs.vdyp.application.test;

import java.util.Optional;
import java.util.function.Consumer;

import ca.bc.gov.nrs.vdyp.model.BaseVdypSpecies;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.PolygonIdentifier;
import ca.bc.gov.nrs.vdyp.model.Sp64DistributionSet;

public class TestSpecies extends BaseVdypSpecies<TestSite> {

	protected TestSpecies(
			PolygonIdentifier polygonIdentifier, LayerType layerType, String genus, int genusIndex, float percentGenus,
			Optional<TestSite> site
	) {
		super(
				polygonIdentifier, layerType, genus, genusIndex, Optional.of(percentGenus), new Sp64DistributionSet(),
				site
		);
	}

	public static TestSpecies build(Consumer<TestSpecies.Builder> config) {
		var builder = new Builder();
		config.accept(builder);
		return builder.build();
	}

	public static class Builder extends BaseVdypSpecies.Builder<TestSpecies, TestSite, TestSite.Builder> {

		@Override
		protected TestSpecies doBuild() {
			return new TestSpecies(
					polygonIdentifier.get(), layerType.get(), genus.get(), genusIndex.get(), percentGenus.get(), site
			);
		}

		@Override
		protected TestSite buildSite(Consumer<TestSite.Builder> config) {
			return TestSite.build(builder -> {
				builder.polygonIdentifier(this.polygonIdentifier.get());
				builder.layerType(this.layerType.get());
				builder.genus(this.genus.get());

				config.accept(builder);
			});
		}

	}
}