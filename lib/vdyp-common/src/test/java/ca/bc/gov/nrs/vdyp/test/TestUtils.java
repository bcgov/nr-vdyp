package ca.bc.gov.nrs.vdyp.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import ca.bc.gov.nrs.vdyp.application.VdypApplicationIdentifier;
import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.io.ConcreteFileResolver;
import ca.bc.gov.nrs.vdyp.io.FileResolver;
import ca.bc.gov.nrs.vdyp.io.parse.coe.BecDefinitionParser;
import ca.bc.gov.nrs.vdyp.io.parse.coe.BreakageParser;
import ca.bc.gov.nrs.vdyp.io.parse.coe.CloseUtilVolumeParser;
import ca.bc.gov.nrs.vdyp.io.parse.coe.GenusDefinitionParser;
import ca.bc.gov.nrs.vdyp.io.parse.coe.ModifierParser;
import ca.bc.gov.nrs.vdyp.io.parse.coe.UtilComponentWSVolumeParser;
import ca.bc.gov.nrs.vdyp.io.parse.coe.VeteranBAParser;
import ca.bc.gov.nrs.vdyp.io.parse.coe.VolumeNetDecayParser;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.io.parse.control.BaseControlParser;
import ca.bc.gov.nrs.vdyp.io.parse.control.ControlMapValueReplacer;
import ca.bc.gov.nrs.vdyp.io.parse.control.NonFipControlParser;
import ca.bc.gov.nrs.vdyp.io.parse.control.ResourceControlMapModifier;
import ca.bc.gov.nrs.vdyp.io.parse.control.StartApplicationControlParser;
import ca.bc.gov.nrs.vdyp.model.BaseVdypLayer;
import ca.bc.gov.nrs.vdyp.model.BaseVdypPolygon;
import ca.bc.gov.nrs.vdyp.model.BaseVdypSpecies;
import ca.bc.gov.nrs.vdyp.model.BecDefinition;
import ca.bc.gov.nrs.vdyp.model.BecLookup;
import ca.bc.gov.nrs.vdyp.model.Coefficients;
import ca.bc.gov.nrs.vdyp.model.GenusDefinition;
import ca.bc.gov.nrs.vdyp.model.GenusDefinitionMap;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2Impl;
import ca.bc.gov.nrs.vdyp.model.PolygonIdentifier;
import ca.bc.gov.nrs.vdyp.model.Region;

public class TestUtils {

	/**
	 * Create a stream returning the given sequence of lines.
	 */
	public static InputStream makeInputStream(String... lines) {
		return new ByteArrayInputStream(String.join("\r\n", lines).getBytes());
	}

	public static class MockOutputStream extends OutputStream {
		private boolean isOpen = true;
		private ByteArrayOutputStream realStream;
		private String streamName;

		public MockOutputStream(String streamName) {
			this.realStream = new ByteArrayOutputStream();
			this.streamName = streamName;
		}

		@Override
		public void write(int b) throws IOException {
			if (isOpen) {
				realStream.write(b);
			} else {
				fail("Attempt to write to closed stream");
			}
		}

		@Override
		public void close() throws IOException {
			isOpen = false;
		}

		@Override
		public String toString() {
			return realStream.toString();
		}

		public boolean isOpen() {
			return isOpen;
		}

		public void assertClosed() {
			assertTrue(!isOpen, "stream " + streamName + " was not closed");
		}

		public void assertContent(Matcher<String> matcher) {
			assertThat("Stream " + streamName + "contents", toString(), matcher);
		}
	}

	/**
	 * Read an output streams contents as a string..
	 */
	public static String readOutputStream(ByteArrayOutputStream os) {
		return os.toString();
	}

	/**
	 * Return a mock file resolver that expects to resolve one file with the given name and returns the given input
	 * stream.
	 *
	 * @param expectedFilename
	 * @param is
	 * @return
	 */
	public static ConcreteFileResolver fileResolver(String expectedFilename, InputStream is) {
		var result = new MockFileResolver("TEST");
		result.addStream(expectedFilename, is);
		return result;
	}

	/**
	 * Add a mock control map entry for BEC parse results
	 */
	public static void populateControlMapBecReal(Map<String, Object> controlMap) {
		populateControlMapFromResource(controlMap, new BecDefinitionParser(), "Becdef.dat");
	}

