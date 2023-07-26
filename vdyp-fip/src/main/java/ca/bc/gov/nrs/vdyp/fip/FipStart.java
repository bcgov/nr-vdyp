package ca.bc.gov.nrs.vdyp.fip;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

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
import ca.bc.gov.nrs.vdyp.io.parse.DecayEquationGroupParser;
import ca.bc.gov.nrs.vdyp.io.parse.ResourceParseException;
import ca.bc.gov.nrs.vdyp.io.parse.StreamingParser;
import ca.bc.gov.nrs.vdyp.io.parse.StreamingParserFactory;
import ca.bc.gov.nrs.vdyp.io.parse.VeteranBQParser;
import ca.bc.gov.nrs.vdyp.io.parse.VolumeEquationGroupParser;
import ca.bc.gov.nrs.vdyp.model.BecLookup.Substitution;
import ca.bc.gov.nrs.vdyp.model.Coefficients;
import ca.bc.gov.nrs.vdyp.model.Layer;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2;
import ca.bc.gov.nrs.vdyp.model.Region;
import ca.bc.gov.nrs.vdyp.model.VdypLayer;
import ca.bc.gov.nrs.vdyp.model.VdypSpecies;

public class FipStart {

	static private final Logger log = LoggerFactory.getLogger(FipStart.class);

	public static final int CONFIG_LOAD_ERROR = 1; // TODO check what Fortran FIPStart would exit with.
	public static final int PROCESSING_ERROR = 2; // TODO check what Fortran FIPStart would exit with.

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
		 * c At this point we SHOULD invoke a root finding procedure C sets species
		 * percents and adjusts DQ by species. C fills in main components, through
		 * whole-stem volume c INSTEAD, I will assume %volumes apply to % BA's
		 */

		for (var vSpec : vdypSpecies.values()) {
			vSpec.getBaseAreaByUtilization()
					.setCoe(4, baseAreaByUtilization.getCoe(4) * vSpec.getPercentGenus() / 100f);
		}

		var vdypLayer = new VdypLayer(polygonIdentifier, layer);
		vdypLayer.setAgeTotal(ageTotal);
		vdypLayer.setHeight(height);
		vdypLayer.setYearsToBreastHeight(yearsToBreastHeight);
		vdypLayer.setBreastHeightAge(breastHeightAge);
		vdypLayer.setSpecies(vdypSpecies);
		vdypLayer.setPrimaryGenus(primaryGenus);
		vdypLayer.setBaseAreaByUtilization(baseAreaByUtilization);

		return vdypLayer;
	}

	int getGroup(FipPolygon fipPolygon, MatrixMap2<String, String, Integer> volumeGroupMap, VdypSpecies vSpec) {
		return volumeGroupMap.get(vSpec.getGenus(), fipPolygon.getBiogeoclimaticZone())
				.orElseThrow(() -> new AssertionError("Equation Group map should not return empty"));
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
				.getM(genus, region).orElseThrow(
						() -> new ProcessingException(
								"Could not find Veteran Base Area Coefficients for genus " + genus + " and region "
										+ region
						)
				);

		// mismatched index is copied from VDYP7
		float a0 = coefficients.getCoe(1);
		float a1 = coefficients.getCoe(2);
		float a2 = coefficients.getCoe(3);

		float baseArea = a0 * (float) Math.pow(Math.max(height - a1, 0.0f), a2);

		baseArea *= crownClosure / 4.0f;

		baseArea = Math.max(baseArea, 0.01f);

		return baseArea;
	}

	private static <E extends Throwable> void throwIfPresent(Optional<E> opt) throws E {
		if (opt.isPresent()) {
			throw opt.get();
		}
	}

	private static ProcessingException validationError(String template, Object... values) {
		return new ProcessingException(String.format(template, values));
	};
}
