package ca.bc.gov.nrs.vdyp.common;

import static ca.bc.gov.nrs.vdyp.common_calculators.BaseAreaTreeDensityDiameter.quadMeanDiameter;
import static ca.bc.gov.nrs.vdyp.common_calculators.BaseAreaTreeDensityDiameter.treesPerHectare;
import static ca.bc.gov.nrs.vdyp.model.CommonData.HARDWOODS;
import static ca.bc.gov.nrs.vdyp.model.CommonData.ITG_PURE;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.solvers.BrentSolver;
import org.apache.commons.math3.exception.TooManyEvaluationsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.application.SpeciesCopier;
import ca.bc.gov.nrs.vdyp.application.VdypApplicationIdentifier;
import ca.bc.gov.nrs.vdyp.application.VdypStartApplication;
import ca.bc.gov.nrs.vdyp.common_calculators.BaseAreaTreeDensityDiameter;
import ca.bc.gov.nrs.vdyp.controlmap.ResolvedControlMap;
import ca.bc.gov.nrs.vdyp.exceptions.CouldNotFindBracketingIntervalException;
import ca.bc.gov.nrs.vdyp.exceptions.FatalProcessingException;
import ca.bc.gov.nrs.vdyp.exceptions.ProcessingException;
import ca.bc.gov.nrs.vdyp.exceptions.UnsupportedSpeciesException;
import ca.bc.gov.nrs.vdyp.math.FloatMath;
import ca.bc.gov.nrs.vdyp.model.BaseVdypSite;
import ca.bc.gov.nrs.vdyp.model.BaseVdypSpecies;
import ca.bc.gov.nrs.vdyp.model.BecDefinition;
import ca.bc.gov.nrs.vdyp.model.Coefficients;
import ca.bc.gov.nrs.vdyp.model.CommonData;
import ca.bc.gov.nrs.vdyp.model.CompatibilityVariableMode;
import ca.bc.gov.nrs.vdyp.model.ComponentSizeLimits;
import ca.bc.gov.nrs.vdyp.model.DebugSettings;
import ca.bc.gov.nrs.vdyp.model.GenusDefinitionMap;
import ca.bc.gov.nrs.vdyp.model.NonFipDebugSettings;
import ca.bc.gov.nrs.vdyp.model.Region;
import ca.bc.gov.nrs.vdyp.model.UtilizationClass;
import ca.bc.gov.nrs.vdyp.model.UtilizationVector;
import ca.bc.gov.nrs.vdyp.model.VdypLayer;
import ca.bc.gov.nrs.vdyp.model.VdypSpecies;
import ca.bc.gov.nrs.vdyp.model.VdypUtilizationHolder;
import ca.bc.gov.nrs.vdyp.model.VolumeComputeMode;
import ca.bc.gov.nrs.vdyp.model.VolumeVariable;

public class ComputationMethods {

	public static final Logger log = LoggerFactory.getLogger(ComputationMethods.class);

	/**
	 * Accessor methods for utilization vectors, except for Lorey Height, on Layer and Species objects.
	 */
	protected static final Collection<PropertyDescriptor> UTILIZATION_VECTOR_ACCESSORS;

	/**
	 * Accessor methods for utilization vectors, except for Lorey Height and Quadratic Mean Diameter, on Layer and
	 * Species objects. These are properties where the values for the layer are the sum of those for its species.
	 */
	public static final Collection<PropertyDescriptor> SUMMABLE_UTILIZATION_VECTOR_ACCESSORS;

	/**
	 * Accessor methods for utilization vectors, except for Lorey Height,and Volume on Layer and Species objects.
	 */
	protected static final Collection<PropertyDescriptor> NON_VOLUME_UTILIZATION_VECTOR_ACCESSORS;

	static {
		try {
			var bean = Introspector.getBeanInfo(VdypUtilizationHolder.class);
			UTILIZATION_VECTOR_ACCESSORS = Arrays.stream(bean.getPropertyDescriptors()) //
					.filter(p -> p.getName().endsWith("ByUtilization")) //
					.filter(p -> !p.getName().startsWith("loreyHeight")) //
					.filter(p -> p.getPropertyType() == UtilizationVector.class) //
					.toList();
		} catch (IntrospectionException e) {
			throw new IllegalStateException(e);
		}

		SUMMABLE_UTILIZATION_VECTOR_ACCESSORS = UTILIZATION_VECTOR_ACCESSORS.stream()
				.filter(x -> !x.getName().startsWith("quadraticMeanDiameter")).toList();

		NON_VOLUME_UTILIZATION_VECTOR_ACCESSORS = UTILIZATION_VECTOR_ACCESSORS.stream()
				.filter(x -> !x.getName().contains("Volume")).toList();
	}

	private final EstimationMethods estimationMethods;

	private final VdypApplicationIdentifier context;

	private ResolvedControlMap controlMap;

	public ComputationMethods(EstimationMethods estimationMethods, VdypApplicationIdentifier context) {
		this.estimationMethods = estimationMethods;
		this.context = context;
		this.controlMap = estimationMethods.controlMap;
	}

