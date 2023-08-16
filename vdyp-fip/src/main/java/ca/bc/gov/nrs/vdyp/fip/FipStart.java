package ca.bc.gov.nrs.vdyp.fip;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.lang.Math.min;

import java.beans.BeanDescriptor;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;

import static java.lang.Math.max;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.common.Utils;
import ca.bc.gov.nrs.vdyp.fip.model.FipLayer;
import ca.bc.gov.nrs.vdyp.fip.model.FipLayerPrimary;
import ca.bc.gov.nrs.vdyp.fip.model.FipMode;
import ca.bc.gov.nrs.vdyp.fip.model.FipPolygon;
import ca.bc.gov.nrs.vdyp.fip.model.FipSpecies;
import ca.bc.gov.nrs.vdyp.io.FileResolver;
import ca.bc.gov.nrs.vdyp.io.FileSystemFileResolver;
import ca.bc.gov.nrs.vdyp.io.parse.BecDefinitionParser;
import ca.bc.gov.nrs.vdyp.io.parse.BreakageEquationGroupParser;
import ca.bc.gov.nrs.vdyp.io.parse.BreakageParser;
import ca.bc.gov.nrs.vdyp.io.parse.CloseUtilVolumeParser;
import ca.bc.gov.nrs.vdyp.io.parse.DecayEquationGroupParser;
import ca.bc.gov.nrs.vdyp.io.parse.GenusDefinitionParser;
import ca.bc.gov.nrs.vdyp.io.parse.ResourceParseException;
import ca.bc.gov.nrs.vdyp.io.parse.StreamingParser;
import ca.bc.gov.nrs.vdyp.io.parse.StreamingParserFactory;
import ca.bc.gov.nrs.vdyp.io.parse.UtilComponentWSVolumeParser;
import ca.bc.gov.nrs.vdyp.io.parse.VeteranBQParser;
import ca.bc.gov.nrs.vdyp.io.parse.VeteranDQParser;
import ca.bc.gov.nrs.vdyp.io.parse.VeteranLayerVolumeAdjustParser;
import ca.bc.gov.nrs.vdyp.io.parse.VolumeEquationGroupParser;
import ca.bc.gov.nrs.vdyp.io.parse.VolumeNetDecayParser;
import ca.bc.gov.nrs.vdyp.io.parse.VolumeNetDecayWasteParser;
import ca.bc.gov.nrs.vdyp.model.BecDefinition;
import ca.bc.gov.nrs.vdyp.model.BecLookup.Substitution;
import ca.bc.gov.nrs.vdyp.model.Coefficients;
import ca.bc.gov.nrs.vdyp.model.Layer;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2;
import ca.bc.gov.nrs.vdyp.model.Region;
import ca.bc.gov.nrs.vdyp.model.VdypLayer;
import ca.bc.gov.nrs.vdyp.model.VdypSpecies;
import ca.bc.gov.nrs.vdyp.model.VdypUtilizationHolder;

public class FipStart {

	private static final Comparator<FipSpecies> PERCENT_GENUS_DESCENDING = compareUsing(FipSpecies::getPercentGenus)
			.reversed();

	static private final Logger log = LoggerFactory.getLogger(FipStart.class);

	public static final int CONFIG_LOAD_ERROR = 1; // TODO check what Fortran FIPStart would exit with.
	public static final int PROCESSING_ERROR = 2; // TODO check what Fortran FIPStart would exit with.

	int jprogram = 1; // FIPSTART only TODO Track this down

	public static final float PI_40K = 0.78539816E-04f;

	static final Collection<Integer> UTIL_CLASS_INDICES = IntStream.rangeClosed(1, 4).mapToObj(x -> x).toList();

	/**
	 * When finding primary species these genera should be combined
	 */
	static final Collection<Collection<String>> PRIMARY_SPECIES_TO_COMBINE = Arrays
			.asList(Arrays.asList("PL", "PA"), Arrays.asList("C", "Y"));

	private Map<String, Object> controlMap = Collections.emptyMap();

	/**
	 * Initialize FipStart
	 *
	 * @param resolver
	 * @param controlFilePath
	 * @throws IOException
	 * @throws ResourceParseException
	 */
	void init(FileResolver resolver, String controlFilePath) throws IOException, ResourceParseException {

		// Load the control map

		var parser = new FipControlParser();
		try (var is = resolver.resolve(controlFilePath)) {
			setControlMap(parser.parse(is, resolver));
		}

	}

	void setControlMap(Map<String, Object> controlMap) {
		this.controlMap = controlMap;
	}

	public static void main(final String... args) {

		var app = new FipStart();

		var resolver = new FileSystemFileResolver();

		var controlFileName = args[0];

		try {
			app.init(resolver, controlFileName);
		} catch (Exception ex) {
			log.error("Error during initialization", ex);
			System.exit(CONFIG_LOAD_ERROR);
		}

		try {
			app.process();
		} catch (Exception ex) {
			log.error("Error during processing", ex);
			System.exit(PROCESSING_ERROR);
		}
	}

	@SuppressWarnings("unchecked")
	private <T> StreamingParser<T> getStreamingParser(String key) throws ProcessingException {
		try {
			var factory = (StreamingParserFactory<T>) controlMap.get(key);
			if (factory == null) {
				throw new ProcessingException(String.format("Data file %s not specified in control map.", key));
			}
			return factory.get();
		} catch (IOException ex) {
			throw new ProcessingException("Error while opening data file.", ex);
		}
	}

