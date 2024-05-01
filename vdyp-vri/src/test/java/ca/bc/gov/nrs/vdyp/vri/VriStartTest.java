package ca.bc.gov.nrs.vdyp.vri;

import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.closeTo;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.notPresent;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;

import ca.bc.gov.nrs.vdyp.application.ApplicationTestUtils;
import ca.bc.gov.nrs.vdyp.application.StandProcessingException;
import ca.bc.gov.nrs.vdyp.application.VdypStartApplication;
import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.common.Utils;
import ca.bc.gov.nrs.vdyp.io.parse.streaming.MockStreamingParser;
import ca.bc.gov.nrs.vdyp.io.parse.streaming.StreamingParser;
import ca.bc.gov.nrs.vdyp.io.parse.streaming.StreamingParserFactory;
import ca.bc.gov.nrs.vdyp.model.BecDefinition;
import ca.bc.gov.nrs.vdyp.model.BecLookup;
import ca.bc.gov.nrs.vdyp.model.Coefficients;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2Impl;
import ca.bc.gov.nrs.vdyp.model.PolygonMode;
import ca.bc.gov.nrs.vdyp.model.Region;
import ca.bc.gov.nrs.vdyp.test.MockFileResolver;
import ca.bc.gov.nrs.vdyp.test.VdypMatchers;
import ca.bc.gov.nrs.vdyp.vri.model.VriLayer;
import ca.bc.gov.nrs.vdyp.vri.model.VriPolygon;
import ca.bc.gov.nrs.vdyp.vri.model.VriSite;
import ca.bc.gov.nrs.vdyp.vri.model.VriSpecies;
import ca.bc.gov.nrs.vdyp.vri.test.VriTestUtils;

class VriStartTest {

	Map<String, Object> controlMap = new HashMap<>();

	ByteArrayOutputStream polyOut;
	ByteArrayOutputStream specOut;
	ByteArrayOutputStream utilOut;

	private MockFileResolver dummyInput() {
		controlMap.put(ControlKey.VRI_OUTPUT_VDYP_POLYGON.name(), "DUMMY1");
		controlMap.put(ControlKey.VRI_OUTPUT_VDYP_LAYER_BY_SPECIES.name(), "DUMMY2");
		controlMap.put(ControlKey.VRI_OUTPUT_VDYP_LAYER_BY_SP0_BY_UTIL.name(), "DUMMY3");

		MockFileResolver resolver = new MockFileResolver("Test");

		polyOut = new ByteArrayOutputStream();
		specOut = new ByteArrayOutputStream();
		utilOut = new ByteArrayOutputStream();

		resolver.addStream("DUMMY1", (OutputStream) polyOut);
		resolver.addStream("DUMMY2", (OutputStream) specOut);
		resolver.addStream("DUMMY3", (OutputStream) utilOut);
		return resolver;
	}

	@SuppressWarnings("resource")
	@Test
	void testEstimateBaseAreaYield() throws StandProcessingException {
		Map<String, Object> controlMap = VriTestUtils.loadControlMap();
		VriStart app = new VriStart();
		ApplicationTestUtils.setControlMap(app, controlMap);

		var polygon = VriPolygon.build(pBuilder -> {
			pBuilder.polygonIdentifier("Test");
			pBuilder.biogeoclimaticZone("IDF");
			pBuilder.yieldFactor(1.0f);
			pBuilder.buildLayer(lBuilder -> {
				lBuilder.layerType(LayerType.PRIMARY);
				((VriLayer.Builder) lBuilder).crownClosure(57.8f);
				((VriLayer.Builder) lBuilder).baseArea(66f);
				((VriLayer.Builder) lBuilder).treesPerHectare(850f);
				((VriLayer.Builder) lBuilder).utilization(7.5f);
				((VriLayer.Builder) lBuilder).empiricalRelationshipParameterIndex(76);

				lBuilder.addSpecies(sBuilder -> {
					sBuilder.genus("B"); // 3
					sBuilder.percentGenus(2.99999993f);
				});
				lBuilder.addSpecies(sBuilder -> {
					sBuilder.genus("C"); // 4
					sBuilder.percentGenus(30.0000012f);
				});
				lBuilder.addSpecies(sBuilder -> {
					sBuilder.genus("H"); // 8
					sBuilder.percentGenus(48.9000022f);
				});
				lBuilder.addSpecies(sBuilder -> {
					sBuilder.genus("S"); // 15
					sBuilder.percentGenus(18.1000009f);
				});
			});
		});

		var species = polygon.getLayers().get(LayerType.PRIMARY).getSpecies().values();

		var bec = Utils.expectParsedControl(controlMap, ControlKey.BEC_DEF, BecLookup.class).get("IDF").get();

		float result = app.estimateBaseAreaYield(32f, 190.300003f, Optional.empty(), false, species, bec, 76);

		assertThat(result, closeTo(62.0858421f));
	}

