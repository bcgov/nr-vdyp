package ca.bc.gov.nrs.vdyp.model;

import java.io.IOException;
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
	public Optional<Float> getYearsAtBreastHeight() {
		return this.getPrimarySite().flatMap(BaseVdypSite::getYearsAtBreastHeight);
	}

	@Computed
	public Optional<Float> getComputedYearsAtBreastHeight() {
		return this.getPrimarySite().flatMap(BaseVdypSite::getComputedYearsAtBreastHeight);
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
			extends BaseVdypLayer.Builder<VdypLayer, VdypSpecies, VdypSite, VdypSpecies.Builder, VdypSite.Builder> {

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

		public void loreyHeightByUtilization(float height) {
			this.loreyHeightByUtilization = Utils.heightVector(0, height);
		}

		public void loreyHeightByUtilization(float small, float height) {
			this.loreyHeightByUtilization = Utils.heightVector(small, height);
		}

		protected UtilizationVector baseAreaByUtilization = VdypUtilizationHolder.emptyUtilization();

		public void baseAreaByUtilization(float small, float u1, float u2, float u3, float u4) {
			this.baseAreaByUtilization = Utils.utilizationVector(small, u1, u2, u3, u4);
		}

		public void baseAreaByUtilization(float height) {
			this.baseAreaByUtilization = Utils.utilizationVector(height);
		}

		protected UtilizationVector treesPerHectareByUtilization = VdypUtilizationHolder.emptyUtilization();

		public void treesPerHectareByUtilization(float small, float u1, float u2, float u3, float u4) {
			this.treesPerHectareByUtilization = Utils.utilizationVector(small, u1, u2, u3, u4);
		}

		public void treesPerHectareByUtilization(float height) {
			this.treesPerHectareByUtilization = Utils.utilizationVector(height);
		}

		protected UtilizationVector quadraticMeanDiameterByUtilization = VdypUtilizationHolder.emptyUtilization();

		public void
				quadraticMeanDiameterByUtilization(float small, float uAll, float u1, float u2, float u3, float u4) {
			this.quadraticMeanDiameterByUtilization = Utils.utilizationVector(small, uAll, u1, u2, u3, u4);
		}

		public void quadraticMeanDiameterByUtilization(float height) {
			this.quadraticMeanDiameterByUtilization = Utils.utilizationVector(height);
		}

		protected UtilizationVector wholeStemVolumeByUtilization = VdypUtilizationHolder.emptyUtilization();

		public void wholeStemVolumeByUtilization(float small, float u1, float u2, float u3, float u4) {
			this.wholeStemVolumeByUtilization = Utils.utilizationVector(small, u1, u2, u3, u4);
		}

		public void wholeStemVolumeByUtilization(float volume) {
			this.wholeStemVolumeByUtilization = Utils.utilizationVector(volume);
		}

		protected UtilizationVector closeUtilizationVolumeByUtilization = VdypUtilizationHolder.emptyUtilization();

		public void closeUtilizationVolumeByUtilization(float small, float u1, float u2, float u3, float u4) {
			this.closeUtilizationVolumeByUtilization = Utils.utilizationVector(small, u1, u2, u3, u4);
		}

		public void closeUtilizationVolumeByUtilization(float volume) {
			this.closeUtilizationVolumeByUtilization = Utils.utilizationVector(volume);
		}

		protected UtilizationVector closeUtilizationVolumeNetOfDecayByUtilization = VdypUtilizationHolder
				.emptyUtilization();

		public void closeUtilizationVolumeNetOfDecayByUtilization(float small, float u1, float u2, float u3, float u4) {
			this.closeUtilizationVolumeNetOfDecayByUtilization = Utils.utilizationVector(small, u1, u2, u3, u4);
		}

		public void closeUtilizationVolumeNetOfDecayByUtilization(float volume) {
			this.closeUtilizationVolumeNetOfDecayByUtilization = Utils.utilizationVector(volume);
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
	}

	@Override
	public void dumpState(Appendable output, int indent) throws IOException {
		super.dumpState(output, indent);

		Dumpable.writeProperty(
				output, indent + 1, "empiricalRelationshipParameterIndex", empiricalRelationshipParameterIndex
		);
		Dumpable.writeProperty(output, indent + 1, "primarySp0", primarySp0);
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

	}

}
