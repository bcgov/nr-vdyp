package ca.bc.gov.nrs.vdyp.backend.projection.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.AbstractProjectionRequestException;
import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.PolygonValidationException;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.backend.model.v1.ProjectionRequestKind;
import ca.bc.gov.nrs.vdyp.backend.projection.ProjectionContext;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.GrowthModelCode;
import ca.bc.gov.nrs.vdyp.common_calculators.enumerations.SiteIndexEquation;
import ca.bc.gov.nrs.vdyp.si32.site.SiteTool;
import ca.bc.gov.nrs.vdyp.si32.vdyp.VdypMethods;
import io.quarkus.test.InjectMock;
import org.gradle.api.specs.Spec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.ProjectionTypeCode;

import java.util.Map;

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
        context = new ProjectionContext(ProjectionRequestKind.HCSV,"Test", params, false);
        layerAdjustments = new LayerAdjustments();
        history = new History.Builder().build();
        polygon = new Polygon.Builder().build();
    }

    Stand addStand(String sp64Code)
    {
        var stand =  new Stand.Builder()
                .layer(layer)
                .sp0Code(VdypMethods.getVDYP7Species(sp64Code))
                .build();
        var sp0 = new Species.Builder().stand(stand).speciesCode(VdypMethods.getVDYP7Species(sp64Code)).speciesPercent(0).build();
        stand.addSpeciesGroup(sp0, layer.getSp0sAsSupplied().size());

        layer.addStand(stand);
        return stand;
    }
    Species addSpecies(Stand stand, Map<String, Object> speciesData)
    {
        var builder = new Species.Builder().stand(stand);

        for (var p : speciesData.entrySet())
        {
            switch (p.getKey())
            {
                case "sp64" -> builder = builder.speciesCode((String) p.getValue());
                case "perc" -> builder = builder.speciesPercent((Double) p.getValue());
                case "aabh" -> builder = builder.ageAtBreastHeight((Double) p.getValue());
                case "age" -> builder = builder.totalAge((Double) p.getValue());
                case "h" -> builder = builder.dominantHeight((Double) p.getValue());
                case "sc" -> builder = builder.siteCurve((SiteIndexEquation) p.getValue());
                case "si" -> builder = builder.siteIndex((Double) p.getValue());
                case "y2bh" -> builder = builder.yearsToBreastHeight((Double)p.getValue());
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
    void testDuplicateStand()
    {
        layer = new Layer.Builder()
                .layerId("TEST")
                .polygon(polygon)
                .build();
        Stand stand = addStand( "PL");
        assertThrows(IllegalStateException.class, () -> addStand( "PL"));
    }

    @Test
    void testDuplicateSpecies()
    {
        layer = new Layer.Builder()
                .layerId("TEST")
                .polygon(polygon)
                .build();
        Map<String, Object> params = Map.of("sp64","PL", "perc", 100.0);

        Stand stand = addStand("PL");
        addSpecies(stand, params);
        assertThrows(IllegalStateException.class, () -> addSpecies(stand, params));
    }

    @Nested
    class EstimatedSiteIndex {
        @Test
        void testSpeciesSuppliedEstimatedSiteIndex() throws PolygonValidationException {
            layer = new Layer.Builder()
                    .layerId("TEST")
                    .polygon(polygon)
                    .build();
            Map<String, Object> params = Map.of("sp64", "PL", "perc", 100.0, "age", 29.0, "si", 25.0);

            Stand stand = addStand( "PL");
            Species sp64 = addSpecies(stand, params);

            layer.calculateEstimatedSiteIndex(context, GrowthModelCode.FIP, false);

            assertNotNull(sp64.getSiteIndex());
            assertThat(sp64.getSiteIndex(), is(stand.getSpeciesGroup().getSiteIndex()));
        }

        @Test
        void testLayerSuppliedEstimatedSiteIndexAndSpecies() throws PolygonValidationException {
            layer = new Layer.Builder()
                    .layerId("TEST")
                    .polygon(polygon)
                    .estimatedSiteIndex(25.0)
                    .estimatedSiteIndexSpecies("FD")
                    .build();
            Map<String, Object> params = Map.of("sp64", "PL", "perc", 100.0, "age", 29.0, "si", 24.0);

            Stand stand = addStand( "PL");
            Species sp64 = addSpecies(stand, params);

            layer.doBuildSiteSpecies();
            layer.doCompleteSiteSpeciesSiteIndexInfo();
            layer.calculateEstimatedSiteIndex(context, GrowthModelCode.VRI, false);

            assertNotNull(sp64.getSiteIndex());
            assertThat(sp64.getSiteIndex(), not(stand.getSpeciesGroup().getSiteIndex())); //TODO revisit why these should be different
        }

        @Test
        void testEstimateSpeciesSiteIndexFromAgeAndHeight() throws PolygonValidationException {
            layer = new Layer.Builder()
                    .layerId("TEST")
                    .polygon(polygon)
                    .build();
            Map<String, Object> params = Map.of("sp64", "PL", "perc", 100.0, "age", 29.0, "h", 10.0);

            Stand stand = addStand( "PL");
            Species sp64 = addSpecies(stand, params);

            layer.doBuildSiteSpecies();
            layer.doCompleteSiteSpeciesSiteIndexInfo();
            layer.calculateEstimatedSiteIndex(context, GrowthModelCode.VRI, false);
            // make sure that all values have been copied back to the species group
            assertNotNull(sp64.getSiteIndex());
            assertThat(sp64.getSiteIndex(), is(stand.getSpeciesGroup().getSiteIndex()));
        }

        @Test
        void testSpeciesEstimatedSiteIndexFromOlderLeadingSP64() throws PolygonValidationException {
            layer = new Layer.Builder()
                    .layerId("TEST")
                    .polygon(polygon)
                    .build();
            Map<String, Object> params = Map.of("sp64", "PL", "perc", 100.0, "age", 100.0, "h", 50.0);

            Stand stand = addStand("PL");
            Species sp64 = addSpecies(stand, params);

            layer.doBuildSiteSpecies();
            layer.doCompleteSiteSpeciesSiteIndexInfo();
            layer.calculateEstimatedSiteIndex(context, GrowthModelCode.VRI, false);
            // make sure that all values have been copied back to the species group
            assertNotNull(sp64.getSiteIndex());
            assertThat(sp64.getSiteIndex(), is(stand.getSpeciesGroup().getSiteIndex()));
        }


        @Test
        void testSecondSP64Species() throws PolygonValidationException {
            layer = new Layer.Builder()
                    .layerId("TEST")
                    .polygon(polygon)
                    .build();
            Map<String, Object> params = Map.of("sp64", "FD", "perc", 90.0, "age", 100.0, "h", 50.0);
            Map<String, Object> params2 = Map.of("sp64", "PY", "perc", 10.0);

            Stand stand = addStand("FD");
            Species sp64 = addSpecies(stand, params);

            //Stand stand2 = addStand("PL");
            Species sp642 = addSpecies(stand, params2);

            layer.doBuildSiteSpecies();
            layer.doCompleteSiteSpeciesSiteIndexInfo();
            layer.calculateEstimatedSiteIndex(context, GrowthModelCode.VRI, false);
            // make sure that all values have been copied back to the species group
            assertNotNull(sp64.getSiteIndex());
            assertThat(sp64.getSiteIndex(), is(stand.getSpeciesGroup().getSiteIndex()));
        }
/*
    @Test
    void testgroup44condition() throws PolygonValidationException {

    }
    */
    }


    @Nested
    class EstimateCrownClosure
    {
        @Test
        void testSuppliedCrownClosure() throws PolygonValidationException
        {
            layer = new Layer.Builder()
                    .layerId("TEST")
                    .polygon(polygon)
                    .crownClosure((short)5)
                    .doSuppressPerHAYields(true)
                    .build();
            Map<String, Object> params = Map.of("sp64", "PL", "perc", 100.0, "age", 100.0, "h", 50.0);

            Stand stand = addStand("PL");
            Species sp64 = addSpecies(stand, params);

            layer.doCompleteDefinition();
            layer.doBuildSiteSpecies();
            layer.doCompleteSiteSpeciesSiteIndexInfo();

            layer.estimateCrownClosure(context);

            // make sure that all values have been copied back to the species group
            assertNotNull(layer.getCrownClosure());
            assertThat(layer.getCrownClosure(), is((short)5));
        }
        @Test
        void testNotSuppliedNoLeadingSpecies() throws PolygonValidationException
        {
            layer = new Layer.Builder()
                    .layerId("TEST")
                    .polygon(polygon)
                    .assignedProjectionType(ProjectionTypeCode.PRIMARY)
                    .doSuppressPerHAYields(true)
                    .build();

            addStand("PL");

            layer.doCompleteDefinition();
            layer.doBuildSiteSpecies();
            layer.doCompleteSiteSpeciesSiteIndexInfo();
            layer.estimateCrownClosure(context);

            // make sure that all values have been copied back to the species group
            assertThat(polygon.doAllowProjectionOfType(layer.getAssignedProjectionType()), is(false));

        }
        @Test
        void testNotSuppliedLeadingSpeciesTooShort() throws PolygonValidationException
        {

            layer = new Layer.Builder()
                    .layerId("TEST")
                    .polygon(polygon)
                    .doSuppressPerHAYields(true)
                    .build();
            Map<String, Object> params = Map.of("sp64", "PL", "perc", 100.0, "age", 30.0, "h", 9.0);

            Stand stand = addStand("PL");
            Species sp64 = addSpecies(stand, params);

            layer.doCompleteDefinition();
            layer.doBuildSiteSpecies();
            layer.doCompleteSiteSpeciesSiteIndexInfo();
            layer.estimateCrownClosure(context);

            // make sure that all values have been copied back to the species group
            assertNull(layer.getCrownClosure());
        }

        @Test
        void testNotSuppliedCalculateFromLeadingSpecies() throws PolygonValidationException
        {
            layer = new Layer.Builder()
                    .layerId("TEST")
                    .polygon(polygon)
                    .doSuppressPerHAYields(true)
                    .build();
            Map<String, Object> params = Map.of("sp64", "PL", "perc", 100.0, "age", 100.0, "h", 50.0);

            Stand stand = addStand("PL");
            Species sp64 = addSpecies(stand, params);

            layer.doCompleteDefinition();
            layer.doBuildSiteSpecies();
            layer.doCompleteSiteSpeciesSiteIndexInfo();
            layer.estimateCrownClosure(context);

            // make sure that all values have been copied back to the species group
            assertNotNull(layer.getCrownClosure());
            assertThat(layer.getCrownClosure(), is((short)SiteTool.getSpeciesDefaultCrownClosure("PL", polygon.getIsCoastal())));
        }
        @Test
        void testNotSuppliedCalculateVeteranSpecies() throws PolygonValidationException
        {
            layer = new Layer.Builder()
                    .layerId("TEST")
                    .polygon(polygon)
                    .doSuppressPerHAYields(false)
                    .assignedProjectionType(ProjectionTypeCode.VETERAN)
                    .build();
            Map<String, Object> params = Map.of("sp64", "PL", "perc", 100.0, "age", 100.0, "h", 50.0);

            Stand stand = addStand("PL");
            Species sp64 = addSpecies(stand, params);

            layer.setAssignedProjectionType(ProjectionTypeCode.UNKNOWN);
            layer.doCompleteDefinition();
            layer.doBuildSiteSpecies();
            layer.doCompleteSiteSpeciesSiteIndexInfo();
            layer.estimateCrownClosure(context);

            // make sure that all values have been copied back to the species group
            assertNotNull(layer.getCrownClosure());
            assertThat(layer.getCrownClosure(), is((short)4));
        }

    }


    @Test
    void testDoesHeightExceedErrorHandling()
    {
        layer = new Layer.Builder()
                .layerId("TEST")
                .polygon(polygon)
                .doSuppressPerHAYields(false)
                .assignedProjectionType(ProjectionTypeCode.VETERAN)
                .build();
        Map<String, Object> params = Map.of("sp64", "PL", "perc", 100.0, "age", 100.0, "h", 50.0);

        Stand stand = addStand("PL");
        Species sp64 = addSpecies(stand, params);

        assertThat(layer.doesHeightExceed(100.0), is(false));
        assertThat(layer.doesHeightExceed(50.0), is(true));
    }


    @Test
    void testFindNthSpeciesByCriteria()
    {
        layer = new Layer.Builder()
                .layerId("TEST")
                .polygon(polygon)
                .build();
        Map<String, Object> params = Map.of("sp64", "PY", "perc", 20.0);
        Map<String, Object> params2 = Map.of("sp64", "FD", "perc", 30.0, "age", 200.0, "h", 27.0);
        Map<String, Object> params3 = Map.of("sp64", "LW", "perc", 50.0, "age", 100.0, "h", 15.0);

        Stand stand = addStand("FD");
        Species sp64 = addSpecies(stand, params);
        Species sp642 = addSpecies(stand, params2);
        Species sp643 = addSpecies(stand, params3);

        assertThrows(IllegalArgumentException.class,()-> layer.findNthSpeciesByCriteria(-1, SpeciesSelectionCriteria.BY_NAME));
        assertThrows(IllegalArgumentException.class,()-> layer.findNthSpeciesByCriteria(3, SpeciesSelectionCriteria.BY_NAME));
        Species found = layer.findNthSpeciesByCriteria(0, SpeciesSelectionCriteria.AS_SUPPLIED);
        assertThat(found, is(sp64));
        found = layer.findNthSpeciesByCriteria(0, SpeciesSelectionCriteria.BY_NAME);
        assertThat(found, is(sp642));
        found = layer.findNthSpeciesByCriteria(0, SpeciesSelectionCriteria.BY_PERCENT);
        assertThat(found, is(sp643));
    }

    @Test
    void testGetSP64()
    {
        layer = new Layer.Builder()
                .layerId("TEST")
                .polygon(polygon)
                .build();
        Map<String, Object> params = Map.of("sp64", "PL", "perc", 100.0, "age", 100.0, "h", 50.0);

        Stand stand = addStand("PL");
        Species sp64 = addSpecies(stand, params);

        assertThrows(IllegalArgumentException.class,()-> layer.getSp64("CW"));
        Species found = layer.getSp64("PL");
        assertThat(found, is(sp64));
    }

    @Test
    void testDetermineLayerAgeAtYear() throws PolygonValidationException
    {
        //polygon = new Polygon.Builder().referenceYear(2020).build();

        layer = new Layer.Builder()
                .layerId("TEST")
                .polygon(polygon)
                .doSuppressPerHAYields(false)
                .build();
        Map<String, Object> params = Map.of("sp64", "PL", "perc", 100.0, "age", 100.0, "h", 50.0);

        Stand stand = addStand("PL");
        Species sp64 = addSpecies(stand, params);

        assertThrows(IllegalArgumentException.class,()-> layer.determineLayerAgeAtYear(null));
        assertThrows(IllegalArgumentException.class,()-> layer.determineLayerAgeAtYear(1399));
        assertThrows(IllegalArgumentException.class,()-> layer.determineLayerAgeAtYear(2501));

        Double age = layer.determineLayerAgeAtYear(2499);
        assertNull(age);// NO SP0

        layer.doCompleteDefinition();
        layer.doBuildSiteSpecies();
        layer.doCompleteSiteSpeciesSiteIndexInfo();

        age = layer.determineLayerAgeAtYear(2499);
        assertNull(age); // no reference Year for the polygon

        polygon = new Polygon.Builder().referenceYear(2020).build();

        layer = new Layer.Builder()
                .layerId("TEST")
                .polygon(polygon)
                .doSuppressPerHAYields(false)
                .build();
        stand = addStand("PL");
        sp64 = addSpecies(stand, params);

        layer.doCompleteDefinition();
        layer.doBuildSiteSpecies();
        layer.doCompleteSiteSpeciesSiteIndexInfo();
        age = layer.determineLayerAgeAtYear(2499);
        assertThat(age, is(579.0));

        age = layer.determineLayerAgeAtYear(1919);
        assertThat(age, is(0.0));
    }

    @Test
    void testDoConpleteSiteSpeciesInfo() throws PolygonValidationException
    {
        layer = new Layer.Builder()
                .layerId("TEST")
                .crownClosure((short)3)
                .treesPerHectare(12.0)
                .basalArea(0.8656)
                .polygon(polygon)
                .build();
        Map<String, Object> params = Map.of("sp64", "FD", "perc", 50.0, "age", 200.0, "h", 27.0);
        Map<String, Object> params2 = Map.of("sp64", "LW", "perc", 30.0, "age", 100.0, "h", 15.0);
        Map<String, Object> params3 = Map.of("sp64", "PY", "perc", 20.0);

        Stand stand = addStand("FD");
        Species sp64 = addSpecies(stand, params);
        Species sp642 = addSpecies(stand, params2);
        Species sp643 = addSpecies(stand, params3);

        layer.doBuildSiteSpecies();
        layer.doCompleteSiteSpeciesSiteIndexInfo();

        assertThat(sp64.getTotalAge(), is(100.0));
    }


}
