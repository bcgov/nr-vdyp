package ca.bc.gov.nrs.vdyp.fip;

import static ca.bc.gov.nrs.vdyp.test.TestUtils.polygonId;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.*;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.closeTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.application.ApplicationTestUtils;
import ca.bc.gov.nrs.vdyp.application.VdypApplicationIdentifier;
import ca.bc.gov.nrs.vdyp.application.VdypStartApplication;
import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.common.Utils;
import ca.bc.gov.nrs.vdyp.exceptions.HeightLowException;
import ca.bc.gov.nrs.vdyp.exceptions.LayerMissingException;
import ca.bc.gov.nrs.vdyp.exceptions.LayerSpeciesDoNotSumTo100PercentException;
import ca.bc.gov.nrs.vdyp.exceptions.ProcessingException;
import ca.bc.gov.nrs.vdyp.exceptions.SiteIndexLowException;
import ca.bc.gov.nrs.vdyp.exceptions.StandProcessingException;
import ca.bc.gov.nrs.vdyp.exceptions.TotalAgeLowException;
import ca.bc.gov.nrs.vdyp.exceptions.UnsupportedModeException;
import ca.bc.gov.nrs.vdyp.exceptions.YearsToBreastHeightLowException;
import ca.bc.gov.nrs.vdyp.fip.model.FipLayer;
import ca.bc.gov.nrs.vdyp.fip.model.FipLayerPrimary;
import ca.bc.gov.nrs.vdyp.fip.model.FipLayerPrimary.PrimaryBuilder;
import ca.bc.gov.nrs.vdyp.fip.model.FipPolygon;
import ca.bc.gov.nrs.vdyp.fip.model.FipSite;
import ca.bc.gov.nrs.vdyp.fip.model.FipSpecies;
import ca.bc.gov.nrs.vdyp.fip.test.FipTestUtils;
import ca.bc.gov.nrs.vdyp.io.parse.coe.BecDefinitionParser;
import ca.bc.gov.nrs.vdyp.io.parse.coe.GenusDefinitionParser;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.io.parse.control.BaseControlParser;
import ca.bc.gov.nrs.vdyp.io.parse.streaming.MockStreamingParser;
import ca.bc.gov.nrs.vdyp.io.parse.streaming.StreamingParserFactory;
import ca.bc.gov.nrs.vdyp.model.BecDefinition;
import ca.bc.gov.nrs.vdyp.model.Coefficients;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2;
import ca.bc.gov.nrs.vdyp.model.PolygonIdentifier;
import ca.bc.gov.nrs.vdyp.model.PolygonMode;
import ca.bc.gov.nrs.vdyp.model.Region;
import ca.bc.gov.nrs.vdyp.model.StockingClassFactor;
import ca.bc.gov.nrs.vdyp.model.VdypLayer;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;
import ca.bc.gov.nrs.vdyp.model.VdypSpecies;
import ca.bc.gov.nrs.vdyp.model.VdypUtilizationHolder;
import ca.bc.gov.nrs.vdyp.test.TestUtils;
import ca.bc.gov.nrs.vdyp.test.VdypMatchers;

class FipStartTest {

	@Test
	void testProcessEmpty() throws Exception {

		testWith(Arrays.asList(), Arrays.asList(), Arrays.asList(), (app, controlMap) -> {
			assertDoesNotThrow(app::process);
		});
	}

	@Test
	void testProcessSimple() throws Exception {

		var polygonId = polygonId("Test Polygon", 2023);
		var layer = LayerType.PRIMARY;

		// One polygon with one primary layer with one species entry
		testWith(
				FipTestUtils.loadControlMap(), Arrays.asList(getTestPolygon(polygonId, TestUtils.valid())), //
				Arrays.asList(layerMap(getTestPrimaryLayer(polygonId, TestUtils.valid(), TestUtils.valid()))), //
				Arrays.asList(Collections.singletonList(getTestSpecies(polygonId, layer, TestUtils.valid()))), //
				(app, controlMap) -> {
					assertDoesNotThrow(app::process);
				}
		);

	}

	@Test
	void testPolygonWithNoLayersRecord() throws Exception {

		var polygonId = polygonId("Test Polygon", 2023);

		testWith(
				Arrays.asList(getTestPolygon(polygonId, TestUtils.valid())), //
				Collections.emptyList(), //
				Collections.emptyList(), //
				(app, controlMap) -> {
					var ex = assertThrows(ProcessingException.class, () -> app.process());

					assertThat(ex, hasProperty("message", is("Layers file has fewer records than polygon file.")));

				}
		);
	}

	@Test
	void testPolygonWithNoSpeciesRecord() throws Exception {

		var polygonId = polygonId("Test Polygon", 2023);

		testWith(
				Arrays.asList(getTestPolygon(polygonId, TestUtils.valid())), //
				Arrays.asList(layerMap(getTestPrimaryLayer(polygonId, TestUtils.valid(), TestUtils.valid()))), //
				Collections.emptyList(), //
				(app, controlMap) -> {
					var ex = assertThrows(ProcessingException.class, () -> app.process());

					assertThat(ex, hasProperty("message", is("Species file has fewer records than polygon file.")));

				}
		);
	}

	@Test
	void testPolygonWithNoPrimaryLayer() throws Exception {

		// One polygon with one layer with one species entry, and type is VETERAN

		var controlMap = FipTestUtils.loadControlMap();
		try (var app = new FipStart()) {
			ApplicationTestUtils.setControlMap(app, controlMap);

			var polygonId = polygonId("Test Polygon", 2023);

			var polygon = getTestPolygon(polygonId, TestUtils.valid());
			var layer2 = getTestVeteranLayer(polygonId, TestUtils.valid(), siteBuilder -> {
				siteBuilder.height(9f);
			});
			polygon.setLayers(List.of(layer2));

			var ex = assertThrows(LayerMissingException.class, () -> app.checkPolygon(polygon));
			assertThat(ex, hasProperty("layer", is(LayerType.PRIMARY)));
		}
	}

	@Test
	void testPrimaryLayerHeightLessThanMinimum() throws Exception {

		var controlMap = FipTestUtils.loadControlMap();
		var polygonId = new PolygonIdentifier("TestPolygon", 2024);
		try (var app = new FipStart()) {
			ApplicationTestUtils.setControlMap(app, controlMap);

			var polygon = getTestPolygon(polygonId, TestUtils.valid());
			FipLayer layer = this.getTestPrimaryLayer(polygonId, TestUtils.valid(), sBuilder -> {
				sBuilder.height(4f);
			});
			polygon.setLayers(Collections.singletonMap(LayerType.PRIMARY, layer));

			var ex = assertThrows(HeightLowException.class, () -> app.checkPolygon(polygon));
			assertThat(ex, hasProperty("message", is("Height of 4 was lower than expected 5")));
			assertThat(ex, hasProperty("layer", is(LayerType.PRIMARY)));
			assertThat(ex, hasProperty("value", present(is(4f))));
			assertThat(ex, hasProperty("threshold", present(is(5f))));
		}

	}

	@Test
	void testVeteranLayerHeightLessThanMinimum() throws Exception {
		var controlMap = FipTestUtils.loadControlMap();
		try (var app = new FipStart()) {
			ApplicationTestUtils.setControlMap(app, controlMap);

			var polygonId = polygonId("Test Polygon", 2023);

			var polygon = getTestPolygon(polygonId, TestUtils.valid());
			var layer1 = getTestPrimaryLayer(polygonId, TestUtils.valid(), TestUtils.valid());
			var layer2 = getTestVeteranLayer(polygonId, TestUtils.valid(), sBuilder -> {
				sBuilder.height(9f);
			});
			polygon.setLayers(List.of(layer1, layer2));

			var ex = assertThrows(HeightLowException.class, () -> app.checkPolygon(polygon));

			assertThat(ex, hasProperty("layer", is(LayerType.VETERAN)));
			assertThat(ex, hasProperty("value", present(is(9f))));
			assertThat(ex, hasProperty("threshold", present(is(10f))));
		}

	}

	@Test
	void testPrimaryLayerYearsToBreastHeightLessThanMinimum() throws Exception {

		var controlMap = FipTestUtils.loadControlMap();
		try (var app = new FipStart()) {
			ApplicationTestUtils.setControlMap(app, controlMap);

			var polygonId = polygonId("Test Polygon", 2023);

			var polygon = getTestPolygon(polygonId, TestUtils.valid());
			var layer1 = getTestPrimaryLayer(polygonId, TestUtils.valid(), sBuilder -> {
				sBuilder.yearsToBreastHeight(0.2f);
			});
			polygon.setLayers(List.of(layer1));

			var ex = assertThrows(YearsToBreastHeightLowException.class, () -> app.checkPolygon(polygon));
			assertThat(ex, hasProperty("layer", is(LayerType.PRIMARY)));
			assertThat(ex, hasProperty("value", present(is(0.2f))));
			assertThat(ex, hasProperty("threshold", present(is(0.5f))));

		}
	}

	@Test
	void testPrimaryLayerTotalAgeLessThanYearsToBreastHeight() throws Exception {

		// FIXME VDYP7 actually tests if total age - YTBH is less than 0.5 but gives an
		// error that total age is "less than" YTBH. Replicating that for now but
		// consider changing it.

		var controlMap = FipTestUtils.loadControlMap();
		try (var app = new FipStart()) {
			ApplicationTestUtils.setControlMap(app, controlMap);

			var polygonId = polygonId("Test Polygon", 2023);

			var polygon = getTestPolygon(polygonId, TestUtils.valid());
			var layer1 = getTestPrimaryLayer(polygonId, TestUtils.valid(), siteBuilder -> {
				siteBuilder.ageTotal(7f);
				siteBuilder.yearsToBreastHeight(8f);
			});
			polygon.setLayers(List.of(layer1));

			var ex = assertThrows(TotalAgeLowException.class, () -> app.checkPolygon(polygon));

			assertThat(ex, hasProperty("layer", is(LayerType.PRIMARY)));
			assertThat(ex, hasProperty("value", present(is(7f))));
			assertThat(ex, hasProperty("threshold", present(is(8.5f))));

		}
	}

	@Test
	void testPrimaryLayerSiteIndexLessThanMinimum() throws Exception {

		var controlMap = FipTestUtils.loadControlMap();
		var polygonId = new PolygonIdentifier("TestPolygon", 2024);
		try (var app = new FipStart()) {
			ApplicationTestUtils.setControlMap(app, controlMap);

			var polygon = getTestPolygon(polygonId, TestUtils.valid());
			var layer = this.getTestPrimaryLayer(polygonId, TestUtils.valid(), siteBuilder -> {
				siteBuilder.siteIndex(0.2f);
			});
			polygon.setLayers(Collections.singletonMap(LayerType.PRIMARY, layer));

			var ex = assertThrows(SiteIndexLowException.class, () -> app.checkPolygon(polygon));
			assertThat(ex, hasProperty("layer", is(LayerType.PRIMARY)));
			assertThat(ex, hasProperty("value", present(is(0.2f))));
			assertThat(ex, hasProperty("threshold", present(is(0.5f))));

		}
	}

	@Test
	void testPolygonWithModeFipYoung() throws Exception {

		var controlMap = FipTestUtils.loadControlMap();
		var polygonId = new PolygonIdentifier("TestPolygon", 2024);
		try (var app = new FipStart()) {
			ApplicationTestUtils.setControlMap(app, controlMap);

			var polygon = getTestPolygon(polygonId, x -> {
				x.setMode(Optional.of(PolygonMode.YOUNG));
			});
			var layer = this.getTestPrimaryLayer(polygonId, TestUtils.valid(), TestUtils.valid());
			polygon.setLayers(List.of(layer));

			var ex = assertThrows(UnsupportedModeException.class, () -> app.checkPolygon(polygon));
			assertThat(ex, hasProperty("message", is("Mode YOUNG is not supported.")));
			assertThat(
					"IPASS code for FIP_START", ex.getIpassCode(VdypApplicationIdentifier.FIP_START), present(is(-4))
			);
			assertThat("IPASS code for VRI_START", ex.getIpassCode(VdypApplicationIdentifier.VRI_START), notPresent());
		}

	}

	@Test
	void testOneSpeciesLessThan100Percent() throws Exception {

		var controlMap = FipTestUtils.loadControlMap();
		try (var app = new FipStart()) {
			ApplicationTestUtils.setControlMap(app, controlMap);

			var polygonId = polygonId("Test Polygon", 2023);

			var polygon = getTestPolygon(polygonId, TestUtils.valid());
			var layer = this.getTestPrimaryLayer(polygonId, TestUtils.valid(), TestUtils.valid());
			var spec = getTestSpecies(polygonId, LayerType.PRIMARY, x -> {
				x.setPercentGenus(99f);
			});
			layer.setSpecies(List.of(spec));
			polygon.setLayers(List.of(layer));

			var ex = assertThrows(LayerSpeciesDoNotSumTo100PercentException.class, () -> app.checkPolygon(polygon));
			assertThat(ex, hasProperty("layer", is(LayerType.PRIMARY)));

		}

	}

	@Test
	void testOneSpeciesMoreThan100Percent() throws Exception {

		var controlMap = FipTestUtils.loadControlMap();
		try (var app = new FipStart()) {
			ApplicationTestUtils.setControlMap(app, controlMap);

			var polygonId = polygonId("Test Polygon", 2023);

			var polygon = getTestPolygon(polygonId, TestUtils.valid());
			var layer = this.getTestPrimaryLayer(polygonId, TestUtils.valid(), TestUtils.valid());
			var spec = getTestSpecies(polygonId, LayerType.PRIMARY, x -> {
				x.setPercentGenus(101f);
			});
			layer.setSpecies(List.of(spec));
			polygon.setLayers(List.of(layer));

			var ex = assertThrows(LayerSpeciesDoNotSumTo100PercentException.class, () -> app.checkPolygon(polygon));
			assertThat(ex, hasProperty("layer", is(LayerType.PRIMARY)));
		}

	}

	@Test
	void testTwoSpeciesSumTo100Percent() throws Exception {

		var controlMap = FipTestUtils.loadControlMap();
		try (var app = new FipStart()) {
			ApplicationTestUtils.setControlMap(app, controlMap);

			var polygonId = polygonId("Test Polygon", 2023);

			var polygon = getTestPolygon(polygonId, TestUtils.valid());
			var layer = this.getTestPrimaryLayer(polygonId, TestUtils.valid(), TestUtils.valid());
			var spec1 = getTestSpecies(polygonId, LayerType.PRIMARY, "B", 3, x -> {
				x.setPercentGenus(75f);
			});
			var spec2 = getTestSpecies(polygonId, LayerType.PRIMARY, "C", 4, x -> {
				x.setPercentGenus(25f);
			});
			layer.setSpecies(List.of(spec1, spec2));
			polygon.setLayers(List.of(layer));

			assertDoesNotThrow(() -> app.checkPolygon(polygon));
		}
	}

	@Test
	void testTwoSpeciesSumToLessThan100Percent() throws Exception {

		var controlMap = FipTestUtils.loadControlMap();
		try (var app = new FipStart()) {
			ApplicationTestUtils.setControlMap(app, controlMap);

			var polygonId = polygonId("Test Polygon", 2023);

			var polygon = getTestPolygon(polygonId, TestUtils.valid());
			var layer = this.getTestPrimaryLayer(polygonId, TestUtils.valid(), TestUtils.valid());
			var spec1 = getTestSpecies(polygonId, LayerType.PRIMARY, "B", 3, x -> {
				x.setPercentGenus(75f - 1f);
			});
			var spec2 = getTestSpecies(polygonId, LayerType.PRIMARY, "C", 4, x -> {
				x.setPercentGenus(25f);
			});
			layer.setSpecies(List.of(spec1, spec2));
			polygon.setLayers(List.of(layer));

			var ex = assertThrows(LayerSpeciesDoNotSumTo100PercentException.class, () -> app.checkPolygon(polygon));
			assertThat(ex, hasProperty("layer", is(LayerType.PRIMARY)));

		}

	}

	@Test
	void testTwoSpeciesSumToMoreThan100Percent() throws Exception {

		var controlMap = FipTestUtils.loadControlMap();
		try (var app = new FipStart()) {
			ApplicationTestUtils.setControlMap(app, controlMap);

			var polygonId = polygonId("Test Polygon", 2023);

			var polygon = getTestPolygon(polygonId, TestUtils.valid());
			var layer = this.getTestPrimaryLayer(polygonId, TestUtils.valid(), TestUtils.valid());
			var spec1 = getTestSpecies(polygonId, LayerType.PRIMARY, "B", 3, x -> {
				x.setPercentGenus(75f + 1f);
			});
			var spec2 = getTestSpecies(polygonId, LayerType.PRIMARY, "C", 4, x -> {
				x.setPercentGenus(25f);
			});
			layer.setSpecies(List.of(spec1, spec2));
			polygon.setLayers(List.of(layer));

			var ex = assertThrows(LayerSpeciesDoNotSumTo100PercentException.class, () -> app.checkPolygon(polygon));
			assertThat(ex, hasProperty("layer", is(LayerType.PRIMARY)));

		}

	}

	@Test
	void testFractionGenusCalculation() throws Exception {

		var polygonId = polygonId("Test Polygon", 2023);
		var layer = LayerType.PRIMARY;

		final var speciesList = Arrays.asList(
				//
				getTestSpecies(polygonId, layer, "B", 3, x -> {
					x.setPercentGenus(75f);
				}), getTestSpecies(polygonId, layer, "C", 4, x -> {
					x.setPercentGenus(25f);
				})
		);
		testWith(
				FipTestUtils.loadControlMap(), Arrays.asList(getTestPolygon(polygonId, TestUtils.valid())), //
				Arrays.asList(layerMap(getTestPrimaryLayer(polygonId, TestUtils.valid(), TestUtils.valid()))), //
				Arrays.asList(speciesList), //
				(app, controlMap) -> {

					app.process();

					// Testing exact floating point equality is intentional
					assertThat(
							speciesList, contains(
									//
									allOf(hasProperty("genus", is("B")), hasProperty("fractionGenus", is(0.75f))), //
									allOf(hasProperty("genus", is("C")), hasProperty("fractionGenus", is(0.25f)))//
							)
					);
				}
		);

	}

