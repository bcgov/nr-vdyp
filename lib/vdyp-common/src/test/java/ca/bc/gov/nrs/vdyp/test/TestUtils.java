package ca.bc.gov.nrs.vdyp.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
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

import org.apache.commons.lang3.StringEscapeUtils;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import ca.bc.gov.nrs.vdyp.application.VdypApplicationIdentifier;
import ca.bc.gov.nrs.vdyp.common.ControlKey;
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
import ca.bc.gov.nrs.vdyp.model.UtilizationClass;
import ca.bc.gov.nrs.vdyp.model.UtilizationVector;
import ca.bc.gov.nrs.vdyp.model.VdypCompatibilityVariables;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;

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
	public static FileResolver fileResolver(String expectedFilename, InputStream is) {
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

	public static FileResolver fileResolver(Class<?> klazz) {
		return new FileResolver() {

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
			public String toString(String filename) throws IOException {
				return klazz.getResource(filename).getPath();
			}

			@Override
			public FileResolver relative(String path) throws IOException {
				fail("Should not be requesting relative file resolver " + path);
				return null;
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

	public static BecDefinition mockBec() {
		return new BecDefinition("T", Region.COASTAL, "Test");
	}

	private static void line(Appendable out, String line, Object... args) throws UncheckedIOException {
		try {
			out.append(String.format(line, args)).append("\n");
		} catch (IOException e) {
			new UncheckedIOException(e);
		}
	}

	private static String stringLiteral(String s) {
		return String.format("\"%s\"", StringEscapeUtils.escapeJava(s));
	}

	private static String utilVectorLiteral(UtilizationVector uv) {
		if (uv.size() == 2) {
			return String.format("Utils.heightVector(%ff, %ff)", uv.getSmall(), uv.getAll());
		}
		if (uv.get(UtilizationClass.SMALL) == 0 && uv.get(UtilizationClass.U75TO125) == 0
				&& uv.get(UtilizationClass.U125TO175) == 0 && uv.get(UtilizationClass.U175TO225) == 0
				&& uv.getAll() == uv.getLarge()) {
			return String.format("Utils.utilizationVector(%f)", uv.getAll());
		}
		if (Math.abs(UtilizationClass.ALL_CLASSES.stream().mapToDouble(uv::get).sum() - uv.getAll()) < 0.00001) {
			return String.format(
					"Utils.utilizationVector(%ff, %ff, %ff, %ff, %ff)", uv.getSmall(),
					uv.get(UtilizationClass.U75TO125), uv.get(UtilizationClass.U125TO175),
					uv.get(UtilizationClass.U175TO225), uv.get(UtilizationClass.OVER225)
			);
		}
		return String.format(
				"Utils.utilizationVector(%ff, %ff, %ff, %ff, %ff, %ff) /* ALL does not match sum of bands */",
				uv.getSmall(), uv.getAll(), uv.get(UtilizationClass.U75TO125), uv.get(UtilizationClass.U125TO175),
				uv.get(UtilizationClass.U175TO225), uv.get(UtilizationClass.OVER225)
		);

	}

	private static String enumLiteral(Enum<?> e) {
		e.getClass().getName();
		return String.format("%s.%s", e.getClass().getName(), e.toString());
	}

	private static String floatLiteral(float v) {
		return String.format("%ff", v);
	}

	private static String intLiteral(int v) {
		return String.format("%d", v);
	}

	private static <T> String optionalLiteral(Optional<T> opt, Function<T, String> valueLiteral) {
		return opt.map(valueLiteral).map(s -> String.format("Optional.of(%s)", s)).orElse("Optional.empty()");
	}

	/**
	 * Serializes a VdypPolygon as Java code that can be executed to recreate it. Meant to be used to aid in creating
	 * unit tests.
	 */
	public static void writeModel(VdypPolygon poly, Appendable out, String assignTo) throws IOException {
		try {
			line(out, "/* the following Polygon definition was generated */");
			line(out, "");
			line(out, "%s = VdypPolygon.build(pb -> {", assignTo);

			line(
					out, "	pb.polygonIdentifier(%s, %d);", stringLiteral(poly.getPolygonIdentifier().getBase()),
					poly.getPolygonIdentifier().getYear()
			);

			line(out, "");
			line(
					out, "	pb.biogeoclimaticZone(Utils.getBec(%s, controlMap));",
					stringLiteral(poly.getBiogeoclimaticZone().getAlias())
			);
			line(out, "	pb.forestInventoryZone(%s);", stringLiteral(poly.getForestInventoryZone()));
			line(out, "");
			line(
					out, "	pb.inventoryTypeGroup(%s);",
					optionalLiteral(poly.getInventoryTypeGroup(), TestUtils::intLiteral)
			);
			line(out, "	pb.targetYear(%s);", optionalLiteral(poly.getTargetYear(), TestUtils::intLiteral));
			line(out, "");
			line(out, "	pb.mode(%s);", optionalLiteral(poly.getMode(), TestUtils::enumLiteral));
			line(out, "	pb.percentAvailable(%s);", floatLiteral(poly.getPercentAvailable()));
			line(out, "");
			for (var layer : poly.getLayers().values()) {
				line(out, "	pb.addLayer(lb -> {");
				line(out, "		lb.layerType(%s);", enumLiteral(layer.getLayerType()));
				line(out, "");
				line(
						out, "		lb.empiricalRelationshipParameterIndex(%s);",
						optionalLiteral(layer.getEmpiricalRelationshipParameterIndex(), TestUtils::intLiteral)
				);
				line(out, "");
				line(
						out, "		lb.inventoryTypeGroup(%s);",
						optionalLiteral(layer.getInventoryTypeGroup(), TestUtils::intLiteral)
				);
				line(out, "");
				line(out, "		lb.loreyHeight(%s);", utilVectorLiteral(layer.getLoreyHeightByUtilization()));
				line(out, "		lb.treesPerHectare(%s);", utilVectorLiteral(layer.getTreesPerHectareByUtilization()));
				line(
						out, "		lb.quadMeanDiameter(%s);",
						utilVectorLiteral(layer.getQuadraticMeanDiameterByUtilization())
				);
				line(out, "		lb.baseArea(%s);", utilVectorLiteral(layer.getBaseAreaByUtilization()));
				line(out, "");
				line(out, "		lb.wholeStemVolume(%s);", utilVectorLiteral(layer.getWholeStemVolumeByUtilization()));
				line(
						out, "		lb.closeUtilizationVolumeByUtilization(%s);",
						utilVectorLiteral(layer.getCloseUtilizationVolumeByUtilization())
				);
				line(
						out, "		lb.closeUtilizationVolumeNetOfDecayByUtilization(%s);",
						utilVectorLiteral(layer.getCloseUtilizationVolumeNetOfDecayByUtilization())
				);
				line(
						out, "		lb.closeUtilizationVolumeNetOfDecayAndWasteByUtilization(%s);",
						utilVectorLiteral(layer.getCloseUtilizationVolumeNetOfDecayAndWasteByUtilization())
				);
				line(
						out, "		lb.closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(%s);",
						utilVectorLiteral(layer.getCloseUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization())
				);
				line(out, "");
				for (var spec : layer.getSpecies().values()) {
					line(out, "		lb.addSpecies(sb -> {");
					line(out, "			sb.genus(%s);", stringLiteral(spec.getGenus()));
					line(out, "			sb.genus(%d);", spec.getGenusIndex());
					line(out, "");
					line(out, "			sb.breakageGroup(%d);", spec.getBreakageGroup());
					line(out, "			sb.volumeGroup(%d);", spec.getVolumeGroup());
					line(out, "			sb.decayGroup(%d);", spec.getDecayGroup());
					line(out, "");
					line(out, "			sb.percentGenus(%s);", floatLiteral(spec.getPercentGenus()));
					line(out, "");

					spec.getSp64DistributionSet().getSp64DistributionList().forEach(sp64 -> {
						line(
								out, "			sb.addSp64Distribution(%s, %s);", stringLiteral(sp64.getGenusAlias()),
								floatLiteral(sp64.getPercentage())
						);
					});

					line(out, "");
					line(out, "			sb.loreyHeight(%s);", utilVectorLiteral(spec.getLoreyHeightByUtilization()));
					line(
							out, "			sb.treesPerHectare(%s);",
							utilVectorLiteral(spec.getTreesPerHectareByUtilization())
					);
					line(
							out, "			sb.quadMeanDiameter(%s);",
							utilVectorLiteral(spec.getQuadraticMeanDiameterByUtilization())
					);
					line(out, "			sb.baseArea(%s);", utilVectorLiteral(spec.getBaseAreaByUtilization()));
					line(out, "");
					line(
							out, "			sb.wholeStemVolume(%s);",
							utilVectorLiteral(spec.getWholeStemVolumeByUtilization())
					);
					line(
							out, "			sb.closeUtilizationVolumeByUtilization(%s);",
							utilVectorLiteral(spec.getCloseUtilizationVolumeByUtilization())
					);
					line(
							out, "			sb.closeUtilizationVolumeNetOfDecayByUtilization(%s);",
							utilVectorLiteral(spec.getCloseUtilizationVolumeNetOfDecayByUtilization())
					);
					line(
							out, "			sb.closeUtilizationVolumeNetOfDecayAndWasteByUtilization(%s);",
							utilVectorLiteral(spec.getCloseUtilizationVolumeNetOfDecayAndWasteByUtilization())
					);
					line(
							out, "			sb.closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(%s);",
							utilVectorLiteral(spec.getCloseUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization())
					);
					line(out, "");
					spec.getCompatibilityVariables().ifPresent(cv -> {
						line(out, "			sb.addCompatibilityVariables(cvb -> {");
						line(out, "");

						line(
								out,
								"				MatrixMap3<UtilizationClass, UtilizationClassVariable, LayerType, Float> cvVolume = new MatrixMap3Impl<>("
						);
						line(out, "						UtilizationClass.UTIL_CLASSES, ");
						line(out, "						VdypCompatibilityVariables.VOLUME_UTILIZATION_VARIABLES, ");
						line(out, "						LayerType.ALL_USED,");
						line(out, "						(uc, vv, lt) -> 0f");
						line(out, "				);");
						for (var uc : UtilizationClass.UTIL_CLASSES) {
							for (var vv : VdypCompatibilityVariables.VOLUME_UTILIZATION_VARIABLES) {
								for (var lt : LayerType.ALL_USED) {
									line(
											out,
											"				cvVolume.put(UtilizationClass.%s, UtilizationClassVariable.%s, LayerType.%s, %s);",
											uc, vv, lt, floatLiteral(cv.getCvVolume(uc, vv, lt))
									);
								}
							}
						}
						line(
								out,
								"				MatrixMap2<UtilizationClass, LayerType, Float> cvBasalArea = new MatrixMap2Impl<>("
						);
						line(out, "						UtilizationClass.UTIL_CLASSES, ");
						line(out, "						LayerType.ALL_USED,");
						line(out, "						(uc, lt) -> 0f");
						line(out, "				);");

						for (var uc : UtilizationClass.UTIL_CLASSES) {
							for (var lt : LayerType.ALL_USED) {
								line(
										out, "				cvBasalArea.put(UtilizationClass.%s, LayerType.%s, %s);",
										uc, lt, floatLiteral(cv.getCvBasalArea(uc, lt))
								);
							}
						}

						line(
								out,
								"				MatrixMap2<UtilizationClass, LayerType, Float> cvQuadraticMeanDiameter = new MatrixMap2Impl<>("
						);
						line(out, "						UtilizationClass.UTIL_CLASSES, ");
						line(out, "						LayerType.ALL_USED,");
						line(out, "						(uc, lt) -> 0f");
						line(out, "				);");
						line(out, "");
						for (var uc : UtilizationClass.UTIL_CLASSES) {
							for (var lt : LayerType.ALL_USED) {
								line(
										out,
										"				cvQuadraticMeanDiameter.put(UtilizationClass.%s, LayerType.%s, %s);",
										uc, lt, floatLiteral(cv.getCvQuadraticMeanDiameter(uc, lt))
								);
							}
						}
						line(out, "");
						line(
								out,
								"				Map<UtilizationClassVariable, Float> cvPrimaryLayerSmall = new HashMap<>();"
						);
						line(out, "");
						for (var ucv : VdypCompatibilityVariables.SMALL_UTILIZATION_VARIABLES) {
							line(
									out, "				cvPrimaryLayerSmall.put(UtilizationClassVariable.%s, %s);", ucv,
									floatLiteral(cv.getCvPrimaryLayerSmall(ucv))
							);
						}
						line(out, "");
						line(out, "				cvb.cvVolume(cvVolume);");
						line(out, "				cvb.cvBasalArea(cvBasalArea);");
						line(out, "				cvb.cvQuadraticMeanDiameter(cvQuadraticMeanDiameter);");
						line(out, "				cvb.cvPrimaryLayerSmall(cvPrimaryLayerSmall);");
						line(out, "			});");
					});
					line(out, "");
					spec.getSite().ifPresent(site -> {
						line(out, "");

						line(out, "			sb.addSite(ib -> {");
						line(
								out, "				ib.ageTotal(%s);",
								optionalLiteral(site.getAgeTotal(), TestUtils::floatLiteral)
						);
						line(
								out, "				ib.height(%s);",
								optionalLiteral(site.getHeight(), TestUtils::floatLiteral)
						);
						line(
								out, "				ib.siteCurveNumber(%s);",
								optionalLiteral(site.getSiteCurveNumber(), TestUtils::intLiteral)
						);
						line(
								out, "				ib.siteIndex(%s);",
								optionalLiteral(site.getSiteIndex(), TestUtils::floatLiteral)
						);
						line(
								out, "				ib.yearsToBreastHeight(%s);",
								optionalLiteral(site.getYearsToBreastHeight(), TestUtils::floatLiteral)
						);
						line(out, "			});");
						line(out, "");
					});
					line(out, "");
					line(out, "		});");
				}
				line(out, "");
				line(
						out, "		lb.primaryGenus(%s);",
						optionalLiteral(layer.getPrimaryGenus(), TestUtils::stringLiteral)
				);
				line(out, "	});");
			}
			line(out, "});");

			line(out, "");

			line(out, "/* End of generated polygon Definition */");

		} catch (UncheckedIOException e) {
			throw new IOException(e);
		}
	}

}