	/**
	 * Add a mock control map entry for SP0 parse results. Alternates assigning to Coastal and Interior regions,
	 * starting with Coastal.
	 */
	public static void populateControlMapBec(Map<String, Object> controlMap, String... aliases) {

		List<BecDefinition> becs = new ArrayList<>();

		int i = 0;
		for (var alias : aliases) {
			becs.add(new BecDefinition(alias, Region.values()[i % 2], "Test " + alias));
			i++;
		}

		controlMap.put(ControlKey.BEC_DEF.name(), new BecLookup(becs));
	}

	/**
	 * Add a mock control map entry for BEC parse results with species "B1" and "B2" for Coastal and Interior Regions
	 * respectively
	 */
	public static void populateControlMapBec(Map<String, Object> controlMap) {
		populateControlMapBec(controlMap, "B1", "B2");
	}

	@SuppressWarnings("unused")
	private static BecDefinition makeBec(String id, Region region, String name) {
		return new BecDefinition(id, region, name);
	}

	/**
	 * Add a mock control map entry for SP0 parse results with species "S1" and "S2"
	 */
	public static void populateControlMapGenus(Map<String, Object> controlMap) {
		populateControlMapGenus(controlMap, "S1", "S2");
	}

	/**
	 * Add a mock control map entry for SP0 parse results with 16 species
	 */
	public static void populateControlMapGenusReal(Map<String, Object> controlMap) {
		populateControlMapFromResource(controlMap, new GenusDefinitionParser(), "SP0DEF_v0.dat");
	}

	/**
	 * Get the species aliases expected
	 */
	public static String[] getSpeciesAliases() {
		return new String[] { "AC", "AT", "B", "C", "D", "E", "F", "H", "L", "MB", "PA", "PL", "PW", "PY", "S", "Y" };
	}

	/**
	 * Add a mock control map entry for SP0 parse results
	 */
	public static void populateControlMapGenus(Map<String, Object> controlMap, String... aliases) {

		List<GenusDefinition> sp0List = new ArrayList<>();

		int speciesNumber = 1;
		for (var alias : aliases) {
			sp0List.add(new GenusDefinition(alias, speciesNumber++, "Test " + alias));
		}

		controlMap.put(ControlKey.SP0_DEF.name(), new GenusDefinitionMap(sp0List));
	}

	/**
	 * Add a mock control map entries for equation groups
	 */
	public static void populateControlMapEquationGroups(
			Map<String, Object> controlMap, //
			BiFunction<String, String, int[]> mapper
	) {

		var becAliases = BecDefinitionParser.getBecs(controlMap).getBecAliases();
		var genusAliases = GenusDefinitionParser.getSpeciesAliases(controlMap);

		var volume = new MatrixMap2Impl<String, String, Integer>(genusAliases, becAliases, mapper.andThen(x -> x[0]));
		controlMap.put(ControlKey.VOLUME_EQN_GROUPS.name(), volume);

		var decay = new MatrixMap2Impl<String, String, Integer>(genusAliases, becAliases, mapper.andThen(x -> x[1]));
		controlMap.put(ControlKey.DECAY_GROUPS.name(), decay);

		var breakage = new MatrixMap2Impl<String, String, Integer>(genusAliases, becAliases, mapper.andThen(x -> x[2]));
		controlMap.put(ControlKey.BREAKAGE_GROUPS.name(), breakage);
	}

	/**
	 * Add mock control map entry for VeteranBQ Map
	 */
	public static void populateControlMapVeteranBq(Map<String, Object> controlMap) {
		populateControlMapFromResource(controlMap, new VeteranBAParser(), "REGBAV01.COE");
	}

	public static void
			populateControlMapVeteranDq(Map<String, Object> controlMap, BiFunction<String, Region, float[]> mapper) {
		var regions = Arrays.asList(Region.values());
		var genusAliases = GenusDefinitionParser.getSpeciesAliases(controlMap);

		var result = new MatrixMap2Impl<String, Region, Coefficients>(
				genusAliases, regions, mapper.andThen(x -> new Coefficients(x, 1))
		);
		controlMap.put(ControlKey.VETERAN_LAYER_DQ.name(), result);
	}

	public static void
			populateControlMapVeteranVolAdjust(Map<String, Object> controlMap, Function<String, float[]> mapper) {

		var genusAliases = GenusDefinitionParser.getSpeciesAliases(controlMap);

		var result = genusAliases.stream()
				.collect(Collectors.toMap(x -> x, mapper.andThen(x -> new Coefficients(x, 1))));

		controlMap.put(ControlKey.VETERAN_LAYER_VOLUME_ADJUST.name(), result);
	}

