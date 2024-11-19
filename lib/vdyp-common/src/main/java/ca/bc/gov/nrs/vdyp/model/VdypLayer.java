package ca.bc.gov.nrs.vdyp.model;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import ca.bc.gov.nrs.vdyp.common.Computed;
import ca.bc.gov.nrs.vdyp.common.Utils;

public class VdypLayer extends BaseVdypLayer<VdypSpecies, VdypSite> implements VdypUtilizationHolder {

	private UtilizationVector baseAreaByUtilization = //
			VdypUtilizationHolder.emptyUtilization(); // LVCOM/BA species 0
	private UtilizationVector loreyHeightByUtilization = //
			VdypUtilizationHolder.emptyLoreyHeightUtilization(); // LVCOM/HL species 0
	private UtilizationVector quadraticMeanDiameterByUtilization = //
			VdypUtilizationHolder.emptyUtilization(); // LVCOM/DQ species 0
	private UtilizationVector treesPerHectareByUtilization = //
			VdypUtilizationHolder.emptyUtilization(); // LVCOM/TPH species 0

	private UtilizationVector wholeStemVolumeByUtilization = //
			VdypUtilizationHolder.emptyUtilization(); // LVCOM/VOLWS species 0
	private UtilizationVector closeUtilizationVolumeByUtilization = //
			VdypUtilizationHolder.emptyUtilization(); // LVCOM/VOLCU species/ 0
	private UtilizationVector closeUtilizationVolumeNetOfDecayByUtilization = //
			VdypUtilizationHolder.emptyUtilization(); // LVCOM/VOL_D species 0
	private UtilizationVector closeUtilizationVolumeNetOfDecayAndWasteByUtilization = //
			VdypUtilizationHolder.emptyUtilization(); // LVCOM/VOL_DW species 0
	private UtilizationVector closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization = //
			VdypUtilizationHolder.emptyUtilization(); // LVCOM/VOL_DWB species 0

	private Optional<Integer> empiricalRelationshipParameterIndex = Optional.empty(); // INXL1/GRPBA1
	private Optional<String> primarySp0;

	public VdypLayer(
			PolygonIdentifier polygonIdentifier, LayerType layer, Optional<Integer> inventoryTypeGroup,
			Optional<Integer> empiricalRelationshipParameterIndex, Optional<String> primarySp0
	) {
		super(polygonIdentifier, layer, inventoryTypeGroup);
		this.empiricalRelationshipParameterIndex = empiricalRelationshipParameterIndex;
		this.primarySp0 = primarySp0;
	}

	@Computed
	public Optional<Float> getBreastHeightAge() {
		return this.getAgeTotal().flatMap(at -> this.getYearsToBreastHeight().map(bha -> at - bha));
	}

	@Computed
	public Optional<Float> getAgeTotal() {
		return this.getPrimarySite().flatMap(BaseVdypSite::getAgeTotal);
	}

	@Computed
	public Optional<Float> getYearsToBreastHeight() {
		return this.getPrimarySite().flatMap(BaseVdypSite::getYearsToBreastHeight);
	}

	@Computed
	public Optional<Float> getHeight() {
		return this.getPrimarySite().flatMap(BaseVdypSite::getHeight);
	}

	@Override
	public UtilizationVector getBaseAreaByUtilization() {
		return baseAreaByUtilization;
	}

	@Override
	public void setBaseAreaByUtilization(UtilizationVector baseAreaByUtilization) {
		this.baseAreaByUtilization = baseAreaByUtilization;
	}

	@Override
	public UtilizationVector getLoreyHeightByUtilization() {
		return loreyHeightByUtilization;
	}

