package ca.bc.gov.nrs.vdyp.model;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import ca.bc.gov.nrs.vdyp.model.builders.ModelClassBuilder;
import ca.bc.gov.nrs.vdyp.model.builders.SpeciesGroupIdentifiedBuilder;

public class VdypCompatibilityVariables {

	private final MatrixMap3<UtilizationClass, VolumeVariable, LayerType, Float> cvVolume;
	private final MatrixMap2<UtilizationClass, LayerType, Float> cvBasalArea;
	private final MatrixMap2<UtilizationClass, LayerType, Float> cvQuadraticMeanDiameter;
	private final Map<UtilizationClassVariable, Float> cvPrimaryLayerSmall;

	public VdypCompatibilityVariables(
			MatrixMap3<UtilizationClass, VolumeVariable, LayerType, Float> cvVolume,
			MatrixMap2<UtilizationClass, LayerType, Float> cvBasalArea,
			MatrixMap2<UtilizationClass, LayerType, Float> cvQuadraticMeanDiameter,
			Map<UtilizationClassVariable, Float> cvPrimaryLayerSmall
	) {
		super();
		this.cvVolume = cvVolume;
		this.cvBasalArea = cvBasalArea;
		this.cvQuadraticMeanDiameter = cvQuadraticMeanDiameter;
		this.cvPrimaryLayerSmall = cvPrimaryLayerSmall;
	}

	public MatrixMap3<UtilizationClass, VolumeVariable, LayerType, Float> getCvVolume() {
		return cvVolume;
	}

	public MatrixMap2<UtilizationClass, LayerType, Float> getCvBasalArea() {
		return cvBasalArea;
	}

	public MatrixMap2<UtilizationClass, LayerType, Float> getCvQuadraticMeanDiameter() {
		return cvQuadraticMeanDiameter;
	}

	public Map<UtilizationClassVariable, Float> getCvPrimaryLayerSmall() {
		return cvPrimaryLayerSmall;
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
	public static VdypCompatibilityVariables build(Consumer<Builder> config) {
		var builder = new Builder();
		config.accept(builder);
		return builder.build();
	}

	public static VdypCompatibilityVariables build(VdypSpecies spec, Consumer<Builder> config) {
		var builder = new Builder();
		builder.polygonIdentifier(spec.getPolygonIdentifier());
		builder.layerType(spec.getLayerType());
		builder.genus(spec.getGenus());
		config.accept(builder);
		return builder.build();
	}

	public static class Builder extends ModelClassBuilder<VdypCompatibilityVariables>
			implements SpeciesGroupIdentifiedBuilder {

		private Optional<PolygonIdentifier> polygonIdentifier = Optional.empty();
		private Optional<LayerType> layerType = Optional.empty();
		private Optional<String> speciesGroup = Optional.empty();

		public void polygonIndentifier(PolygonIdentifier polyId) {
			this.polygonIndentifier(polyId);
		}

		private Optional<MatrixMap3<UtilizationClass, VolumeVariable, LayerType, Float>> cvVolume = Optional.empty();
		private Optional<MatrixMap2<UtilizationClass, LayerType, Float>> cvBasalArea = Optional.empty();
		private Optional<MatrixMap2<UtilizationClass, LayerType, Float>> cvQuadraticMeanDiameter = Optional.empty();
		private Optional<Map<UtilizationClassVariable, Float>> cvPrimaryLayerSmall = Optional.empty();

		public void cvVolume(MatrixMap3<UtilizationClass, VolumeVariable, LayerType, Float> cvVolume) {
			this.cvVolume = Optional.of(cvVolume);
		}

		public void cvBasalArea(MatrixMap2<UtilizationClass, LayerType, Float> cvBasalArea) {
			this.cvBasalArea = Optional.of(cvBasalArea);
		}

		public void cvQuadraticMeanDiameter(MatrixMap2<UtilizationClass, LayerType, Float> cvQuadraticMeanDiameter) {
			this.cvQuadraticMeanDiameter = Optional.of(cvQuadraticMeanDiameter);
		}

		public void cvPrimaryLayerSmall(Map<UtilizationClassVariable, Float> cvPrimaryLayerSmall) {
			this.cvPrimaryLayerSmall = Optional.of(cvPrimaryLayerSmall);
		}

		@Override
		protected void check(Collection<String> errors) {
			requirePresent(cvVolume, "cvVolume", errors);
			requirePresent(cvBasalArea, "cvBasalArea", errors);
			requirePresent(cvQuadraticMeanDiameter, "cvQuadraticMeanDiameter", errors);
			requirePresent(cvPrimaryLayerSmall, "cvPrimaryLayerSmall", errors);
		}

		@Override
		protected String getBuilderId() {
			return MessageFormat.format(
					"Compatibility Variables {0} {1} {2}", //
					polygonIdentifier.map(Object::toString).orElse("N/A"), //
					layerType.map(Object::toString).orElse("N/A"), //
					speciesGroup.map(Object::toString).orElse("N/A")//
			);
		}

		public void copy(VdypCompatibilityVariables toCopy) {
			cvVolume(toCopy.getCvVolume());
			cvBasalArea(toCopy.getCvBasalArea());
			cvQuadraticMeanDiameter(toCopy.getCvQuadraticMeanDiameter());
			cvPrimaryLayerSmall(toCopy.getCvPrimaryLayerSmall());
		}

		@Override
		protected VdypCompatibilityVariables doBuild() {
			return new VdypCompatibilityVariables(
					cvVolume.get(), cvBasalArea.get(), cvQuadraticMeanDiameter.get(), cvPrimaryLayerSmall.get()
			);
		}

		@Override
		public void layerType(LayerType type) {
			this.layerType = Optional.of(type);
		}

		@Override
		public void polygonIdentifier(PolygonIdentifier polygonIdentifier) {
			this.polygonIdentifier = Optional.of(polygonIdentifier);
		}

		@Override
		public void genus(String speciesGroup) {
			this.speciesGroup = Optional.of(speciesGroup);
		}

	}
}
