package ca.bc.gov.nrs.vdyp.fip;

import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.coe;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.describedAs;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.fip.model.FipLayer;
import ca.bc.gov.nrs.vdyp.fip.model.FipLayerPrimary;
import ca.bc.gov.nrs.vdyp.fip.model.FipMode;
import ca.bc.gov.nrs.vdyp.fip.model.FipPolygon;
import ca.bc.gov.nrs.vdyp.fip.model.FipSpecies;
import ca.bc.gov.nrs.vdyp.fip.test.FipTestUtils;
import ca.bc.gov.nrs.vdyp.io.parse.BecDefinitionParser;
import ca.bc.gov.nrs.vdyp.io.parse.GenusDefinitionParser;
import ca.bc.gov.nrs.vdyp.io.parse.MockStreamingParser;
import ca.bc.gov.nrs.vdyp.io.parse.ResourceParseException;
import ca.bc.gov.nrs.vdyp.io.parse.StreamingParserFactory;
import ca.bc.gov.nrs.vdyp.io.parse.VeteranLayerVolumeAdjustParser;
import ca.bc.gov.nrs.vdyp.model.Coefficients;
import ca.bc.gov.nrs.vdyp.model.Layer;
import ca.bc.gov.nrs.vdyp.model.Region;
import ca.bc.gov.nrs.vdyp.model.VdypSpecies;
import ca.bc.gov.nrs.vdyp.model.VdypUtilizationHolder;
import ca.bc.gov.nrs.vdyp.test.TestUtils;

class FipStartTest {

	static final float EPSILON = 0.00001f;

	@Test
	void testProcessEmpty() throws Exception {

		testWith(Arrays.asList(), Arrays.asList(), Arrays.asList(), (app, controlMap) -> {
			assertDoesNotThrow(app::process);
		});
	}

	@Test
	void testProcessSimple() throws Exception {

		var polygonId = polygonId("Test Polygon", 2023);
		var layer = Layer.PRIMARY;

		// One polygon with one primary layer with one species entry
		testWith(
				FipTestUtils.loadControlMap(), Arrays.asList(getTestPolygon(polygonId, valid())), //
				Arrays.asList(layerMap(getTestPrimaryLayer(polygonId, valid()))), //
				Arrays.asList(Collections.singletonList(getTestSpecies(polygonId, layer, valid()))), //
				(app, controlMap) -> {
					assertDoesNotThrow(app::process);
				}
		);

	}

	@Test
	void testPolygonWithNoLayersRecord() throws Exception {

		var polygonId = polygonId("Test Polygon", 2023);

		testWith(
				Arrays.asList(getTestPolygon(polygonId, valid())), //
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
				Arrays.asList(getTestPolygon(polygonId, valid())), //
				Arrays.asList(layerMap(getTestPrimaryLayer(polygonId, valid()))), //
				Collections.emptyList(), //
				(app, controlMap) -> {
					var ex = assertThrows(ProcessingException.class, () -> app.process());

					assertThat(ex, hasProperty("message", is("Species file has fewer records than polygon file.")));

				}
		);
	}

	@Test
	void testPolygonWithNoPrimaryLayer() throws Exception {

		var polygonId = polygonId("Test Polygon", 2023);
		var layer = Layer.VETERAN;

		// One polygon with one layer with one species entry, and type is VETERAN
		testWith(
				Arrays.asList(getTestPolygon(polygonId, valid())), //
				Arrays.asList(layerMap(getTestVeteranLayer(polygonId, valid()))), //
				Arrays.asList(Collections.singletonList(getTestSpecies(polygonId, layer, valid()))), //
				(app, controlMap) -> {
					var ex = assertThrows(ProcessingException.class, () -> app.process());

					assertThat(
							ex,
							hasProperty(
									"message",
									is(
											"Polygon " + polygonId + " has no " + Layer.PRIMARY
													+ " layer, or that layer has non-positive height or crown closure."
									)
							)
					);

				}
		);

	}

	@Test
	void testPrimaryLayerHeightLessThanMinimum() throws Exception {

		var polygonId = polygonId("Test Polygon", 2023);
		var layer = Layer.PRIMARY;

		testWith(
				Arrays.asList(getTestPolygon(polygonId, valid())), //
				Arrays.asList(layerMap(getTestPrimaryLayer(polygonId, x -> {
					x.setHeight(4f);
				}))), //
				Arrays.asList(Collections.singletonList(getTestSpecies(polygonId, layer, valid()))), //
				(app, controlMap) -> {
					var ex = assertThrows(ProcessingException.class, () -> app.process());

					assertThat(
							ex,
							hasProperty(
									"message",
									is(
											"Polygon " + polygonId + " has " + Layer.PRIMARY
													+ " layer where height 4.0 is less than minimum 5.0."
									)
							)
					);

				}
		);

	}

	@Test
	void testVeteranLayerHeightLessThanMinimum() throws Exception {

		var polygonId = polygonId("Test Polygon", 2023);
		var layer = Layer.VETERAN;

		testWith(
				Arrays.asList(getTestPolygon(polygonId, valid())), //
				Arrays.asList(
						layerMap(
								getTestPrimaryLayer(polygonId, valid()), //
								getTestVeteranLayer(polygonId, x -> {
									x.setHeight(9f);
								}) //
						)
				), //
				Arrays.asList(Collections.singletonList(getTestSpecies(polygonId, layer, valid()))), //
				(app, controlMap) -> {
					var ex = assertThrows(ProcessingException.class, () -> app.process());

					assertThat(
							ex,
							hasProperty(
									"message",
									is(
											"Polygon " + polygonId + " has " + Layer.VETERAN
													+ " layer where height 9.0 is less than minimum 10.0."
									)
							)
					);

				}
		);

	}

	@Test
	void testPrimaryLayerYearsToBreastHeightLessThanMinimum() throws Exception {

		var polygonId = polygonId("Test Polygon", 2023);
		var layer = Layer.PRIMARY;

		testWith(
				Arrays.asList(getTestPolygon(polygonId, valid())), //
				Arrays.asList(layerMap(getTestPrimaryLayer(polygonId, x -> {
					x.setYearsToBreastHeight(0.2f);
				}))), //
				Arrays.asList(Collections.singletonList(getTestSpecies(polygonId, layer, valid()))), //
				(app, controlMap) -> {
					var ex = assertThrows(ProcessingException.class, () -> app.process());

					assertThat(
							ex,
							hasProperty(
									"message",
									is(
											"Polygon " + polygonId + " has " + Layer.PRIMARY
													+ " layer where years to breast height 0.2 is less than minimum 0.5 years."
									)
							)
					);

				}
		);

	}

	@Test
	void testPrimaryLayerTotalAgeLessThanYearsToBreastHeight() throws Exception {

		var polygonId = polygonId("Test Polygon", 2023);
		var layer = Layer.PRIMARY;

		// FIXME VDYP7 actually tests if total age - YTBH is less than 0.5 but gives an
		// error that total age is "less than" YTBH. Replicating that for now but
		// consider changing it.

		testWith(
				Arrays.asList(getTestPolygon(polygonId, valid())), //
				Arrays.asList(layerMap(getTestPrimaryLayer(polygonId, x -> {
					x.setAgeTotal(7f);
					x.setYearsToBreastHeight(8f);
				}))), //
				Arrays.asList(Collections.singletonList(getTestSpecies(polygonId, layer, valid()))), //
				(app, controlMap) -> {

					var ex = assertThrows(ProcessingException.class, () -> app.process());

					assertThat(
							ex,
							hasProperty(
									"message",
									is(
											"Polygon " + polygonId + " has " + Layer.PRIMARY
													+ " layer where total age is less than YTBH."
									)
							)
					);
				}
		);

	}

	@Test
	void testPrimaryLayerSiteIndexLessThanMinimum() throws Exception {

		var polygonId = polygonId("Test Polygon", 2023);
		var layer = Layer.PRIMARY;

		testWith(
				Arrays.asList(getTestPolygon(polygonId, valid())), //
				Arrays.asList(layerMap(getTestPrimaryLayer(polygonId, x -> {
					x.setSiteIndex(0.2f);
				}))), //
				Arrays.asList(Collections.singletonList(getTestSpecies(polygonId, layer, valid()))), //
				(app, controlMap) -> {
					var ex = assertThrows(ProcessingException.class, () -> app.process());

					assertThat(
							ex,
							hasProperty(
									"message",
									is(
											"Polygon " + polygonId + " has " + Layer.PRIMARY
													+ " layer where site index 0.2 is less than minimum 0.5 years."
									)
							)
					);

				}
		);

	}