	public static void populateControlMapWholeStemVolume(
			Map<String, Object> controlMap, BiFunction<Integer, Integer, Optional<Coefficients>> mapper
	) {

		var groupIndicies = groupIndices(UtilComponentWSVolumeParser.MAX_GROUPS);

		populateControlMap2(controlMap, ControlKey.UTIL_COMP_WS_VOLUME.name(), UTIL_CLASSES, groupIndicies, mapper);
	}

	public static void populateControlMapCloseUtilization(
			Map<String, Object> controlMap, BiFunction<Integer, Integer, Optional<Coefficients>> mapper
	) {

		var groupIndicies = groupIndices(CloseUtilVolumeParser.MAX_GROUPS);

		populateControlMap2(controlMap, ControlKey.CLOSE_UTIL_VOLUME.name(), UTIL_CLASSES, groupIndicies, mapper);
	}

	public static void populateControlMapNetDecay(
			Map<String, Object> controlMap, BiFunction<Integer, Integer, Optional<Coefficients>> mapper
	) {

		var groupIndicies = groupIndices(VolumeNetDecayParser.MAX_GROUPS);

		populateControlMap2(controlMap, ControlKey.VOLUME_NET_DECAY.name(), UTIL_CLASSES, groupIndicies, mapper);
	}

	public static void
			populateControlMapNetWaste(Map<String, Object> controlMap, Function<String, Coefficients> mapper) {
		var speciesDim = Arrays.asList(getSpeciesAliases());

		populateControlMap1(controlMap, ControlKey.VOLUME_NET_DECAY_WASTE.name(), speciesDim, mapper);
	}

	public static void
			populateControlMapNetBreakage(Map<String, Object> controlMap, Function<Integer, Coefficients> mapper) {
		var groupIndicies = groupIndices(BreakageParser.MAX_GROUPS);

		populateControlMap1(controlMap, ControlKey.BREAKAGE.name(), groupIndicies, mapper);
	}

	public static <K1, K2, V> void populateControlMap2(
			Map<String, Object> controlMap, String key, Collection<K1> keys1, Collection<K2> keys2,
			BiFunction<K1, K2, V> mapper
	) {

		var result = new MatrixMap2Impl<>(keys1, keys2, mapper);

		controlMap.put(key, result);
	}

	public static <K, V> void populateControlMap1(
			Map<String, Object> controlMap, String key, Collection<K> keys1, Function<K, V> mapper
	) {

		var result = keys1.stream().collect(Collectors.toMap(k -> k, mapper));

		controlMap.put(key, result);
	}

	/**
	 * Fill in the decay modifiers in a control map with mock data for testing.
	 *
	 * @param controlMap
	 * @param mapper
	 */
	public static void
			populateControlMapDecayModifiers(Map<String, Object> controlMap, BiFunction<String, Region, Float> mapper) {
		var spec = Arrays.asList(TestUtils.getSpeciesAliases());
		var regions = Arrays.asList(Region.values());
		TestUtils
				.populateControlMap2(controlMap, ModifierParser.CONTROL_KEY_MOD301_DECAY.name(), spec, regions, mapper);
	}

	/**
	 * Fill in the waste modifiers in a control map with mock data for testing.
	 *
	 * @param controlMap
	 * @param mapper
	 */
	public static void
			populateControlMapWasteModifiers(Map<String, Object> controlMap, BiFunction<String, Region, Float> mapper) {
		var spec = Arrays.asList(TestUtils.getSpeciesAliases());
		var regions = Arrays.asList(Region.values());
		TestUtils
				.populateControlMap2(controlMap, ModifierParser.CONTROL_KEY_MOD301_WASTE.name(), spec, regions, mapper);
	}

	static final Collection<Integer> UTIL_CLASSES = IntStream.rangeClosed(-1, 4).mapToObj(x -> x).toList();

	static Collection<Integer> groupIndices(int max) {
		return IntStream.rangeClosed(1, max).mapToObj(x -> x).toList();
	}

	public static void populateControlMapFromResource(
			Map<String, Object> controlMap, ResourceControlMapModifier parser, String resource
	) {
		try (var is = TestUtils.class.getResourceAsStream("coe/" + resource)) {
			parser.modify(controlMap, is);
		} catch (IOException | ResourceParseException ex) {
			fail(ex);
		}
	}

