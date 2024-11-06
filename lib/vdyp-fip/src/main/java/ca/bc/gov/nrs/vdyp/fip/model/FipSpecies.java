package ca.bc.gov.nrs.vdyp.fip.model;

import java.util.Optional;
import java.util.function.Consumer;

import ca.bc.gov.nrs.vdyp.model.BaseVdypSpecies;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.PolygonIdentifier;
import ca.bc.gov.nrs.vdyp.model.Sp64DistributionSet;

public class FipSpecies extends BaseVdypSpecies<FipSite> {

	public FipSpecies(
			PolygonIdentifier polygonIdentifier, LayerType layer, String genus, int genusIndex, float percentGenus,
			Sp64DistributionSet sp64DistributionSet, Optional<FipSite> site
	) {
		super(polygonIdentifier, layer, genus, genusIndex, Optional.of(percentGenus), sp64DistributionSet, site);
	}

	/**
	 * Accepts a configuration function that accepts a builder to configure.
	 *
	 * <pre>
	 * FipSpecies myLayer = FipSpecies.build(builder-&gt; {
			builder.polygonIdentifier(polygonId);
			builder.layerType(LayerType.VETERAN);
			builder.genus("B");
			builder.percentGenus(6f);
	 * })
	 * </pre>
	 *
	 * @param config The configuration function
	 * @return The object built by the configured builder.
	 * @throws IllegalStateException if any required properties have not been set by the configuration function.
	 */
	public static FipSpecies build(Consumer<Builder> config) {
		var builder = new Builder();
		config.accept(builder);
		return builder.build();
	}

	/**
	 * Builds a species and adds it to the layer.
	 *
	 * @param layer  Layer to create the species for.
	 * @param config Configuration function for the builder.
	 * @return the new species.
	 */
	public static FipSpecies build(FipLayer layer, Consumer<Builder> config) {
		var result = build(builder -> {
			builder.polygonIdentifier(layer.getPolygonIdentifier());
			builder.layerType(layer.getLayerType());

			config.accept(builder);
		});
		layer.getSpecies().put(result.getGenus(), result);
		return result;
	}

	public static class Builder extends BaseVdypSpecies.Builder<FipSpecies, FipSite, FipSite.Builder> {

		@Override
		protected FipSpecies doBuild() {
			return new FipSpecies(
					this.polygonIdentifier.get(), //
					this.layerType.get(), //
					this.genus.get(), //
					this.genusIndex.get(), //
					this.percentGenus.get(), //
					this.sp64DistributionSet, //
					this.site
			);
		}

		@Override
		protected FipSite buildSite(Consumer<FipSite.Builder> config) {
			return FipSite.build(builder -> {
				builder.genus(this.genus.get());
				builder.polygonIdentifier(this.polygonIdentifier.get());
				builder.layerType(this.layerType.get());
				config.accept(builder);
			});
		}
	}
}
