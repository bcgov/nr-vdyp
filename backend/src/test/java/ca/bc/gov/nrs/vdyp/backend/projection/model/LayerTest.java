package ca.bc.gov.nrs.vdyp.backend.projection.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.AbstractProjectionRequestException;
import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.PolygonValidationException;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.backend.model.v1.ProjectionRequestKind;
import ca.bc.gov.nrs.vdyp.backend.projection.ProjectionContext;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.GrowthModelCode;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.ProjectionTypeCode;
import ca.bc.gov.nrs.vdyp.common_calculators.enumerations.SiteIndexEquation;
import ca.bc.gov.nrs.vdyp.si32.site.SiteTool;
import ca.bc.gov.nrs.vdyp.si32.vdyp.VdypMethods;

public class LayerTest {
	ProjectionContext context;
	Parameters params;
	History history;
	Polygon polygon;
	Layer layer;
	LayerAdjustments layerAdjustments;

	@BeforeEach
	void setup() throws AbstractProjectionRequestException {
		params = new Parameters().ageStart(0).ageEnd(100);
		context = new ProjectionContext(ProjectionRequestKind.HCSV, "Test", params, false);
		polygon = new Polygon.Builder().build();
	}

	Layer buildLayer(Map<String, Object> params) {
		var layerBuilder = new Layer.Builder() //
				.polygon(polygon).layerId("1");
		for (Map.Entry<String, Object> p : params.entrySet()) {
			switch (p.getKey()) {
			case "adjustments" -> layerBuilder.adjustments((LayerAdjustments) p.getValue());
			case "ageAtDeath" -> layerBuilder.ageAtDeath((Double) p.getValue());
			case "assignedProjectionType" -> layerBuilder.assignedProjectionType((ProjectionTypeCode) p.getValue());
			case "basalArea" -> layerBuilder.basalArea((Double) p.getValue());
			case "crownClosure" -> layerBuilder.crownClosure((Short) p.getValue());
			case "doIncludeWithProjection" -> layerBuilder.doIncludeWithProjection((Boolean) p.getValue());
			case "doSuppressPerHAYields" -> layerBuilder.doSuppressPerHAYields((Boolean) p.getValue());
			case "estimatedSiteIndex" -> layerBuilder.estimatedSiteIndex((Double) p.getValue());
			case "estimatedSiteIndexSpecies" -> layerBuilder.estimatedSiteIndexSpecies((String) p.getValue());
			case "history" -> layerBuilder.history((History) p.getValue());
			case "isDeadLayer" -> layerBuilder.isDeadLayer((Boolean) p.getValue());
			case "measuredUtilizationLevel" -> layerBuilder.measuredUtilizationLevel((Double) p.getValue());
			case "nonForestDescriptor" -> layerBuilder.nonForestDescriptor((String) p.getValue());
			case "rankCode" -> layerBuilder.rankCode((String) p.getValue());
			case "treesPerHectare" -> layerBuilder.treesPerHectare((Double) p.getValue());
			case "vdyp7LayerCode" -> layerBuilder.vdyp7LayerCode((ProjectionTypeCode) p.getValue());
			case "yearOfDeath" -> layerBuilder.yearOfDeath((Integer) p.getValue());
			}
		}
		layer = layerBuilder.build();
		return layer;

	}

	Stand addStand(Layer layer, String sp64Code) {
		var stand = new Stand.Builder().layer(layer).sp0Code(VdypMethods.getVDYP7Species(sp64Code)).build();
		var sp0 = new Species.Builder().stand(stand).speciesCode(VdypMethods.getVDYP7Species(sp64Code))
				.speciesPercent(0).build();
		stand.addSpeciesGroup(sp0, layer.getSp0sAsSupplied().size());

		layer.addStand(stand);
		return stand;
	}