	/**
	 * Computes the average Lorey Height across both Small and All classes for the given entity.
	 *
	 * @param entity
	 * @return
	 */
	public static float computeLoreyHeightWithSmallClass(VdypUtilizationHolder entity) {
		// Average the trees from the 4-7.5 and 7.5+ classes weighted by basal area.

		final var ba = entity.getBaseAreaByUtilization();
		final var tph = entity.getTreesPerHectareByUtilization(); // Area of the stand divides out so TPH works in
																	// place of number of trees
		final var lh = entity.getLoreyHeightByUtilization();

		double numerator = 0;
		double denominator = 0;

		for (var uc : new UtilizationClass[] { UtilizationClass.ALL, UtilizationClass.SMALL }) {
			final double baTph = ba.get(uc) * tph.get(uc);
			numerator += baTph * lh.get(uc);
			denominator += baTph;
		}
		if (denominator == 0) {
			// BA, TPH, and LH are always non-negative so if the sum of BA*TPH is 0, then each term was 0, and each
			// BA*TPH*LH term was then also 0 so denominator==0 implies numerator==0. There are no trees 4.0 cm or
			// larger diameter.
			return 0f;
		}
		return (float) (numerator / denominator);
	}

	/**
	 * YUC1 - compute Utilization components (quad-mean-diameter, basal area and trees-per-hectare) and, optionally,
	 * volumes for a polygon's primary layer.
	 *
	 * @param bec                       Bec zone
	 * @param vdypLayer                 (primary) layer in question
	 * @param volumeComputeMode         the {@link VolumeComputeMode} under which this method is to operate
	 * @param compatibilityVariableMode the {@link CompatibilityVariableMode} under which this method is to operate.
	 * @throws ProcessingException
	 */
	public void computeUtilizationComponentsPrimary(
			BecDefinition bec, VdypLayer vdypLayer, VolumeComputeMode volumeComputeMode,
			CompatibilityVariableMode compatibilityVariableMode
	) throws ProcessingException {
		log.atTrace().setMessage("computeUtilizationComponentsPrimary for {}, stand total age is {}")
				.addArgument(vdypLayer.getPolygonIdentifier()).addArgument(vdypLayer.getAgeTotal()).log();

		log.atDebug().setMessage("Primary layer for {} has {} species/genera: {}")
				.addArgument(vdypLayer::getPolygonIdentifier) //
				.addArgument(() -> vdypLayer.getSpecies().size()) //
				.addArgument(() -> vdypLayer.getSpecies().keySet().stream().collect(Collectors.joining(", "))) //
				.log();

		for (VdypSpecies spec : vdypLayer.getSpecies().values()) {
			float loreyHeightSpec = spec.getLoreyHeightByUtilization().getAll();
			float baseAreaSpec = spec.getBaseAreaByUtilization().getAll();
			float quadMeanDiameterSpec = spec.getQuadraticMeanDiameterByUtilization().getAll();
			float treesPerHectareSpec = spec.getTreesPerHectareByUtilization().getAll();

			log.atDebug().setMessage("Working with species {}  LH: {}  DQ: {}  BA: {}  TPH: {}")
					.addArgument(spec.getClass()).addArgument(loreyHeightSpec).addArgument(quadMeanDiameterSpec)
					.addArgument(baseAreaSpec).addArgument(treesPerHectareSpec);

			if (volumeComputeMode == VolumeComputeMode.BY_UTIL_WITH_WHOLE_STEM_BY_SPEC) {
				log.atDebug().log("Estimating tree volume");

				var volumeGroup = spec.getVolumeGroup();
				var meanVolume = this.estimationMethods
						.estimateWholeStemVolumePerTree(volumeGroup, loreyHeightSpec, quadMeanDiameterSpec);
				var specWholeStemVolume = treesPerHectareSpec * meanVolume;

				spec.getWholeStemVolumeByUtilization().setAll(specWholeStemVolume);
			}
			float wholeStemVolumeSpec = spec.getWholeStemVolumeByUtilization().getAll();

			var basalAreaUtil = Utils.utilizationVector();
			var quadMeanDiameterUtil = Utils.utilizationVector();
			var treesPerHectareUtil = Utils.utilizationVector();
			var wholeStemVolumeUtil = Utils.utilizationVector();
			var closeVolumeUtil = Utils.utilizationVector();
			var closeVolumeNetDecayUtil = Utils.utilizationVector();
			var closeVolumeNetDecayWasteUtil = Utils.utilizationVector();
			var closeVolumeNetDecayWasteBreakUtil = Utils.utilizationVector();

			basalAreaUtil.setAll(baseAreaSpec); // BAU
			quadMeanDiameterUtil.setAll(quadMeanDiameterSpec); // DQU
			treesPerHectareUtil.setAll(treesPerHectareSpec); // TPHU
			wholeStemVolumeUtil.setAll(wholeStemVolumeSpec); // WSU

			var adjustCloseUtil = Utils.utilizationVector(); // ADJVCU
			var adjustDecayUtil = Utils.utilizationVector(); // ADJVD
			var adjustDecayWasteUtil = Utils.utilizationVector(); // ADJVDW

			// EMP071
			estimationMethods.estimateQuadMeanDiameterByUtilization(bec, quadMeanDiameterUtil, spec.getGenus());

			// EMP070
			estimationMethods.estimateBaseAreaByUtilization(bec, quadMeanDiameterUtil, basalAreaUtil, spec.getGenus());

			// Calculate tree density components
			for (var uc : VdypStartApplication.UTIL_CLASSES) {
				treesPerHectareUtil.set(
						uc,
						BaseAreaTreeDensityDiameter.treesPerHectare(basalAreaUtil.get(uc), quadMeanDiameterUtil.get(uc))
				);
			}

			// reconcile components with totals

			// YUC1R
			ReconcilationMethods.reconcileComponents(basalAreaUtil, treesPerHectareUtil, quadMeanDiameterUtil);

			if (compatibilityVariableMode != CompatibilityVariableMode.NONE) {

				float basalAreaSumForSpecies = 0.0f;
				for (var uc : VdypStartApplication.UTIL_CLASSES) {

					float currentUcBasalArea = basalAreaUtil.get(uc);
					basalAreaUtil.set(uc, currentUcBasalArea + spec.getCvBasalArea(uc, spec.getLayerType()));
					if (basalAreaUtil.get(uc) < 0.0f) {
						basalAreaUtil.set(uc, 0.0f);
					}

					basalAreaSumForSpecies += basalAreaUtil.get(uc);

					float newDqValue = quadMeanDiameterUtil.get(uc)
							+ spec.getCvQuadraticMeanDiameter(uc, spec.getLayerType());
					quadMeanDiameterUtil.set(uc, FloatMath.clamp(newDqValue, uc.lowBound, uc.highBound));
				}

				if (basalAreaSumForSpecies > 0.0f) {
					float baMult = basalAreaUtil.get(UtilizationClass.ALL) / basalAreaSumForSpecies;

					for (UtilizationClass uc : UtilizationClass.ALL_CLASSES) {
						basalAreaUtil.set(uc, basalAreaUtil.get(uc) * baMult);
					}
				}
			}

			// Recalculate TPH's

			for (var uc : VdypStartApplication.UTIL_CLASSES) {
				treesPerHectareUtil.setCoe(
						uc.index,
						BaseAreaTreeDensityDiameter
								.treesPerHectare(basalAreaUtil.getCoe(uc.index), quadMeanDiameterUtil.getCoe(uc.index))
				);
			}

			// Since DQ's may have changed, MUST RECONCILE AGAIN
			// Seems this might only be needed when compatibilityVariableMode is not NONE?

			// YUC1R
			ReconcilationMethods.reconcileComponents(basalAreaUtil, treesPerHectareUtil, quadMeanDiameterUtil);

			if (volumeComputeMode == VolumeComputeMode.ZERO) {
				throw new UnsupportedOperationException("TODO");
			} else {

				// EMP091
				estimationMethods.estimateWholeStemVolume(
						UtilizationClass.ALL, adjustCloseUtil.getCoe(4), spec.getVolumeGroup(), loreyHeightSpec,
						quadMeanDiameterUtil, basalAreaUtil, wholeStemVolumeUtil
				);

				if (compatibilityVariableMode == CompatibilityVariableMode.ALL) {
					// apply compatibility variables to WS volume

					float wholeStemVolumeSum = 0.0f;
					for (UtilizationClass uc : UtilizationClass.UTIL_CLASSES) {
						wholeStemVolumeUtil.set(
								uc,
								wholeStemVolumeUtil.get(uc) * FloatMath
										.exp(spec.getCvVolume(uc, VolumeVariable.WHOLE_STEM_VOL, spec.getLayerType()))
						);
						wholeStemVolumeSum += wholeStemVolumeUtil.get(uc);
					}
					wholeStemVolumeUtil.set(UtilizationClass.ALL, wholeStemVolumeSum);

					// Set the adjustment factors for next three volume types
					for (UtilizationClass uc : UtilizationClass.UTIL_CLASSES) {
						adjustCloseUtil
								.set(uc, spec.getCvVolume(uc, VolumeVariable.CLOSE_UTIL_VOL, spec.getLayerType()));
						adjustDecayUtil.set(
								uc, spec.getCvVolume(uc, VolumeVariable.CLOSE_UTIL_VOL_LESS_DECAY, spec.getLayerType())
						);
						adjustDecayWasteUtil.set(
								uc,
								spec.getCvVolume(
										uc, VolumeVariable.CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, spec.getLayerType()
								)
						);
					}
				} else {
					// Do nothing as the adjustment vectors are already set to 0
				}

				// EMP092
				estimationMethods.estimateCloseUtilizationVolume(
						UtilizationClass.ALL, adjustCloseUtil, spec.getVolumeGroup(), loreyHeightSpec,
						quadMeanDiameterUtil, wholeStemVolumeUtil, closeVolumeUtil
				);

				// EMP093
				estimationMethods.estimateNetDecayVolume(
						spec.getGenus(), bec.getRegion(), UtilizationClass.ALL, adjustDecayUtil, spec.getDecayGroup(),
						vdypLayer.getYearsAtBreastHeight().orElse(0f), quadMeanDiameterUtil, closeVolumeUtil,
						closeVolumeNetDecayUtil
				);

				// EMP094
				estimationMethods.estimateNetDecayAndWasteVolume(
						bec.getRegion(), UtilizationClass.ALL, adjustDecayWasteUtil, spec.getGenus(), loreyHeightSpec,
						quadMeanDiameterUtil, closeVolumeUtil, closeVolumeNetDecayUtil, closeVolumeNetDecayWasteUtil
				);

				if (context.isStart()) {
					// EMP095
					estimationMethods.estimateNetDecayWasteAndBreakageVolume(
							UtilizationClass.ALL, spec.getBreakageGroup(), quadMeanDiameterUtil, closeVolumeUtil,
							closeVolumeNetDecayWasteUtil, closeVolumeNetDecayWasteBreakUtil
					);
				}
			}

			spec.getBaseAreaByUtilization().pairwiseInPlace(basalAreaUtil, EstimationMethods.COPY_IF_BAND);
			spec.getTreesPerHectareByUtilization().pairwiseInPlace(treesPerHectareUtil, EstimationMethods.COPY_IF_BAND);
			spec.getQuadraticMeanDiameterByUtilization()
					.pairwiseInPlace(quadMeanDiameterUtil, EstimationMethods.COPY_IF_BAND);

			spec.getWholeStemVolumeByUtilization()
					.pairwiseInPlace(wholeStemVolumeUtil, EstimationMethods.COPY_IF_NOT_SMALL);
			spec.getCloseUtilizationVolumeByUtilization()
					.pairwiseInPlace(closeVolumeUtil, EstimationMethods.COPY_IF_NOT_SMALL);
			spec.getCloseUtilizationVolumeNetOfDecayByUtilization()
					.pairwiseInPlace(closeVolumeNetDecayUtil, EstimationMethods.COPY_IF_NOT_SMALL);
			spec.getCloseUtilizationVolumeNetOfDecayAndWasteByUtilization()
					.pairwiseInPlace(closeVolumeNetDecayWasteUtil, EstimationMethods.COPY_IF_NOT_SMALL);
			spec.getCloseUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization()
					.pairwiseInPlace(closeVolumeNetDecayWasteBreakUtil, EstimationMethods.COPY_IF_NOT_SMALL);

		}

		computeLayerUtilizationComponentsFromSpecies(vdypLayer);

		for (VdypSpecies spec : vdypLayer.getSpecies().values()) {
			if (vdypLayer.getBaseAreaByUtilization().getAll() > 0f) {
				spec.setFractionGenus(
						spec.getBaseAreaByUtilization().getAll() / vdypLayer.getBaseAreaByUtilization().getAll()
				);
			}
			log.atDebug().addArgument(spec.getGenus()).addArgument(spec.getFractionGenus())
					.setMessage("Species {} base area {}%").log();
		}

		log.atDebug().setMessage("Calculating Stand Lorey Height").log();

		vdypLayer.getLoreyHeightByUtilization().setSmall(0f);
		vdypLayer.getLoreyHeightByUtilization().setAll(0f);

		for (VdypSpecies spec : vdypLayer.getSpecies().values()) {
			log.atDebug() //
					.addArgument(spec.getGenus()) //
					.addArgument(() -> spec.getLoreyHeightByUtilization().getAll())
					.addArgument(() -> spec.getBaseAreaByUtilization().getAll())
					.addArgument(
							() -> spec.getLoreyHeightByUtilization().getAll() * spec.getBaseAreaByUtilization().getAll()
					)
					.setMessage(
							"For species {}, Species LH (7.5cm+): {}, Species BA (7.5cm+): {}, Weighted LH (7.5cm+): {}"
					).log();
			vdypLayer.getLoreyHeightByUtilization().scalarInPlace(
					UtilizationClass.SMALL,
					x -> x + spec.getLoreyHeightByUtilization().getSmall() * spec.getBaseAreaByUtilization().getSmall()
			);
			vdypLayer.getLoreyHeightByUtilization().scalarInPlace(
					UtilizationClass.ALL,
					x -> x + spec.getLoreyHeightByUtilization().getAll() * spec.getBaseAreaByUtilization().getAll()
			);
		}
		{
			float baSmall = vdypLayer.getBaseAreaByUtilization().getSmall();
			float baAll = vdypLayer.getBaseAreaByUtilization().getAll();

			if (baSmall > 0) {
				vdypLayer.getLoreyHeightByUtilization().scalarInPlace(UtilizationClass.SMALL, x -> x / baSmall);
			}
			if (baAll > 0) {
				vdypLayer.getLoreyHeightByUtilization().scalarInPlace(UtilizationClass.ALL, x -> x / baAll);
			}

		}

	}