	@Test
	void testPolygonWithModeFipYoung() throws Exception {

		var polygonId = polygonId("Test Polygon", 2023);
		var layer = Layer.PRIMARY;

		testWith(Arrays.asList(getTestPolygon(polygonId, x -> {
			x.setModeFip(Optional.of(FipMode.FIPYOUNG));
		})), //
				Arrays.asList(layerMap(getTestPrimaryLayer(polygonId, valid()))), //
				Arrays.asList(Collections.singletonList(getTestSpecies(polygonId, layer, valid()))), //
				(app, controlMap) -> {

					var ex = assertThrows(ProcessingException.class, () -> app.process());

					assertThat(
							ex,
							hasProperty(
									"message",
									is("Polygon " + polygonId + " is using unsupported mode " + FipMode.FIPYOUNG + ".")
							)
					);
				}
		);

	}

	@Test
	void testOneSpeciesLessThan100Percent() throws Exception {

		var polygonId = polygonId("Test Polygon", 2023);
		var layer = Layer.PRIMARY;

		testWith(
				Arrays.asList(getTestPolygon(polygonId, valid())), //
				Arrays.asList(layerMap(getTestPrimaryLayer(polygonId, valid()))), //
				Arrays.asList(Collections.singletonList(getTestSpecies(polygonId, layer, x -> {
					x.setPercentGenus(99f);
				}))), //
				(app, controlMap) -> {

					var ex = assertThrows(ProcessingException.class, () -> app.process());

					assertThat(
							ex,
							hasProperty(
									"message",
									is(
											"Polygon " + polygonId
													+ " has PRIMARY layer where species entries have a percentage total that does not sum to 100%."
									)
							)
					);
				}
		);

	}

	@Test
	void testOneSpeciesMoreThan100Percent() throws Exception {

		var polygonId = polygonId("Test Polygon", 2023);
		var layer = Layer.PRIMARY;

		testWith(
				Arrays.asList(getTestPolygon(polygonId, valid())), //
				Arrays.asList(layerMap(getTestPrimaryLayer(polygonId, valid()))), //
				Arrays.asList(Collections.singletonList(getTestSpecies(polygonId, layer, x -> {
					x.setPercentGenus(101f);
				}))), //
				(app, controlMap) -> {

					var ex = assertThrows(ProcessingException.class, () -> app.process());

					assertThat(
							ex,
							hasProperty(
									"message",
									is(
											"Polygon " + polygonId
													+ " has PRIMARY layer where species entries have a percentage total that does not sum to 100%."
									)
							)
					);
				}
		);

	}

	@Test
	void testTwoSpeciesSumTo100Percent() throws Exception {

		var polygonId = polygonId("Test Polygon", 2023);
		var layer = Layer.PRIMARY;

		testWith(
				FipTestUtils.loadControlMap(), Arrays.asList(getTestPolygon(polygonId, valid())), //
				Arrays.asList(layerMap(getTestPrimaryLayer(polygonId, valid()))), //
				Arrays.asList(
						Arrays.asList(
								//
								getTestSpecies(polygonId, layer, "B", x -> {
									x.setPercentGenus(75f);
								}), getTestSpecies(polygonId, layer, "P", x -> {
									x.setPercentGenus(25f);
								})
						)
				), //
				(app, controlMap) -> {

					assertDoesNotThrow(() -> app.process());

				}
		);

	}

	@Test
	void testTwoSpeciesSumToLessThan100Percent() throws Exception {

		var polygonId = polygonId("Test Polygon", 2023);
		var layer = Layer.PRIMARY;

		testWith(
				Arrays.asList(getTestPolygon(polygonId, valid())), //
				Arrays.asList(layerMap(getTestPrimaryLayer(polygonId, valid()))), //
				Arrays.asList(
						Arrays.asList(
								//
								getTestSpecies(polygonId, layer, "B", x -> {
									x.setPercentGenus(75 - 1f);
								}), getTestSpecies(polygonId, layer, "P", x -> {
									x.setPercentGenus(25f);
								})
						)
				), //
				(app, controlMap) -> {

					var ex = assertThrows(ProcessingException.class, () -> app.process());

					assertThat(
							ex,
							hasProperty(
									"message",
									is(
											"Polygon " + polygonId
													+ " has PRIMARY layer where species entries have a percentage total that does not sum to 100%."
									)
							)
					);
				}
		);

	}

	@Test
	void testTwoSpeciesSumToMoreThan100Percent() throws Exception {

		var polygonId = polygonId("Test Polygon", 2023);
		var layer = Layer.PRIMARY;

		testWith(
				Arrays.asList(getTestPolygon(polygonId, valid())), //
				Arrays.asList(layerMap(getTestPrimaryLayer(polygonId, valid()))), //
				Arrays.asList(
						Arrays.asList(
								//
								getTestSpecies(polygonId, layer, "B", x -> {
									x.setPercentGenus(75 + 1f);
								}), getTestSpecies(polygonId, layer, "P", x -> {
									x.setPercentGenus(25f);
								})
						)
				), //
				(app, controlMap) -> {

					var ex = assertThrows(ProcessingException.class, () -> app.process());

					assertThat(
							ex,
							hasProperty(
									"message",
									is(
											"Polygon " + polygonId
													+ " has PRIMARY layer where species entries have a percentage total that does not sum to 100%."
									)
							)
					);
				}
		);

	}

	@Test
	void testFractionGenusCalculation() throws Exception {

		var polygonId = polygonId("Test Polygon", 2023);
		var layer = Layer.PRIMARY;

		final var speciesList = Arrays.asList(
				//
				getTestSpecies(polygonId, layer, "B", x -> {
					x.setPercentGenus(75f);
				}), getTestSpecies(polygonId, layer, "P", x -> {
					x.setPercentGenus(25f);
				})
		);
		testWith(
				FipTestUtils.loadControlMap(), Arrays.asList(getTestPolygon(polygonId, valid())), //
				Arrays.asList(layerMap(getTestPrimaryLayer(polygonId, valid()))), //
				Arrays.asList(speciesList), //
				(app, controlMap) -> {

					app.process();

					// Testing exact floating point equality is intentional
					assertThat(
							speciesList, contains(
									//
									allOf(hasProperty("genus", is("B")), hasProperty("fractionGenus", is(0.75f))), //
									allOf(hasProperty("genus", is("P")), hasProperty("fractionGenus", is(0.25f)))//
							)
					);
				}
		);

	}

	@Test
	void testFractionGenusCalculationWithSlightError() throws Exception {

		var polygonId = polygonId("Test Polygon", 2023);
		var layer = Layer.PRIMARY;

		final var speciesList = Arrays.asList(
				//
				getTestSpecies(polygonId, layer, "B", x -> {
					x.setPercentGenus(75 + 0.009f);
				}), getTestSpecies(polygonId, layer, "P", x -> {
					x.setPercentGenus(25f);
				})
		);
		testWith(
				FipTestUtils.loadControlMap(), Arrays.asList(getTestPolygon(polygonId, valid())), //
				Arrays.asList(layerMap(getTestPrimaryLayer(polygonId, valid()))), //
				Arrays.asList(speciesList), //
				(app, controlMap) -> {

					app.process();

					// Testing exact floating point equality is intentional
					assertThat(
							speciesList, contains(
									//
									allOf(hasProperty("genus", is("B")), hasProperty("fractionGenus", is(0.75002253f))), //
									allOf(hasProperty("genus", is("P")), hasProperty("fractionGenus", is(0.2499775f)))//
							)
					);
				}
		);

	}

	@Test
	void testProcessVeteran() throws Exception {

		var polygonId = polygonId("Test Polygon", 2023);

		var fipPolygon = getTestPolygon(polygonId, valid());
		var fipLayer = getTestVeteranLayer(polygonId, valid());
		var fipSpecies = getTestSpecies(polygonId, Layer.VETERAN, valid());
		fipPolygon.setLayers(Collections.singletonMap(Layer.VETERAN, fipLayer));
		fipLayer.setSpecies(Collections.singletonMap(fipSpecies.getGenus(), fipSpecies));

		var controlMap = new HashMap<String, Object>();
		TestUtils.populateControlMapBecReal(controlMap);
		TestUtils.populateControlMapGenusReal(controlMap);
		TestUtils.populateControlMapVeteranBq(controlMap);
		TestUtils.populateControlMapEquationGroups(controlMap, (s, b) -> new int[] { 1, 1, 1 });
		TestUtils.populateControlMapVeteranDq(controlMap, (s, r) -> new float[] { 0f, 0f, 0f });
		TestUtils.populateControlMapVeteranVolAdjust(controlMap, s -> new float[] { 0f, 0f, 0f, 0f });
		TestUtils.populateControlMapWholeStemVolume(controlMap, wholeStemMap(1));
		TestUtils.populateControlMapCloseUtilization(controlMap, closeUtilMap(1));
		TestUtils.populateControlMapNetDecay(controlMap, closeUtilMap(1));
		FipTestUtils.populateControlMapDecayModifiers(controlMap, (s, r) -> 0f);
		TestUtils.populateControlMapNetWaste(
				controlMap, s -> new Coefficients(new float[] { 0f, 0f, 0f, 0f, 0f, 0f }, 0)
		);
		FipTestUtils.populateControlMapWasteModifiers(controlMap, (s, r) -> 0f);
		TestUtils
				.populateControlMapNetBreakage(controlMap, bgrp -> new Coefficients(new float[] { 0f, 0f, 0f, 0f }, 1));

		var app = new FipStart();
		app.setControlMap(controlMap);

		var result = app.processLayerAsVeteran(fipPolygon, fipLayer);

		assertThat(result, notNullValue());

		// Keys
		assertThat(result, hasProperty("polygonIdentifier", is(polygonId)));
		assertThat(result, hasProperty("layer", is(Layer.VETERAN)));

		// Direct Copy
		assertThat(result, hasProperty("ageTotal", is(8f)));
		assertThat(result, hasProperty("height", is(6f)));
		assertThat(result, hasProperty("yearsToBreastHeight", is(7f)));

		// Computed
		assertThat(result, hasProperty("breastHeightAge", is(1f)));

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
		assertThat(speciesResult, hasProperty("layer", is(Layer.VETERAN)));
		assertThat(speciesResult, hasProperty("genus", is("B")));

		// Copied
		assertThat(speciesResult, hasProperty("percentGenus", is(100f)));

		// Species distribution
		assertThat(speciesResult, hasProperty("speciesPercent", anEmptyMap())); // Test map was empty
	}