	@Test
	void testFractionGenusCalculationWithSlightError() throws Exception {

		var controlMap = FipTestUtils.loadControlMap();

		var polygon = FipPolygon.build(pb -> {
			pb.polygonIdentifier("Test Polygon", 2024);
			pb.forestInventoryZone("0");
			pb.biogeoclimaticZone(Utils.getBec("BG", controlMap));
			pb.mode(PolygonMode.START);
			pb.yieldFactor(1.0f);

			pb.addLayer(lb -> {
				lb.layerType(LayerType.PRIMARY);
				lb.crownClosure(0.9f);

				lb.addSpecies(sb -> {
					sb.genus("B", controlMap);
					sb.percentGenus(75f + 0.009f);
					sb.addSite(ib -> {
						ib.ageTotal(8f);
						ib.yearsToBreastHeight(7f);
						ib.height(6f);
						ib.siteIndex(5f);
						ib.siteSpecies("B");
					});
				});
				lb.addSpecies(sb -> {
					sb.genus("C", controlMap);
					sb.percentGenus(25f);
				});
			});

		});

		try (var app = new FipStart()) {
			ApplicationTestUtils.setControlMap(app, controlMap);

			app.checkPolygon(polygon);
			var speciesList = polygon.getLayers().get(LayerType.PRIMARY).getSpecies().values();
			assertThat(
					speciesList, containsInAnyOrder(
							//
							allOf(hasProperty("genus", is("B")), hasProperty("fractionGenus", is(0.75002253f))), //
							allOf(hasProperty("genus", is("C")), hasProperty("fractionGenus", is(0.2499775f)))//
					)
			);
		}

	}

	@Test
	void testProcessVeteran() throws Exception {

		var polygonId = polygonId("Test Polygon", 2023);

		var fipPolygon = getTestPolygon(polygonId, TestUtils.valid());
		var fipLayer = getTestVeteranLayer(polygonId, TestUtils.valid(), TestUtils.valid());
		var fipSpecies = getTestSpecies(polygonId, LayerType.VETERAN, x -> {
			x.setSpeciesPercentages(Collections.emptyMap());
		});
		fipPolygon.setLayers(Collections.singletonMap(LayerType.VETERAN, fipLayer));
		fipLayer.setSpecies(Collections.singletonMap(fipSpecies.getGenus(), fipSpecies));

		var controlMap = new HashMap<String, Object>();
		TestUtils.populateControlMapBecReal(controlMap);
		TestUtils.populateControlMapGenusReal(controlMap);
		TestUtils.populateControlMapVeteranBq(controlMap);
		TestUtils.populateControlMapEquationGroups(controlMap, (s, b) -> new int[] { 1, 1, 1 });
		TestUtils.populateControlMapVeteranDq(controlMap, (s, r) -> new float[] { 0f, 0f, 0f });
		TestUtils.populateControlMapVeteranVolAdjust(controlMap, s -> new float[] { 0f, 0f, 0f, 0f });
		TestUtils.populateControlMapWholeStemVolume(controlMap, TestUtils.wholeStemMap(1));
		TestUtils.populateControlMapCloseUtilization(controlMap, TestUtils.closeUtilMap(1));
		TestUtils.populateControlMapNetDecay(controlMap, TestUtils.closeUtilMap(1));
		TestUtils.populateControlMapDecayModifiers(controlMap, (s, r) -> 0f);
		TestUtils.populateControlMapNetWaste(
				controlMap, s -> new Coefficients(new float[] { 0f, 0f, 0f, 0f, 0f, 0f }, 0)
		);
		TestUtils.populateControlMapWasteModifiers(controlMap, (s, r) -> 0f);
		TestUtils
				.populateControlMapNetBreakage(controlMap, bgrp -> new Coefficients(new float[] { 0f, 0f, 0f, 0f }, 1));

		try (var app = new FipStart()) {
			ApplicationTestUtils.setControlMap(app, controlMap);

			var result = app.processLayerAsVeteran(fipPolygon, fipLayer);

			assertThat(result, notNullValue());

			// Keys
			assertThat(result, hasProperty("polygonIdentifier", is(polygonId)));
			assertThat(result, hasProperty("layerType", is(LayerType.VETERAN)));

			// Direct Copy
			assertThat(result, hasProperty("ageTotal", present(is(8f))));
			assertThat(result, hasProperty("height", present(is(6f))));
			assertThat(result, hasProperty("yearsToBreastHeight", present(is(7f))));

			// Computed
			assertThat(result, hasProperty("breastHeightAge", present(is(1f))));

			// Remap species
			assertThat(
					result, hasProperty(
							"species", allOf(
									aMapWithSize(1), //
									hasEntry(is("B"), instanceOf(VdypSpecies.class))//
							)
					)
			);
			var speciesResult = result.getSpecies().get("B");

			// Keys
			assertThat(speciesResult, hasProperty("polygonIdentifier", is(polygonId)));
			assertThat(speciesResult, hasProperty("layerType", is(LayerType.VETERAN)));
			assertThat(speciesResult, hasProperty("genus", is("B")));

			// Copied
			assertThat(speciesResult, hasProperty("percentGenus", is(100f)));

			// Species distribution
			assertThat(
					speciesResult, hasProperty("sp64DistributionSet", hasProperty("sp64DistributionMap", anEmptyMap()))
			);
		}
	}

	@Test
	void testProcessVeteranUtilization() throws Exception {

		var controlMap = FipTestUtils.loadControlMap();

		var polygonId = polygonId("Test Polygon", 2023);

		var fipPolygon = getTestPolygon(polygonId, x -> {
			x.setBiogeoclimaticZone(Utils.getBec("CWH", controlMap));
			x.setForestInventoryZone("A");
			x.setYieldFactor(1f);
		});
		var fipLayer = getTestVeteranLayer(polygonId, layerBuilder -> {
			layerBuilder.crownClosure(4f);
		}, siteBuilder -> {
			siteBuilder.siteCurveNumber(Optional.of(34));
			siteBuilder.height(26.2f);
			siteBuilder.siteIndex(16.7f);
			siteBuilder.yearsToBreastHeight(7.1f);
			siteBuilder.ageTotal(97.9f + 7.1f);
			siteBuilder.siteSpecies("H");
			siteBuilder.siteGenus("H");
		});
		var fipSpecies1 = getTestSpecies(polygonId, LayerType.VETERAN, "B", 3, x -> {
			x.setPercentGenus(22f);
		});
		var fipSpecies2 = getTestSpecies(polygonId, LayerType.VETERAN, "H", 8, x -> {
			x.setPercentGenus(60f);
		});
		var fipSpecies3 = getTestSpecies(polygonId, LayerType.VETERAN, "S", 15, x -> {
			x.setPercentGenus(18f);
		});
		fipPolygon.setLayers(Collections.singletonMap(LayerType.VETERAN, fipLayer));
		fipLayer.setSpecies(List.of(fipSpecies1, fipSpecies2, fipSpecies3));

		VdypLayer result;
		try (var app = new FipStart();) {
			ApplicationTestUtils.setControlMap(app, controlMap);

			result = app.processLayerAsVeteran(fipPolygon, fipLayer);
		}

		assertThat(result, notNullValue());

		// Keys
		assertThat(result, hasProperty("polygonIdentifier", is(polygonId)));
		assertThat(result, hasProperty("layerType", is(LayerType.VETERAN)));

		// Direct Copy
		assertThat(result, hasProperty("ageTotal", present(is(105f))));
		assertThat(result, hasProperty("height", present(is(26.2f))));
		assertThat(result, hasProperty("yearsToBreastHeight", present(is(7.1f))));

		// Computed
		assertThat(result, hasProperty("breastHeightAge", present(closeTo(97.9f))));

		// Remap species
		assertThat(
				result, hasProperty(
						"species", allOf(
								aMapWithSize(3), //
								hasEntry(is("B"), instanceOf(VdypSpecies.class)), //
								hasEntry(is("H"), instanceOf(VdypSpecies.class)), //
								hasEntry(is("S"), instanceOf(VdypSpecies.class))//
						)
				)
		);

		var speciesResult1 = result.getSpecies().get("B");

		// Keys
		assertThat(speciesResult1, hasProperty("polygonIdentifier", is(polygonId)));
		assertThat(speciesResult1, hasProperty("layerType", is(LayerType.VETERAN)));
		assertThat(speciesResult1, hasProperty("genus", is("B")));

		// Copied
		assertThat(speciesResult1, hasProperty("percentGenus", is(22f)));

		// Species distribution
		assertThat(
				speciesResult1, hasProperty("sp64DistributionSet", hasProperty("sp64DistributionMap", aMapWithSize(1)))
		);

		var speciesResult2 = result.getSpecies().get("H");

		// Keys
		assertThat(speciesResult2, hasProperty("polygonIdentifier", is(polygonId)));
		assertThat(speciesResult2, hasProperty("layerType", is(LayerType.VETERAN)));
		assertThat(speciesResult2, hasProperty("genus", is("H")));

		// Copied
		assertThat(speciesResult2, hasProperty("percentGenus", is(60f)));

		// Species distribution
		assertThat(
				speciesResult2, hasProperty("sp64DistributionSet", hasProperty("sp64DistributionMap", aMapWithSize(1)))
		);

		var speciesResult3 = result.getSpecies().get("S");

		// Keys
		assertThat(speciesResult3, hasProperty("polygonIdentifier", is(polygonId)));
		assertThat(speciesResult3, hasProperty("layerType", is(LayerType.VETERAN)));
		assertThat(speciesResult3, hasProperty("genus", is("S")));

		// Copied
		assertThat(speciesResult3, hasProperty("percentGenus", is(18f)));

		// Species distribution
		assertThat(
				speciesResult3, hasProperty("sp64DistributionSet", hasProperty("sp64DistributionMap", aMapWithSize(1)))
		);

		// These Utilizations should differ between the layer and each genus

		{
			var holder = speciesResult1;
			String reason = "Genus " + holder.getGenus();
			assertThat(
					reason, holder,
					hasProperty("baseAreaByUtilization", VdypMatchers.utilizationAllAndBiggest(0.492921442f))
			);
			assertThat(
					reason, holder,
					hasProperty("treesPerHectareByUtilization", VdypMatchers.utilizationAllAndBiggest(2.3357718f))
			);
			assertThat(
					reason, holder,
					hasProperty("wholeStemVolumeByUtilization", VdypMatchers.utilizationAllAndBiggest(6.11904192f))
			);
			assertThat(
					reason, holder,
					hasProperty(
							"closeUtilizationVolumeByUtilization", VdypMatchers.utilizationAllAndBiggest(5.86088896f)
					)
			);
			assertThat(
					reason, holder,
					hasProperty(
							"closeUtilizationVolumeNetOfDecayByUtilization",
							VdypMatchers.utilizationAllAndBiggest(5.64048958f)
					)
			);
			assertThat(
					reason, holder,
					hasProperty(
							"closeUtilizationVolumeNetOfDecayAndWasteByUtilization",
							VdypMatchers.utilizationAllAndBiggest(5.57935333f)
					)
			);
			assertThat(
					reason, holder,
					hasProperty(
							"closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization",
							VdypMatchers.utilizationAllAndBiggest(5.27515411f)
					)
			);
			assertThat(
					reason, holder,
					hasProperty(
							"quadraticMeanDiameterByUtilization", VdypMatchers.utilizationAllAndBiggest(51.8356705f)
					)
			);
		}
		{
			var holder = speciesResult2;
			String reason = "Genus " + holder.getGenus();
			assertThat(
					reason, holder,
					hasProperty("baseAreaByUtilization", VdypMatchers.utilizationAllAndBiggest(1.34433115f))
			);
			assertThat(
					reason, holder,
					hasProperty("treesPerHectareByUtilization", VdypMatchers.utilizationAllAndBiggest(5.95467329f))
			);
			assertThat(
					reason, holder,
					hasProperty("wholeStemVolumeByUtilization", VdypMatchers.utilizationAllAndBiggest(14.5863571f))
			);
			assertThat(
					reason, holder,
					hasProperty(
							"closeUtilizationVolumeByUtilization", VdypMatchers.utilizationAllAndBiggest(13.9343023f)
					)
			);
			assertThat(
					reason, holder,
					hasProperty(
							"closeUtilizationVolumeNetOfDecayByUtilization",
							VdypMatchers.utilizationAllAndBiggest(13.3831034f)
					)
			);
			assertThat(
					reason, holder,
					hasProperty(
							"closeUtilizationVolumeNetOfDecayAndWasteByUtilization",
							VdypMatchers.utilizationAllAndBiggest(13.2065458f)
					)
			);
			assertThat(
					reason, holder,
					hasProperty(
							"closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization",
							VdypMatchers.utilizationAllAndBiggest(12.4877129f)
					)
			);
			assertThat(
					reason, holder,
					hasProperty(
							"quadraticMeanDiameterByUtilization", VdypMatchers.utilizationAllAndBiggest(53.6141243f)
					)
			);
		}
		{
			var holder = speciesResult3;
			String reason = "Genus " + holder.getGenus();
			assertThat(
					reason, holder,
					hasProperty("baseAreaByUtilization", VdypMatchers.utilizationAllAndBiggest(0.403299361f))
			);
			assertThat(
					reason, holder,
					hasProperty("treesPerHectareByUtilization", VdypMatchers.utilizationAllAndBiggest(2.38468361f))
			);
			assertThat(
					reason, holder,
					hasProperty("wholeStemVolumeByUtilization", VdypMatchers.utilizationAllAndBiggest(4.04864883f))
			);
			assertThat(
					reason, holder,
					hasProperty(
							"closeUtilizationVolumeByUtilization", VdypMatchers.utilizationAllAndBiggest(3.81141663f)
					)
			);
			assertThat(
					reason, holder,
					hasProperty(
							"closeUtilizationVolumeNetOfDecayByUtilization",
							VdypMatchers.utilizationAllAndBiggest(3.75043678f)
					)
			);
			assertThat(
					reason, holder,
					hasProperty(
							"closeUtilizationVolumeNetOfDecayAndWasteByUtilization",
							VdypMatchers.utilizationAllAndBiggest(3.72647476f)
					)
			);
			assertThat(
					reason, holder,
					hasProperty(
							"closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization",
							VdypMatchers.utilizationAllAndBiggest(3.56433797f)
					)
			);
			assertThat(
					reason, holder,
					hasProperty(
							"quadraticMeanDiameterByUtilization", VdypMatchers.utilizationAllAndBiggest(46.4037895f)
					)
			);
		}
		{
			var holder = result;
			String reason = "Layer";
			assertThat(
					reason, holder,
					hasProperty("baseAreaByUtilization", VdypMatchers.utilizationAllAndBiggest(2.24055195f))
			);
			assertThat(
					reason, holder,
					hasProperty("treesPerHectareByUtilization", VdypMatchers.utilizationAllAndBiggest(10.6751289f))
			);
			assertThat(
					reason, holder,
					hasProperty("wholeStemVolumeByUtilization", VdypMatchers.utilizationAllAndBiggest(24.7540474f))
			);
			assertThat(
					reason, holder,
					hasProperty(
							"closeUtilizationVolumeByUtilization", VdypMatchers.utilizationAllAndBiggest(23.6066074f)
					)
			);
			assertThat(
					reason, holder,
					hasProperty(
							"closeUtilizationVolumeNetOfDecayByUtilization",
							VdypMatchers.utilizationAllAndBiggest(22.7740307f)
					)
			);
			assertThat(
					reason, holder,
					hasProperty(
							"closeUtilizationVolumeNetOfDecayAndWasteByUtilization",
							VdypMatchers.utilizationAllAndBiggest(22.5123749f)
					)
			);
			assertThat(
					reason, holder,
					hasProperty(
							"closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization",
							VdypMatchers.utilizationAllAndBiggest(21.3272057f)
					)
			);
			assertThat(
					reason, holder,
					hasProperty(
							"quadraticMeanDiameterByUtilization", VdypMatchers.utilizationAllAndBiggest(51.6946983f)
					)
			);
		}
		// Lorey Height should be the same across layer and each species
		for (var holder : List.of(speciesResult1, speciesResult2, speciesResult3, result)) {
			String reason;
			if (holder instanceof VdypLayer) {
				reason = "Layer";
			} else {
				reason = "Genus " + ((VdypSpecies) holder).getGenus();
			}
			assertThat(reason, holder, hasProperty("loreyHeightByUtilization", coe(-1, 0f, 26.2f)));
		}
	}

	@Test
	void testProcessVeteranYearsToBreastHeightLessThanMinimum() throws Exception {

		var polygonId = polygonId("Test Polygon", 2023);

		var fipPolygon = getTestPolygon(polygonId, TestUtils.valid());
		var fipLayer = getTestVeteranLayer(polygonId, TestUtils.valid(), siteBuilder -> {
			siteBuilder.yearsToBreastHeight(5.0f);
		});
		var fipSpecies = getTestSpecies(polygonId, LayerType.VETERAN, TestUtils.valid());
		fipPolygon.setLayers(Collections.singletonMap(LayerType.VETERAN, fipLayer));
		fipLayer.setSpecies(Collections.singletonMap(fipSpecies.getGenus(), fipSpecies));

		var controlMap = new HashMap<String, Object>();
		TestUtils.populateControlMapBecReal(controlMap);
		TestUtils.populateControlMapGenusReal(controlMap);
		TestUtils.populateControlMapVeteranBq(controlMap);
		TestUtils.populateControlMapEquationGroups(controlMap, (s, b) -> new int[] { 1, 1, 1 });
		TestUtils.populateControlMapVeteranDq(controlMap, (s, r) -> new float[] { 0f, 0f, 0f });
		TestUtils.populateControlMapVeteranVolAdjust(controlMap, s -> new float[] { 0f, 0f, 0f, 0f });
		TestUtils.populateControlMapWholeStemVolume(controlMap, (TestUtils.wholeStemMap(1)));
		TestUtils.populateControlMapCloseUtilization(controlMap, TestUtils.closeUtilMap(1));
		TestUtils.populateControlMapNetDecay(controlMap, TestUtils.closeUtilMap(1));
		TestUtils.populateControlMapDecayModifiers(controlMap, (s, r) -> 0f);
		TestUtils.populateControlMapNetWaste(
				controlMap, s -> new Coefficients(new float[] { 0f, 0f, 0f, 0f, 0f, 0f }, 0)
		);
		TestUtils.populateControlMapWasteModifiers(controlMap, (s, r) -> 0f);
		TestUtils
				.populateControlMapNetBreakage(controlMap, bgrp -> new Coefficients(new float[] { 0f, 0f, 0f, 0f }, 1));

		try (var app = new FipStart()) {
			ApplicationTestUtils.setControlMap(app, controlMap);

			var result = app.processLayerAsVeteran(fipPolygon, fipLayer);

			assertThat(result, notNullValue());

			// Set minimum
			assertThat(result, hasProperty("yearsToBreastHeight", present(is(6f))));

			// Computed based on minimum
			assertThat(result, hasProperty("breastHeightAge", present(is(2f))));
		}

	}