	/**
	 * Sets the Layer's utilization components based on those of its species.
	 *
	 * @param vdypLayer
	 */
	protected static void computeLayerUtilizationComponentsFromSpecies(VdypLayer vdypLayer) {

		// Layer utilization vectors other than quadratic mean diameter are the pairwise
		// sums of those of their species
		sumSpeciesUtilizationVectorsToLayer(vdypLayer);

		{
			var hlVector = Utils.heightVector();
			vdypLayer.getSpecies().values().stream().forEach(spec -> {
				var ba = spec.getBaseAreaByUtilization();
				hlVector.pairwiseInPlace(
						spec.getLoreyHeightByUtilization(),
						(float x, float y, UtilizationClass uc) -> x + y * ba.get(uc)
				);
			});
			var ba = vdypLayer.getBaseAreaByUtilization();
			hlVector.scalarInPlace((float x, UtilizationClass uc) -> ba.get(uc) > 0 ? x / ba.get(uc) : x);
			vdypLayer.setLoreyHeightByUtilization(hlVector);
		}

		// Quadratic mean diameter for the layer is computed from the BA and TPH after
		// they have been found from the species
		{
			var utilVector = vdypLayer.getBaseAreaByUtilization().pairwise(
					vdypLayer.getTreesPerHectareByUtilization(), BaseAreaTreeDensityDiameter::quadMeanDiameter
			);
			vdypLayer.setQuadraticMeanDiameterByUtilization(utilVector);
		}
	}