	public static void populateControlMapFromStream(
			Map<String, Object> controlMap, ResourceControlMapModifier parser, InputStream is
	) {
		try {
			parser.modify(controlMap, is);
		} catch (IOException | ResourceParseException ex) {
			fail(ex);
		}
	}

	public static ConcreteFileResolver fileResolver(Class<?> klazz) {
		return new ConcreteFileResolver() {

			@Override
			public InputStream resolveForInput(String filename) throws IOException {
				assertThat("Attempt to resolve a null filename for input", filename, Matchers.notNullValue());
				InputStream resourceAsStream = klazz.getResourceAsStream(filename);
				if (resourceAsStream == null)
					throw new IOException("Could not load " + filename);
				return resourceAsStream;
			}

			@Override
			public OutputStream resolveForOutput(String filename) throws IOException {
				fail("Should not be opening file " + filename + " for output");
				return null;
			}

			@Override
			public FileResolver getOutputFileResolver() {
				return this;
			}

			@Override
			public FileResolver getInputFileResolver() {
				return this;
			}

			@Override
			public String toString(String filename) throws IOException {
				return klazz.getResource(filename).getPath();
			}

			@Override
			public Path toPath(String filename) throws IOException {
				return Path.of(toString(filename));
			}
		};
	}

	public static void assumeThat(java.lang.Object actual, @SuppressWarnings("rawtypes") org.hamcrest.Matcher matcher) {
		assumeTrue(matcher.matches(actual));
	}

	public static Map<String, Object> loadControlMap(BaseControlParser parser, Class<?> klazz, String resourceName)
			throws IOException, ResourceParseException {
		try (var is = klazz.getResourceAsStream(resourceName)) {

			return parser.parse(is, TestUtils.fileResolver(klazz), new HashMap<>());
		}
	}

	public static Map<String, Object> loadControlMap() {
		return loadControlMap(Path.of("VRISTART.CTR"));
	}

	public static Map<String, Object> loadControlMap(Path controlMapPath) {
		BaseControlParser parser = new TestNonFipControlParser();
		try {
			return TestUtils.loadControlMap(parser, TestUtils.class, controlMapPath.toString());
		} catch (IOException | ResourceParseException ex) {
			fail(ex);
			return null;
		}
	}

	public static PolygonIdentifier polygonId(String name, int year) {
		return new PolygonIdentifier(name, year);
	}

	public static StartApplicationControlParser startAppControlParser() {
		return new TestNonFipControlParser();
	}

	static private class TestNonFipControlParser extends NonFipControlParser {

		public TestNonFipControlParser() {
			initialize();
		}

		@Override
		protected List<ControlMapValueReplacer<Object, String>> inputFileParsers() {
			return Collections.emptyList();
		}

		@Override
		protected List<ControlKey> outputFileParsers() {
			return Collections.emptyList();
		}

		@Override
		protected VdypApplicationIdentifier getProgramId() {
			return VdypApplicationIdentifier.VRI_START;
		}
	}

	/**
	 * Do nothing to mutate valid test data
	 */
	public static final <T> Consumer<T> valid() {
		return x -> {
		};
	}

	public static BiFunction<Integer, Integer, Optional<Coefficients>> wholeStemMap(int group) {
		return (u, g) -> {
			if (g == group) {
				switch (u) {
				case 1:
					return Optional.of(
							new Coefficients(new float[] { -1.20775998f, 0.670000017f, 1.43023002f, -0.886789978f }, 0)
					);
				case 2:
					return Optional.of(
							new Coefficients(new float[] { -1.58211005f, 0.677200019f, 1.36449003f, -0.781769991f }, 0)
					);
				case 3:
					return Optional.of(
							new Coefficients(new float[] { -1.61995006f, 0.651030004f, 1.17782998f, -0.607379973f }, 0)
					);
				case 4:
					return Optional
							.of(
									new Coefficients(
											new float[] { -0.172529995f, 0.932619989f, -0.0697899982f,
													-0.00362000009f },
											0
									)
							);
				}
			}
			return Optional.empty();
		};
	}

	public static BiFunction<Integer, Integer, Optional<Coefficients>> closeUtilMap(int group) {
		return (u, g) -> {
			if (g == group) {
				switch (u) {
				case 1:
					return Optional.of(new Coefficients(new float[] { -10.6339998f, 0.835500002f, 0f }, 1));
				case 2:
					return Optional.of(new Coefficients(new float[] { -4.44999981f, 0.373400003f, 0f }, 1));
				case 3:
					return Optional.of(new Coefficients(new float[] { -0.796000004f, 0.141299993f, 0.0033499999f }, 1));
				case 4:
					return Optional.of(new Coefficients(new float[] { 2.35400009f, 0.00419999985f, 0.0247699991f }, 1));
				}
			}
			return Optional.empty();
		};
	}

