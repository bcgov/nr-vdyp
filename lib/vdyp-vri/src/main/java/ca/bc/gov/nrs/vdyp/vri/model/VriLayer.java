package ca.bc.gov.nrs.vdyp.vri.model;

import java.util.Collection;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import ca.bc.gov.nrs.vdyp.common.Computed;
import ca.bc.gov.nrs.vdyp.model.BaseVdypLayer;
import ca.bc.gov.nrs.vdyp.model.BaseVdypSite;
import ca.bc.gov.nrs.vdyp.model.BaseVdypSpecies;
import ca.bc.gov.nrs.vdyp.model.InputLayer;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.PolygonIdentifier;
import ca.bc.gov.nrs.vdyp.model.UtilizationClass;

public class VriLayer extends BaseVdypLayer<VriSpecies, VriSite> implements InputLayer {

	private final float crownClosure; // VRIL/CCL
	private final Optional<Float> baseArea; // VRIL/BAL
	private final Optional<Float> treesPerHectare; // VRIL/TPHL
	private final float utilization; // VRIL/UTLL
	public final Optional<String> primaryGenus; // FIPL_1C/JPRIME_L1 ISPP
	private final Optional<String> secondaryGenus; // FIPL_1C/JPRIME_L1 ISPS
	private final Optional<Integer> empiricalRelationshipParameterIndex; // INXL1/GRPBA1
	private final float ageIncrease; // YOUNG1/AGE_INCR

	public VriLayer(
			PolygonIdentifier polygonIdentifier, LayerType layer, float crownClosure, Optional<Float> baseArea,
			Optional<Float> treesPerHectare, float utilization, Optional<String> primaryGenus,
			Optional<String> secondaryGenus, Optional<Integer> empiricalRelationshipParameterIndex, float ageIncrease
	) {
		super(polygonIdentifier, layer, Optional.empty());
		this.crownClosure = crownClosure;
		this.baseArea = baseArea;
		this.treesPerHectare = treesPerHectare;
		this.utilization = utilization;
		this.primaryGenus = primaryGenus;
		this.secondaryGenus = secondaryGenus;
		this.empiricalRelationshipParameterIndex = empiricalRelationshipParameterIndex;
		this.ageIncrease = ageIncrease;
	}

	@Override
	public float getCrownClosure() {
		return crownClosure;
	}

	public Optional<Float> getBaseArea() {
		return baseArea;
	}

	public Optional<Float> getTreesPerHectare() {
		return treesPerHectare;
	}

	public float getUtilization() {
		return utilization;
	}

	@Override
	public Optional<String> getPrimaryGenus() {
		return primaryGenus;
	}

	public Optional<String> getSecondaryGenus() {
		return secondaryGenus;
	}

	@Computed
	public Optional<VriSpecies> getSecondarySpeciesRecord() {
		return secondaryGenus.map(this.getSpecies()::get);
	}

	@Computed
	public Optional<VriSite> getSecondarySite() {
		return secondaryGenus.map(this.getSites()::get);
	}

	public Optional<Integer> getEmpiricalRelationshipParameterIndex() {
		return empiricalRelationshipParameterIndex;
	}

	public float getAgeIncrease() {
		return this.ageIncrease;
	};

	/**
	 * Accepts a configuration function that accepts a builder to configure.
	 *
	 * @param config The configuration function
	 * @return The object built by the configured builder.
	 * @throws IllegalStateException if any required properties have not been set by the configuration function.
	 */

	public static VriLayer build(Consumer<Builder> config) {
		var builder = new Builder();
		config.accept(builder);
		return builder.build();
	}

	public static InputLayer build(VriPolygon polygon, Consumer<Builder> config) {
		var layer = build(builder -> {
			builder.polygonIdentifier(polygon.getPolygonIdentifier());
			config.accept(builder);
		});
		polygon.getLayers().put(layer.getLayerType(), layer);
		return layer;
	}

