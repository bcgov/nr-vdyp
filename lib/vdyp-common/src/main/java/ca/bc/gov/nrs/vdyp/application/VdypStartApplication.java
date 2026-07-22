package ca.bc.gov.nrs.vdyp.application;

import static ca.bc.gov.nrs.vdyp.math.FloatMath.clamp;
import static ca.bc.gov.nrs.vdyp.math.FloatMath.floor;
import static java.lang.Math.max;
import static java.lang.Math.min;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.common.ComputationMethods;
import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.common.Utils;
import ca.bc.gov.nrs.vdyp.common.ValueOrMarker;
import ca.bc.gov.nrs.vdyp.common.VdypApplicationInitializationException;
import ca.bc.gov.nrs.vdyp.common_calculators.BaseAreaTreeDensityDiameter;
import ca.bc.gov.nrs.vdyp.controlmap.ResolvedControlMap;
import ca.bc.gov.nrs.vdyp.controlmap.StartResolvedControlMapImpl;
import ca.bc.gov.nrs.vdyp.exceptions.FatalProcessingException;
import ca.bc.gov.nrs.vdyp.exceptions.LayerMissingException;
import ca.bc.gov.nrs.vdyp.exceptions.LayerSpeciesDoNotSumTo100PercentException;
import ca.bc.gov.nrs.vdyp.exceptions.ProcessingException;
import ca.bc.gov.nrs.vdyp.exceptions.StandProcessingException;
import ca.bc.gov.nrs.vdyp.exceptions.UnsupportedSpeciesException;
import ca.bc.gov.nrs.vdyp.io.FileSystemFileResolver;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.io.parse.streaming.StreamingParser;
import ca.bc.gov.nrs.vdyp.io.parse.streaming.StreamingParserFactory;
import ca.bc.gov.nrs.vdyp.io.write.VdypOutputWriter;
import ca.bc.gov.nrs.vdyp.model.BaseVdypLayer;
import ca.bc.gov.nrs.vdyp.model.BaseVdypPolygon;
import ca.bc.gov.nrs.vdyp.model.BaseVdypSite;
import ca.bc.gov.nrs.vdyp.model.BaseVdypSpecies;
import ca.bc.gov.nrs.vdyp.model.BecDefinition;
import ca.bc.gov.nrs.vdyp.model.Coefficients;
import ca.bc.gov.nrs.vdyp.model.DebugSettings;
import ca.bc.gov.nrs.vdyp.model.GenusDefinitionMap;
import ca.bc.gov.nrs.vdyp.model.InputLayer;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2;
import ca.bc.gov.nrs.vdyp.model.PolygonMode;
import ca.bc.gov.nrs.vdyp.model.Region;
import ca.bc.gov.nrs.vdyp.model.UtilizationClass;
import ca.bc.gov.nrs.vdyp.model.UtilizationVector;
import ca.bc.gov.nrs.vdyp.model.VdypLayer;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;
import ca.bc.gov.nrs.vdyp.model.VdypSpecies;
import ca.bc.gov.nrs.vdyp.model.VdypUtilizationHolder;

/**
 * Base class for a start processing application. Provides shared framework between VRIStart and FIPStart
 *
 * @param <P> input polygon class
 * @param <L> input layer class
 * @param <S> input species class
 * @param <I> input site class
 */