	// TODO De-reflectify this when we want to make it work in GraalVM
	private static void sumSpeciesUtilizationVectorsToLayer(VdypLayer vdypLayer) throws IllegalStateException {
		try {
			for (var accessors : SUMMABLE_UTILIZATION_VECTOR_ACCESSORS) {
				var utilVector = Utils.utilizationVector();
				for (var vdypSpecies : vdypLayer.getSpecies().values()) {
					var speciesVector = (Coefficients) accessors.getReadMethod().invoke(vdypSpecies);
					utilVector.pairwiseInPlace(speciesVector, (x, y) -> x + y);
				}
				accessors.getWriteMethod().invoke(vdypLayer, utilVector);
			}
		} catch (IllegalAccessException | InvocationTargetException ex) {
			throw new IllegalStateException(ex);
		}
	}

	// TODO De-reflectify this when we want to make it work in GralVM
	protected static void scaleAllSummableUtilization(VdypUtilizationHolder holder, float factor)
			throws IllegalStateException {
		try {
			for (var accessors : SUMMABLE_UTILIZATION_VECTOR_ACCESSORS) {
				((Coefficients) accessors.getReadMethod().invoke(holder)).scalarInPlace(x -> x * factor);
			}
		} catch (IllegalAccessException | InvocationTargetException ex) {
			throw new IllegalStateException(ex);
		}
	}