	@Test
	void testProcessVeteranYearsToBreastHeightLessThanMinimum() throws Exception {

		var polygonId = polygonId("Test Polygon", 2023);

		var fipPolygon = getTestPolygon(polygonId, valid());
		var fipLayer = getTestVeteranLayer(polygonId, (l) -> {
			l.setYearsToBreastHeight(5.0f);
		});
		var fipSpecies = getTestSpecies(polygonId, Layer.VETERAN, valid());
		fipPolygon.setLayers(Collections.singletonMap(Layer.VETERAN, fipLayer));
		fipLayer.setSpecies(Collections.singletonMap(fipSpecies.getGenus(), fipSpecies));

		var controlMap = new HashMap<String, Object>();
		TestUtils.populateControlMapBecReal(controlMap);
		TestUtils.populateControlMapGenusReal(controlMap);
		TestUtils.populateControlMapVeteranBq(controlMap);
		TestUtils.populateControlMapEquationGroups(controlMap, (s, b) -> new int[] { 1, 1, 1 });
		TestUtils.populateControlMapVeteranDq(controlMap, (s, r) -> new float[] { 0f, 0f, 0f });
		TestUtils.populateControlMapVeteranVolAdjust(controlMap, s -> new float[] { 0f, 0f, 0f, 0f });
		TestUtils.populateControlMapWholeStemVolume(controlMap, (wholeStemMap(1)));
		TestUtils.populateControlMapCloseUtilization(controlMap, closeUtilMap(1));
		TestUtils.populateControlMapNetDecay(controlMap, closeUtilMap(1));
		FipTestUtils.populateControlMapDecayModifiers(controlMap, (s, r) -> 0f);
		TestUtils.populateControlMapNetWaste(
				controlMap, s -> new Coefficients(new float[] { 0f, 0f, 0f, 0f, 0f, 0f }, 0)
		);
		FipTestUtils.populateControlMapWasteModifiers(controlMap, (s, r) -> 0f);
		TestUtils
				.populateControlMapNetBreakage(controlMap, bgrp -> new Coefficients(new float[] { 0f, 0f, 0f, 0f }, 1));

		var app = new FipStart();
		app.setControlMap(controlMap);

		var result = app.processLayerAsVeteran(fipPolygon, fipLayer);

		assertThat(result, notNullValue());

		// Set minimum
		assertThat(result, hasProperty("yearsToBreastHeight", is(6f)));

		// Computed based on minimum
		assertThat(result, hasProperty("breastHeightAge", is(2f)));

	}

	@Test
	void testProcessVeteranWithSpeciesDistribution() throws Exception {

		var polygonId = polygonId("Test Polygon", 2023);

		var fipPolygon = getTestPolygon(polygonId, valid());
		var fipLayer = getTestVeteranLayer(polygonId, valid());
		var fipSpecies = getTestSpecies(polygonId, Layer.VETERAN, x -> {
			var map = new LinkedHashMap<String, Float>();
			map.put("S1", 75f);
			map.put("S2", 25f);
			x.setSpeciesPercent(map);
		});
		fipPolygon.setLayers(Collections.singletonMap(Layer.VETERAN, fipLayer));
		fipLayer.setSpecies(Collections.singletonMap(fipSpecies.getGenus(), fipSpecies));

		var controlMap = new HashMap<String, Object>();
		TestUtils.populateControlMapBecReal(controlMap);
		TestUtils.populateControlMapGenusReal(controlMap);
		TestUtils.populateControlMapVeteranBq(controlMap);
		TestUtils.populateControlMapEquationGroups(controlMap, (s, b) -> new int[] { 1, 1, 1 });
		TestUtils.populateControlMapVeteranDq(controlMap, (s, r) -> new float[] { 0f, 0f, 0f });
		TestUtils.populateControlMapVeteranVolAdjust(controlMap, s -> new float[] { 0f, 0f, 0f, 0f });
		TestUtils.populateControlMapWholeStemVolume(controlMap, (wholeStemMap(1)));
		TestUtils.populateControlMapCloseUtilization(controlMap, closeUtilMap(1));
		TestUtils.populateControlMapNetDecay(controlMap, closeUtilMap(1));
		FipTestUtils.populateControlMapDecayModifiers(controlMap, (s, r) -> 0f);
		TestUtils.populateControlMapNetWaste(
				controlMap, s -> new Coefficients(new float[] { 0f, 0f, 0f, 0f, 0f, 0f }, 0)
		);
		FipTestUtils.populateControlMapWasteModifiers(controlMap, (s, r) -> 0f);
		TestUtils
				.populateControlMapNetBreakage(controlMap, bgrp -> new Coefficients(new float[] { 0f, 0f, 0f, 0f }, 1));

		var app = new FipStart();
		app.setControlMap(controlMap);

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
		assertThat(speciesResult, hasProperty("layer", is(Layer.VETERAN)));
		assertThat(speciesResult, hasProperty("genus", is("B")));

		// Copied
		assertThat(speciesResult, hasProperty("percentGenus", is(100f)));

		// Species distribution
		assertThat(speciesResult, hasProperty("speciesPercent", aMapWithSize(2)));

		var distributionResult = speciesResult.getSpeciesPercent();

		assertThat(distributionResult, hasEntry("S1", 75f));
		assertThat(distributionResult, hasEntry("S2", 25f));

	}

	@Test
	void testProcessVeteranSoleSpeciesIsPrimary() throws Exception {

		var polygonId = polygonId("Test Polygon", 2023);

		var fipPolygon = getTestPolygon(polygonId, valid());
		var fipLayer = getTestVeteranLayer(polygonId, valid());
		var fipSpecies = getTestSpecies(polygonId, Layer.VETERAN, x -> {
			var map = new LinkedHashMap<String, Float>();
			map.put("S1", 75f);
			map.put("S2", 25f);
			x.setSpeciesPercent(map);
		});
		fipPolygon.setLayers(Collections.singletonMap(Layer.VETERAN, fipLayer));
		fipLayer.setSpecies(Collections.singletonMap(fipSpecies.getGenus(), fipSpecies));

		var controlMap = new HashMap<String, Object>();
		TestUtils.populateControlMapBecReal(controlMap);
		TestUtils.populateControlMapGenusReal(controlMap);
		TestUtils.populateControlMapVeteranBq(controlMap);
		TestUtils.populateControlMapEquationGroups(controlMap, (s, b) -> new int[] { 1, 1, 1 });
		TestUtils.populateControlMapVeteranDq(controlMap, (s, r) -> new float[] { 0f, 0f, 0f });
		TestUtils.populateControlMapVeteranVolAdjust(controlMap, s -> new float[] { 0f, 0f, 0f, 0f });
		TestUtils.populateControlMapWholeStemVolume(controlMap, (wholeStemMap(1)));
		TestUtils.populateControlMapCloseUtilization(controlMap, closeUtilMap(1));
		TestUtils.populateControlMapNetDecay(controlMap, closeUtilMap(1));
		FipTestUtils.populateControlMapDecayModifiers(controlMap, (s, r) -> 0f);
		TestUtils.populateControlMapNetWaste(
				controlMap, s -> new Coefficients(new float[] { 0f, 0f, 0f, 0f, 0f, 0f }, 0)
		);
		FipTestUtils.populateControlMapWasteModifiers(controlMap, (s, r) -> 0f);
		TestUtils
				.populateControlMapNetBreakage(controlMap, bgrp -> new Coefficients(new float[] { 0f, 0f, 0f, 0f }, 1));

		var app = new FipStart();
		app.setControlMap(controlMap);

		var result = app.processLayerAsVeteran(fipPolygon, fipLayer);

		assertThat(result, notNullValue());

		assertThat(result, hasProperty("primaryGenus", is("B")));
		assertThat(
				result, hasProperty(
						"primarySpeciesRecord", is(
								allOf(
										hasProperty("genus", is("B")) //
								)
						)
				)
		);

	}