	void process() throws ProcessingException {
		try (
				var polyStream = this.<FipPolygon>getStreamingParser(FipPolygonParser.CONTROL_KEY);
				var layerStream = this.<Map<Layer, FipLayer>>getStreamingParser(FipLayerParser.CONTROL_KEY);
				var speciesStream = this.<Collection<FipSpecies>>getStreamingParser(FipSpeciesParser.CONTROL_KEY);
		) {

			int polygonsRead = 0;
			int polygonsWritten = 0;

			while (polyStream.hasNext()) {

				// FIP_GET

				log.info("Getting polygon {}", polygonsRead + 1);
				var polygon = getPolygon(polyStream, layerStream, speciesStream);

				log.info("Read polygon {}, preparing to process", polygon.getPolygonIdentifier());

				// if (MODE .eq. -1) go to 100

				final var mode = polygon.getModeFip().orElse(FipMode.DONT_PROCESS);

				if (mode == FipMode.DONT_PROCESS) {
					log.info("Skipping polygon with mode {}", mode);
					continue;
				}

				// IP_IN = IP_IN+1
				// if (IP_IN .gt. MAXPOLY) go to 200

				polygonsRead++; // Don't count polygons we aren't processing due to mode. This was the behavior
				// in VDYP7

				// IPASS = 1
				// CALL FIP_CHK( IPASS, IER)
				// if (ier .gt. 0) go to 1000
				//
				// if (IPASS .le. 0) GO TO 120

				log.info("Checking validity of polygon {}:{}", polygonsRead, polygon.getPolygonIdentifier());
				checkPolygon(polygon);

				// CALL FIPCALCV( BAV, IER)
				// CALL FIPCALC1( BAV, BA_TOTL1, IER)

				Map<Layer, VdypLayer> processedLayers = new HashMap<>();

				for (FipLayer fipLayer : polygon.getLayers().values()) {
					switch (fipLayer.getLayer()) {
					case PRIMARY:
						assert fipLayer instanceof FipLayerPrimary;
						var primaryLayer = processLayerAsPrimary(polygon, (FipLayerPrimary) fipLayer);
						processedLayers.put(Layer.PRIMARY, primaryLayer);
						break;
					case VETERAN:
						var veteranLayer = processLayerAsVeteran(polygon, fipLayer);
						processedLayers.put(Layer.VETERAN, veteranLayer);
						break;
					default:
						throw new UnsupportedOperationException();
					}
				}

			}
		} catch (IOException | ResourceParseException ex) {
			throw new ProcessingException("Error while reading or writing data.", ex);
		}
	}

	private VdypLayer processLayerAsPrimary(FipPolygon fipPolygon, FipLayerPrimary fipLayerPrimary) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Creates a Comparator that compares two objects by applying the given accessor
	 * function to get comparable values that are then compared.
	 *
	 * @param <T>      type to be compared with the Comparator
	 * @param <V>      Comparable type
	 * @param accessor Function getting a V from a T
	 */
	static <T, V extends Comparable<V>> Comparator<T> compareUsing(Function<T, V> accessor) {
		return (x, y) -> accessor.apply(x).compareTo(accessor.apply(y));
	}

	VdypLayer processLayerAsVeteran(FipPolygon fipPolygon, FipLayer fipLayer) throws ProcessingException {

		var polygonIdentifier = fipLayer.getPolygonIdentifier();

		assert fipLayer.getLayer().equals(Layer.VETERAN) : "Layer must be VETERAN";
		assert fipPolygon.getPolygonIdentifier().equals(fipLayer.getPolygonIdentifier()) : String.format(
				"Polygon polygonIdentifier '%s' doesn't match that of layer '%s'", fipPolygon.getPolygonIdentifier(),
				fipLayer.getPolygonIdentifier()
		);

		var layer = Layer.VETERAN;

		// find Primary genus (highest percentage) ISPPVET

		var primaryGenus = fipLayer.getSpecies().values().stream() //
				.max(compareUsing(FipSpecies::getPercentGenus)) //
				.orElseThrow(() -> new IllegalStateException("No primarty genus (SP0) found. This should not happen."))
				.getGenus();

		// ageTotal copy, LVCOM3/AGETOTLV copied from FIPL_V/AGETOT_LV
		var ageTotal = fipLayer.getAgeTotal();

		// yearsToBreastHeight copy, minimum 6.0, LVCOM3/YTBHLV copied from
		// FIPL_V/YTBH_L
		var yearsToBreastHeight = Math.max(fipLayer.getYearsToBreastHeight(), 6.0f);

		// breastHeightAge LVCOM3/AGEBHLV ageTotal-yearsToBreastHeight
		var breastHeightAge = ageTotal - yearsToBreastHeight;

		// height? copy LVCOM3/HDLV = FIPL_V/HT_LV
		var height = fipLayer.getHeight();

		var crownClosure = fipLayer.getCrownClosure();

		var becId = fipPolygon.getBiogeoclimaticZone();
		var bec = BecDefinitionParser.getBecs(controlMap).get(becId, Substitution.SUBSTITUTE)
				.orElseThrow(() -> new ProcessingException("Could not find BEC " + becId));
		var region = bec.getRegion();

		// Call EMP098 to get Veteran Basal Area, store in LVCOM1/BA array at positions
		// 0,0 and 0,4
		var estimatedBaseArea = estimateVeteranBaseArea(height, crownClosure, primaryGenus, region);
		var baseAreaByUtilization = new Coefficients(
				new float[] { 0.0f, estimatedBaseArea, 0.0f, 0.0f, 0.0f, estimatedBaseArea }, -1
		);
		// Copy over Species entries.
		// LVCOM/ISPLV=ISPV
		// LVCOM4/SP0LV=FIPSA/SP0V
		// LVCOM4/SP64DISTLV=FIPSA/VDISTRV
		// LVCOM1/PCLTV=FIPS/PCTVOLV
		// LVCOM1/HL=FIPL_V/HT_LV
		var vdypSpecies = fipLayer.getSpecies().values().stream() //
				.map(fipSpec -> {
					var vs = new VdypSpecies(fipSpec);
					vs.setLoreyHeightByUtilization(new Coefficients(new float[] { 0f, height }, -1));
					return vs;
				}) //
				.collect(Collectors.toMap(VdypSpecies::getGenus, Function.identity()));

		// Lookup volume group, Decay Group, and Breakage group for each species.

		var volumeGroupMap = getGroupMap(VolumeEquationGroupParser.CONTROL_KEY);
		var decayGroupMap = getGroupMap(DecayEquationGroupParser.CONTROL_KEY);
		var breakageGroupMap = getGroupMap(BreakageEquationGroupParser.CONTROL_KEY);
		for (var vSpec : vdypSpecies.values()) {
			var volumeGroup = getGroup(fipPolygon, volumeGroupMap, vSpec);
			var decayGroup = getGroup(fipPolygon, decayGroupMap, vSpec);
			var breakageGroup = getGroup(fipPolygon, breakageGroupMap, vSpec);

			vSpec.setVolumeGroup(volumeGroup);
			vSpec.setDecayGroup(decayGroup);
			vSpec.setBreakageGroup(breakageGroup);
		}

		/*
		 * From VDYP7
		 *
		 * At this point we SHOULD invoke a root finding procedure sets species percents
		 * and adjusts DQ by species. fills in main components, through whole-stem
		 * volume INSTEAD, I will assume %volumes apply to % BA's
		 */

		for (var vSpec : vdypSpecies.values()) {
			vSpec.getBaseAreaByUtilization()
					.setCoe(4, baseAreaByUtilization.getCoe(4) * vSpec.getPercentGenus() / 100f);
		}

		var vetDqMap = Utils.<MatrixMap2<String, Region, Coefficients>>expectParsedControl(
				controlMap, VeteranDQParser.CONTROL_KEY, MatrixMap2.class
		);

		for (var vSpec : vdypSpecies.values()) {
			var genus = vSpec.getGenus();
			var coe = vetDqMap.get(genus, region);
			var a0 = coe.getCoe(1);
			var a1 = coe.getCoe(2);
			var a2 = coe.getCoe(3);
			float hl = vSpec.getLoreyHeightByUtilization().getCoe(0);
			float dq = max(a0 + a1 * pow(hl, a2), 22.5f);
			vSpec.getQuadraticMeanDiameterByUtilization().setCoe(4, dq);
			vSpec.getTreesPerHectareByUtilization()
					.setCoe(4, treesPerHectare(vSpec.getBaseAreaByUtilization().getCoe(4), dq));
		}

		var vdypLayer = new VdypLayer(polygonIdentifier, layer);
		vdypLayer.setAgeTotal(ageTotal);
		vdypLayer.setHeight(height);
		vdypLayer.setYearsToBreastHeight(yearsToBreastHeight);
		vdypLayer.setBreastHeightAge(breastHeightAge);
		vdypLayer.setSpecies(vdypSpecies);
		vdypLayer.setPrimaryGenus(primaryGenus);
		vdypLayer.setBaseAreaByUtilization(baseAreaByUtilization);

		computeUtilizationComponentsVeteran(vdypLayer, bec);

		return vdypLayer;
	}