	Species addSpecies(Layer layer, Stand stand, Map<String, Object> speciesData) {
		var builder = new Species.Builder().stand(stand);

		for (var p : speciesData.entrySet()) {
			switch (p.getKey()) {
			case "sp64" -> builder = builder.speciesCode((String) p.getValue());
			case "perc" -> builder = builder.speciesPercent((Double) p.getValue());
			case "aabh" -> builder = builder.ageAtBreastHeight((Double) p.getValue());
			case "age" -> builder = builder.totalAge((Double) p.getValue());
			case "h" -> builder = builder.dominantHeight((Double) p.getValue());
			case "sc" -> builder = builder.siteCurve((SiteIndexEquation) p.getValue());
			case "si" -> builder = builder.siteIndex((Double) p.getValue());
			case "y2bh" -> builder = builder.yearsToBreastHeight((Double) p.getValue());
			}
		}
		var species = builder.build();

		stand.addSp64(species);
		layer.addSp64(species);
		return species;
	}

	@Test
	void TestLayerBuilderNoLayerId() {
		assertThrows(IllegalStateException.class, () -> new Layer.Builder().build());
	}

	@Test
	void TestLayerBuilder() {
		layerAdjustments = new LayerAdjustments();
		history = new History.Builder().build();
		var layer = new Layer.Builder() //
				.adjustments(layerAdjustments) //
				.ageAtDeath(20.0) //
				.assignedProjectionType(ProjectionTypeCode.PRIMARY) //
				.basalArea(20.0) //
				.crownClosure((short) 20) //
				.doIncludeWithProjection(true) //
				.doSuppressPerHAYields(false) //
				.estimatedSiteIndex(20.0) //
				.estimatedSiteIndexSpecies("PL") //
				.history(history) //
				.isDeadLayer(false) //
				.layerId("1") //
				.measuredUtilizationLevel(20.0) //
				.nonForestDescriptor("NFD") //
				.polygon(polygon) //
				.percentStockable(20.0) //
				.rankCode("1") //
				.treesPerHectare(20.0) //
				.vdyp7LayerCode(ProjectionTypeCode.PRIMARY) //
				.yearOfDeath(2000).build();

		assertThat(
				layer, allOf(
						hasProperty("adjustments", is(layerAdjustments)), //
						hasProperty("ageAtDeath", is(20.0)), //
						hasProperty("assignedProjectionType", is(ProjectionTypeCode.PRIMARY)), //
						hasProperty("basalArea", is(20.0)), //
						hasProperty("crownClosure", is((short) 20)), //
						hasProperty("doIncludeWithProjection", is(true)), //
						hasProperty("doSuppressPerHAYields", is(false)), //
						hasProperty("estimatedSiteIndex", is(20.0)), //
						hasProperty("history", is(history)), //
						hasProperty("isDeadLayer", is(false)), //
						hasProperty("layerId", is("1")), //
						hasProperty("measuredUtilizationLevel", is(20.0)), //
						hasProperty("nonForestDescriptor", is("NFD")), //
						hasProperty("polygon", is(polygon)), //
						hasProperty("percentStockable", is(20.0)), //
						hasProperty("rankCode", is("1")), //
						hasProperty("treesPerHectare", is(20.0)), //
						hasProperty("vdyp7LayerCode", is(ProjectionTypeCode.PRIMARY)), //
						hasProperty("yearOfDeath", is(2000)) //
				)
		);
	}

	@Test
	void testDuplicateStand() {
		layer = buildLayer(Map.of());
		addStand(layer, "PL");
		assertThrows(IllegalStateException.class, () -> addStand(layer, "PL"));
	}

	@Test
	void testDuplicateSpecies() {
		layer = buildLayer(Map.of());
		Map<String, Object> params = Map.of("sp64", "PL", "perc", 100.0);

		Stand stand = addStand(layer, "PL");
		addSpecies(layer, stand, params);
		assertThrows(IllegalStateException.class, () -> addSpecies(layer, stand, params));
	}

	@Nested
	class EstimatedSiteIndex {

