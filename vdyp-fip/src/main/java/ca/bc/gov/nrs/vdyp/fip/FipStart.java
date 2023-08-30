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
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.DoubleFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToDoubleBiFunction;
import java.util.function.ToDoubleFunction;
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
import ca.bc.gov.nrs.vdyp.io.parse.CoefficientParser;
import ca.bc.gov.nrs.vdyp.io.parse.DecayEquationGroupParser;
import ca.bc.gov.nrs.vdyp.io.parse.DefaultEquationNumberParser;
import ca.bc.gov.nrs.vdyp.io.parse.EquationModifierParser;
import ca.bc.gov.nrs.vdyp.io.parse.GenusDefinitionParser;
import ca.bc.gov.nrs.vdyp.io.parse.HLCoefficientParser;
import ca.bc.gov.nrs.vdyp.io.parse.HLNonprimaryCoefficientParser;
import ca.bc.gov.nrs.vdyp.io.parse.ResourceParseException;
import ca.bc.gov.nrs.vdyp.io.parse.StreamingParser;
import ca.bc.gov.nrs.vdyp.io.parse.StreamingParserFactory;
import ca.bc.gov.nrs.vdyp.io.parse.TotalStandWholeStemParser;
import ca.bc.gov.nrs.vdyp.io.parse.UpperCoefficientParser;
import ca.bc.gov.nrs.vdyp.io.parse.UtilComponentWSVolumeParser;
import ca.bc.gov.nrs.vdyp.io.parse.VeteranBQParser;
import ca.bc.gov.nrs.vdyp.io.parse.VeteranDQParser;
import ca.bc.gov.nrs.vdyp.io.parse.VeteranLayerVolumeAdjustParser;
import ca.bc.gov.nrs.vdyp.io.parse.VolumeEquationGroupParser;
import ca.bc.gov.nrs.vdyp.io.parse.VolumeNetDecayParser;
import ca.bc.gov.nrs.vdyp.io.parse.VolumeNetDecayWasteParser;
import ca.bc.gov.nrs.vdyp.model.BecDefinition;
import ca.bc.gov.nrs.vdyp.model.Coefficients;
import ca.bc.gov.nrs.vdyp.model.Layer;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2;
import ca.bc.gov.nrs.vdyp.model.MatrixMap3;
import ca.bc.gov.nrs.vdyp.model.NonprimaryHLCoefficients;
import ca.bc.gov.nrs.vdyp.model.Region;
import ca.bc.gov.nrs.vdyp.model.VdypLayer;
import ca.bc.gov.nrs.vdyp.model.VdypSpecies;
import ca.bc.gov.nrs.vdyp.model.VdypUtilizationHolder;

public class FipStart {

	private static final Comparator<FipSpecies> PERCENT_GENUS_DESCENDING = Utils.compareUsing(FipSpecies::getPercentGenus)
			.reversed();

	static private final Logger log = LoggerFactory.getLogger(FipStart.class);

	public static final int CONFIG_LOAD_ERROR = 1; // TODO check what Fortran FIPStart would exit with.
	public static final int PROCESSING_ERROR = 2; // TODO check what Fortran FIPStart would exit with.

	public final static int UTIL_ALL = 0;
	public final static int UTIL_LARGEST = 4;

	int jprogram = 1; // FIPSTART only TODO Track this down

	public static final float PI_40K = 0.78539816E-04f;

	static final Collection<Integer> UTIL_CLASS_INDICES = IntStream.rangeClosed(1, UTIL_LARGEST).mapToObj(x -> x)
			.toList();

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