	public static BiFunction<Integer, Integer, Optional<Coefficients>> netDecayMap(int group) {
		return (u, g) -> {
			if (g == group) {
				switch (u) {
				case 1:
					return Optional.of(new Coefficients(new float[] { 9.84819984f, -0.224209994f, -0.814949989f }, 1));
				case 2:
					return Optional.of(new Coefficients(new float[] { 9.61330032f, -0.224209994f, -0.814949989f }, 1));
				case 3:
					return Optional.of(new Coefficients(new float[] { 9.40579987f, -0.224209994f, -0.814949989f }, 1));
				case 4:
					return Optional.of(new Coefficients(new float[] { 10.7090998f, -0.952880025f, -0.808309972f }, 1));
				}
			}
			return Optional.empty();
		};
	}

	/**
	 * Assert that a polygon has a layer of the given type.
	 *
	 * @param polygon the polygon
	 * @param type    the type of layer
	 * @param number  the total number of layers the polygon should have
	 * @return the layer
	 */
	public static <P extends BaseVdypPolygon<L, ?, ?, ?>, L extends BaseVdypLayer<?, ?>> L
			assertLayer(P polygon, LayerType type) {
		assertThat(polygon, hasProperty("layers", hasKey(type)));

		var resultLayer = polygon.getLayers().get(type);

		assertThat(resultLayer, hasProperty("polygonIdentifier", equalTo(polygon.getPolygonIdentifier())));
		assertThat(resultLayer, hasProperty("layerType", is(type)));

		return resultLayer;
	};

	/**
	 * Assert that a polygon only has a primary layer.
	 *
	 * @param polygon the polygon
	 * @param polygon
	 * @return the primary layer
	 */
	public static <P extends BaseVdypPolygon<L, ?, ?, ?>, L extends BaseVdypLayer<?, ?>> L
			assertOnlyPrimaryLayer(P polygon) {
		assertThat(polygon, hasProperty("layers", aMapWithSize(1)));
		return assertLayer(polygon, LayerType.PRIMARY);
	};

	/**
	 * Assert that a polygon has a primary layer, allowing for other layers.
	 *
	 * @param polygon the polygon
	 * @param polygon
	 * @return the primary layer
	 */
	public static <P extends BaseVdypPolygon<L, ?, ?, ?>, L extends BaseVdypLayer<?, ?>> L
			assertHasPrimaryLayer(P polygon) {
		return assertLayer(polygon, LayerType.PRIMARY);
	};

	/**
	 * Assert that a polygon has a veteran layer as well as a primary.
	 *
	 * @param polygon the polygon
	 * @param polygon
	 * @return the veteran layer
	 */
	public static <P extends BaseVdypPolygon<L, ?, ?, ?>, L extends BaseVdypLayer<?, ?>> L
			assertHasVeteranLayer(P polygon) {
		assertThat(polygon, hasProperty("layers", aMapWithSize(2)));
		return assertLayer(polygon, LayerType.VETERAN);
	};

	/**
	 * Assert that a layer has a species of the given genus ID.
	 *
	 * @param layer
	 * @param id
	 * @return The species
	 */
	public static <L extends BaseVdypLayer<S, ?>, S extends BaseVdypSpecies<?>> S assertHasSpecies(L layer, String id) {

		assertThat(layer, hasProperty("species", hasKey(id)));

		var resultSpecies = layer.getSpecies().get(id);
		assertThat(resultSpecies, hasProperty("polygonIdentifier", equalTo(layer.getPolygonIdentifier())));
		assertThat(resultSpecies, hasProperty("layerType", is(layer.getLayerType())));
		assertThat(resultSpecies, hasProperty("genus", is(id)));

		return resultSpecies;
	};

	/**
	 * Assert that a layer has a species of the given genera IDs.
	 *
	 * @param layer
	 * @param ids
	 * @return the first species specified
	 */
	public static <L extends BaseVdypLayer<S, ?>, S extends BaseVdypSpecies<?>> S
			assertHasSpecies(L layer, String... ids) {
		assertThat(layer, hasProperty("species", aMapWithSize(ids.length)));

		for (var id : ids) {
			assertHasSpecies(layer, id);
		}

		return layer.getSpecies().get(ids[0]);
	}
}