	public static class Builder
			extends BaseVdypLayer.Builder<VriLayer, VriSpecies, VriSite, VriSpecies.Builder, VriSite.Builder> {

		private static final float DEFAULT_AGE_INCREASE = 0;

		protected Optional<Float> crownClosure = Optional.empty();
		protected Optional<Float> baseArea = Optional.empty();
		protected Optional<Float> treesPerHectare = Optional.empty();
		protected Optional<Float> utilization = Optional.empty();
		protected Optional<Float> percentAvailable = Optional.empty();
		protected Optional<String> primaryGenus = Optional.empty();
		protected Optional<String> secondaryGenus = Optional.empty();
		protected Optional<Integer> empiricalRelationshipParameterIndex = Optional.empty();
		protected Optional<Float> ageIncrease = Optional.empty();

		public Builder empiricalRelationshipParameterIndex(Optional<Integer> empiricalRelationshipParameterIndex) {
			this.empiricalRelationshipParameterIndex = empiricalRelationshipParameterIndex;
			return this;
		}

		public Builder empiricalRelationshipParameterIndex(int empiricalRelationshipParameterIndex) {
			return this.empiricalRelationshipParameterIndex(Optional.of(empiricalRelationshipParameterIndex));
		}

		public Builder crownClosure(float crownClosure) {
			this.crownClosure = Optional.of(crownClosure);
			return this;
		}

		public Builder baseArea(Optional<Float> baseArea) {
			this.baseArea = baseArea;
			return this;
		}

		public Builder treesPerHectare(Optional<Float> treesPerHectare) {
			this.treesPerHectare = treesPerHectare;
			return this;
		}

		public Builder baseArea(float baseArea) {
			return baseArea(Optional.of(baseArea));
		}

		public Builder treesPerHectare(float treesPerHectare) {
			return treesPerHectare(Optional.of(treesPerHectare));
		}

		public Builder utilization(float utilization) {
			this.utilization = Optional.of(utilization);
			return this;
		}

		public Builder percentAvailable(Optional<Float> percentAvailable) {
			this.percentAvailable = percentAvailable;
			return this;
		}

		public Builder percentAvailable(float percentAvailable) {
			return percentAvailable(Optional.of(percentAvailable));
		}

		public Optional<Float> getBaseArea() {
			return baseArea;
		}

		public Optional<Float> getTreesPerHectare() {
			return treesPerHectare;
		}

		public Optional<Float> getCrownClosure() {
			return crownClosure;
		}

		public Builder primaryGenus(Optional<String> primaryGenus) {
			this.primaryGenus = primaryGenus;
			return this;
		}

		public Builder primaryGenus(String primaryGenus) {
			return primaryGenus(Optional.of(primaryGenus));
		}

		public Builder secondaryGenus(Optional<String> secondaryGenus) {
			this.secondaryGenus = secondaryGenus;
			return this;
		}

		public Builder secondaryGenus(String secondaryGenus) {
			return secondaryGenus(Optional.of(secondaryGenus));
		}

		public Builder ageIncrease(float ageIncrease) {
			this.ageIncrease = Optional.of(ageIncrease);
			return this;
		}

		@Override
		protected void check(Collection<String> errors) {
			super.check(errors);
			requirePresent(crownClosure, "crownClosure", errors);
			requirePresent(utilization, "utilization", errors);
		}

		@Override
		protected VriLayer doBuild() {
			// We want the reciprocal of the forest fraction.  When we output at the other end the derived values are multiplied by the forest fraction canceling this out.
			float multiplier = 100f / percentAvailable.orElse(100f);
			VriLayer result = new VriLayer(
					polygonIdentifier.get(), //
					layerType.get(), //
					crownClosure.get(), //
					baseArea.map(x -> x * multiplier), //
					treesPerHectare.map(x -> x * multiplier), //
					Math.max(utilization.get(), UtilizationClass.U75TO125.lowBound), //
					primaryGenus, //
					secondaryGenus, //
					empiricalRelationshipParameterIndex, //
					ageIncrease.orElse(DEFAULT_AGE_INCREASE)
			);
			result.setInventoryTypeGroup(inventoryTypeGroup);
			return result;
		}

		@Override
		protected VriSpecies buildSpecies(Consumer<VriSpecies.Builder> config) {
			return VriSpecies.build(builder -> {
				config.accept(builder);
				builder.polygonIdentifier(this.polygonIdentifier.get());
				builder.layerType(layerType.get());
			});
		}

		@Override
		public Builder copy(VriLayer toCopy) {
			super.copy(toCopy);
			this.baseArea(toCopy.getBaseArea());
			this.crownClosure(toCopy.getCrownClosure());
			this.utilization(toCopy.getUtilization());
			this.primaryGenus(toCopy.getPrimaryGenus());
			this.secondaryGenus(toCopy.getSecondaryGenus());
			this.empiricalRelationshipParameterIndex(toCopy.getEmpiricalRelationshipParameterIndex());
			return this;
		}

		@Override
		public <S2 extends BaseVdypSpecies<I2>, I2 extends BaseVdypSite>
				BaseVdypLayer.Builder<VriLayer, VriSpecies, VriSite, VriSpecies.Builder, VriSite.Builder>
				adaptSpecies(BaseVdypLayer<S2, ?> toCopy, BiConsumer<VriSpecies.Builder, S2> config) {
			this.primaryGenus(toCopy.getPrimaryGenus());
			return super.adaptSpecies(toCopy, config);
		}

		@Override
		public BaseVdypLayer.Builder<VriLayer, VriSpecies, VriSite, VriSpecies.Builder, VriSite.Builder>
				copySpecies(VriLayer toCopy, BiConsumer<VriSpecies.Builder, VriSpecies> config) {
			this.primaryGenus(toCopy.getPrimaryGenus());
			return super.copySpecies(toCopy, config);
		}

	}

}