	@Test
	void testProcessVeteranWithSpeciesDistribution() throws Exception {

		var polygonId = polygonId("Test Polygon", 2023);

		var fipPolygon = getTestPolygon(polygonId, TestUtils.valid());
		var fipLayer = getTestVeteranLayer(polygonId, TestUtils.valid(), TestUtils.valid());
		var fipSpecies = getTestSpecies(polygonId, LayerType.VETERAN, x -> {
			var map = new LinkedHashMap<String, Float>();
			map.put("S1", 75f);
			map.put("S2", 25f);
			x.setSpeciesPercentages(map);
		});
		fipPolygon.setLayers(Collections.singletonMap(LayerType.VETERAN, fipLayer));
		fipLayer.setSpecies(Collections.singletonMap(fipSpecies.getGenus(), fipSpecies));

		var controlMap = new HashMap<String, Object>();
		TestUtils.populateControlMapBecReal(controlMap);
		TestUtils.populateControlMapGenusReal(controlMap);
		TestUtils.populateControlMapVeteranBq(controlMap);
		TestUtils.populateControlMapEquationGroups(controlMap, (s, b) -> new int[] { 1, 1, 1 });
		TestUtils.populateControlMapVeteranDq(controlMap, (s, r) -> new float[] { 0f, 0f, 0f });
		TestUtils.populateControlMapVeteranVolAdjust(controlMap, s -> new float[] { 0f, 0f, 0f, 0f });
		TestUtils.populateControlMapWholeStemVolume(controlMap, (TestUtils.wholeStemMap(1)));
		TestUtils.populateControlMapCloseUtilization(controlMap, TestUtils.closeUtilMap(1));
		TestUtils.populateControlMapNetDecay(controlMap, TestUtils.closeUtilMap(1));
		TestUtils.populateControlMapDecayModifiers(controlMap, (s, r) -> 0f);
		TestUtils.populateControlMapNetWaste(
				controlMap, s -> new Coefficients(new float[] { 0f, 0f, 0f, 0f, 0f, 0f }, 0)
		);
		TestUtils.populateControlMapWasteModifiers(controlMap, (s, r) -> 0f);
		TestUtils
				.populateControlMapNetBreakage(controlMap, bgrp -> new Coefficients(new float[] { 0f, 0f, 0f, 0f }, 1));

		try (var app = new FipStart()) {
			ApplicationTestUtils.setControlMap(app, controlMap);

			var result = app.processLayerAsVeteran(fipPolygon, fipLayer);

			assertThat(result, notNullValue());

			// Remap species
			assertThat(
					result, hasProperty(
							"species", allOf(
									aMapWithSize(1), //
									hasEntry(is("B"), instanceOf(VdypSpecies.class))//
							)
					)
			);
			var speciesResult = result.getSpecies().get("B");

			// Keys
			assertThat(speciesResult, hasProperty("polygonIdentifier", is(polygonId)));
			assertThat(speciesResult, hasProperty("layerType", is(LayerType.VETERAN)));
			assertThat(speciesResult, hasProperty("genus", is("B")));

			// Copied
			assertThat(speciesResult, hasProperty("percentGenus", is(100f)));

			// Species distribution
			assertThat(
					speciesResult,
					hasProperty("sp64DistributionSet", hasProperty("sp64DistributionMap", aMapWithSize(2)))
			);

			var distributionResult = speciesResult.getSp64DistributionSet();

			assertThat(
					distributionResult.getSp64DistributionMap(),
					allOf(
							hasEntry(
									is(1),
									allOf(hasProperty("genusAlias", is("S1")), hasProperty("percentage", is(75f)))
							),
							hasEntry(
									is(2),
									allOf(hasProperty("genusAlias", is("S2")), hasProperty("percentage", is(25f)))
							)
					)
			);
		}

	}

	@Test
	void testProcessPrimary() throws Exception {

		var controlMap = FipTestUtils.loadControlMap();

		var polygonId = polygonId("Test Polygon", 2023);

		var fipPolygon = getTestPolygon(polygonId, x -> {
			x.setBiogeoclimaticZone(Utils.getBec("CWH", controlMap));
			x.setForestInventoryZone("A");
		});
		var fipLayer = getTestPrimaryLayer(polygonId, layerBuilder -> {
			((FipLayerPrimary.Builder) layerBuilder).crownClosure(87.4f);
			((FipLayerPrimary.PrimaryBuilder) layerBuilder).primaryGenus("H");
			layerBuilder.inventoryTypeGroup(13);
		}, siteBuilder -> {
			siteBuilder.ageTotal(55f);
			siteBuilder.yearsToBreastHeight(1f);
			siteBuilder.height(35.3f);
			siteBuilder.siteIndex(5f);
			siteBuilder.siteGenus("D");
			siteBuilder.siteSpecies("D");
		});
		var fipSpecies1 = getTestSpecies(polygonId, LayerType.PRIMARY, "B", 3, x -> {
			x.setPercentGenus(1f);
		});
		var fipSpecies2 = getTestSpecies(polygonId, LayerType.PRIMARY, "C", 4, x -> {
			x.setPercentGenus(7f);
		});
		var fipSpecies3 = getTestSpecies(polygonId, LayerType.PRIMARY, "D", 5, x -> {
			x.setPercentGenus(74f);
		});
		var fipSpecies4 = getTestSpecies(polygonId, LayerType.PRIMARY, "H", 8, x -> {
			x.setPercentGenus(9f);
		});
		var fipSpecies5 = getTestSpecies(polygonId, LayerType.PRIMARY, "S", 15, x -> {
			x.setPercentGenus(9f);
		});
		fipPolygon.setLayers(List.of(fipLayer));
		fipLayer.setSpecies(List.of(fipSpecies1, fipSpecies2, fipSpecies3, fipSpecies4, fipSpecies5));

		try (var app = new FipStart()) {
			ApplicationTestUtils.setControlMap(app, controlMap);

			var result = app.processLayerAsPrimary(fipPolygon, fipLayer, 0f);

			assertThat(result, notNullValue());

			assertThat(result, hasProperty("polygonIdentifier", is(polygonId)));
			assertThat(result, hasProperty("layerType", is(LayerType.PRIMARY)));

			assertThat(result, hasProperty("ageTotal", present(is(55f))));
			assertThat(result, hasProperty("height", present(is(35.3f))));
			assertThat(result, hasProperty("yearsToBreastHeight", present(is(1f))));

			assertThat(result, hasProperty("breastHeightAge", present(is(54f))));

			assertThat(
					result,
					allOf(
							hasProperty(
									"loreyHeightByUtilization", VdypMatchers.utilizationHeight(7.14446497f, 31.3307228f)
							),
							hasProperty(
									"baseAreaByUtilization",
									VdypMatchers.utilization(
											0.0153773092f, 44.6249809f, 0.513127923f, 1.26773751f, 2.5276401f,
											40.3164787f
									)
							),
							hasProperty(
									"quadraticMeanDiameterByUtilization",
									VdypMatchers.utilization(
											6.05058956f, 30.2606678f, 10.208025f, 15.0549212f, 20.11759f, 35.5117531f
									)
							),
							hasProperty(
									"treesPerHectareByUtilization",
									VdypMatchers.utilization(
											5.34804535f, 620.484802f, 62.6977997f, 71.2168045f, 79.5194702f, 407.05072f
									)
							),
							hasProperty(
									"wholeStemVolumeByUtilization",
									VdypMatchers.utilization(
											0.0666879341f, 635.659668f, 2.66822577f, 9.68201256f, 26.5469246f,
											596.762512f
									)
							),

							// Ignore intermediate close volumes, if they are wrong,
							// closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization should also be
							// wrong

							hasProperty(
									"closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization",
									VdypMatchers.utilization(
											0f, 563.218933f, 0.414062887f, 7.01947737f, 22.6179276f, 533.16748f
									)
							)

					)
			);

			assertThat(
					result, hasProperty(
							"species", allOf(
									aMapWithSize(5), //
									hasEntry(is("B"), instanceOf(VdypSpecies.class)), //
									hasEntry(is("C"), instanceOf(VdypSpecies.class)), //
									hasEntry(is("D"), instanceOf(VdypSpecies.class)), //
									hasEntry(is("H"), instanceOf(VdypSpecies.class)), //
									hasEntry(is("S"), instanceOf(VdypSpecies.class))//
							)
					)
			);

			// Setting the primaryGenus on the FIP layer is a necessary side effect
			assertThat(fipLayer, hasProperty("primaryGenus", present(equalTo("D"))));

			var speciesResult = result.getSpecies().get("B");

			assertThat(speciesResult, hasProperty("polygonIdentifier", is(polygonId)));
			assertThat(speciesResult, hasProperty("layerType", is(LayerType.PRIMARY)));
			assertThat(speciesResult, hasProperty("genus", is("B")));

			assertThat(speciesResult, hasProperty("fractionGenus", closeTo(0.00890319888f)));

			assertThat(
					speciesResult,
					hasProperty("sp64DistributionSet", hasProperty("sp64DistributionMap", aMapWithSize(1)))
			);

			assertThat(
					speciesResult,
					allOf(
							hasProperty("loreyHeightByUtilization", coe(-1, 8.39441967f, 38.6004372f)),
							hasProperty(
									"baseAreaByUtilization",
									VdypMatchers.utilization(
											0f, 0.397305071f, 0.00485289097f, 0.0131751001f, 0.0221586525f, 0.357118428f
									)
							),
							hasProperty(
									"quadraticMeanDiameterByUtilization",
									VdypMatchers.utilization(
											6.13586617f, 31.6622887f, 9.17939758f, 13.6573782f, 18.2005272f, 42.1307297f
									)
							),
							hasProperty(
									"treesPerHectareByUtilization",
									VdypMatchers.utilization(
											0f, 5.04602766f, 0.733301044f, 0.899351299f, 0.851697803f, 2.56167722f
									)
							),
							hasProperty(
									"wholeStemVolumeByUtilization",
									VdypMatchers.utilization(
											0f, 6.35662031f, 0.0182443243f, 0.0747248605f, 0.172960356f, 6.09069061f
									)
							),

							// Ignore intermediate close volumes, if they are wrong,
							// closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization should also be
							// wrong

							hasProperty(
									"closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization",
									VdypMatchers.utilization(
											0f, 5.65764236f, 0.000855736958f, 0.046797853f, 0.143031254f, 5.46695757f
									)
							)

					)
			);

			speciesResult = result.getSpecies().get("C");

			assertThat(speciesResult, hasProperty("polygonIdentifier", is(polygonId)));
			assertThat(speciesResult, hasProperty("layerType", is(LayerType.PRIMARY)));
			assertThat(speciesResult, hasProperty("genus", is("C")));

			assertThat(speciesResult, hasProperty("fractionGenus", closeTo(0.114011094f)));

			assertThat(
					speciesResult,
					hasProperty("sp64DistributionSet", hasProperty("sp64DistributionMap", aMapWithSize(1)))
			);

			assertThat(
					speciesResult,
					allOf(
							hasProperty("loreyHeightByUtilization", coe(-1, 6.61517191f, 22.8001652f)),
							hasProperty(
									"baseAreaByUtilization",
									VdypMatchers.utilization(
											0.0131671466f, 5.08774281f, 0.157695293f, 0.365746498f, 0.565057278f,
											3.99924374f
									)
							),
							hasProperty(
									"quadraticMeanDiameterByUtilization",
									VdypMatchers.utilization(
											5.99067688f, 26.4735165f, 10.1137667f, 14.9345293f, 19.964777f, 38.7725677f
									)
							),
							hasProperty(
									"treesPerHectareByUtilization",
									VdypMatchers.utilization(
											4.67143154f, 92.4298019f, 19.6292171f, 20.8788815f, 18.0498524f, 33.8718452f
									)
							),
							hasProperty(
									"wholeStemVolumeByUtilization",
									VdypMatchers.utilization(
											0.0556972362f, 44.496151f, 0.78884691f, 2.40446854f, 4.43335152f,
											36.8694839f
									)
							),

							// Ignore intermediate close volumes, if they are wrong,
							// closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization should also be
							// wrong

							hasProperty(
									"closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization",
									VdypMatchers.utilization(
											0f, 33.6030083f, 0.138336331f, 1.6231581f, 3.49037051f, 28.3511429f
									)
							)

					)
			);

			speciesResult = result.getSpecies().get("D");

			assertThat(speciesResult, hasProperty("polygonIdentifier", is(polygonId)));
			assertThat(speciesResult, hasProperty("layerType", is(LayerType.PRIMARY)));
			assertThat(speciesResult, hasProperty("genus", is("D")));

			assertThat(speciesResult, hasProperty("fractionGenus", closeTo(0.661987007f)));

			assertThat(
					speciesResult,
					hasProperty("sp64DistributionSet", hasProperty("sp64DistributionMap", aMapWithSize(1)))
			);

			assertThat(
					speciesResult,
					allOf(
							hasProperty("loreyHeightByUtilization", coe(-1, 10.8831682f, 33.5375252f)),
							hasProperty(
									"baseAreaByUtilization",
									VdypMatchers.utilization(
											0.00163476227f, 29.5411568f, 0.0225830078f, 0.0963115692f, 0.748186111f,
											28.6740761f
									)
							),
							hasProperty(
									"quadraticMeanDiameterByUtilization",
									VdypMatchers.utilization(
											6.46009731f, 33.9255791f, 10.4784775f, 15.5708427f, 20.4805717f, 35.0954628f
									)
							),
							hasProperty(
									"treesPerHectareByUtilization",
									VdypMatchers.utilization(
											0.498754263f, 326.800781f, 2.61875916f, 5.05783129f, 22.7109661f,
											296.413239f
									)
							),
							hasProperty(
									"wholeStemVolumeByUtilization",
									VdypMatchers.utilization(
											0.0085867513f, 470.388489f, 0.182312608f, 1.08978188f, 10.1118069f,
											459.004578f
									)
							),

							// Ignore intermediate close volumes, if they are wrong,
							// closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization should also be
							// wrong

							hasProperty(
									"closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization",
									VdypMatchers.utilization(
											0f, 424.163849f, 0.0895428956f, 0.929004371f, 8.9712553f, 414.174042f
									)
							)

					)
			);

			speciesResult = result.getSpecies().get("H");

			assertThat(speciesResult, hasProperty("polygonIdentifier", is(polygonId)));
			assertThat(speciesResult, hasProperty("layerType", is(LayerType.PRIMARY)));
			assertThat(speciesResult, hasProperty("genus", is("H")));

			assertThat(speciesResult, hasProperty("fractionGenus", closeTo(0.123297341f)));

			assertThat(
					speciesResult,
					hasProperty("sp64DistributionSet", hasProperty("sp64DistributionMap", aMapWithSize(1)))
			);

			assertThat(
					speciesResult,
					allOf(
							hasProperty("loreyHeightByUtilization", coe(-1, 7.93716192f, 24.3451157f)),
							hasProperty(
									"baseAreaByUtilization",
									VdypMatchers.utilization(
											0f, 5.50214148f, 0.311808586f, 0.736046314f, 0.988982677f, 3.4653039f
									)
							),
							hasProperty(
									"quadraticMeanDiameterByUtilization",
									VdypMatchers.utilization(
											6.03505516f, 21.4343796f, 10.260808f, 15.0888424f, 20.0664616f, 32.2813988f
									)
							),
							hasProperty(
									"treesPerHectareByUtilization",
									VdypMatchers.utilization(
											0f, 152.482513f, 37.7081375f, 41.1626587f, 31.2721119f, 42.3395996f
									)
							),
							hasProperty(
									"wholeStemVolumeByUtilization",
									VdypMatchers.utilization(
											0f, 57.2091446f, 1.57991886f, 5.59581661f, 9.53606987f, 40.4973412f
									)
							),

							// Ignore intermediate close volumes, if they are wrong,
							// closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization should also be
							// wrong

							hasProperty(
									"closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization",
									VdypMatchers.utilization(
											0f, 48.1333618f, 0.168331802f, 4.01862335f, 8.05745506f, 35.8889503f
									)
							)

					)
			);

			speciesResult = result.getSpecies().get("S");

			assertThat(speciesResult, hasProperty("polygonIdentifier", is(polygonId)));
			assertThat(speciesResult, hasProperty("layerType", is(LayerType.PRIMARY)));
			assertThat(speciesResult, hasProperty("genus", is("S")));

			assertThat(speciesResult, hasProperty("fractionGenus", closeTo(0.0918014571f)));

			assertThat(
					speciesResult,
					hasProperty("sp64DistributionSet", hasProperty("sp64DistributionMap", aMapWithSize(1)))
			);

			assertThat(
					speciesResult,
					allOf(
							hasProperty("loreyHeightByUtilization", coe(-1, 8.63455391f, 34.6888771f)),
							hasProperty(
									"baseAreaByUtilization",
									VdypMatchers.utilization(
											0.000575399841f, 4.0966382f, 0.0161881447f, 0.0564579964f, 0.203255415f,
											3.82073665f
									)
							),
							hasProperty(
									"quadraticMeanDiameterByUtilization",
									VdypMatchers.utilization(
											6.41802597f, 34.5382729f, 10.1304808f, 14.9457884f, 19.7497196f, 39.0729332f
									)
							),
							hasProperty(
									"treesPerHectareByUtilization",
									VdypMatchers.utilization(
											0.17785944f, 43.7256737f, 2.00838566f, 3.21808815f, 6.63483906f, 31.8643608f
									)
							),
							hasProperty(
									"wholeStemVolumeByUtilization",
									VdypMatchers.utilization(
											0.00240394124f, 57.2092552f, 0.0989032984f, 0.517220974f, 2.29273605f,
											54.300396f
									)
							),

							// Ignore intermediate close volumes, if they are wrong,
							// closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization should also be
							// wrong

							hasProperty(
									"closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization",
									VdypMatchers.utilization(
											0f, 51.6610985f, 0.0169961192f, 0.401893795f, 1.95581412f, 49.286396f
									)
							)

					)
			);
		}
	}

