package ca.bc.gov.nrs.vdyp.model;

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

	private Optional<VdypCompatibilityVariables> compatibilityVariables = Optional.empty();

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

		this.compatibilityVariables = Optional.of(VdypCompatibilityVariables.build(this, cvb -> {

			cvb.cvVolume(cvVolume);
			cvb.cvBasalArea(cvBasalArea);
			cvb.cvQuadraticMeanDiameter(cvQuadraticMeanDiameter);
			cvb.cvPrimaryLayerSmall(cvPrimaryLayerSmall);

		}));
	}

	public Optional<VdypCompatibilityVariables> getCompatibilityVariables() {
		return this.compatibilityVariables;
	}

	/**
	 * @return the compatibility variables if they are present
	 * @throws InitializationIncompleteException if there are no compatibility variables
	 */
	public VdypCompatibilityVariables requireCompatibilityVariables() throws InitializationIncompleteException {
		return getCompatibilityVariables().orElseThrow(
				() -> new InitializationIncompleteException(
						MessageFormat.format("Species {0}: compatibilityVariables", this)
				)
		);
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

	public static class Builder extends BaseVdypSpecies.Builder<VdypSpecies, VdypSite, VdypSite.Builder>
			implements VdypUtilizationHolder.Builder {
		protected Optional<Integer> volumeGroup = Optional.empty();
		protected Optional<Integer> decayGroup = Optional.empty();
		protected Optional<Integer> breakageGroup = Optional.empty();

		protected UtilizationVector loreyHeight = VdypUtilizationHolder.emptyLoreyHeightUtilization();

		@Override
		public void loreyHeight(UtilizationVector vector) {
			this.loreyHeight = vector;
		}

		protected UtilizationVector baseArea = VdypUtilizationHolder.emptyUtilization();

		@Override
		public void baseArea(UtilizationVector vector) {
			this.baseArea = vector;
		}

		protected UtilizationVector treesPerHectare = VdypUtilizationHolder.emptyUtilization();

		@Override
		public void treesPerHectare(UtilizationVector vector) {
			this.treesPerHectare = vector;
		}

		protected UtilizationVector quadMeanDiameter = VdypUtilizationHolder.emptyUtilization();

		@Override
		public void quadMeanDiameter(UtilizationVector vector) {
			this.quadMeanDiameter = vector;
		}

		protected UtilizationVector wholeStemVolume = VdypUtilizationHolder.emptyUtilization();

		@Override
		public void wholeStemVolume(UtilizationVector vector) {
			this.wholeStemVolume = vector;
		}

		protected UtilizationVector closeUtilizationVolumeByUtilization = VdypUtilizationHolder.emptyUtilization();

		@Override
		public void closeUtilizationVolumeByUtilization(UtilizationVector vector) {
			this.closeUtilizationVolumeByUtilization = vector;
		}

		protected UtilizationVector closeUtilizationNetVolumeOfDecayByUtilization = VdypUtilizationHolder
				.emptyUtilization();

		@Override
		public void closeUtilizationVolumeNetOfDecayByUtilization(UtilizationVector vector) {
			this.closeUtilizationNetVolumeOfDecayByUtilization = vector;
		}

		protected UtilizationVector closeUtilizationVolumeNetOfDecayAndWasteByUtilization = VdypUtilizationHolder
				.emptyUtilization();

		@Override
		public void closeUtilizationVolumeNetOfDecayAndWasteByUtilization(UtilizationVector vector) {
			this.closeUtilizationVolumeNetOfDecayAndWasteByUtilization = vector;
		}

		protected UtilizationVector closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization = VdypUtilizationHolder
				.emptyUtilization();

		private Optional<Consumer<VdypCompatibilityVariables.Builder>> compatibilityVariablesBuilder;
		private Optional<VdypCompatibilityVariables> compatibilityVariables;

		@Override
		public void closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(UtilizationVector vector) {
			this.closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization = vector;
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
				builder.genus(genus);
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

		public void addCompatibilityVariables(Consumer<VdypCompatibilityVariables.Builder> config) {
			this.compatibilityVariablesBuilder = Optional.of(config);
			this.compatibilityVariables = Optional.empty();
		}

		public void addCompatibilityVariables(VdypCompatibilityVariables cvs) {
			this.addCompatibilityVariables(Optional.of(cvs));
		}

		public void addCompatibilityVariables(Optional<VdypCompatibilityVariables> cvs) {
			this.compatibilityVariablesBuilder = Optional.empty();
			this.compatibilityVariables = cvs;
		}

		@Override
		protected void preProcess() {
			super.preProcess();

			compatibilityVariables = compatibilityVariablesBuilder.map(this::buildCompatibilityVariables).or(
					() -> compatibilityVariables
			);

		}

		protected VdypCompatibilityVariables buildCompatibilityVariables(
				Consumer<VdypCompatibilityVariables.Builder> config
		) {
			return VdypCompatibilityVariables.build(builder -> {
				config.accept(builder);
				builder.polygonIdentifier(polygonIdentifier.get());
				builder.layerType(layerType.get());
				builder.genus(genus);
			});
		}

	}
}