	@Override
	public void setLoreyHeightByUtilization(UtilizationVector loreyHeightByUtilization) {
		this.loreyHeightByUtilization = loreyHeightByUtilization;
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
			UtilizationVector closeUtilizationVolumeNetOfDecayByUtilization
	) {
		this.closeUtilizationVolumeNetOfDecayByUtilization = closeUtilizationVolumeNetOfDecayByUtilization;
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

	public Optional<Integer> getEmpiricalRelationshipParameterIndex() {
		return empiricalRelationshipParameterIndex;
	}

	public void setEmpiricalRelationshipParameterIndex(Optional<Integer> empiricalRelationshipParameterIndex) {
		this.empiricalRelationshipParameterIndex = empiricalRelationshipParameterIndex;
	}

	@Override
	public Optional<String> getPrimaryGenus() {
		return primarySp0;
	}

	/**
	 * Accepts a configuration function that accepts a builder to configure.
	 *
	 * <pre>
	 * VdypLayer myLayer = VdypLayer.build(builder-&gt; {
			builder.polygonIdentifier(polygonId);
			builder.layerType(LayerType.VETERAN);

			builder.crownClosure(0.9f);
	 * })
	 * </pre>
	 *
	 * @param config The configuration function
	 * @return The object built by the configured builder.
	 * @throws IllegalStateException if any required properties have not been set by the configuration function.
	 */
	public static VdypLayer build(Consumer<Builder> config) {
		var builder = new Builder();
		config.accept(builder);
		return builder.build();
	}

	/**
	 * Builds a layer and adds it to the polygon.
	 *
	 * @param layerType Layer to create the species for.
	 * @param config    Configuration function for the builder.
	 * @return the new species.
	 */
	public static VdypLayer build(VdypPolygon polygon, Consumer<Builder> config) {
		var result = build(builder -> {
			builder.polygonIdentifier(polygon.getPolygonIdentifier());

			config.accept(builder);
		});
		polygon.getLayers().put(result.getLayerType(), result);
		return result;
	}

	public static class Builder
			extends BaseVdypLayer.Builder<VdypLayer, VdypSpecies, VdypSite, VdypSpecies.Builder, VdypSite.Builder>
			implements VdypUtilizationHolder.Builder {

		Optional<Integer> empiricalRelationshipParameterIndex = Optional.empty();

		public void empiricalRelationshipParameterIndex(Optional<Integer> empiricalRelationshipParameterIndex) {
			this.empiricalRelationshipParameterIndex = empiricalRelationshipParameterIndex;
		}

		public void empiricalRelationshipParameterIndex(int empiricalRelationshipParameterIndex) {
			this.empiricalRelationshipParameterIndex(Optional.of(empiricalRelationshipParameterIndex));
		}

		protected Optional<String> primarySp0 = Optional.empty();

		public VdypLayer.Builder primaryGenus(Optional<String> primarySp0) {
			this.primarySp0 = primarySp0;
			return this;
		}

		public VdypLayer.Builder primaryGenus(String primarySp0) {
			return primaryGenus(Optional.of(primarySp0));
		}

		UtilizationVector loreyHeightByUtilization = VdypUtilizationHolder.emptyLoreyHeightUtilization();

		@Override
		public void loreyHeight(UtilizationVector vector) {
			this.loreyHeightByUtilization = vector;
		}

		protected UtilizationVector baseAreaByUtilization = VdypUtilizationHolder.emptyUtilization();

		@Override
		public void baseArea(UtilizationVector vector) {
			this.baseAreaByUtilization = vector;
		}

		protected UtilizationVector treesPerHectareByUtilization = VdypUtilizationHolder.emptyUtilization();

		@Override
		public void treesPerHectare(UtilizationVector vector) {
			this.treesPerHectareByUtilization = vector;
		}

		protected UtilizationVector quadraticMeanDiameterByUtilization = VdypUtilizationHolder.emptyUtilization();

		@Override
		public void quadMeanDiameter(UtilizationVector vector) {
			this.quadraticMeanDiameterByUtilization = vector;
		}

		protected UtilizationVector wholeStemVolumeByUtilization = VdypUtilizationHolder.emptyUtilization();

		@Override
		public void wholeStemVolume(UtilizationVector vector) {
			this.wholeStemVolumeByUtilization = vector;
		}

		protected UtilizationVector closeUtilizationVolumeByUtilization = VdypUtilizationHolder.emptyUtilization();

		@Override
		public void closeUtilizationVolumeByUtilization(UtilizationVector vector) {
			this.closeUtilizationVolumeByUtilization = vector;
		}

		protected UtilizationVector closeUtilizationVolumeNetOfDecayByUtilization = VdypUtilizationHolder
				.emptyUtilization();

		@Override
		public void closeUtilizationVolumeNetOfDecayByUtilization(UtilizationVector vector) {
			this.closeUtilizationVolumeNetOfDecayByUtilization = vector;
		}

		protected UtilizationVector closeUtilizationVolumeNetOfDecayAndWasteByUtilization = VdypUtilizationHolder
				.emptyUtilization();

		@Override
		public void closeUtilizationVolumeNetOfDecayAndWasteByUtilization(UtilizationVector vector) {
			this.closeUtilizationVolumeNetOfDecayAndWasteByUtilization = vector;
		}

		protected UtilizationVector closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization = VdypUtilizationHolder
				.emptyUtilization();

		@Override
		public void closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(UtilizationVector vector) {
			this.closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization = vector;
		}

		@Override
		public VdypLayer.Builder adapt(BaseVdypLayer<?, ?> baseSource) {
			super.adapt(baseSource);

			if (baseSource instanceof VdypLayer source) {
				loreyHeightByUtilization = new UtilizationVector(source.loreyHeightByUtilization);
				baseAreaByUtilization = new UtilizationVector(source.baseAreaByUtilization);
				treesPerHectareByUtilization = new UtilizationVector(source.treesPerHectareByUtilization);
				quadraticMeanDiameterByUtilization = new UtilizationVector(source.quadraticMeanDiameterByUtilization);
				wholeStemVolumeByUtilization = new UtilizationVector(source.wholeStemVolumeByUtilization);
				closeUtilizationVolumeByUtilization = new UtilizationVector(source.closeUtilizationVolumeByUtilization);
				closeUtilizationVolumeNetOfDecayByUtilization = new UtilizationVector(
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
		protected void postProcess(VdypLayer layer) {
			super.postProcess(layer);

			layer.setLoreyHeightByUtilization(loreyHeightByUtilization);
			layer.setBaseAreaByUtilization(baseAreaByUtilization);
			layer.setTreesPerHectareByUtilization(treesPerHectareByUtilization);
			layer.setQuadraticMeanDiameterByUtilization(quadraticMeanDiameterByUtilization);
			layer.setWholeStemVolumeByUtilization(wholeStemVolumeByUtilization);
			layer.setCloseUtilizationVolumeByUtilization(closeUtilizationVolumeByUtilization);
			layer.setCloseUtilizationVolumeNetOfDecayByUtilization(closeUtilizationVolumeNetOfDecayByUtilization);
			layer.setCloseUtilizationVolumeNetOfDecayAndWasteByUtilization(
					closeUtilizationVolumeNetOfDecayAndWasteByUtilization
			);
			layer.setCloseUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(
					closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization
			);
		}

		@Override
		protected VdypLayer doBuild() {
			return new VdypLayer(
					polygonIdentifier.get(), //
					layerType.get(), //
					inventoryTypeGroup, //
					empiricalRelationshipParameterIndex, //
					primarySp0
			);
		}

		@Override
		protected VdypSpecies buildSpecies(Consumer<VdypSpecies.Builder> config) {
			return VdypSpecies.build(builder -> {
				config.accept(builder);
				builder.polygonIdentifier(this.polygonIdentifier.get());
				builder.layerType(this.layerType.get());
			});
		}

		@Override
		public <S2 extends BaseVdypSpecies<I2>, I2 extends BaseVdypSite>
				BaseVdypLayer.Builder<VdypLayer, VdypSpecies, VdypSite, VdypSpecies.Builder, VdypSite.Builder>
				adaptSpecies(BaseVdypLayer<S2, ?> toCopy, BiConsumer<VdypSpecies.Builder, S2> config) {
			this.primaryGenus(toCopy.getPrimaryGenus());
			return super.adaptSpecies(toCopy, config);
		}

		@Override
		public BaseVdypLayer.Builder<VdypLayer, VdypSpecies, VdypSite, VdypSpecies.Builder, VdypSite.Builder>
				copySpecies(VdypLayer toCopy, BiConsumer<VdypSpecies.Builder, VdypSpecies> config) {
			this.primaryGenus(toCopy.getPrimaryGenus());
			return super.copySpecies(toCopy, config);
		}

		@Override
		public Builder copy(VdypLayer source) {
			super.copy(source);
			this.empiricalRelationshipParameterIndex(source.getEmpiricalRelationshipParameterIndex());
			return this;
		}

	}
}