	@Test
	void testProcessVeteranBiggestSpeciesIsPrimary() throws Exception {

		var polygonId = polygonId("Test Polygon", 2023);

		var fipPolygon = getTestPolygon(polygonId, valid());
		var fipLayer = getTestVeteranLayer(polygonId, valid());
		var fipSpecies1 = getTestSpecies(polygonId, Layer.VETERAN, "B", x -> {
			var map = new LinkedHashMap<String, Float>();
			map.put("S1", 75f);
			map.put("S2", 25f);
			x.setSpeciesPercent(map);
			x.setPercentGenus(60f);
		});
		var fipSpecies2 = getTestSpecies(polygonId, Layer.VETERAN, "C", x -> {
			var map = new LinkedHashMap<String, Float>();
			map.put("S3", 75f);
			map.put("S4", 25f);
			x.setSpeciesPercent(map);
			x.setPercentGenus(40f);
		});
		fipPolygon.setLayers(Collections.singletonMap(Layer.VETERAN, fipLayer));
		var speciesMap = new HashMap<String, FipSpecies>();
		speciesMap.put("B", fipSpecies1);
		speciesMap.put("C", fipSpecies2);
		fipLayer.setSpecies(speciesMap);

		var controlMap = new HashMap<String, Object>();
		TestUtils.populateControlMapBecReal(controlMap);
		TestUtils.populateControlMapGenusReal(controlMap);
		TestUtils.populateControlMapVeteranBq(controlMap);
		TestUtils.populateControlMapEquationGroups(controlMap, (s, b) -> new int[] { 1, 1, 1 });
		TestUtils.populateControlMapVeteranDq(controlMap, (s, r) -> new float[] { 0f, 0f, 0f });
		TestUtils.populateControlMapVeteranVolAdjust(controlMap, s -> new float[] { 0f, 0f, 0f, 0f });
		TestUtils.populateControlMapWholeStemVolume(controlMap, (wholeStemMap(1)));
		TestUtils.populateControlMapCloseUtilization(controlMap, closeUtilMap(1));
		TestUtils.populateControlMapNetDecay(controlMap, closeUtilMap(1));
		FipTestUtils.populateControlMapDecayModifiers(controlMap, (s, r) -> 0f);
		TestUtils.populateControlMapNetWaste(
				controlMap, s -> new Coefficients(new float[] { 0f, 0f, 0f, 0f, 0f, 0f }, 0)
		);
		FipTestUtils.populateControlMapWasteModifiers(controlMap, (s, r) -> 0f);
		TestUtils
				.populateControlMapNetBreakage(controlMap, bgrp -> new Coefficients(new float[] { 0f, 0f, 0f, 0f }, 1));

		var app = new FipStart();
		app.setControlMap(controlMap);

		var result = app.processLayerAsVeteran(fipPolygon, fipLayer);

		assertThat(result, notNullValue());

		assertThat(result, hasProperty("primaryGenus", is("B")));
		assertThat(
				result, hasProperty(
						"primarySpeciesRecord", is(
								allOf(
										hasProperty("genus", is("B")) //
								)
						)
				)
		);

	}

	@Test
	void testEstimateVeteranLayerBaseArea() throws Exception {

		var controlMap = FipTestUtils.loadControlMap();

		var app = new FipStart();
		app.setControlMap(controlMap);

		var result = app.estimateVeteranBaseArea(26.2000008f, 4f, "H", Region.COASTAL);

		assertThat(result, closeTo(2.24055195f));
	}

	void populateControlMapVeteranVolumeAdjust(HashMap<String, Object> controlMap, Function<String, float[]> mapper) {
		var map = GenusDefinitionParser.getSpeciesAliases(controlMap).stream()
				.collect(Collectors.toMap(x -> x, mapper.andThen(x -> new Coefficients(x, 1))));

		controlMap.put(VeteranLayerVolumeAdjustParser.CONTROL_KEY, map);
	}

	@Test
	void testVeteranLayerLoreyHeight() throws Exception {

		var polygonId = polygonId("Test Polygon", 2023);

		var fipPolygon = getTestPolygon(polygonId, valid());
		var fipLayer = getTestVeteranLayer(polygonId, valid());
		var fipSpecies = getTestSpecies(polygonId, Layer.VETERAN, x -> {
			var map = new LinkedHashMap<String, Float>();
			map.put("S1", 100f);
			x.setSpeciesPercent(map);
		});
		fipPolygon.setLayers(Collections.singletonMap(Layer.VETERAN, fipLayer));
		fipLayer.setSpecies(Collections.singletonMap(fipSpecies.getGenus(), fipSpecies));

		var controlMap = new HashMap<String, Object>();
		TestUtils.populateControlMapBecReal(controlMap);
		TestUtils.populateControlMapGenusReal(controlMap);
		TestUtils.populateControlMapVeteranBq(controlMap);
		TestUtils.populateControlMapEquationGroups(controlMap, (s, b) -> new int[] { 1, 1, 1 });
		TestUtils.populateControlMapVeteranDq(controlMap, (s, r) -> new float[] { 0f, 0f, 0f });
		TestUtils.populateControlMapVeteranVolAdjust(controlMap, s -> new float[] { 0f, 0f, 0f, 0f });
		TestUtils.populateControlMapWholeStemVolume(controlMap, (wholeStemMap(1)));
		TestUtils.populateControlMapCloseUtilization(controlMap, closeUtilMap(1));
		TestUtils.populateControlMapNetDecay(controlMap, closeUtilMap(1));
		FipTestUtils.populateControlMapDecayModifiers(controlMap, (s, r) -> 0f);
		TestUtils.populateControlMapNetWaste(
				controlMap, s -> new Coefficients(new float[] { 0f, 0f, 0f, 0f, 0f, 0f }, 0)
		);
		FipTestUtils.populateControlMapWasteModifiers(controlMap, (s, r) -> 0f);
		TestUtils
				.populateControlMapNetBreakage(controlMap, bgrp -> new Coefficients(new float[] { 0f, 0f, 0f, 0f }, 1));

		var app = new FipStart();
		app.setControlMap(controlMap);

		var result = app.processLayerAsVeteran(fipPolygon, fipLayer).getPrimarySpeciesRecord();

		Matcher<Float> heightMatcher = closeTo(6f);
		Matcher<Float> zeroMatcher = is(0.0f);
		// Expect the estimated HL in 0 (-1 to 0)
		assertThat(result, hasProperty("loreyHeightByUtilization", contains(zeroMatcher, heightMatcher)));

	}

	@Test
	void testVeteranLayerEquationGroups() throws Exception {

		var polygonId = polygonId("Test Polygon", 2023);

		var fipPolygon = getTestPolygon(polygonId, valid());
		var fipLayer = getTestVeteranLayer(polygonId, valid());
		var fipSpecies = getTestSpecies(polygonId, Layer.VETERAN, x -> {
			var map = new LinkedHashMap<String, Float>();
			map.put("S1", 100f);
			x.setSpeciesPercent(map);
		});
		fipPolygon.setLayers(Collections.singletonMap(Layer.VETERAN, fipLayer));
		fipLayer.setSpecies(Collections.singletonMap(fipSpecies.getGenus(), fipSpecies));

		var controlMap = new HashMap<String, Object>();
		TestUtils.populateControlMapBecReal(controlMap);
		TestUtils.populateControlMapGenusReal(controlMap);
		TestUtils.populateControlMapVeteranBq(controlMap);
		TestUtils.populateControlMapEquationGroups(
				controlMap, (s, b) -> s.equals("B") && b.equals("BG") ? new int[] { 1, 2, 3 } : new int[] { 0, 0, 0 }
		);
		TestUtils.populateControlMapVeteranDq(controlMap, (s, r) -> new float[] { 0f, 0f, 0f });
		TestUtils.populateControlMapVeteranVolAdjust(controlMap, s -> new float[] { 0f, 0f, 0f, 0f });
		TestUtils.populateControlMapWholeStemVolume(controlMap, (wholeStemMap(1)));
		TestUtils.populateControlMapCloseUtilization(controlMap, closeUtilMap(1));
		TestUtils.populateControlMapNetDecay(controlMap, closeUtilMap(2));
		FipTestUtils.populateControlMapDecayModifiers(controlMap, (s, r) -> 0f);
		TestUtils.populateControlMapNetWaste(
				controlMap, s -> new Coefficients(new float[] { 0f, 0f, 0f, 0f, 0f, 0f }, 0)
		);
		FipTestUtils.populateControlMapWasteModifiers(controlMap, (s, r) -> 0f);
		TestUtils
				.populateControlMapNetBreakage(controlMap, bgrp -> new Coefficients(new float[] { 0f, 0f, 0f, 0f }, 1));

		var app = new FipStart();
		app.setControlMap(controlMap);

		var result = app.processLayerAsVeteran(fipPolygon, fipLayer).getPrimarySpeciesRecord();

		assertThat(result, hasProperty("volumeGroup", is(1)));
		assertThat(result, hasProperty("decayGroup", is(2)));
		assertThat(result, hasProperty("breakageGroup", is(3)));

	}