	@Test
	void testProcessPrimaryWithOverstory() throws Exception {

		var controlMap = FipTestUtils.loadControlMap();

		var polygonId = polygonId("01002 S000002 00", 1970);

		var fipPolygon = getTestPolygon(polygonId, x -> {
			x.setBiogeoclimaticZone(Utils.getBec("CWH", controlMap));
			x.setForestInventoryZone("A");
		});
		var fipLayer = getTestPrimaryLayer(polygonId, x -> {
			((FipLayer.Builder) x).crownClosure(82.8f);
			((PrimaryBuilder) x).primaryGenus(Optional.empty());
			x.inventoryTypeGroup(Optional.empty());
		}, x -> {
			x.ageTotal(45f);
			x.height(24.3f);
			x.siteIndex(28.7f);
			x.siteGenus("H");
			x.siteSpecies("H");
			x.yearsToBreastHeight(5.4f);
			x.siteCurveNumber(34);
		});
		var fipSpecies1 = getTestSpecies(polygonId, LayerType.PRIMARY, "B", 3, x -> {
			x.setPercentGenus(15f);
		});
		var fipSpecies2 = getTestSpecies(polygonId, LayerType.PRIMARY, "D", 4, x -> {
			x.setPercentGenus(7f);
		});
		var fipSpecies3 = getTestSpecies(polygonId, LayerType.PRIMARY, "H", 8, x -> {
			x.setPercentGenus(77f);
		});
		var fipSpecies4 = getTestSpecies(polygonId, LayerType.PRIMARY, "S", 15, x -> {
			x.setPercentGenus(1f);
		});
		fipPolygon.setLayers(List.of(fipLayer));
		fipLayer.setSpecies(List.of(fipSpecies1, fipSpecies2, fipSpecies3, fipSpecies4));

		try (var app = new FipStart()) {
			ApplicationTestUtils.setControlMap(app, controlMap);

			var result = app.processLayerAsPrimary(fipPolygon, fipLayer, 2.24055195f);

			assertThat(result, notNullValue());

			assertThat(result, hasProperty("polygonIdentifier", is(polygonId)));
			assertThat(result, hasProperty("layerType", is(LayerType.PRIMARY)));

			assertThat(result, hasProperty("ageTotal", present(is(45f))));
			assertThat(result, hasProperty("height", present(is(24.3f))));
			assertThat(result, hasProperty("yearsToBreastHeight", present(is(5.4f))));

			assertThat(result, hasProperty("breastHeightAge", present(is(45f - 5.4f))));

			assertThat(
					result, hasProperty(
							"species", allOf(
									aMapWithSize(4), //
									hasEntry(is("B"), instanceOf(VdypSpecies.class)), //
									hasEntry(is("D"), instanceOf(VdypSpecies.class)), //
									hasEntry(is("H"), instanceOf(VdypSpecies.class)), //
									hasEntry(is("S"), instanceOf(VdypSpecies.class))
							)
					)
			);

			// Setting the primaryGenus on the FIP layer is a necessary side effect
			assertThat(fipLayer, hasProperty("primaryGenus", present(equalTo("H"))));

			var speciesResult = result.getSpecies().get("H");

			assertThat(speciesResult, hasProperty("polygonIdentifier", is(polygonId)));
			assertThat(speciesResult, hasProperty("layerType", is(LayerType.PRIMARY)));
			assertThat(speciesResult, hasProperty("genus", is("H")));

			assertThat(speciesResult, hasProperty("fractionGenus", closeTo(0.787526369f)));

			assertThat(
					speciesResult,
					hasProperty("sp64DistributionSet", hasProperty("sp64DistributionMap", aMapWithSize(1)))
			);

			assertThat(
					speciesResult,
					allOf(
							hasProperty(
									"loreyHeightByUtilization", VdypMatchers.utilizationHeight(7.00809479f, 20.9070625f)
							),
							hasProperty(
									"baseAreaByUtilization",
									VdypMatchers.utilization(
											0.512469947f, 35.401783f, 2.32033157f, 5.18892097f, 6.6573391f, 21.2351913f
									)
							),
							hasProperty(
									"quadraticMeanDiameterByUtilization",
									VdypMatchers.utilization(
											5.94023561f, 20.7426338f, 10.2836504f, 15.1184902f, 20.1040707f, 31.6741638f
									)
							),
							hasProperty(
									"treesPerHectareByUtilization",
									VdypMatchers.utilization(
											184.914597f, 1047.62891f, 279.36087f, 289.048248f, 209.72142f, 269.49826f
									)
							)

					)
			);

			assertThat(
					result,
					allOf(
							hasProperty("loreyHeightByUtilization", coe(-1, 7.01034021f, 21.1241722f)),
							hasProperty(
									"baseAreaByUtilization",
									VdypMatchers.utilization(
											0.553745031f, 44.9531403f, 2.83213019f, 6.17823505f, 8.11753464f,
											27.8252392f
									)
							),
							hasProperty(
									"quadraticMeanDiameterByUtilization",
									VdypMatchers.utilization(
											5.9399271f, 21.0548763f, 10.235322f, 15.0843554f, 20.0680523f, 32.0662689f
									)
							),
							hasProperty(
									"treesPerHectareByUtilization",
									VdypMatchers.utilization(
											199.828629f, 1291.1145f, 344.207489f, 345.717224f, 256.639709f, 344.549957f
									)
							)

					)
			);
		}
	}

	@Nested
	class FindPrimaryHeightPass {
		Map<String, Object> controlMap = FipTestUtils.loadControlMap();

		BecDefinition bec;

		FipStart app;

		@BeforeEach
		void setup() throws ProcessingException {
			app = new FipStart();
			ApplicationTestUtils.setControlMap(app, controlMap);
			bec = Utils.getBec("CWH", controlMap);
		}

		@AfterEach
		void teardown() throws IOException {
			app.close();
		}

		@Test
		void testPass1() throws ProcessingException {
			var targets = Utils.<String, Float>constMap(map -> {
				map.put("H", 9.0f);
				map.put("B", 1.0f);
				map.put("C", 7.0f);
				map.put("S", 9.0f);
				map.put("D", 74.0f);
			});
			var layer = VdypLayer.build(lb -> {
				lb.polygonIdentifier("Test", 2024);
				lb.layerType(LayerType.PRIMARY);

				lb.addSpecies(sb -> {
					sb.genus("B", controlMap);
					sb.percentGenus(1);

					sb.volumeGroup(12);
					sb.decayGroup(7);
					sb.breakageGroup(5);

					sb.addSp64Distribution("B", 100);
				});
				lb.addSpecies(sb -> {
					sb.genus("C", controlMap);
					sb.percentGenus(7);

					sb.volumeGroup(20);
					sb.decayGroup(14);
					sb.breakageGroup(6);

					sb.addSp64Distribution("C", 100);
				});
				lb.addSpecies(sb -> {
					sb.genus("S", controlMap);
					sb.percentGenus(9);

					sb.volumeGroup(66);
					sb.decayGroup(54);
					sb.breakageGroup(28);

					sb.addSp64Distribution("S", 100);
				});
				lb.addSpecies(sb -> {
					sb.genus("D", controlMap);
					sb.percentGenus(74);

					sb.volumeGroup(25);
					sb.decayGroup(19);
					sb.breakageGroup(12);

					sb.addSp64Distribution("D", 100);
				});
				lb.addSpecies(sb -> {
					sb.genus("H", controlMap);
					sb.percentGenus(9);

					sb.volumeGroup(37);
					sb.decayGroup(31);
					sb.breakageGroup(17);

					sb.addSp64Distribution("H", 100);
				});
			});
			var species = layer.getSpecies();
			var primary = species.get("D");
			app.findPrimaryHeightPass(bec, 620.50494f, species, primary, targets, 35.3f, 1);

			assertThat(
					species, hasEntry(
							is("B"), //
							hasProperty("loreyHeightByUtilization", utilizationHeight(0, 38.74565f))
					)
			);
			assertThat(
					species, hasEntry(
							is("C"), //
							hasProperty("loreyHeightByUtilization", utilizationHeight(0, 22.800163f))
					)
			);
			assertThat(
					species, hasEntry(
							is("S"), //
							hasProperty("loreyHeightByUtilization", utilizationHeight(0, 34.688877f))
					)
			);
			assertThat(
					species, hasEntry(
							is("D"), //
							hasProperty("loreyHeightByUtilization", utilizationHeight(0, 33.688976f))
					)
			);
			assertThat(
					species, hasEntry(
							is("H"), //
							hasProperty("loreyHeightByUtilization", utilizationHeight(0, 24.345116f))
					)
			);
		}

		@Test
		void testPass2() throws ProcessingException {
			var targets = Utils.<String, Float>constMap(map -> {
				map.put("H", 9.0f);
				map.put("B", 1.0f);
				map.put("C", 7.0f);
				map.put("S", 9.0f);
				map.put("D", 74.0f);
			});
			var layer = VdypLayer.build(lb -> {
				lb.polygonIdentifier("Test", 2024);
				lb.layerType(LayerType.PRIMARY);

				lb.addSpecies(sb -> {
					sb.genus("B", controlMap);
					sb.percentGenus(0.8918811f);

					sb.volumeGroup(12);
					sb.decayGroup(7);
					sb.breakageGroup(5);

					sb.addSp64Distribution("B", 100);

					sb.baseArea(0.3980018f);
					sb.loreyHeight(38.74565f);
					sb.quadMeanDiameter(31.716667f);
					sb.treesPerHectare(5.037558f);
					// sb.wholeStemVolume(6.3858347f);
				});
				lb.addSpecies(sb -> {
					sb.genus("C", controlMap);
					sb.percentGenus(11.449178f);

					sb.volumeGroup(20);
					sb.decayGroup(14);
					sb.breakageGroup(6);

					sb.addSp64Distribution("C", 100);

					sb.baseArea(5.1091933f);
					sb.loreyHeight(22.800163f);
					sb.quadMeanDiameter(26.453901f);
					sb.treesPerHectare(92.95719f);
					// sb.wholeStemVolume(44.700314f);
				});
				lb.addSpecies(sb -> {
					sb.genus("S", controlMap);
					sb.percentGenus(9.215943f);

					sb.volumeGroup(66);
					sb.decayGroup(54);
					sb.breakageGroup(28);

					sb.addSp64Distribution("S", 100);

					sb.baseArea(4.1126127f);
					sb.loreyHeight(34.688877f);
					sb.quadMeanDiameter(34.462196f);
					sb.treesPerHectare(44.0902f);
					// sb.wholeStemVolume(57.47183f);
				});
				lb.addSpecies(sb -> {
					sb.genus("D", controlMap);
					sb.percentGenus(66.05741f);

					sb.volumeGroup(25);
					sb.decayGroup(19);
					sb.breakageGroup(12);

					sb.addSp64Distribution("D", 100);

					sb.baseArea(29.478107f);
					sb.loreyHeight(33.688976f);
					sb.quadMeanDiameter(33.973206f);
					sb.treesPerHectare(325.1896f);
					// sb.wholeStemVolume(472.54596f);
				});
				lb.addSpecies(sb -> {
					sb.genus("H", controlMap);
					sb.percentGenus(12.385582f);

					sb.volumeGroup(37);
					sb.decayGroup(31);
					sb.breakageGroup(17);

					sb.addSp64Distribution("H", 100);

					sb.baseArea(5.5270634f);
					sb.loreyHeight(24.345116f);
					sb.quadMeanDiameter(21.430225f);
					sb.treesPerHectare(153.23257f);
					// sb.wholeStemVolume(57.471436f);
				});
			});
			var species = layer.getSpecies();
			var primary = species.get("D");
			app.findPrimaryHeightPass(bec, 620.50494f, species, primary, targets, 35.3f, 2);

			assertThat(
					species, hasEntry(
							is("B"), //
							hasProperty("loreyHeightByUtilization", utilizationHeight(0, 38.600403f))
					)
			);
			assertThat(
					species, hasEntry(
							is("C"), //
							hasProperty("loreyHeightByUtilization", utilizationHeight(0, 22.800163f))
					)
			);
			assertThat(
					species, hasEntry(
							is("S"), //
							hasProperty("loreyHeightByUtilization", utilizationHeight(0, 34.688877f))
					)
			);
			assertThat(
					species, hasEntry(
							is("D"), //
							hasProperty("loreyHeightByUtilization", utilizationHeight(0, 33.53749f))
					)
			);
			assertThat(
					species, hasEntry(
							is("H"), //
							hasProperty("loreyHeightByUtilization", utilizationHeight(0, 24.345116f))
					)
			);
		}
	}

	@Test
	void testEstimateVeteranLayerBaseArea() throws Exception {

		var controlMap = FipTestUtils.loadControlMap();

		try (var app = new FipStart()) {
			ApplicationTestUtils.setControlMap(app, controlMap);

			var result = app.estimateVeteranBaseArea(26.2000008f, 4f, "H", Region.COASTAL);

			assertThat(result, closeTo(2.24055195f));
		}
	}

	void populateControlMapVeteranVolumeAdjust(HashMap<String, Object> controlMap, Function<String, float[]> mapper) {
		var map = GenusDefinitionParser.getSpeciesAliases(controlMap).stream()
				.collect(Collectors.toMap(x -> x, mapper.andThen(x -> new Coefficients(x, 1))));

		controlMap.put(ControlKey.VETERAN_LAYER_VOLUME_ADJUST.name(), map);
	}

	@Test
	void testVeteranLayerLoreyHeight() throws Exception {

		var polygonId = polygonId("Test Polygon", 2023);

		var fipPolygon = getTestPolygon(polygonId, TestUtils.valid());
		var fipLayer = getTestVeteranLayer(polygonId, TestUtils.valid(), TestUtils.valid());
		var fipSpecies = getTestSpecies(polygonId, LayerType.VETERAN, x -> {
			var map = new LinkedHashMap<String, Float>();
			map.put("B", 100f);
			x.setSpeciesPercentages(map);
		});
		fipPolygon.setLayers(Collections.singletonMap(LayerType.VETERAN, fipLayer));
		fipLayer.setSpecies(Collections.singletonMap(fipSpecies.getGenus(), fipSpecies));

		var controlMap = new HashMap<String, Object>();
		TestUtils.populateControlMapBecReal(controlMap);
		TestUtils.populateControlMapGenusReal(controlMap);
		TestUtils.populateControlMapVeteranBq(controlMap);
		TestUtils.populateControlMapEquationGroups(controlMap, (s, b) -> new int[] { 1, 1, 1 });
		TestUtils.populateControlMapVeteranDq(controlMap, (s, r) -> new float[] { 0f, 0f, 0f });
		TestUtils.populateControlMapVeteranVolAdjust(controlMap, s -> new float[] { 0f, 0f, 0f, 0f });
		TestUtils.populateControlMapWholeStemVolume(controlMap, (TestUtils.wholeStemMap(1)));
		TestUtils.populateControlMapCloseUtilization(controlMap, TestUtils.closeUtilMap(1));
		TestUtils.populateControlMapNetDecay(controlMap, TestUtils.closeUtilMap(1));
		TestUtils.populateControlMapDecayModifiers(controlMap, (s, r) -> 0f);
		TestUtils.populateControlMapNetWaste(
				controlMap, s -> new Coefficients(new float[] { 0f, 0f, 0f, 0f, 0f, 0f }, 0)
		);
		TestUtils.populateControlMapWasteModifiers(controlMap, (s, r) -> 0f);
		TestUtils
				.populateControlMapNetBreakage(controlMap, bgrp -> new Coefficients(new float[] { 0f, 0f, 0f, 0f }, 1));

		try (var app = new FipStart()) {
			ApplicationTestUtils.setControlMap(app, controlMap);

			var result = app.processLayerAsVeteran(fipPolygon, fipLayer);

			Matcher<Float> heightMatcher = closeTo(6f);
			Matcher<Float> zeroMatcher = is(0.0f);
			// Expect the estimated HL in 0 (-1 to 0)
			assertThat(
					result,
					hasProperty(
							"species",
							hasEntry(
									is("B"),
									hasProperty("loreyHeightByUtilization", contains(zeroMatcher, heightMatcher))
							)
					)
			);
		}

	}

	@Test
	void testVeteranLayerEquationGroups() throws Exception {

		var polygonId = polygonId("Test Polygon", 2023);

		var fipPolygon = getTestPolygon(polygonId, TestUtils.valid());
		var fipLayer = getTestVeteranLayer(polygonId, TestUtils.valid(), TestUtils.valid());
		var fipSpecies = getTestSpecies(polygonId, LayerType.VETERAN, x -> {
			var map = new LinkedHashMap<String, Float>();
			map.put("B", 100f);
			x.setSpeciesPercentages(map);
		});
		fipPolygon.setLayers(Collections.singletonMap(LayerType.VETERAN, fipLayer));
		fipLayer.setSpecies(Collections.singletonMap(fipSpecies.getGenus(), fipSpecies));

		var controlMap = new HashMap<String, Object>();
		TestUtils.populateControlMapBecReal(controlMap);
		TestUtils.populateControlMapGenusReal(controlMap);
		TestUtils.populateControlMapVeteranBq(controlMap);
		TestUtils.populateControlMapEquationGroups(controlMap, (s, b) -> {
			if (s.equals("B") && b.equals("BG"))
				return new int[] { 1, 2, 3 };
			if (s.equals("B") && b.equals("ESSF"))
				return new int[] { 4, 5, 6 };
			return new int[] { 0, 0, 0 };
		});
		TestUtils.populateControlMapVeteranDq(controlMap, (s, r) -> new float[] { 0f, 0f, 0f });
		TestUtils.populateControlMapVeteranVolAdjust(controlMap, s -> new float[] { 0f, 0f, 0f, 0f });
		TestUtils.populateControlMapWholeStemVolume(controlMap, TestUtils.wholeStemMap(4));
		TestUtils.populateControlMapCloseUtilization(controlMap, TestUtils.closeUtilMap(4));
		TestUtils.populateControlMapNetDecay(controlMap, TestUtils.closeUtilMap(2));
		TestUtils.populateControlMapDecayModifiers(controlMap, (s, r) -> 0f);
		TestUtils.populateControlMapNetWaste(
				controlMap, s -> new Coefficients(new float[] { 0f, 0f, 0f, 0f, 0f, 0f }, 0)
		);
		TestUtils.populateControlMapWasteModifiers(controlMap, (s, r) -> 0f);
		TestUtils
				.populateControlMapNetBreakage(controlMap, bgrp -> new Coefficients(new float[] { 0f, 0f, 0f, 0f }, 1));

		try (var app = new FipStart()) {
			ApplicationTestUtils.setControlMap(app, controlMap);

			var result = app.processLayerAsVeteran(fipPolygon, fipLayer).getSpecies().get("B");

			assertThat(result, hasProperty("volumeGroup", is(4))); // Remapped BEC to ESSF
			assertThat(result, hasProperty("decayGroup", is(2)));
			assertThat(result, hasProperty("breakageGroup", is(3)));
		}

	}