	/**
	 * Returns the primary, and secondary if present species records as a one or two element list.
	 */
	public <S extends BaseVdypSpecies<I>, I extends BaseVdypSite> List<S> findPrimarySpecies(
			Collection<S> allSpecies, GenusDefinitionMap sp0Lookup, DebugSettings debugSettings,
			SpeciesCopier<S, I> speciesCopier
	) {
		return findPrimarySpecies(
				allSpecies, CommonData.PRIMARY_SPECIES_TO_COMBINE, sp0Lookup, debugSettings, speciesCopier
		);
	}

	/**
	 * Returns the primary, and secondary if present species records as a one or two element list.
	 */
	public <S extends BaseVdypSpecies<I>, I extends BaseVdypSite> List<S> findPrimarySpecies(
			Collection<S> allSpecies, Collection<? extends Collection<String>> speciesToCombine,
			GenusDefinitionMap sp0Lookup, DebugSettings debugSettings, SpeciesCopier<S, I> speciesCopier
	) {
		// Comparators typed to S so they can be used directly on collections of S
		// Type-safe comparator: sort by percentGenus descending, then by SP0 index
		final Comparator<S> percentGenusDescending = Comparator.comparingDouble((S s) -> s.getPercentGenus()).reversed()
				.thenComparingInt(s -> sp0Lookup.getByAlias(s.getGenus()).getIndex());

		final Comparator<S> percentGenusDescendingFudged = (S sp1, S sp2) -> {
			int i1 = sp0Lookup.getByAlias(sp1.getGenus()).getIndex();
			int i2 = sp0Lookup.getByAlias(sp2.getGenus()).getIndex();
			float adjustmentFactor = i1 < i2 ? 0.9995f : 1.0005f;
			return (int) Math.signum(sp2.getPercentGenus() * adjustmentFactor - sp1.getPercentGenus());
		};

		final Comparator<S> comparatorToUse;

		final var speciesGroupPreferenceMode = debugSettings.getSpeciesGroupPreference();
		switch (speciesGroupPreferenceMode) {
		case DEFAULT:
			comparatorToUse = percentGenusDescending;
			break;
		case USE_PREFERRED_WITHIN_TOLERANCE:
			comparatorToUse = percentGenusDescendingFudged;
			break;
		default:
			throw new IllegalStateException(
					MessageFormat.format("Debug flag 22 value of {0} is unknown", speciesGroupPreferenceMode)
			);
		}

		if (allSpecies.isEmpty()) {
			throw new IllegalArgumentException("Can not find primary species as there are no species");
		}
		var result = new ArrayList<S>(2);

		// Start with a deep copy of the species map so there are no side effects from
		// the manipulation this method does.
		var combined = new HashMap<String, S>(allSpecies.size());
		allSpecies.stream().forEach(spec -> combined.put(spec.getGenus(), speciesCopier.copySpecies(spec, x -> {
		})));

		for (var combinationGroup : speciesToCombine) {
			var groupSpecies = combinationGroup.stream().map(combined::get).filter(Objects::nonNull).toList();
			if (groupSpecies.size() < 2) {
				continue;
			}

			// groupSpecies.size() is at least 2 so findFirst will not be empty
			var groupPrimary = speciesCopier.copySpecies(
					groupSpecies.stream().sorted(percentGenusDescending).findFirst().orElseThrow(), builder -> {
						var total = (float) groupSpecies.stream().mapToDouble(BaseVdypSpecies::getPercentGenus).sum();
						builder.percentGenus(total);
					}
			);
			combinationGroup.forEach(combined::remove);
			combined.put(groupPrimary.getGenus(), groupPrimary);
		}

		assert !combined.isEmpty();

		if (combined.size() == 1) {
			// There's only one
			result.addAll(combined.values());
		} else {
			combined.values().stream().sorted(comparatorToUse).limit(2).forEach(result::add);
		}

		assert !result.isEmpty();
		assert result.size() <= 2;
		return result;
	}