	@SuppressWarnings("resource")
	@Test
	void testEstimateBaseAreaYieldCoefficients() throws StandProcessingException {
		Map<String, Object> controlMap = VriTestUtils.loadControlMap();
		VriStart app = new VriStart();
		ApplicationTestUtils.setControlMap(app, controlMap);

		var polygon = VriPolygon.build(pBuilder -> {
			pBuilder.polygonIdentifier("Test");
			pBuilder.biogeoclimaticZone("IDF");
			pBuilder.yieldFactor(1.0f);
			pBuilder.buildLayer(lBuilder -> {
				lBuilder.layerType(LayerType.PRIMARY);
				((VriLayer.Builder) lBuilder).crownClosure(57.8f);
				((VriLayer.Builder) lBuilder).baseArea(66f);
				((VriLayer.Builder) lBuilder).treesPerHectare(850f);
				((VriLayer.Builder) lBuilder).utilization(7.5f);
				((VriLayer.Builder) lBuilder).empiricalRelationshipParameterIndex(76);

				lBuilder.addSpecies(sBuilder -> {
					sBuilder.genus("B"); // 3
					sBuilder.percentGenus(2.99999993f);
				});
				lBuilder.addSpecies(sBuilder -> {
					sBuilder.genus("C"); // 4
					sBuilder.percentGenus(30.0000012f);
				});
				lBuilder.addSpecies(sBuilder -> {
					sBuilder.genus("H"); // 8
					sBuilder.percentGenus(48.9000022f);
				});
				lBuilder.addSpecies(sBuilder -> {
					sBuilder.genus("S"); // 15
					sBuilder.percentGenus(18.1000009f);
				});
			});
		});

		var species = polygon.getLayers().get(LayerType.PRIMARY).getSpecies().values();

		var bec = Utils.expectParsedControl(controlMap, ControlKey.BEC_DEF, BecLookup.class).get("IDF").get();

		Coefficients result = app.estimateBaseAreaYieldCoefficients(species, bec);

		assertThat(
				result, VdypMatchers.coe(
						0, 7.29882717f, 0.934803009f, 7.22950029f, 0.478330702f, 0.00542420009f, 0f, -0.00899999961f
				)
		);
	}