		@Test
		void testLayerSuppliedEstimatedSiteIndexAndSpecies() throws PolygonValidationException {
			layer = buildLayer(Map.of("estimatedSiteIndex", 25.0, "estimatedSiteIndexSpecies", "FD"));
			Map<String, Object> params = Map.of("sp64", "PL", "perc", 100.0, "age", 29.0, "si", 24.0);

			Stand stand = addStand(layer, "PL");
			Species sp64 = addSpecies(layer, stand, params);

			layer.doBuildSiteSpecies();
			layer.doCompleteSiteSpeciesSiteIndexInfo();
			layer.calculateEstimatedSiteIndex(context, GrowthModelCode.VRI, false);

			assertNotNull(sp64.getSiteIndex());
			assertThat(sp64.getSiteIndex(), not(stand.getSpeciesGroup().getSiteIndex())); // TODO revisit why these
																							// should be different
		}

		static Stream<Arguments> siteIndexParameters() {
			return Stream.of(
					Arguments.of(
							// Species Supplied EstimatedSiteIndex
							Map.of("growthModelCode", GrowthModelCode.FIP),
							List.of(Map.of("sp64", "PL", "perc", 100.0, "age", 29.0, "si", 25.0))
					),
					Arguments.of(
							// Species Supplied Age and Height Estimate
							Map.of("growthModelCode", GrowthModelCode.VRI),
							List.of(Map.of("sp64", "PL", "perc", 100.0, "age", 29.0, "h", 10.0))
					),
					Arguments.of(
							// Species Supplied Age and Height Estimate Older Leading sp64
							Map.of("growthModelCode", GrowthModelCode.VRI),
							List.of(Map.of("sp64", "PL", "perc", 100.0, "age", 100.0, "h", 50.0))
					),
					Arguments.of(
							// Species Supplied Age and Height Secondary Species
							Map.of("growthModelCode", GrowthModelCode.VRI),
							List.of(
									Map.of("sp64", "FD", "perc", 90.0, "age", 100.0, "h", 50.0),
									Map.of("sp64", "PY", "perc", 10.0)
							)
					)
			);
		}

		@ParameterizedTest
		@MethodSource("siteIndexParameters")
		void testSpeciesSuppliedEstimatedSiteIndex(
				Map<String, Object> layerParams, List<Map<String, Object>> speciesParamsList
		) throws PolygonValidationException {
			layer = buildLayer(layerParams);

			Map<String, Object> firstSpeciesParam = speciesParamsList.get(0);
			Stand stand = addStand(layer, (String) firstSpeciesParam.get("sp64"));
			Species sp64 = addSpecies(layer, stand, firstSpeciesParam);

			if (speciesParamsList.size() > 1) {
				for (int i = 1; i < speciesParamsList.size(); i++) {
					addSpecies(layer, stand, speciesParamsList.get(i));
				}
			}

			layer.doBuildSiteSpecies();
			layer.doCompleteSiteSpeciesSiteIndexInfo();
			layer.calculateEstimatedSiteIndex(context, (GrowthModelCode) layerParams.get("growthModelCode"), false);

			assertNotNull(sp64.getSiteIndex());
			assertThat(sp64.getSiteIndex(), is(stand.getSpeciesGroup().getSiteIndex()));
		}
	}

	@Nested
	class EstimateCrownClosure {
		@Test
		void testSuppliedCrownClosure() throws PolygonValidationException {
			layer = buildLayer(Map.of("crownClosure", (short) 5, "doSuppressPerHAYields", true));
			Map<String, Object> params = Map.of("sp64", "PL", "perc", 100.0, "age", 100.0, "h", 50.0);

			Stand stand = addStand(layer, "PL");
			addSpecies(layer, stand, params);

			layer.doCompleteDefinition();
			layer.doBuildSiteSpecies();
			layer.doCompleteSiteSpeciesSiteIndexInfo();

			layer.estimateCrownClosure(context);

			// make sure crown closure was not taken from layer
			assertNotNull(layer.getCrownClosure());
			assertThat(layer.getCrownClosure(), is((short) 5));
		}