	@Test
	void testEstimateVeteranLayerDQ() throws Exception {

		var polygonId = polygonId("Test Polygon", 2023);

		var fipPolygon = getTestPolygon(polygonId, TestUtils.valid());
		var fipLayer = getTestVeteranLayer(polygonId, TestUtils.valid(), x -> {
			x.height(10f);
		});
		var fipSpecies1 = getTestSpecies(polygonId, LayerType.VETERAN, "B", 3, x -> {
			var map = new LinkedHashMap<String, Float>();
			map.put("S1", 75f);
			map.put("S2", 25f);
			x.setSpeciesPercentages(map);
			x.setPercentGenus(60f);
		});
		var fipSpecies2 = getTestSpecies(polygonId, LayerType.VETERAN, "C", 4, x -> {
			var map = new LinkedHashMap<String, Float>();
			map.put("S3", 75f);
			map.put("S4", 25f);
			x.setSpeciesPercentages(map);
			x.setPercentGenus(40f);
		});
		fipPolygon.setLayers(Collections.singletonMap(LayerType.VETERAN, fipLayer));
		var speciesMap = new HashMap<String, FipSpecies>();
		speciesMap.put("B", fipSpecies1);
		speciesMap.put("C", fipSpecies2);
		fipLayer.setSpecies(speciesMap);

		var controlMap = new HashMap<String, Object>();
		TestUtils.populateControlMapBecReal(controlMap);
		TestUtils.populateControlMapGenusReal(controlMap);
		TestUtils.populateControlMapEquationGroups(controlMap, (s, b) -> new int[] { 1, 1, 1 });
		TestUtils.populateControlMapVeteranBq(controlMap);
		TestUtils.populateControlMapVeteranDq(controlMap, (s, r) -> {
			if (s.equals("B") && r == Region.INTERIOR)
				return new float[] { 19.417f, 0.04354f, 1.96395f };
			else if (s.equals("C") && r == Region.INTERIOR)
				return new float[] { 22.500f, 0.00157f, 2.96382f };
			return new float[] { 0f, 0f, 0f };
		});
		TestUtils.populateControlMapVeteranVolAdjust(controlMap, s -> new float[] { 0f, 0f, 0f, 0f });
		TestUtils.populateControlMapWholeStemVolume(controlMap, (TestUtils.wholeStemMap(1)));
		TestUtils.populateControlMapCloseUtilization(controlMap, TestUtils.closeUtilMap(1));
		TestUtils.populateControlMapNetDecay(controlMap, TestUtils.closeUtilMap(1));
		TestUtils.populateControlMapDecayModifiers(controlMap, (s, r) -> 0f);
		TestUtils.populateControlMapNetWaste(
				controlMap, s -> new Coefficients(new float[] { 0f, 0f, 0f, 0f, 0f, 0f }, 0)
		);
		TestUtils.populateControlMapWasteModifiers(controlMap, (s, r) -> 0f);
		TestUtils
				.populateControlMapNetBreakage(controlMap, bgrp -> new Coefficients(new float[] { 0f, 0f, 0f, 0f }, 1));

		try (var app = new FipStart()) {
			ApplicationTestUtils.setControlMap(app, controlMap);

			var result = app.processLayerAsVeteran(fipPolygon, fipLayer);

			Matcher<Float> zeroMatcher = is(0.0f);
			// Expect the estimated DQ in 4 (-1 to 4)

			var expectedDqB = 19.417f + 0.04354f * (float) Math.pow(10f, 1.96395f);
			var expectedDqC = 22.500f + 0.00157f * (float) Math.pow(10f, 2.96382);

			var resultB = result.getSpecies().get("B");

			assertThat(
					resultB,
					hasProperty(
							"quadraticMeanDiameterByUtilization",
							contains(
									zeroMatcher, closeTo(expectedDqB), zeroMatcher, zeroMatcher, zeroMatcher,
									closeTo(expectedDqB)
							)
					)
			);
			assertThat(
					resultB,
					hasProperty(
							"treesPerHectareByUtilization",
							contains(
									zeroMatcher, closeTo(3.8092144f), zeroMatcher, zeroMatcher, zeroMatcher,
									closeTo(3.8092144f)
							)
					)
			);
			var resultC = result.getSpecies().get("C");
			assertThat(
					resultC,
					hasProperty(
							"quadraticMeanDiameterByUtilization",
							contains(
									zeroMatcher, closeTo(expectedDqC), zeroMatcher, zeroMatcher, zeroMatcher,
									closeTo(expectedDqC)
							)
					)
			);
			assertThat(
					resultC,
					hasProperty(
							"treesPerHectareByUtilization",
							contains(
									zeroMatcher, closeTo(2.430306f), zeroMatcher, zeroMatcher, zeroMatcher,
									closeTo(2.430306f)
							)
					)
			);
		}
	}

	@Test
	void testProcessAsVeteranLayer() throws Exception {

		var controlMap = FipTestUtils.loadControlMap();

		var polygonId = PolygonIdentifier.split("01002 S000002 00     1970");

		var fipPolygon = getTestPolygon(polygonId, x -> {
			x.setBiogeoclimaticZone(Utils.getBec("CWH", controlMap));
			x.setForestInventoryZone("A");
			x.setYieldFactor(1f);
		});

		var fipLayer = getTestVeteranLayer(polygonId, x -> {
			((FipLayer.Builder) x).crownClosure(4.0f);
		}, x -> {
			x.ageTotal(105f);
			x.height(26.2f);
			x.siteIndex(16.7f);
			x.siteGenus("H");
			x.siteSpecies("H");
			x.yearsToBreastHeight(7.1f);
		});
		var fipSpecies1 = getTestSpecies(polygonId, LayerType.VETERAN, "B", 3, x -> {
			var map = new LinkedHashMap<String, Float>();
			x.setSpeciesPercentages(map);
			x.setPercentGenus(22f);
		});
		var fipSpecies2 = getTestSpecies(polygonId, LayerType.VETERAN, "H", 8, x -> {
			var map = new LinkedHashMap<String, Float>();
			x.setSpeciesPercentages(map);
			x.setPercentGenus(60f);
		});
		var fipSpecies3 = getTestSpecies(polygonId, LayerType.VETERAN, "S", 15, x -> {
			var map = new LinkedHashMap<String, Float>();
			x.setSpeciesPercentages(map);
			x.setPercentGenus(18f);
		});
		fipPolygon.setLayers(Collections.singletonMap(LayerType.VETERAN, fipLayer));
		var speciesMap = new HashMap<String, FipSpecies>();
		speciesMap.put("B", fipSpecies1);
		speciesMap.put("H", fipSpecies2);
		speciesMap.put("S", fipSpecies3);
		fipLayer.setSpecies(speciesMap);

		try (var app = new FipStart()) {
			ApplicationTestUtils.setControlMap(app, controlMap);

			var result = app.processLayerAsVeteran(fipPolygon, fipLayer);

			assertThat(result, hasProperty("polygonIdentifier", is(polygonId)));
			assertThat(result, hasProperty("layerType", is(LayerType.VETERAN)));

			assertThat(result, hasProperty("ageTotal", present(closeTo(105f)))); // LVCOM3/AGETOTLV
			assertThat(result, hasProperty("breastHeightAge", present(closeTo(97.9000015f)))); // LVCOM3/AGEBHLV
			assertThat(result, hasProperty("yearsToBreastHeight", present(closeTo(7.0999999f)))); // LVCOM3/YTBHLV
			assertThat(result, hasProperty("height", present(closeTo(26.2000008f)))); // LVCOM3/HDLV

			assertThat(result, hasProperty("species", aMapWithSize(3)));
			var resultSpeciesMap = result.getSpecies();

			assertThat(resultSpeciesMap, Matchers.hasKey("B"));
			assertThat(resultSpeciesMap, Matchers.hasKey("H"));
			assertThat(resultSpeciesMap, Matchers.hasKey("S"));

			var resultSpeciesB = resultSpeciesMap.get("B");
			var resultSpeciesH = resultSpeciesMap.get("H");
			var resultSpeciesS = resultSpeciesMap.get("S");

			assertThat(resultSpeciesB, hasProperty("genus", is("B")));
			assertThat(resultSpeciesH, hasProperty("genus", is("H")));
			assertThat(resultSpeciesS, hasProperty("genus", is("S")));

			vetUtilization("baseAreaByUtilization", matchGenerator -> {
				assertThat(result, matchGenerator.apply(2.24055195f));
				assertThat(result.getSpecies().get("B"), matchGenerator.apply(0.492921442f));
				assertThat(result.getSpecies().get("H"), matchGenerator.apply(1.34433115f));
				assertThat(result.getSpecies().get("S"), matchGenerator.apply(0.403299361f));
			});
			vetUtilization("quadraticMeanDiameterByUtilization", matchGenerator -> {
				assertThat(result, matchGenerator.apply(51.6946983f));
				assertThat(result.getSpecies().get("B"), matchGenerator.apply(51.8356705f));
				assertThat(result.getSpecies().get("H"), matchGenerator.apply(53.6141243f));
				assertThat(result.getSpecies().get("S"), matchGenerator.apply(46.4037895f));
			});
			vetUtilization("treesPerHectareByUtilization", matchGenerator -> {
				assertThat(result, matchGenerator.apply(10.6751289f));
				assertThat(result.getSpecies().get("B"), matchGenerator.apply(2.3357718f));
				assertThat(result.getSpecies().get("H"), matchGenerator.apply(5.95467329f));
				assertThat(result.getSpecies().get("S"), matchGenerator.apply(2.38468361f));
			});
			vetUtilization("wholeStemVolumeByUtilization", matchGenerator -> {
				assertThat(result.getSpecies().get("B"), matchGenerator.apply(6.11904192f));
				assertThat(result.getSpecies().get("H"), matchGenerator.apply(14.5863571f));
				assertThat(result.getSpecies().get("S"), matchGenerator.apply(4.04864883f));
				assertThat(result, matchGenerator.apply(24.7540474f));
			});
			vetUtilization("closeUtilizationVolumeByUtilization", matchGenerator -> {
				assertThat(result, matchGenerator.apply(23.6066074f));
				assertThat(result.getSpecies().get("B"), matchGenerator.apply(5.86088896f));
				assertThat(result.getSpecies().get("H"), matchGenerator.apply(13.9343023f));
				assertThat(result.getSpecies().get("S"), matchGenerator.apply(3.81141663f));
			});
			vetUtilization("closeUtilizationVolumeNetOfDecayByUtilization", matchGenerator -> {
				assertThat(result, matchGenerator.apply(22.7740307f));
				assertThat(result.getSpecies().get("B"), matchGenerator.apply(5.64048958f));
				assertThat(result.getSpecies().get("H"), matchGenerator.apply(13.3831034f));
				assertThat(result.getSpecies().get("S"), matchGenerator.apply(3.75043678f));
			});
			vetUtilization("closeUtilizationVolumeNetOfDecayAndWasteByUtilization", matchGenerator -> {
				assertThat(result, matchGenerator.apply(22.5123749f));
				assertThat(result.getSpecies().get("B"), matchGenerator.apply(5.57935333f));
				assertThat(result.getSpecies().get("H"), matchGenerator.apply(13.2065458f));
				assertThat(result.getSpecies().get("S"), matchGenerator.apply(3.72647476f));
			});
			vetUtilization("closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization", matchGenerator -> {
				assertThat(result, matchGenerator.apply(21.3272057f));
				assertThat(result.getSpecies().get("B"), matchGenerator.apply(5.27515411f));
				assertThat(result.getSpecies().get("H"), matchGenerator.apply(12.4877129f));
				assertThat(result.getSpecies().get("S"), matchGenerator.apply(3.56433797f));
			});
		}

	}

	void vetUtilization(String property, Consumer<Function<Float, Matcher<VdypUtilizationHolder>>> body) {
		Function<Float, Matcher<VdypUtilizationHolder>> generator = v -> hasProperty(
				property, coe(-1, contains(is(0f), closeTo(v), is(0f), is(0f), is(0f), closeTo(v)))
		);
		body.accept(generator);
	}

	@Test
	void testFindRootsForPrimaryLayerDiameterAndAreaOneSpecies() throws Exception {
		var controlMap = FipTestUtils.loadControlMap();
		var polygonId = new PolygonIdentifier("TestPolygon", 2024);
		try (var app = new FipStart()) {
			ApplicationTestUtils.setControlMap(app, controlMap);

			var becLookup = BecDefinitionParser.getBecs(controlMap);
			var bec = becLookup.get("CWH").get();

			var layer = VdypLayer.build(builder -> {
				builder.polygonIdentifier(polygonId);
				builder.layerType(LayerType.PRIMARY);

			});
			layer.getBaseAreaByUtilization().setCoe(0, 76.5122147f);
			layer.getTreesPerHectareByUtilization().setCoe(0, 845.805969f);
			layer.getQuadraticMeanDiameterByUtilization().setCoe(0, 33.9379082f);

			var spec = VdypSpecies.build(layer, builder -> {
				builder.genus("Y", controlMap);
				builder.percentGenus(100f);
				builder.volumeGroup(74);
				builder.decayGroup(63);
				builder.breakageGroup(31);
				builder.addSite(siteBuilder -> {
					siteBuilder.ageTotal(285f);
					siteBuilder.yearsToBreastHeight(11.3999996f);
					siteBuilder.height(24.3999996f);
				});
			});
			spec.getLoreyHeightByUtilization().setCoe(0, 19.9850883f);

			var fipLayer = this.getTestPrimaryLayer(polygonId, l -> {
				l.inventoryTypeGroup(Optional.of(9));
				((PrimaryBuilder) l).primaryGenus(Optional.of("Y"));
			}, TestUtils.valid());

			app.findRootsForDiameterAndBaseArea(layer, fipLayer, bec, 2);

			assertThat(
					layer, hasProperty(
							"loreyHeightByUtilization", //
							coe(-1, 0f, 19.9850883f)
					)
			);
			assertThat(
					spec, hasProperty(
							"baseAreaByUtilization", //
							coe(-1, 0f, 76.5122147f, 0f, 0f, 0f, 0f)
					)
			);
			assertThat(
					spec, hasProperty(
							"treesPerHectareByUtilization", //
							coe(-1, 0f, 845.805969f, 0f, 0f, 0f, 0f)
					)
			);
			assertThat(
					spec, hasProperty(
							"quadraticMeanDiameterByUtilization", //
							coe(-1, 0f, 33.9379082f, 0f, 0f, 0f, 0f)
					)
			);

			assertThat(
					layer, hasProperty(
							"wholeStemVolumeByUtilization", //
							coe(-1, 0f, 571.22583f, 0f, 0f, 0f, 0f)
					)
			);
			assertThat(
					spec, hasProperty(
							"wholeStemVolumeByUtilization", //
							coe(-1, 0f, 571.22583f, 0f, 0f, 0f, 0f)
					)
			);
		}

	}