	@Test
	void testEstimateVeteranLayerDQ() throws Exception {

		var polygonId = polygonId("Test Polygon", 2023);

		var fipPolygon = getTestPolygon(polygonId, valid());
		var fipLayer = getTestVeteranLayer(polygonId, x -> {
			x.setHeight(10f);
		});
		var fipSpecies1 = getTestSpecies(polygonId, Layer.VETERAN, "B", x -> {
			var map = new LinkedHashMap<String, Float>();
			map.put("S1", 75f);
			map.put("S2", 25f);
			x.setSpeciesPercent(map);
			x.setPercentGenus(60f);
		});
		var fipSpecies2 = getTestSpecies(polygonId, Layer.VETERAN, "C", x -> {
			var map = new LinkedHashMap<String, Float>();
			map.put("S3", 75f);
			map.put("S4", 25f);
			x.setSpeciesPercent(map);
			x.setPercentGenus(40f);
		});
		fipPolygon.setLayers(Collections.singletonMap(Layer.VETERAN, fipLayer));
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
		TestUtils.populateControlMapWholeStemVolume(controlMap, (wholeStemMap(1)));
		TestUtils.populateControlMapCloseUtilization(controlMap, closeUtilMap(1));
		TestUtils.populateControlMapNetDecay(controlMap, closeUtilMap(1));
		FipTestUtils.populateControlMapDecayModifiers(controlMap, (s, r) -> 0f);
		TestUtils.populateControlMapNetWaste(
				controlMap, s -> new Coefficients(new float[] { 0f, 0f, 0f, 0f, 0f, 0f }, 0)
		);
		FipTestUtils.populateControlMapWasteModifiers(controlMap, (s, r) -> 0f);
		TestUtils
				.populateControlMapNetBreakage(controlMap, bgrp -> new Coefficients(new float[] { 0f, 0f, 0f, 0f }, 1));

		var app = new FipStart();
		app.setControlMap(controlMap);

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

	static BiFunction<Integer, Integer, Optional<Coefficients>> wholeStemMap(int group) {
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

	static BiFunction<Integer, Integer, Optional<Coefficients>> closeUtilMap(int group) {
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

	static BiFunction<Integer, Integer, Optional<Coefficients>> netDecayMap(int group) {
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

	@Test
	void testEstimateVeteranWholeStemVolume() throws Exception {
		var polygonId = polygonId("Test Polygon", 2023);

		var fipPolygon = getTestPolygon(polygonId, valid());
		var fipLayer = getTestVeteranLayer(polygonId, valid());
		var fipSpecies = getTestSpecies(polygonId, Layer.VETERAN, valid());
		fipPolygon.setLayers(Collections.singletonMap(Layer.VETERAN, fipLayer));
		fipLayer.setSpecies(Collections.singletonMap(fipSpecies.getGenus(), fipSpecies));

		var controlMap = new HashMap<String, Object>();
		TestUtils.populateControlMapWholeStemVolume(controlMap, wholeStemMap(12));

		var app = new FipStart();
		app.setControlMap(controlMap);

		var utilizationClass = 4;
		var aAdjust = 0.10881f;
		var volumeGroup = 12;
		var lorieHeight = 26.2000008f;
		var quadMeanDiameterUtil = new Coefficients(new float[] { 51.8356705f, 0f, 0f, 0f, 51.8356705f }, 0);
		var baseAreaUtil = new Coefficients(new float[] { 0.492921442f, 0f, 0f, 0f, 0.492921442f }, 0);
		var wholeStemVolumeUtil = new Coefficients(new float[] { 0f, 0f, 0f, 0f, 0f }, 0);

		app.estimateWholeStemVolume(
				utilizationClass, aAdjust, volumeGroup, lorieHeight, quadMeanDiameterUtil, baseAreaUtil,
				wholeStemVolumeUtil
		);

		assertThat(wholeStemVolumeUtil, coe(0, contains(is(0f), is(0f), is(0f), is(0f), closeTo(6.11904192f))));

	}

	@Test
	void testEstimateVeteranCloseUtilization() throws Exception {
		var polygonId = polygonId("Test Polygon", 2023);

		var fipPolygon = getTestPolygon(polygonId, valid());
		var fipLayer = getTestVeteranLayer(polygonId, valid());
		var fipSpecies = getTestSpecies(polygonId, Layer.VETERAN, valid());
		fipPolygon.setLayers(Collections.singletonMap(Layer.VETERAN, fipLayer));
		fipLayer.setSpecies(Collections.singletonMap(fipSpecies.getGenus(), fipSpecies));

		var controlMap = new HashMap<String, Object>();
		TestUtils.populateControlMapCloseUtilization(controlMap, closeUtilMap(12));

		var app = new FipStart();
		app.setControlMap(controlMap);

		var utilizationClass = 4;
		var aAdjust = new Coefficients(new float[] { 0f, 0f, 0f, -0.0981800035f }, 1);
		var volumeGroup = 12;
		var lorieHeight = 26.2000008f;
		var quadMeanDiameterUtil = new Coefficients(new float[] { 51.8356705f, 0f, 0f, 0f, 51.8356705f }, 0);
		var wholeStemVolumeUtil = new Coefficients(new float[] { 0f, 0f, 0f, 0f, 6.11904192f }, 0);

		var closeUtilizationUtil = new Coefficients(new float[] { 0f, 0f, 0f, 0f, 0f }, 0);

		app.estimateCloseUtilizationVolume(
				utilizationClass, aAdjust, volumeGroup, lorieHeight, quadMeanDiameterUtil, wholeStemVolumeUtil,
				closeUtilizationUtil
		);

		assertThat(closeUtilizationUtil, coe(0, contains(is(0f), is(0f), is(0f), is(0f), closeTo(5.86088896f))));

	}

	@Test
	void testEstimateVeteranNetDecay() throws Exception {
		var polygonId = polygonId("Test Polygon", 2023);

		var fipPolygon = getTestPolygon(polygonId, valid());
		var fipLayer = getTestVeteranLayer(polygonId, valid());
		var fipSpecies = getTestSpecies(polygonId, Layer.VETERAN, s -> {
		});
		fipPolygon.setLayers(Collections.singletonMap(Layer.VETERAN, fipLayer));
		fipLayer.setSpecies(Collections.singletonMap(fipSpecies.getGenus(), fipSpecies));

		var controlMap = new HashMap<String, Object>();
		TestUtils.populateControlMapNetDecay(controlMap, netDecayMap(7));
		FipTestUtils.populateControlMapDecayModifiers(
				controlMap, (s, r) -> s.equals("B") && r == Region.INTERIOR ? 0f : 0f
		);

		var app = new FipStart();
		app.setControlMap(controlMap);

		var utilizationClass = 4;
		var aAdjust = new Coefficients(new float[] { 0f, 0f, 0f, 0.000479999988f }, 1);
		var decayGroup = 7;
		var lorieHeight = 26.2000008f;
		var breastHeightAge = 97.9000015f;
		var quadMeanDiameterUtil = new Coefficients(new float[] { 51.8356705f, 0f, 0f, 0f, 51.8356705f }, 0);
		var closeUtilizationUtil = new Coefficients(new float[] { 0f, 0f, 0f, 0f, 5.86088896f }, 0);

		var closeUtilizationNetOfDecayUtil = new Coefficients(new float[] { 0f, 0f, 0f, 0f, 0f }, 0);

		app.estimateNetDecayVolume(
				fipSpecies.getGenus(), Region.INTERIOR, utilizationClass, aAdjust, decayGroup, lorieHeight,
				breastHeightAge, quadMeanDiameterUtil, closeUtilizationUtil, closeUtilizationNetOfDecayUtil
		);

		assertThat(
				closeUtilizationNetOfDecayUtil, coe(0, contains(is(0f), is(0f), is(0f), is(0f), closeTo(5.64048958f)))
		);

	}

	@Test
	void testEstimateVeteranNetWaste() throws Exception {
		var polygonId = polygonId("Test Polygon", 2023);

		var fipPolygon = getTestPolygon(polygonId, valid());
		var fipLayer = getTestVeteranLayer(polygonId, valid());
		var fipSpecies = getTestSpecies(polygonId, Layer.VETERAN, s -> {
		});
		fipPolygon.setLayers(Collections.singletonMap(Layer.VETERAN, fipLayer));
		fipLayer.setSpecies(Collections.singletonMap(fipSpecies.getGenus(), fipSpecies));

		var controlMap = new HashMap<String, Object>();
		TestUtils.populateControlMapNetWaste(controlMap, s -> s.equals("B") ? //
				new Coefficients(
						new float[] { -4.20249987f, 11.2235003f, -33.0270004f, 0.124600001f, -0.231800005f, -0.1259f },
						0
				) : //
				new Coefficients(new float[] { 0f, 0f, 0f, 0f, 0f, 0f }, 0)
		);
		FipTestUtils.populateControlMapWasteModifiers(
				controlMap, (s, r) -> s.equals("B") && r == Region.INTERIOR ? 0f : 0f
		);

		var app = new FipStart();
		app.setControlMap(controlMap);

		var utilizationClass = 4;
		var aAdjust = new Coefficients(new float[] { 0f, 0f, 0f, -0.00295000011f }, 1);
		var lorieHeight = 26.2000008f;
		var breastHeightAge = 97.9000015f;
		var quadMeanDiameterUtil = new Coefficients(new float[] { 51.8356705f, 0f, 0f, 0f, 51.8356705f }, 0);
		var closeUtilizationUtil = new Coefficients(new float[] { 0f, 0f, 0f, 0f, 5.86088896f }, 0);
		var closeUtilizationNetOfDecayUtil = new Coefficients(new float[] { 0f, 0f, 0f, 0f, 5.64048958f }, 0);

		var closeUtilizationNetOfDecayAndWasteUtil = new Coefficients(new float[] { 0f, 0f, 0f, 0f, 0f }, 0);

		app.estimateNetDecayAndWasteVolume(
				Region.INTERIOR, utilizationClass, aAdjust, fipSpecies.getGenus(), lorieHeight, breastHeightAge,
				quadMeanDiameterUtil, closeUtilizationUtil, closeUtilizationNetOfDecayUtil,
				closeUtilizationNetOfDecayAndWasteUtil
		);

		assertThat(
				closeUtilizationNetOfDecayAndWasteUtil,
				coe(0, contains(is(0f), is(0f), is(0f), is(0f), closeTo(5.57935333f)))
		);

	}

	@Test
	void testEstimateVeteranNetBreakage() throws Exception {
		var polygonId = polygonId("Test Polygon", 2023);

		var fipPolygon = getTestPolygon(polygonId, valid());
		var fipLayer = getTestVeteranLayer(polygonId, valid());
		var fipSpecies = getTestSpecies(polygonId, Layer.VETERAN, s -> {
		});
		fipPolygon.setLayers(Collections.singletonMap(Layer.VETERAN, fipLayer));
		fipLayer.setSpecies(Collections.singletonMap(fipSpecies.getGenus(), fipSpecies));

		var controlMap = new HashMap<String, Object>();
		TestUtils.populateControlMapNetBreakage(controlMap, bgrp -> bgrp == 5 ? //
				new Coefficients(new float[] { 2.2269001f, 0.75059998f, 4f, 6f }, 1) : //
				new Coefficients(new float[] { 0f, 0f, 0f, 0f }, 1)
		);

		var app = new FipStart();
		app.setControlMap(controlMap);

		var utilizationClass = 4;
		var breakageGroup = 5;
		var quadMeanDiameterUtil = new Coefficients(new float[] { 51.8356705f, 0f, 0f, 0f, 51.8356705f }, 0);
		var closeUtilizationUtil = new Coefficients(new float[] { 0f, 0f, 0f, 0f, 5.86088896f }, 0);
		var closeUtilizationNetOfDecayAndWasteUtil = new Coefficients(new float[] { 0f, 0f, 0f, 0f, 5.57935333f }, 0);

		var closeUtilizationNetOfDecayWasteAndBreakageUtil = new Coefficients(new float[] { 0f, 0f, 0f, 0f, 0f }, 0);

		app.estimateNetDecayWasteAndBreakageVolume(
				utilizationClass, breakageGroup, quadMeanDiameterUtil, closeUtilizationUtil,
				closeUtilizationNetOfDecayAndWasteUtil, closeUtilizationNetOfDecayWasteAndBreakageUtil
		);

		assertThat(
				closeUtilizationNetOfDecayWasteAndBreakageUtil,
				coe(0, contains(is(0f), is(0f), is(0f), is(0f), closeTo(5.27515411f)))
		);

	}

	@Test
	void testProcessAsVeteranLayer() throws Exception {

		var polygonId = "01002 S000002 00     1970";

		var fipPolygon = getTestPolygon(polygonId, x -> {
			x.setBiogeoclimaticZone("CWH");
			x.setForestInventoryZone("A");
			x.setYieldFactor(1f);
		});

		var fipLayer = getTestVeteranLayer(polygonId, x -> {
			x.setAgeTotal(105f);
			x.setHeight(26.2f);
			x.setSiteIndex(16.7f);
			x.setCrownClosure(4.0f);
			x.setSiteGenus("H");
			x.setSiteSpecies("H");
			x.setYearsToBreastHeight(7.1f);
		});
		var fipSpecies1 = getTestSpecies(polygonId, Layer.VETERAN, "B", x -> {
			var map = new LinkedHashMap<String, Float>();
			x.setSpeciesPercent(map);
			x.setPercentGenus(22f);
		});
		var fipSpecies2 = getTestSpecies(polygonId, Layer.VETERAN, "H", x -> {
			var map = new LinkedHashMap<String, Float>();
			x.setSpeciesPercent(map);
			x.setPercentGenus(60f);
		});
		var fipSpecies3 = getTestSpecies(polygonId, Layer.VETERAN, "S", x -> {
			var map = new LinkedHashMap<String, Float>();
			x.setSpeciesPercent(map);
			x.setPercentGenus(18f);
		});
		fipPolygon.setLayers(Collections.singletonMap(Layer.VETERAN, fipLayer));
		var speciesMap = new HashMap<String, FipSpecies>();
		speciesMap.put("B", fipSpecies1);
		speciesMap.put("H", fipSpecies2);
		speciesMap.put("S", fipSpecies3);
		fipLayer.setSpecies(speciesMap);

		var controlMap = FipTestUtils.loadControlMap();

		var app = new FipStart();
		app.setControlMap(controlMap);

		var result = app.processLayerAsVeteran(fipPolygon, fipLayer);

		assertThat(result, hasProperty("polygonIdentifier", is(polygonId)));
		assertThat(result, hasProperty("layer", is(Layer.VETERAN)));

		assertThat(result, hasProperty("ageTotal", closeTo(105f))); // LVCOM3/AGETOTLV
		assertThat(result, hasProperty("breastHeightAge", closeTo(97.9000015f))); // LVCOM3/AGEBHLV
		assertThat(result, hasProperty("yearsToBreastHeight", closeTo(7.0999999f))); // LVCOM3/YTBHLV
		assertThat(result, hasProperty("height", closeTo(26.2000008f))); // LVCOM3/HDLV

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
			assertThat(result, matchGenerator.apply(24.7540474f));
			assertThat(result.getSpecies().get("B"), matchGenerator.apply(6.11904192f));
			assertThat(result.getSpecies().get("H"), matchGenerator.apply(14.5863571f));
			assertThat(result.getSpecies().get("S"), matchGenerator.apply(4.04864883f));
		});
		vetUtilization("closeUtilizationVolumeByUtilization", matchGenerator -> {
			assertThat(result, matchGenerator.apply(23.6066074f));
			assertThat(result.getSpecies().get("B"), matchGenerator.apply(5.86088896f));
			assertThat(result.getSpecies().get("H"), matchGenerator.apply(13.9343023f));
			assertThat(result.getSpecies().get("S"), matchGenerator.apply(3.81141663f));
		});
		vetUtilization("closeUtilizationNetVolumeOfDecayByUtilization", matchGenerator -> {
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

	@Test
	void testFindPrimarySpeciesNoSpecies() throws Exception {
		var controlMap = FipTestUtils.loadControlMap();
		var app = new FipStart();
		app.setControlMap(controlMap);

		Map<String, FipSpecies> allSpecies = Collections.emptyMap();
		assertThrows(IllegalArgumentException.class, () -> app.findPrimarySpecies(allSpecies));
	}

	@Test
	void testFindPrimarySpeciesOneSpecies() throws Exception {
		var controlMap = FipTestUtils.loadControlMap();
		var app = new FipStart();
		app.setControlMap(controlMap);

		var spec = this.getTestSpecies("test polygon", Layer.PRIMARY, "B", valid());

		Map<String, FipSpecies> allSpecies = Collections.singletonMap("B", spec);
		var result = app.findPrimarySpecies(allSpecies);

		assertThat(result, hasSize(1));
		assertThat(result, contains(allOf(hasProperty("genus", is("B")), hasProperty("percentGenus", closeTo(100f)))));
	}

	@Test
	void testFindPrimaryCombinePAIntoPL() throws Exception {
		var controlMap = FipTestUtils.loadControlMap();
		var app = new FipStart();
		app.setControlMap(controlMap);

		var spec1 = this.getTestSpecies("test polygon", Layer.PRIMARY, "PA", spec -> {
			spec.setPercentGenus(25);
		});
		var spec2 = this.getTestSpecies("test polygon", Layer.PRIMARY, "PL", spec -> {
			spec.setPercentGenus(75);
		});

		Map<String, FipSpecies> allSpecies = new HashMap<>();
		allSpecies.put(spec1.getGenus(), spec1);
		allSpecies.put(spec2.getGenus(), spec2);

		var result = app.findPrimarySpecies(allSpecies);

		assertThat(result, hasSize(1));
		assertThat(result, contains(allOf(hasProperty("genus", is("PL")), hasProperty("percentGenus", closeTo(100f)))));
	}

	@Test
	void testFindPrimaryCombinePLIntoPA() throws Exception {
		var controlMap = FipTestUtils.loadControlMap();
		var app = new FipStart();
		app.setControlMap(controlMap);

		var spec1 = this.getTestSpecies("test polygon", Layer.PRIMARY, "PA", spec -> {
			spec.setPercentGenus(75);
		});
		var spec2 = this.getTestSpecies("test polygon", Layer.PRIMARY, "PL", spec -> {
			spec.setPercentGenus(25);
		});

		Map<String, FipSpecies> allSpecies = new HashMap<>();
		allSpecies.put(spec1.getGenus(), spec1);
		allSpecies.put(spec2.getGenus(), spec2);

		var result = app.findPrimarySpecies(allSpecies);

		assertThat(result, hasSize(1));
		assertThat(result, contains(allOf(hasProperty("genus", is("PA")), hasProperty("percentGenus", closeTo(100f)))));
	}

	@Test
	void testFindPrimaryCombineCIntoY() throws Exception {
		var controlMap = FipTestUtils.loadControlMap();
		var app = new FipStart();
		app.setControlMap(controlMap);

		var spec1 = this.getTestSpecies("test polygon", Layer.PRIMARY, "C", spec -> {
			spec.setPercentGenus(25);
		});
		var spec2 = this.getTestSpecies("test polygon", Layer.PRIMARY, "Y", spec -> {
			spec.setPercentGenus(75);
		});

		Map<String, FipSpecies> allSpecies = new HashMap<>();
		allSpecies.put(spec1.getGenus(), spec1);
		allSpecies.put(spec2.getGenus(), spec2);

		var result = app.findPrimarySpecies(allSpecies);

		assertThat(result, hasSize(1));
		assertThat(result, contains(allOf(hasProperty("genus", is("Y")), hasProperty("percentGenus", closeTo(100f)))));
	}

	@Test
	void testFindPrimaryCombineYIntoC() throws Exception {
		var controlMap = FipTestUtils.loadControlMap();
		var app = new FipStart();
		app.setControlMap(controlMap);

		var spec1 = this.getTestSpecies("test polygon", Layer.PRIMARY, "C", spec -> {
			spec.setPercentGenus(75);
		});
		var spec2 = this.getTestSpecies("test polygon", Layer.PRIMARY, "Y", spec -> {
			spec.setPercentGenus(25);
		});

		Map<String, FipSpecies> allSpecies = new HashMap<>();
		allSpecies.put(spec1.getGenus(), spec1);
		allSpecies.put(spec2.getGenus(), spec2);

		var result = app.findPrimarySpecies(allSpecies);

		assertThat(result, hasSize(1));
		assertThat(result, contains(allOf(hasProperty("genus", is("C")), hasProperty("percentGenus", closeTo(100f)))));
	}

	@Test
	void testFindPrimarySort() throws Exception {
		var controlMap = FipTestUtils.loadControlMap();
		var app = new FipStart();
		app.setControlMap(controlMap);

		var spec1 = this.getTestSpecies("test polygon", Layer.PRIMARY, "B", spec -> {
			spec.setPercentGenus(20);
		});
		var spec2 = this.getTestSpecies("test polygon", Layer.PRIMARY, "H", spec -> {
			spec.setPercentGenus(70);
		});
		var spec3 = this.getTestSpecies("test polygon", Layer.PRIMARY, "MB", spec -> {
			spec.setPercentGenus(10);
		});

		Map<String, FipSpecies> allSpecies = new HashMap<>();
		allSpecies.put(spec1.getGenus(), spec1);
		allSpecies.put(spec2.getGenus(), spec2);
		allSpecies.put(spec3.getGenus(), spec3);

		var result = app.findPrimarySpecies(allSpecies);

		assertThat(
				result,
				contains(
						allOf(hasProperty("genus", is("H")), hasProperty("percentGenus", closeTo(70f))),
						allOf(hasProperty("genus", is("B")), hasProperty("percentGenus", closeTo(20f)))
				)
		);
	}

	@Test
	void testFindItg80PercentPure() throws Exception {
		var controlMap = FipTestUtils.loadControlMap();
		var app = new FipStart();
		app.setControlMap(controlMap);

		var spec1 = this.getTestSpecies("test polygon", Layer.PRIMARY, "F", spec -> {
			spec.setPercentGenus(80);
		});
		var spec2 = this.getTestSpecies("test polygon", Layer.PRIMARY, "C", spec -> {
			spec.setPercentGenus(20);
		});

		List<FipSpecies> primarySpecies = List.of(spec1, spec2);

		var result = app.findItg(primarySpecies);

		assertEquals(1, result);
	}

	@Test
	void testFindItgNoSecondary() throws Exception {
		var controlMap = FipTestUtils.loadControlMap();
		var app = new FipStart();
		app.setControlMap(controlMap);

		var spec1 = this.getTestSpecies("test polygon", Layer.PRIMARY, "F", spec -> {
			spec.setPercentGenus(100);
		});

		List<FipSpecies> primarySpecies = List.of(spec1);

		var result = app.findItg(primarySpecies);

		assertEquals(1, result);
	}

	List<FipSpecies> primarySecondarySpecies(String primary, String secondary) {
		var spec1 = this.getTestSpecies("test polygon", Layer.PRIMARY, primary, spec -> {
			spec.setPercentGenus(70);
		});
		var spec2 = this.getTestSpecies("test polygon", Layer.PRIMARY, secondary, spec -> {
			spec.setPercentGenus(20);
		});

		return List.of(spec1, spec2);
	}

	void assertItgMixed(FipStart app, int expected, String primary, String... secondary) throws ProcessingException {
		for (var sec : secondary) {
			var result = app.findItg(primarySecondarySpecies(primary, sec));
			assertThat(
					result, describedAs("ITG for " + primary + " and " + sec + " should be " + expected, is(expected))
			);
		}
	}

	void assertItgMixed(FipStart app, int expected, String primary, Collection<String> secondary)
			throws ProcessingException {
		for (var sec : secondary) {
			var result = app.findItg(primarySecondarySpecies(primary, sec));
			assertThat(
					result, describedAs("ITG for " + primary + " and " + sec + " should be " + expected, is(expected))
			);
		}
	}

	@Test
	void testFindItgMixed() throws Exception {
		var controlMap = FipTestUtils.loadControlMap();
		var app = new FipStart();
		app.setControlMap(controlMap);

		assertItgMixed(app, 2, "F", /*  */ "Y", "C");
		assertItgMixed(app, 3, "F", /*  */ "B", "H");
		assertItgMixed(app, 3, "F", /*  */ "H");
		assertItgMixed(app, 4, "F", /*  */ "S");
		assertItgMixed(app, 5, "F", /*  */ "PL", "PA");
		assertItgMixed(app, 6, "F", /*  */ "PY");
		assertItgMixed(app, 7, "F", /*  */ "L", "PW");
		assertItgMixed(app, 8, "F", /*  */ FipStart.HARDWOODS);

		assertItgMixed(app, 10, "C", /* */ "Y");
		assertItgMixed(app, 11, "C", /* */ "B", "H", "S");
		assertItgMixed(app, 10, "C", /* */ "PL", "PA", "PY", "L", "PW");
		assertItgMixed(app, 10, "C", /* */ FipStart.HARDWOODS);

		assertItgMixed(app, 10, "Y", /* */ "C");
		assertItgMixed(app, 11, "Y", /* */ "B", "H", "S");
		assertItgMixed(app, 10, "Y", /* */ "PL", "PA", "PY", "L", "PW");
		assertItgMixed(app, 10, "Y", /* */ FipStart.HARDWOODS);

		assertItgMixed(app, 19, "B", /* */ "C", "Y", "H");
		assertItgMixed(app, 20, "B", /* */ "S", "PL", "PA", "PY", "L", "PW");
		assertItgMixed(app, 20, "B", /* */ FipStart.HARDWOODS);

		assertItgMixed(app, 22, "S", /* */ "F", "L", "PA", "PW", "PY");
		assertItgMixed(app, 23, "S", /* */ "C", "Y", "H");
		assertItgMixed(app, 24, "S", /* */ "B");
		assertItgMixed(app, 25, "S", /* */ "PL");
		assertItgMixed(app, 26, "S", /* */ FipStart.HARDWOODS);

		assertItgMixed(app, 27, "PW", /**/ "B", "C", "F", "H", "L", "PA", "PL", "PY", "S", "Y");
		assertItgMixed(app, 27, "PW", /**/ FipStart.HARDWOODS);

		assertItgMixed(app, 28, "PL", /**/ "PA");
		assertItgMixed(app, 30, "PL", /**/ "B", "C", "H", "S", "Y");
		assertItgMixed(app, 29, "PL", /**/ "F", "PW", "L", "PY");
		assertItgMixed(app, 31, "PL", /**/ FipStart.HARDWOODS);

		assertItgMixed(app, 28, "PA", /**/ "PL");
		assertItgMixed(app, 30, "PA", /**/ "B", "C", "H", "S", "Y");
		assertItgMixed(app, 29, "PA", /**/ "F", "PW", "L", "PY");
		assertItgMixed(app, 31, "PA", /**/ FipStart.HARDWOODS);

		assertItgMixed(app, 32, "PY", /**/ "B", "C", "F", "H", "L", "PA", "PL", "PW", "S", "Y");
		assertItgMixed(app, 32, "PY", /**/ FipStart.HARDWOODS);

		assertItgMixed(app, 33, "L", /* */ "F");
		assertItgMixed(app, 34, "L", /* */ "B", "C", "H", "PA", "PL", "PW", "PY", "S", "Y");
		assertItgMixed(app, 34, "L", /* */ FipStart.HARDWOODS);

		assertItgMixed(app, 35, "AC", /**/ "B", "C", "F", "H", "L", "PA", "PL", "PW", "PY", "S", "Y");
		assertItgMixed(app, 36, "AC", /**/ "AT", "D", "E", "MB");

		assertItgMixed(app, 37, "D", /* */ "B", "C", "F", "H", "L", "PA", "PL", "PW", "PY", "S", "Y");
		assertItgMixed(app, 38, "D", /* */ "AC", "AT", "E", "MB");

		assertItgMixed(app, 39, "MB", /**/ "B", "C", "F", "H", "L", "PA", "PL", "PW", "PY", "S", "Y");
		assertItgMixed(app, 39, "MB", /**/ "AC", "AT", "D", "E");

		assertItgMixed(app, 40, "E", /* */ "B", "C", "F", "H", "L", "PA", "PL", "PW", "PY", "S", "Y");
		assertItgMixed(app, 40, "E", /* */ "AC", "AT", "D", "MB");

		assertItgMixed(app, 41, "AT", /**/ "B", "C", "F", "H", "L", "PA", "PL", "PW", "PY", "S", "Y");
		assertItgMixed(app, 42, "AT", /**/ "AC", "D", "E", "MB");

	}

	@Test
	void testFindEquationGroupDefault() throws Exception {
		var controlMap = FipTestUtils.loadControlMap();
		var app = new FipStart();
		app.setControlMap(controlMap);

		var becLookup = BecDefinitionParser.getBecs(controlMap);
		var bec = becLookup.get("ESSF").get();

		var spec1 = this.getTestSpecies("test polygon", Layer.PRIMARY, "F", valid());

		var result = app.findEquationGroup(spec1, bec, 3);

		assertThat(result, is(55));
	}

	@Test
	void testFindEquationGroupModified() throws Exception {
		var controlMap = FipTestUtils.loadControlMap();
		var app = new FipStart();
		app.setControlMap(controlMap);

		var becLookup = BecDefinitionParser.getBecs(controlMap);
		var bec = becLookup.get("PP").get();

		var spec1 = this.getTestSpecies("test polygon", Layer.PRIMARY, "F", valid());

		var result = app.findEquationGroup(spec1, bec, 2);

		assertThat(result, is(61)); // Modified from 57
	}

	void vetUtilization(String property, Consumer<Function<Float, Matcher<VdypUtilizationHolder>>> body) {
		Function<Float, Matcher<VdypUtilizationHolder>> generator = v -> hasProperty(
				property, coe(-1, contains(is(0f), closeTo(v), is(0f), is(0f), is(0f), closeTo(v)))
		);
		body.accept(generator);
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

	private String polygonId(String prefix, int year) {
		return String.format("%-23s%4d", prefix, year);
	}

	private static final void testWith(
			List<FipPolygon> polygons, List<Map<Layer, FipLayer>> layers, List<Collection<FipSpecies>> species,
			TestConsumer<FipStart> test
	) throws Exception {
		testWith(new HashMap<>(), polygons, layers, species, test);
	}

	private static final void testWith(
			Map<String, Object> myControlMap, List<FipPolygon> polygons, List<Map<Layer, FipLayer>> layers,
			List<Collection<FipSpecies>> species, TestConsumer<FipStart> test
	) throws Exception {

		var app = new FipStart();

		Map<String, Object> controlMap = new HashMap<>();

		Map<String, Float> minima = new HashMap<>();

		minima.put(FipControlParser.MINIMUM_HEIGHT, 5f);
		minima.put(FipControlParser.MINIMUM_BASE_AREA, 0f);
		minima.put(FipControlParser.MINIMUM_PREDICTED_BASE_AREA, 2f);
		minima.put(FipControlParser.MINIMUM_VETERAN_HEIGHT, 10f);

		controlMap.put(FipControlParser.MINIMA, minima);

		controlMap.putAll(myControlMap);

		var control = EasyMock.createControl();

		MockStreamingParser<FipPolygon> polygonStream = mockStream(
				control, controlMap, FipPolygonParser.CONTROL_KEY, "polygonStream"
		);
		MockStreamingParser<Map<Layer, FipLayer>> layerStream = mockStream(
				control, controlMap, FipLayerParser.CONTROL_KEY, "layerStream"
		);
		MockStreamingParser<Collection<FipSpecies>> speciesStream = mockStream(
				control, controlMap, FipSpeciesParser.CONTROL_KEY, "speciesStream"
		);

		mockWith(polygonStream, polygons);
		mockWith(layerStream, layers);
		mockWith(speciesStream, species);

		app.setControlMap(controlMap);

		control.replay();

		test.accept(app, controlMap);

		control.verify();

		expectAllClosed(polygonStream, layerStream, speciesStream);

	}

	/**
	 * Do nothing to mutate valid test data
	 */
	static final <T> Consumer<T> valid() {
		return x -> {
		};
	};

	static Map<Layer, FipLayer> layerMap(FipLayer... layers) {
		Map<Layer, FipLayer> result = new HashMap<>();
		for (var layer : layers) {
			result.put(layer.getLayer(), layer);
		}
		return result;
	}

	FipPolygon getTestPolygon(String polygonId, Consumer<FipPolygon> mutator) {
		var result = new FipPolygon(
				polygonId, // polygonIdentifier
				"0", // fiz
				"BG", // becIdentifier
				Optional.empty(), // percentAvailable
				Optional.of(FipMode.FIPSTART), // modeFip
				Optional.empty(), // nonproductiveDescription
				1.0f // yieldFactor
		);
		mutator.accept(result);
		return result;
	};

	FipLayerPrimary getTestPrimaryLayer(String polygonId, Consumer<FipLayerPrimary> mutator) {
		var result = new FipLayerPrimary(polygonId);
		result.setAgeTotal(8f);
		result.setHeight(6f);
		result.setSiteIndex(5f);
		result.setCrownClosure(0.9f);
		result.setSiteGenus("B");
		result.setSiteSpecies("B");
		result.setYearsToBreastHeight(7f);

		mutator.accept(result);
		return result;
	};

	FipLayer getTestVeteranLayer(String polygonId, Consumer<FipLayer> mutator) {
		var result = new FipLayer(
				polygonId, // polygonIdentifier
				Layer.VETERAN // layer
		);
		result.setAgeTotal(8f);
		result.setHeight(6f);
		result.setSiteIndex(5f);
		result.setCrownClosure(0.9f);
		result.setSiteGenus("B");
		result.setSiteSpecies("B");
		result.setYearsToBreastHeight(7f);

		mutator.accept(result);
		return result;
	};

	FipSpecies getTestSpecies(String polygonId, Layer layer, Consumer<FipSpecies> mutator) {
		return getTestSpecies(polygonId, layer, "B", mutator);
	};

	FipSpecies getTestSpecies(String polygonId, Layer layer, String genusId, Consumer<FipSpecies> mutator) {
		var result = new FipSpecies(
				polygonId, // polygonIdentifier
				layer, // layer
				genusId // genus
		);
		result.setPercentGenus(100.0f);
		result.setSpeciesPercent(Collections.emptyMap());
		mutator.accept(result);
		return result;
	};

	@FunctionalInterface
	private static interface TestConsumer<T> {
		public void accept(T unit, Map<String, Object> controlMap) throws Exception;
	}

	Matcher<Float> asFloat(Matcher<Double> doubleMatcher) {
		return new TypeSafeDiagnosingMatcher<Float>() {

			@Override
			public void describeTo(Description description) {
				doubleMatcher.describeTo(description);
			}

			@Override
			protected boolean matchesSafely(Float item, Description mismatchDescription) {
				if (!doubleMatcher.matches((double) item)) {
					doubleMatcher.describeMismatch(item, mismatchDescription);
					return false;
				}
				return true;
			}

		};
	}

	Matcher<Float> closeTo(float expected) {
		return asFloat(Matchers.closeTo(expected, EPSILON));
	}
}