public abstract class VdypStartApplication<P extends BaseVdypPolygon<L, Optional<Float>, S, I>, L extends BaseVdypLayer<S, I> & InputLayer, S extends BaseVdypSpecies<I>, I extends BaseVdypSite, D extends DebugSettings>
		extends VdypApplication<D> implements AutoCloseable, SpeciesCopier<S, I> {

	public static final Logger log = LoggerFactory.getLogger(VdypStartApplication.class);

	static final Map<String, Integer> ITG_PURE = Utils.constMap(map -> {
		map.put("AC", 36);
		map.put("AT", 42);
		map.put("B", 18);
		map.put("C", 9);
		map.put("D", 38);
		map.put("E", 40);
		map.put("F", 1);
		map.put("H", 12);
		map.put("L", 34);
		map.put("MB", 39);
		map.put("PA", 28);
		map.put("PL", 28);
		map.put("PW", 27);
		map.put("PY", 32);
		map.put("S", 21);
		map.put("Y", 9);
	});

	protected PolygonMode modeUsed = PolygonMode.DONT_PROCESS;

	public PolygonMode getModeUsed() {
		return modeUsed;
	}

	static final Set<String> HARDWOODS = Set.of("AC", "AT", "D", "E", "MB");

	/**
	 * Accessor methods for utilization vectors, except for Lorey Height, on Layer and Species objects.
	 */
	protected static final Collection<PropertyDescriptor> UTILIZATION_VECTOR_ACCESSORS;

	/**
	 * Accessor methods for utilization vectors, except for Lorey Height and Quadratic Mean Diameter, on Layer and
	 * Species objects. These are properties where the values for the layer are the sum of those for its species.
	 */
	static final Collection<PropertyDescriptor> SUMMABLE_UTILIZATION_VECTOR_ACCESSORS;

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

	protected VdypOutputWriter vriWriter;

	/**
	 * When finding primary species these genera should be combined
	 */
	protected static final Collection<Collection<String>> PRIMARY_SPECIES_TO_COMBINE = Arrays
			.asList(Arrays.asList("PL", "PA"), Arrays.asList("C", "Y"));

	protected VdypStartApplication() {
		super();
	}

	protected VdypOutputWriter createWriter(FileSystemFileResolver resolver, Map<String, Object> controlMap)
			throws IOException {
		return new VdypOutputWriter(controlMap, resolver);
	}

	void closeVriWriter() throws IOException {
		if (vriWriter != null) {
			vriWriter.close();
			vriWriter = null;
		}
	}

	protected <T> StreamingParser<T> getStreamingParser(ControlKey key) throws ProcessingException {
		try {
			var factory = Utils
					.<StreamingParserFactory<T>>expectParsedControl(controlMap, key, StreamingParserFactory.class);

			return factory.get();
		} catch (IllegalStateException ex) {
			throw new ProcessingException(
					MessageFormat.format(
							"Data file {0} ({1}) not specified in control map.", key, Utils.optPretty(key.sequence)
					), ex
			);
		} catch (IOException ex) {
			throw new ProcessingException(MessageFormat.format("Error while opening data file {0}.", key), ex);
		}
	}

	@Override
	public void close() throws VdypApplicationInitializationException {
		try {
			closeVriWriter();
		} catch (IOException e) {
			throw new VdypApplicationInitializationException(e);
		}
	}

	protected Coefficients getCoeForSpecies(BaseVdypSpecies<?> species, ControlKey controlKey) {
		var coeMap = Utils.<Map<String, Coefficients>>expectParsedControl(controlMap, controlKey, java.util.Map.class);
		return coeMap.get(species.getGenus());
	}

	/**
	 * Get the sum of the percentages of the species in a layer. Throws an exception if this differs from the expected
	 * 100% by too much.
	 *
	 * @param layer
	 * @return
	 * @throws StandProcessingException
	 */
	protected float getPercentTotal(L layer) throws LayerSpeciesDoNotSumTo100PercentException {
		var percentTotal = (float) layer.getSpecies().values().stream()//
				.mapToDouble(BaseVdypSpecies::getPercentGenus)//
				.sum();
		if (Math.abs(percentTotal - 100f) > 0.01f) {
			throw new LayerSpeciesDoNotSumTo100PercentException(layer.getLayerType());
		}
		return percentTotal;
	}

	/**
	 * Returns the primary, and secondary if present species records as a one or two element list.
	 */
	protected List<S> findPrimarySpecies(Collection<S> allSpecies) {
		var sp0Lookup = Utils.expectParsedControl(controlMap, ControlKey.SP0_DEF, GenusDefinitionMap.class);
		return computers.findPrimarySpecies(allSpecies, sp0Lookup, getDebugModes(), this);
	}

	/**
	 * Find Inventory type group (ITG)
	 *
	 * @param primarySecondary
	 * @return
	 * @throws ProcessingException
	 */
	protected int findItg(List<S> primarySecondary) throws UnsupportedSpeciesException {
		return ComputationMethods.findItg(primarySecondary);
	}

	public int findEmpiricalRelationshipParameterIndex(String specAlias, BecDefinition bec, int itg) {
		var groupMap = Utils.<MatrixMap2<String, String, Integer>>expectParsedControl(
				controlMap, ControlKey.DEFAULT_EQ_NUM, ca.bc.gov.nrs.vdyp.model.MatrixMap2.class
		);
		var modMap = Utils.<MatrixMap2<Integer, Integer, Integer>>expectParsedControl(
				controlMap, ControlKey.EQN_MODIFIERS, ca.bc.gov.nrs.vdyp.model.MatrixMap2.class
		);
		var group = groupMap.get(specAlias, bec.getGrowthBec().getAlias());
		var modGroup = modMap.get(group, itg);
		return modGroup > 0 ? modGroup : group;
	}

	protected VdypOutputWriter getVriWriter() {
		return vriWriter;
	}

	protected abstract float getYieldFactor(P polygon);

	protected Optional<Float> getLayerAgeTotal(L layer) {
		return layer.getPrimarySite().flatMap(BaseVdypSite::getAgeTotal);
	}

	protected Optional<Float> getLayerYearstoBreastHhight(L layer) {
		return layer.getPrimarySite().flatMap(BaseVdypSite::getYearsToBreastHeight);
	}

	protected Optional<Float> getLayerBreastHeightAge(L layer) {
		// TODO implement accessor for VRI and FIP Site. InputSite interface?
		return layer.getPrimarySite().flatMap(
				site -> Utils.mapBoth(site.getAgeTotal(), site.getYearsToBreastHeight(), (at, ytbh) -> at - ytbh)
		);
	}

	/**
	 * Return the value of the given optional otherwise throw a FatalProcessingException
	 *
	 * @param <T>  type of the value
	 * @param opt  optional to check
	 * @param name name of the field for the error message
	 * @return the value of the optional
	 * @throws FatalProcessingException if it is not present
	 */
	protected static <T> T require(Optional<T> opt, String name) throws FatalProcessingException {
		return opt.orElseThrow(() -> new FatalProcessingException(name + " is not present"));
	}

	/**
	 * Require that the optional value is present and has a positive value.
	 *
	 * @param <T>  type of the value
	 * @param opt  optional to check
	 * @param name name to use in exception message
	 * @return The value
	 * @throws FatalProcessingException if it is not present or has a non-positive value
	 */
	protected static <T extends Number> T requirePositive(Optional<T> opt, String name)
			throws FatalProcessingException {

		T value = require(opt, name);

		if (value.doubleValue() <= 0) {
			throw new FatalProcessingException(name + " " + value + " is not positive");
		}

		return value;
	}

	static public <P extends BaseVdypPolygon<L, ?, ?, ?>, L extends BaseVdypLayer<?, ?>> L
			requireLayer(P polygon, LayerType type) throws LayerMissingException {
		if (!polygon.getLayers().containsKey(type)) {
			throw new LayerMissingException(type);
		}

		return polygon.getLayers().get(type);
	}

	/**
	 * Gets the PRIMARY layer from the given polygon
	 *
	 * @param <P>  The Polygon type
	 * @param <L>  The Layer type
	 * @param poly the polygon
	 * @return the PRIMARY layer
	 * @throws LayerMissingException if the polygon does not have a PRIMARY layer
	 */
	static public <P extends BaseVdypPolygon<L, ?, ?, ?>, L extends BaseVdypLayer<?, ?>> L getPrimaryLayer(P poly)
			throws LayerMissingException {
		return requireLayer(poly, LayerType.PRIMARY);
	}

	/**
	 * Gets the VETERAN layer from the given polygon
	 *
	 * @param <P>  The Polygon type
	 * @param <L>  The Layer type
	 * @param poly
	 * @return the VETERAN layer, or empty if there is none
	 */
	static public <P extends BaseVdypPolygon<L, ?, ?, ?>, L extends BaseVdypLayer<?, ?>> Optional<L>
			getVeteranLayer(P poly) {
		return Utils.optSafe(poly.getLayers().get(LayerType.VETERAN));
	}

	/**
	 * If the given optional contains an exception, throw it as a FatalProcessingException, wrapping it in a new
	 * exception if necessary.
	 */
	protected static <E extends Throwable> void fatalIfPresent(Optional<E> opt) throws FatalProcessingException {
		Utils.throwIfPresent(opt.map(ex -> {
			if (ex instanceof FatalProcessingException fatal) {
				return fatal;
			}
			return new FatalProcessingException(ex);
		}));
	}

	/**
	 * Create a FatalProcessingException with its message created using {@link MessageFormat.format}
	 *
	 * @param template message template
	 * @param values   objects to format
	 * @return a constructed FatalProcessingException
	 */
	protected static FatalProcessingException fatalError(String template, Object... values) {

		return new FatalProcessingException(MessageFormat.format(template, values));
	}

	/**
	 * Create a FatalProcessingException with its message created using {@link MessageFormat.format}
	 *
	 * @param template message template
	 * @param cause    the cause of the exception
	 * @param values   objects to format
	 * @return a constructed FatalProcessingException
	 */
	protected static FatalProcessingException causedFatalError(String template, Throwable cause, Object... values) {

		return new FatalProcessingException(String.format(template, values), cause);
	}

	// FIPLAND
	public float estimatePercentForestLand(P polygon, Optional<L> vetLayer, L primaryLayer) {
		if (polygon.getPercentAvailable().isPresent()) {
			return polygon.getPercentAvailable().get();
		}

		assert primaryLayer != null;

		final boolean veteran;
		{
			var resultOrIsVeteran = isVeteranForEstimatePercentForestLand(polygon, vetLayer);
			if (resultOrIsVeteran.isValue()) {
				return resultOrIsVeteran.getValue().orElseThrow();
			}

			veteran = resultOrIsVeteran.getMarker().orElseThrow();
		}

		float primaryAgeTotal = getLayerAgeTotal(primaryLayer).orElseThrow();

		float crownClosure = crownClosureForPercentForestLand(vetLayer, primaryLayer, veteran, primaryAgeTotal);

		/*
		 * assume that CC occurs at age 25 and that most land goes to 90% occupancy but that occupancy increases only 1%
		 * /yr with no increases after ages 25. });
		 */

		// Obtain the percent yield (in comparison with CC = 90%)

		float crownClosureTop = 90f;
		float breastHeightAge = primaryAgeTotal
				- primaryLayer.getPrimarySite().flatMap(BaseVdypSite::getYearsToBreastHeight).orElseThrow();

		float yieldFactor = getYieldFactor(polygon);

		var bec = polygon.getBiogeoclimaticZone();

		breastHeightAge = max(5.0f, breastHeightAge);
		// EMP040
		float baseAreaTop = estimationMethods
				.estimatePrimaryBaseAreaAdjust(primaryLayer, bec, yieldFactor, breastHeightAge, 0f, crownClosureTop);
		// EMP040
		float baseAreaHat = estimationMethods
				.estimatePrimaryBaseAreaAdjust(primaryLayer, bec, yieldFactor, breastHeightAge, 0f, crownClosure);

		float percentYield;
		if (baseAreaTop > 0f && baseAreaHat > 0f) {
			percentYield = min(100f, 100f * baseAreaHat / baseAreaTop);
		} else {
			percentYield = 90f;
		}

		float gainMax;
		if (primaryAgeTotal > 125f) {
			gainMax = 0f;
		} else if (primaryAgeTotal < 25f) {
			gainMax = max(90f - percentYield, 0);
		} else {
			gainMax = max(90f - percentYield, 0);
			gainMax = min(gainMax, 125 - primaryAgeTotal);
		}

		return floor(min(percentYield + gainMax, 100f));

	}

	protected static final ValueOrMarker.Builder<Float, Boolean> FLOAT_OR_BOOL = ValueOrMarker
			.builder(Float.class, Boolean.class);

	public static final Collection<UtilizationClass> UTIL_CLASSES = List.of(
			UtilizationClass.U75TO125, UtilizationClass.U125TO175, UtilizationClass.U175TO225, UtilizationClass.OVER225
	);

	protected ValueOrMarker<Float, Boolean> isVeteranForEstimatePercentForestLand(P polygon, Optional<L> vetLayer) {
		boolean veteran = vetLayer//
				.filter(layer -> estimationMethods.getLayerHeight(layer).orElse(0f) > 0f) //
				.filter(layer -> layer.getCrownClosure() > 0f)//
				.isPresent(); // LAYERV

		return FLOAT_OR_BOOL.marker(veteran);
	}

	private float crownClosureForPercentForestLand(
			Optional<L> vetLayer, L primaryLayer, boolean veteran, float primaryAgeTotal
	) {
		float crownClosure = primaryLayer.getCrownClosure();

		// Assume crown closure linear with age, to 25.
		if (primaryAgeTotal < 25f) {
			crownClosure *= 25f / primaryAgeTotal;
		}
		// define crown closure as the SUM of two layers
		if (veteran) {
			crownClosure += vetLayer.map(InputLayer::getCrownClosure).orElse(0f);
		}

		crownClosure = clamp(crownClosure, 0, 100);
		return crownClosure;
	}

	protected Map<String, Float> applyGroupsAndGetTargetPercentages(
			BaseVdypPolygon<?, ?, ?, ?> fipPolygon, Collection<VdypSpecies> vdypSpecies
	) {

		applyGroups(fipPolygon, vdypSpecies);
		return getTargetPercentages(vdypSpecies);
	}

	protected void applyGroups(BaseVdypPolygon<?, ?, ?, ?> fipPolygon, Collection<VdypSpecies> vdypSpecies) {
		// Lookup volume group, Decay Group, and Breakage group for each species.

		BecDefinition bec = fipPolygon.getBiogeoclimaticZone();
		var volumeGroupMap = getGroupMap(ControlKey.VOLUME_EQN_GROUPS);
		var decayGroupMap = getGroupMap(ControlKey.DECAY_GROUPS);
		var breakageGroupMap = getGroupMap(ControlKey.BREAKAGE_GROUPS);
		for (var vSpec : vdypSpecies) {
			// VGRPFIND
			var volumeGroup = volumeGroupMap.get(vSpec.getGenus(), bec.getVolumeBec().getAlias());
			// DGRPFIND
			var decayGroup = decayGroupMap.get(vSpec.getGenus(), bec.getDecayBec().getAlias());
			// BGRPFIND (Breakage uses decay BEC)
			var breakageGroup = breakageGroupMap.get(vSpec.getGenus(), bec.getDecayBec().getAlias());

			vSpec.setVolumeGroup(volumeGroup);
			vSpec.setDecayGroup(decayGroup);
			vSpec.setBreakageGroup(breakageGroup);

		}

	}

	protected Map<String, Float> getTargetPercentages(Collection<VdypSpecies> vdypSpecies) {
		Map<String, Float> targetPercentages = new HashMap<>(vdypSpecies.size());

		for (var vSpec : vdypSpecies) {

			targetPercentages.put(vSpec.getGenus(), vSpec.getPercentGenus());
		}

		return targetPercentages;
	}

	protected void applyGroups(BecDefinition bec, String genus, VdypSpecies.Builder builder) {
		// Lookup volume group, Decay Group, and Breakage group for each species.

		var volumeGroupMap = getGroupMap(ControlKey.VOLUME_EQN_GROUPS);
		var decayGroupMap = getGroupMap(ControlKey.DECAY_GROUPS);
		var breakageGroupMap = getGroupMap(ControlKey.BREAKAGE_GROUPS);

		// VGRPFIND
		var volumeGroup = volumeGroupMap.get(genus, bec.getVolumeBec().getAlias());
		// DGRPFIND
		var decayGroup = decayGroupMap.get(genus, bec.getDecayBec().getAlias());
		// BGRPFIND (Breakage uses decay BEC)
		var breakageGroup = breakageGroupMap.get(genus, bec.getDecayBec().getAlias());

		builder.volumeGroup(volumeGroup);
		builder.decayGroup(decayGroup);
		builder.breakageGroup(breakageGroup);

	}

	protected MatrixMap2<String, String, Integer> getGroupMap(ControlKey key) {
		return Utils.expectParsedControl(controlMap, key, ca.bc.gov.nrs.vdyp.model.MatrixMap2.class);
	}

	// YSMALL(0, X)
	/**
	 * Estimate small components for primary layer
	 *
	 * @throws ProcessingException
	 */
	public void estimateSmallComponents(P fPoly, VdypLayer layer) {
		float loreyHeightSum = 0f;
		float baseAreaSum = 0f;
		float treesPerHectareSum = 0f;
		float volumeSum = 0f;

		Region region = fPoly.getBiogeoclimaticZone().getRegion();

		for (VdypSpecies spec : layer.getSpecies().values()) {
			@SuppressWarnings("unused")
			float loreyHeightSpec = spec.getLoreyHeightByUtilization().getAll(); // HLsp
			@SuppressWarnings("unused")
			float quadMeanDiameterSpec = spec.getQuadraticMeanDiameterByUtilization().getAll(); // DQsp

			// EMP080
			float smallComponentProbability = estimationMethods.estimateSmallComponentProbability(layer, spec, region); // PROBsp

			// this WHOLE operation on Actual BA's, not 100% occupancy.
			float fractionAvailable = Utils.<Float>optSafe(fPoly.getPercentAvailable()).map(p -> p / 100f).orElse(1f);

			// EMP081
			float conditionalExpectedBaseArea = estimationMethods
					.estimateSmallComponentConditionalExpectedBasalAreaNormalized(spec, fractionAvailable, region); // BACONDsp

			float baseAreaSpecSmall = smallComponentProbability * conditionalExpectedBaseArea; // BASMsp

			// EMP082
			float quadMeanDiameterSpecSmall = estimationMethods.estimateSmallComponentQuadMeanDiameter(spec); // DQSMsp

			// EMP085
			float loreyHeightSpecSmall = estimationMethods
					.estimateSmallComponentLoreyHeight(spec, quadMeanDiameterSpecSmall); // HLSMsp

			// EMP086
			float meanVolumeSmall = this.estimationMethods
					.estimateMeanVolumeSmall(spec, loreyHeightSpecSmall, quadMeanDiameterSpecSmall); // VMEANSMs

			// TODO Apply Compatibility Variables, not needed for FIPSTART or VRISTART

			spec.getLoreyHeightByUtilization().setSmall(loreyHeightSpecSmall);
			float treesPerHectareSpecSmall = BaseAreaTreeDensityDiameter
					.treesPerHectare(baseAreaSpecSmall, quadMeanDiameterSpecSmall); // TPHSMsp
			spec.getBaseAreaByUtilization().setSmall(baseAreaSpecSmall);
			spec.getTreesPerHectareByUtilization().setSmall(treesPerHectareSpecSmall);
			spec.getQuadraticMeanDiameterByUtilization().setSmall(quadMeanDiameterSpecSmall);
			float wholeStemVolumeSpecSmall = treesPerHectareSpecSmall * meanVolumeSmall; // VOLWS(I,-1)
			spec.getWholeStemVolumeByUtilization().setSmall(wholeStemVolumeSpecSmall);

			loreyHeightSum += baseAreaSpecSmall * loreyHeightSpecSmall;
			baseAreaSum += baseAreaSpecSmall;
			treesPerHectareSum += treesPerHectareSpecSmall;
			volumeSum += wholeStemVolumeSpecSmall;
		}

		if (baseAreaSum > 0f) {
			layer.getLoreyHeightByUtilization().setSmall(loreyHeightSum / baseAreaSum);
		} else {
			layer.getLoreyHeightByUtilization().setSmall(0f);
		}
		layer.getBaseAreaByUtilization().setSmall(baseAreaSum);
		layer.getTreesPerHectareByUtilization().setSmall(treesPerHectareSum);
		layer.getQuadraticMeanDiameterByUtilization()
				.setSmall(BaseAreaTreeDensityDiameter.quadMeanDiameter(baseAreaSum, treesPerHectareSum));
		layer.getWholeStemVolumeByUtilization().setSmall(volumeSum);
	}

	/**
	 * Sets the Layer's utilization components based on those of its species.
	 *
	 * @param vdypLayer
	 */
	protected void computeLayerUtilizationComponentsFromSpecies(VdypLayer vdypLayer) {
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

			// Update percent based on updated areas
			vdypLayer.getSpecies().values().stream().forEach(spec -> {
				spec.setPercentGenus(100 * spec.getBaseAreaByUtilization().getAll() / ba.getAll());
			});

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

	// TODO De-reflectify this when we want to make it work in GralVM
	void sumSpeciesUtilizationVectorsToLayer(VdypLayer vdypLayer) throws IllegalStateException {
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
	protected void scaleAllSummableUtilization(VdypUtilizationHolder holder, float factor)
			throws IllegalStateException {
		try {
			for (var accessors : SUMMABLE_UTILIZATION_VECTOR_ACCESSORS) {
				((Coefficients) accessors.getReadMethod().invoke(holder)).scalarInPlace(x -> x * factor);
			}
		} catch (IllegalAccessException | InvocationTargetException ex) {
			throw new IllegalStateException(ex);
		}
	}

	// YUCV
	protected void computeUtilizationComponentsVeteran(VdypLayer vdypLayer, BecDefinition bec)
			throws ProcessingException {
		log.trace(
				"computeUtilizationComponentsVeteran for {}, stand total age is {}", vdypLayer.getPolygonIdentifier(),
				vdypLayer.getAgeTotal()
		);

		var volumeAdjustMap = Utils.<Map<String, Coefficients>>expectParsedControl(
				controlMap, ControlKey.VETERAN_LAYER_VOLUME_ADJUST, java.util.Map.class
		);
		try {
			for (var vdypSpecies : vdypLayer.getSpecies().values()) {

				var treesPerHectareUtil = Utils.utilizationVector();
				var quadMeanDiameterUtil = Utils.utilizationVector();
				var baseAreaUtil = Utils.utilizationVector();
				var wholeStemVolumeUtil = Utils.utilizationVector();

				var closeUtilizationVolumeUtil = Utils.utilizationVector();
				var closeUtilizationNetOfDecayUtil = Utils.utilizationVector();
				var closeUtilizationNetOfDecayAndWasteUtil = Utils.utilizationVector();
				var closeUtilizationNetOfDecayWasteAndBreakageUtil = Utils.utilizationVector();

				var hlSp = vdypSpecies.getLoreyHeightByUtilization().getAll();
				{
					var baSp = vdypSpecies.getBaseAreaByUtilization().getLarge();
					var tphSp = vdypSpecies.getTreesPerHectareByUtilization().getLarge();
					var dqSp = vdypSpecies.getQuadraticMeanDiameterByUtilization().getLarge();

					treesPerHectareUtil.setAll(tphSp);
					quadMeanDiameterUtil.setAll(dqSp);
					baseAreaUtil.setAll(baSp);
					wholeStemVolumeUtil.setAll(0f);

					treesPerHectareUtil.setLarge(tphSp);
					quadMeanDiameterUtil.setLarge(dqSp);
					baseAreaUtil.setLarge(baSp);
					wholeStemVolumeUtil.setLarge(0f);
				}
				// AADJUSTV
				var volumeAdjustCoe = volumeAdjustMap.get(vdypSpecies.getGenus());

				var utilizationClass = UtilizationClass.OVER225; // IUC_VET

				// ADJ
				var adjust = new Coefficients(new float[] { 0f, 0f, 0f, 0f }, 1);

				// EMP091
				estimationMethods.estimateWholeStemVolume(
						utilizationClass, volumeAdjustCoe.getCoe(1), vdypSpecies.getVolumeGroup(), hlSp,
						quadMeanDiameterUtil, baseAreaUtil, wholeStemVolumeUtil
				);

				adjust.setCoe(4, volumeAdjustCoe.getCoe(2));
				// EMP092
				estimationMethods.estimateCloseUtilizationVolume(
						utilizationClass, adjust, vdypSpecies.getVolumeGroup(), hlSp, quadMeanDiameterUtil,
						wholeStemVolumeUtil, closeUtilizationVolumeUtil
				);

				adjust.setCoe(4, volumeAdjustCoe.getCoe(3));
				// EMP093
				estimationMethods.estimateNetDecayVolume(
						vdypSpecies.getGenus(), bec.getRegion(), utilizationClass, adjust, vdypSpecies.getDecayGroup(),
						vdypLayer.getComputedYearsAtBreastHeight().orElse(0f), quadMeanDiameterUtil,
						closeUtilizationVolumeUtil, closeUtilizationNetOfDecayUtil
				);

				adjust.setCoe(4, volumeAdjustCoe.getCoe(4));
				// EMP094
				estimationMethods.estimateNetDecayAndWasteVolume(
						bec.getRegion(), utilizationClass, adjust, vdypSpecies.getGenus(), hlSp, quadMeanDiameterUtil,
						closeUtilizationVolumeUtil, closeUtilizationNetOfDecayUtil,
						closeUtilizationNetOfDecayAndWasteUtil
				);

				if (getId().isStart()) {
					// EMP095
					estimationMethods.estimateNetDecayWasteAndBreakageVolume(
							utilizationClass, vdypSpecies.getBreakageGroup(), quadMeanDiameterUtil,
							closeUtilizationVolumeUtil, closeUtilizationNetOfDecayAndWasteUtil,
							closeUtilizationNetOfDecayWasteAndBreakageUtil
					);
				}

				vdypSpecies.setBaseAreaByUtilization(baseAreaUtil);
				vdypSpecies.setTreesPerHectareByUtilization(treesPerHectareUtil);
				vdypSpecies.setQuadraticMeanDiameterByUtilization(quadMeanDiameterUtil);
				vdypSpecies.setWholeStemVolumeByUtilization(wholeStemVolumeUtil);
				vdypSpecies.setCloseUtilizationVolumeByUtilization(closeUtilizationVolumeUtil);
				vdypSpecies.setCloseUtilizationVolumeNetOfDecayByUtilization(closeUtilizationNetOfDecayUtil);
				vdypSpecies.setCloseUtilizationVolumeNetOfDecayAndWasteByUtilization(
						closeUtilizationNetOfDecayAndWasteUtil
				);
				vdypSpecies.setCloseUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(
						closeUtilizationNetOfDecayWasteAndBreakageUtil
				);

				for (var accessors : UTILIZATION_VECTOR_ACCESSORS) {
					UtilizationVector utilVector = (UtilizationVector) accessors.getReadMethod().invoke(vdypSpecies);

					// Set all components other than 4 to 0.0
					for (var uc : UtilizationClass.ALL_BUT_LARGEST) {
						utilVector.set(uc, 0f);
					}

					// Set component 0 to equal component 4.
					utilVector.setAll(utilVector.getLarge());

					accessors.getWriteMethod().invoke(vdypSpecies, utilVector);
				}
			}

			computeLayerUtilizationComponentsFromSpecies(vdypLayer);

		} catch (IllegalAccessException | InvocationTargetException ex) {
			throw new IllegalStateException(ex);
		}
	}

	/**
	 * Main loop for processing
	 */
	protected void handleProcessing(CombinedPolygonStream<P> combinedStream) throws IOException, ResourceParseException,
			FatalProcessingException, ProcessingException, StandProcessingException {
		log.atDebug().setMessage("Start Stand processing").log();
		int polygonsRead = 0;
		int polygonsWritten = 0;

		while (combinedStream.hasNext()) {

			log.atInfo().setMessage("Getting polygon {}").addArgument(polygonsRead + 1).log();
			var polygon = combinedStream.next();
			try {

				var resultPoly = processPolygon(polygonsRead, polygon);
				if (resultPoly.isPresent()) {
					polygonsRead++;

					// Output
					this.getVriWriter().writePolygonWithSpeciesAndUtilization(resultPoly.get());

					polygonsWritten++;
				}

				log.atInfo().setMessage("Read {} polygons and wrote {}").addArgument(polygonsRead)
						.addArgument(polygonsWritten);

			} catch (StandProcessingException ex) {

				if (!combinedStream.hasNext()) {
					throw ex; // Propagate if this is the last one
				}

				// Otherwise log a warning and move on to the next one.

				log.atWarn().setMessage("Polygon {} bypassed").addArgument(polygon.getPolygonIdentifier()).setCause(ex);
			}

		}
	}

	/**
	 * Process a source polygon into a VdypPolygon.
	 *
	 * @param polygonsRead The number of polygons that have been read for processing.
	 * @param polygon      The source polygon to process.
	 * @return the processed polygon, or empty if it should be skipped without a warning.
	 * @throws StandProcessingException if the processing failed in a way that only affects this polygon
	 * @throws FatalProcessingException if the processing failed in a way that should stop processing
	 */
	protected abstract Optional<VdypPolygon> processPolygon(int polygonsRead, P polygon) throws ProcessingException;

	/**
	 * Simple Iterator like interface for accessing the assembled output of several StreamingParsers
	 *
	 * @param <P> The polygon type this stream produces
	 */
	protected static interface CombinedPolygonStream<P extends BaseVdypPolygon<?, ?, ?, ?>> {
		boolean hasNext() throws IOException, ResourceParseException;

		P next() throws ProcessingException, IOException, ResourceParseException;
	}

	@Override
	public void init(FileSystemFileResolver resolver, Map<String, Object> controlMap) throws IOException {
		super.init(resolver, controlMap);
		closeVriWriter();
		vriWriter = createWriter(resolver, controlMap);
	}

	@Override
	protected ResolvedControlMap resolveControlMap(Map<String, Object> rawControlMap) {
		return new StartResolvedControlMapImpl(rawControlMap);
	}
}
