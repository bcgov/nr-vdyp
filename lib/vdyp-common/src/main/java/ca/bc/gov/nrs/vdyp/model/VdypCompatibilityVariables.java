package ca.bc.gov.nrs.vdyp.model;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import ca.bc.gov.nrs.vdyp.model.MatrixMap3Impl.TriFunction;
import ca.bc.gov.nrs.vdyp.model.builders.ModelClassBuilder;
import ca.bc.gov.nrs.vdyp.model.builders.SpeciesGroupIdentifiedBuilder;
import ca.bc.gov.nrs.vdyp.model.variables.UtilizationClassVariable;

public class VdypCompatibilityVariables {

	private final MatrixMap3<UtilizationClass, UtilizationClassVariable, LayerType, Float> cvVolume;
	private final MatrixMap2<UtilizationClass, LayerType, Float> cvBasalArea;
	private final MatrixMap2<UtilizationClass, LayerType, Float> cvQuadraticMeanDiameter;
	private final Map<UtilizationClassVariable, Float> cvPrimaryLayerSmall;

	public static final Set<UtilizationClassVariable> VOLUME_UTILIZATION_VARIABLES = EnumSet.of(
			UtilizationClassVariable.WHOLE_STEM_VOL, UtilizationClassVariable.CLOSE_UTIL_VOL,
			UtilizationClassVariable.CLOSE_UTIL_VOL_LESS_DECAY,
			UtilizationClassVariable.CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE
	);
	public static final Set<UtilizationClassVariable> SMALL_UTILIZATION_VARIABLES = EnumSet.of(
			UtilizationClassVariable.BASAL_AREA, UtilizationClassVariable.QUAD_MEAN_DIAMETER,
			UtilizationClassVariable.LOREY_HEIGHT, UtilizationClassVariable.WHOLE_STEM_VOL
	);

	public VdypCompatibilityVariables(
			MatrixMap3<UtilizationClass, UtilizationClassVariable, LayerType, Float> cvVolume,
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

	public MatrixMap3<UtilizationClass, UtilizationClassVariable, LayerType, Float> getCvVolume() {
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

	public float getCvVolume(UtilizationClass uc, UtilizationClassVariable vv, LayerType lt) {
		return getCvVolume().get(uc, vv, lt);
	}

	public float getCvBasalArea(UtilizationClass uc, LayerType lt) {
		return getCvBasalArea().get(uc, lt);
	}

	public float getCvQuadraticMeanDiameter(UtilizationClass uc, LayerType lt) {
		return getCvQuadraticMeanDiameter().get(uc, lt);
	}

	public float getCvPrimaryLayerSmall(UtilizationClassVariable ucv) {
		return getCvPrimaryLayerSmall().get(ucv);
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

		private Optional<MatrixMap3<UtilizationClass, UtilizationClassVariable, LayerType, Float>> cvVolume = Optional
				.empty();
		private Optional<MatrixMap2<UtilizationClass, LayerType, Float>> cvBasalArea = Optional.empty();
		private Optional<MatrixMap2<UtilizationClass, LayerType, Float>> cvQuadraticMeanDiameter = Optional.empty();
		private Optional<Map<UtilizationClassVariable, Float>> cvPrimaryLayerSmall = Optional.empty();

		public void cvVolume(TriFunction<UtilizationClass, UtilizationClassVariable, LayerType, Float> init) {
			MatrixMap3Impl<UtilizationClass, UtilizationClassVariable, LayerType, Float> cvVolume = new MatrixMap3Impl<>(
					UtilizationClass.UTIL_CLASSES, VOLUME_UTILIZATION_VARIABLES, LayerType.ALL_USED, init
			);
			cvVolume(cvVolume);
		}

		public void cvVolume(MatrixMap3<UtilizationClass, UtilizationClassVariable, LayerType, Float> cvVolume) {
			this.cvVolume = Optional.of(cvVolume);
		}

		public void cvBasalArea(BiFunction<UtilizationClass, LayerType, Float> init) {
			MatrixMap2Impl<UtilizationClass, LayerType, Float> cvBasalArea = new MatrixMap2Impl<>(
					UtilizationClass.UTIL_CLASSES, LayerType.ALL_USED, init
			);
			cvBasalArea(cvBasalArea);
		}

		public void cvBasalArea(MatrixMap2<UtilizationClass, LayerType, Float> cvBasalArea) {
			this.cvBasalArea = Optional.of(cvBasalArea);
		}

		public void cvQuadraticMeanDiameter(BiFunction<UtilizationClass, LayerType, Float> init) {
			MatrixMap2Impl<UtilizationClass, LayerType, Float> cvQuadraticMeanDiameter = new MatrixMap2Impl<>(
					UtilizationClass.UTIL_CLASSES, LayerType.ALL_USED, init
			);
			cvQuadraticMeanDiameter(cvQuadraticMeanDiameter);
		}

		public void cvQuadraticMeanDiameter(MatrixMap2<UtilizationClass, LayerType, Float> cvQuadraticMeanDiameter) {
			this.cvQuadraticMeanDiameter = Optional.of(cvQuadraticMeanDiameter);
		}

		public void cvPrimaryLayerSmall(Function<UtilizationClassVariable, Float> init) {
			var cvPrimaryLayerSmall = new HashMap<UtilizationClassVariable, Float>();
			for (var ucv : SMALL_UTILIZATION_VARIABLES) {
				cvPrimaryLayerSmall.put(ucv, init.apply(ucv));
			}
			cvPrimaryLayerSmall(cvPrimaryLayerSmall);
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
			// Do a deep copy of the contents of the matrix maps
			cvVolume(toCopy.getCvVolume()::getM);
			cvBasalArea(toCopy.getCvBasalArea()::getM);
			cvQuadraticMeanDiameter(toCopy.getCvQuadraticMeanDiameter()::getM);
			cvPrimaryLayerSmall(toCopy.getCvPrimaryLayerSmall()::get);
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

		public void genus(Optional<String> speciesGroup) {
			this.speciesGroup = speciesGroup;
		}

	}
}