	@Test
	void testFindDefaultModeLowBA() throws Exception {
		var app = new VriStart();

		MockFileResolver resolver = dummyInput();

		controlMap.put(ControlKey.MINIMA.name(), Utils.constMap(map -> {
			map.put(VriControlParser.MINIMUM_BASE_AREA, 0f);
			map.put(VriControlParser.MINIMUM_HEIGHT, 6f);
			map.put(VriControlParser.MINIMUM_PREDICTED_BASE_AREA, 14f); // Set this high for test
		}));

		var baYieldMap = new MatrixMap2Impl<>(
				List.of("IDF"), List.of("B"),
				(k1, k2) -> new Coefficients(
						new float[] { -2.9907f + 11.2376f, 2.4562f - 3.2149f, 7.2295f + 0.0000f, 0.5074f + 0.3763f,
								-0.0150f + 0.030f, 0.0120f + -0.0364f, -0.0090f + 0.0000f },
						0
				)
		);
		controlMap.put(ControlKey.BA_YIELD.name(), baYieldMap);

		controlMap.put(ControlKey.BA_DQ_UPPER_BOUNDS.name(), Utils.constMap(map -> {
			map.put(76, new Coefficients(new float[] { 89.69f, 53.60f }, 1));
		}));

		app.init(resolver, controlMap);

		Optional<Float> ageTotal = Optional.of(200f);
		Optional<Float> yearsToBreastHeight = Optional.of(191f);
		Optional<Float> height = Optional.of(10f);
		Optional<Float> baseArea = Optional.of(30f);
		Optional<Float> treesPerHectare = Optional.of(300f);
		Optional<Float> percentForest = Optional.of(90f);

		Collection<VriSpecies> species = List.of(VriSpecies.build(builder -> {
			builder.polygonIdentifier("Test");
			builder.layerType(LayerType.PRIMARY);
			builder.genus("B");
			builder.percentGenus(100f);
		}));
		var bec = new BecDefinition("IDF", Region.INTERIOR, "Interior Douglas Fir");

		var result = app.findDefaultPolygonMode(
				ageTotal, yearsToBreastHeight, height, baseArea, treesPerHectare, percentForest, species, bec, Optional
						.of(76)
		);

		assertThat(result, is(PolygonMode.YOUNG));

		app.close();
	}

	@Test
	void testFindDefaultModeLowHeight() throws Exception {
		var app = new VriStart();

		MockFileResolver resolver = dummyInput();

		controlMap.put(ControlKey.MINIMA.name(), Utils.constMap(map -> {
			map.put(VriControlParser.MINIMUM_BASE_AREA, 0f);
			map.put(VriControlParser.MINIMUM_HEIGHT, 6f);
			map.put(VriControlParser.MINIMUM_PREDICTED_BASE_AREA, 2f);
		}));

		var baYieldMap = new MatrixMap2Impl<>(
				List.of("IDF"), List.of("B"),
				(k1, k2) -> new Coefficients(
						new float[] { -2.9907f + 11.2376f, 2.4562f - 3.2149f, 7.2295f + 0.0000f, 0.5074f + 0.3763f,
								-0.0150f + 0.030f, 0.0120f + -0.0364f, -0.0090f + 0.0000f },
						0
				)
		);
		controlMap.put(ControlKey.BA_YIELD.name(), baYieldMap);

		controlMap.put(ControlKey.BA_DQ_UPPER_BOUNDS.name(), Utils.constMap(map -> {
			map.put(76, new Coefficients(new float[] { 89.69f, 53.60f }, 1));
		}));

		app.init(resolver, controlMap);

		Optional<Float> ageTotal = Optional.of(200f);
		Optional<Float> yearsToBreastHeight = Optional.of(189f);
		Optional<Float> height = Optional.of(1f); // Set this low for test.
		Optional<Float> baseArea = Optional.of(30f);
		Optional<Float> treesPerHectare = Optional.of(300f);
		Optional<Float> percentForest = Optional.of(90f);

		Collection<VriSpecies> species = List.of(VriSpecies.build(builder -> {
			builder.polygonIdentifier("Test");
			builder.layerType(LayerType.PRIMARY);
			builder.genus("B");
			builder.percentGenus(100f);
		}));
		var bec = new BecDefinition("IDF", Region.INTERIOR, "Interior Douglas Fir");

		var result = app.findDefaultPolygonMode(
				ageTotal, yearsToBreastHeight, height, baseArea, treesPerHectare, percentForest, species, bec, Optional
						.of(76)
		);

		assertThat(result, is(PolygonMode.YOUNG));

		app.close();
	}