	/**
	 * Find Inventory type group (ITG)
	 *
	 * @param primarySecondary
	 * @return
	 * @throws ProcessingException
	 */
	public static int findItg(List<? extends BaseVdypSpecies<?>> primarySecondary) throws UnsupportedSpeciesException {
		var primary = primarySecondary.get(0);

		if (primary.getPercentGenus() > 79.999) { // Copied from VDYP7
			if (ITG_PURE.containsKey(primary.getGenus()))
				return ITG_PURE.get(primary.getGenus());
			else
				throw new UnsupportedSpeciesException(primary.getGenus());
		}
		assert primarySecondary.size() == 2;

		var secondary = primarySecondary.get(1);

		assert !primary.getGenus().equals(secondary.getGenus());

		switch (primary.getGenus()) {
		case "F":
			switch (secondary.getGenus()) {
			case "C", "Y":
				return 2;
			case "B", "H":
				return 3;
			case "S":
				return 4;
			case "PL", "PA":
				return 5;
			case "PY":
				return 6;
			case "L", "PW":
				return 7;
			default:
				return 8;
			}
		case "C", "Y":
			switch (secondary.getGenus()) {
			case "C", "Y":
				return 10;
			case "H", "B", "S":
				return 11;
			default:
				return 10;
			}
		case "B":
			switch (secondary.getGenus()) {
			case "C", "Y", "H":
				return 19;
			default:
				return 20;
			}
		case "H":
			switch (secondary.getGenus()) {
			case "C", "Y":
				return 14;
			case "B":
				return 15;
			case "S":
				return 16;
			default:
				if (HARDWOODS.contains(secondary.getGenus())) {
					return 17;
				}
				return 13;
			}
		case "S":
			switch (secondary.getGenus()) {
			case "C", "Y", "H":
				return 23;
			case "B":
				return 24;
			case "PL":
				return 25;
			default:
				if (HARDWOODS.contains(secondary.getGenus())) {
					return 26;
				}
				return 22;
			}
		case "PW":
			return 27;
		case "PL", "PA":
			switch (secondary.getGenus()) {
			case "PL", "PA":
				return 28;
			case "F", "PW", "L", "PY":
				return 29;
			default:
				if (HARDWOODS.contains(secondary.getGenus())) {
					return 31;
				}
				return 30;
			}
		case "PY":
			return 32;
		case "L":
			switch (secondary.getGenus()) {
			case "F":
				return 33;
			default:
				return 34;
			}
		case "AC":
			if (HARDWOODS.contains(secondary.getGenus())) {
				return 36;
			}
			return 35;
		case "D":
			if (HARDWOODS.contains(secondary.getGenus())) {
				return 38;
			}
			return 37;
		case "MB":
			return 39;
		case "E":
			return 40;
		case "AT":
			if (HARDWOODS.contains(secondary.getGenus())) {
				return 42;
			}
			return 41;
		default:
			throw new UnsupportedSpeciesException(primary.getGenus());
		}
	}

	public static record PerSpeciesLimits(Map<String, Float> minimum, Map<String, Float> maximum) {
	};

	// ROOTV01
	public PerSpeciesLimits
			getDqBySpecies(VdypLayer layer, Region region, BiFunction<String, Region, ComponentSizeLimits> getLimits)
					throws FatalProcessingException {

		// RCOM2/DQ_TOT
		float quadMeanDiameterTotal = layer.getQuadraticMeanDiameterByUtilization().getAll();
		// RCOM2/BA_TOT
		float baseAreaTotal = layer.getBaseAreaByUtilization().getAll();
		// RCOM2/TPH_TOT
		float treeDensityTotal = treesPerHectare(baseAreaTotal, quadMeanDiameterTotal);

		float loreyHeightTotal = layer.getLoreyHeightByUtilization().getAll();

		// RCOM2/DQV
		Map<String, Float> initialDqEstimate = new LinkedHashMap<>(layer.getSpecies().size());
		// RCOM2/BAV
		Map<String, Float> baseAreaPerSpecies = new LinkedHashMap<>(layer.getSpecies().size());
		// RCOM2/DQMIN
		Map<String, Float> minPerSpecies = new LinkedHashMap<>(layer.getSpecies().size());
		// RCOM2/DQMAX
		Map<String, Float> maxPerSpecies = new LinkedHashMap<>(layer.getSpecies().size());
		// RCOM2/DQFINAL
		Map<String, Float> resultsPerSpecies = new LinkedHashMap<>(layer.getSpecies().size());

		getDqBySpeciesInitial(
				// In
				layer, region, quadMeanDiameterTotal, baseAreaTotal, treeDensityTotal, loreyHeightTotal, getLimits,
				// Out
				initialDqEstimate, baseAreaPerSpecies, minPerSpecies, maxPerSpecies
		);

		resultsPerSpecies.putAll(initialDqEstimate);

		findRootForQuadMeanDiameterFractionalError(
				-0.6f, 0.5f, resultsPerSpecies, initialDqEstimate, baseAreaPerSpecies, minPerSpecies, maxPerSpecies,
				treeDensityTotal
		);

		applyDqBySpecies(layer, baseAreaTotal, baseAreaPerSpecies, resultsPerSpecies);

		return new PerSpeciesLimits(minPerSpecies, maxPerSpecies);
	}

	void getDqBySpeciesInitial(
			VdypLayer layer, Region region, float quadMeanDiameterTotal, float baseAreaTotal, float treeDensityTotal,
			float loreyHeightTotal, BiFunction<String, Region, ComponentSizeLimits> getLimits,
			Map<String, Float> initialDqEstimate, Map<String, Float> baseAreaPerSpecies,
			Map<String, Float> minPerSpecies, Map<String, Float> maxPerSpecies
	) throws FatalProcessingException {
		for (var spec : layer.getSpecies().values()) {
			// EMP060
			float specDq = estimationMethods.estimateQuadMeanDiameterForSpecies(
					spec, layer.getSpecies(), region, quadMeanDiameterTotal, baseAreaTotal, treeDensityTotal,
					loreyHeightTotal
			);

			var limits = getLimits.apply(spec.getGenus(), region);

			float min = Math.max(
					7.6f, limits.minQuadMeanDiameterLoreyHeightRatio() * spec.getLoreyHeightByUtilization().getAll()
			);
			float loreyHeightToUse = Math.max(spec.getLoreyHeightByUtilization().getAll(), 7.0f);
			float max = Math.min(
					limits.quadMeanDiameterMaximum(), limits.maxQuadMeanDiameterLoreyHeightRatio() * loreyHeightToUse
			);
			max = Math.max(7.75f, max);

			minPerSpecies.put(spec.getGenus(), min);
			maxPerSpecies.put(spec.getGenus(), max);

			specDq = FloatMath.clamp(specDq, Math.max(min, 7.75f), max);

			initialDqEstimate.put(spec.getGenus(), specDq);

			baseAreaPerSpecies.put(spec.getGenus(), spec.getBaseAreaByUtilization().getAll());
		}
	}