	private Coefficients utilizationVector() {
		return new Coefficients(new float[] { 0f, 0f, 0f, 0f, 0f, 0f }, -1);
	}

	private void computeUtilizationComponentsVeteran(VdypLayer vdypLayer, BecDefinition bec)
			throws ProcessingException {

		var volumeAdjustMap = Utils.<Map<String, Coefficients>>expectParsedControl(
				controlMap, VeteranLayerVolumeAdjustParser.CONTROL_KEY, Map.class
		);
		try {
			for (var vdypSpecies : vdypLayer.getSpecies().values()) {

				var treesPerHectareUtil = utilizationVector();
				var quadMeanDiameterUtil = utilizationVector();
				var baseAreaUtil = utilizationVector();
				var wholeStemVolumeUtil = utilizationVector();

				var closeUtilizationVolumeUtil = utilizationVector();
				var closeUtilizationNetOfDecayUtil = utilizationVector();
				var closeUtilizationNetOfDecayAndWasteUtil = utilizationVector();
				var closeUtilizationNetOfDecayWasteAndBreakageUtil = utilizationVector();

				var hlSp = vdypSpecies.getLoreyHeightByUtilization().getCoe(0);
				{
					var baSp = vdypSpecies.getBaseAreaByUtilization().getCoe(4);
					var tphSp = vdypSpecies.getTreesPerHectareByUtilization().getCoe(4);
					var dqSp = vdypSpecies.getQuadraticMeanDiameterByUtilization().getCoe(4);

					treesPerHectareUtil.setCoe(0, tphSp);
					quadMeanDiameterUtil.setCoe(0, dqSp);
					baseAreaUtil.setCoe(0, baSp);
					wholeStemVolumeUtil.setCoe(0, 0f);

					treesPerHectareUtil.setCoe(4, tphSp);
					quadMeanDiameterUtil.setCoe(4, dqSp);
					baseAreaUtil.setCoe(4, baSp);
					wholeStemVolumeUtil.setCoe(4, 0f);
				}
				// AADJUSTV
				var volumeAdjustCoe = volumeAdjustMap.get(vdypSpecies.getGenus());

				var utilizationClass = 4; // IUC_VET

				// ADJ
				var adjust = new Coefficients(new float[] { 0f, 0f, 0f, 0f }, 1);

				// EMP091
				estimateWholeStemVolume(
						utilizationClass, volumeAdjustCoe.getCoe(1), vdypSpecies.getVolumeGroup(), hlSp,
						quadMeanDiameterUtil, baseAreaUtil, wholeStemVolumeUtil
				);

				adjust.setCoe(4, volumeAdjustCoe.getCoe(2));
				// EMP092
				estimateCloseUtilizationVolume(
						utilizationClass, adjust, vdypSpecies.getVolumeGroup(), hlSp, quadMeanDiameterUtil,
						wholeStemVolumeUtil, closeUtilizationVolumeUtil
				);

				adjust.setCoe(4, volumeAdjustCoe.getCoe(3));
				// EMP093
				estimateNetDecayVolume(
						vdypSpecies.getGenus(), bec.getRegion(), utilizationClass, adjust, vdypSpecies.getDecayGroup(),
						hlSp, vdypLayer.getBreastHeightAge(), quadMeanDiameterUtil, closeUtilizationVolumeUtil,
						closeUtilizationNetOfDecayUtil
				);

				adjust.setCoe(4, volumeAdjustCoe.getCoe(4));
				// EMP094
				estimateNetDecayAndWasteVolume(
						bec.getRegion(), utilizationClass, adjust, vdypSpecies.getGenus(), hlSp,
						vdypLayer.getBreastHeightAge(), quadMeanDiameterUtil, closeUtilizationVolumeUtil,
						closeUtilizationNetOfDecayUtil, closeUtilizationNetOfDecayAndWasteUtil
				);

				if (jprogram < 6) {
					// EMP095
					estimateNetDecayWasteAndBreakageVolume(
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
				vdypSpecies.setCloseUtilizationNetVolumeOfDecayByUtilization(closeUtilizationNetOfDecayUtil);
				vdypSpecies.setCloseUtilizationVolumeNetOfDecayAndWasteByUtilization(
						closeUtilizationNetOfDecayAndWasteUtil
				);
				vdypSpecies.setCloseUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(
						closeUtilizationNetOfDecayWasteAndBreakageUtil
				);

				for (var accessors : UTILIZATION_VECTOR_ACCESSORS) {
					Coefficients utilVector = (Coefficients) accessors.getReadMethod().invoke(vdypSpecies);

					// Set all components other than 4 to 0.0
					for (var i = -1; i < 4; i++) {
						utilVector.setCoe(i, 0f);
					}

					// Set component 0 to equal component 4.
					utilVector.setCoe(0, utilVector.getCoe(4));

					accessors.getWriteMethod().invoke(vdypSpecies, utilVector);
				}
			}

			// Layer utilization vectors other than quadratic mean diameter are the pairwise
			// sums of those of their species
			for (var accessors : SUMMABLE_UTILIZATION_VECTOR_ACCESSORS) {
				var utilVector = utilizationVector();
				for (var vdypSpecies : vdypLayer.getSpecies().values()) {
					var speciesVector = (Coefficients) accessors.getReadMethod().invoke(vdypSpecies);
					utilVector.pairwiseInPlace(speciesVector, (x, y) -> x + y);
				}
				accessors.getWriteMethod().invoke(vdypLayer, utilVector);
			}

			// Quadratic mean diameter for the layer is computed from the BA and TPH after
			// they have been found from the species
			{
				var utilVector = vdypLayer.getBaseAreaByUtilization()
						.pairwise(vdypLayer.getTreesPerHectareByUtilization(), FipStart::quadMeanDiameter);
				vdypLayer.setQuadraticMeanDiameterByUtilization(utilVector);
			}

		} catch (IllegalAccessException | InvocationTargetException ex) {
			throw new IllegalStateException(ex);
		}
	}

	/**
	 * Returns the primary, and secondary if present species records as a one or two
	 * element list.
	 */
	List<FipSpecies> findPrimarySpecies(Map<String, FipSpecies> allSpecies) {
		if (allSpecies.isEmpty()) {
			throw new IllegalArgumentException("Can not find primary species as there are no species");
		}
		var result = new ArrayList<FipSpecies>(2);

		// Start with a deep copy of the species map so there are no side effects from
		// the manipulation this method does.
		var combined = new HashMap<String, FipSpecies>(allSpecies.size());
		allSpecies.entrySet().stream().forEach(spec -> {
			combined.put(spec.getKey(), new FipSpecies(spec.getValue()));
		});

		for (var combinationGroup : PRIMARY_SPECIES_TO_COMBINE) {
			var groupSpecies = combinationGroup.stream().map(combined::get).filter(Objects::nonNull).toList();
			if (groupSpecies.size() < 2) {
				continue;
			}
			var groupPrimary = new FipSpecies(groupSpecies.stream().sorted(PERCENT_GENUS_DESCENDING).findFirst().get());
			var total = (float) groupSpecies.stream().mapToDouble(FipSpecies::getPercentGenus).sum();
			combinationGroup.forEach(combined::remove);
			groupPrimary.setPercentGenus(total);
			combined.put(groupPrimary.getGenus(), groupPrimary);
		}

		if (combined.size() == 1) {
			result.addAll(combined.values());
		} else {
			var NDEBUG_22 = false;
			if (NDEBUG_22) {
				// TODO
				throw new UnsupportedOperationException();
			} else {
				combined.values().stream().sorted(PERCENT_GENUS_DESCENDING).limit(2).forEach(result::add);
			}
		}

		assert !result.isEmpty();
		assert result.size() <= 2;
		return result;
	}

	/**
	 * Accessor methods for utilization vectors, except for Lorey Height, on Layer
	 * and Species objects.
	 */
	static final Collection<PropertyDescriptor> UTILIZATION_VECTOR_ACCESSORS;

	/**
	 * Accessor methods for utilization vectors, except for Lorey Height and
	 * Quadratic Mean Diameter, on Layer and Species objects. These are properties
	 * where the values for the layer are the sum of those for its species.
	 */
	static final Collection<PropertyDescriptor> SUMMABLE_UTILIZATION_VECTOR_ACCESSORS;

	static {
		try {
			var bean = Introspector.getBeanInfo(VdypUtilizationHolder.class);
			UTILIZATION_VECTOR_ACCESSORS = Arrays.stream(bean.getPropertyDescriptors()) //
					.filter(p -> p.getName().endsWith("ByUtilization")) //
					.filter(p -> !p.getName().startsWith("loreyHeight")) //
					.filter(p -> p.getPropertyType() == Coefficients.class) //
					.toList();
		} catch (IntrospectionException e) {
			throw new IllegalStateException(e);
		}

		SUMMABLE_UTILIZATION_VECTOR_ACCESSORS = UTILIZATION_VECTOR_ACCESSORS.stream()
				.filter(x -> !x.getName().startsWith("quadraticMeanDiameter")).toList();
	}

	// EMP091
	/**
	 * Updates wholeStemVolumeUtil with estimated values.
	 */
	void estimateWholeStemVolume(
			int utilizationClass, float aAdjust, int volumeGroup, Float hlSp, Coefficients quadMeanDiameterUtil,
			Coefficients baseAreaUtil, Coefficients wholeStemVolumeUtil
	) throws ProcessingException {
		var dqSp = quadMeanDiameterUtil.getCoe(0);
		final var wholeStemUtilizationComponentMap = Utils
				.<MatrixMap2<Integer, Integer, Optional<Coefficients>>>expectParsedControl(
						controlMap, UtilComponentWSVolumeParser.CONTROL_KEY, MatrixMap2.class
				);
		estimateUtilization(baseAreaUtil, wholeStemVolumeUtil, utilizationClass, (i, ba) -> {
			Coefficients wholeStemCoe = wholeStemUtilizationComponentMap.get(i, volumeGroup).orElseThrow(
					() -> new ProcessingException(
							"Could not find whole stem utilization coefficients for group " + volumeGroup
					)
			);

			// Fortran code uses 1 index into array when reading it here, but 0 index when
			// writing into it in the parser. I use 0 for both.
			var a0 = wholeStemCoe.getCoe(0);
			var a1 = wholeStemCoe.getCoe(1);
			var a2 = wholeStemCoe.getCoe(2);
			var a3 = wholeStemCoe.getCoe(3);

			var arg = a0 + a1 * log(hlSp) + a2 * log(quadMeanDiameterUtil.getCoe(i))
					+ ( (i < 3) ? a3 * log(dqSp) : a3 * dqSp);

			if (i == utilizationClass) {
				arg += aAdjust;
			}

			var vbaruc = exp(arg); // volume base area ?? utilization class?

			return ba * vbaruc;
		}, x -> x < 0f, 0f);

		if (utilizationClass == 0) {
			normalizeUtilizationComponents(wholeStemVolumeUtil);
		}

	}

	// EMP092
	/**
	 * Updates closeUtilizationVolumeUtil with estimated values.
	 *
	 * @throws ProcessingException
	 */
	void estimateCloseUtilizationVolume(
			int utilizationClass, Coefficients aAdjust, int volumeGroup, float hlSp, Coefficients quadMeanDiameterUtil,
			Coefficients wholeStemVolumeUtil, Coefficients closeUtilizationVolumeUtil
	) throws ProcessingException {
		var dqSp = quadMeanDiameterUtil.getCoe(0);
		final var closeUtilizationCoeMap = Utils
				.<MatrixMap2<Integer, Integer, Optional<Coefficients>>>expectParsedControl(
						controlMap, CloseUtilVolumeParser.CONTROL_KEY, MatrixMap2.class
				);
		estimateUtilization(wholeStemVolumeUtil, closeUtilizationVolumeUtil, utilizationClass, (i, ws) -> {
			Coefficients closeUtilCoe = closeUtilizationCoeMap.get(i, volumeGroup).orElseThrow(
					() -> new ProcessingException(
							"Could not find whole stem utilization coefficients for group " + volumeGroup
					)
			);
			var a0 = closeUtilCoe.getCoe(1);
			var a1 = closeUtilCoe.getCoe(2);
			var a2 = closeUtilCoe.getCoe(3);

			var arg = a0 + a1 * quadMeanDiameterUtil.getCoe(i) + a2 * hlSp + aAdjust.getCoe(i);

			float ratio = ratio(arg, 7.0f);

			return ws * ratio;
		});

		if (utilizationClass == 0) {
			storeSumUtilizationComponents(closeUtilizationVolumeUtil);
		}
	}

	@FunctionalInterface
	static interface UtilizationProcessor {
		float apply(int utilizationClass, float inputValue) throws ProcessingException;
	}

	/**
	 * Estimate values for one utilization vector from another
	 *
	 * @param input            source utilization
	 * @param output           result utilization
	 * @param utilizationClass the utilization class for which to do the
	 *                         computation, 0 for all of them.
	 * @param processor        Given a utilization class, and the source utilization
	 *                         for that class, return the result utilization
	 * @throws ProcessingException
	 */
	static void estimateUtilization(
			Coefficients input, Coefficients output, int utilizationClass, UtilizationProcessor processor
	) throws ProcessingException {
		estimateUtilization(input, output, utilizationClass, processor, x -> false, 0f);
	}

	/**
	 * Estimate values for one utilization vector from another
	 *
	 * @param input            source utilization
	 * @param output           result utilization
	 * @param utilizationClass the utilization class for which to do the
	 *                         computation, 0 for all of them.
	 * @param processor        Given a utilization class, and the source utilization
	 *                         for that class, return the result utilization
	 * @param skip             a utilization class will be skipped and the result
	 *                         set to the default value if this is true for the
	 *                         value of the source utilization
	 * @param defaultValue     the default value
	 * @throws ProcessingException
	 */
	static void estimateUtilization(
			Coefficients input, Coefficients output, int utilizationClass, UtilizationProcessor processor,
			Predicate<Float> skip, float defaultValue
	) throws ProcessingException {
		for (var i : UTIL_CLASS_INDICES) {
			var inputValue = input.getCoe(i);

			// it seems like this should be done after checking i against utilizationClass,
			// which could just be done as part of the processor definition, but this is how
			// VDYP7 did it.
			if (skip.test(inputValue)) {
				output.setCoe(i, defaultValue);
				continue;
			}

			if (utilizationClass != 0 && utilizationClass != i) {
				continue;
			}

			var result = processor.apply(i, input.getCoe(i));
			output.setCoe(i, result);
		}
	}

	/**
	 * Estimate volume NET OF DECAY by (DBH) utilization classes
	 *
	 * @param utilizationClass
	 * @param aAdjust
	 * @param decayGroup
	 * @param lorieHeight
	 * @param ageBreastHeight
	 * @param quadMeanDiameterUtil
	 * @param closeUtilizationUtil
	 * @param closeUtilizationNetOfDecayUtil
	 * @throws ProcessingException
	 */
	void estimateNetDecayVolume(
			String genus, Region region, int utilizationClass, Coefficients aAdjust, int decayGroup, float lorieHeight,
			float ageBreastHeight, Coefficients quadMeanDiameterUtil, Coefficients closeUtilizationUtil,
			Coefficients closeUtilizationNetOfDecayUtil
	) throws ProcessingException {
		var dqSp = quadMeanDiameterUtil.getCoe(0);
		final var netDecayCoeMap = Utils.<MatrixMap2<Integer, Integer, Optional<Coefficients>>>expectParsedControl(
				controlMap, VolumeNetDecayParser.CONTROL_KEY, MatrixMap2.class
		);
		final var decayModifierMap = Utils.<MatrixMap2<String, Region, Float>>expectParsedControl(
				controlMap, ModifierParser.CONTROL_KEY_MOD301_DECAY, MatrixMap2.class
		);

		final var ageTr = (float) Math.log(Math.max(20.0, ageBreastHeight));

		estimateUtilization(closeUtilizationUtil, closeUtilizationNetOfDecayUtil, utilizationClass, (i, cu) -> {
			Coefficients netDecayCoe = netDecayCoeMap.get(i, decayGroup).orElseThrow(
					() -> new ProcessingException("Could not find net decay coefficients for group " + decayGroup)
			);
			var a0 = netDecayCoe.getCoe(1);
			var a1 = netDecayCoe.getCoe(2);
			var a2 = netDecayCoe.getCoe(3);

			float arg;
			if (i <= 3) {
				arg = a0 + a1 * log(dqSp) + a2 * ageTr;
			} else {
				arg = a0 + a1 * log(quadMeanDiameterUtil.getCoe(i)) + a2 * ageTr;
			}

			arg += aAdjust.getCoe(i) + decayModifierMap.get(genus, region);

			float ratio = ratio(arg, 8.0f);

			return cu * ratio;
		});

		if (utilizationClass == 0) {
			storeSumUtilizationComponents(closeUtilizationNetOfDecayUtil);
		}
	}

	/**
	 * Estimate utilization net of decay and waste
	 */
	void estimateNetDecayAndWasteVolume(
			Region region, int utilizationClass, Coefficients aAdjust, String genus, float lorieHeight,
			float ageBreastHeight, Coefficients quadMeanDiameterUtil, Coefficients closeUtilizationUtil,
			Coefficients closeUtilizationNetOfDecayUtil, Coefficients closeUtilizationNetOfDecayAndWasteUtil
	) throws ProcessingException {
		final var netDecayCoeMap = Utils.<Map<String, Coefficients>>expectParsedControl(
				controlMap, VolumeNetDecayWasteParser.CONTROL_KEY, Map.class
		);
		final var wasteModifierMap = Utils.<MatrixMap2<String, Region, Float>>expectParsedControl(
				controlMap, ModifierParser.CONTROL_KEY_MOD301_WASTE, MatrixMap2.class
		);

		final var ageTr = (float) Math.log(Math.max(20.0, ageBreastHeight));

		estimateUtilization(
				closeUtilizationNetOfDecayUtil, closeUtilizationNetOfDecayAndWasteUtil, utilizationClass,
				(i, netDecay) -> {
					if (Float.isNaN(netDecay) || netDecay <= 0f) {
						return 0f;
					}

					Coefficients netWasteCoe = netDecayCoeMap.get(genus);
					if (netWasteCoe == null) {
						throw new ProcessingException("Could not find net waste coefficients for genus " + genus);
					}
					;
					var a0 = netWasteCoe.getCoe(0);
					var a1 = netWasteCoe.getCoe(1);
					var a2 = netWasteCoe.getCoe(2);
					var a3 = netWasteCoe.getCoe(3);
					var a4 = netWasteCoe.getCoe(4);
					var a5 = netWasteCoe.getCoe(5);

					if (i == 4) {
						a0 += a5;
					}
					var frd = 1.0f - netDecay / closeUtilizationUtil.getCoe(i);

					float arg = a0 + a1 * frd + a3 * log(quadMeanDiameterUtil.getCoe(i)) + a4 * log(lorieHeight);

					arg += wasteModifierMap.get(genus, region);

					arg = clamp(arg, -10f, 10f);

					var frw = (1.0f - exp(a2 * frd)) * exp(arg) / (1f + exp(arg)) * (1f - frd);
					frw = min(frd, frw);

					float result = closeUtilizationUtil.getCoe(i) * (1f - frd - frw);

					/*
					 * Check for an apply adjustments. This is done after computing the result above
					 * to allow for clamping frw to frd
					 */
					if (aAdjust.getCoe(i) != 0f) {
						var ratio = result / netDecay;
						if (ratio < 1f && ratio > 0f) {
							arg = log(ratio / (1f - ratio));
							arg += aAdjust.getCoe(i);
							arg = clamp(arg, -10f, 10f);
							result = exp(arg) / (1f + exp(arg)) * netDecay;
						}
					}

					return result;
				}
		);

		if (utilizationClass == 0) {
			storeSumUtilizationComponents(closeUtilizationNetOfDecayAndWasteUtil);
		}
	}

	/**
	 * Estimate utilization net of decay, waste, and breakage
	 *
	 * @throws ProcessingException
	 */
	void estimateNetDecayWasteAndBreakageVolume(
			int utilizationClass, int breakageGroup, Coefficients quadMeanDiameterUtil,
			Coefficients closeUtilizationUtil, Coefficients closeUtilizationNetOfDecayAndWasteUtil,
			Coefficients closeUtilizationNetOfDecayWasteAndBreakageUtil
	) throws ProcessingException {
		final var netBreakageCoeMap = Utils
				.<Map<Integer, Coefficients>>expectParsedControl(controlMap, BreakageParser.CONTROL_KEY, Map.class);
		final var coefficients = netBreakageCoeMap.get(breakageGroup);
		if (coefficients == null) {
			throw new ProcessingException("Could not find net breakage coefficients for group " + breakageGroup);
		}

		final var a1 = coefficients.getCoe(1);
		final var a2 = coefficients.getCoe(2);
		final var a3 = coefficients.getCoe(3);
		final var a4 = coefficients.getCoe(4);

		estimateUtilization(
				closeUtilizationNetOfDecayAndWasteUtil, closeUtilizationNetOfDecayWasteAndBreakageUtil,
				utilizationClass, (i, netWaste) -> {

					if (netWaste <= 0f) {
						return 0f;
					}
					var percentBroken = a1 + a2 * log(quadMeanDiameterUtil.getCoe(i));
					percentBroken = clamp(percentBroken, a3, a4);
					var broken = min(percentBroken / 100 * closeUtilizationUtil.getCoe(i), netWaste);
					return netWaste - broken;
				}
		);

		if (utilizationClass == 0) {
			storeSumUtilizationComponents(closeUtilizationNetOfDecayAndWasteUtil);
		}

	}

	/**
	 * Sums the individual utilization components (1-4)
	 */
	float sumUtilizationComponents(Coefficients components) {
		return (float) UTIL_CLASS_INDICES.stream().mapToDouble(components::getCoe).sum();
	}

	/**
	 * Sums the individual utilization components (1-4) and stores the results in
	 * coefficient 0
	 */
	float storeSumUtilizationComponents(Coefficients components) {
		var sum = sumUtilizationComponents(components);
		components.setCoe(0, sum);
		return sum;
	}

	/**
	 * Normalizes the utilization components 1-4 so they sum to the value of
	 * component 0
	 *
	 * @throws ProcessingException if the sum is not positive
	 */
	float normalizeUtilizationComponents(Coefficients components) throws ProcessingException {
		var sum = sumUtilizationComponents(components);
		var k = components.getCoe(0) / sum;
		if (sum <= 0f) {
			throw new ProcessingException("Total volume " + sum + " was not positive.");
		}
		UTIL_CLASS_INDICES.forEach(i -> {
			components.setCoe(i, components.getCoe(i) * k);
		});
		return k;
	}

	int getGroup(FipPolygon fipPolygon, MatrixMap2<String, String, Integer> volumeGroupMap, VdypSpecies vSpec) {
		return volumeGroupMap.get(vSpec.getGenus(), fipPolygon.getBiogeoclimaticZone());
	}

	MatrixMap2<String, String, Integer> getGroupMap(String key) {
		return Utils.expectParsedControl(controlMap, key, MatrixMap2.class);
	}

	private FipPolygon getPolygon(
			StreamingParser<FipPolygon> polyStream, StreamingParser<Map<Layer, FipLayer>> layerStream,
			StreamingParser<Collection<FipSpecies>> speciesStream
	) throws ProcessingException, IOException, ResourceParseException {

		log.trace("Getting polygon");
		var polygon = polyStream.next();

		log.trace("Getting layers for polygon {}", polygon.getPolygonIdentifier());
		Map<Layer, FipLayer> layers;
		try {
			layers = layerStream.next();
		} catch (NoSuchElementException ex) {
			throw validationError("Layers file has fewer records than polygon file.", ex);
		}

		log.trace("Getting species for polygon {}", polygon.getPolygonIdentifier());
		Collection<FipSpecies> species;
		try {
			species = speciesStream.next();
		} catch (NoSuchElementException ex) {
			throw validationError("Species file has fewer records than polygon file.", ex);
		}

		// Validate that layers belong to the correct polygon
		for (FipLayer layer : layers.values()) {
			if (!layer.getPolygonIdentifier().equals(polygon.getPolygonIdentifier())) {
				throw validationError(
						"Record in layer file contains layer for polygon %s when expecting one for %s.",
						layer.getPolygonIdentifier(), polygon.getPolygonIdentifier()
				);
			}
			layer.setSpecies(new HashMap<>());
		}

		for (var spec : species) {
			var layer = layers.get(spec.getLayer());
			// Validate that species belong to the correct polygon
			if (!spec.getPolygonIdentifier().equals(polygon.getPolygonIdentifier())) {
				throw validationError(
						"Record in species file contains species for polygon %s when expecting one for %s.",
						layer.getPolygonIdentifier(), polygon.getPolygonIdentifier()
				);
			}
			if (Objects.isNull(layer)) {
				throw validationError(
						"Species entry references layer %s of polygon %s but it is not present.", layer,
						polygon.getPolygonIdentifier()
				);
			}
			layer.getSpecies().put(spec.getGenus(), spec);
		}

		polygon.setLayers(layers);

		return polygon;
	}

	private Optional<Float> heightMinimum(Layer layer) {
		var minima = Utils.<Map<String, Float>>expectParsedControl(controlMap, FipControlParser.MINIMA, Map.class);
		switch (layer) {
		case PRIMARY:
			return Optional.of(minima.get(FipControlParser.MINIMUM_HEIGHT));
		case VETERAN:
			return Optional.of(minima.get(FipControlParser.MINIMUM_VETERAN_HEIGHT));
		default:
			return Optional.empty();
		}
	}

	private void checkPolygon(FipPolygon polygon) throws ProcessingException {

		if (!polygon.getLayers().containsKey(Layer.PRIMARY)) {
			throw validationError(
					"Polygon %s has no %s layer, or that layer has non-positive height or crown closure.",
					polygon.getPolygonIdentifier(), Layer.PRIMARY
			);
		}

		var primaryLayer = (FipLayerPrimary) polygon.getLayers().get(Layer.PRIMARY);

		// FIXME VDYP7 actually tests if total age - YTBH is less than 0.5 but gives an
		// error that total age is "less than" YTBH. Replicating that for now but
		// consider changing it.

		if (primaryLayer.getAgeTotal() - primaryLayer.getYearsToBreastHeight() < 0.5f) {
			throw validationError(
					"Polygon %s has %s layer where total age is less than YTBH.", polygon.getPolygonIdentifier(),
					Layer.PRIMARY
			);
		}

		// TODO This is the only validation step done to non-primary layers, VDYP7 had a
		// less well defined idea of a layer being present or not and so it may have
		// skipped validating other layers rather than validating them conditionally on
		// being present. Consider extending validation of other properties to other
		// layers.

		for (FipLayer layer : polygon.getLayers().values()) {
			var height = layer.getHeight();

			throwIfPresent(
					heightMinimum(layer.getLayer()).filter(minimum -> height < minimum).map(
							minimum -> validationError(
									"Polygon %s has %s layer where height %.1f is less than minimum %.1f.",
									polygon.getPolygonIdentifier(), layer.getLayer(), layer.getHeight(), minimum
							)
					)
			);
		}

		if (polygon.getModeFip().map(x -> x == FipMode.FIPYOUNG).orElse(false)) {
			throw validationError(
					"Polygon %s is using unsupported mode %s.", polygon.getPolygonIdentifier(), FipMode.FIPYOUNG
			);
		}

		if (primaryLayer.getYearsToBreastHeight() < 0.5) {
			throw validationError(
					"Polygon %s has %s layer where years to breast height %.1f is less than minimum %.1f years.",
					polygon.getPolygonIdentifier(), Layer.PRIMARY, primaryLayer.getYearsToBreastHeight(), 0.5f
			);
		}

		if (primaryLayer.getSiteIndex() < 0.5) {
			throw validationError(
					"Polygon %s has %s layer where site index %.1f is less than minimum %.1f years.",
					polygon.getPolygonIdentifier(), Layer.PRIMARY, primaryLayer.getSiteIndex(), 0.5f
			);
		}

		for (FipLayer layer : polygon.getLayers().values()) {
			var percentTotal = layer.getSpecies().values().stream()//
					.map(FipSpecies::getPercentGenus)//
					.reduce(0.0f, (x, y) -> x + y);
			if (Math.abs(percentTotal - 100f) > 0.01f) {
				throw validationError(
						"Polygon %s has %s layer where species entries have a percentage total that does not sum to 100%%.",
						polygon.getPolygonIdentifier(), Layer.PRIMARY
				);
			}
			// VDYP7 performs this step which should be negligible but might have a small
			// impact due to the 0.01 percent variation and floating point errors.
			if (layer.getLayer() == Layer.PRIMARY) {
				layer.getSpecies().values()
						.forEach(species -> species.setFractionGenus(species.getPercentGenus() / percentTotal));
			}
		}

	}

	float estimateVeteranBaseArea(float height, float crownClosure, String genus, Region region)
			throws ProcessingException {
		@SuppressWarnings("unchecked")
		var coefficients = ((MatrixMap2<String, Region, Coefficients>) controlMap.get(VeteranBQParser.CONTROL_KEY))
				.getM(genus, region);

		// mismatched index is copied from VDYP7
		float a0 = coefficients.getCoe(1);
		float a1 = coefficients.getCoe(2);
		float a2 = coefficients.getCoe(3);

		float baseArea = a0 * pow(max(height - a1, 0.0f), a2);

		baseArea *= crownClosure / 4.0f;

		baseArea = max(baseArea, 0.01f);

		return baseArea;
	}

	static float treesPerHectare(float baseArea, float quadraticMeanDiameter) {
		if (baseArea != 0) {
			return baseArea / PI_40K / (quadraticMeanDiameter * quadraticMeanDiameter);
		}
		return 0f;
	}

	static float quadMeanDiameter(float baseArea, float treesPerHectare) {
		if (baseArea > 1e6f || treesPerHectare > 1e6f || Float.isNaN(baseArea) || Float.isNaN(treesPerHectare)) {
			return 0f;
		} else if (baseArea > 0f && treesPerHectare > 0f) {
			return sqrt(baseArea / treesPerHectare / PI_40K);
		}
		return 0f;

	}

	private static <E extends Throwable> void throwIfPresent(Optional<E> opt) throws E {
		if (opt.isPresent()) {
			throw opt.get();
		}
	}

	private static ProcessingException validationError(String template, Object... values) {
		return new ProcessingException(String.format(template, values));
	}

	// wrap standard library double math functions to work with floats so equations
	// aren't littered with explicit casts

	static float log(float f) {
		return (float) Math.log(f);
	}

	static float exp(float f) {
		return (float) Math.exp(f);
	}

	static float pow(float b, float e) {
		return (float) Math.pow(b, e);
	}

	static float abs(float f) {
		return Math.abs(f);
	}

	static float sqrt(float f) {
		return (float) Math.sqrt(f);
	}

	static float clamp(float x, float min, float max) {
		assert max >= min;
		if (x < min)
			return min;
		if (x > max)
			return max;
		return x;
	}

	static float ratio(float arg, float radius) {
		if (arg < -radius) {
			return 0.0f;
		} else if (arg > radius) {
			return 1.0f;
		}
		return exp(arg) / (1.0f + exp(arg));
	}

}