	@Test
	void testFindDefaultModeNoBA() throws Exception {
		var app = new VriStart();

		MockFileResolver resolver = dummyInput();

		controlMap.put(ControlKey.MINIMA.name(), Utils.constMap(map -> {
			map.put(VriControlParser.MINIMUM_BASE_AREA, 0f);
			map.put(VriControlParser.MINIMUM_HEIGHT, 6f);
			map.put(VriControlParser.MINIMUM_PREDICTED_BASE_AREA, 2f);
		}));

		var baYieldMap = new MatrixMap2Impl<>(
				List.of("IDF"), List.of("B"),
				(k1, k2) -> new Coefficients(
						new float[] { -2.9907f + 11.2376f, 2.4562f - 3.2149f, 7.2295f + 0.0000f, 0.5074f + 0.3763f,
								-0.0150f + 0.030f, 0.0120f + -0.0364f, -0.0090f + 0.0000f },
						0
				)
		);
		controlMap.put(ControlKey.BA_YIELD.name(), baYieldMap);

		controlMap.put(ControlKey.BA_DQ_UPPER_BOUNDS.name(), Utils.constMap(map -> {
			map.put(76, new Coefficients(new float[] { 89.69f, 53.60f }, 1));
		}));

		app.init(resolver, controlMap);

		Optional<Float> ageTotal = Optional.of(200f);
		Optional<Float> yearsToBreastHeight = Optional.of(189f);
		Optional<Float> height = Optional.of(20f);
		Optional<Float> baseArea = Optional.empty(); // Null this for the test
		Optional<Float> treesPerHectare = Optional.of(300f);
		Optional<Float> percentForest = Optional.of(90f);

		Collection<VriSpecies> species = List.of(VriSpecies.build(builder -> {
			builder.polygonIdentifier("Test");
			builder.layerType(LayerType.PRIMARY);
			builder.genus("B");
			builder.percentGenus(100f);
		}));
		var bec = new BecDefinition("IDF", Region.INTERIOR, "Interior Douglas Fir");

		var result = app.findDefaultPolygonMode(
				ageTotal, yearsToBreastHeight, height, baseArea, treesPerHectare, percentForest, species, bec, Optional
						.of(76)
		);

		assertThat(result, is(PolygonMode.YOUNG));

		app.close();
	}

	@Test
	void testFindDefaultModeNoTPH() throws Exception {
		var app = new VriStart();

		MockFileResolver resolver = dummyInput();

		controlMap.put(ControlKey.MINIMA.name(), Utils.constMap(map -> {
			map.put(VriControlParser.MINIMUM_BASE_AREA, 0f);
			map.put(VriControlParser.MINIMUM_HEIGHT, 6f);
			map.put(VriControlParser.MINIMUM_PREDICTED_BASE_AREA, 2f);
		}));

		var baYieldMap = new MatrixMap2Impl<>(
				List.of("IDF"), List.of("B"),
				(k1, k2) -> new Coefficients(
						new float[] { -2.9907f + 11.2376f, 2.4562f - 3.2149f, 7.2295f + 0.0000f, 0.5074f + 0.3763f,
								-0.0150f + 0.030f, 0.0120f + -0.0364f, -0.0090f + 0.0000f },
						0
				)
		);
		controlMap.put(ControlKey.BA_YIELD.name(), baYieldMap);

		controlMap.put(ControlKey.BA_DQ_UPPER_BOUNDS.name(), Utils.constMap(map -> {
			map.put(76, new Coefficients(new float[] { 89.69f, 53.60f }, 1));
		}));

		app.init(resolver, controlMap);

		Optional<Float> ageTotal = Optional.of(200f);
		Optional<Float> yearsToBreastHeight = Optional.of(189f);
		Optional<Float> height = Optional.of(20f);
		Optional<Float> baseArea = Optional.of(30f);
		Optional<Float> treesPerHectare = Optional.empty(); // Null this for the test
		Optional<Float> percentForest = Optional.of(90f);

		Collection<VriSpecies> species = List.of(VriSpecies.build(builder -> {
			builder.polygonIdentifier("Test");
			builder.layerType(LayerType.PRIMARY);
			builder.genus("B");
			builder.percentGenus(100f);
		}));
		var bec = new BecDefinition("IDF", Region.INTERIOR, "Interior Douglas Fir");

		var result = app.findDefaultPolygonMode(
				ageTotal, yearsToBreastHeight, height, baseArea, treesPerHectare, percentForest, species, bec, Optional
						.of(76)
		);

		assertThat(result, is(PolygonMode.YOUNG));

		app.close();
	}

