package ca.bc.gov.nrs.vdyp.vri;

import static ca.bc.gov.nrs.vdyp.test.TestUtils.*;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.*;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.closeTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.exception.NoBracketingException;
import org.apache.commons.math3.exception.TooManyEvaluationsException;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;
import org.junit.jupiter.params.provider.ValueSource;

import ca.bc.gov.nrs.vdyp.application.ApplicationTestUtils;
import ca.bc.gov.nrs.vdyp.application.VdypApplicationIdentifier;
import ca.bc.gov.nrs.vdyp.application.VdypStartApplication;
import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.common.Utils;
import ca.bc.gov.nrs.vdyp.common_calculators.enumerations.SiteIndexEquation;
import ca.bc.gov.nrs.vdyp.exceptions.FatalProcessingException;
import ca.bc.gov.nrs.vdyp.exceptions.ProcessingException;
import ca.bc.gov.nrs.vdyp.io.parse.coe.BasalAreaYieldParser;
import ca.bc.gov.nrs.vdyp.io.parse.coe.BaseAreaCoefficientParser;
import ca.bc.gov.nrs.vdyp.io.parse.coe.HLNonprimaryCoefficientParser;
import ca.bc.gov.nrs.vdyp.io.parse.coe.HLPrimarySpeciesEqnP1Parser;
import ca.bc.gov.nrs.vdyp.io.parse.coe.HLPrimarySpeciesEqnP2Parser;
import ca.bc.gov.nrs.vdyp.io.parse.coe.HLPrimarySpeciesEqnP3Parser;
import ca.bc.gov.nrs.vdyp.io.parse.coe.ModifierParser;
import ca.bc.gov.nrs.vdyp.io.parse.coe.QuadMeanDiameterCoefficientParser;
import ca.bc.gov.nrs.vdyp.io.parse.coe.QuadraticMeanDiameterYieldParser;
import ca.bc.gov.nrs.vdyp.io.parse.coe.SiteCurveParser;
import ca.bc.gov.nrs.vdyp.io.parse.coe.UpperBoundsParser;
import ca.bc.gov.nrs.vdyp.io.parse.coe.UpperCoefficientParser;
import ca.bc.gov.nrs.vdyp.io.parse.streaming.MockStreamingParser;
import ca.bc.gov.nrs.vdyp.io.parse.streaming.StreamingParser;
import ca.bc.gov.nrs.vdyp.io.parse.streaming.StreamingParserFactory;
import ca.bc.gov.nrs.vdyp.model.BecDefinition;
import ca.bc.gov.nrs.vdyp.model.BecLookup;
import ca.bc.gov.nrs.vdyp.model.Coefficients;
import ca.bc.gov.nrs.vdyp.model.DebugSettings;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2Impl;
import ca.bc.gov.nrs.vdyp.model.PolygonMode;
import ca.bc.gov.nrs.vdyp.model.Region;
import ca.bc.gov.nrs.vdyp.model.VdypLayer;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;
import ca.bc.gov.nrs.vdyp.model.VdypSpecies;
import ca.bc.gov.nrs.vdyp.test.MockFileResolver;
import ca.bc.gov.nrs.vdyp.test.TestUtils;
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
		controlMap.put(ControlKey.VDYP_OUTPUT_VDYP_POLYGON.name(), "DUMMY1");
		controlMap.put(ControlKey.VDYP_OUTPUT_VDYP_LAYER_BY_SPECIES.name(), "DUMMY2");
		controlMap.put(ControlKey.VDYP_OUTPUT_VDYP_LAYER_BY_SP0_BY_UTIL.name(), "DUMMY3");
		TestUtils.populateControlMapGenusReal(controlMap);

		MockFileResolver resolver = new MockFileResolver("Test");

		polyOut = new ByteArrayOutputStream();
		specOut = new ByteArrayOutputStream();
		utilOut = new ByteArrayOutputStream();

		resolver.addStream("DUMMY1", (OutputStream) polyOut);
		resolver.addStream("DUMMY2", (OutputStream) specOut);
		resolver.addStream("DUMMY3", (OutputStream) utilOut);
		return resolver;
	}

	@Nested
	class EstimateBaseAreaYield {
		@Test
		void testCompute() throws ProcessingException {
			Map<String, Object> controlMap = VriTestUtils.loadControlMap();
			VriStart app = new VriStart();
			ApplicationTestUtils.setControlMap(app, controlMap);

			var polygon = VriPolygon.build(pBuilder -> {
				pBuilder.polygonIdentifier("Test", 2024);
				pBuilder.biogeoclimaticZone(Utils.getBec("IDF", controlMap));
				pBuilder.yieldFactor(1.0f);
				pBuilder.addLayer(lBuilder -> {
					lBuilder.layerType(LayerType.PRIMARY);
					lBuilder.crownClosure(57.8f);
					lBuilder.baseArea(66f);
					lBuilder.treesPerHectare(850f);
					lBuilder.utilization(7.5f);
					lBuilder.empiricalRelationshipParameterIndex(76);

					lBuilder.addSpecies(sBuilder -> {
						sBuilder.genus("B", controlMap);
						sBuilder.percentGenus(2.99999993f);
					});
					lBuilder.addSpecies(sBuilder -> {
						sBuilder.genus("C", controlMap);
						sBuilder.percentGenus(30.0000012f);
					});
					lBuilder.addSpecies(sBuilder -> {
						sBuilder.genus("H", controlMap);
						sBuilder.percentGenus(48.9000022f);
					});
					lBuilder.addSpecies(sBuilder -> {
						sBuilder.genus("S", controlMap);
						sBuilder.percentGenus(18.1000009f);
					});
				});
			});

			var species = polygon.getLayers().get(LayerType.PRIMARY).getSpecies().values();

			var bec = Utils.expectParsedControl(controlMap, ControlKey.BEC_DEF, BecLookup.class).get("IDF").get();

			float result = app.estimateBaseAreaYield(32f, 190.300003f, Optional.empty(), false, species, bec, 76);

			assertThat(result, closeTo(62.0858421f));
		}

		@Test
		void testGetCoefficients() {
			Map<String, Object> controlMap = VriTestUtils.loadControlMap();
			VriStart app = new VriStart();
			ApplicationTestUtils.setControlMap(app, controlMap);

			var polygon = VriPolygon.build(pBuilder -> {
				pBuilder.polygonIdentifier("Test", 2024);
				pBuilder.biogeoclimaticZone(Utils.getBec("IDF", controlMap));
				pBuilder.yieldFactor(1.0f);
				pBuilder.addLayer(lBuilder -> {
					lBuilder.layerType(LayerType.PRIMARY);
					lBuilder.crownClosure(57.8f);
					lBuilder.baseArea(66f);
					lBuilder.treesPerHectare(850f);
					lBuilder.utilization(7.5f);
					lBuilder.empiricalRelationshipParameterIndex(76);

					lBuilder.addSpecies(sBuilder -> {
						sBuilder.genus("B", controlMap);
						sBuilder.percentGenus(2.99999993f);
					});
					lBuilder.addSpecies(sBuilder -> {
						sBuilder.genus("C", controlMap);
						sBuilder.percentGenus(30.0000012f);
					});
					lBuilder.addSpecies(sBuilder -> {
						sBuilder.genus("H", controlMap);
						sBuilder.percentGenus(48.9000022f);
					});
					lBuilder.addSpecies(sBuilder -> {
						sBuilder.genus("S", controlMap);
						sBuilder.percentGenus(18.1000009f);
					});
				});
			});

			var species = polygon.getLayers().get(LayerType.PRIMARY).getSpecies().values();

			var bec = Utils.expectParsedControl(controlMap, ControlKey.BEC_DEF, BecLookup.class).get("IDF").get();

			Coefficients result = app.estimateBaseAreaYieldCoefficients(species, bec);

			assertThat(
					result,
					VdypMatchers.coe(
							0, 7.29882717f, 0.934803009f, 7.22950029f, 0.478330702f, 0.00542420009f, 0f, -0.00899999961f
					)
			);
		}
	}

	@Nested
	class FindDefaultMode {

		@Test
		void testLowBA() throws Exception {
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
				builder.polygonIdentifier("Test", 2024);
				builder.layerType(LayerType.PRIMARY);
				builder.genus("B", controlMap);
				builder.percentGenus(100f);
			}));
			var bec = new BecDefinition("IDF", Region.INTERIOR, "Interior Douglas Fir");

			var result = app.findDefaultPolygonMode(
					ageTotal, yearsToBreastHeight, height, baseArea, treesPerHectare, percentForest, species, bec,
					Optional.of(76)
			);

			assertThat(result, is(PolygonMode.YOUNG));

			app.close();
		}

		@Test
		void testLowHeight() throws Exception {
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
				builder.polygonIdentifier("Test", 2024);
				builder.layerType(LayerType.PRIMARY);
				builder.genus("B", controlMap);
				builder.percentGenus(100f);
			}));
			var bec = new BecDefinition("IDF", Region.INTERIOR, "Interior Douglas Fir");

			var result = app.findDefaultPolygonMode(
					ageTotal, yearsToBreastHeight, height, baseArea, treesPerHectare, percentForest, species, bec,
					Optional.of(76)
			);

			assertThat(result, is(PolygonMode.YOUNG));

			app.close();
		}

		@Test
		void testNoBA() throws Exception {
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
				builder.polygonIdentifier("Test", 2024);
				builder.layerType(LayerType.PRIMARY);
				builder.genus("B", controlMap);
				builder.percentGenus(100f);
			}));
			var bec = new BecDefinition("IDF", Region.INTERIOR, "Interior Douglas Fir");

			var result = app.findDefaultPolygonMode(
					ageTotal, yearsToBreastHeight, height, baseArea, treesPerHectare, percentForest, species, bec,
					Optional.of(76)
			);

			assertThat(result, is(PolygonMode.YOUNG));

			app.close();
		}

		@Test
		void testNoTPH() throws Exception {
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
				builder.polygonIdentifier("Test", 2024);
				builder.layerType(LayerType.PRIMARY);
				builder.genus("B", controlMap);
				builder.percentGenus(100f);
			}));
			var bec = new BecDefinition("IDF", Region.INTERIOR, "Interior Douglas Fir");

			var result = app.findDefaultPolygonMode(
					ageTotal, yearsToBreastHeight, height, baseArea, treesPerHectare, percentForest, species, bec,
					Optional.of(76)
			);

			assertThat(result, is(PolygonMode.YOUNG));

			app.close();
		}

		@Test
		void testLowRation() throws Exception {
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
				builder.polygonIdentifier("Test", 2024);
				builder.layerType(LayerType.PRIMARY);
				builder.genus("B", controlMap);
				builder.percentGenus(100f);
			}));
			var bec = new BecDefinition("IDF", Region.INTERIOR, "Interior Douglas Fir");

			var result = app.findDefaultPolygonMode(
					ageTotal, yearsToBreastHeight, height, baseArea, treesPerHectare, percentForest, species, bec,
					Optional.of(76)
			);

			assertThat(result, is(PolygonMode.YOUNG));

			app.close();
		}

		@Test
		void testStart() throws Exception {
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
				builder.polygonIdentifier("Test", 2024);
				builder.layerType(LayerType.PRIMARY);
				builder.genus("B", controlMap);
				builder.percentGenus(100f);
			}));
			var bec = new BecDefinition("IDF", Region.INTERIOR, "Interior Douglas Fir");

			var result = app.findDefaultPolygonMode(
					ageTotal, yearsToBreastHeight, height, baseArea, treesPerHectare, percentForest, species, bec,
					Optional.of(76)
			);

			assertThat(result, is(PolygonMode.START));

			app.close();
		}
	}

	@Nested
	class QuadMeanDiameterRootFinding {
		@Nested
		class ErrorFunction {
			@Test
			void testCompute() {

				controlMap = VriTestUtils.loadControlMap();
				VriStart app = new VriStart();
				ApplicationTestUtils.setControlMap(app, controlMap);

				Map<String, Float> initialDqs = Utils.constMap(map -> {
					map.put("B", 12.0803461f);
					map.put("C", 8.66746521f);
					map.put("F", 11.8044939f);
					map.put("H", 9.06493855f);
					map.put("S", 10.4460621f);
				});
				Map<String, Float> baseAreas = Utils.constMap(map -> {
					map.put("B", 0.634290636f);
					map.put("C", 1.26858127f);
					map.put("F", 1.90287197f);
					map.put("H", 1.90287197f);
					map.put("S", 0.634290636f);

				});
				Map<String, Float> minDq = Utils.constMap(map -> {
					map.put("B", 7.6f);
					map.put("C", 7.6f);
					map.put("F", 7.6f);
					map.put("H", 7.6f);
					map.put("S", 7.6f);
				});
				Map<String, Float> maxDq = Utils.constMap(map -> {
					map.put("B", 13.8423338f);
					map.put("C", 16.6669998f);
					map.put("F", 15.5116472f);
					map.put("H", 12.5369997f);
					map.put("S", 12.6630001f);
				});

				float x = 0.161783934f;
				float tph = 748.402222f;

				var resultPerSpecies = new HashMap<String, Float>();

				float result = app
						.quadMeanDiameterFractionalError(x, resultPerSpecies, initialDqs, baseAreas, minDq, maxDq, tph);

				assertThat(result, closeTo(0.00525851687f));
				assertThat(
						resultPerSpecies, allOf(
								hasEntry(is("B"), closeTo(12.8846836f)), //
								hasEntry(is("C"), closeTo(8.87247944f)), //
								hasEntry(is("F"), closeTo(12.5603895f)), //
								hasEntry(is("H"), closeTo(9.33975124f)), //
								hasEntry(is("S"), closeTo(10.9634094f))
						)
				);
			}

			@Test
			void testComputeGraph() {

				controlMap = VriTestUtils.loadControlMap();
				VriStart app = new VriStart();
				ApplicationTestUtils.setControlMap(app, controlMap);

				Map<String, Float> initialDqs = Utils.constMap(map -> {
					map.put("B", 12.0803461f);
					map.put("C", 8.66746521f);
					map.put("F", 11.8044939f);
					map.put("H", 9.06493855f);
					map.put("S", 10.4460621f);
				});
				Map<String, Float> baseAreas = Utils.constMap(map -> {
					map.put("B", 0.634290636f);
					map.put("C", 1.26858127f);
					map.put("F", 1.90287197f);
					map.put("H", 1.90287197f);
					map.put("S", 0.634290636f);

				});
				Map<String, Float> minDq = Utils.constMap(map -> {
					map.put("B", 7.6f);
					map.put("C", 7.6f);
					map.put("F", 7.6f);
					map.put("H", 7.6f);
					map.put("S", 7.6f);
				});
				Map<String, Float> maxDq = Utils.constMap(map -> {
					map.put("B", 13.8423338f);
					map.put("C", 16.6669998f);
					map.put("F", 15.5116472f);
					map.put("H", 12.5369997f);
					map.put("S", 12.6630001f);
				});

				float tph = 748.402222f;

				var resultPerSpecies = new HashMap<String, Float>();

				for (float x = -3.9f; x <= 2f; x += 0.01f) {

					float result = app.quadMeanDiameterFractionalError(
							x, resultPerSpecies, initialDqs, baseAreas, minDq, maxDq, tph
					);

					System.out.println(String.format("%f\t%f", x, result));

				}
			}

			@Test
			void testComputeXClamppedHigh() {

				controlMap = VriTestUtils.loadControlMap();
				VriStart app = new VriStart();
				ApplicationTestUtils.setControlMap(app, controlMap);

				Map<String, Float> initialDqs = Utils.constMap(map -> {
					map.put("B", 12.0803461f);
					map.put("C", 8.66746521f);
					map.put("F", 11.8044939f);
					map.put("H", 9.06493855f);
					map.put("S", 10.4460621f);
				});
				Map<String, Float> baseAreas = Utils.constMap(map -> {
					map.put("B", 0.634290636f);
					map.put("C", 1.26858127f);
					map.put("F", 1.90287197f);
					map.put("H", 1.90287197f);
					map.put("S", 0.634290636f);

				});
				Map<String, Float> minDq = Utils.constMap(map -> {
					map.put("B", 7.6f);
					map.put("C", 7.6f);
					map.put("F", 7.6f);
					map.put("H", 7.6f);
					map.put("S", 7.6f);
				});
				Map<String, Float> maxDq = Utils.constMap(map -> {
					map.put("B", 13.8423338f);
					map.put("C", 16.6669998f);
					map.put("F", 15.5116472f);
					map.put("H", 12.5369997f);
					map.put("S", 12.6630001f);
				});

				float x = 12;
				float tph = 748.402222f;

				var resultPerSpecies = new HashMap<String, Float>();

				float result = app
						.quadMeanDiameterFractionalError(x, resultPerSpecies, initialDqs, baseAreas, minDq, maxDq, tph);

				assertThat(result, closeTo(-0.45818153f));
				assertThat(
						resultPerSpecies, allOf(
								hasEntry(is("B"), closeTo(13.8423338f)), //
								hasEntry(is("C"), closeTo(16.6669998f)), //
								hasEntry(is("F"), closeTo(15.5116472f)), //
								hasEntry(is("H"), closeTo(12.5369997f)), //
								hasEntry(is("S"), closeTo(12.6630001f))
						)
				);
			}

			@Test
			void testComputeXClamppedLow() {

				controlMap = VriTestUtils.loadControlMap();
				VriStart app = new VriStart();
				ApplicationTestUtils.setControlMap(app, controlMap);

				Map<String, Float> initialDqs = Utils.constMap(map -> {
					map.put("B", 12.0803461f);
					map.put("C", 8.66746521f);
					map.put("F", 11.8044939f);
					map.put("H", 9.06493855f);
					map.put("S", 10.4460621f);
				});
				Map<String, Float> baseAreas = Utils.constMap(map -> {
					map.put("B", 0.634290636f);
					map.put("C", 1.26858127f);
					map.put("F", 1.90287197f);
					map.put("H", 1.90287197f);
					map.put("S", 0.634290636f);

				});
				Map<String, Float> minDq = Utils.constMap(map -> {
					map.put("B", 7.6f);
					map.put("C", 7.6f);
					map.put("F", 7.6f);
					map.put("H", 7.6f);
					map.put("S", 7.6f);
				});
				Map<String, Float> maxDq = Utils.constMap(map -> {
					map.put("B", 13.8423338f);
					map.put("C", 16.6669998f);
					map.put("F", 15.5116472f);
					map.put("H", 12.5369997f);
					map.put("S", 12.6630001f);
				});

				float x = -12;
				float tph = 748.402222f;

				var resultPerSpecies = new HashMap<String, Float>();

				float result = app
						.quadMeanDiameterFractionalError(x, resultPerSpecies, initialDqs, baseAreas, minDq, maxDq, tph);

				assertThat(result, closeTo(0.868255138f));
				assertThat(
						resultPerSpecies, allOf(
								hasEntry(is("B"), closeTo(7.6f)), //
								hasEntry(is("C"), closeTo(7.6f)), //
								hasEntry(is("F"), closeTo(7.6f)), //
								hasEntry(is("H"), closeTo(7.6f)), //
								hasEntry(is("S"), closeTo(7.6f))
						)
				);
			}

			@Test
			void testComputeInitial() {

				controlMap = VriTestUtils.loadControlMap();
				VriStart app = new VriStart();
				ApplicationTestUtils.setControlMap(app, controlMap);

				Map<String, Float> initialDqs = Utils.constMap(map -> {
					map.put("B", 12.0803461f);
					map.put("C", 8.66746521f);
					map.put("F", 11.8044939f);
					map.put("H", 9.06493855f);
					map.put("S", 10.4460621f);
				});
				Map<String, Float> baseAreas = Utils.constMap(map -> {
					map.put("B", 0.634290636f);
					map.put("C", 1.26858127f);
					map.put("F", 1.90287197f);
					map.put("H", 1.90287197f);
					map.put("S", 0.634290636f);

				});
				Map<String, Float> minDq = Utils.constMap(map -> {
					map.put("B", 7.6f);
					map.put("C", 7.6f);
					map.put("F", 7.6f);
					map.put("H", 7.6f);
					map.put("S", 7.6f);
				});
				Map<String, Float> maxDq = Utils.constMap(map -> {
					map.put("B", 13.8423338f);
					map.put("C", 16.6669998f);
					map.put("F", 15.5116472f);
					map.put("H", 12.5369997f);
					map.put("S", 12.6630001f);
				});

				float x = -10;
				float tph = 748.402222f;

				var resultPerSpecies = new HashMap<String, Float>();

				float result = app
						.quadMeanDiameterFractionalError(x, resultPerSpecies, initialDqs, baseAreas, minDq, maxDq, tph);

				assertThat(result, closeTo(0.868255138f));
				assertThat(
						resultPerSpecies, allOf(
								hasEntry(is("B"), closeTo(7.6f)), //
								hasEntry(is("C"), closeTo(7.6f)), //
								hasEntry(is("F"), closeTo(7.6f)), //
								hasEntry(is("H"), closeTo(7.6f)), //
								hasEntry(is("S"), closeTo(7.6f))
						)
				);
			}

		}

		@Nested
		class ExpandIntervalOfRootFinder {
			@Test
			void testNoChange() {

				UnivariateFunction errorFunc = x -> x;

				var xInterval = new VriStart.Interval(-1, 1);

				VriStart app = new VriStart();

				var result = app.findInterval(xInterval, errorFunc);

				app.close();

				assertThat(result, equalTo(xInterval));

			}

			@Test
			void testSimpleChange() {

				UnivariateFunction errorFunc = x -> x;

				var xInterval = new VriStart.Interval(-2, -1);

				VriStart app = new VriStart();

				var result = app.findInterval(xInterval, errorFunc);

				app.close();

				var evaluated = result.evaluate(errorFunc);
				assertTrue(
						evaluated.start() * evaluated.end() <= 0,
						() -> "F(" + result + ") should have mixed signs but was " + evaluated
				);

			}

			@ParameterizedTest
			@CsvSource({ "1, 1", "-1, 1", "1, -1", "-1, -1" })
			void testDifficultChange(float a, float b) {

				UnivariateFunction errorFunc = x -> a * (Math.exp(b * x) - 0.000001);

				var xInterval = new VriStart.Interval(-1, 1);

				VriStart app = new VriStart();

				app.close();

				var result = app.findInterval(xInterval, errorFunc);

				var evaluated = result.evaluate(errorFunc);
				assertTrue(
						evaluated.start() * evaluated.end() <= 0,
						() -> "F(" + result + ") should have mixed signs but was " + evaluated
				);

			}

			@ParameterizedTest
			@ValueSource(floats = { 1, -1, 20, -20 })
			void testTwoRoots(float a) {

				UnivariateFunction errorFunc = x -> a * (x * x - 0.5);

				var xInterval = new VriStart.Interval(-1, 1);

				VriStart app = new VriStart();

				app.close();

				var result = app.findInterval(xInterval, errorFunc);

				var evaluated = result.evaluate(errorFunc);
				assertTrue(
						evaluated.start() * evaluated.end() <= 0,
						() -> "F(" + result + ") should have mixed signs but was " + evaluated
				);

			}

			@ParameterizedTest
			@CsvSource({ "1, 1", "-1, 1", "1, -1", "-1, -1" })
			void testImpossible(float a, float b) {

				UnivariateFunction errorFunc = x -> a * (Math.exp(b * x) + 1);

				var xInterval = new VriStart.Interval(-1, 1);

				VriStart app = new VriStart();

				app.close();

				assertThrows(NoBracketingException.class, () -> app.findInterval(xInterval, errorFunc));

			}

		}

		@Nested
		class FindRootOfErrorFunction {

			@Test
			void testSuccess() throws ProcessingException {
				controlMap = VriTestUtils.loadControlMap();
				VriStart app = new VriStart();
				ApplicationTestUtils.setControlMap(app, controlMap);

				Map<String, Float> initialDqs = Utils.constMap(map -> {
					map.put("B", 12.0803461f);
					map.put("C", 8.66746521f);
					map.put("F", 11.8044939f);
					map.put("H", 9.06493855f);
					map.put("S", 10.4460621f);
				});
				Map<String, Float> baseAreas = Utils.constMap(map -> {
					map.put("B", 0.634290636f);
					map.put("C", 1.26858127f);
					map.put("F", 1.90287197f);
					map.put("H", 1.90287197f);
					map.put("S", 0.634290636f);

				});
				Map<String, Float> minDq = Utils.constMap(map -> {
					map.put("B", 7.6f);
					map.put("C", 7.6f);
					map.put("F", 7.6f);
					map.put("H", 7.6f);
					map.put("S", 7.6f);
				});
				Map<String, Float> maxDq = Utils.constMap(map -> {
					map.put("B", 13.8423338f);
					map.put("C", 16.6669998f);
					map.put("F", 15.5116472f);
					map.put("H", 12.5369997f);
					map.put("S", 12.6630001f);
				});

				float x1 = -0.6f;
				float x2 = 0.5f;
				float tph = 748.402222f;

				var resultPerSpecies = new HashMap<String, Float>();

				float result = app.findRootForQuadMeanDiameterFractionalError(
						x1, x2, resultPerSpecies, initialDqs, baseAreas, minDq, maxDq, tph
				);

				assertThat(result, closeTo(0.172141284f));

				assertThat(
						resultPerSpecies, allOf(
								hasEntry(is("B"), closeTo(12.9407434f)), //
								hasEntry(is("C"), closeTo(8.88676834f)), //
								hasEntry(is("F"), closeTo(12.6130743f)), //
								hasEntry(is("H"), closeTo(9.35890579f)), //
								hasEntry(is("S"), closeTo(10.9994669f))
						)
				);

			}

			@Test
			void testNoIntervalThrow() {
				controlMap = VriTestUtils.loadControlMap();

				VriStart app = new VriStart() {

					@Override
					float quadMeanDiameterFractionalError(
							double x, Map<String, Float> finalDiameters, Map<String, Float> initial,
							Map<String, Float> baseArea, Map<String, Float> min, Map<String, Float> max,
							float totalTreeDensity
					) {
						// Force this to be something with no root. Finding a set of inputs that have no real root or
						// which the interval fixer can't handle would be better

						var f = Math.exp(x) + 1;

						initial.forEach((k, v) -> finalDiameters.put(k, (float) (v * x)));

						return (float) f;
					}

				};

				ApplicationTestUtils.setControlMap(app, controlMap);

				Map<String, Float> initialDqs = Utils.constMap(map -> {
					map.put("B", 12.0803461f);
					map.put("C", 8.66746521f);
					map.put("F", 11.8044939f);
					map.put("H", 9.06493855f);
					map.put("S", 10.4460621f);
				});
				Map<String, Float> baseAreas = Utils.constMap(map -> {
					map.put("B", 0.634290636f);
					map.put("C", 1.26858127f);
					map.put("F", 1.90287197f);
					map.put("H", 1.90287197f);
					map.put("S", 0.634290636f);

				});
				Map<String, Float> minDq = Utils.constMap(map -> {
					map.put("B", 7.6f);
					map.put("C", 7.6f);
					map.put("F", 7.6f);
					map.put("H", 7.6f);
					map.put("S", 7.6f);
				});
				Map<String, Float> maxDq = Utils.constMap(map -> {
					map.put("B", 13.8423338f);
					map.put("C", 16.6669998f);
					map.put("F", 15.5116472f);
					map.put("H", 12.5369997f);
					map.put("S", 12.6630001f);
				});

				float x1 = -0.6f;
				float x2 = 0.5f;

				float tph = 748.402222f;

				var resultPerSpecies = new HashMap<String, Float>();

				app.setDebugModes(TestUtils.debugSettingsSingle(1, 2));

				assertThrows(
						FatalProcessingException.class,
						() -> app.findRootForQuadMeanDiameterFractionalError(
								x1, x2, resultPerSpecies, initialDqs, baseAreas, minDq, maxDq, tph
						)
				);
			}

			@Test
			void testNoIntervalGuess() throws ProcessingException {
				controlMap = VriTestUtils.loadControlMap();

				VriStart app = new VriStart() {

					@Override
					float quadMeanDiameterFractionalError(
							double x, Map<String, Float> finalDiameters, Map<String, Float> initial,
							Map<String, Float> baseArea, Map<String, Float> min, Map<String, Float> max,
							float totalTreeDensity
					) {
						// Force this to be something with no root. Finding a set of inputs that have no real root or
						// which the interval fixer can't handle would be better

						var f = Math.exp(x) + 1;

						initial.forEach((k, v) -> finalDiameters.put(k, (float) (v * x)));

						return (float) f;
					}

				};

				ApplicationTestUtils.setControlMap(app, controlMap);

				Map<String, Float> initialDqs = Utils.constMap(map -> {
					map.put("B", 12.0803461f);
					map.put("C", 8.66746521f);
					map.put("F", 11.8044939f);
					map.put("H", 9.06493855f);
					map.put("S", 10.4460621f);
				});
				Map<String, Float> baseAreas = Utils.constMap(map -> {
					map.put("B", 0.634290636f);
					map.put("C", 1.26858127f);
					map.put("F", 1.90287197f);
					map.put("H", 1.90287197f);
					map.put("S", 0.634290636f);

				});
				Map<String, Float> minDq = Utils.constMap(map -> {
					map.put("B", 7.6f);
					map.put("C", 7.6f);
					map.put("F", 7.6f);
					map.put("H", 7.6f);
					map.put("S", 7.6f);
				});
				Map<String, Float> maxDq = Utils.constMap(map -> {
					map.put("B", 13.8423338f);
					map.put("C", 16.6669998f);
					map.put("F", 15.5116472f);
					map.put("H", 12.5369997f);
					map.put("S", 12.6630001f);
				});

				float x1 = -0.6f;
				float x2 = 0.5f;

				float tph = 748.402222f;

				var resultPerSpecies = new HashMap<String, Float>();

				app.setDebugModes(TestUtils.debugSettingsSingle(1, 0));

				var result = app.findRootForQuadMeanDiameterFractionalError(
						x1, x2, resultPerSpecies, initialDqs, baseAreas, minDq, maxDq, tph
				);

				assertThat(result, closeTo(-0.1f));

				// Complete nonsense numbers, but they test if the function is doing the right thing based on the
				// nonsense function used in the mock
				assertThat(
						resultPerSpecies, allOf(
								hasEntry(is("B"), closeTo(12.0803461f * (-0.1f))), //
								hasEntry(is("C"), closeTo(8.66746521f * (-0.1f))), //
								hasEntry(is("F"), closeTo(11.8044939f * (-0.1f))), //
								hasEntry(is("H"), closeTo(9.06493855f * (-0.1f))), //
								hasEntry(is("S"), closeTo(10.4460621f * (-0.1f)))
						)
				);

			}

			@Test
			void testTooManyEvaluationsStrictThrow() {
				controlMap = VriTestUtils.loadControlMap();

				float expectedX = 0.172142f;

				VriStart app = new VriStart() {

					@Override
					double doSolve(float min, float max, UnivariateFunction errorFunc) {
						errorFunc.value(0.1);
						errorFunc.value(expectedX);
						throw new TooManyEvaluationsException(100);
					}

				};

				ApplicationTestUtils.setControlMap(app, controlMap);

				Map<String, Float> initialDqs = Utils.constMap(map -> {
					map.put("B", 12.0803461f);
					map.put("C", 8.66746521f);
					map.put("F", 11.8044939f);
					map.put("H", 9.06493855f);
					map.put("S", 10.4460621f);
				});
				Map<String, Float> baseAreas = Utils.constMap(map -> {
					map.put("B", 0.634290636f);
					map.put("C", 1.26858127f);
					map.put("F", 1.90287197f);
					map.put("H", 1.90287197f);
					map.put("S", 0.634290636f);

				});
				Map<String, Float> minDq = Utils.constMap(map -> {
					map.put("B", 7.6f);
					map.put("C", 7.6f);
					map.put("F", 7.6f);
					map.put("H", 7.6f);
					map.put("S", 7.6f);
				});
				Map<String, Float> maxDq = Utils.constMap(map -> {
					map.put("B", 13.8423338f);
					map.put("C", 16.6669998f);
					map.put("F", 15.5116472f);
					map.put("H", 12.5369997f);
					map.put("S", 12.6630001f);
				});

				float x1 = -0.6f;
				float x2 = 0.5f;

				float tph = 748.402222f;

				var resultPerSpecies = new HashMap<String, Float>();

				app.setDebugModes(TestUtils.debugSettingsSingle(1, 2));

				assertThrows(
						FatalProcessingException.class,
						() -> app.findRootForQuadMeanDiameterFractionalError(
								x1, x2, resultPerSpecies, initialDqs, baseAreas, minDq, maxDq, tph
						)
				);
			}

			@Test
			void testTooManyEvaluationsGuess() throws ProcessingException {
				controlMap = VriTestUtils.loadControlMap();

				float expectedX = 0.172142f;

				VriStart app = new VriStart() {

					@Override
					double doSolve(float min, float max, UnivariateFunction errorFunc) {
						errorFunc.value(0.1);
						errorFunc.value(expectedX);
						throw new TooManyEvaluationsException(100);
					}

				};

				ApplicationTestUtils.setControlMap(app, controlMap);

				Map<String, Float> initialDqs = Utils.constMap(map -> {
					map.put("B", 12.0803461f);
					map.put("C", 8.66746521f);
					map.put("F", 11.8044939f);
					map.put("H", 9.06493855f);
					map.put("S", 10.4460621f);
				});
				Map<String, Float> baseAreas = Utils.constMap(map -> {
					map.put("B", 0.634290636f);
					map.put("C", 1.26858127f);
					map.put("F", 1.90287197f);
					map.put("H", 1.90287197f);
					map.put("S", 0.634290636f);

				});
				Map<String, Float> minDq = Utils.constMap(map -> {
					map.put("B", 7.6f);
					map.put("C", 7.6f);
					map.put("F", 7.6f);
					map.put("H", 7.6f);
					map.put("S", 7.6f);
				});
				Map<String, Float> maxDq = Utils.constMap(map -> {
					map.put("B", 13.8423338f);
					map.put("C", 16.6669998f);
					map.put("F", 15.5116472f);
					map.put("H", 12.5369997f);
					map.put("S", 12.6630001f);
				});

				float x1 = -0.6f;
				float x2 = 0.5f;

				float tph = 748.402222f;

				var resultPerSpecies = new HashMap<String, Float>();

				app.setDebugModes(TestUtils.debugSettingsSingle(1, 0));

				var result = app.findRootForQuadMeanDiameterFractionalError(
						x1, x2, resultPerSpecies, initialDqs, baseAreas, minDq, maxDq, tph
				);

				assertThat(result, closeTo((float) expectedX));
				assertThat(
						resultPerSpecies,
						allOf(
								appliedX("B", expectedX, app, initialDqs, minDq, maxDq),
								appliedX("C", expectedX, app, initialDqs, minDq, maxDq),
								appliedX("F", expectedX, app, initialDqs, minDq, maxDq),
								appliedX("H", expectedX, app, initialDqs, minDq, maxDq),
								appliedX("S", expectedX, app, initialDqs, minDq, maxDq)
						)
				);

			}

			@Test
			void testTooManyEvaluationsDiscontinuity() {
				controlMap = VriTestUtils.loadControlMap();

				float expectedX = -0.2f;

				VriStart app = new VriStart() {

					@Override
					double doSolve(float min, float max, UnivariateFunction errorFunc) {
						errorFunc.value(0.1);
						errorFunc.value(expectedX);
						throw new TooManyEvaluationsException(100);
					}

				};

				ApplicationTestUtils.setControlMap(app, controlMap);

				Map<String, Float> initialDqs = Utils.constMap(map -> {
					map.put("B", 12.0803461f);
					map.put("C", 8.66746521f);
					map.put("F", 11.8044939f);
					map.put("H", 9.06493855f);
					map.put("S", 10.4460621f);
				});
				Map<String, Float> baseAreas = Utils.constMap(map -> {
					map.put("B", 0.634290636f);
					map.put("C", 1.26858127f);
					map.put("F", 1.90287197f);
					map.put("H", 1.90287197f);
					map.put("S", 0.634290636f);

				});
				Map<String, Float> minDq = Utils.constMap(map -> {
					map.put("B", 7.6f);
					map.put("C", 7.6f);
					map.put("F", 7.6f);
					map.put("H", 7.6f);
					map.put("S", 7.6f);
				});
				Map<String, Float> maxDq = Utils.constMap(map -> {
					map.put("B", 13.8423338f);
					map.put("C", 16.6669998f);
					map.put("F", 15.5116472f);
					map.put("H", 12.5369997f);
					map.put("S", 12.6630001f);
				});

				float x1 = -0.6f;
				float x2 = 0.5f;

				float tph = 748.402222f;

				var resultPerSpecies = new HashMap<String, Float>();

				app.setDebugModes(TestUtils.debugSettingsSingle(1, 0));

				assertThrows(
						FatalProcessingException.class,
						() -> app.findRootForQuadMeanDiameterFractionalError(
								x1, x2, resultPerSpecies, initialDqs, baseAreas, minDq, maxDq, tph
						)
				);
			}

			static Matcher<Map<? extends String, ? extends Float>> appliedX(
					String species, float expectedX, VriStart app, Map<String, Float> initialDqs,
					Map<String, Float> minDq, Map<String, Float> maxDq
			) {
				return hasEntry(
						is(species),
						closeTo(
								app.quadMeanDiameterSpeciesAdjust(
										expectedX, initialDqs.get(species), minDq.get(species), maxDq.get(species)
								)
						)
				);
			}

		}

		@Nested
		class BestOf {

			static double func(double x) {
				return 7 * Math.sin(x / 7) + 2 * Math.sin(x / 2);
			}

			@ParameterizedTest
			@CsvSource(
				{ //
						"-1, 0, 1, 0", // Increasing middle
						"-23, -21, -19, -21", // Decreasing middle
						"4, 10, 20, 20", // Last from above
						"-9, -5, -1, -1", // Last from below
						"2, 8, 14, 2", // First from above
						"22, 30, 34, 22" // First from below
				}
			)
			void testThreePoints(double x1, double x2, double x3, double expect) {

				var result = VriStart.bestOf(BestOf::func, x1, x2, x3);

				assertThat(result, is(expect));
			}

			@ParameterizedTest
			@ValueSource(doubles = { 0, -1, 1, -23, -21, -19, -9, -5, 4, 10, 20, 2, 8, 14, 22, 30, 34 })
			void testOnePoint(double x) {

				var result = VriStart.bestOf(BestOf::func, x);

				assertThat(result, is(x));
			}

			@Test
			void noPoints() {

				assertThrows(IllegalArgumentException.class, () -> VriStart.bestOf(BestOf::func));
			}
		}

		@Nested
		class InitialMaps {
			@Test
			void testCompute() throws ProcessingException {

				controlMap = VriTestUtils.loadControlMap();
				VriStart app = new VriStart();
				ApplicationTestUtils.setControlMap(app, controlMap);

				VdypLayer layer = VdypLayer.build((lb) -> {
					lb.polygonIdentifier("Test", 2024);
					lb.layerType(LayerType.PRIMARY);
					lb.addSpecies(sb -> {
						sb.genus("B", controlMap);
						sb.percentGenus(10);
						sb.volumeGroup(15);
						sb.decayGroup(11);
						sb.breakageGroup(4);
						sb.loreyHeight(8.98269558f);
						sb.baseArea(0.634290636f);
					});
					lb.addSpecies(sb -> {
						sb.genus("C", controlMap);
						sb.percentGenus(20);
						sb.volumeGroup(23);
						sb.decayGroup(15);
						sb.breakageGroup(10);
						sb.loreyHeight(5.06450224f);
						sb.baseArea(1.26858127f);
					});
					lb.addSpecies(sb -> {
						sb.genus("F", controlMap);
						sb.percentGenus(30);
						sb.volumeGroup(33);
						sb.decayGroup(27);
						sb.breakageGroup(16);
						sb.loreyHeight(7.1979804f);
						sb.baseArea(1.90287197f);
					});
					lb.addSpecies(sb -> {
						sb.genus("H", controlMap);
						sb.percentGenus(30);
						sb.volumeGroup(40);
						sb.decayGroup(33);
						sb.breakageGroup(19);
						sb.loreyHeight(6.18095589f);
						sb.baseArea(1.90287197f);
					});
					lb.addSpecies(sb -> {
						sb.genus("S", controlMap);
						sb.percentGenus(10);
						sb.volumeGroup(69);
						sb.decayGroup(59);
						sb.breakageGroup(30);
						sb.loreyHeight(6.89051533f);
						sb.baseArea(0.634290636f);
					});
				});

				Region region = Region.INTERIOR;

				float quadMeanDiameterTotal = 10.3879938f;
				float baseAreaTotal = 6.34290648f;
				float treeDensityTotal = 748.402222f;
				float loreyHeightTotal = 6.61390257f;

				Map<String, Float> initialDqs = new HashMap<>(5);
				Map<String, Float> baseAreaPerSpecies = new HashMap<>(5);
				Map<String, Float> minPerSpecies = new HashMap<>(5);
				Map<String, Float> maxPerSpecies = new HashMap<>(5);

				app.getDqBySpeciesInitial(
						layer, region, quadMeanDiameterTotal, baseAreaTotal, treeDensityTotal, loreyHeightTotal,
						initialDqs, baseAreaPerSpecies, minPerSpecies, maxPerSpecies
				);

				assertThat(
						initialDqs, allOf(
								hasEntry(is("B"), closeTo(12.0803461f)), //
								hasEntry(is("C"), closeTo(8.66746521f)), //
								hasEntry(is("F"), closeTo(11.8044939f)), //
								hasEntry(is("H"), closeTo(9.06493855f)), //
								hasEntry(is("S"), closeTo(10.4460621f))
						)
				);
				assertThat(
						baseAreaPerSpecies, allOf(
								hasEntry(is("B"), closeTo(0.634290636f)), //
								hasEntry(is("C"), closeTo(1.26858127f)), //
								hasEntry(is("F"), closeTo(1.90287197f)), //
								hasEntry(is("H"), closeTo(1.90287197f)), //
								hasEntry(is("S"), closeTo(0.634290636f))
						)
				);
				assertThat(
						minPerSpecies, allOf(
								hasEntry(is("B"), closeTo(7.6f)), //
								hasEntry(is("C"), closeTo(7.6f)), //
								hasEntry(is("F"), closeTo(7.6f)), //
								hasEntry(is("H"), closeTo(7.6f)), //
								hasEntry(is("S"), closeTo(7.6f))
						)
				);
				assertThat(
						maxPerSpecies, allOf(
								hasEntry(is("B"), closeTo(13.8423338f)), //
								hasEntry(is("C"), closeTo(16.6669998f)), //
								hasEntry(is("F"), closeTo(15.5116472f)), //
								hasEntry(is("H"), closeTo(12.5369997f)), //
								hasEntry(is("S"), closeTo(12.6630001f))
						)
				);
			}
		}

		@Nested
		class ApplyResults {
			@Test
			void testApply() {

				controlMap = VriTestUtils.loadControlMap();
				VriStart app = new VriStart();
				ApplicationTestUtils.setControlMap(app, controlMap);

				VdypLayer layer = VdypLayer.build((lb) -> {
					lb.polygonIdentifier("Test", 2024);
					lb.layerType(LayerType.PRIMARY);
					lb.addSpecies(sb -> {
						sb.genus("B", controlMap);
						sb.percentGenus(10);
						sb.volumeGroup(15);
						sb.decayGroup(11);
						sb.breakageGroup(4);
						sb.loreyHeight(8.98269558f);
						sb.baseArea(0.634290636f);
					});
					lb.addSpecies(sb -> {
						sb.genus("C", controlMap);
						sb.percentGenus(20);
						sb.volumeGroup(23);
						sb.decayGroup(15);
						sb.breakageGroup(10);
						sb.loreyHeight(5.06450224f);
						sb.baseArea(1.26858127f);
					});
					lb.addSpecies(sb -> {
						sb.genus("F", controlMap);
						sb.percentGenus(30);
						sb.volumeGroup(33);
						sb.decayGroup(27);
						sb.breakageGroup(16);
						sb.loreyHeight(7.1979804f);
						sb.baseArea(1.90287197f);
					});
					lb.addSpecies(sb -> {
						sb.genus("H", controlMap);
						sb.percentGenus(30);
						sb.volumeGroup(40);
						sb.decayGroup(33);
						sb.breakageGroup(19);
						sb.loreyHeight(6.18095589f);
						sb.baseArea(1.90287197f);
					});
					lb.addSpecies(sb -> {
						sb.genus("S", controlMap);
						sb.percentGenus(10);
						sb.volumeGroup(69);
						sb.decayGroup(59);
						sb.breakageGroup(30);
						sb.loreyHeight(6.89051533f);
						sb.baseArea(0.634290636f);
					});
				});

				float baseAreaTotal = 6.34290648f;

				Map<String, Float> baseAreaPerSpecies = Utils.constMap(map -> {
					map.put("B", 0.634290636f);
					map.put("C", 1.26858127f);
					map.put("F", 1.90287197f);
					map.put("H", 1.90287197f);
					map.put("S", 0.634290636f);
				});

				Map<String, Float> diameterPerSpecies = Utils.constMap(map -> {
					map.put("B", 12.9407434f);
					map.put("C", 8.88676834f);
					map.put("F", 12.6130743f);
					map.put("H", 9.35890579f);
					map.put("S", 10.9994669f);
				});

				app.applyDqBySpecies(layer, baseAreaTotal, baseAreaPerSpecies, diameterPerSpecies);

				assertThat(layer.getQuadraticMeanDiameterByUtilization(), coe(-1, 0f, 10.3879948f, 0f, 0f, 0f, 0f));
				assertThat(layer.getTreesPerHectareByUtilization(), coe(-1, 0f, 748.4021f, 0f, 0f, 0f, 0f));

				var spec = layer.getSpecies().get("C");
				assertThat(spec.getQuadraticMeanDiameterByUtilization(), coe(-1, 0f, 8.88676834f, 0f, 0f, 0f, 0f));
				assertThat(spec.getTreesPerHectareByUtilization(), coe(-1, 0f, 204.522324f, 0f, 0f, 0f, 0f));

				// Total is correct and one of the individual species is correct. No need to check the other 4.

			}
		}

		@Nested
		class CompleteRun {
			@Test
			void testCompute() throws ProcessingException {

				controlMap = VriTestUtils.loadControlMap();
				VriStart app = new VriStart();
				ApplicationTestUtils.setControlMap(app, controlMap);

				VdypLayer layer = VdypLayer.build((lb) -> {
					lb.polygonIdentifier("Test", 2024);
					lb.layerType(LayerType.PRIMARY);
					lb.baseAreaByUtilization(6.34290648f);
					lb.treesPerHectareByUtilization(748.402222f);
					lb.quadraticMeanDiameterByUtilization(10.3879938f);
					lb.loreyHeightByUtilization(6.61390257f);
					lb.addSpecies(sb -> {
						sb.genus("B", controlMap);
						sb.percentGenus(10);
						sb.volumeGroup(15);
						sb.decayGroup(11);
						sb.breakageGroup(4);
						sb.loreyHeight(8.98269558f);
						sb.baseArea(0.634290636f);
					});
					lb.addSpecies(sb -> {
						sb.genus("C", controlMap);
						sb.percentGenus(20);
						sb.volumeGroup(23);
						sb.decayGroup(15);
						sb.breakageGroup(10);
						sb.loreyHeight(5.06450224f);
						sb.baseArea(1.26858127f);
					});
					lb.addSpecies(sb -> {
						sb.genus("F", controlMap);
						sb.percentGenus(30);
						sb.volumeGroup(33);
						sb.decayGroup(27);
						sb.breakageGroup(16);
						sb.loreyHeight(7.1979804f);
						sb.baseArea(1.90287197f);
					});
					lb.addSpecies(sb -> {
						sb.genus("H", controlMap);
						sb.percentGenus(30);
						sb.volumeGroup(40);
						sb.decayGroup(33);
						sb.breakageGroup(19);
						sb.loreyHeight(6.18095589f);
						sb.baseArea(1.90287197f);
					});
					lb.addSpecies(sb -> {
						sb.genus("S", controlMap);
						sb.percentGenus(10);
						sb.volumeGroup(69);
						sb.decayGroup(59);
						sb.breakageGroup(30);
						sb.loreyHeight(6.89051533f);
						sb.baseArea(0.634290636f);
					});
				});

				app.getDqBySpecies(layer, Region.INTERIOR);

				assertThat(
						layer.getQuadraticMeanDiameterByUtilization(), coe(-1, 0f, 10.3879948f, 0f, 0f, 0f, 10.3879948f)
				);
				assertThat(layer.getTreesPerHectareByUtilization(), coe(-1, 0f, 748.4021f, 0f, 0f, 0f, 748.4021f));

				var spec = layer.getSpecies().get("C");
				assertThat(spec.getQuadraticMeanDiameterByUtilization(), coe(-1, 0f, 8.88676834f, 0f, 0f, 0f, 0f));
				assertThat(spec.getTreesPerHectareByUtilization(), coe(-1, 0f, 204.522324f, 0f, 0f, 0f, 0f));

				// Total is correct and one of the individual species is correct. No need to check the other 4.

			}
		}
	}

	@Nested
	class Process {
		@ParameterizedTest
		@EnumSource(value = PolygonMode.class, names = { "START", "YOUNG", "BATC", "BATN" })
		void testDontSkip(PolygonMode mode) throws Exception {

			TestUtils.populateControlMapBecReal(controlMap);

			var control = EasyMock.createControl();

			VriStart app = EasyMock.createMockBuilder(VriStart.class) //
					.addMockedMethod("processYoung") //
					.addMockedMethod("processBatc") //
					.addMockedMethod("processBatn") //
					.addMockedMethod("checkPolygon") //
					.addMockedMethod("processPrimaryLayer") //
					.createMock(control);

			MockFileResolver resolver = dummyInput();

			var poly = VriPolygon.build(pb -> {
				pb.polygonIdentifier("TestPoly", 2024);
				pb.biogeoclimaticZone(Utils.getBec("IDF", controlMap));
				pb.yieldFactor(1.0f);
				pb.mode(mode);
				pb.addLayer(lb -> {
					lb.layerType(LayerType.PRIMARY);
					lb.crownClosure(80f);
					lb.utilization(0.6f);
				});
			});

			var polyYoung = VriPolygon.build(pb -> {
				pb.polygonIdentifier("TestPolyYoung", 2024);
				pb.biogeoclimaticZone(Utils.getBec("IDF", controlMap));
				pb.yieldFactor(1.0f);
				pb.mode(mode);
				pb.addLayer(lb -> {
					lb.layerType(LayerType.PRIMARY);
					lb.crownClosure(80f);
					lb.utilization(0.6f);
				});
			});
			var polyBatc = VriPolygon.build(pb -> {
				pb.polygonIdentifier("TestPolyBatc", 2024);
				pb.biogeoclimaticZone(Utils.getBec("IDF", controlMap));
				pb.yieldFactor(1.0f);
				pb.mode(mode);
				pb.addLayer(lb -> {
					lb.layerType(LayerType.PRIMARY);
					lb.crownClosure(80f);
					lb.utilization(0.6f);
				});
			});
			var polyBatn = VriPolygon.build(pb -> {
				pb.polygonIdentifier("TestPolyBatn", 2024);
				pb.biogeoclimaticZone(Utils.getBec("IDF", controlMap));
				pb.yieldFactor(1.0f);
				pb.mode(mode);
				pb.addLayer(lb -> {
					lb.layerType(LayerType.PRIMARY);
					lb.crownClosure(80f);
					lb.utilization(0.6f);
				});
			});

			EasyMock.expect(app.checkPolygon(poly)).andReturn(mode).once();
			EasyMock.expect(app.processYoung(poly)).andReturn(polyYoung).times(0, 1);
			EasyMock.expect(app.processBatc(poly)).andReturn(polyBatc).times(0, 1);
			EasyMock.expect(app.processBatn(poly)).andReturn(polyBatn).times(0, 1);
			EasyMock.expect(app.processBatn(polyYoung)).andReturn(polyBatn).times(0, 1);
			app.processPrimaryLayer(EasyMock.anyObject(VriPolygon.class), EasyMock.anyObject(VdypLayer.Builder.class));
			EasyMock.expectLastCall().once();

			// 1 and 9 set to 0
			controlMap.put(ControlKey.DEBUG_SWITCHES.name(), TestUtils.debugSettings(0, 0, 0, 0, 0, 0, 0, 0, 0));

			control.replay();

			app.init(resolver, controlMap);

			var result = app.processPolygon(0, poly);

			assertThat(result, notNullValue());

			app.close();

			control.verify();
		}

		@ParameterizedTest
		@EnumSource(value = PolygonMode.class, mode = Mode.EXCLUDE, names = { "START", "YOUNG", "BATC", "BATN" })
		void testDoSkip(PolygonMode mode) throws Exception {
			var control = EasyMock.createControl();

			VriStart app = EasyMock.createMockBuilder(VriStart.class).addMockedMethod("checkPolygon")
					.createMock(control);

			MockFileResolver resolver = dummyInput();

			// expect no calls
			TestUtils.populateControlMapBecReal(controlMap);

			var poly = VriPolygon.build(pb -> {
				pb.polygonIdentifier("TestPoly", 2024);
				pb.biogeoclimaticZone(Utils.getBec("IDF", controlMap));
				pb.yieldFactor(1.0f);
				pb.mode(mode);
			});

			control.replay();

			app.init(resolver, controlMap);

			var result = app.processPolygon(0, poly);

			assertThat(result, notPresent());

			app.close();

			control.verify();
		}

		@Test
		void testProcessEmpty() throws Exception {
			var control = EasyMock.createControl();

			VdypStartApplication<VriPolygon, VriLayer, VriSpecies, VriSite> app = EasyMock
					.createMockBuilder(VriStart.class)//
					.addMockedMethod("getVriWriter") //
					.addMockedMethod("checkPolygon") //
					.addMockedMethod("getPolygon") //
					.createMock(control);

			MockFileResolver resolver = dummyInput();

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
			TestUtils.populateControlMapBecReal(controlMap);

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
			TestUtils.populateControlMapBecReal(controlMap);

			var poly = VriPolygon.build(pb -> {
				pb.polygonIdentifier("TestPoly", 2024);
				pb.biogeoclimaticZone(Utils.getBec("IDF", controlMap));
				pb.yieldFactor(1.0f);
				pb.mode(PolygonMode.DONT_PROCESS);
			});

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

		@Test
		void testFatalExceptionProcessingPrimaryLayer() throws Exception {

			TestUtils.populateControlMapBecReal(controlMap);

			var control = EasyMock.createControl();

			var mode = PolygonMode.START;

			VriStart app = EasyMock.createMockBuilder(VriStart.class) //
					.addMockedMethod("processYoung") //
					.addMockedMethod("processBatc") //
					.addMockedMethod("processBatn") //
					.addMockedMethod("checkPolygon") //
					.addMockedMethod("processPrimaryLayer") //
					.createMock(control);

			MockFileResolver resolver = dummyInput();

			var poly = VriPolygon.build(pb -> {
				pb.polygonIdentifier("TestPoly", 2024);
				pb.biogeoclimaticZone(Utils.getBec("IDF", controlMap));
				pb.yieldFactor(1.0f);
				pb.mode(mode);
				pb.addLayer(lb -> {
					lb.layerType(LayerType.PRIMARY);
					lb.crownClosure(80f);
					lb.utilization(0.6f);
				});
			});

			EasyMock.expect(app.checkPolygon(poly)).andReturn(mode).once();
			app.processPrimaryLayer(EasyMock.same(poly), EasyMock.anyObject(VdypLayer.Builder.class));
			EasyMock.expectLastCall().andThrow(new FatalProcessingException("Test Exception")).once();

			control.replay();

			app.init(resolver, controlMap);

			assertThrows(FatalProcessingException.class, () -> app.processPolygon(0, poly));

			app.close();

			control.verify();
		}

		@Test
		void testProcessPrimary() throws Exception {

			controlMap = TestUtils.loadControlMap();

			VriStart app = new VriStart();

			MockFileResolver resolver = dummyInput();

			var poly = VriPolygon.build(pb -> {
				pb.polygonIdentifier("TestPoly", 2024);
				pb.biogeoclimaticZone(Utils.getBec("IDF", controlMap));
				pb.yieldFactor(1.0f);
				pb.mode(PolygonMode.BATN);
				pb.forestInventoryZone("");
				pb.percentAvailable(85);
				pb.addLayer(lb -> {
					lb.layerType(LayerType.PRIMARY);
					lb.crownClosure(30f);
					lb.utilization(7.5f);

					lb.inventoryTypeGroup(3);
					lb.empiricalRelationshipParameterIndex(61);

					lb.primaryGenus("F");
					// 1
					lb.addSpecies(sb -> {
						sb.genus("B", controlMap);
						sb.percentGenus(10);
						sb.addSp64Distribution("BL", 100);
						sb.addSite(ib -> {
							ib.siteSpecies("BL");
						});
					});

					// 2
					lb.addSpecies(sb -> {
						sb.genus("C", controlMap);
						sb.percentGenus(20);
						sb.addSp64Distribution("CW", 100);
						sb.addSite(ib -> {
							ib.siteCurveNumber(11);
							ib.siteSpecies("CW");
						});
					});

					// 3
					lb.addSpecies(sb -> {
						sb.genus("F", controlMap);
						sb.percentGenus(30);
						sb.addSp64Distribution("FD", 100);
						sb.addSite(ib -> {
							ib.siteCurveNumber(23);
							ib.ageTotal(24);
							ib.height(7.6f);
							ib.siteIndex(19.7f);
							ib.yearsToBreastHeight(9);
							ib.breastHeightAge(15);
							ib.siteSpecies("FD");
						});
					});

					// 4
					lb.addSpecies(sb -> {
						sb.genus("H", controlMap);
						sb.percentGenus(30);
						sb.addSp64Distribution("HW", 100);
						sb.addSite(ib -> {
							ib.siteCurveNumber(37);
							ib.siteSpecies("HW");
						});
					});

					// 5
					lb.addSpecies(sb -> {
						sb.genus("S", controlMap);
						sb.percentGenus(10);
						sb.addSp64Distribution("S", 100);
						sb.addSite(ib -> {
							ib.siteCurveNumber(71);
							ib.siteSpecies("S");
						});
					});
				});
			});

			app.init(resolver, controlMap);

			var result = app.processPolygon(0, poly).get();

			assertThat(result, hasProperty("polygonIdentifier", isPolyId("TestPoly", 2024)));
			assertThat(result, hasProperty("biogeoclimaticZone", hasProperty("alias", is("IDF"))));
			assertThat(result, hasProperty("forestInventoryZone", blankString()));
			assertThat(result, hasProperty("mode", present(is(PolygonMode.BATN))));
			assertThat(result, hasProperty("percentAvailable", is(85f)));

			var resultLayer = assertOnlyPrimaryLayer(result);

			assertThat(resultLayer, hasProperty("ageTotal", present(closeTo(24))));
			assertThat(resultLayer, hasProperty("breastHeightAge", present(closeTo(15))));
			assertThat(resultLayer, hasProperty("yearsToBreastHeight", present(closeTo(9))));

			assertThat(resultLayer, hasProperty("primaryGenus", present(is("F"))));

			assertThat(resultLayer, hasProperty("height", present(closeTo(7.6f))));
			assertThat(resultLayer, hasProperty("inventoryTypeGroup", present(is(3))));
			assertThat(resultLayer, hasProperty("empiricalRelationshipParameterIndex", present(is(61))));

			assertThat(
					resultLayer, hasProperty("loreyHeightByUtilization", utilizationHeight(4.14067888f, 6.61390257f))
			);
			assertThat(
					resultLayer,
					hasProperty(
							"baseAreaByUtilization",
							utilization(
									0.0679966733f, 6.34290648f, 4.24561071f, 1.01540196f, 0.571661115f, 0.510232806f
							)
					)
			);
			assertThat(
					resultLayer,
					hasProperty(
							"quadraticMeanDiameterByUtilization",
							utilization(5.58983135f, 10.3879948f, 9.11466217f, 13.9179964f, 18.6690178f, 25.3685265f)
					)
			);
			assertThat(
					resultLayer,
					hasProperty(
							"treesPerHectareByUtilization",
							utilization(27.707695f, 748.4021f, 650.682556f, 66.7413025f, 20.8836231f, 10.094574f)
					)
			);

			assertThat(
					resultLayer,
					hasProperty(
							"closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization",
							utilization(0, 4.73118162f, 0.0503439531f, 1.59589052f, 1.62338901f, 1.46155834f)
					)
			);
			assertThat(
					resultLayer.getSpecies(),
					allOf(aMapWithSize(5), hasKey("B"), hasKey("C"), hasKey("F"), hasKey("H"), hasKey("S"))
			);

			VdypSpecies resultSpecB = TestUtils.assertHasSpecies(resultLayer, "B", "C", "F", "H", "S");

			assertThat(
					resultSpecB,
					hasProperty(
							"baseAreaByUtilization",
							utilization(
									0.0116237309f, 0.634290636f, 0.239887208f, 0.196762085f, 0.102481194f, 0.095160149f
							)
					)
			);
			assertThat(
					resultSpecB,
					hasProperty(
							"quadraticMeanDiameterByUtilization",
							utilization(5.61674118f, 12.9407434f, 9.93954372f, 14.3500404f, 19.1790199f, 27.5482502f)
					)
			);
			assertThat(
					resultSpecB,
					hasProperty(
							"treesPerHectareByUtilization",
							utilization(4.69123125f, 48.2258606f, 30.9160728f, 12.1659298f, 3.54732919f, 1.59653044f)
					)
			);

			assertThat(
					resultSpecB,
					hasProperty(
							"wholeStemVolumeByUtilization",
							utilization(0.0244281366f, 2.41518188f, 0.747900844f, 0.752810001f, 0.4540295f, 0.46044156f)
					)
			);
			assertThat(
					resultSpecB,
					hasProperty(
							"closeUtilizationVolumeByUtilization",
							utilization(0, 1.28733742f, 0.0235678982f, 0.464995325f, 0.378819793f, 0.41995436f)
					)
			);
			assertThat(
					resultSpecB,
					hasProperty(
							"closeUtilizationVolumeNetOfDecayByUtilization",
							utilization(0, 1.24826729f, 0.0230324566f, 0.454239398f, 0.369579285f, 0.401416153f)
					)
			);
			assertThat(
					resultSpecB,
					hasProperty(
							"closeUtilizationVolumeNetOfDecayAndWasteByUtilization",
							utilization(0, 1.23482728f, 0.0228475146f, 0.450360179f, 0.366144955f, 0.395474672f)
					)
			);
			assertThat(
					resultSpecB,
					hasProperty(
							"closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization",
							utilization(0, 1.20897281f, 0.0223761573f, 0.441060275f, 0.358547896f, 0.386988521f)
					)
			);

			app.close();
		}

		@Test
		void testProcessVeteran() throws Exception {

			controlMap = TestUtils.loadControlMap();

			VriStart app = new VriStart();

			MockFileResolver resolver = dummyInput();

			var poly = VriPolygon.build(pb -> {
				pb.polygonIdentifier("TestPoly", 2024);
				pb.biogeoclimaticZone(Utils.getBec("IDF", controlMap));
				pb.yieldFactor(1.0f);
				pb.forestInventoryZone("");
				pb.percentAvailable(85);
				pb.addLayer(lb -> {
					lb.layerType(LayerType.PRIMARY);
					lb.crownClosure(40.2f);
					lb.utilization(7.5f);
					lb.baseArea(47.0588226f);
					lb.treesPerHectare(764.705872f);
					lb.utilization(7.5f);

					lb.inventoryTypeGroup(14);
					lb.empiricalRelationshipParameterIndex(33);

					lb.primaryGenus("C");
					// 1 3
					lb.addSpecies(sb -> {
						sb.genus("B", controlMap);
						sb.percentGenus(10);
						sb.addSp64Distribution("BL", 100);
						sb.addSite(ib -> {
							ib.siteSpecies("BL");
							ib.siteCurveNumber(8);
						});
					});

					// 2 4 (Primary)
					lb.addSpecies(sb -> {
						sb.genus("C", controlMap);
						sb.percentGenus(50);
						sb.addSp64Distribution("CW", 100);
						sb.addSite(ib -> {
							ib.siteCurveNumber(11);
							ib.ageTotal(100);
							ib.height(20f);
							ib.siteIndex(12f);
							ib.yearsToBreastHeight(10.9f);
							ib.breastHeightAge(89.1f);
							ib.ageTotal(100f);
							ib.siteSpecies("CW");
						});
					});

					// 3 8
					lb.addSpecies(sb -> {
						sb.genus("H", controlMap);
						sb.percentGenus(40);
						sb.addSp64Distribution("HW", 100);
						sb.addSite(ib -> {
							ib.siteCurveNumber(37);
							ib.height(25f);
							ib.siteIndex(12.6f);
							ib.yearsToBreastHeight(9.7f);
							ib.breastHeightAge(90.3f);
							ib.ageTotal(100f);
							ib.siteSpecies("HW");
						});
					});

				});
				pb.addLayer(lb -> {
					lb.layerType(LayerType.VETERAN);
					lb.crownClosure(50.8f);
					lb.utilization(7.5f);
					lb.baseArea(20f);
					lb.treesPerHectare(123f);
					lb.utilization(7.5f);

					lb.inventoryTypeGroup(14);
					// lb.empiricalRelationshipParameterIndex(33);

					lb.primaryGenus("H"); // 3
					// 1 3
					lb.addSpecies(sb -> {
						sb.genus("B", controlMap);
						sb.percentGenus(20);
						sb.addSp64Distribution("BL", 100);
						sb.addSite(ib -> {
							ib.siteSpecies("BL");
							ib.siteCurveNumber(8);
						});
					});

					// 2 4
					lb.addSpecies(sb -> {
						sb.genus("C", controlMap);
						sb.percentGenus(30);
						sb.addSp64Distribution("CW", 100);
						sb.addSite(ib -> {
							ib.siteCurveNumber(11);
							ib.ageTotal(100);
							ib.height(30f);
							ib.siteIndex(14.3f);
							ib.yearsToBreastHeight(10.9f);
							ib.breastHeightAge(189.1f);
							ib.ageTotal(200f);
							ib.siteSpecies("CW");
						});
					});

					// 3 8 (Primary)
					lb.addSpecies(sb -> {
						sb.genus("H", controlMap);
						sb.percentGenus(50);
						sb.addSp64Distribution("HW", 100);
						sb.addSite(ib -> {
							ib.siteCurveNumber(37);
							ib.height(34f);
							ib.siteIndex(14.6f);
							ib.yearsToBreastHeight(9.7f);
							ib.breastHeightAge(190.3f);
							ib.ageTotal(200f);
							ib.siteSpecies("HW");
						});
					});

				});
			});

			app.init(resolver, controlMap);

			var result = app.processPolygon(0, poly).get();

			assertThat(result, hasProperty("polygonIdentifier", isPolyId("TestPoly", 2024)));
			assertThat(result, hasProperty("biogeoclimaticZone", isBec("IDF")));
			assertThat(result, hasProperty("forestInventoryZone", blankString()));
			assertThat(result, hasProperty("mode", present(is(PolygonMode.START))));
			assertThat(result, hasProperty("percentAvailable", is(85f)));

			var primaryLayer = assertHasPrimaryLayer(result);

			assertThat(primaryLayer, hasProperty("ageTotal", present(closeTo(100))));
			assertThat(primaryLayer, hasProperty("breastHeightAge", present(closeTo(89.1f))));
			assertThat(primaryLayer, hasProperty("yearsToBreastHeight", present(closeTo(10.9f))));

			assertThat(primaryLayer, hasProperty("primaryGenus", present(is("C"))));

			assertThat(primaryLayer, hasProperty("height", present(closeTo(20f))));
			assertThat(primaryLayer, hasProperty("inventoryTypeGroup", present(is(14))));
			assertThat(primaryLayer, hasProperty("empiricalRelationshipParameterIndex", present(is(33))));

			assertThat(
					primaryLayer, hasProperty("loreyHeightByUtilization", utilizationHeight(5.45770216f, 21.0985336f))
			);
			assertThat(
					primaryLayer,
					hasProperty(
							"baseAreaByUtilization",
							utilization(0.0787888616f, 47.0588226f, 0.787343979f, 2.33701372f, 3.97268224f, 39.9617844f)
					)
			);
			assertThat(
					primaryLayer,
					hasProperty(
							"quadraticMeanDiameterByUtilization",
							utilization(5.89174175f, 27.9916744f, 9.26363468f, 14.1112642f, 18.8414402f, 37.8068199f)
					)
			);
			assertThat(
					primaryLayer,
					hasProperty(
							"treesPerHectareByUtilization",
							utilization(28.8993168f, 764.704102f, 116.818542f, 149.430603f, 142.483887f, 355.971069f)
					)
			);

			assertThat(
					primaryLayer,
					hasProperty(
							"closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization",
							utilization(0, 252.98407f, 0.0354338735f, 4.66429567f, 14.5271645f, 233.757172f)
					)
			);
			assertThat(primaryLayer.getSpecies(), allOf(aMapWithSize(3), hasKey("B"), hasKey("C"), hasKey("H")));

			var veteranLayer = assertHasVeteranLayer(result);

			VdypSpecies resultSpecB = TestUtils.assertHasSpecies(veteranLayer, "B", "C", "H");

			assertThat(resultSpecB, hasProperty("loreyHeightByUtilization", utilizationHeight(0f, 34f)));
			assertThat(resultSpecB, hasProperty("baseAreaByUtilization", utilizationAllAndBiggest(4f)));
			assertThat(
					resultSpecB,
					hasProperty("quadraticMeanDiameterByUtilization", utilizationAllAndBiggest(45.8757401f))
			);
			assertThat(resultSpecB, hasProperty("treesPerHectareByUtilization", utilizationAllAndBiggest(24.1993656f)));

			assertThat(resultSpecB, hasProperty("wholeStemVolumeByUtilization", utilizationAllAndBiggest(47.5739288f)));
			assertThat(
					resultSpecB,
					hasProperty("closeUtilizationVolumeByUtilization", utilizationAllAndBiggest(45.9957237f))
			);
			assertThat(
					resultSpecB,
					hasProperty("closeUtilizationVolumeNetOfDecayByUtilization", utilizationAllAndBiggest(39.5351295f))
			);
			assertThat(
					resultSpecB,
					hasProperty(
							"closeUtilizationVolumeNetOfDecayAndWasteByUtilization",
							utilizationAllAndBiggest(37.830616f)
					)
			);
			assertThat(
					resultSpecB,
					hasProperty(
							"closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization",
							utilizationAllAndBiggest(36.8912659f)
					)
			);

			assertThat(veteranLayer, hasProperty("ageTotal", present(closeTo(200))));
			assertThat(veteranLayer, hasProperty("breastHeightAge", present(closeTo(190.3f))));
			assertThat(veteranLayer, hasProperty("yearsToBreastHeight", present(closeTo(9.7f))));

			assertThat(veteranLayer, hasProperty("primaryGenus", present(is("H"))));

			assertThat(veteranLayer, hasProperty("height", present(closeTo(34f))));
			assertThat(veteranLayer, hasProperty("inventoryTypeGroup", present(is(14)))); // ?
			assertThat(veteranLayer, hasProperty("empiricalRelationshipParameterIndex", notPresent())); // ?

			assertThat(veteranLayer, hasProperty("loreyHeightByUtilization", utilizationHeight(0f, 32.8f)));
			assertThat(veteranLayer, hasProperty("baseAreaByUtilization", utilizationAllAndBiggest(20f)));
			assertThat(
					veteranLayer,
					hasProperty("quadraticMeanDiameterByUtilization", utilizationAllAndBiggest(45.5006409f))
			);
			assertThat(veteranLayer, hasProperty("treesPerHectareByUtilization", utilizationAllAndBiggest(123f)));

			assertThat(
					veteranLayer,
					hasProperty(
							"closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization",
							utilizationAllAndBiggest(167.61972f)
					)
			);

			app.close();
		}

		@Test
		void testProcessVeteranBadTphTotal() throws Exception {

			controlMap = TestUtils.loadControlMap();

			VriStart app = new VriStart();

			MockFileResolver resolver = dummyInput();

			var poly = VriPolygon.build(pb -> {
				pb.polygonIdentifier("TestPoly", 2024);
				pb.biogeoclimaticZone(Utils.getBec("IDF", controlMap));
				pb.yieldFactor(1.0f);
				pb.forestInventoryZone("");
				pb.percentAvailable(85);
				pb.addLayer(lb -> {
					lb.layerType(LayerType.PRIMARY);
					lb.crownClosure(40.2f);
					lb.utilization(7.5f);
					lb.baseArea(47.0588226f);
					lb.treesPerHectare(764.705872f);
					lb.utilization(7.5f);

					lb.inventoryTypeGroup(14);
					lb.empiricalRelationshipParameterIndex(33);

					lb.primaryGenus("C");
					// 1 3
					lb.addSpecies(sb -> {
						sb.genus("B", controlMap);
						sb.percentGenus(10);
						sb.addSp64Distribution("BL", 100);
						sb.addSite(ib -> {
							ib.siteSpecies("BL");
							ib.siteCurveNumber(8);
						});
					});

					// 2 4 (Primary)
					lb.addSpecies(sb -> {
						sb.genus("C", controlMap);
						sb.percentGenus(50);
						sb.addSp64Distribution("CW", 100);
						sb.addSite(ib -> {
							ib.siteCurveNumber(11);
							ib.ageTotal(100);
							ib.height(20f);
							ib.siteIndex(12f);
							ib.yearsToBreastHeight(10.9f);
							ib.breastHeightAge(89.1f);
							ib.ageTotal(100f);
							ib.siteSpecies("CW");
						});
					});

					// 3 8
					lb.addSpecies(sb -> {
						sb.genus("H", controlMap);
						sb.percentGenus(40);
						sb.addSp64Distribution("HW", 100);
						sb.addSite(ib -> {
							ib.siteCurveNumber(37);
							ib.height(25f);
							ib.siteIndex(12.6f);
							ib.yearsToBreastHeight(9.7f);
							ib.breastHeightAge(90.3f);
							ib.ageTotal(100f);
							ib.siteSpecies("HW");
						});
					});

				});
				pb.addLayer(lb -> {
					lb.layerType(LayerType.VETERAN);
					lb.crownClosure(50.8f);
					lb.utilization(7.5f);
					lb.baseArea(20f);
					lb.treesPerHectare(123f);
					lb.utilization(7.5f);

					lb.inventoryTypeGroup(14);
					// lb.empiricalRelationshipParameterIndex(33);

					lb.primaryGenus("H"); // 3
					// 1 3
					lb.addSpecies(sb -> {
						sb.genus("B", controlMap);
						sb.percentGenus(20);
						sb.addSp64Distribution("BL", 100);
						sb.addSite(ib -> {
							ib.siteSpecies("BL");
							ib.siteCurveNumber(8);
						});
					});

					// 2 4
					lb.addSpecies(sb -> {
						sb.genus("C", controlMap);
						sb.percentGenus(30);
						sb.addSp64Distribution("CW", 100);
						sb.addSite(ib -> {
							ib.siteCurveNumber(11);
							ib.ageTotal(100);
							ib.height(30f);
							ib.siteIndex(14.3f);
							ib.yearsToBreastHeight(10.9f);
							ib.breastHeightAge(189.1f);
							ib.ageTotal(200f);
							ib.siteSpecies("CW");
						});
					});

					// 3 8 (Primary)
					lb.addSpecies(sb -> {
						sb.genus("H", controlMap);
						sb.percentGenus(50);
						sb.addSp64Distribution("HW", 100);
						sb.addSite(ib -> {
							ib.siteCurveNumber(37);
							ib.height(34f);
							ib.siteIndex(14.6f);
							ib.yearsToBreastHeight(9.7f);
							ib.breastHeightAge(190.3f);
							ib.ageTotal(200f);
							ib.siteSpecies("HW");
						});
					});

				});
			});
			// doesn't need to be a completely accurate mock for this test
			var result = VdypPolygon.build(pb -> {
				pb.adapt(poly, x -> x.get());
				pb.adaptLayers(poly, (l, lb) -> {

				});
			});
			app.init(resolver, controlMap);

			// inputTph set to something utterly wrong should cause an error
			var ex = assertThrows(FatalProcessingException.class, () -> app.postProcessPolygon(poly, 10000f, result));
			assertThat(ex, hasProperty("message", containsString("10,000 trees/ha")));

			app.close();
		}

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

	@Nested
	class SiteCurveNumber {
		@Test
		void testFindSiteCurveNumber() throws Exception {
			var control = EasyMock.createControl();

			VriStart app = new VriStart();

			MockFileResolver resolver = dummyInput();

			TestUtils.populateControlMapGenusReal(controlMap);
			TestUtils.populateControlMapFromResource(controlMap, new SiteCurveParser(), "SIEQN.PRM");

			control.replay();

			app.init(resolver, controlMap);

			assertThat(app.findSiteCurveNumber(Region.COASTAL, "MB"), is(SiteIndexEquation.getByIndex(10)));
			assertThat(app.findSiteCurveNumber(Region.INTERIOR, "MB"), is(SiteIndexEquation.getByIndex(10)));

			assertThat(app.findSiteCurveNumber(Region.COASTAL, "B"), is(SiteIndexEquation.getByIndex(12)));
			assertThat(app.findSiteCurveNumber(Region.INTERIOR, "B"), is(SiteIndexEquation.getByIndex(42)));

			assertThat(app.findSiteCurveNumber(Region.COASTAL, "ZZZ", "B"), is(SiteIndexEquation.getByIndex(12)));
			assertThat(app.findSiteCurveNumber(Region.INTERIOR, "ZZZ", "B"), is(SiteIndexEquation.getByIndex(42)));

			assertThat(app.findSiteCurveNumber(Region.COASTAL, "YYY", "B"), is(SiteIndexEquation.getByIndex(42)));
			assertThat(app.findSiteCurveNumber(Region.INTERIOR, "YYY", "B"), is(SiteIndexEquation.getByIndex(06)));

			assertThrows(FatalProcessingException.class, () -> app.findSiteCurveNumber(Region.COASTAL, "ZZZ"));
			assertThrows(FatalProcessingException.class, () -> app.findSiteCurveNumber(Region.INTERIOR, "ZZZ"));

			app.close();

			control.verify();
		}

		@Test
		void testGetSiteCurveNumberByFinding() throws Exception {
			var control = EasyMock.createControl();

			VriStart app = new VriStart();

			MockFileResolver resolver = dummyInput();

			TestUtils.populateControlMapGenusReal(controlMap);
			TestUtils.populateControlMapBecReal(controlMap);
			TestUtils.populateControlMapFromResource(controlMap, new SiteCurveParser(), "SIEQN.PRM");
			var bec = Utils.getBec("CDF", controlMap);

			control.replay();

			app.init(resolver, controlMap);

			var site = VriSite.build(builder -> {
				builder.polygonIdentifier("Test", 2025);
				builder.layerType(LayerType.PRIMARY);
				builder.siteSpecies("PL");
				builder.siteGenus("PL");
			});

			var result = app.getSiteCurveNumber(bec, site);

			assertThat(result, is(SiteIndexEquation.SI_BL_KURUCZ82));

			app.close();

			control.verify();
		}

		@Test
		void testGetSiteCurveNumberDirectly() throws Exception {
			var control = EasyMock.createControl();

			VriStart app = new VriStart();

			MockFileResolver resolver = dummyInput();

			TestUtils.populateControlMapGenusReal(controlMap);
			TestUtils.populateControlMapBecReal(controlMap);
			TestUtils.populateControlMapFromResource(controlMap, new SiteCurveParser(), "SIEQN.PRM");
			var bec = Utils.getBec("CDF", controlMap);

			control.replay();

			app.init(resolver, controlMap);

			var site = VriSite.build(builder -> {
				builder.polygonIdentifier("Test", 2025);
				builder.layerType(LayerType.PRIMARY);
				builder.siteSpecies("PL");
				builder.siteGenus("PL");
				builder.siteCurveNumber(58);
			});

			var result = app.getSiteCurveNumber(bec, site);

			assertThat(result, is(SiteIndexEquation.SI_SS_NIGHGI));

			app.close();

			control.verify();
		}

		@Test
		void testGetSiteCurveNumberFail() throws Exception {
			var control = EasyMock.createControl();

			VriStart app = new VriStart();

			MockFileResolver resolver = dummyInput();

			TestUtils.populateControlMapGenusReal(controlMap);
			TestUtils.populateControlMapBecReal(controlMap);
			TestUtils.populateControlMapFromResource(controlMap, new SiteCurveParser(), "SIEQN.PRM");
			var bec = Utils.getBec("CDF", controlMap);

			control.replay();

			app.init(resolver, controlMap);

			var site = VriSite.build(builder -> {
				builder.polygonIdentifier("Test", 2025);
				builder.layerType(LayerType.PRIMARY);
				builder.siteSpecies("X");
				builder.siteGenus("X");
			});

			var ex = assertThrows(FatalProcessingException.class, () -> app.getSiteCurveNumber(bec, site));
			assertThat(ex, hasProperty("message", containsString("Could not find Site Curve Number")));

			app.close();

			control.verify();
		}
	}

	@Nested
	class ProcessYoung {

		@Test
		void testBasic() throws Exception {
			var control = EasyMock.createControl();

			VriStart app = EasyMock.createMockBuilder(VriStart.class).createMock(control);

			MockFileResolver resolver = dummyInput();

			TestUtils.populateControlMapGenusReal(controlMap);
			TestUtils.populateControlMapBecReal(controlMap);
			controlMap.put(ControlKey.MINIMA.name(), Utils.constMap(map -> {
				map.put(VriControlParser.MINIMUM_BASE_AREA, 0f);
				map.put(VriControlParser.MINIMUM_HEIGHT, 6f);
				map.put(VriControlParser.MINIMUM_PREDICTED_BASE_AREA, 2f);
			}));
			TestUtils.populateControlMapFromResource(controlMap, new BasalAreaYieldParser(), "YLDBA407.COE");
			TestUtils.populateControlMapFromResource(controlMap, new UpperBoundsParser(), "PCT_407.coe");

			var poly = VriPolygon.build(pb -> {
				pb.polygonIdentifier("082F074/0142", 1997);
				pb.forestInventoryZone(" ");
				pb.biogeoclimaticZone(Utils.getBec("IDF", controlMap));
				pb.yieldFactor(1.0f);
				pb.mode(PolygonMode.YOUNG);

				pb.addLayer(lb -> {
					lb.layerType(LayerType.PRIMARY);
					lb.baseArea(Optional.empty());
					lb.treesPerHectare(Optional.empty());
					lb.primaryGenus("F");
					lb.inventoryTypeGroup(3);
					lb.crownClosure(30);
					lb.utilization(7.5f);

					lb.empiricalRelationshipParameterIndex(61);

					lb.addSpecies(spb -> {
						spb.genus("B", controlMap);
						spb.percentGenus(10);
						spb.addSp64Distribution("BL", 100);
						spb.addSite(sib -> {
							sib.siteSpecies("BL");
						});
					});
					lb.addSpecies(spb -> {
						spb.genus("C", controlMap);
						spb.percentGenus(20);
						spb.addSp64Distribution("CW", 100);
						spb.addSite(sib -> {
							sib.siteCurveNumber(11);
							sib.siteSpecies("CW");
						});
					});
					lb.addSpecies(spb -> {
						spb.genus("F", controlMap);
						spb.percentGenus(30);
						spb.addSp64Distribution("FD", 100);
						spb.addSite(sib -> {
							sib.siteSpecies("FD");
							sib.siteCurveNumber(23);
							sib.siteIndex(19.7f);
							sib.height(7.6f);
							sib.yearsToBreastHeight(9);
							sib.breastHeightAge(15);
							sib.ageTotal(24);
						});
					});
					lb.addSpecies(spb -> {
						spb.genus("H", controlMap);
						spb.percentGenus(30);
						spb.addSp64Distribution("HW", 100);
						spb.addSite(sib -> {
							sib.siteSpecies("HW");
							sib.siteCurveNumber(37);
						});
					});
					lb.addSpecies(spb -> {
						spb.genus("S", controlMap);
						spb.percentGenus(10);
						spb.addSp64Distribution("S", 100);
						spb.addSite(sib -> {
							sib.siteSpecies("S");
							sib.siteCurveNumber(71);
						});
					});

				});
			});

			control.replay();

			app.init(resolver, controlMap);

			// Run the process

			var result = assertDoesNotThrow(() -> app.processYoung(poly));

			// Assertions

			final var forPolygon = hasProperty("polygonIdentifier", isPolyId("082F074/0142", 1997));
			final var forPrimeLayer = both(forPolygon).and(hasProperty("layerType", is(LayerType.PRIMARY)));

			assertThat(result, forPolygon);
			assertThat(result, hasProperty("mode", present(is(PolygonMode.YOUNG))));

			assertThat(
					result, hasProperty("layers", allOf(aMapWithSize(1), hasEntry(is(LayerType.PRIMARY), anything())))
			);
			var resultPrimaryLayer = result.getLayers().get(LayerType.PRIMARY);

			assertThat(resultPrimaryLayer, forPrimeLayer);

			assertThat(
					resultPrimaryLayer, hasProperty(
							"sites", allOf(
									aMapWithSize(5), //
									hasSite(
											is("B"), is("BL"),
											forPrimeLayer.and(hasProperty("siteCurveNumber", notPresent()))
									), //
									hasSite(
											is("C"), is("CW"),
											forPrimeLayer.and(hasProperty("siteCurveNumber", present(is(11))))
									), //
									hasSite(
											is("F"), is("FD"),
											forPrimeLayer.and(hasProperty("siteCurveNumber", present(is(23))))
									), //
									hasSite(
											is("H"), is("HW"),
											forPrimeLayer.and(hasProperty("siteCurveNumber", present(is(37))))
									), //
									hasSite(
											is("S"), is("S"),
											forPrimeLayer.and(hasProperty("siteCurveNumber", present(is(71))))
									)
							)
					)
			);

			assertThat(
					resultPrimaryLayer, hasProperty(
							"species", allOf(
									aMapWithSize(5), //
									hasSpecies(is("B"), is("BL"), closeTo(10), forPrimeLayer), //
									hasSpecies(is("C"), is("CW"), closeTo(20), forPrimeLayer), //
									hasSpecies(is("F"), is("FD"), closeTo(30), forPrimeLayer), //
									hasSpecies(is("H"), is("HW"), closeTo(30), forPrimeLayer), //
									hasSpecies(is("S"), is("S"), closeTo(10), forPrimeLayer)
							)
					)
			);

			for (var nonPrimaryGenus : List.of("B", "C", "H", "S")) {
				var nonPrimarySite = resultPrimaryLayer.getSites().get(nonPrimaryGenus);
				assertThat(
						nonPrimarySite, allOf(
								hasProperty("siteIndex", notPresent()), //
								hasProperty("height", notPresent()), //
								hasProperty("ageTotal", notPresent()), //
								hasProperty("yearsToBreastHeight", notPresent()), //
								hasProperty("breastHeightAge", notPresent())
						)
				);
			}

			var primarySite = resultPrimaryLayer.getPrimarySite().get();

			assertThat(
					primarySite, allOf(
							hasProperty("siteGenus", is("F")), //
							hasProperty("siteIndex", present(closeTo(19.7f))), //
							hasProperty("height", present(closeTo(7.6f))), //
							hasProperty("ageTotal", present(closeTo(24f))), //
							hasProperty("yearsToBreastHeight", present(closeTo(9f))), //
							hasProperty("breastHeightAge", present(closeTo(15f)))
					)
			);

			app.close();

			control.verify();
		}

		@Test
		void testLowPercentAvailable() throws Exception {
			var control = EasyMock.createControl();

			VriStart app = EasyMock.createMockBuilder(VriStart.class).createMock(control);

			MockFileResolver resolver = dummyInput();

			TestUtils.populateControlMapGenusReal(controlMap);
			TestUtils.populateControlMapBecReal(controlMap);
			controlMap.put(ControlKey.MINIMA.name(), Utils.constMap(map -> {
				map.put(VriControlParser.MINIMUM_BASE_AREA, 0f);
				map.put(VriControlParser.MINIMUM_HEIGHT, 6f);
				map.put(VriControlParser.MINIMUM_PREDICTED_BASE_AREA, 10f); // Set this high
			}));
			TestUtils.populateControlMapFromResource(controlMap, new BasalAreaYieldParser(), "YLDBA407.COE");
			TestUtils.populateControlMapFromResource(controlMap, new UpperBoundsParser(), "PCT_407.coe");

			// Target BA should be 11.11111111 due to low PCTFLAND

			var poly = VriPolygon.build(pb -> {
				pb.polygonIdentifier("082F074/0142", 1997);
				pb.forestInventoryZone(" ");
				pb.biogeoclimaticZone(Utils.getBec("IDF", controlMap));
				pb.yieldFactor(1.0f);
				pb.mode(PolygonMode.YOUNG);

				pb.percentAvailable(9); // Set this less than 10

				pb.addLayer(lb -> {
					lb.layerType(LayerType.PRIMARY);
					lb.baseArea(Optional.empty());
					lb.treesPerHectare(Optional.empty());
					lb.primaryGenus("F");
					lb.inventoryTypeGroup(3);
					lb.crownClosure(30);
					lb.utilization(7.5f);

					lb.empiricalRelationshipParameterIndex(61);

					lb.addSpecies(spb -> {
						spb.genus("B", controlMap);
						spb.percentGenus(10);
						spb.addSp64Distribution("BL", 100);
						spb.addSite(sib -> {
							sib.siteSpecies("BL");
						});
					});
					lb.addSpecies(spb -> {
						spb.genus("C", controlMap);
						spb.percentGenus(20);
						spb.addSp64Distribution("CW", 100);
						spb.addSite(sib -> {
							sib.siteCurveNumber(11);
							sib.siteSpecies("CW");
						});
					});
					lb.addSpecies(spb -> {
						spb.genus("F", controlMap);
						spb.percentGenus(30);
						spb.addSp64Distribution("FD", 100);
						spb.addSite(sib -> {
							sib.siteSpecies("FD");
							sib.siteCurveNumber(23);
							sib.siteIndex(19.7f);
							sib.height(7.6f);
							sib.yearsToBreastHeight(9);
							sib.breastHeightAge(15);
							sib.ageTotal(24);
						});
					});
					lb.addSpecies(spb -> {
						spb.genus("H", controlMap);
						spb.percentGenus(30);
						spb.addSp64Distribution("HW", 100);
						spb.addSite(sib -> {
							sib.siteSpecies("HW");
							sib.siteCurveNumber(37);
						});
					});
					lb.addSpecies(spb -> {
						spb.genus("S", controlMap);
						spb.percentGenus(10);
						spb.addSp64Distribution("S", 100);
						spb.addSite(sib -> {
							sib.siteSpecies("S");
							sib.siteCurveNumber(71);
						});
					});

				});
			});

			control.replay();

			app.init(resolver, controlMap);

			// Run the process

			var result = assertDoesNotThrow(() -> app.processYoung(poly));

			// Assertions

			final var forPolygon = hasProperty("polygonIdentifier", isPolyId("082F074/0142", 1999));
			final var forPrimeLayer = both(forPolygon).and(hasProperty("layerType", is(LayerType.PRIMARY)));

			assertThat(result, forPolygon);
			assertThat(result, hasProperty("mode", present(is(PolygonMode.YOUNG))));

			assertThat(
					result, hasProperty("layers", allOf(aMapWithSize(1), hasEntry(is(LayerType.PRIMARY), anything())))
			);
			var resultPrimaryLayer = result.getLayers().get(LayerType.PRIMARY);

			assertThat(resultPrimaryLayer, forPrimeLayer);

			assertThat(
					resultPrimaryLayer, hasProperty(
							"sites", allOf(
									aMapWithSize(5), //
									hasSite(
											is("B"), is("BL"),
											forPrimeLayer.and(hasProperty("siteCurveNumber", notPresent()))
									), //
									hasSite(
											is("C"), is("CW"),
											forPrimeLayer.and(hasProperty("siteCurveNumber", present(is(11))))
									), //
									hasSite(
											is("F"), is("FD"),
											forPrimeLayer.and(hasProperty("siteCurveNumber", present(is(23))))
									), //
									hasSite(
											is("H"), is("HW"),
											forPrimeLayer.and(hasProperty("siteCurveNumber", present(is(37))))
									), //
									hasSite(
											is("S"), is("S"),
											forPrimeLayer.and(hasProperty("siteCurveNumber", present(is(71))))
									)
							)
					)
			);

			assertThat(
					resultPrimaryLayer, hasProperty(
							"species", allOf(
									aMapWithSize(5), //
									hasSpecies(is("B"), is("BL"), closeTo(10), forPrimeLayer), //
									hasSpecies(is("C"), is("CW"), closeTo(20), forPrimeLayer), //
									hasSpecies(is("F"), is("FD"), closeTo(30), forPrimeLayer), //
									hasSpecies(is("H"), is("HW"), closeTo(30), forPrimeLayer), //
									hasSpecies(is("S"), is("S"), closeTo(10), forPrimeLayer)
							)
					)
			);

			for (var nonPrimaryGenus : List.of("B", "C", "H", "S")) {
				var nonPrimarySite = resultPrimaryLayer.getSites().get(nonPrimaryGenus);
				assertThat(
						nonPrimarySite, allOf(
								hasProperty("siteIndex", notPresent()), //
								hasProperty("height", notPresent()), //
								hasProperty("ageTotal", notPresent()), //
								hasProperty("yearsToBreastHeight", notPresent()), //
								hasProperty("breastHeightAge", notPresent())
						)
				);
			}

			var primarySite = resultPrimaryLayer.getPrimarySite().get();

			assertThat(
					primarySite, allOf(
							hasProperty("siteGenus", is("F")), //
							hasProperty("siteIndex", present(closeTo(19.7f))), //
							hasProperty("height", present(closeTo(8.43922043f))), //
							hasProperty("ageTotal", present(closeTo(26f))), //
							hasProperty("yearsToBreastHeight", present(closeTo(9f))), //
							hasProperty("breastHeightAge", present(closeTo(17f)))
					)
			);

			app.close();

			control.verify();
		}

		@Test
		void testIncreaseYear() throws Exception {
			var control = EasyMock.createControl();

			VriStart app = EasyMock.createMockBuilder(VriStart.class).createMock(control);

			MockFileResolver resolver = dummyInput();

			TestUtils.populateControlMapGenusReal(controlMap);
			TestUtils.populateControlMapBecReal(controlMap);
			controlMap.put(ControlKey.MINIMA.name(), Utils.constMap(map -> {
				map.put(VriControlParser.MINIMUM_BASE_AREA, 0f);
				map.put(VriControlParser.MINIMUM_HEIGHT, 6f);
				map.put(VriControlParser.MINIMUM_PREDICTED_BASE_AREA, 2f);
			}));
			TestUtils.populateControlMapFromResource(controlMap, new BasalAreaYieldParser(), "YLDBA407.COE");
			TestUtils.populateControlMapFromResource(controlMap, new UpperBoundsParser(), "PCT_407.coe");

			var poly = VriPolygon.build(pb -> {
				pb.polygonIdentifier("082F074/0142", 1997);
				pb.forestInventoryZone(" ");
				pb.biogeoclimaticZone(Utils.getBec("IDF", controlMap));
				pb.yieldFactor(1.0f);
				pb.mode(PolygonMode.YOUNG);
				pb.percentAvailable(85f);

				pb.addLayer(lb -> {
					lb.layerType(LayerType.PRIMARY);
					lb.baseArea(Optional.empty());
					lb.treesPerHectare(Optional.empty());
					lb.primaryGenus("F");
					lb.inventoryTypeGroup(3);
					lb.crownClosure(30);
					lb.utilization(7.5f);

					lb.empiricalRelationshipParameterIndex(61);
					lb.inventoryTypeGroup(3);

					lb.addSpecies(spb -> {
						spb.genus("B", controlMap);
						spb.percentGenus(10);
						spb.addSp64Distribution("BL", 100);
						spb.addSite(sib -> {
							sib.siteSpecies("BL");
						});
					});
					lb.addSpecies(spb -> {
						spb.genus("C", controlMap);
						spb.percentGenus(20);
						spb.addSp64Distribution("CW", 100);
						spb.addSite(sib -> {
							sib.siteCurveNumber(11);
							sib.siteSpecies("CW");
						});
					});
					lb.addSpecies(spb -> {
						spb.genus("F", controlMap);
						spb.percentGenus(30);
						spb.addSp64Distribution("FD", 100);
						spb.addSite(sib -> {
							sib.siteSpecies("FD");
							sib.siteCurveNumber(23);
							sib.siteIndex(19.7f);
							sib.height(6f); // Set this low so we have to increment year
							sib.yearsToBreastHeight(9);
							sib.breastHeightAge(15);
							sib.ageTotal(24);
						});
					});
					lb.addSpecies(spb -> {
						spb.genus("H", controlMap);
						spb.percentGenus(30);
						spb.addSp64Distribution("HW", 100);
						spb.addSite(sib -> {
							sib.siteSpecies("HW");
							sib.siteCurveNumber(37);
						});
					});
					lb.addSpecies(spb -> {
						spb.genus("S", controlMap);
						spb.percentGenus(10);
						spb.addSp64Distribution("S", 100);
						spb.addSite(sib -> {
							sib.siteSpecies("S");
							sib.siteCurveNumber(71);
						});
					});

				});
			});

			control.replay();

			app.init(resolver, controlMap);

			// Run the process

			var result = assertDoesNotThrow(() -> app.processYoung(poly));

			// Assertions

			final var forPolygon = hasProperty("polygonIdentifier", isPolyId("082F074/0142", 2001));
			final var forPrimeLayer = both(forPolygon).and(hasProperty("layerType", is(LayerType.PRIMARY)));

			assertThat(result, forPolygon);
			assertThat(result, hasProperty("mode", present(is(PolygonMode.YOUNG))));

			assertThat(
					result, hasProperty("layers", allOf(aMapWithSize(1), hasEntry(is(LayerType.PRIMARY), anything())))
			);
			var resultPrimaryLayer = result.getLayers().get(LayerType.PRIMARY);

			assertThat(resultPrimaryLayer, forPrimeLayer);

			assertThat(
					resultPrimaryLayer, hasProperty(
							"sites", allOf(
									aMapWithSize(5), //
									hasSite(
											is("B"), is("BL"),
											forPrimeLayer.and(hasProperty("siteCurveNumber", notPresent()))
									), //
									hasSite(
											is("C"), is("CW"),
											forPrimeLayer.and(hasProperty("siteCurveNumber", present(is(11))))
									), //
									hasSite(
											is("F"), is("FD"),
											forPrimeLayer.and(hasProperty("siteCurveNumber", present(is(23))))
									), //
									hasSite(
											is("H"), is("HW"),
											forPrimeLayer.and(hasProperty("siteCurveNumber", present(is(37))))
									), //
									hasSite(
											is("S"), is("S"),
											forPrimeLayer.and(hasProperty("siteCurveNumber", present(is(71))))
									)
							)
					)
			);

			assertThat(
					resultPrimaryLayer, hasProperty(
							"species", allOf(
									aMapWithSize(5), //
									hasSpecies(is("B"), is("BL"), closeTo(10), forPrimeLayer), //
									hasSpecies(is("C"), is("CW"), closeTo(20), forPrimeLayer), //
									hasSpecies(is("F"), is("FD"), closeTo(30), forPrimeLayer), //
									hasSpecies(is("H"), is("HW"), closeTo(30), forPrimeLayer), //
									hasSpecies(is("S"), is("S"), closeTo(10), forPrimeLayer)
							)
					)
			);

			for (var nonPrimaryGenus : List.of("B", "C", "H", "S")) {
				var nonPrimarySite = resultPrimaryLayer.getSites().get(nonPrimaryGenus);
				assertThat(
						nonPrimarySite, allOf(
								hasProperty("siteIndex", notPresent()), //
								hasProperty("height", notPresent()), //
								hasProperty("ageTotal", notPresent()), //
								hasProperty("yearsToBreastHeight", notPresent()), //
								hasProperty("breastHeightAge", notPresent())
						)
				);
			}

			var primarySite = resultPrimaryLayer.getPrimarySite().get();

			assertThat(
					primarySite, allOf(
							hasProperty("siteGenus", is("F")), //
							hasProperty("siteIndex", present(closeTo(19.7f))), //
							hasProperty("height", present(closeTo(7.6620512f))), //
							hasProperty("ageTotal", present(closeTo(28f))), //
							hasProperty("yearsToBreastHeight", present(closeTo(9f))), //
							hasProperty("breastHeightAge", present(closeTo(19f)))
					)
			);

			app.close();

			control.verify();
		}

		@Test
		void testTooOld() throws Exception {
			var control = EasyMock.createControl();

			VriStart app = new VriStart();

			MockFileResolver resolver = dummyInput();

			TestUtils.populateControlMapBecReal(controlMap);

			var poly = VriPolygon.build(pb -> {
				pb.polygonIdentifier("TestPolygon", 1899);
				pb.biogeoclimaticZone(Utils.getBec("IDF", controlMap));
				pb.yieldFactor(1.0f);
				pb.mode(PolygonMode.YOUNG);
			});

			control.replay();

			app.init(resolver, controlMap);

			var ex = assertThrows(FatalProcessingException.class, () -> app.processYoung(poly));

			assertThat(ex, hasProperty("message", is("Year for YOUNG stand should be at least 1900 but was 1899")));

			app.close();

			control.verify();
		}

	}

	@Nested
	class ProcessBatc {
		@Test
		void testNoVeteran() throws Exception {
			var control = EasyMock.createControl();

			VriStart app = EasyMock.createMockBuilder(VriStart.class).createMock(control);

			MockFileResolver resolver = dummyInput();

			TestUtils.populateControlMapGenusReal(controlMap);
			TestUtils.populateControlMapBecReal(controlMap);

			TestUtils.populateControlMapFromResource(controlMap, new BaseAreaCoefficientParser(), "REGBA25.coe");
			TestUtils
					.populateControlMapFromResource(controlMap, new QuadMeanDiameterCoefficientParser(), "REGDQ26.coe");

			TestUtils.populateControlMapFromResource(controlMap, new HLPrimarySpeciesEqnP1Parser(), "REGYHLP.COE");
			TestUtils.populateControlMapFromResource(controlMap, new HLPrimarySpeciesEqnP2Parser(), "REGYHLPA.COE");
			TestUtils.populateControlMapFromResource(controlMap, new HLPrimarySpeciesEqnP3Parser(), "REGYHLPB.DAT");
			TestUtils.populateControlMapFromResource(controlMap, new HLNonprimaryCoefficientParser(), "REGHL.COE");

			TestUtils.populateControlMapFromResource(controlMap, new UpperCoefficientParser(), "UPPERB02.COE");

			TestUtils.populateControlMapFromResource(
					controlMap, new ModifierParser(VdypApplicationIdentifier.VRI_START), "mod19813.prm"
			);

			var poly = VriPolygon.build(pb -> {
				pb.polygonIdentifier("082F074/0142", 1997);
				pb.forestInventoryZone(" ");
				pb.biogeoclimaticZone(Utils.getBec("IDF", controlMap));
				pb.yieldFactor(1.0f);
				pb.mode(PolygonMode.BATC);

				pb.addLayer(lb -> {
					lb.layerType(LayerType.PRIMARY);
					lb.baseArea(Optional.empty());
					lb.treesPerHectare(Optional.empty());
					lb.primaryGenus("F");
					lb.inventoryTypeGroup(3);
					lb.crownClosure(30);
					lb.utilization(7.5f);

					lb.empiricalRelationshipParameterIndex(61);

					lb.addSpecies(spb -> {
						spb.genus("B", controlMap);
						spb.percentGenus(10);
						spb.addSp64Distribution("BL", 100);
						spb.addSite(sib -> {
							sib.siteSpecies("BL");
						});
					});
					lb.addSpecies(spb -> {
						spb.genus("C", controlMap);
						spb.percentGenus(20);
						spb.addSp64Distribution("CW", 100);
						spb.addSite(sib -> {
							sib.siteCurveNumber(11);
							sib.siteSpecies("CW");
						});
					});
					lb.addSpecies(spb -> {
						spb.genus("F", controlMap);
						spb.percentGenus(30);
						spb.addSp64Distribution("FD", 100);
						spb.addSite(sib -> {
							sib.siteSpecies("FD");
							sib.siteCurveNumber(23);
							sib.siteIndex(19.7f);
							sib.height(7.6f);
							sib.yearsToBreastHeight(9);
							sib.breastHeightAge(15);
							sib.ageTotal(24);
						});
					});
					lb.addSpecies(spb -> {
						spb.genus("H", controlMap);
						spb.percentGenus(30);
						spb.addSp64Distribution("HW", 100);
						spb.addSite(sib -> {
							sib.siteSpecies("HW");
							sib.siteCurveNumber(37);
						});
					});
					lb.addSpecies(spb -> {
						spb.genus("S", controlMap);
						spb.percentGenus(10);
						spb.addSp64Distribution("S", 100);
						spb.addSite(sib -> {
							sib.siteSpecies("S");
							sib.siteCurveNumber(71);
						});
					});

				});
			});

			control.replay();

			app.init(resolver, controlMap);

			// Run the process

			var result = assertDoesNotThrow(() -> app.processBatc(poly));

			// Assertions

			final var forPolygon = hasProperty("polygonIdentifier", isPolyId("082F074/0142", 1997));
			final var forPrimeLayer = both(forPolygon).and(hasProperty("layerType", is(LayerType.PRIMARY)));

			assertThat(result, forPolygon);
			assertThat(result, hasProperty("mode", present(is(PolygonMode.BATC))));

			assertThat(
					result, hasProperty("layers", allOf(aMapWithSize(1), hasEntry(is(LayerType.PRIMARY), anything())))
			);
			var resultPrimaryLayer = result.getLayers().get(LayerType.PRIMARY);

			assertThat(resultPrimaryLayer, forPrimeLayer);

			assertThat(resultPrimaryLayer, hasProperty("baseArea", present(closeTo(0.72882f))));
			assertThat(resultPrimaryLayer, hasProperty("treesPerHectare", present(closeTo(122.3f))));

			assertThat(
					resultPrimaryLayer, hasProperty(
							"sites", allOf(
									aMapWithSize(5), //
									hasSite(
											is("B"), is("BL"),
											forPrimeLayer.and(hasProperty("siteCurveNumber", notPresent()))
									), //
									hasSite(
											is("C"), is("CW"),
											forPrimeLayer.and(hasProperty("siteCurveNumber", present(is(11))))
									), //
									hasSite(
											is("F"), is("FD"),
											forPrimeLayer.and(hasProperty("siteCurveNumber", present(is(23))))
									), //
									hasSite(
											is("H"), is("HW"),
											forPrimeLayer.and(hasProperty("siteCurveNumber", present(is(37))))
									), //
									hasSite(
											is("S"), is("S"),
											forPrimeLayer.and(hasProperty("siteCurveNumber", present(is(71))))
									)
							)
					)
			);

			assertThat(
					resultPrimaryLayer, hasProperty(
							"species", allOf(
									aMapWithSize(5), //
									hasSpecies(is("B"), is("BL"), closeTo(10), forPrimeLayer), //
									hasSpecies(is("C"), is("CW"), closeTo(20), forPrimeLayer), //
									hasSpecies(is("F"), is("FD"), closeTo(30), forPrimeLayer), //
									hasSpecies(is("H"), is("HW"), closeTo(30), forPrimeLayer), //
									hasSpecies(is("S"), is("S"), closeTo(10), forPrimeLayer)
							)
					)
			);

			for (var nonPrimaryGenus : List.of("B", "C", "H", "S")) {
				var nonPrimarySite = resultPrimaryLayer.getSites().get(nonPrimaryGenus);
				assertThat(
						nonPrimarySite, allOf(
								hasProperty("siteIndex", notPresent()), //
								hasProperty("height", notPresent()), //
								hasProperty("ageTotal", notPresent()), //
								hasProperty("yearsToBreastHeight", notPresent()), //
								hasProperty("breastHeightAge", notPresent())
						)
				);
			}

			var primarySite = resultPrimaryLayer.getPrimarySite().get();

			assertThat(
					primarySite, allOf(
							hasProperty("siteGenus", is("F")), //
							hasProperty("siteIndex", present(closeTo(19.7f))), //
							hasProperty("height", present(closeTo(7.6f))), //
							hasProperty("ageTotal", present(closeTo(24f))), //
							hasProperty("yearsToBreastHeight", present(closeTo(9f))), //
							hasProperty("breastHeightAge", present(closeTo(15f)))
					)
			);

			app.close();

			control.verify();
		}

		@Test
		void testWithVeteran() throws Exception {
			var control = EasyMock.createControl();

			VriStart app = EasyMock.createMockBuilder(VriStart.class).createMock(control);

			MockFileResolver resolver = dummyInput();

			TestUtils.populateControlMapGenusReal(controlMap);
			TestUtils.populateControlMapBecReal(controlMap);

			TestUtils.populateControlMapFromResource(controlMap, new BaseAreaCoefficientParser(), "REGBA25.coe");
			TestUtils
					.populateControlMapFromResource(controlMap, new QuadMeanDiameterCoefficientParser(), "REGDQ26.coe");

			TestUtils.populateControlMapFromResource(controlMap, new HLPrimarySpeciesEqnP1Parser(), "REGYHLP.COE");
			TestUtils.populateControlMapFromResource(controlMap, new HLPrimarySpeciesEqnP2Parser(), "REGYHLPA.COE");
			TestUtils.populateControlMapFromResource(controlMap, new HLPrimarySpeciesEqnP3Parser(), "REGYHLPB.DAT");
			TestUtils.populateControlMapFromResource(controlMap, new HLNonprimaryCoefficientParser(), "REGHL.COE");

			TestUtils.populateControlMapFromResource(controlMap, new UpperCoefficientParser(), "UPPERB02.COE");

			TestUtils.populateControlMapFromResource(
					controlMap, new ModifierParser(VdypApplicationIdentifier.VRI_START), "mod19813.prm"
			);

			var poly = VriPolygon.build(pb -> {
				pb.polygonIdentifier("082F074/0142", 1997);
				pb.forestInventoryZone(" ");
				pb.biogeoclimaticZone(Utils.getBec("IDF", controlMap));
				pb.yieldFactor(1.0f);
				pb.mode(PolygonMode.BATC);

				pb.addLayer(lb -> {
					lb.layerType(LayerType.PRIMARY);
					lb.baseArea(Optional.empty());
					lb.treesPerHectare(Optional.empty());
					lb.primaryGenus("F");
					lb.inventoryTypeGroup(3);
					lb.crownClosure(30);
					lb.utilization(7.5f);

					lb.empiricalRelationshipParameterIndex(61);

					lb.addSpecies(spb -> {
						spb.genus("B", controlMap);
						spb.percentGenus(10);
						spb.addSp64Distribution("BL", 100);
						spb.addSite(sib -> {
							sib.siteSpecies("BL");
						});
					});
					lb.addSpecies(spb -> {
						spb.genus("C", controlMap);
						spb.percentGenus(20);
						spb.addSp64Distribution("CW", 100);
						spb.addSite(sib -> {
							sib.siteCurveNumber(11);
							sib.siteSpecies("CW");
						});
					});
					lb.addSpecies(spb -> {
						spb.genus("F", controlMap);
						spb.percentGenus(30);
						spb.addSp64Distribution("FD", 100);
						spb.addSite(sib -> {
							sib.siteSpecies("FD");
							sib.siteCurveNumber(23);
							sib.siteIndex(19.7f);
							sib.height(7.6f);
							sib.yearsToBreastHeight(9);
							sib.breastHeightAge(15);
							sib.ageTotal(24);
						});
					});
					lb.addSpecies(spb -> {
						spb.genus("H", controlMap);
						spb.percentGenus(30);
						spb.addSp64Distribution("HW", 100);
						spb.addSite(sib -> {
							sib.siteSpecies("HW");
							sib.siteCurveNumber(37);
						});
					});
					lb.addSpecies(spb -> {
						spb.genus("S", controlMap);
						spb.percentGenus(10);
						spb.addSp64Distribution("S", 100);
						spb.addSite(sib -> {
							sib.siteSpecies("S");
							sib.siteCurveNumber(71);
						});
					});

				});
				pb.addLayer(lb -> {
					lb.layerType(LayerType.VETERAN);
					lb.baseArea(Optional.empty());
					lb.treesPerHectare(Optional.empty());
					lb.primaryGenus("F");
					lb.inventoryTypeGroup(3);
					lb.crownClosure(30);
					lb.utilization(7.5f);

					lb.empiricalRelationshipParameterIndex(61);

					lb.addSpecies(spb -> {
						spb.genus("B", controlMap);
						spb.percentGenus(10);
						spb.addSp64Distribution("BL", 100);
						spb.addSite(sib -> {
							sib.siteSpecies("BL");
						});
					});
					lb.addSpecies(spb -> {
						spb.genus("C", controlMap);
						spb.percentGenus(20);
						spb.addSp64Distribution("CW", 100);
						spb.addSite(sib -> {
							sib.siteCurveNumber(11);
							sib.siteSpecies("CW");
						});
					});
					lb.addSpecies(spb -> {
						spb.genus("F", controlMap);
						spb.percentGenus(30);
						spb.addSp64Distribution("FD", 100);
						spb.addSite(sib -> {
							sib.siteSpecies("FD");
							sib.siteCurveNumber(23);
							sib.siteIndex(19.7f);
							sib.height(7.6f);
							sib.yearsToBreastHeight(9);
							sib.breastHeightAge(15);
							sib.ageTotal(24);
						});
					});
					lb.addSpecies(spb -> {
						spb.genus("H", controlMap);
						spb.percentGenus(30);
						spb.addSp64Distribution("HW", 100);
						spb.addSite(sib -> {
							sib.siteSpecies("HW");
							sib.siteCurveNumber(37);
						});
					});
					lb.addSpecies(spb -> {
						spb.genus("S", controlMap);
						spb.percentGenus(10);
						spb.addSp64Distribution("S", 100);
						spb.addSite(sib -> {
							sib.siteSpecies("S");
							sib.siteCurveNumber(71);
						});
					});

				});
			});

			control.replay();

			app.init(resolver, controlMap);

			// Run the process

			var result = assertDoesNotThrow(() -> app.processBatc(poly));

			// Assertions

			final var forPolygon = hasProperty("polygonIdentifier", isPolyId("082F074/0142", 1997));
			final var forPrimeLayer = both(forPolygon).and(hasProperty("layerType", is(LayerType.PRIMARY)));
			final var forVeteranLayer = both(forPolygon).and(hasProperty("layerType", is(LayerType.VETERAN)));

			assertThat(result, forPolygon);
			assertThat(result, hasProperty("mode", present(is(PolygonMode.BATC))));

			assertThat(
					result,
					hasProperty(
							"layers",
							allOf(
									aMapWithSize(2), hasEntry(is(LayerType.PRIMARY), anything()),
									hasEntry(is(LayerType.VETERAN), anything())
							)
					)
			);
			var resultPrimaryLayer = result.getLayers().get(LayerType.PRIMARY);
			var resultVeteranLayer = result.getLayers().get(LayerType.VETERAN);

			assertThat(resultPrimaryLayer, forPrimeLayer);
			assertThat(resultVeteranLayer, forVeteranLayer);

			assertThat(resultPrimaryLayer, hasProperty("baseArea", present(closeTo(0.72324f))));
			assertThat(resultPrimaryLayer, hasProperty("treesPerHectare", present(closeTo(121.6f))));

			assertThat(resultVeteranLayer, hasProperty("baseArea", present(closeTo(0.80981f))));

			for (var nonPrimaryGenus : List.of("B", "C", "H", "S")) {
				var nonPrimarySite = resultPrimaryLayer.getSites().get(nonPrimaryGenus);
				assertThat(
						nonPrimarySite, allOf(
								hasProperty("siteIndex", notPresent()), //
								hasProperty("height", notPresent()), //
								hasProperty("ageTotal", notPresent()), //
								hasProperty("yearsToBreastHeight", notPresent()), //
								hasProperty("breastHeightAge", notPresent())
						)
				);
			}

			var primarySite = resultPrimaryLayer.getPrimarySite().get();

			assertThat(
					primarySite, allOf(
							hasProperty("siteGenus", is("F")), //
							hasProperty("siteIndex", present(closeTo(19.7f))), //
							hasProperty("height", present(closeTo(7.6f))), //
							hasProperty("ageTotal", present(closeTo(24f))), //
							hasProperty("yearsToBreastHeight", present(closeTo(9f))), //
							hasProperty("breastHeightAge", present(closeTo(15f)))
					)
			);

			app.close();

			control.verify();
		}
	}

	@Nested
	class ProcessBatn {

		@Test
		void testNoVeteran() throws Exception {
			var control = EasyMock.createControl();

			VriStart app = new VriStart();

			MockFileResolver resolver = dummyInput();

			TestUtils.populateControlMapGenusReal(controlMap);
			TestUtils.populateControlMapBecReal(controlMap);

			TestUtils.populateControlMapFromResource(controlMap, new BaseAreaCoefficientParser(), "REGBA25.coe");
			TestUtils
					.populateControlMapFromResource(controlMap, new QuadMeanDiameterCoefficientParser(), "REGDQ26.coe");

			TestUtils.populateControlMapFromResource(controlMap, new HLPrimarySpeciesEqnP1Parser(), "REGYHLP.COE");
			TestUtils.populateControlMapFromResource(controlMap, new HLPrimarySpeciesEqnP2Parser(), "REGYHLPA.COE");
			TestUtils.populateControlMapFromResource(controlMap, new HLPrimarySpeciesEqnP3Parser(), "REGYHLPB.DAT");
			TestUtils.populateControlMapFromResource(controlMap, new HLNonprimaryCoefficientParser(), "REGHL.COE");

			TestUtils.populateControlMapFromResource(controlMap, new UpperCoefficientParser(), "UPPERB02.COE");

			TestUtils.populateControlMapFromResource(controlMap, new BasalAreaYieldParser(), "YLDBA407.COE");
			TestUtils.populateControlMapFromResource(controlMap, new QuadraticMeanDiameterYieldParser(), "YLDDQ45.COE");
			TestUtils.populateControlMapFromResource(controlMap, new UpperBoundsParser(), "PCT_407.coe");

			TestUtils.populateControlMapFromResource(
					controlMap, new ModifierParser(VdypApplicationIdentifier.VRI_START), "mod19813.prm"
			);

			var poly = VriPolygon.build(pb -> {
				pb.polygonIdentifier("082F074/0142", 1997);
				pb.forestInventoryZone(" ");
				pb.biogeoclimaticZone(Utils.getBec("IDF", controlMap));
				pb.yieldFactor(1.0f);
				pb.mode(PolygonMode.BATN);

				pb.addLayer(lb -> {
					lb.layerType(LayerType.PRIMARY);
					lb.baseArea(Optional.empty());
					lb.treesPerHectare(Optional.empty());
					lb.primaryGenus("F");
					lb.inventoryTypeGroup(3);
					lb.crownClosure(30);
					lb.utilization(7.5f);

					lb.empiricalRelationshipParameterIndex(61);

					lb.addSpecies(spb -> {
						spb.genus("B", controlMap);
						spb.percentGenus(10);
						spb.addSp64Distribution("BL", 100);
						spb.addSite(sib -> {
							sib.siteSpecies("BL");
						});
					});
					lb.addSpecies(spb -> {
						spb.genus("C", controlMap);
						spb.percentGenus(20);
						spb.addSp64Distribution("CW", 100);
						spb.addSite(sib -> {
							sib.siteCurveNumber(11);
							sib.siteSpecies("CW");
						});
					});
					lb.addSpecies(spb -> {
						spb.genus("F", controlMap);
						spb.percentGenus(30);
						spb.addSp64Distribution("FD", 100);
						spb.addSite(sib -> {
							sib.siteSpecies("FD");
							sib.siteCurveNumber(23);
							sib.siteIndex(19.7f);
							sib.height(7.6f);
							sib.yearsToBreastHeight(9);
							sib.breastHeightAge(15);
							sib.ageTotal(24);
						});
					});
					lb.addSpecies(spb -> {
						spb.genus("H", controlMap);
						spb.percentGenus(30);
						spb.addSp64Distribution("HW", 100);
						spb.addSite(sib -> {
							sib.siteSpecies("HW");
							sib.siteCurveNumber(37);
						});
					});
					lb.addSpecies(spb -> {
						spb.genus("S", controlMap);
						spb.percentGenus(10);
						spb.addSp64Distribution("S", 100);
						spb.addSite(sib -> {
							sib.siteSpecies("S");
							sib.siteCurveNumber(71);
						});
					});

				});
			});

			control.replay();

			app.init(resolver, controlMap);

			// Run the process

			var result = assertDoesNotThrow(() -> app.processBatn(poly));

			// Assertions

			final var forPolygon = hasProperty("polygonIdentifier", isPolyId("082F074/0142", 1997));
			final var forPrimeLayer = both(forPolygon).and(hasProperty("layerType", is(LayerType.PRIMARY)));

			assertThat(result, forPolygon);
			assertThat(result, hasProperty("mode", present(is(PolygonMode.BATN))));

			assertThat(
					result, hasProperty("layers", allOf(aMapWithSize(1), hasEntry(is(LayerType.PRIMARY), anything())))
			);
			var resultPrimaryLayer = result.getLayers().get(LayerType.PRIMARY);

			assertThat(resultPrimaryLayer, forPrimeLayer);

			assertThat(resultPrimaryLayer, hasProperty("baseArea", present(closeTo(6.34290648f))));
			assertThat(resultPrimaryLayer, hasProperty("treesPerHectare", present(closeTo(748.402222f))));

			assertThat(
					resultPrimaryLayer, hasProperty(
							"sites", allOf(
									aMapWithSize(5), //
									hasSite(
											is("B"), is("BL"),
											forPrimeLayer.and(hasProperty("siteCurveNumber", notPresent()))
									), //
									hasSite(
											is("C"), is("CW"),
											forPrimeLayer.and(hasProperty("siteCurveNumber", present(is(11))))
									), //
									hasSite(
											is("F"), is("FD"),
											forPrimeLayer.and(hasProperty("siteCurveNumber", present(is(23))))
									), //
									hasSite(
											is("H"), is("HW"),
											forPrimeLayer.and(hasProperty("siteCurveNumber", present(is(37))))
									), //
									hasSite(
											is("S"), is("S"),
											forPrimeLayer.and(hasProperty("siteCurveNumber", present(is(71))))
									)
							)
					)
			);

			assertThat(
					resultPrimaryLayer, hasProperty(
							"species", allOf(
									aMapWithSize(5), //
									hasSpecies(is("B"), is("BL"), closeTo(10), forPrimeLayer), //
									hasSpecies(is("C"), is("CW"), closeTo(20), forPrimeLayer), //
									hasSpecies(is("F"), is("FD"), closeTo(30), forPrimeLayer), //
									hasSpecies(is("H"), is("HW"), closeTo(30), forPrimeLayer), //
									hasSpecies(is("S"), is("S"), closeTo(10), forPrimeLayer)
							)
					)
			);

			for (var nonPrimaryGenus : List.of("B", "C", "H", "S")) {
				var nonPrimarySite = resultPrimaryLayer.getSites().get(nonPrimaryGenus);
				assertThat(
						nonPrimarySite, allOf(
								hasProperty("siteIndex", notPresent()), //
								hasProperty("height", notPresent()), //
								hasProperty("ageTotal", notPresent()), //
								hasProperty("yearsToBreastHeight", notPresent()), //
								hasProperty("breastHeightAge", notPresent())
						)
				);
			}

			var primarySite = resultPrimaryLayer.getPrimarySite().get();

			assertThat(
					primarySite, allOf(
							hasProperty("siteGenus", is("F")), //
							hasProperty("siteIndex", present(closeTo(19.7f))), //
							hasProperty("height", present(closeTo(7.6f))), //
							hasProperty("ageTotal", present(closeTo(24f))), //
							hasProperty("yearsToBreastHeight", present(closeTo(9f))), //
							hasProperty("breastHeightAge", present(closeTo(15f)))
					)
			);

			app.close();

			control.verify();
		}

		@Test
		void testWithVeteran() throws Exception {
			var control = EasyMock.createControl();

			VriStart app = EasyMock.createMockBuilder(VriStart.class).createMock(control);

			MockFileResolver resolver = dummyInput();

			TestUtils.populateControlMapGenusReal(controlMap);
			TestUtils.populateControlMapBecReal(controlMap);

			TestUtils.populateControlMapFromResource(controlMap, new BaseAreaCoefficientParser(), "REGBA25.coe");
			TestUtils
					.populateControlMapFromResource(controlMap, new QuadMeanDiameterCoefficientParser(), "REGDQ26.coe");

			TestUtils.populateControlMapFromResource(controlMap, new HLPrimarySpeciesEqnP1Parser(), "REGYHLP.COE");
			TestUtils.populateControlMapFromResource(controlMap, new HLPrimarySpeciesEqnP2Parser(), "REGYHLPA.COE");
			TestUtils.populateControlMapFromResource(controlMap, new HLPrimarySpeciesEqnP3Parser(), "REGYHLPB.DAT");
			TestUtils.populateControlMapFromResource(controlMap, new HLNonprimaryCoefficientParser(), "REGHL.COE");

			TestUtils.populateControlMapFromResource(controlMap, new UpperCoefficientParser(), "UPPERB02.COE");

			TestUtils.populateControlMapFromResource(controlMap, new BasalAreaYieldParser(), "YLDBA407.COE");
			TestUtils.populateControlMapFromResource(controlMap, new QuadraticMeanDiameterYieldParser(), "YLDDQ45.COE");
			TestUtils.populateControlMapFromResource(controlMap, new UpperBoundsParser(), "PCT_407.coe");

			TestUtils.populateControlMapFromResource(
					controlMap, new ModifierParser(VdypApplicationIdentifier.VRI_START), "mod19813.prm"
			);

			var poly = VriPolygon.build(pb -> {
				pb.polygonIdentifier("082F074/0142", 1997);
				pb.forestInventoryZone(" ");
				pb.biogeoclimaticZone(Utils.getBec("IDF", controlMap));
				pb.yieldFactor(1.0f);
				pb.mode(PolygonMode.BATN);

				pb.addLayer(lb -> {
					lb.layerType(LayerType.PRIMARY);
					lb.baseArea(Optional.empty());
					lb.treesPerHectare(Optional.empty());
					lb.primaryGenus("F");
					lb.inventoryTypeGroup(3);
					lb.crownClosure(30);
					lb.utilization(7.5f);

					lb.empiricalRelationshipParameterIndex(61);

					lb.addSpecies(spb -> {
						spb.genus("B", controlMap);
						spb.percentGenus(10);
						spb.addSp64Distribution("BL", 100);
						spb.addSite(sib -> {
							sib.siteSpecies("BL");
						});
					});
					lb.addSpecies(spb -> {
						spb.genus("C", controlMap);
						spb.percentGenus(20);
						spb.addSp64Distribution("CW", 100);
						spb.addSite(sib -> {
							sib.siteCurveNumber(11);
							sib.siteSpecies("CW");
						});
					});
					lb.addSpecies(spb -> {
						spb.genus("F", controlMap);
						spb.percentGenus(30);
						spb.addSp64Distribution("FD", 100);
						spb.addSite(sib -> {
							sib.siteSpecies("FD");
							sib.siteCurveNumber(23);
							sib.siteIndex(19.7f);
							sib.height(7.6f);
							sib.yearsToBreastHeight(9);
							sib.breastHeightAge(15);
							sib.ageTotal(24);
						});
					});
					lb.addSpecies(spb -> {
						spb.genus("H", controlMap);
						spb.percentGenus(30);
						spb.addSp64Distribution("HW", 100);
						spb.addSite(sib -> {
							sib.siteSpecies("HW");
							sib.siteCurveNumber(37);
						});
					});
					lb.addSpecies(spb -> {
						spb.genus("S", controlMap);
						spb.percentGenus(10);
						spb.addSp64Distribution("S", 100);
						spb.addSite(sib -> {
							sib.siteSpecies("S");
							sib.siteCurveNumber(71);
						});
					});

				});
				pb.addLayer(lb -> {
					lb.layerType(LayerType.VETERAN);
					lb.baseArea(5f);
					lb.treesPerHectare(Optional.empty());
					lb.primaryGenus("F");
					lb.inventoryTypeGroup(3);
					lb.crownClosure(30);
					lb.utilization(7.5f);

					lb.empiricalRelationshipParameterIndex(61);

					lb.addSpecies(spb -> {
						spb.genus("B", controlMap);
						spb.percentGenus(10);
						spb.addSp64Distribution("BL", 100);
						spb.addSite(sib -> {
							sib.siteSpecies("BL");
						});
					});
					lb.addSpecies(spb -> {
						spb.genus("C", controlMap);
						spb.percentGenus(20);
						spb.addSp64Distribution("CW", 100);
						spb.addSite(sib -> {
							sib.siteCurveNumber(11);
							sib.siteSpecies("CW");
						});
					});
					lb.addSpecies(spb -> {
						spb.genus("F", controlMap);
						spb.percentGenus(30);
						spb.addSp64Distribution("FD", 100);
						spb.addSite(sib -> {
							sib.siteSpecies("FD");
							sib.siteCurveNumber(23);
							sib.siteIndex(19.7f);
							sib.height(7.6f);
							sib.yearsToBreastHeight(9);
							sib.breastHeightAge(15);
							sib.ageTotal(24);
						});
					});
					lb.addSpecies(spb -> {
						spb.genus("H", controlMap);
						spb.percentGenus(30);
						spb.addSp64Distribution("HW", 100);
						spb.addSite(sib -> {
							sib.siteSpecies("HW");
							sib.siteCurveNumber(37);
						});
					});
					lb.addSpecies(spb -> {
						spb.genus("S", controlMap);
						spb.percentGenus(10);
						spb.addSp64Distribution("S", 100);
						spb.addSite(sib -> {
							sib.siteSpecies("S");
							sib.siteCurveNumber(71);
						});
					});

				});
			});

			control.replay();

			app.init(resolver, controlMap);

			// Run the process

			var result = assertDoesNotThrow(() -> app.processBatn(poly));

			// Assertions

			final var forPolygon = hasProperty("polygonIdentifier", isPolyId("082F074/0142", 1997));
			final var forPrimeLayer = both(forPolygon).and(hasProperty("layerType", is(LayerType.PRIMARY)));
			final var forVeteranLayer = both(forPolygon).and(hasProperty("layerType", is(LayerType.VETERAN)));

			assertThat(result, forPolygon);
			assertThat(result, hasProperty("mode", present(is(PolygonMode.BATN))));

			assertThat(
					result,
					hasProperty(
							"layers",
							allOf(
									aMapWithSize(2), hasEntry(is(LayerType.PRIMARY), anything()),
									hasEntry(is(LayerType.VETERAN), anything())
							)
					)
			);
			var resultPrimaryLayer = result.getLayers().get(LayerType.PRIMARY);
			var resultVeteranLayer = result.getLayers().get(LayerType.VETERAN);

			assertThat(resultPrimaryLayer, forPrimeLayer);
			assertThat(resultVeteranLayer, forVeteranLayer);

			assertThat(resultPrimaryLayer, hasProperty("baseArea", present(closeTo(6.06380272f))));
			assertThat(resultPrimaryLayer, hasProperty("treesPerHectare", present(closeTo(715.977112f))));

			assertThat(resultVeteranLayer, hasProperty("baseArea", present(closeTo(5f))));

			for (var nonPrimaryGenus : List.of("B", "C", "H", "S")) {
				var nonPrimarySite = resultPrimaryLayer.getSites().get(nonPrimaryGenus);
				assertThat(
						nonPrimarySite, allOf(
								hasProperty("siteIndex", notPresent()), //
								hasProperty("height", notPresent()), //
								hasProperty("ageTotal", notPresent()), //
								hasProperty("yearsToBreastHeight", notPresent()), //
								hasProperty("breastHeightAge", notPresent())
						)
				);
			}

			var primarySite = resultPrimaryLayer.getPrimarySite().get();

			assertThat(
					primarySite, allOf(
							hasProperty("siteGenus", is("F")), //
							hasProperty("siteIndex", present(closeTo(19.7f))), //
							hasProperty("height", present(closeTo(7.6f))), //
							hasProperty("ageTotal", present(closeTo(24f))), //
							hasProperty("yearsToBreastHeight", present(closeTo(9f))), //
							hasProperty("breastHeightAge", present(closeTo(15f)))
					)
			);

			app.close();

			control.verify();
		}
	}

	@Nested
	class EstimateQuadMeanDiameterYield {

		@Test
		void testCompute() throws ProcessingException {

			controlMap = VriTestUtils.loadControlMap();
			VriStart app = new VriStart();
			ApplicationTestUtils.setControlMap(app, controlMap);

			var polygon = VriPolygon.build(pBuilder -> {
				pBuilder.polygonIdentifier("Test", 2024);
				pBuilder.biogeoclimaticZone(Utils.getBec("IDF", controlMap));
				pBuilder.yieldFactor(1.0f);
				pBuilder.addLayer(lBuilder -> {
					lBuilder.layerType(LayerType.PRIMARY);
					lBuilder.crownClosure(57.8f);
					lBuilder.utilization(7.5f);
					lBuilder.empiricalRelationshipParameterIndex(61);

					lBuilder.addSpecies(sBuilder -> {
						sBuilder.genus("B", controlMap);
						sBuilder.percentGenus(10f);
					});
					lBuilder.addSpecies(sBuilder -> {
						sBuilder.genus("C", controlMap);
						sBuilder.percentGenus(20f);
					});
					lBuilder.addSpecies(sBuilder -> {
						sBuilder.genus("F", controlMap);
						sBuilder.percentGenus(30f);
					});
					lBuilder.addSpecies(sBuilder -> {
						sBuilder.genus("H", controlMap);
						sBuilder.percentGenus(30f);
					});
					lBuilder.addSpecies(sBuilder -> {
						sBuilder.genus("S", controlMap);
						sBuilder.percentGenus(10f);
					});

					lBuilder.primaryGenus("F");
				});
			});

			var species = polygon.getLayers().get(LayerType.PRIMARY).getSpecies().values();

			var bec = Utils.expectParsedControl(controlMap, ControlKey.BEC_DEF, BecLookup.class).get("IDF").get();

			float result = app.estimateQuadMeanDiameterYield(7.6f, 15f, Optional.empty(), species, bec, 61);

			assertThat(result, closeTo(10.3879938f));
		}

		@ParameterizedTest
		@ValueSource(floats = { 0f, -1f, -Float.MIN_VALUE, -Float.MAX_VALUE, Float.NEGATIVE_INFINITY })
		void testBreastHeightAgeLow(float breastHeightAge) {
			controlMap = VriTestUtils.loadControlMap();
			VriStart app = new VriStart();
			ApplicationTestUtils.setControlMap(app, controlMap);

			var polygon = VriPolygon.build(pBuilder -> {
				pBuilder.polygonIdentifier("Test", 2024);
				pBuilder.biogeoclimaticZone(Utils.getBec("IDF", controlMap));
				pBuilder.yieldFactor(1.0f);
				pBuilder.addLayer(lBuilder -> {
					lBuilder.layerType(LayerType.PRIMARY);
					lBuilder.crownClosure(57.8f);
					lBuilder.utilization(7.5f);
					lBuilder.empiricalRelationshipParameterIndex(61);

					lBuilder.addSpecies(sBuilder -> {
						sBuilder.genus("B", controlMap);
						sBuilder.percentGenus(10f);
					});
					lBuilder.addSpecies(sBuilder -> {
						sBuilder.genus("C", controlMap);
						sBuilder.percentGenus(20f);
					});
					lBuilder.addSpecies(sBuilder -> {
						sBuilder.genus("F", controlMap);
						sBuilder.percentGenus(30f);
					});
					lBuilder.addSpecies(sBuilder -> {
						sBuilder.genus("H", controlMap);
						sBuilder.percentGenus(30f);
					});
					lBuilder.addSpecies(sBuilder -> {
						sBuilder.genus("S", controlMap);
						sBuilder.percentGenus(10f);
					});

					lBuilder.primaryGenus("F");
				});
			});

			var species = polygon.getLayers().get(LayerType.PRIMARY).getSpecies().values();

			var bec = Utils.expectParsedControl(controlMap, ControlKey.BEC_DEF, BecLookup.class).get("IDF").get();

			var ex = assertThrows(
					FatalProcessingException.class,
					() -> app.estimateQuadMeanDiameterYield(7.6f, breastHeightAge, Optional.empty(), species, bec, 61)
			);

			assertThat(ex, hasProperty("message", endsWith(MessageFormat.format("{0,number}", breastHeightAge))));

		}

	}

	@Test
	void testModifyPrimaryLayerBuildLowDQ() throws IOException {
		var control = EasyMock.createControl();

		VriStart app = new VriStart();

		MockFileResolver resolver = dummyInput();

		TestUtils.populateControlMapGenusReal(controlMap);
		TestUtils.populateControlMapBecReal(controlMap);
		controlMap.put(
				ControlKey.DEFAULT_EQ_NUM.name(),
				new MatrixMap2Impl<Object, Object, Object>(List.of("D"), List.of("CDF"), (x, y) -> 42)
		);
		controlMap.put(
				ControlKey.EQN_MODIFIERS.name(),
				new MatrixMap2Impl<Object, Object, Object>(List.of(42), List.of(37), (x, y) -> 64)
		);

		var bec = Utils.getBec("CDF", controlMap);

		control.replay();

		app.init(resolver, controlMap);

		var spec = VriSpecies.build(sBuilder -> {
			sBuilder.polygonIdentifier("Test", 2025);
			sBuilder.layerType(LayerType.PRIMARY);
			sBuilder.genus("D", controlMap);
			sBuilder.percentGenus(100f);
		});

		var builder = new ca.bc.gov.nrs.vdyp.vri.model.VriLayer.Builder();
		builder.baseArea(2f);
		builder.treesPerHectare(1_000_000);
		app.modifyPrimaryLayerBuild(bec, builder, List.of(spec), 37);

		app.close();

		assertThat(builder.getBaseArea(), notPresent());
		assertThat(builder.getTreesPerHectare(), notPresent());

	}

	/**
	 * Matches a species entry with the given genus and a single species entry of 100%
	 *
	 * @param genus
	 * @param species
	 * @param percent
	 * @return
	 */
	@SuppressWarnings("unchecked")
	static Matcher<Map<? extends String, ? extends VriSpecies>> hasSpecies(
			Matcher<String> genus, Matcher<String> species, Matcher<Float> percent,
			Matcher<? super VriSpecies> additional
	) {
		return hasEntry(
				genus, //
				both(hasProperty("genus", genus)).and(hasProperty("percentGenus", percent))
						.and(
								hasProperty(
										"sp64DistributionSet",
										hasProperty(
												"sp64DistributionMap",
												allOf(
														aMapWithSize(1),
														hasEntry(
																is(1),
																allOf(
																		hasProperty("genusAlias", species),
																		hasProperty("percentage", is(100f))
																)
														)
												)
										)
								)
						).and((Matcher<? super Object>) additional)
		);
	}

	@SuppressWarnings("unchecked")
	static Matcher<Map<? extends String, ? extends VriSite>>
			hasSite(Matcher<String> genus, Matcher<String> species, Matcher<? super VriSite> additional) {
		return hasEntry(
				genus, //
				both(hasProperty("siteGenus", genus)) //
						.and(hasProperty("siteSpecies", species)) //
						.and((Matcher<? super Object>) additional)
		);
	}
}