	float findRootForQuadMeanDiameterFractionalError(
			float min, float max, Map<String, Float> resultPerSpecies, Map<String, Float> initialDqs,
			Map<String, Float> baseAreas, Map<String, Float> minDq, Map<String, Float> maxDq, float tph
	) throws FatalProcessingException {

		// Note, this function has side effects in that it modifies resultPerSpecies. This is intentional, the goal is
		// to apply adjustment factor x to the values in initialDqs until the combination of their values has minimal
		// error then use those adjusted values.

		// Keeping track of the recent X values tied can be used to make some sort of guess if it doesn't converge.
		double[] lastXes = new double[2];
		double[] lastFs = new double[2];

		final double tol = 0.00001;

		UnivariateFunction errorFunc = createQuadMeanDiameterFractionalErrorFunction(
				resultPerSpecies, initialDqs, baseAreas, minDq, maxDq, tph, lastXes, lastFs
		);

		debugModeExpandRootSearchWindow(
				getNonFipDebugModes().flatMap(NonFipDebugSettings::getExpandDiameterForTPHRecovery), minDq, maxDq,
				errorFunc
		);
		// If the search window was expanded we need to rebox the min and max values in the univariate function
		errorFunc = createQuadMeanDiameterFractionalErrorFunction(
				resultPerSpecies, initialDqs, baseAreas, minDq, maxDq, tph, lastXes, lastFs
		);
		try {
			double x = doSolve(min, max, errorFunc);

			return (float) x;
		} catch (CouldNotFindBracketingIntervalException ex) {
			double x;
			if (ex.isExitEarly()) {
				x = ex.getLo();
				// Note that in this case we don't call handleRootForQuadMeanDiameterFractionalErrorException to
				// potentially error out. This is how VDYP 7 works although I'm not sure why.
			} else {
				// Decide if we want to propagate the exception or try to come up with something anyway.
				handleRootForQuadMeanDiameterFractionalErrorException(ex);

				// Try three values and take the least bad option.

				x = bestOf(errorFunc, 0, -0.1, 0.1);
			}
			// Invoke the function again to set the species map via
			var error = errorFunc.value(x);
			log.atWarn().setMessage(
					"Failed to reconcile total DQ/TPH for species with layer.  Using a distribution that has an error of {}."
			).addArgument(error).log();

			return (float) x;

		} catch (TooManyEvaluationsException ex) {

			if (tol > 0.0 && Math.abs(lastFs[0]) < tol / 2) {

				// Decide if we want to propagate the exception or try to use the last result.
				handleRootForQuadMeanDiameterFractionalErrorException(ex);

				return (float) lastXes[0];

			}

			throw new FatalProcessingException(
					"Could not find solution for quadratic mean diameter.  There appears to be a discontinuity.", ex
			);

		}
	}

	Optional<NonFipDebugSettings> getNonFipDebugModes() {
		return Optional.of(this.controlMap.getDebugSettings()).filter(NonFipDebugSettings.class::isInstance)
				.map(NonFipDebugSettings.class::cast);
	}

	private UnivariateFunction createQuadMeanDiameterFractionalErrorFunction(
			Map<String, Float> resultPerSpecies, Map<String, Float> initialDqs, Map<String, Float> baseAreas,
			Map<String, Float> minDq, Map<String, Float> maxDq, float tph, double[] lastXes, double[] lastFs
	) {
		return x -> {
			lastXes[1] = lastXes[0];
			lastXes[0] = x;
			lastFs[1] = lastFs[0];
			lastFs[0] = this
					.quadMeanDiameterFractionalError(x, resultPerSpecies, initialDqs, baseAreas, minDq, maxDq, tph);
			return lastFs[0];
		};
	}

	void debugModeExpandRootSearchWindow(
			Optional<Float> recoveryFactor, Map<String, Float> minDq, Map<String, Float> maxDq,
			UnivariateFunction errorFunc
	) {
		recoveryFactor.ifPresent(p -> {
			final double f1 = errorFunc.value(-10f);
			final double f2 = errorFunc.value(10f);
			final float base = 7.5f;
			if (f2 * f1 > 0d) {
				float lowFactor = 1.0f - p;
				float highFactor = 1.0f + p;
				for (var entry : maxDq.entrySet()) {
					var key = entry.getKey();
					minDq.put(key, base + lowFactor * (minDq.get(key) - base));
					maxDq.put(key, base + highFactor * (entry.getValue() - base));
				}
			}
		});
	}

	double doSolve(float min, float max, UnivariateFunction errorFunc) {
		var interval = new Interval(min, max);

		// I couldn't identify the method the original Fortran was using, so I just picked one and it worked
		// We could swap this for another like NewtonRaphsonSolver
		var solver = new BrentSolver();

		// The Fortran solver library, $ZERO, included an ability to search for a better interval if given one where
		// the function values at the end points have the same sign. This replicates that.
		interval = findInterval(interval, errorFunc, (i, x) -> i >= 2 && Math.abs(x) > 20);

		return solver.solve(100, errorFunc, interval.start(), interval.end(), interval.mid());

	}

