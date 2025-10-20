package ca.bc.gov.nrs.vdyp.model;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Consumer;

import ca.bc.gov.nrs.vdyp.application.InitializationIncompleteException;
import ca.bc.gov.nrs.vdyp.common.Utils;

public class VdypSpecies extends BaseVdypSpecies<VdypSite> implements VdypUtilizationHolder {

	private UtilizationVector baseAreaByUtilization = Utils.utilizationVector(); // LVCOM/BA
	private UtilizationVector loreyHeightByUtilization = Utils.heightVector(); // LVCOM/HL
	private UtilizationVector quadraticMeanDiameterByUtilization = Utils.utilizationVector(); // LVCOM/DQ
	private UtilizationVector treesPerHectareByUtilization = Utils.utilizationVector(); // LVCOM/TPH

	private UtilizationVector wholeStemVolumeByUtilization = Utils.utilizationVector(); // LVCOM/VOLWS
	private UtilizationVector closeUtilizationVolumeByUtilization = Utils.utilizationVector(); // LVCOM/VOLCU
	private UtilizationVector closeUtilizationVolumeNetOfDecayByUtilization = Utils.utilizationVector(); // LVCOM/VOL_D
	private UtilizationVector closeUtilizationVolumeNetOfDecayAndWasteByUtilization = Utils.utilizationVector(); // LVCOM/VOL_DW
	private UtilizationVector closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization = Utils.utilizationVector(); // LVCOM/VOL_DWB

	private Optional<Integer> volumeGroup;
	private Optional<Integer> decayGroup;
	private Optional<Integer> breakageGroup;

	// Compatibility Variables

	private Optional<MatrixMap3<UtilizationClass, VolumeVariable, LayerType, Float>> cvVolume = Optional.empty();
	private Optional<MatrixMap2<UtilizationClass, LayerType, Float>> cvBasalArea = Optional.empty();
	private Optional<MatrixMap2<UtilizationClass, LayerType, Float>> cvQuadraticMeanDiameter = Optional.empty();
	private Optional<Map<UtilizationClassVariable, Float>> cvPrimaryLayerSmall = Optional.empty();

	public VdypSpecies(
			PolygonIdentifier polygonIdentifier, LayerType layer, String genus, int genusIndex,
			Optional<Float> percentGenus, Sp64DistributionSet sp64DistributionSet, Optional<VdypSite> site,
			Optional<Integer> volumeGroup, Optional<Integer> decayGroup, Optional<Integer> breakageGroup
	) {
		super(polygonIdentifier, layer, genus, genusIndex, percentGenus, sp64DistributionSet, site);
		this.volumeGroup = volumeGroup;
		this.decayGroup = decayGroup;
		this.breakageGroup = breakageGroup;
	}

	/**
	 * Base area for utilization index -1 through 4
	 */
	@Override
	public UtilizationVector getBaseAreaByUtilization() {
		return baseAreaByUtilization;
	}

	/**
	 * Base area for utilization index -1 through 4
	 */
	@Override
	public void setBaseAreaByUtilization(UtilizationVector baseAreaByUtilization) {
		this.baseAreaByUtilization = baseAreaByUtilization;
	}

	/**
	 * Lorey height for utilization index -1 through 0
	 */
	@Override
	public UtilizationVector getLoreyHeightByUtilization() {
		return loreyHeightByUtilization;
	}

	/**
	 * Lorey height for utilization index -1 through 0
	 */
	@Override
	public void setLoreyHeightByUtilization(UtilizationVector loreyHeightByUtilization) {
		this.loreyHeightByUtilization = loreyHeightByUtilization;
	}

	public int getVolumeGroup() {
		return volumeGroup.orElseThrow(
				() -> new NoSuchElementException(MessageFormat.format("Species {0} volumeGroup", toString()))
		);
	}

	public void setVolumeGroup(int volumeGroup) {
		if (this.volumeGroup.isPresent()) {
			throw new IllegalStateException(MessageFormat.format("Species {0} volumeGroup is already set", toString()));
		}

		this.volumeGroup = Optional.of(volumeGroup);
	}