		@Test
		void testNotSuppliedNoLeadingSpecies() throws PolygonValidationException {
			layer = buildLayer(
					Map.of("assignedProjectionType", ProjectionTypeCode.PRIMARY, "doSuppressPerHAYields", true)
			);

			addStand(layer, "PL");

			layer.doCompleteDefinition();
			layer.doBuildSiteSpecies();
			layer.doCompleteSiteSpeciesSiteIndexInfo();
			layer.estimateCrownClosure(context);

			// Projections not allowed no leading species
			assertThat(polygon.doAllowProjectionOfType(layer.getAssignedProjectionType()), is(false));

		}

		@Test
		void testNotSuppliedLeadingSpeciesTooShort() throws PolygonValidationException {
			layer = buildLayer(Map.of("doSuppressPerHAYields", true));
			Map<String, Object> params = Map.of("sp64", "PL", "perc", 100.0, "age", 30.0, "h", 9.0);

			Stand stand = addStand(layer, "PL");
			addSpecies(layer, stand, params);

			layer.doCompleteDefinition();
			layer.doBuildSiteSpecies();
			layer.doCompleteSiteSpeciesSiteIndexInfo();
			layer.estimateCrownClosure(context);

			// Could not calculate crown closure
			assertNull(layer.getCrownClosure());
		}

		@Test
		void testNotSuppliedCalculateFromLeadingSpecies() throws PolygonValidationException {
			layer = buildLayer(Map.of("doSuppressPerHAYields", true));
			Map<String, Object> params = Map.of("sp64", "PL", "perc", 100.0, "age", 100.0, "h", 50.0);

			Stand stand = addStand(layer, "PL");
			addSpecies(layer, stand, params);

			layer.doCompleteDefinition();
			layer.doBuildSiteSpecies();
			layer.doCompleteSiteSpeciesSiteIndexInfo();
			layer.estimateCrownClosure(context);

			// Make sure estimate was pulled from the default for the species
			assertNotNull(layer.getCrownClosure());
			assertThat(
					layer.getCrownClosure(),
					is((short) SiteTool.getSpeciesDefaultCrownClosure("PL", polygon.getIsCoastal()))
			);
		}
	}

	@Test
	void testDoesHeightExceedErrorHandling() {
		layer = new Layer.Builder().layerId("TEST").polygon(polygon).doSuppressPerHAYields(false)
				.assignedProjectionType(ProjectionTypeCode.VETERAN).build();
		Map<String, Object> params = Map.of("sp64", "PL", "perc", 100.0, "age", 100.0, "h", 50.0);

		Stand stand = addStand(layer, "PL");
		addSpecies(layer, stand, params);

		assertThat(layer.doesHeightExceed(100.0), is(false));
		assertThat(layer.doesHeightExceed(50.0), is(true));
	}

	@Test
	void testFindNthSpeciesByCriteria() {
		layer = new Layer.Builder().layerId("TEST").polygon(polygon).build();
		Map<String, Object> params = Map.of("sp64", "PY", "perc", 20.0);
		Map<String, Object> params2 = Map.of("sp64", "FD", "perc", 30.0, "age", 200.0, "h", 27.0);
		Map<String, Object> params3 = Map.of("sp64", "LW", "perc", 50.0, "age", 100.0, "h", 15.0);

		Stand stand = addStand(layer, "FD");
		Species sp64 = addSpecies(layer, stand, params);
		Species sp642 = addSpecies(layer, stand, params2);
		Species sp643 = addSpecies(layer, stand, params3);

		assertThrows(
				IllegalArgumentException.class,
				() -> layer.findNthSpeciesByCriteria(-1, SpeciesSelectionCriteria.BY_NAME)
		);
		assertThrows(
				IllegalArgumentException.class,
				() -> layer.findNthSpeciesByCriteria(3, SpeciesSelectionCriteria.BY_NAME)
		);
		Species found = layer.findNthSpeciesByCriteria(0, SpeciesSelectionCriteria.AS_SUPPLIED);
		assertThat(found, is(sp64));
		found = layer.findNthSpeciesByCriteria(0, SpeciesSelectionCriteria.BY_NAME);
		assertThat(found, is(sp642));
		found = layer.findNthSpeciesByCriteria(0, SpeciesSelectionCriteria.BY_PERCENT);
		assertThat(found, is(sp643));
	}

