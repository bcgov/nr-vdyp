package ca.bc.gov.nrs.vdyp.fip;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.fip.model.FipLayer;
import ca.bc.gov.nrs.vdyp.fip.model.FipLayerPrimary;
import ca.bc.gov.nrs.vdyp.fip.model.FipMode;
import ca.bc.gov.nrs.vdyp.fip.model.FipPolygon;
import ca.bc.gov.nrs.vdyp.fip.model.FipSpecies;
import ca.bc.gov.nrs.vdyp.io.parse.MockStreamingParser;
import ca.bc.gov.nrs.vdyp.io.parse.ResourceParseException;
import ca.bc.gov.nrs.vdyp.io.parse.StreamingParserFactory;
import ca.bc.gov.nrs.vdyp.model.Layer;
import ca.bc.gov.nrs.vdyp.model.VdypSpecies;
import ca.bc.gov.nrs.vdyp.test.TestUtils;

class FipStartTest {

	static final float EPSILON = 0.000001f;

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
				Arrays.asList(getTestPolygon(polygonId, valid())), //
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
				Arrays.asList(getTestPolygon(polygonId, valid())), //
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
				Arrays.asList(getTestPolygon(polygonId, valid())), //
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
				Arrays.asList(getTestPolygon(polygonId, valid())), //
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
		TestUtils.populateControlMapReal(controlMap);
		TestUtils.populateControlMapGensuReal(controlMap);
		TestUtils.populateControlMapVeteranBq(controlMap);
		TestUtils.populateControlMapEquationGroups(controlMap, (s, b) -> new int[] { 0, 0, 0 });

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
		TestUtils.populateControlMapReal(controlMap);
		TestUtils.populateControlMapGensuReal(controlMap);
		TestUtils.populateControlMapVeteranBq(controlMap);
		TestUtils.populateControlMapEquationGroups(controlMap, (s, b) -> new int[] { 0, 0, 0 });

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
		TestUtils.populateControlMapReal(controlMap);
		TestUtils.populateControlMapGensuReal(controlMap);
		TestUtils.populateControlMapVeteranBq(controlMap);
		TestUtils.populateControlMapEquationGroups(controlMap, (s, b) -> new int[] { 0, 0, 0 });

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
		TestUtils.populateControlMapReal(controlMap);
		TestUtils.populateControlMapGensuReal(controlMap);
		TestUtils.populateControlMapVeteranBq(controlMap);
		TestUtils.populateControlMapEquationGroups(controlMap, (s, b) -> new int[] { 0, 0, 0 });

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
		TestUtils.populateControlMapReal(controlMap);
		TestUtils.populateControlMapGensuReal(controlMap);
		TestUtils.populateControlMapVeteranBq(controlMap);
		TestUtils.populateControlMapEquationGroups(controlMap, (s, b) -> new int[] { 0, 0, 0 });

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
		TestUtils.populateControlMapReal(controlMap);
		TestUtils.populateControlMapGensuReal(controlMap);
		TestUtils.populateControlMapEquationGroups(controlMap, (s, b) -> new int[] { 0, 0, 0 });
		TestUtils.populateControlMapVeteranBq(controlMap);

		var app = new FipStart();
		app.setControlMap(controlMap);

		var result = app.processLayerAsVeteran(fipPolygon, fipLayer);
		var expectedBaseArea = 0.70932f * (float) Math.pow(10f - 7.63269f, 0.62545f) * 0.9f / 4.0f;

		Matcher<Float> baMatcher = closeTo(expectedBaseArea);
		Matcher<Float> zeroMatcher = is(0.0f);
		// Expect the estimated BA in 0 and 4 (-1 to 4)
		assertThat(
				result,
				hasProperty(
						"baseAreaByUtilization",
						contains(zeroMatcher, baMatcher, zeroMatcher, zeroMatcher, zeroMatcher, baMatcher)
				)
		);

		assertThat(
				result,
				hasProperty(
						"species",
						hasEntry(
								is("B"),
								hasProperty(
										"baseAreaByUtilization",
										contains(
												zeroMatcher, zeroMatcher, zeroMatcher, zeroMatcher, zeroMatcher,
												closeTo(expectedBaseArea * 0.6f)
										)
								)
						)
				)
		);
		assertThat(
				result,
				hasProperty(
						"species",
						hasEntry(
								is("C"),
								hasProperty(
										"baseAreaByUtilization",
										contains(
												zeroMatcher, zeroMatcher, zeroMatcher, zeroMatcher, zeroMatcher,
												closeTo(expectedBaseArea * 0.4f)
										)
								)
						)
				)
		);
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
		TestUtils.populateControlMapReal(controlMap);
		TestUtils.populateControlMapGensuReal(controlMap);
		TestUtils.populateControlMapVeteranBq(controlMap);
		TestUtils.populateControlMapEquationGroups(controlMap, (s, b) -> new int[] { 0, 0, 0 });

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
		TestUtils.populateControlMapReal(controlMap);
		TestUtils.populateControlMapGensuReal(controlMap);
		TestUtils.populateControlMapVeteranBq(controlMap);
		TestUtils.populateControlMapEquationGroups(
				controlMap, (s, b) -> s.equals("B") && b.equals("BG") ? new int[] { 1, 2, 3 } : new int[] { 0, 0, 0 }
		);

		var app = new FipStart();
		app.setControlMap(controlMap);

		var result = app.processLayerAsVeteran(fipPolygon, fipLayer).getPrimarySpeciesRecord();

		assertThat(result, hasProperty("volumeGroup", is(1)));
		assertThat(result, hasProperty("decayGroup", is(2)));
		assertThat(result, hasProperty("breakageGroup", is(3)));

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