	@Test
	void testFindDefaultLowRation() throws Exception {
		var app = new VriStart();

		MockFileResolver resolver = dummyInput();

		controlMap.put(ControlKey.MINIMA.name(), Utils.constMap(map -> {
			map.put(VriControlParser.MINIMUM_BASE_AREA, 13f);// Set this high
			map.put(VriControlParser.MINIMUM_HEIGHT, 0f);
			map.put(VriControlParser.MINIMUM_PREDICTED_BASE_AREA, 2f);
		}));

		var baYieldMap = new MatrixMap2Impl<>(
				List.of("IDF"), List.of("B"),
				(k1, k2) -> new Coefficients(
						new float[] { -2.9907f + 11.2376f, 2.4562f - 3.2149f, 7.2295f + 0.0000f, 0.5074f + 0.3763f,
								-0.0150f + 0.030f, 0.0120f + -0.0364f, -0.0090f + 0.0000f },
						0
				)
		);
		controlMap.put(ControlKey.BA_YIELD.name(), baYieldMap);

		controlMap.put(ControlKey.BA_DQ_UPPER_BOUNDS.name(), Utils.constMap(map -> {
			map.put(76, new Coefficients(new float[] { 89.69f, 53.60f }, 1));
		}));

		app.init(resolver, controlMap);

		Optional<Float> ageTotal = Optional.of(200f);
		Optional<Float> yearsToBreastHeight = Optional.of(189f);
		Optional<Float> height = Optional.of(20f);
		Optional<Float> baseArea = Optional.of(10f); // Set this low
		Optional<Float> treesPerHectare = Optional.of(300f);
		Optional<Float> percentForest = Optional.of(95f); // Set this high

		Collection<VriSpecies> species = List.of(VriSpecies.build(builder -> {
			builder.polygonIdentifier("Test");
			builder.layerType(LayerType.PRIMARY);
			builder.genus("B");
			builder.percentGenus(100f);
		}));
		var bec = new BecDefinition("IDF", Region.INTERIOR, "Interior Douglas Fir");

		var result = app.findDefaultPolygonMode(
				ageTotal, yearsToBreastHeight, height, baseArea, treesPerHectare, percentForest, species, bec, Optional
						.of(76)
		);

		assertThat(result, is(PolygonMode.YOUNG));

		app.close();
	}

	@Test
	void testFindDefaultStart() throws Exception {
		var app = new VriStart();

		MockFileResolver resolver = dummyInput();

		controlMap.put(ControlKey.MINIMA.name(), Utils.constMap(map -> {
			map.put(VriControlParser.MINIMUM_BASE_AREA, 0f);
			map.put(VriControlParser.MINIMUM_HEIGHT, 0f);
			map.put(VriControlParser.MINIMUM_PREDICTED_BASE_AREA, 2f);
		}));

		var baYieldMap = new MatrixMap2Impl<>(
				List.of("IDF"), List.of("B"),
				(k1, k2) -> new Coefficients(
						new float[] { -2.9907f + 11.2376f, 2.4562f - 3.2149f, 7.2295f + 0.0000f, 0.5074f + 0.3763f,
								-0.0150f + 0.030f, 0.0120f + -0.0364f, -0.0090f + 0.0000f },
						0
				)
		);
		controlMap.put(ControlKey.BA_YIELD.name(), baYieldMap);

		controlMap.put(ControlKey.BA_DQ_UPPER_BOUNDS.name(), Utils.constMap(map -> {
			map.put(76, new Coefficients(new float[] { 89.69f, 53.60f }, 1));
		}));

		app.init(resolver, controlMap);

		Optional<Float> ageTotal = Optional.of(200f);
		Optional<Float> yearsToBreastHeight = Optional.of(189f);
		Optional<Float> height = Optional.of(20f);
		Optional<Float> baseArea = Optional.of(30f);
		Optional<Float> treesPerHectare = Optional.of(300f);
		Optional<Float> percentForest = Optional.of(85f);

		Collection<VriSpecies> species = List.of(VriSpecies.build(builder -> {
			builder.polygonIdentifier("Test");
			builder.layerType(LayerType.PRIMARY);
			builder.genus("B");
			builder.percentGenus(100f);
		}));
		var bec = new BecDefinition("IDF", Region.INTERIOR, "Interior Douglas Fir");

		var result = app.findDefaultPolygonMode(
				ageTotal, yearsToBreastHeight, height, baseArea, treesPerHectare, percentForest, species, bec, Optional
						.of(76)
		);

		assertThat(result, is(PolygonMode.START));

		app.close();
	}