	@Test
	void testFindRootsForPrimaryLayerDiameterAndAreaMultipleSpeciesPass1() throws Exception {
		var controlMap = FipTestUtils.loadControlMap();
		var polygonId = new PolygonIdentifier("TestPolygon", 2024);
		try (var app = new FipStart()) {
			ApplicationTestUtils.setControlMap(app, controlMap);

			var becLookup = BecDefinitionParser.getBecs(controlMap);
			var bec = becLookup.get("CWH").get();

			var layer = VdypLayer.build(builder -> {
				builder.polygonIdentifier(polygonId);
				builder.layerType(LayerType.PRIMARY);
			});
			layer.getBaseAreaByUtilization().setCoe(0, 44.6249847f);
			layer.getTreesPerHectareByUtilization().setCoe(0, 620.504883f);
			layer.getQuadraticMeanDiameterByUtilization().setCoe(0, 30.2601795f);

			/*
			 * HL[*, -1] 0 HL[0, 0] 0 BA[*, -1] BA[1, 0] VOLWS VOLCU VOL_D VOL_DW VOLDWB dqspbase,goal
			 *
			 * HL[1, 0] spec BA[0, 0] layer TPH[0, 0] layer DQ[0,0] layer INL1VGRP, INL1DGRP, INL1BGRP spec VGRPL,
			 * DGRPL, BGRPL spec Same as above AGETOTL1 layer AGEBHL1 layer YTBH hdl1
			 *
			 */
			// sp 3, 4, 5, 8, 15
			// sp B, C, D, H, S
			var spec1 = VdypSpecies.build(layer, builder -> {
				builder.genus("B", controlMap);
				builder.percentGenus(1f);
				builder.volumeGroup(12);
				builder.decayGroup(7);
				builder.breakageGroup(5);
				builder.addSite(siteBuilder -> {
					siteBuilder.ageTotal(55f);
					siteBuilder.yearsToBreastHeight(1f);
					siteBuilder.height(35.2999992f);
				});
			});
			spec1.getLoreyHeightByUtilization().setCoe(0, 38.7456512f);
			var spec2 = VdypSpecies.build(layer, builder -> {
				builder.genus("C", controlMap);
				builder.percentGenus(7f);
				builder.volumeGroup(20);
				builder.decayGroup(14);
				builder.breakageGroup(6);
			});

			spec2.getLoreyHeightByUtilization().setCoe(0, 22.8001652f);
			var spec3 = VdypSpecies.build(layer, builder -> {
				builder.genus("D", controlMap);
				builder.percentGenus(74f);
				builder.volumeGroup(25);
				builder.decayGroup(19);
				builder.breakageGroup(12);
			});
			spec3.getLoreyHeightByUtilization().setCoe(0, 33.6889763f);
			var spec4 = VdypSpecies.build(layer, builder -> {
				builder.genus("H", controlMap);
				builder.percentGenus(9f);
				builder.volumeGroup(37);
				builder.decayGroup(31);
				builder.breakageGroup(17);
			});
			spec4.getLoreyHeightByUtilization().setCoe(0, 24.3451157f);
			var spec5 = VdypSpecies.build(layer, builder -> {
				builder.genus("S", controlMap);
				builder.percentGenus(9f);
				builder.volumeGroup(66);
				builder.decayGroup(54);
				builder.breakageGroup(28);
			});
			spec5.getLoreyHeightByUtilization().setCoe(0, 34.6888771f);

			Collection<VdypSpecies> specs = new ArrayList<>(5);
			specs.add(spec1);
			specs.add(spec2);
			specs.add(spec3);
			specs.add(spec4);
			specs.add(spec5);

			layer.setSpecies(specs);

			var fipLayer = this.getTestPrimaryLayer(polygonId, l -> {
				l.inventoryTypeGroup(Optional.of(9));
				((PrimaryBuilder) l).primaryGenus(Optional.of("H"));
			}, TestUtils.valid());

			app.findRootsForDiameterAndBaseArea(layer, fipLayer, bec, 2);

			assertThat(
					layer, hasProperty(
							"loreyHeightByUtilization", //
							coe(-1, 0f, 31.4222546f)
					)
			);
			assertThat(
					spec1, hasProperty(
							"loreyHeightByUtilization", //
							coe(-1, 0f, 38.7456512f)
					)
			);
			assertThat(
					spec2, hasProperty(
							"loreyHeightByUtilization", //
							coe(-1, 0f, 22.8001652f)
					)
			);
			assertThat(
					spec3, hasProperty(
							"loreyHeightByUtilization", //
							coe(-1, 0f, 33.6889763f)
					)
			);
			assertThat(
					spec4, hasProperty(
							"loreyHeightByUtilization", //
							coe(-1, 0f, 24.3451157f)
					)
			);
			assertThat(
					spec5, hasProperty(
							"loreyHeightByUtilization", //
							coe(-1, 0f, 34.6888771f)
					)
			);

			assertThat(
					layer, hasProperty(
							"baseAreaByUtilization", //
							coe(-1, 0f, 44.6249847f, 0f, 0f, 0f, 0f)
					)
			);
			assertThat(
					spec1, hasProperty(
							"baseAreaByUtilization", //
							coe(-1, 0f, 0.398000091f, 0f, 0f, 0f, 0f)
					)
			);
			assertThat(
					spec2, hasProperty(
							"baseAreaByUtilization", //
							coe(-1, 0f, 5.10918713f, 0f, 0f, 0f, 0f)
					)
			);
			assertThat(
					spec3, hasProperty(
							"baseAreaByUtilization", //
							coe(-1, 0f, 29.478117f, 0f, 0f, 0f, 0f)
					)
			);
			assertThat(
					spec4, hasProperty(
							"baseAreaByUtilization", //
							coe(-1, 0f, 5.52707148f, 0f, 0f, 0f, 0f)
					)
			);
			assertThat(
					spec5, hasProperty(
							"baseAreaByUtilization", //
							coe(-1, 0f, 4.11260939f, 0f, 0f, 0f, 0f)
					)
			);

			assertThat(
					layer, hasProperty(
							"treesPerHectareByUtilization", //
							coe(-1, 0f, 620.497803f, 0f, 0f, 0f, 0f)
					)
			);
			assertThat(
					spec1, hasProperty(
							"treesPerHectareByUtilization", //
							coe(-1, 0f, 5.04042435f, 0f, 0f, 0f, 0f)
					)
			);
			assertThat(
					spec2, hasProperty(
							"treesPerHectareByUtilization", //
							coe(-1, 0f, 92.9547882f, 0f, 0f, 0f, 0f)
					)
			);
			assertThat(
					spec3, hasProperty(
							"treesPerHectareByUtilization", //
							coe(-1, 0f, 325.183502f, 0f, 0f, 0f, 0f)
					)
			);
			assertThat(
					spec4, hasProperty(
							"treesPerHectareByUtilization", //
							coe(-1, 0f, 153.230591f, 0f, 0f, 0f, 0f)
					)
			);
			assertThat(
					spec5, hasProperty(
							"treesPerHectareByUtilization", //
							coe(-1, 0f, 44.0884819f, 0f, 0f, 0f, 0f)
					)
			);

			assertThat(
					layer, hasProperty(
							"quadraticMeanDiameterByUtilization", //
							coe(-1, 0f, 30.2603531f, 0f, 0f, 0f, 0f)
					)
			);
			assertThat(
					spec1, hasProperty(
							"quadraticMeanDiameterByUtilization", //
							coe(-1, 0f, 31.7075806f, 0f, 0f, 0f, 0f)
					)
			);
			assertThat(
					spec2, hasProperty(
							"quadraticMeanDiameterByUtilization", //
							coe(-1, 0f, 26.4542274f, 0f, 0f, 0f, 0f)
					)
			);
			assertThat(
					spec3, hasProperty(
							"quadraticMeanDiameterByUtilization", //
							coe(-1, 0f, 33.9735298f, 0f, 0f, 0f, 0f)
					)
			);
			assertThat(
					spec4, hasProperty(
							"quadraticMeanDiameterByUtilization", //
							coe(-1, 0f, 21.4303799f, 0f, 0f, 0f, 0f)
					)
			);
			assertThat(
					spec5, hasProperty(
							"quadraticMeanDiameterByUtilization", //
							coe(-1, 0f, 34.4628525f, 0f, 0f, 0f, 0f)
					)
			);

			assertThat(
					layer, hasProperty(
							"wholeStemVolumeByUtilization", //
							coe(-1, 0f, 638.572754f, 0f, 0f, 0f, 0f)
					)
			);
			assertThat(
					spec1, hasProperty(
							"wholeStemVolumeByUtilization", //
							coe(-1, 0f, 6.38573837f, 0f, 0f, 0f, 0f)
					)
			);
			assertThat(
					spec2, hasProperty(
							"wholeStemVolumeByUtilization", //
							coe(-1, 0f, 44.7000046f, 0f, 0f, 0f, 0f)
					)
			);
			assertThat(
					spec3, hasProperty(
							"wholeStemVolumeByUtilization", //
							coe(-1, 0f, 472.54422f, 0f, 0f, 0f, 0f)
					)
			);
			assertThat(
					spec4, hasProperty(
							"wholeStemVolumeByUtilization", //
							coe(-1, 0f, 57.471405f, 0f, 0f, 0f, 0f)
					)
			);
			assertThat(
					spec5, hasProperty(
							"wholeStemVolumeByUtilization", //
							coe(-1, 0f, 57.4714355f, 0f, 0f, 0f, 0f)
					)
			);
		}

	}

	@Test
	void testFindRootsForPrimaryLayerDiameterAndAreaMultipleSpeciesPass1Test2() throws Exception {
		var controlMap = FipTestUtils.loadControlMap();
		var polygonId = new PolygonIdentifier("TestPolygon", 2024);
		try (var app = new FipStart()) {
			ApplicationTestUtils.setControlMap(app, controlMap);

			var becLookup = BecDefinitionParser.getBecs(controlMap);
			var bec = becLookup.get("CWH").get();

			var layer = VdypLayer.build(builder -> {
				builder.polygonIdentifier(polygonId);
				builder.layerType(LayerType.PRIMARY);
			});

			layer.getBaseAreaByUtilization().setCoe(0, 44.9531403f);
			layer.getTreesPerHectareByUtilization().setCoe(0, 1291.11597f);
			layer.getQuadraticMeanDiameterByUtilization().setCoe(0, 21.0548649f);

			/*
			 * HL[*, -1] 0 HL[0, 0] 0 BA[*, -1] BA[1, 0] VOLWS VOLCU VOL_D VOL_DW VOLDWB dqspbase,goal
			 *
			 * HL[1, 0] spec BA[0, 0] layer TPH[0, 0] layer DQ[0,0] layer INL1VGRP, INL1DGRP, INL1BGRP spec VGRPL,
			 * DGRPL, BGRPL spec Same as above AGETOTL1 layer AGEBHL1 layer YTBH hdl1
			 *
			 */
			// sp 3, 4, 5, 8, 15
			// sp B, C, D, H, S
			var spec1 = VdypSpecies.build(layer, builder -> {
				builder.genus("B", controlMap);
				builder.percentGenus(15f);
				builder.volumeGroup(12);
				builder.decayGroup(7);
				builder.breakageGroup(5);
			});
			spec1.getLoreyHeightByUtilization().setCoe(0, 21.5356998f);
			var spec2 = VdypSpecies.build(layer, builder -> {
				builder.genus("D", controlMap);
				builder.percentGenus(7f);
				builder.volumeGroup(25);
				builder.decayGroup(19);
				builder.breakageGroup(12);
			});
			spec2.getLoreyHeightByUtilization().setCoe(0, 22.4329224f);
			var spec3 = VdypSpecies.build(layer, builder -> {
				builder.genus("H", controlMap);
				builder.percentGenus(77f);
				builder.volumeGroup(37);
				builder.decayGroup(54);
				builder.breakageGroup(28);
				builder.addSite(siteBuilder -> {
					siteBuilder.ageTotal(45f);
					siteBuilder.yearsToBreastHeight(5.4000001f);
					siteBuilder.height(24.2999992f);
				});
			});
			spec3.getLoreyHeightByUtilization().setCoe(0, 20.5984688f);
			var spec4 = VdypSpecies.build(layer, builder -> {
				builder.genus("S", controlMap);
				builder.percentGenus(1f);
				builder.volumeGroup(66);
				builder.decayGroup(54);
				builder.breakageGroup(28);
			});
			spec4.getLoreyHeightByUtilization().setCoe(0, 24.0494442f);

			var fipLayer = this.getTestPrimaryLayer(polygonId, l -> {
				l.inventoryTypeGroup(Optional.of(15));
				((PrimaryBuilder) l).primaryGenus(Optional.of("H"));
			}, TestUtils.valid());

			app.findRootsForDiameterAndBaseArea(layer, fipLayer, bec, 2);

			// This is L1COM1/PCTL1 not FIPS/PCTVOLV
			assertThat(
					spec1, hasProperty(
							"percentGenus", //
							closeTo(13.4858189f) // Change
					)
			);
			assertThat(
					spec2, hasProperty(
							"percentGenus", //
							closeTo(6.56449223f) // Change
					)
			);
			assertThat(
					spec3, hasProperty(
							"percentGenus", //
							closeTo(79.0027771f) // Change
					)
			);
			assertThat(
					spec4, hasProperty(
							"percentGenus", //
							closeTo(0.946914673f) // Change
					)
			);

			assertThat(
					layer, hasProperty(
							"loreyHeightByUtilization", //
							coe(-1, 0f, 20.8779621f) // Changed
					)
			);
			assertThat(
					spec1, hasProperty(
							"loreyHeightByUtilization", //
							coe(-1, 0f, 21.5356998f) // No Change
					)
			);
			assertThat(
					spec2, hasProperty(
							"loreyHeightByUtilization", //
							coe(-1, 0f, 22.4329224f) // No Change
					)
			);
			assertThat(
					spec3, hasProperty(
							"loreyHeightByUtilization", //
							coe(-1, 0f, 20.5984688f) // No Change
					)
			);
			assertThat(
					spec4, hasProperty(
							"loreyHeightByUtilization", //
							coe(-1, 0f, 24.0494442f) // No Change
					)
			);

			assertThat(
					layer, hasProperty(
							"baseAreaByUtilization", //
							coe(-1, 0f, 44.9531403f, 0f, 0f, 0f, 0f) // No Change
					)
			);
			assertThat(
					spec1, hasProperty(
							"baseAreaByUtilization", //
							coe(-1, 0f, 6.06229925f, 0f, 0f, 0f, 0f) // Change
					)
			);
			assertThat(
					spec2, hasProperty(
							"baseAreaByUtilization", //
							coe(-1, 0f, 2.95094538f, 0f, 0f, 0f, 0f) // Change
					)
			);
			assertThat(
					spec3, hasProperty(
							"baseAreaByUtilization", //
							coe(-1, 0f, 35.5142288f, 0f, 0f, 0f, 0f) // Change
					)
			);
			assertThat(
					spec4, hasProperty(
							"baseAreaByUtilization", //
							coe(-1, 0f, 0.425667882f, 0f, 0f, 0f, 0f) // Change
					)
			);

			assertThat(
					layer, hasProperty(
							"treesPerHectareByUtilization", //
							coe(-1, 0f, 1291.11621f, 0f, 0f, 0f, 0f) // Change
					)
			);
			assertThat(
					spec1, hasProperty(
							"treesPerHectareByUtilization", //
							coe(-1, 0f, 175.253494f, 0f, 0f, 0f, 0f) // Change
					)
			);
			assertThat(
					spec2, hasProperty(
							"treesPerHectareByUtilization", //
							coe(-1, 0f, 52.7488708f, 0f, 0f, 0f, 0f) // Change
					)
			);
			assertThat(
					spec3, hasProperty(
							"treesPerHectareByUtilization", //
							coe(-1, 0f, 1056.43982f, 0f, 0f, 0f, 0f) // Change
					)
			);
			assertThat(
					spec4, hasProperty(
							"treesPerHectareByUtilization", //
							coe(-1, 0f, 6.67403078f, 0f, 0f, 0f, 0f) // Change
					)
			);

			assertThat(
					layer, hasProperty(
							"quadraticMeanDiameterByUtilization", //
							coe(-1, 0f, 21.054863f, 0f, 0f, 0f, 0f) // No Change
					)
			);
			assertThat(
					spec1, hasProperty(
							"quadraticMeanDiameterByUtilization", //
							coe(-1, 0f, 20.9865189f, 0f, 0f, 0f, 0f) // Change
					)
			);
			assertThat(
					spec2, hasProperty(
							"quadraticMeanDiameterByUtilization", //
							coe(-1, 0f, 26.6888008f, 0f, 0f, 0f, 0f) // Change
					)
			);
			assertThat(
					spec3, hasProperty(
							"quadraticMeanDiameterByUtilization", //
							coe(-1, 0f, 20.6887321f, 0f, 0f, 0f, 0f) // Change
					)
			);
			assertThat(
					spec4, hasProperty(
							"quadraticMeanDiameterByUtilization", //
							coe(-1, 0f, 28.4968204f, 0f, 0f, 0f, 0f) // Change
					)
			);

			assertThat(
					layer, hasProperty(
							"wholeStemVolumeByUtilization", //
							coe(-1, 0f, 393.150146f, 0f, 0f, 0f, 0f) // No Change
					)
			);
			assertThat(
					spec1, hasProperty(
							"wholeStemVolumeByUtilization", //
							coe(-1, 0f, 58.9725418f, 0f, 0f, 0f, 0f) // No Change
					)
			);
			assertThat(
					spec2, hasProperty(
							"wholeStemVolumeByUtilization", //
							coe(-1, 0f, 27.5205307f, 0f, 0f, 0f, 0f) // No Change
					)
			);
			assertThat(
					spec3, hasProperty(
							"wholeStemVolumeByUtilization", //
							coe(-1, 0f, 302.725555f, 0f, 0f, 0f, 0f) // No Change
					)
			);
			assertThat(
					spec4, hasProperty(
							"wholeStemVolumeByUtilization", //
							coe(-1, 0f, 3.93152428f, 0f, 0f, 0f, 0f) // No Change
					)
			);
		}

	}

	@Test
	void testCreateVdypPolygon() throws ProcessingException {
		var controlMap = FipTestUtils.loadControlMap();
		try (var app = new FipStart()) {
			ApplicationTestUtils.setControlMap(app, controlMap);

			var fipPolygon = FipPolygon.build(builder -> {
				builder.polygonIdentifier("Test", 2024);
				builder.forestInventoryZone("D");
				builder.biogeoclimaticZone(Utils.getBec("IDF", controlMap));
				builder.mode(PolygonMode.START);
				builder.yieldFactor(1f);
			});

			// var fipVeteranLayer = new FipLayer("Test", LayerType.VETERAN);
			var fipPrimaryLayer = FipLayerPrimary.buildPrimary(fipPolygon, builder -> {

				builder.crownClosure(60f);

			});

			var processedLayers = new HashMap<LayerType, VdypLayer>();
			processedLayers.put(LayerType.PRIMARY, VdypLayer.build(builder -> {
				builder.polygonIdentifier("Test", 2024);
				builder.layerType(LayerType.PRIMARY);
			}));

			FipSpecies.build(fipPrimaryLayer, builder -> {
				builder.genus("L", controlMap);
				builder.percentGenus(10f);
				builder.addSite(siteBuilder -> {
					siteBuilder.ageTotal(60f);
					siteBuilder.yearsToBreastHeight(8.5f);
					siteBuilder.height(15f);
					siteBuilder.siteSpecies("L");
					siteBuilder.siteIndex(5f);
				});

			});
			FipSpecies.build(fipPrimaryLayer, builder -> {
				builder.genus("PL", controlMap);
				builder.percentGenus(90f);
			});

			var vdypPolygon = app.createVdypPolygon(fipPolygon, processedLayers);

			assertThat(vdypPolygon, notNullValue());
			assertThat(vdypPolygon, hasProperty("layers", equalTo(processedLayers)));
			assertThat(vdypPolygon, hasProperty("percentAvailable", closeTo(90f)));
		}
	}