	static final Set<String> HARDWOODS = Set.of("AC", "AT", "D", "E", "MB");

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
				try {

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

					var fipLayers = polygon.getLayers();
					var fipVetLayer = Optional.ofNullable(fipLayers.get(Layer.VETERAN));
					Optional<VdypLayer> resultVetLayer;
					if (fipVetLayer.isPresent()) {
						resultVetLayer = Optional.of(processLayerAsVeteran(polygon, fipVetLayer.get()));
					} else {
						resultVetLayer = Optional.empty();
					}
					resultVetLayer.ifPresent(layer -> processedLayers.put(Layer.VETERAN, layer));

					FipLayerPrimary fipPrimeLayer = (FipLayerPrimary) fipLayers.get(Layer.PRIMARY);
					assert fipPrimeLayer != null;
					var resultPrimeLayer = processLayerAsPrimary(
							polygon, fipPrimeLayer,
							resultVetLayer.map(VdypLayer::getBaseAreaByUtilization).map(coe -> coe.get(UTIL_ALL)).orElse(0f)
					);
					processedLayers.put(Layer.PRIMARY, resultPrimeLayer);

				} catch (LowValueException ex) {
					// TODO include other exceptions that cause a polygon to be bypassed
					// TODO include some sort of hook for different forms of user output

					// TODO fip_sub:241-250
					log.warn(String.format("Polygon %s bypassed", polygon.getPolygonIdentifier()), ex);
				}
			}
		} catch (IOException | ResourceParseException ex) {
			throw new ProcessingException("Error while reading or writing data.", ex);
		}
	}

	VdypLayer processLayerAsPrimary(FipPolygon fipPolygon, FipLayerPrimary fipLayer, float baseAreaOverstory)
			throws ProcessingException {

		var lookup = BecDefinitionParser.getBecs(controlMap);
		var primarySpecies = findPrimarySpecies(fipLayer.getSpecies());
		// VDYP7 stores this in the common FIPL_1C/ITGL1 but only seems to use it
		// locally
		var itg = findItg(primarySpecies);

		BecDefinition bec = lookup.get(fipPolygon.getBiogeoclimaticZone())
				.orElseThrow(() -> new IllegalStateException("Could not find BEC " + fipPolygon.getBiogeoclimaticZone()));

		var baseAreaGroup = findBaseAreaGroup(primarySpecies.get(0), bec, itg);

		var result = new VdypLayer(fipLayer.getPolygonIdentifier(), fipLayer.getLayer());

		result.setAgeTotal(fipLayer.getAgeTotal());
		result.setYearsToBreastHeight(fipLayer.getYearsToBreastHeight());
		result.setBreastHeightAge(fipLayer.getAgeTotal() - fipLayer.getYearsToBreastHeight());
		result.setHeight(fipLayer.getHeight());

		var baseArea = estimatePrimaryBaseArea(
				fipLayer, bec, fipPolygon.getYieldFactor(), result.getBreastHeightAge(), baseAreaOverstory
		);

		result.getBaseAreaByUtilization().set(UTIL_ALL, baseArea);

		var quadMeanDiameter = estimatePrimaryQuadMeanDiameter(
				fipLayer, bec, result.getBreastHeightAge(), baseAreaOverstory
		);

		result.getQuadraticMeanDiameterByUtilization().set(UTIL_ALL, quadMeanDiameter);

		var tphTotal = treesPerHectare(baseArea, quadMeanDiameter);

		result.getTreesPerHectareByUtilization().set(UTIL_ALL, tphTotal);

		// Copy over Species entries.
		// LVCOM/ISPL1=ISPV
		// LVCOM4/SP0L1=FIPSA/SP0V
		// LVCOM4/SP64DISTL1=FIPSA/VDISTRV
		// LVCOM1/PCLT1=FIPS/PCTVOLV
		var vdypSpecies = fipLayer.getSpecies().values().stream() //
				.map(fipSpec -> new VdypSpecies(fipSpec)) //
				.collect(Collectors.toMap(VdypSpecies::getGenus, Function.identity()));

		var vdypPrimarySpecies = vdypSpecies.get(primarySpecies.get(0).getGenus());

		// Lookup volume group, Decay Group, and Breakage group for each species.

		var volumeGroupMap = getGroupMap(VolumeEquationGroupParser.CONTROL_KEY);
		var decayGroupMap = getGroupMap(DecayEquationGroupParser.CONTROL_KEY);
		var breakageGroupMap = getGroupMap(BreakageEquationGroupParser.CONTROL_KEY);

		Map<String, Float> targetPercentages = new HashMap<>(vdypSpecies.size());

		for (var vSpec : vdypSpecies.values()) {
			var volumeGroup = getGroup(fipPolygon, volumeGroupMap, vSpec);
			var decayGroup = getGroup(fipPolygon, decayGroupMap, vSpec);
			var breakageGroup = getGroup(fipPolygon, breakageGroupMap, vSpec);

			vSpec.setVolumeGroup(volumeGroup);
			vSpec.setDecayGroup(decayGroup);
			vSpec.setBreakageGroup(breakageGroup);

			targetPercentages.put(vSpec.getGenus(), vSpec.getPercentGenus());
		}

		var maxPass = fipLayer.getSpecies().size() > 1 ? 2 : 1;

		float primaryHeight = 0f;
		float leadHeight = fipLayer.getHeight();
		for (var iPass = 1; iPass <= maxPass; iPass++) {
			if (iPass == 2) {
				for (var vSpec : vdypSpecies.values()) {
					vSpec.setPercentGenus(targetPercentages.get(vSpec.getGenus()));
				}
			}

			if (iPass == 1 && vdypSpecies.size() == 1) {
				primaryHeight = primaryHeightFromLeadHeight(
						leadHeight, vdypPrimarySpecies.getGenus(), bec.getRegion(), tphTotal
				);
			} else if (iPass == 1) {
				primaryHeight = primaryHeightFromLeadHeightInitial(leadHeight, vdypPrimarySpecies.getGenus(), bec.getRegion());
			} else {
				primaryHeight = primaryHeightFromLeadHeight(
						leadHeight, vdypPrimarySpecies.getGenus(), bec.getRegion(),
						vdypPrimarySpecies.getTreesPerHectareByUtilization().getCoe(UTIL_ALL)
				);
			}

			// Estimate lorey height for non-primary species
			for (var vspec : vdypSpecies.values()) {
				if (vspec == vdypPrimarySpecies)
					continue;

				// EMP053
				vspec.getLoreyHeightByUtilization()
						.setCoe(UTIL_ALL, estimateNonPrimaryLoreyHeight(vspec, vdypPrimarySpecies, bec, leadHeight, primaryHeight));
			}

			// TODO
			// ROOTF01

			findRootsForDiameterAndBaseArea(result, iPass+1);
		}

		return null; // TODO
	}

	void findRootsForDiameterAndBaseArea(VdypLayer result, int source) throws ProcessingException {

		var quadMeanDiameterTotal = result.getQuadraticMeanDiameterByUtilization().getCoe(UTIL_ALL);
		var baseAreaTotal = result.getBaseAreaByUtilization().getCoe(UTIL_ALL);
		var treesPerHectareTotal = result.getTreesPerHectareByUtilization().getCoe(UTIL_ALL);
		Map<String, Float> goal = new HashMap<>();

		float treesPerHectareSum;

		if (result.getSpecies().size() == 1) {
			var spec = result.getSpecies().values().iterator().next();
			for (var accessors : NON_VOLUME_UTILIZATION_VECTOR_ACCESSORS) {

				try {
					Coefficients specVector = (Coefficients) accessors.getReadMethod().invoke(spec);
					Coefficients layerVector = (Coefficients) accessors.getReadMethod().invoke(result);
					specVector.setCoe(UTIL_ALL, layerVector.getCoe(UTIL_ALL));
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			result.getLoreyHeightByUtilization().setCoe(UTIL_ALL, spec.getLoreyHeightByUtilization().getCoe(UTIL_ALL));
			spec.setPercentGenus(100f);
			treesPerHectareSum = treesPerHectareTotal;
		} else {
			// TODO
			treesPerHectareSum = 0; // To make it compile, set correctly when this branch is implemented.
		}
		var volumeSum = 0f;

		for (var spec : result.getSpecies().values()) {
			// EMP090
			var wholeStemVolume = spec.getTreesPerHectareByUtilization().getCoe(UTIL_ALL) * estimateWholeStemVolumePerTree(
					spec.getVolumeGroup(), spec.getLoreyHeightByUtilization().getCoe(UTIL_ALL),
					spec.getQuadraticMeanDiameterByUtilization().getCoe(UTIL_ALL)
			);
			spec.getWholeStemVolumeByUtilization().setCoe(UTIL_ALL, wholeStemVolume);
			volumeSum += wholeStemVolume;
		}

		result.getWholeStemVolumeByUtilization().setCoe(UTIL_ALL, volumeSum);
		var treesPerHectareStart = result.getTreesPerHectareByUtilization().getCoe(UTIL_ALL);
		result.getTreesPerHectareByUtilization().setCoe(UTIL_ALL, treesPerHectareSum);
		result.getQuadraticMeanDiameterByUtilization().setCoe(
				UTIL_ALL,
				quadMeanDiameter(
						result.getBaseAreaByUtilization().getCoe(UTIL_ALL),
						result.getTreesPerHectareByUtilization().getCoe(UTIL_ALL)
				)
		);

		if (abs(treesPerHectareStart / result.getTreesPerHectareByUtilization().getCoe(UTIL_ALL) - 1f) > 0.002) {
			throw new ProcessingException("TODO");
		}

		if (result.getSpecies().size() > 1) {
			for (var spec : result.getSpecies().values()) {
				if (spec.getWholeStemVolumeByUtilization().getCoe(UTIL_ALL) / volumeSum - goal.get(spec.getGenus()) > 0.1) {
					throw new ProcessingException("TODO");
				}
			}
		}
	}

	private float estimateWholeStemVolumePerTree(int volumeGroup, float loreyHeight, float quadMeanDiameter) {
		var coeMap = Utils.<Map<Integer, Coefficients>>expectParsedControl(
				controlMap, TotalStandWholeStemParser.CONTROL_KEY, Map.class
		);
		var coe = coeMap.get(volumeGroup).reindex(0);

		var logMeanVolume = coe.getCoe(UTIL_ALL) + //
				coe.getCoe(1) * log(quadMeanDiameter) + //
				coe.getCoe(2) * log(loreyHeight) + //
				coe.getCoe(3) * quadMeanDiameter + //
				coe.getCoe(4) / quadMeanDiameter + //
				coe.getCoe(5) * loreyHeight + //
				coe.getCoe(6) * quadMeanDiameter * quadMeanDiameter + //
				coe.getCoe(7) * loreyHeight * quadMeanDiameter //
				+ coe.getCoe(8) * loreyHeight / quadMeanDiameter;

		return exp(logMeanVolume);
	}

	// EMP053 Using eqns N1 and N2 from ipsjf124.doc
	/**
	 * Estimate the lorey height of a non-primary species of a primary layer.
	 * 
	 * @param vspec         The species.
	 * @param vspecPrime    The primary species.
	 * @param leadHeight    lead height of the layer
	 * @param primaryHeight height of the primary species
	 * @throws ProcessingException
	 */
	float estimateNonPrimaryLoreyHeight(
			VdypSpecies vspec, VdypSpecies vspecPrime, BecDefinition bec, float leadHeight, float primaryHeight
	) throws ProcessingException {
		var coeMap = Utils.<MatrixMap3<String, String, Region, Optional<NonprimaryHLCoefficients>>>expectParsedControl(
				controlMap, HLNonprimaryCoefficientParser.CONTROL_KEY, MatrixMap3.class
		);

		var coe = coeMap.get(vspec.getGenus(), vspecPrime.getGenus(), bec.getRegion()).orElseThrow(
				() -> new ProcessingException(
						String.format(
								"Could not find Lorey Height Nonprimary Coefficients for %s %s %s", vspec.getGenus(),
								vspecPrime.getGenus(), bec.getRegion()
						)
				)
		);
		var heightToUse = coe.getEquationIndex() == 1 ? leadHeight : primaryHeight;
		return 1.3f + coe.getCoe(1) * pow(heightToUse - 1.3f, coe.getCoe(2));
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
				.max(Utils.compareUsing(FipSpecies::getPercentGenus)) //
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
		var bec = BecDefinitionParser.getBecs(controlMap).get(becId)
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
					.setCoe(UTIL_LARGEST, baseAreaByUtilization.getCoe(UTIL_LARGEST) * vSpec.getPercentGenus() / 100f);
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
			vSpec.getQuadraticMeanDiameterByUtilization().setCoe(UTIL_LARGEST, dq);
			vSpec.getTreesPerHectareByUtilization()
					.setCoe(UTIL_LARGEST, treesPerHectare(vSpec.getBaseAreaByUtilization().getCoe(UTIL_LARGEST), dq));
		}

		var vdypLayer = new VdypLayer(polygonIdentifier, layer);
		vdypLayer.setAgeTotal(ageTotal);
		vdypLayer.setHeight(height);
		vdypLayer.setYearsToBreastHeight(yearsToBreastHeight);
		vdypLayer.setBreastHeightAge(breastHeightAge);
		vdypLayer.setSpecies(vdypSpecies);
		// vdypLayer.setPrimaryGenus(primaryGenus);
		vdypLayer.setBaseAreaByUtilization(baseAreaByUtilization);

		computeUtilizationComponentsVeteran(vdypLayer, bec);

		return vdypLayer;
	}

	private Coefficients utilizationVector() {
		return new Coefficients(new float[] { 0f, 0f, 0f, 0f, 0f, 0f }, -1);
	}

	private void computeUtilizationComponentsVeteran(VdypLayer vdypLayer, BecDefinition bec) throws ProcessingException {

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

				var hlSp = vdypSpecies.getLoreyHeightByUtilization().getCoe(UTIL_ALL);
				{
					var baSp = vdypSpecies.getBaseAreaByUtilization().getCoe(UTIL_LARGEST);
					var tphSp = vdypSpecies.getTreesPerHectareByUtilization().getCoe(UTIL_LARGEST);
					var dqSp = vdypSpecies.getQuadraticMeanDiameterByUtilization().getCoe(UTIL_LARGEST);

					treesPerHectareUtil.setCoe(UTIL_ALL, tphSp);
					quadMeanDiameterUtil.setCoe(UTIL_ALL, dqSp);
					baseAreaUtil.setCoe(UTIL_ALL, baSp);
					wholeStemVolumeUtil.setCoe(UTIL_ALL, 0f);

					treesPerHectareUtil.setCoe(UTIL_LARGEST, tphSp);
					quadMeanDiameterUtil.setCoe(UTIL_LARGEST, dqSp);
					baseAreaUtil.setCoe(UTIL_LARGEST, baSp);
					wholeStemVolumeUtil.setCoe(UTIL_LARGEST, 0f);
				}
				// AADJUSTV
				var volumeAdjustCoe = volumeAdjustMap.get(vdypSpecies.getGenus());

				var utilizationClass = UTIL_LARGEST; // IUC_VET

				// ADJ
				var adjust = new Coefficients(new float[] { 0f, 0f, 0f, 0f }, 1);

				// EMP091
				estimateWholeStemVolume(
						utilizationClass, volumeAdjustCoe.getCoe(1), vdypSpecies.getVolumeGroup(), hlSp, quadMeanDiameterUtil,
						baseAreaUtil, wholeStemVolumeUtil
				);

				adjust.setCoe(4, volumeAdjustCoe.getCoe(2));
				// EMP092
				estimateCloseUtilizationVolume(
						utilizationClass, adjust, vdypSpecies.getVolumeGroup(), hlSp, quadMeanDiameterUtil, wholeStemVolumeUtil,
						closeUtilizationVolumeUtil
				);

				adjust.setCoe(4, volumeAdjustCoe.getCoe(3));
				// EMP093
				estimateNetDecayVolume(
						vdypSpecies.getGenus(), bec.getRegion(), utilizationClass, adjust, vdypSpecies.getDecayGroup(), hlSp,
						vdypLayer.getBreastHeightAge(), quadMeanDiameterUtil, closeUtilizationVolumeUtil,
						closeUtilizationNetOfDecayUtil
				);

				adjust.setCoe(4, volumeAdjustCoe.getCoe(4));
				// EMP094
				estimateNetDecayAndWasteVolume(
						bec.getRegion(), utilizationClass, adjust, vdypSpecies.getGenus(), hlSp, vdypLayer.getBreastHeightAge(),
						quadMeanDiameterUtil, closeUtilizationVolumeUtil, closeUtilizationNetOfDecayUtil,
						closeUtilizationNetOfDecayAndWasteUtil
				);

				if (jprogram < 6) {
					// EMP095
					estimateNetDecayWasteAndBreakageVolume(
							utilizationClass, vdypSpecies.getBreakageGroup(), quadMeanDiameterUtil, closeUtilizationVolumeUtil,
							closeUtilizationNetOfDecayAndWasteUtil, closeUtilizationNetOfDecayWasteAndBreakageUtil
					);
				}

				vdypSpecies.setBaseAreaByUtilization(baseAreaUtil);
				vdypSpecies.setTreesPerHectareByUtilization(treesPerHectareUtil);
				vdypSpecies.setQuadraticMeanDiameterByUtilization(quadMeanDiameterUtil);
				vdypSpecies.setWholeStemVolumeByUtilization(wholeStemVolumeUtil);
				vdypSpecies.setCloseUtilizationVolumeByUtilization(closeUtilizationVolumeUtil);
				vdypSpecies.setCloseUtilizationNetVolumeOfDecayByUtilization(closeUtilizationNetOfDecayUtil);
				vdypSpecies.setCloseUtilizationVolumeNetOfDecayAndWasteByUtilization(closeUtilizationNetOfDecayAndWasteUtil);
				vdypSpecies.setCloseUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(
						closeUtilizationNetOfDecayWasteAndBreakageUtil
				);

				for (var accessors : UTILIZATION_VECTOR_ACCESSORS) {
					Coefficients utilVector = (Coefficients) accessors.getReadMethod().invoke(vdypSpecies);

					// Set all components other than 4 to 0.0
					for (var i = -1; i < UTIL_LARGEST; i++) {
						utilVector.setCoe(i, 0f);
					}

					// Set component 0 to equal component 4.
					utilVector.setCoe(UTIL_ALL, utilVector.getCoe(UTIL_LARGEST));

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
	// PRIMFIND
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

		assert !combined.isEmpty();

		if (combined.size() == 1) {
			// There's only one
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
	 * Find Inventory type group (ITG)
	 *
	 * @param primarySecondary
	 * @return
	 * @throws ProcessingException
	 */
	// ITGFIND
	int findItg(List<FipSpecies> primarySecondary) throws ProcessingException {
		var primary = primarySecondary.get(0);

		if (primary.getPercentGenus() > 79.999) { // Copied from VDYP7
			return ITG_PURE.get(primary.getGenus());
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
			throw new ProcessingException("Unexpected primary species: " + primary.getGenus());
		}
	}

	// GRPBA1FD
	int findBaseAreaGroup(FipSpecies fipSpecies, BecDefinition bec, int itg) {
		var growthBec = bec.getGrowthBec();
		final var defaultGroupsMap = Utils.<MatrixMap2<String, String, Integer>>expectParsedControl(
				controlMap, DefaultEquationNumberParser.CONTROL_KEY, MatrixMap2.class
		);
		final var modifierMap = Utils.<MatrixMap2<Integer, Integer, Optional<Integer>>>expectParsedControl(
				controlMap, EquationModifierParser.CONTROL_KEY, MatrixMap2.class
		);
		var defaultGroup = defaultGroupsMap.get(fipSpecies.getGenus(), growthBec.getAlias());
		return modifierMap.getM(defaultGroup, itg).orElse(defaultGroup);
	}

	private float heightMultiplier(String genus, Region region, float treesPerHectarePrimary) {
		final var coeMap = Utils.<MatrixMap2<String, Region, Optional<Coefficients>>>expectParsedControl(
				controlMap, HLCoefficientParser.CONTROL_KEY_P1, MatrixMap2.class
		);
		var coe = coeMap.get(genus, region).orElse(Coefficients.empty(HLCoefficientParser.NUM_COEFFICIENTS_P1, 1))
				.reindex(0);
		return coe.get(0) - coe.getCoe(1) + coe.getCoe(1) * exp(coe.getCoe(2) * (treesPerHectarePrimary - 100f));
	}

	// EMP050 Meth==1
	/**
	 * Return the lorey height of the primary species based on the dominant height
	 * of the lead species.
	 * 
	 * @param leadHeight             dominant height of the lead species
	 * @param genus                  Primary species
	 * @param region                 Region of the polygon
	 * @param treesPerHectarePrimary trees per hectare >7.5 cm of the primary
	 *                               species
	 * @return
	 */
	float primaryHeightFromLeadHeight(float leadHeight, String genus, Region region, float treesPerHectarePrimary) {
		return 1.3f + (leadHeight - 1.3f) * heightMultiplier(genus, region, treesPerHectarePrimary);
	}

	// EMP050 Meth==2
	/**
	 * Return the dominant height of the lead species based on the lorey height of
	 * the primary species.
	 * 
	 * @param primaryHeight          lorey height of the primary species
	 * @param genus                  Primary species
	 * @param region                 Region of the polygon
	 * @param treesPerHectarePrimary trees per hectare >7.5 cm of the primary
	 *                               species
	 * @return
	 */
	float leadHeightFromPrimaryHeight(float primaryHeight, String genus, Region region, float treesPerHectarePrimary) {
		return 1.3f + (primaryHeight - 1.3f) / heightMultiplier(genus, region, treesPerHectarePrimary);
	}

	// EMP051
	/**
	 * Return the lorey height of the primary species based on the dominant height
	 * of the lead species.
	 * 
	 * @param leadHeight dominant height of the lead species
	 * @param genus      Primary species
	 * @param region     Region of the polygon
	 * @return
	 */
	private float primaryHeightFromLeadHeightInitial(float leadHeight, String genus, Region region) {
		final var coeMap = Utils.<MatrixMap2<String, Region, Optional<Coefficients>>>expectParsedControl(
				controlMap, HLCoefficientParser.CONTROL_KEY_P2, MatrixMap2.class
		);
		var coe = coeMap.get(genus, region).orElse(Coefficients.empty(HLCoefficientParser.NUM_COEFFICIENTS_P2, 1));
		return 1.3f + coe.getCoe(0) * pow(leadHeight - 1.3f, coe.getCoe(1));
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

	/**
	 * Accessor methods for utilization vectors, except for Lorey Height,and Volume
	 * on Layer and Species objects.
	 */
	static final Collection<PropertyDescriptor> NON_VOLUME_UTILIZATION_VECTOR_ACCESSORS;

	private static final float LOW_CROWN_CLOSURE = 10f;

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

		NON_VOLUME_UTILIZATION_VECTOR_ACCESSORS = UTILIZATION_VECTOR_ACCESSORS.stream()
				.filter(x -> !x.getName().contains("Volume")).toList();
	}

	// EMP091
	/**
	 * Updates wholeStemVolumeUtil with estimated values.
	 */
	void estimateWholeStemVolume(
			int utilizationClass, float aAdjust, int volumeGroup, Float hlSp, Coefficients quadMeanDiameterUtil,
			Coefficients baseAreaUtil, Coefficients wholeStemVolumeUtil
	) throws ProcessingException {
		var dqSp = quadMeanDiameterUtil.getCoe(UTIL_ALL);
		final var wholeStemUtilizationComponentMap = Utils
				.<MatrixMap2<Integer, Integer, Optional<Coefficients>>>expectParsedControl(
						controlMap, UtilComponentWSVolumeParser.CONTROL_KEY, MatrixMap2.class
				);
		estimateUtilization(baseAreaUtil, wholeStemVolumeUtil, utilizationClass, (i, ba) -> {
			Coefficients wholeStemCoe = wholeStemUtilizationComponentMap.get(i, volumeGroup).orElseThrow(
					() -> new ProcessingException("Could not find whole stem utilization coefficients for group " + volumeGroup)
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

		if (utilizationClass == UTIL_ALL) {
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
		var dqSp = quadMeanDiameterUtil.getCoe(UTIL_ALL);
		final var closeUtilizationCoeMap = Utils.<MatrixMap2<Integer, Integer, Optional<Coefficients>>>expectParsedControl(
				controlMap, CloseUtilVolumeParser.CONTROL_KEY, MatrixMap2.class
		);
		estimateUtilization(wholeStemVolumeUtil, closeUtilizationVolumeUtil, utilizationClass, (i, ws) -> {
			Coefficients closeUtilCoe = closeUtilizationCoeMap.get(i, volumeGroup).orElseThrow(
					() -> new ProcessingException("Could not find whole stem utilization coefficients for group " + volumeGroup)
			);
			var a0 = closeUtilCoe.getCoe(1);
			var a1 = closeUtilCoe.getCoe(2);
			var a2 = closeUtilCoe.getCoe(3);

			var arg = a0 + a1 * quadMeanDiameterUtil.getCoe(i) + a2 * hlSp + aAdjust.getCoe(i);

			float ratio = ratio(arg, 7.0f);

			return ws * ratio;
		});

		if (utilizationClass == UTIL_ALL) {
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
	 *                         computation, UTIL_ALL for all of them.
	 * @param processor        Given a utilization class, and the source utilization
	 *                         for that class, return the result utilization
	 * @throws ProcessingException
	 */
	static void
			estimateUtilization(Coefficients input, Coefficients output, int utilizationClass, UtilizationProcessor processor)
					throws ProcessingException {
		estimateUtilization(input, output, utilizationClass, processor, x -> false, 0f);
	}

	/**
	 * Estimate values for one utilization vector from another
	 *
	 * @param input            source utilization
	 * @param output           result utilization
	 * @param utilizationClass the utilization class for which to do the
	 *                         computation, UTIL_ALL for all of them.
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

			if (utilizationClass != UTIL_ALL && utilizationClass != i) {
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
		var dqSp = quadMeanDiameterUtil.getCoe(UTIL_ALL);
		final var netDecayCoeMap = Utils.<MatrixMap2<Integer, Integer, Optional<Coefficients>>>expectParsedControl(
				controlMap, VolumeNetDecayParser.CONTROL_KEY, MatrixMap2.class
		);
		final var decayModifierMap = Utils.<MatrixMap2<String, Region, Float>>expectParsedControl(
				controlMap, ModifierParser.CONTROL_KEY_MOD301_DECAY, MatrixMap2.class
		);

		final var ageTr = (float) Math.log(Math.max(20.0, ageBreastHeight));

		estimateUtilization(closeUtilizationUtil, closeUtilizationNetOfDecayUtil, utilizationClass, (i, cu) -> {
			Coefficients netDecayCoe = netDecayCoeMap.get(i, decayGroup)
					.orElseThrow(() -> new ProcessingException("Could not find net decay coefficients for group " + decayGroup));
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
			Region region, int utilizationClass, Coefficients aAdjust, String genus, float lorieHeight, float ageBreastHeight,
			Coefficients quadMeanDiameterUtil, Coefficients closeUtilizationUtil, Coefficients closeUtilizationNetOfDecayUtil,
			Coefficients closeUtilizationNetOfDecayAndWasteUtil
	) throws ProcessingException {
		final var netDecayCoeMap = Utils
				.<Map<String, Coefficients>>expectParsedControl(controlMap, VolumeNetDecayWasteParser.CONTROL_KEY, Map.class);
		final var wasteModifierMap = Utils.<MatrixMap2<String, Region, Float>>expectParsedControl(
				controlMap, ModifierParser.CONTROL_KEY_MOD301_WASTE, MatrixMap2.class
		);

		final var ageTr = (float) Math.log(Math.max(20.0, ageBreastHeight));

		estimateUtilization(
				closeUtilizationNetOfDecayUtil, closeUtilizationNetOfDecayAndWasteUtil, utilizationClass, (i, netDecay) -> {
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

		if (utilizationClass == UTIL_ALL) {
			storeSumUtilizationComponents(closeUtilizationNetOfDecayAndWasteUtil);
		}
	}

	/**
	 * Estimate utilization net of decay, waste, and breakage
	 *
	 * @throws ProcessingException
	 */
	void estimateNetDecayWasteAndBreakageVolume(
			int utilizationClass, int breakageGroup, Coefficients quadMeanDiameterUtil, Coefficients closeUtilizationUtil,
			Coefficients closeUtilizationNetOfDecayAndWasteUtil, Coefficients closeUtilizationNetOfDecayWasteAndBreakageUtil
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
				closeUtilizationNetOfDecayAndWasteUtil, closeUtilizationNetOfDecayWasteAndBreakageUtil, utilizationClass,
				(i, netWaste) -> {

					if (netWaste <= 0f) {
						return 0f;
					}
					var percentBroken = a1 + a2 * log(quadMeanDiameterUtil.getCoe(i));
					percentBroken = clamp(percentBroken, a3, a4);
					var broken = min(percentBroken / 100 * closeUtilizationUtil.getCoe(i), netWaste);
					return netWaste - broken;
				}
		);

		if (utilizationClass == UTIL_ALL) {
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
	 * coefficient UTIL_ALL
	 */
	float storeSumUtilizationComponents(Coefficients components) {
		var sum = sumUtilizationComponents(components);
		components.setCoe(UTIL_ALL, sum);
		return sum;
	}

	/**
	 * Normalizes the utilization components 1-4 so they sum to the value of
	 * component UTIL_ALL
	 *
	 * @throws ProcessingException if the sum is not positive
	 */
	float normalizeUtilizationComponents(Coefficients components) throws ProcessingException {
		var sum = sumUtilizationComponents(components);
		var k = components.getCoe(UTIL_ALL) / sum;
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
					"Polygon %s has %s layer where total age is less than YTBH.", polygon.getPolygonIdentifier(), Layer.PRIMARY
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

	// EMP040
	float estimatePrimaryBaseArea(
			FipLayer fipLayer, BecDefinition bec, float yieldFactor, float breastHeightAge, float baseAreaOverstory
	) throws LowValueException {

		boolean lowCrownClosure = fipLayer.getCrownClosure() < LOW_CROWN_CLOSURE;
		float crownClosure = lowCrownClosure ? LOW_CROWN_CLOSURE : fipLayer.getCrownClosure();

		var coeMap = Utils.<MatrixMap2<String, String, Coefficients>>expectParsedControl(
				controlMap, CoefficientParser.BA_CONTROL_KEY, MatrixMap2.class
		);
		var modMap = Utils.<MatrixMap2<String, Region, Float>>expectParsedControl(
				controlMap, ModifierParser.CONTROL_KEY_MOD200_BA, MatrixMap2.class
		);
		var upperBoundMap = Utils.<MatrixMap3<Region, String, Integer, Float>>expectParsedControl(
				controlMap, UpperCoefficientParser.CONTROL_KEY, MatrixMap3.class
		);

		var leadGenus = leadGenus(fipLayer);

		var decayBecAlias = bec.getDecayBec().getAlias();
		Coefficients coe = weightedCoefficientSum(
				List.of(0, 1, 2, 3, 4, 5), 9, 0, fipLayer.getSpecies().values(), FipSpecies::getFractionGenus,
				s -> coeMap.get(decayBecAlias, s.getGenus())
		);

		var ageToUse = clamp(breastHeightAge, 5f, 350f);
		var trAge = log(ageToUse);

		/* @formatter:off */
		//      A00 = exp(A(0)) * ( 1.0 +  A(1) * TR_AGE  )
		/* @formatter:on */
		var a00 = exp(coe.getCoe(0)) * (1f + coe.getCoe(1) * trAge);

		/* @formatter:off */
		//      AP  = exp( A(3)) + exp(A(4)) * TR_AGE
		/* @formatter:on */
		var ap = exp(coe.getCoe(3)) + exp(coe.getCoe(4)) * trAge;

		var baseArea = 0f;

		float height = fipLayer.getHeight();
		if (height > coe.getCoe(2) - 3f) {
			/* @formatter:off */
			//  if (HD .le. A(2) - 3.0) then
			//      BAP = 0.0
			//      GO TO 90
			//  else if (HD .lt. A(2)+3.0) then
			//      FHD = (HD- (A(2)-3.00) )**2 / 12.0
			//  else
			//      FHD = HD-A(2)
			//  end if
			/* @formatter:on */
			var fHeight = height <= coe.getCoe(2) + 3f ? //
					pow(height - (coe.getCoe(2) - 3), 2) / 12f //
					: height - coe.getCoe(2);

			/* @formatter:off */
			//      BAP =  A00 * (CCUSE/100) ** ( A(7) + A(8)*log(HD) )   *
			//     &      FHD**AP * exp( A(5)*HD  + A(6) * BAV )
			/* @formatter:on */
			baseArea = a00 * pow(crownClosure / 100, coe.getCoe(7) + coe.getCoe(8) * log(height)) * pow(fHeight, ap)
					* exp(coe.getCoe(5) * height + coe.getCoe(6) * baseAreaOverstory);

			baseArea *= modMap.get(leadGenus.getGenus(), bec.getRegion());

			// TODO
			var NDEBUG_1 = 0;
			if (NDEBUG_1 <= 0) {
				// See ISPSJF128
				var upperBound = upperBoundMap.get(bec.getRegion(), leadGenus.getGenus(), UpperCoefficientParser.BA);
				baseArea = min(baseArea, upperBound);
			}

			if (lowCrownClosure) {
				baseArea *= fipLayer.getCrownClosure() / LOW_CROWN_CLOSURE;
			}

		}

		baseArea *= yieldFactor;

		// This is to prevent underflow errors in later calculations
		if (baseArea <= 0.05f) {
			throw new LowValueException("Estimated base area", baseArea, 0.05f);
		}
		return baseArea;
	}

	/**
	 * Create a coefficients object where its values are either a weighted sum of
	 * those for each of the given entities, or the value from one arbitrarily chose
	 * entity.
	 *
	 * @param <T>             The type of entity
	 * @param weighted        the indicies of the coefficients that should be
	 *                        weighted sums, those that are not included are assumed
	 *                        to be constant across all entities and one is choses
	 *                        arbitrarily.
	 * @param size            Size of the resulting coefficients object
	 * @param indexFrom       index from of the resulting coefficients object
	 * @param entities        the entities to do weighted sums over
	 * @param weight          the weight for a given entity
	 * @param getCoefficients the coefficients for a given entity
	 */
	<T> Coefficients weightedCoefficientSum(
			Collection<Integer> weighted, int size, int indexFrom, Collection<T> entities, ToDoubleFunction<T> weight,
			Function<T, Coefficients> getCoefficients
	) {
		Coefficients coe = Coefficients.empty(size, indexFrom);

		// Do the summation in double precision
		var coeWorking = new double[size];
		Arrays.fill(coeWorking, 0.0);

		for (var entity : entities) {
			var entityCoe = getCoefficients.apply(entity);
			double fraction = weight.applyAsDouble(entity);
			for (int i : weighted) {
				coeWorking[i - indexFrom] += (entityCoe.getCoe(i)) * fraction;
			}
		}
		// Reduce back to float once done
		for (int i : weighted) {
			coe.setCoe(i, (float) coeWorking[i - indexFrom]);
		}

		// Pick one entity to fill in the fixed coefficients
		// Choice is arbitrary, they should all be the same
		var anyCoe = getCoefficients.apply(entities.iterator().next());

		for (int i = indexFrom; i < size + indexFrom; i++) {
			if (weighted.contains(i))
				continue;
			coe.setCoe(i, anyCoe.getCoe(i));
		}
		return coe;
	}

	FipSpecies leadGenus(FipLayer fipLayer) {
		return fipLayer.getSpecies().values().stream().sorted(Utils.compareUsing(FipSpecies::getFractionGenus).reversed())
				.findFirst().orElseThrow();
	}

	// EMP098
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

	// EMP041
	float estimatePrimaryQuadMeanDiameter(
			FipLayerPrimary fipLayer, BecDefinition bec, float breastHeightAge, float baseAreaOverstory
	) {
		var coeMap = Utils.<MatrixMap2<String, String, Coefficients>>expectParsedControl(
				controlMap, CoefficientParser.DQ_CONTROL_KEY, MatrixMap2.class
		);
		var modMap = Utils.<MatrixMap2<String, Region, Float>>expectParsedControl(
				controlMap, ModifierParser.CONTROL_KEY_MOD200_DQ, MatrixMap2.class
		);
		var upperBoundMap = Utils.<MatrixMap3<Region, String, Integer, Float>>expectParsedControl(
				controlMap, UpperCoefficientParser.CONTROL_KEY, MatrixMap3.class
		);

		var leadGenus = leadGenus(fipLayer);

		var decayBecAlias = bec.getDecayBec().getAlias();
		Coefficients coe = weightedCoefficientSum(
				List.of(0, 1, 2, 3, 4), 9, 0, fipLayer.getSpecies().values(), FipSpecies::getFractionGenus,
				s -> coeMap.get(decayBecAlias, s.getGenus())
		);

		var trAge = log(clamp(breastHeightAge, 5f, 350f));
		var height = fipLayer.getHeight();

		if (height <= coe.getCoe(5)) {
			return 7.6f;
		}

		/* @formatter:off */
		//    C0 = A(0)
		//    C1 = EXP(A(1)) + EXP(A(2)) * TR_AGE
		//    C2 = EXP(A(3)) + EXP(A(4)) * TR_AGE
		/* @formatter:on */
		var c0 = coe.get(0);
		var c1 = exp(coe.getCoe(1)) + exp(coe.getCoe(2)) * trAge;
		var c2 = exp(coe.getCoe(3)) + exp(coe.getCoe(4)) * trAge;

		/* @formatter:off */
		//      DQ = C0 + ( C1*(HD - A(5))**C2 )**2 * exp(A(7)*BAV)
		//     &        * (1.0 - A(6)*CC/100.0)
		/* @formatter:on */

		var quadMeanDiameter = c0 + pow(c1 * pow(height - coe.getCoe(5), c2), 2) * exp(coe.getCoe(7) * baseAreaOverstory)
				* (1f - coe.getCoe(6) * fipLayer.getCrownClosure() / 100f);

		/* @formatter:off */
		//      DQ = DQ * DQMOD200(JLEAD, INDEX_IC)
		/* @formatter:on */
		quadMeanDiameter *= modMap.get(leadGenus.getGenus(), bec.getRegion());

		quadMeanDiameter = max(quadMeanDiameter, 7.6f);

		// TODO
		var NDEBUG_2 = 0;
		if (NDEBUG_2 <= 0) {
			// See ISPSJF129
			var upperBound = upperBoundMap.get(bec.getRegion(), leadGenus.getGenus(), UpperCoefficientParser.DQ);
			quadMeanDiameter = min(quadMeanDiameter, upperBound);
		}

		return quadMeanDiameter;
	}

	// FT_BD
	static float treesPerHectare(float baseArea, float quadraticMeanDiameter) {
		if (baseArea != 0) {
			return baseArea / PI_40K / (quadraticMeanDiameter * quadraticMeanDiameter);
		}
		return 0f;
	}

	// FD_BT
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