	public int getDecayGroup() {
		return decayGroup.orElseThrow(
				() -> new NoSuchElementException(MessageFormat.format("Species {0} decayGroup", toString()))
		);
	}

	public void setDecayGroup(int decayGroup) {
		if (this.decayGroup.isPresent()) {
			throw new IllegalStateException(MessageFormat.format("Species {0} decayGroup is already set", toString()));
		}

		this.decayGroup = Optional.of(decayGroup);
	}

	public int getBreakageGroup() {
		return breakageGroup.orElseThrow(
				() -> new NoSuchElementException(MessageFormat.format("Species {0} breakageGroup", toString()))
		);
	}

	public void setBreakageGroup(int breakageGroup) {
		if (this.breakageGroup.isPresent()) {
			throw new IllegalStateException(
					MessageFormat.format("Species {0} breakageGroup is already set", toString())
			);
		}

		this.breakageGroup = Optional.of(breakageGroup);
	}

	@Override
	public UtilizationVector getQuadraticMeanDiameterByUtilization() {
		return quadraticMeanDiameterByUtilization;
	}

	@Override
	public void setQuadraticMeanDiameterByUtilization(UtilizationVector quadraticMeanDiameterByUtilization) {
		this.quadraticMeanDiameterByUtilization = quadraticMeanDiameterByUtilization;
	}

	@Override
	public UtilizationVector getTreesPerHectareByUtilization() {
		return treesPerHectareByUtilization;
	}

	@Override
	public void setTreesPerHectareByUtilization(UtilizationVector treesPerHectareByUtilization) {
		this.treesPerHectareByUtilization = treesPerHectareByUtilization;
	}

	@Override
	public UtilizationVector getWholeStemVolumeByUtilization() {
		return wholeStemVolumeByUtilization;
	}

	@Override
	public void setWholeStemVolumeByUtilization(UtilizationVector wholeStemVolumeByUtilization) {
		this.wholeStemVolumeByUtilization = wholeStemVolumeByUtilization;
	}

	@Override
	public UtilizationVector getCloseUtilizationVolumeByUtilization() {
		return closeUtilizationVolumeByUtilization;
	}

	@Override
	public void setCloseUtilizationVolumeByUtilization(UtilizationVector closeUtilizationVolumeByUtilization) {
		this.closeUtilizationVolumeByUtilization = closeUtilizationVolumeByUtilization;
	}

	@Override
	public UtilizationVector getCloseUtilizationVolumeNetOfDecayByUtilization() {
		return closeUtilizationVolumeNetOfDecayByUtilization;
	}

	@Override
	public void setCloseUtilizationVolumeNetOfDecayByUtilization(
			UtilizationVector closeUtilizationNetVolumeOfDecayByUtilization
	) {
		this.closeUtilizationVolumeNetOfDecayByUtilization = closeUtilizationNetVolumeOfDecayByUtilization;
	}

	@Override
	public UtilizationVector getCloseUtilizationVolumeNetOfDecayAndWasteByUtilization() {
		return closeUtilizationVolumeNetOfDecayAndWasteByUtilization;
	}

	@Override
	public void setCloseUtilizationVolumeNetOfDecayAndWasteByUtilization(
			UtilizationVector closeUtilizationVolumeNetOfDecayAndWasteByUtilization
	) {
		this.closeUtilizationVolumeNetOfDecayAndWasteByUtilization = closeUtilizationVolumeNetOfDecayAndWasteByUtilization;
	}

	@Override
	public UtilizationVector getCloseUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization() {
		return closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization;
	}

	@Override
	public void setCloseUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(
			UtilizationVector closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization
	) {
		this.closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization = closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization;
	}

	public void setCompatibilityVariables(
			MatrixMap3<UtilizationClass, VolumeVariable, LayerType, Float> cvVolume,
			MatrixMap2<UtilizationClass, LayerType, Float> cvBasalArea,
			MatrixMap2<UtilizationClass, LayerType, Float> cvQuadraticMeanDiameter,
			Map<UtilizationClassVariable, Float> cvPrimaryLayerSmall
	) {

		this.cvVolume = Optional.of(cvVolume);
		this.cvBasalArea = Optional.of(cvBasalArea);
		this.cvQuadraticMeanDiameter = Optional.of(cvQuadraticMeanDiameter);
		this.cvPrimaryLayerSmall = Optional.of(cvPrimaryLayerSmall);
	}