	@Test
	void testCreateVdypPolygonPercentForestLandGiven() throws ProcessingException {
		var controlMap = FipTestUtils.loadControlMap();
		try (var app = new FipStart()) {
			ApplicationTestUtils.setControlMap(app, controlMap);

			var fipPolygon = FipPolygon.build(pb -> {
				pb.polygonIdentifier("Test", 2024);
				pb.forestInventoryZone("D");
				pb.biogeoclimaticZone(Utils.getBec("IDF", controlMap));
				pb.mode(PolygonMode.START);
				pb.yieldFactor(1f);

				pb.percentAvailable(42f);
			});

			// var fipVeteranLayer = new FipLayer("Test", LayerType.VETERAN);
			FipLayerPrimary.buildPrimary(fipPolygon, lb -> {
				lb.crownClosure(60f);

				lb.addSpecies(sb -> {
					sb.genus("L", controlMap);
					sb.percentGenus(10f);

					sb.addSite(siteBuilder -> {
						siteBuilder.ageTotal(60f);
						siteBuilder.yearsToBreastHeight(8.5f);
						siteBuilder.height(15f);

						siteBuilder.siteIndex(5f);
						siteBuilder.siteSpecies("L");
					});

				});

				lb.addSpecies(sb -> {
					sb.genus("PL", controlMap);
					sb.percentGenus(90f);
				});

			});

			var processedLayers = new HashMap<LayerType, VdypLayer>();
			processedLayers.put(LayerType.PRIMARY, VdypLayer.build(builder -> {
				builder.polygonIdentifier("Test", 2024);
				builder.layerType(LayerType.PRIMARY);
			}));

			var vdypPolygon = app.createVdypPolygon(fipPolygon, processedLayers);

			assertThat(vdypPolygon, notNullValue());
			assertThat(vdypPolygon, hasProperty("layers", equalTo(processedLayers)));
			assertThat(vdypPolygon, hasProperty("percentAvailable", closeTo(42f)));
		}
	}

	@Test
	void testCreateVdypPolygonFipYoung() throws ProcessingException {
		var controlMap = FipTestUtils.loadControlMap();
		try (var app = new FipStart()) {
			ApplicationTestUtils.setControlMap(app, controlMap);

			var fipPolygon = FipPolygon.build(pb -> {
				pb.polygonIdentifier("Test", 2024);
				pb.forestInventoryZone("D");
				pb.biogeoclimaticZone(Utils.getBec("IDF", controlMap));
				pb.mode(PolygonMode.YOUNG);
				pb.yieldFactor(1f);
			});

			// var fipVeteranLayer = new FipLayer("Test", LayerType.VETERAN);
			FipLayerPrimary.buildPrimary(fipPolygon, lb -> {
				lb.crownClosure(60f);

				lb.addSpecies(sb -> {
					sb.genus("L", controlMap);
					sb.percentGenus(10f);
					sb.addSite(ib -> {
						ib.ageTotal(60f);
						ib.yearsToBreastHeight(8.5f);
						ib.height(15f);

						ib.siteIndex(5f);
						ib.siteSpecies("L");
					});
				});

				lb.addSpecies(sb -> {
					sb.genus("PL", controlMap);
					sb.percentGenus(90f);
				});

			});

			var processedLayers = new HashMap<LayerType, VdypLayer>();
			processedLayers.put(LayerType.PRIMARY, VdypLayer.build(lb -> {
				lb.polygonIdentifier("Test", 2024);
				lb.layerType(LayerType.PRIMARY);
			}));

			var vdypPolygon = app.createVdypPolygon(fipPolygon, processedLayers);

			assertThat(vdypPolygon, notNullValue());
			assertThat(vdypPolygon, hasProperty("layers", equalTo(processedLayers)));
			assertThat(vdypPolygon, hasProperty("percentAvailable", closeTo(100f)));
		}
	}

	@Test
	void testApplyStockingFactor() throws ProcessingException {
		var controlMap = FipTestUtils.loadControlMap();
		@SuppressWarnings("unchecked")
		var stockingClassMap = (MatrixMap2<Character, Region, Optional<StockingClassFactor>>) controlMap
				.get(ControlKey.STOCKING_CLASS_FACTORS.name());

		stockingClassMap
				.put('R', Region.INTERIOR, Optional.of(new StockingClassFactor('R', Region.INTERIOR, 0.42f, 100)));

		try (var app = new FipStart()) {
			ApplicationTestUtils.setControlMap(app, controlMap);

			// var fipVeteranLayer = new FipLayer("Test", LayerType.VETERAN);
			var fipPrimaryLayer = FipLayerPrimary.buildPrimary(lb -> {
				lb.polygonIdentifier("Test", 2024);
				lb.crownClosure(0.9f);

				lb.addSpecies(sb -> {
					sb.genus("L", controlMap);
					sb.percentGenus(50f);
					sb.addSite(ib -> {
						ib.ageTotal(60f);
						ib.yearsToBreastHeight(8.5f);
						ib.height(20f);

						ib.siteIndex(5f);
						ib.siteSpecies("L");
					});
				});
				lb.addSpecies(sb -> {
					sb.genus("PL", controlMap);
					sb.percentGenus(50f);
				});

				lb.stockingClass('R');
			});

			var vdypLayer = VdypLayer.build(lb -> {
				lb.polygonIdentifier("Test", 2024);
				lb.layerType(LayerType.PRIMARY);

				lb.addSpecies(sb -> {
					sb.genus("L", controlMap);
					sb.percentGenus(50f);
					sb.volumeGroup(-1);
					sb.decayGroup(-1);
					sb.breakageGroup(-1);
					sb.addSite(ib -> {
						ib.ageTotal(60f);
						ib.yearsToBreastHeight(8.5f);
						ib.height(20f);

						ib.siteIndex(5f);
					});
				});

				lb.addSpecies(sb -> {
					sb.genus("PL", controlMap);
					sb.percentGenus(50f);
					sb.volumeGroup(-1);
					sb.decayGroup(-1);
					sb.breakageGroup(-1);
				});
			});

			vdypLayer.setLoreyHeightByUtilization(Utils.utilizationVector(1f, 1f, 1f, 1f, 1f, 1f));
			vdypLayer.setQuadraticMeanDiameterByUtilization(Utils.utilizationVector(1f, 1f, 1f, 1f, 1f, 1f));

			vdypLayer.setBaseAreaByUtilization(Utils.utilizationVector(1f, 1f, 1f, 1f, 1f));
			vdypLayer.setTreesPerHectareByUtilization(Utils.utilizationVector(1f, 1f, 1f, 1f, 1f));
			vdypLayer.setWholeStemVolumeByUtilization(Utils.utilizationVector(1f, 1f, 1f, 1f, 1f));
			vdypLayer.setCloseUtilizationVolumeByUtilization(Utils.utilizationVector(1f, 1f, 1f, 1f, 1f));
			vdypLayer.setCloseUtilizationVolumeNetOfDecayByUtilization(Utils.utilizationVector(1f, 1f, 1f, 1f, 1f));
			vdypLayer.setCloseUtilizationVolumeNetOfDecayAndWasteByUtilization(
					Utils.utilizationVector(1f, 1f, 1f, 1f, 1f)
			);
			vdypLayer.setCloseUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(
					Utils.utilizationVector(1f, 1f, 1f, 1f, 1f)
			);

			var spec1 = vdypLayer.getSpecies().get("L");

			spec1.setLoreyHeightByUtilization(Utils.utilizationVector(1f, 1f, 1f, 1f, 1f, 1f));
			spec1.setQuadraticMeanDiameterByUtilization(Utils.utilizationVector(1f, 1f, 1f, 1f, 1f, 1f));

			spec1.setBaseAreaByUtilization(Utils.utilizationVector(1f, 1f, 1f, 1f, 1f));
			spec1.setTreesPerHectareByUtilization(Utils.utilizationVector(1f, 1f, 1f, 1f, 1f));
			spec1.setWholeStemVolumeByUtilization(Utils.utilizationVector(1f, 1f, 1f, 1f, 1f));
			spec1.setCloseUtilizationVolumeNetOfDecayByUtilization(Utils.utilizationVector(1f, 1f, 1f, 1f, 1f));
			spec1.setCloseUtilizationVolumeNetOfDecayAndWasteByUtilization(Utils.utilizationVector(1f, 1f, 1f, 1f, 1f));
			spec1.setCloseUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(
					Utils.utilizationVector(1f, 1f, 1f, 1f, 1f)
			);

			var spec2 = vdypLayer.getSpecies().get("PL");

			spec2.setLoreyHeightByUtilization(Utils.utilizationVector(1f, 1f, 1f, 1f, 1f, 1f));
			spec2.setQuadraticMeanDiameterByUtilization(Utils.utilizationVector(1f, 1f, 1f, 1f, 1f, 1f));

			spec2.setBaseAreaByUtilization(Utils.utilizationVector(1f, 1f, 1f, 1f, 1f));
			spec2.setTreesPerHectareByUtilization(Utils.utilizationVector(1f, 1f, 1f, 1f, 1f));
			spec2.setWholeStemVolumeByUtilization(Utils.utilizationVector(1f, 1f, 1f, 1f, 1f));
			spec2.setCloseUtilizationVolumeNetOfDecayByUtilization(Utils.utilizationVector(1f, 1f, 1f, 1f, 1f));
			spec2.setCloseUtilizationVolumeNetOfDecayAndWasteByUtilization(Utils.utilizationVector(1f, 1f, 1f, 1f, 1f));
			spec2.setCloseUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(
					Utils.utilizationVector(1f, 1f, 1f, 1f, 1f)
			);

			app.adjustForStocking(vdypLayer, fipPrimaryLayer, BecDefinitionParser.getBecs(controlMap).get("IDF").get());

			final var modifiedValue = VdypMatchers.utilization(0.42f, 4 * 0.42f, 0.42f, 0.42f, 0.42f, 0.42f);
			final var neverModifiedValue = VdypMatchers.utilization(1f, 1f, 1f, 1f, 1f, 1f);

			assertThat(vdypLayer, hasProperty("loreyHeightByUtilization", neverModifiedValue));
			assertThat(vdypLayer, hasProperty("quadraticMeanDiameterByUtilization", neverModifiedValue));

			assertThat(vdypLayer, hasProperty("baseAreaByUtilization", modifiedValue));
			assertThat(vdypLayer, hasProperty("treesPerHectareByUtilization", modifiedValue));
			assertThat(vdypLayer, hasProperty("closeUtilizationVolumeNetOfDecayByUtilization", modifiedValue));
			assertThat(vdypLayer, hasProperty("closeUtilizationVolumeNetOfDecayAndWasteByUtilization", modifiedValue));
			assertThat(
					vdypLayer,
					hasProperty("closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization", modifiedValue)
			);

			assertThat(spec1, hasProperty("loreyHeightByUtilization", neverModifiedValue));
			assertThat(spec1, hasProperty("quadraticMeanDiameterByUtilization", neverModifiedValue));

			assertThat(spec1, hasProperty("baseAreaByUtilization", modifiedValue));
			assertThat(spec1, hasProperty("treesPerHectareByUtilization", modifiedValue));
			assertThat(spec1, hasProperty("closeUtilizationVolumeNetOfDecayByUtilization", modifiedValue));
			assertThat(spec1, hasProperty("closeUtilizationVolumeNetOfDecayAndWasteByUtilization", modifiedValue));
			assertThat(
					spec1, hasProperty("closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization", modifiedValue)
			);

			assertThat(spec2, hasProperty("loreyHeightByUtilization", neverModifiedValue));
			assertThat(spec2, hasProperty("quadraticMeanDiameterByUtilization", neverModifiedValue));

			assertThat(spec2, hasProperty("baseAreaByUtilization", modifiedValue));
			assertThat(spec2, hasProperty("treesPerHectareByUtilization", modifiedValue));
			assertThat(spec2, hasProperty("closeUtilizationVolumeNetOfDecayByUtilization", modifiedValue));
			assertThat(spec2, hasProperty("closeUtilizationVolumeNetOfDecayAndWasteByUtilization", modifiedValue));
			assertThat(
					spec2, hasProperty("closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization", modifiedValue)
			);
		}
	}

	@Test
	void testApplyStockingFactorNoFactorForLayer() throws ProcessingException {
		var controlMap = FipTestUtils.loadControlMap();
		@SuppressWarnings("unchecked")
		var stockingClassMap = (MatrixMap2<Character, Region, StockingClassFactor>) controlMap
				.get(ControlKey.STOCKING_CLASS_FACTORS.name());

		stockingClassMap.put('R', Region.INTERIOR, new StockingClassFactor('R', Region.INTERIOR, 0.42f, 100));

		try (var app = new FipStart()) {
			ApplicationTestUtils.setControlMap(app, controlMap);

			// var fipVeteranLayer = new FipLayer("Test", LayerType.VETERAN);
			var fipPrimaryLayer = FipLayerPrimary.buildPrimary(lb -> {
				lb.polygonIdentifier("Test", 2024);
				lb.crownClosure(60f);

				lb.addSpecies(sb -> {
					sb.genus("L", controlMap);
					sb.percentGenus(50f);
					sb.addSite(ib -> {
						ib.ageTotal(60f);
						ib.yearsToBreastHeight(8.5f);
						ib.height(20f);

						ib.siteIndex(5f);
						ib.siteSpecies("L");
					});
				});
				lb.addSpecies(sb -> {
					sb.genus("PL", controlMap);
					sb.percentGenus(50f);
				});

			});

			fipPrimaryLayer.setStockingClass(Optional.empty());

			var vdypLayer = VdypLayer.build(lb -> {
				lb.polygonIdentifier("Test", 2024);
				lb.layerType(LayerType.PRIMARY);

				lb.addSpecies(sb -> {
					sb.genus("L", controlMap);
					sb.percentGenus(50f);
					sb.volumeGroup(-1);
					sb.decayGroup(-1);
					sb.breakageGroup(-1);
					sb.addSite(ib -> {
						ib.ageTotal(60f);
						ib.yearsToBreastHeight(8.5f);
						ib.height(20f);

						ib.siteIndex(5f);
					});
				});

				lb.addSpecies(sb -> {
					sb.genus("PL", controlMap);
					sb.percentGenus(50f);
					sb.volumeGroup(-1);
					sb.decayGroup(-1);
					sb.breakageGroup(-1);
				});
			});
			vdypLayer.setLoreyHeightByUtilization(Utils.utilizationVector(1f, 1f, 1f, 1f, 1f, 1f));
			vdypLayer.setQuadraticMeanDiameterByUtilization(Utils.utilizationVector(1f, 1f, 1f, 1f, 1f, 1f));

			vdypLayer.setBaseAreaByUtilization(Utils.utilizationVector(1f, 1f, 1f, 1f, 1f));
			vdypLayer.setTreesPerHectareByUtilization(Utils.utilizationVector(1f, 1f, 1f, 1f, 1f));
			vdypLayer.setWholeStemVolumeByUtilization(Utils.utilizationVector(1f, 1f, 1f, 1f, 1f));
			vdypLayer.setCloseUtilizationVolumeByUtilization(Utils.utilizationVector(1f, 1f, 1f, 1f, 1f));
			vdypLayer.setCloseUtilizationVolumeNetOfDecayByUtilization(Utils.utilizationVector(1f, 1f, 1f, 1f, 1f));
			vdypLayer.setCloseUtilizationVolumeNetOfDecayAndWasteByUtilization(
					Utils.utilizationVector(1f, 1f, 1f, 1f, 1f)
			);
			vdypLayer.setCloseUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(
					Utils.utilizationVector(1f, 1f, 1f, 1f, 1f)
			);

			var spec1 = vdypLayer.getSpecies().get("L");

			spec1.setLoreyHeightByUtilization(Utils.utilizationVector(1f, 1f, 1f, 1f, 1f, 1f));
			spec1.setQuadraticMeanDiameterByUtilization(Utils.utilizationVector(1f, 1f, 1f, 1f, 1f, 1f));

			spec1.setBaseAreaByUtilization(Utils.utilizationVector(1f, 1f, 1f, 1f, 1f));
			spec1.setTreesPerHectareByUtilization(Utils.utilizationVector(1f, 1f, 1f, 1f, 1f));
			spec1.setWholeStemVolumeByUtilization(Utils.utilizationVector(1f, 1f, 1f, 1f, 1f));
			spec1.setCloseUtilizationVolumeNetOfDecayByUtilization(Utils.utilizationVector(1f, 1f, 1f, 1f, 1f));
			spec1.setCloseUtilizationVolumeNetOfDecayAndWasteByUtilization(Utils.utilizationVector(1f, 1f, 1f, 1f, 1f));
			spec1.setCloseUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(
					Utils.utilizationVector(1f, 1f, 1f, 1f, 1f)
			);

			var spec2 = vdypLayer.getSpecies().get("PL");

			spec2.setLoreyHeightByUtilization(Utils.utilizationVector(1f, 1f, 1f, 1f, 1f, 1f));
			spec2.setQuadraticMeanDiameterByUtilization(Utils.utilizationVector(1f, 1f, 1f, 1f, 1f, 1f));

			spec2.setBaseAreaByUtilization(Utils.utilizationVector(1f, 1f, 1f, 1f, 1f));
			spec2.setTreesPerHectareByUtilization(Utils.utilizationVector(1f, 1f, 1f, 1f, 1f));
			spec2.setWholeStemVolumeByUtilization(Utils.utilizationVector(1f, 1f, 1f, 1f, 1f));
			spec2.setCloseUtilizationVolumeNetOfDecayByUtilization(Utils.utilizationVector(1f, 1f, 1f, 1f, 1f));
			spec2.setCloseUtilizationVolumeNetOfDecayAndWasteByUtilization(Utils.utilizationVector(1f, 1f, 1f, 1f, 1f));
			spec2.setCloseUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(
					Utils.utilizationVector(1f, 1f, 1f, 1f, 1f)
			);

			app.adjustForStocking(vdypLayer, fipPrimaryLayer, BecDefinitionParser.getBecs(controlMap).get("IDF").get());

			final var modifiableNotModifiedValue = VdypMatchers.utilization(1f, 4f, 1f, 1f, 1f, 1f);
			final var neverModifiedValue = VdypMatchers.utilization(1f, 1f, 1f, 1f, 1f, 1f);

			assertThat(vdypLayer, hasProperty("loreyHeightByUtilization", neverModifiedValue));
			assertThat(vdypLayer, hasProperty("quadraticMeanDiameterByUtilization", neverModifiedValue));

			assertThat(vdypLayer, hasProperty("baseAreaByUtilization", modifiableNotModifiedValue));
			assertThat(vdypLayer, hasProperty("treesPerHectareByUtilization", modifiableNotModifiedValue));
			assertThat(
					vdypLayer, hasProperty("closeUtilizationVolumeNetOfDecayByUtilization", modifiableNotModifiedValue)
			);
			assertThat(
					vdypLayer,
					hasProperty("closeUtilizationVolumeNetOfDecayAndWasteByUtilization", modifiableNotModifiedValue)
			);
			assertThat(
					vdypLayer,
					hasProperty(
							"closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization", modifiableNotModifiedValue
					)
			);

			assertThat(spec1, hasProperty("loreyHeightByUtilization", neverModifiedValue));
			assertThat(spec1, hasProperty("quadraticMeanDiameterByUtilization", neverModifiedValue));

			assertThat(spec1, hasProperty("baseAreaByUtilization", modifiableNotModifiedValue));
			assertThat(spec1, hasProperty("treesPerHectareByUtilization", modifiableNotModifiedValue));
			assertThat(spec1, hasProperty("closeUtilizationVolumeNetOfDecayByUtilization", modifiableNotModifiedValue));
			assertThat(
					spec1,
					hasProperty("closeUtilizationVolumeNetOfDecayAndWasteByUtilization", modifiableNotModifiedValue)
			);
			assertThat(
					spec1,
					hasProperty(
							"closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization", modifiableNotModifiedValue
					)
			);

			assertThat(spec2, hasProperty("loreyHeightByUtilization", neverModifiedValue));
			assertThat(spec2, hasProperty("quadraticMeanDiameterByUtilization", neverModifiedValue));

			assertThat(spec2, hasProperty("baseAreaByUtilization", modifiableNotModifiedValue));
			assertThat(spec2, hasProperty("treesPerHectareByUtilization", modifiableNotModifiedValue));
			assertThat(spec2, hasProperty("closeUtilizationVolumeNetOfDecayByUtilization", modifiableNotModifiedValue));
			assertThat(
					spec2,
					hasProperty("closeUtilizationVolumeNetOfDecayAndWasteByUtilization", modifiableNotModifiedValue)
			);
			assertThat(
					spec2,
					hasProperty(
							"closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization", modifiableNotModifiedValue
					)
			);
		}
	}