	@ParameterizedTest
	@EnumSource(value = PolygonMode.class, names = { "START", "YOUNG", "BATC", "BATN" })
	void testProcessPolygonDontSkip(PolygonMode mode) throws Exception {
		var control = EasyMock.createControl();

		VriStart app = EasyMock.createMockBuilder(VriStart.class).addMockedMethod("checkPolygon").createMock(control);

		MockFileResolver resolver = dummyInput();

		var poly = VriPolygon.build(pb -> {
			pb.polygonIdentifier("TestPoly");
			pb.biogeoclimaticZone("IDF");
			pb.yieldFactor(1.0f);
			pb.mode(mode);
		});

		app.checkPolygon(poly);
		EasyMock.expectLastCall().once();

		control.replay();

		app.init(resolver, controlMap);

		var result = app.processPolygon(0, poly);

		app.close();

		control.verify();
	}

	@ParameterizedTest
	@EnumSource(value = PolygonMode.class, mode = Mode.EXCLUDE, names = { "START", "YOUNG", "BATC", "BATN" })
	void testProcessPolygonDoSkip(PolygonMode mode) throws Exception {
		var control = EasyMock.createControl();

		VriStart app = EasyMock.createMockBuilder(VriStart.class).addMockedMethod("checkPolygon").createMock(control);

		MockFileResolver resolver = dummyInput();

		var poly = VriPolygon.build(pb -> {
			pb.polygonIdentifier("TestPoly");
			pb.biogeoclimaticZone("IDF");
			pb.yieldFactor(1.0f);
			pb.mode(mode);
		});

		// expect no calls

		control.replay();

		app.init(resolver, controlMap);

		var result = app.processPolygon(0, poly);

		assertThat(result, notPresent());

		app.close();

		control.verify();
	}

	<T> void mockInputStreamFactory(
			Map<String, Object> controlMap, ControlKey key, StreamingParser<T> stream, IMocksControl control
	) throws IOException {
		StreamingParserFactory<T> factory = control.createMock("factory_" + key.name(), StreamingParserFactory.class);

		EasyMock.expect(factory.get()).andReturn(stream);

		controlMap.put(key.name(), factory);
	}

	<T> StreamingParser<T>
			easyMockInputStreamFactory(Map<String, Object> controlMap, ControlKey key, IMocksControl control)
					throws IOException {
		StreamingParser<T> stream = control.createMock("stream_" + key.name(), StreamingParser.class);
		mockInputStreamFactory(controlMap, key, stream, control);
		return stream;
	}

	<T> MockStreamingParser<T>
			fillableMockInputStreamFactory(Map<String, Object> controlMap, ControlKey key, IMocksControl control)
					throws IOException {
		MockStreamingParser<T> stream = new MockStreamingParser<>();
		mockInputStreamFactory(controlMap, key, stream, control);
		return stream;
	}