	public float getCvVolume(UtilizationClass uc, VolumeVariable vv, LayerType lt) {
		if (cvVolume.isEmpty()) {
			throw new InitializationIncompleteException(MessageFormat.format("Species {0}: cvVolume", this));
		}
		return cvVolume.get().get(uc, vv, lt);
	}

	public float getCvBasalArea(UtilizationClass uc, LayerType lt) {
		if (cvBasalArea.isEmpty()) {
			throw new InitializationIncompleteException(MessageFormat.format("Species {0}: cvBasalArea", this));
		}
		return cvBasalArea.get().get(uc, lt);
	}

	public float getCvQuadraticMeanDiameter(UtilizationClass uc, LayerType lt) {
		if (cvQuadraticMeanDiameter.isEmpty()) {
			throw new InitializationIncompleteException(
					MessageFormat.format("Species {0}: cvQuadraticMeanDiameter", this)
			);
		}
		return cvQuadraticMeanDiameter.get().get(uc, lt);
	}

	public float getCvPrimaryLayerSmall(UtilizationClassVariable ucv) {
		if (cvPrimaryLayerSmall.isEmpty()) {
			throw new InitializationIncompleteException(MessageFormat.format("Species {0}: cvPrimaryLayerSmall", this));
		}
		return cvPrimaryLayerSmall.get().get(ucv);
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
	public static VdypSpecies build(Consumer<Builder> config) {
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
	public static VdypSpecies build(VdypLayer layer, Consumer<Builder> config) {
		var result = build(builder -> {
			builder.polygonIdentifier(layer.getPolygonIdentifier());
			builder.layerType(layer.getLayerType());

			config.accept(builder);
		});
		layer.getSpecies().put(result.getGenus(), result);
		return result;
	}

	public static class Builder extends BaseVdypSpecies.Builder<VdypSpecies, VdypSite, VdypSite.Builder> {
		protected Optional<Integer> volumeGroup = Optional.empty();
		protected Optional<Integer> decayGroup = Optional.empty();
		protected Optional<Integer> breakageGroup = Optional.empty();

		protected UtilizationVector loreyHeight = VdypUtilizationHolder.emptyLoreyHeightUtilization();

		public void loreyHeight(float height) {
			this.loreyHeight = Utils.heightVector(0, height);
		}

		public void loreyHeight(float small, float height) {
			this.loreyHeight = Utils.heightVector(small, height);
		}

		protected UtilizationVector baseArea = VdypUtilizationHolder.emptyUtilization();

		public void baseArea(float small, float u1, float u2, float u3, float u4) {
			this.baseArea = Utils.utilizationVector(small, u1, u2, u3, u4);
		}

		public void baseArea(float baseArea) {
			this.baseArea = Utils.utilizationVector(baseArea);
		}

		protected UtilizationVector treesPerHectare = VdypUtilizationHolder.emptyUtilization();

		public void treesPerHectare(float small, float u1, float u2, float u3, float u4) {
			this.treesPerHectare = Utils.utilizationVector(small, u1, u2, u3, u4);
		}

		public void treesPerHectare(float height) {
			this.treesPerHectare = Utils.utilizationVector(height);
		}

		protected UtilizationVector quadMeanDiameter = VdypUtilizationHolder.emptyUtilization();

		public void quadMeanDiameter(float small, float uAll, float u1, float u2, float u3, float u4) {
			this.quadMeanDiameter = Utils.utilizationVector(small, uAll, u1, u2, u3, u4);
		}

		public void quadMeanDiameter(float height) {
			this.quadMeanDiameter = Utils.utilizationVector(height);
		}

		protected UtilizationVector wholeStemVolume = VdypUtilizationHolder.emptyUtilization();

		public void wholeStemVolume(float small, float u1, float u2, float u3, float u4) {
			this.wholeStemVolume = Utils.utilizationVector(small, u1, u2, u3, u4);
		}

		public void wholeStemVolume(float volume) {
			this.wholeStemVolume = Utils.utilizationVector(volume);
		}

		protected UtilizationVector closeUtilizationVolumeByUtilization = VdypUtilizationHolder.emptyUtilization();

		public void closeUtilizationVolumeByUtilization(float small, float u1, float u2, float u3, float u4) {
			this.closeUtilizationVolumeByUtilization = Utils.utilizationVector(small, u1, u2, u3, u4);
		}

		public void closeUtilizationVolumeByUtilization(float volume) {
			this.closeUtilizationVolumeByUtilization = Utils.utilizationVector(volume);
		}

		protected UtilizationVector closeUtilizationNetVolumeOfDecayByUtilization = VdypUtilizationHolder
				.emptyUtilization();

		public void closeUtilizationVolumeNetOfDecayByUtilization(float small, float u1, float u2, float u3, float u4) {
			this.closeUtilizationNetVolumeOfDecayByUtilization = Utils.utilizationVector(small, u1, u2, u3, u4);
		}

		public void closeUtilizationVolumeNetOfDecayByUtilization(float volume) {
			this.closeUtilizationNetVolumeOfDecayByUtilization = Utils.utilizationVector(volume);
		}

		protected UtilizationVector closeUtilizationVolumeNetOfDecayAndWasteByUtilization = VdypUtilizationHolder
				.emptyUtilization();

		public void closeUtilizationVolumeNetOfDecayAndWasteByUtilization(
				float small, float u1, float u2, float u3, float u4
		) {
			this.closeUtilizationVolumeNetOfDecayAndWasteByUtilization = Utils.utilizationVector(small, u1, u2, u3, u4);
		}

		public void closeUtilizationVolumeNetOfDecayAndWasteByUtilization(float volume) {
			this.closeUtilizationVolumeNetOfDecayAndWasteByUtilization = Utils.utilizationVector(volume);
		}

		protected UtilizationVector closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization = VdypUtilizationHolder
				.emptyUtilization();

		public void closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(
				float small, float u1, float u2, float u3, float u4
		) {
			this.closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization = Utils
					.utilizationVector(small, u1, u2, u3, u4);
		}

		public void closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(float volume) {
			this.closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization = Utils.utilizationVector(volume);
		}

		@Override
		protected void check(Collection<String> errors) {
			super.check(errors);
		}

		@Override
		public VdypSpecies.Builder adapt(BaseVdypSpecies<?> baseSource) {
			super.adapt(baseSource);

			if (baseSource instanceof VdypSpecies source) {
				loreyHeight = new UtilizationVector(source.loreyHeightByUtilization);
				baseArea = new UtilizationVector(source.baseAreaByUtilization);
				treesPerHectare = new UtilizationVector(source.treesPerHectareByUtilization);
				quadMeanDiameter = new UtilizationVector(source.quadraticMeanDiameterByUtilization);
				wholeStemVolume = new UtilizationVector(source.wholeStemVolumeByUtilization);
				closeUtilizationVolumeByUtilization = new UtilizationVector(source.closeUtilizationVolumeByUtilization);
				closeUtilizationNetVolumeOfDecayByUtilization = new UtilizationVector(
						source.closeUtilizationVolumeNetOfDecayByUtilization
				);
				closeUtilizationVolumeNetOfDecayAndWasteByUtilization = new UtilizationVector(
						source.closeUtilizationVolumeNetOfDecayAndWasteByUtilization
				);
				closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization = new UtilizationVector(
						source.closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization
				);
			}

			return this;
		}

		@Override
		public Builder copy(VdypSpecies source) {
			super.copy(source);

			source.volumeGroup.ifPresent(this::volumeGroup);
			source.decayGroup.ifPresent(this::decayGroup);
			source.breakageGroup.ifPresent(this::breakageGroup);

			return this;
		}

		@Override
		protected void postProcess(VdypSpecies spec) {
			super.postProcess(spec);

			spec.setLoreyHeightByUtilization(loreyHeight);
			spec.setBaseAreaByUtilization(baseArea);
			spec.setTreesPerHectareByUtilization(treesPerHectare);
			spec.setQuadraticMeanDiameterByUtilization(quadMeanDiameter);
			spec.setWholeStemVolumeByUtilization(wholeStemVolume);
			spec.setCloseUtilizationVolumeByUtilization(closeUtilizationVolumeByUtilization);
			spec.setCloseUtilizationVolumeNetOfDecayByUtilization(closeUtilizationNetVolumeOfDecayByUtilization);
			spec.setCloseUtilizationVolumeNetOfDecayAndWasteByUtilization(
					closeUtilizationVolumeNetOfDecayAndWasteByUtilization
			);
			spec.setCloseUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(
					closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization
			);
			;
		}

		@Override
		protected VdypSpecies doBuild() {

			return new VdypSpecies(
					polygonIdentifier.get(), //
					layerType.get(), //
					genus.get(), //
					genusIndex.get(), //
					percentGenus, //
					sp64DistributionSet, //
					site, //
					volumeGroup, //
					decayGroup, //
					breakageGroup //
			);
		}

		@Override
		protected VdypSite buildSite(Consumer<VdypSite.Builder> config) {
			return VdypSite.build(builder -> {
				config.accept(builder);
				builder.polygonIdentifier(polygonIdentifier.get());
				builder.layerType(layerType.get());
				builder.siteGenus(genus);
			});
		}

		public Builder volumeGroup(int i) {
			this.volumeGroup = Optional.of(i);
			return this;
		}

		public Builder decayGroup(int i) {
			this.decayGroup = Optional.of(i);
			return this;
		}

		public Builder breakageGroup(int i) {
			this.breakageGroup = Optional.of(i);
			return this;
		}
	}

	@Override
	public void dumpState(Appendable output, int indent) throws IOException {
		super.dumpState(output, indent);

		Dumpable.writeProperty(output, indent + 1, "volumeGroup", volumeGroup);
		Dumpable.writeProperty(output, indent + 1, "decayGroup", decayGroup);
		Dumpable.writeProperty(output, indent + 1, "breakageGroup", breakageGroup);

		Dumpable.writeProperty(output, indent + 1, "baseAreaByUtilization", baseAreaByUtilization);
		Dumpable.writeProperty(output, indent + 1, "loreyHeightByUtilization", loreyHeightByUtilization);
		Dumpable.writeProperty(
				output, indent + 1, "quadraticMeanDiameterByUtilization", quadraticMeanDiameterByUtilization
		);
		Dumpable.writeProperty(output, indent + 1, "treesPerHectareByUtilization", treesPerHectareByUtilization);
		Dumpable.writeProperty(output, indent + 1, "wholeStemVolumeByUtilization", wholeStemVolumeByUtilization);
		Dumpable.writeProperty(
				output, indent + 1, "closeUtilizationVolumeByUtilization", closeUtilizationVolumeByUtilization
		);
		Dumpable.writeProperty(
				output, indent + 1, "closeUtilizationVolumeNetOfDecayByUtilization",
				closeUtilizationVolumeNetOfDecayByUtilization
		);
		Dumpable.writeProperty(
				output, indent + 1, "closeUtilizationVolumeNetOfDecayAndWasteByUtilization",
				closeUtilizationVolumeNetOfDecayAndWasteByUtilization
		);
		Dumpable.writeProperty(
				output, indent + 1, "closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization",
				closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization
		);

		Dumpable.writeProperty(output, indent + 1, "cvVolume", cvVolume);
		Dumpable.writeProperty(output, indent + 1, "cvBasalArea", cvBasalArea);
		Dumpable.writeProperty(output, indent + 1, "cvQuadraticMeanDiameter", cvQuadraticMeanDiameter);
		Dumpable.writeProperty(output, indent + 1, "cvPrimaryLayerSmall", cvPrimaryLayerSmall);

	}
}