	@Test
	void testApplyStockingFactorNoFactorForClass() throws ProcessingException {
		var controlMap = FipTestUtils.loadControlMap();
		@SuppressWarnings("unchecked")
		var stockingClassMap = (MatrixMap2<Character, Region, StockingClassFactor>) controlMap
				.get(ControlKey.STOCKING_CLASS_FACTORS.name());

		stockingClassMap.remove('R', Region.INTERIOR);

		try (var app = new FipStart()) {
			ApplicationTestUtils.setControlMap(app, controlMap);

			// var fipVeteranLayer = new FipLayer("Test", LayerType.VETERAN);
			var fipPrimaryLayer = FipLayerPrimary.buildPrimary(lb -> {
				lb.polygonIdentifier("Test", 2024);
				lb.crownClosure(60f);

				lb.addSpecies(sb -> {
					sb.genus("L", controlMap);
					sb.percentGenus(50f);
					sb.addSite(ib -> {
						ib.ageTotal(60f);
						ib.yearsToBreastHeight(8.5f);
						ib.height(20f);

						ib.siteIndex(5f);
						ib.siteSpecies("L");
					});
				});
				lb.addSpecies(sb -> {
					sb.genus("PL", controlMap);
					sb.percentGenus(50f);
				});
			});

			fipPrimaryLayer.setStockingClass(Optional.of('R'));

			var vdypLayer = VdypLayer.build(lb -> {
				lb.polygonIdentifier("Test", 2024);
				lb.layerType(LayerType.PRIMARY);

				lb.addSpecies(sb -> {
					sb.genus("L", controlMap);
					sb.percentGenus(50f);
					sb.volumeGroup(-1);
					sb.decayGroup(-1);
					sb.breakageGroup(-1);
					sb.addSite(ib -> {
						ib.ageTotal(60f);
						ib.yearsToBreastHeight(8.5f);
						ib.height(20f);

						ib.siteIndex(5f);
					});
				});

				lb.addSpecies(sb -> {
					sb.genus("PL", controlMap);
					sb.percentGenus(50f);
					sb.volumeGroup(-1);
					sb.decayGroup(-1);
					sb.breakageGroup(-1);
				});
			});

			vdypLayer.setLoreyHeightByUtilization(Utils.utilizationVector(1f, 1f, 1f, 1f, 1f, 1f));
			vdypLayer.setQuadraticMeanDiameterByUtilization(Utils.utilizationVector(1f, 1f, 1f, 1f, 1f, 1f));

			vdypLayer.setBaseAreaByUtilization(Utils.utilizationVector(1f, 1f, 1f, 1f, 1f));
			vdypLayer.setTreesPerHectareByUtilization(Utils.utilizationVector(1f, 1f, 1f, 1f, 1f));
			vdypLayer.setWholeStemVolumeByUtilization(Utils.utilizationVector(1f, 1f, 1f, 1f, 1f));
			vdypLayer.setCloseUtilizationVolumeByUtilization(Utils.utilizationVector(1f, 1f, 1f, 1f, 1f));
			vdypLayer.setCloseUtilizationVolumeNetOfDecayByUtilization(Utils.utilizationVector(1f, 1f, 1f, 1f, 1f));
			vdypLayer.setCloseUtilizationVolumeNetOfDecayAndWasteByUtilization(
					Utils.utilizationVector(1f, 1f, 1f, 1f, 1f)
			);
			vdypLayer.setCloseUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(
					Utils.utilizationVector(1f, 1f, 1f, 1f, 1f)
			);

			var spec1 = VdypSpecies.build(vdypLayer, builder -> {
				builder.genus("L", controlMap);
				builder.percentGenus(50f);
				builder.volumeGroup(-1);
				builder.decayGroup(-1);
				builder.breakageGroup(-1);
			});

			spec1.setLoreyHeightByUtilization(Utils.utilizationVector(1f, 1f, 1f, 1f, 1f, 1f));
			spec1.setQuadraticMeanDiameterByUtilization(Utils.utilizationVector(1f, 1f, 1f, 1f, 1f, 1f));

			spec1.setBaseAreaByUtilization(Utils.utilizationVector(1f, 1f, 1f, 1f, 1f));
			spec1.setTreesPerHectareByUtilization(Utils.utilizationVector(1f, 1f, 1f, 1f, 1f));
			spec1.setWholeStemVolumeByUtilization(Utils.utilizationVector(1f, 1f, 1f, 1f, 1f));
			spec1.setCloseUtilizationVolumeNetOfDecayByUtilization(Utils.utilizationVector(1f, 1f, 1f, 1f, 1f));
			spec1.setCloseUtilizationVolumeNetOfDecayAndWasteByUtilization(Utils.utilizationVector(1f, 1f, 1f, 1f, 1f));
			spec1.setCloseUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(
					Utils.utilizationVector(1f, 1f, 1f, 1f, 1f)
			);

			var spec2 = VdypSpecies.build(vdypLayer, builder -> {
				builder.genus("PL", controlMap);
				builder.percentGenus(50f);
				builder.volumeGroup(-1);
				builder.decayGroup(-1);
				builder.breakageGroup(-1);
			});

			spec2.setLoreyHeightByUtilization(Utils.utilizationVector(1f, 1f, 1f, 1f, 1f, 1f));
			spec2.setQuadraticMeanDiameterByUtilization(Utils.utilizationVector(1f, 1f, 1f, 1f, 1f, 1f));

			spec2.setBaseAreaByUtilization(Utils.utilizationVector(1f, 1f, 1f, 1f, 1f));
			spec2.setTreesPerHectareByUtilization(Utils.utilizationVector(1f, 1f, 1f, 1f, 1f));
			spec2.setWholeStemVolumeByUtilization(Utils.utilizationVector(1f, 1f, 1f, 1f, 1f));
			spec2.setCloseUtilizationVolumeNetOfDecayByUtilization(Utils.utilizationVector(1f, 1f, 1f, 1f, 1f));
			spec2.setCloseUtilizationVolumeNetOfDecayAndWasteByUtilization(Utils.utilizationVector(1f, 1f, 1f, 1f, 1f));
			spec2.setCloseUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(
					Utils.utilizationVector(1f, 1f, 1f, 1f, 1f)
			);

			app.adjustForStocking(vdypLayer, fipPrimaryLayer, BecDefinitionParser.getBecs(controlMap).get("IDF").get());

			final var modifiableNotModifiedValue = VdypMatchers.utilization(1f, 4f, 1f, 1f, 1f, 1f);
			final var neverModifiedValue = VdypMatchers.utilization(1f, 1f, 1f, 1f, 1f, 1f);

			assertThat(vdypLayer, hasProperty("loreyHeightByUtilization", neverModifiedValue));
			assertThat(vdypLayer, hasProperty("quadraticMeanDiameterByUtilization", neverModifiedValue));

			assertThat(vdypLayer, hasProperty("baseAreaByUtilization", modifiableNotModifiedValue));
			assertThat(vdypLayer, hasProperty("treesPerHectareByUtilization", modifiableNotModifiedValue));
			assertThat(
					vdypLayer, hasProperty("closeUtilizationVolumeNetOfDecayByUtilization", modifiableNotModifiedValue)
			);
			assertThat(
					vdypLayer,
					hasProperty("closeUtilizationVolumeNetOfDecayAndWasteByUtilization", modifiableNotModifiedValue)
			);
			assertThat(
					vdypLayer,
					hasProperty(
							"closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization", modifiableNotModifiedValue
					)
			);

			assertThat(spec1, hasProperty("loreyHeightByUtilization", neverModifiedValue));
			assertThat(spec1, hasProperty("quadraticMeanDiameterByUtilization", neverModifiedValue));

			assertThat(spec1, hasProperty("baseAreaByUtilization", modifiableNotModifiedValue));
			assertThat(spec1, hasProperty("treesPerHectareByUtilization", modifiableNotModifiedValue));
			assertThat(spec1, hasProperty("closeUtilizationVolumeNetOfDecayByUtilization", modifiableNotModifiedValue));
			assertThat(
					spec1,
					hasProperty("closeUtilizationVolumeNetOfDecayAndWasteByUtilization", modifiableNotModifiedValue)
			);
			assertThat(
					spec1,
					hasProperty(
							"closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization", modifiableNotModifiedValue
					)
			);

			assertThat(spec2, hasProperty("loreyHeightByUtilization", neverModifiedValue));
			assertThat(spec2, hasProperty("quadraticMeanDiameterByUtilization", neverModifiedValue));

			assertThat(spec2, hasProperty("baseAreaByUtilization", modifiableNotModifiedValue));
			assertThat(spec2, hasProperty("treesPerHectareByUtilization", modifiableNotModifiedValue));
			assertThat(spec2, hasProperty("closeUtilizationVolumeNetOfDecayByUtilization", modifiableNotModifiedValue));
			assertThat(
					spec2,
					hasProperty("closeUtilizationVolumeNetOfDecayAndWasteByUtilization", modifiableNotModifiedValue)
			);
			assertThat(
					spec2,
					hasProperty(
							"closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization", modifiableNotModifiedValue
					)
			);
		}
	}

	@Test
	void testProcessPolygon() throws ProcessingException, IOException {
		var controlMap = FipTestUtils.loadControlMap();

		var poly = FipPolygon.build(builder -> {
			builder.polygonIdentifier("Test", 2024);
			builder.biogeoclimaticZone(Utils.getBec("CWH", controlMap));
			builder.yieldFactor(1f);
			builder.forestInventoryZone("0");
			builder.mode(PolygonMode.START);
		});

		var layer = FipLayerPrimary.buildPrimary(poly, lb -> {
			lb.crownClosure(0.9f);

			lb.addSpecies(sb -> {
				sb.genus("B", controlMap);
				sb.percentGenus(100);

				sb.addSite(ib -> {
					ib.ageTotal(50f);
					ib.yearsToBreastHeight(2f);
					ib.height(20f);

					ib.siteIndex(1f);
					ib.siteSpecies("B");
				});
			});

		});

		@SuppressWarnings("unused")
		var spec = layer.getSpecies().get("B");

		var app = new FipStart();
		ApplicationTestUtils.setControlMap(app, controlMap);

		var result = app.processPolygon(0, poly);

		assertThat(result, present(any(VdypPolygon.class)));

		app.close();
	}

	private static <T> MockStreamingParser<T>
			mockStream(IMocksControl control, Map<String, Object> controlMap, String key, String name)
					throws IOException {
		StreamingParserFactory<T> streamFactory = control.mock(name + "Factory", StreamingParserFactory.class);
		MockStreamingParser<T> stream = new MockStreamingParser<>();

		EasyMock.expect(streamFactory.get()).andReturn(stream);

		controlMap.put(key, streamFactory);
		return stream;
	}

	private static void expectAllClosed(MockStreamingParser<?>... toClose) throws Exception {
		for (var x : toClose) {
			x.expectClosed();
		}
	}

	private static <T> void mockWith(MockStreamingParser<T> stream, List<T> results)
			throws IOException, ResourceParseException {
		stream.addValues(results);
	}

	@SuppressWarnings("unused")
	@SafeVarargs
	private static <T> void mockWith(MockStreamingParser<T> stream, T... results)
			throws IOException, ResourceParseException {
		stream.addValues(results);
	}

	private static final void testWith(
			List<FipPolygon> polygons, List<Map<LayerType, FipLayer>> layers, List<Collection<FipSpecies>> species,
			TestConsumer<FipStart> test
	) throws Exception {
		testWith(new HashMap<>(), polygons, layers, species, test);
	}

	private static final void testWith(
			Map<String, Object> myControlMap, List<FipPolygon> polygons, List<Map<LayerType, FipLayer>> layers,
			List<Collection<FipSpecies>> species, TestConsumer<FipStart> test
	) throws Exception {

		VdypStartApplication<FipPolygon, FipLayer, FipSpecies, FipSite> app = new FipStart();

		Map<String, Object> controlMap = new HashMap<>();

		Map<String, Float> minima = new HashMap<>();

		minima.put(BaseControlParser.MINIMUM_HEIGHT, 5f);
		minima.put(BaseControlParser.MINIMUM_BASE_AREA, 0f);
		minima.put(BaseControlParser.MINIMUM_PREDICTED_BASE_AREA, 2f);
		minima.put(BaseControlParser.MINIMUM_VETERAN_HEIGHT, 10f);

		controlMap.put(ControlKey.MINIMA.name(), minima);

		controlMap.putAll(myControlMap);

		var control = EasyMock.createControl();

		MockStreamingParser<FipPolygon> polygonStream = mockStream(
				control, controlMap, ControlKey.FIP_INPUT_YIELD_POLY.name(), "polygonStream"
		);
		MockStreamingParser<Map<LayerType, FipLayer>> layerStream = mockStream(
				control, controlMap, ControlKey.FIP_INPUT_YIELD_LAYER.name(), "layerStream"
		);
		MockStreamingParser<Collection<FipSpecies>> speciesStream = mockStream(
				control, controlMap, ControlKey.FIP_INPUT_YIELD_LX_SP0.name(), "speciesStream"
		);

		mockWith(polygonStream, polygons);
		mockWith(layerStream, layers);
		mockWith(speciesStream, species);

		ApplicationTestUtils.setControlMap(app, controlMap);

		control.replay();

		test.accept(app, controlMap);

		control.verify();

		expectAllClosed(polygonStream, layerStream, speciesStream);

	}

	static Map<LayerType, FipLayer> layerMap(FipLayer... layers) {
		Map<LayerType, FipLayer> result = new HashMap<>();
		for (var layer : layers) {
			result.put(layer.getLayerType(), layer);
		}
		return result;
	}

	FipPolygon getTestPolygon(PolygonIdentifier polygonId, Consumer<FipPolygon> mutator) {

		Map<String, Object> controlMap = new HashMap<>();
		TestUtils.populateControlMapBecReal(controlMap);

		var result = FipPolygon.build(builder -> {
			builder.polygonIdentifier(polygonId);
			builder.forestInventoryZone("0");
			builder.biogeoclimaticZone(Utils.getBec("BG", controlMap));
			builder.mode(PolygonMode.START);
			builder.yieldFactor(1.0f);
		});
		mutator.accept(result);
		return result;
	}

	FipLayerPrimary getTestPrimaryLayer(
			PolygonIdentifier polygonId, Consumer<FipLayerPrimary.Builder> mutator,
			Consumer<FipSite.Builder> siteMutator
	) {
		return FipLayerPrimary.buildPrimary(builder -> {
			builder.polygonIdentifier(polygonId);
			builder.addSiteWithoutSpecies(siteBuilder -> {
				siteBuilder.ageTotal(8f);
				siteBuilder.yearsToBreastHeight(7f);
				siteBuilder.height(6f);
				siteBuilder.siteIndex(5f);
				siteBuilder.siteGenus("B");
				siteBuilder.siteSpecies("B");
				siteMutator.accept(siteBuilder);
			});

			builder.crownClosure(0.9f);
			mutator.accept(builder);
		});
	}

	FipLayer getTestVeteranLayer(
			PolygonIdentifier polygonId, Consumer<FipLayer.Builder> mutator, Consumer<FipSite.Builder> siteMutator
	) {
		return FipLayer.build(builder -> {
			builder.polygonIdentifier(polygonId);
			builder.layerType(LayerType.VETERAN);

			builder.addSiteWithoutSpecies(siteBuilder -> {
				siteBuilder.ageTotal(8f);
				siteBuilder.yearsToBreastHeight(7f);
				siteBuilder.height(6f);
				siteBuilder.siteIndex(5f);
				siteBuilder.siteGenus("B");
				siteBuilder.siteSpecies("B");
				siteMutator.accept(siteBuilder);
			});

			builder.crownClosure(0.9f);
			mutator.accept(builder);
		});
	}

	FipSpecies getTestSpecies(PolygonIdentifier polygonId, LayerType layer, Consumer<FipSpecies> mutator) {
		return getTestSpecies(polygonId, layer, "B", 3, mutator);
	}

	FipSpecies getTestSpecies(
			PolygonIdentifier polygonId, LayerType layer, String genusId, int genusIndex, Consumer<FipSpecies> mutator
	) {
		var result = FipSpecies.build(builder -> {
			builder.polygonIdentifier(polygonId);
			builder.layerType(layer);
			builder.genus(genusId, genusIndex);
			builder.percentGenus(100.0f);
			builder.addSp64Distribution(genusId, 100f);
		});
		mutator.accept(result);
		return result;
	}

	@FunctionalInterface
	private static interface TestConsumer<T> {
		public void accept(
				VdypStartApplication<FipPolygon, FipLayer, FipSpecies, FipSite> unit, Map<String, Object> controlMap
		) throws Exception;
	}
}