	@Test
	void testProcessEmpty() throws Exception {
		var control = EasyMock.createControl();

		VriStart app = EasyMock.createMockBuilder(VriStart.class)//
				.addMockedMethod("getVriWriter") //
				.addMockedMethod("checkPolygon") //
				.addMockedMethod("getPolygon") //
				.createMock(control);

		MockFileResolver resolver = dummyInput();

		var poly = VriPolygon.build(pb -> {
			pb.polygonIdentifier("TestPoly");
			pb.biogeoclimaticZone("IDF");
			pb.yieldFactor(1.0f);
			pb.mode(PolygonMode.START);
		});

		StreamingParser<VriPolygon> polyStream = easyMockInputStreamFactory(
				controlMap, ControlKey.VRI_INPUT_YIELD_POLY, control
		);
		StreamingParser<Map<LayerType, VriLayer.Builder>> layerStream = easyMockInputStreamFactory(
				controlMap, ControlKey.VRI_INPUT_YIELD_LAYER, control
		);
		StreamingParser<Collection<VriSpecies>> specStream = easyMockInputStreamFactory(
				controlMap, ControlKey.VRI_INPUT_YIELD_SPEC_DIST, control
		);
		StreamingParser<Collection<VriSite>> siteStream = easyMockInputStreamFactory(
				controlMap, ControlKey.VRI_INPUT_YIELD_HEIGHT_AGE_SI, control
		);

		EasyMock.expect(polyStream.hasNext()).andReturn(false);

		// Expect no other calls

		polyStream.close();
		EasyMock.expectLastCall();
		layerStream.close();
		EasyMock.expectLastCall();
		specStream.close();
		EasyMock.expectLastCall();
		siteStream.close();
		EasyMock.expectLastCall();

		control.replay();

		app.init(resolver, controlMap);

		app.process();

		app.close();

		control.verify();
	}

	@Test
	void testProcessIgnored() throws Exception {
		var control = EasyMock.createControl();

		VriStart app = EasyMock.createMockBuilder(VriStart.class)//
				.addMockedMethod("getVriWriter") //
				.addMockedMethod("checkPolygon") //
				.addMockedMethod("getPolygon") //
				.createMock(control);

		MockFileResolver resolver = dummyInput();

		var poly = VriPolygon.build(pb -> {
			pb.polygonIdentifier("TestPoly");
			pb.biogeoclimaticZone("IDF");
			pb.yieldFactor(1.0f);
			pb.mode(PolygonMode.DONT_PROCESS);
		});

		StreamingParser<VriPolygon> polyStream = easyMockInputStreamFactory(
				controlMap, ControlKey.VRI_INPUT_YIELD_POLY, control
		);
		StreamingParser<Map<LayerType, VriLayer.Builder>> layerStream = easyMockInputStreamFactory(
				controlMap, ControlKey.VRI_INPUT_YIELD_LAYER, control
		);
		StreamingParser<Collection<VriSpecies>> specStream = easyMockInputStreamFactory(
				controlMap, ControlKey.VRI_INPUT_YIELD_SPEC_DIST, control
		);
		StreamingParser<Collection<VriSite>> siteStream = easyMockInputStreamFactory(
				controlMap, ControlKey.VRI_INPUT_YIELD_HEIGHT_AGE_SI, control
		);

		EasyMock.expect(polyStream.hasNext()).andReturn(true);
		EasyMock.expect(app.getPolygon(polyStream, layerStream, specStream, siteStream)).andReturn(poly);
		EasyMock.expect(polyStream.hasNext()).andReturn(false);

		// Expect no other calls

		polyStream.close();
		EasyMock.expectLastCall();
		layerStream.close();
		EasyMock.expectLastCall();
		specStream.close();
		EasyMock.expectLastCall();
		siteStream.close();
		EasyMock.expectLastCall();

		control.replay();

		app.init(resolver, controlMap);

		app.process();

		app.close();

		control.verify();
	}

}