	private void handleRootForQuadMeanDiameterFractionalErrorException(RuntimeException ex)
			throws FatalProcessingException {
		// Only do this in VRIStart

		if (getNonFipDebugModes().map(NonFipDebugSettings::getMode1ErrorsFatal).orElse(false)) {
			throw new FatalProcessingException("Could not find solution for quadratic mean diameter", ex);
		}

		log.atWarn().setMessage("Could not find exact solution for quadratic mean diameter.  Using inexact estimate.")
				.setCause(ex);

	}

	/**
	 * This replicates the behavior of the SZERO root finding library used by VDYP7
	 *
	 * @param interval Initial interval of parameters to func
	 * @param func
	 * @return an interval for parameters to func
	 */
	public Interval findInterval(Interval intervalInit, UnivariateFunction func, BiPredicate<Integer, Double> breakIf) {

		var interval = intervalInit;
		// Try 40 times before giving up.

		double x1 = interval.start();
		double x2 = interval.end();
		double f1 = func.value(x1);
		double f2 = func.value(x2);

		double currentX = x1; // XX
		double currentF = f1; // FF
		// the "last" variables are set once the first time SZERO is called, then left uninitialized on subsequent
		// calls which has them preserve state over multiple iterations.
		double lastX = x2; // XL
		double lastF = f2; // FL
		int i;
		for (i = 0; i < 40; i++) {

			currentX = x1; // XX
			currentF = f1; // FF
			if (breakIf.test(i, x1)) {
				throw new CouldNotFindBracketingIntervalException(currentX, lastX, currentF, lastF, i, true);
			}

			if (currentF * lastF <= 0) {
				var newInterval = new Interval(Math.min(currentX, lastX), Math.max(currentX, lastX));
				log.atInfo().setMessage("Looking for root in range {}").addArgument(interval);
				return newInterval;
			}

			double tp = currentF / lastF;

			if (tp > 1) {
				double temp = currentX;
				currentX = lastX;
				lastX = temp;
				temp = currentF;
				currentF = lastF;
				lastF = temp;
			}

			if (Math.abs(currentF) >= 8 * Math.abs(lastF - currentF)) {
				tp = 8;
			} else {
				tp = Math.max(0.25 * (i + 1), currentF / (lastF - currentF));
			}

			lastF = currentF;
			double oppositeX = lastX; // XO
			lastX = currentX;
			if (currentX == oppositeX) {
				oppositeX = 1.03125 * currentX + (0.001 * Math.signum(currentX));
			}
			currentX += tp * (currentX - oppositeX);

			x1 = currentX;

			// In the original Fortran this would happen in the subroutine calling SZERO
			f1 = func.value(x1);
		}

		throw new CouldNotFindBracketingIntervalException(currentX, lastX, currentF, lastF, i - 1, false);
	}

	float quadMeanDiameterFractionalError(
			double x, Map<String, Float> finalDiameters, Map<String, Float> initial, Map<String, Float> baseArea,
			Map<String, Float> min, Map<String, Float> max, float totalTreeDensity
	) {
		finalDiameters.clear();

		float xToUse = FloatMath.clamp((float) x, -10, 10);

		double tphSum = initial.entrySet().stream().mapToDouble(spec -> {
			float speciesFinal = quadMeanDiameterSpeciesAdjust(
					xToUse, spec.getValue(), min.get(spec.getKey()), max.get(spec.getKey())
			);
			finalDiameters.put(spec.getKey(), speciesFinal);
			return treesPerHectare(baseArea.get(spec.getKey()), speciesFinal);
		}).sum();

		return (float) ( (tphSum - totalTreeDensity) / totalTreeDensity);
	}

	float quadMeanDiameterSpeciesAdjust(float x, float initialDq, float min, float max) {
		return FloatMath.clamp(
				UtilizationClass.U75TO125.lowBound
						+ (initialDq - UtilizationClass.U75TO125.lowBound) * FloatMath.exp(x),
				min, max
		);
	}

	void applyDqBySpecies(
			VdypLayer layer, float baseAreaTotal, Map<String, Float> baseAreaPerSpecies,
			Map<String, Float> resultsPerSpecies
	) {
		float quadMeanDiameterTotal;
		float treeDensityTotal;
		treeDensityTotal = 0;
		for (var spec : layer.getSpecies().values()) {
			float specDq = resultsPerSpecies.get(spec.getGenus());
			float specBa = baseAreaPerSpecies.get(spec.getGenus());
			float specTph = treesPerHectare(specBa, specDq);
			treeDensityTotal += specTph;
			spec.getQuadraticMeanDiameterByUtilization().setAll(specDq);
			spec.getTreesPerHectareByUtilization().setAll(specTph);
		}
		quadMeanDiameterTotal = quadMeanDiameter(baseAreaTotal, treeDensityTotal);
		layer.getTreesPerHectareByUtilization().setAll(treeDensityTotal);
		layer.getQuadraticMeanDiameterByUtilization().setAll(quadMeanDiameterTotal);
	}

	/**
	 * Returns the x value for which func(x) is closest to 0.
	 *
	 * @param func
	 * @param values
	 * @return
	 */
	static double bestOf(UnivariateFunction func, double... values) {
		if (values.length <= 0) {
			throw new IllegalArgumentException("bestOf requires at least one point to compare");
		}
		double bestX = values[0];
		double bestY = func.value(bestX);
		for (int i = 1; i < values.length; i++) {
			double newX = values[i];
			double newY = func.value(newX);
			if (Math.abs(newY) < Math.abs(bestY)) {
				bestX = newX;
				bestY = newY;
			}
		}
		return bestX;
	}
}