	@Test
	void testGetSP64() {
		layer = new Layer.Builder().layerId("TEST").polygon(polygon).build();
		Map<String, Object> params = Map.of("sp64", "PL", "perc", 100.0, "age", 100.0, "h", 50.0);

		Stand stand = addStand(layer, "PL");
		Species sp64 = addSpecies(layer, stand, params);

		assertThrows(IllegalArgumentException.class, () -> layer.getSp64("CW"));
		Species found = layer.getSp64("PL");
		assertThat(found, is(sp64));
	}

	@Test
	void testDetermineLayerAgeAtYear() throws PolygonValidationException {
		// polygon reference year defaults to 0 instead of null
		layer = new Layer.Builder().layerId("TEST").polygon(polygon).doSuppressPerHAYields(false).build();
		Map<String, Object> params = Map.of("sp64", "PL", "perc", 100.0, "age", 100.0, "h", 50.0);

		Stand stand = addStand(layer, "PL");
		addSpecies(layer, stand, params);

		assertThrows(IllegalArgumentException.class, () -> layer.determineLayerAgeAtYear(null));
		assertThrows(IllegalArgumentException.class, () -> layer.determineLayerAgeAtYear(1399));
		assertThrows(IllegalArgumentException.class, () -> layer.determineLayerAgeAtYear(2501));

		Double age = layer.determineLayerAgeAtYear(2499);
		assertNull(age); // need to complete definition before you get values

		layer.doCompleteDefinition();
		layer.doBuildSiteSpecies();
		layer.doCompleteSiteSpeciesSiteIndexInfo();

		age = layer.determineLayerAgeAtYear(2499);
		assertThat(age, is(2599.0));// Should reference year oin polygon default to null instead of 0?

		polygon = new Polygon.Builder().referenceYear(2020).build();

		layer = new Layer.Builder().layerId("TEST").polygon(polygon).doSuppressPerHAYields(false).build();
		stand = addStand(layer, "PL");
		addSpecies(layer, stand, params);

		layer.doCompleteDefinition();
		layer.doBuildSiteSpecies();
		layer.doCompleteSiteSpeciesSiteIndexInfo();
		age = layer.determineLayerAgeAtYear(2499);
		assertThat(age, is(579.0));

		age = layer.determineLayerAgeAtYear(1919);
		assertThat(age, is(0.0));
	}

	@Test
	void testDoCompleteSiteSpeciesInfo() throws PolygonValidationException {
		layer = new Layer.Builder().layerId("TEST").crownClosure((short) 3).treesPerHectare(12.0).basalArea(0.8656)
				.polygon(polygon).build();
		Map<String, Object> params = Map.of("sp64", "FD", "perc", 50.0, "age", 200.0, "h", 27.0);
		Map<String, Object> params2 = Map.of("sp64", "LW", "perc", 30.0, "age", 100.0, "h", 15.0);
		Map<String, Object> params3 = Map.of("sp64", "PY", "perc", 20.0);

		Stand stand = addStand(layer, "FD");
		Species sp64 = addSpecies(layer, stand, params);
		addSpecies(layer, stand, params2);
		addSpecies(layer, stand, params3);

		layer.doBuildSiteSpecies();
		layer.doCompleteSiteSpeciesSiteIndexInfo();

		assertThat(sp64.getTotalAge(), is(100.0));
	}
}
