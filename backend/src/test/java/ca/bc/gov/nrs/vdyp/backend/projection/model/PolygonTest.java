package ca.bc.gov.nrs.vdyp.backend.projection.model;

import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.notPresent;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.present;
import static org.hamcrest.Matchers.allOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;

import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.AbstractProjectionRequestException;
import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.PolygonValidationException;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.backend.model.v1.ProjectionRequestKind;
import ca.bc.gov.nrs.vdyp.backend.projection.ProjectionContext;
import ca.bc.gov.nrs.vdyp.backend.projection.input.HcsvPolygonRecordBean;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.CfsEcoZoneCode;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.InventoryStandard;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.LayerSummarizationModeCode;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.NonVegetationTypeCode;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.OtherVegetationTypeCode;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.ProjectionTypeCode;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.SilviculturalBaseCode;
import ca.bc.gov.nrs.vdyp.common_calculators.enumerations.SiteIndexEquation;
import ca.bc.gov.nrs.vdyp.si32.enumerations.SpeciesRegion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.backend.model.v1.PolygonMessageKind;
import ca.bc.gov.nrs.vdyp.si32.site.SiteTool;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.beans.IntrospectionException;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class PolygonTest {


    @Test
    void TestPolygonBuilder() {

        var history = new History.Builder().build();
        var polygonReportingInfo = new PolygonReportingInfo.Builder().build();
        var otherVegetationTypes = new HashMap<OtherVegetationTypeCode, HcsvPolygonRecordBean.OtherVegCoverDetails>();
        var nonVegetationTypes = new HashMap<NonVegetationTypeCode, HcsvPolygonRecordBean.NonVegCoverDetails>();
        var doAllowProjectionOfType = new HashMap<ProjectionTypeCode, Boolean>();

        var layers = new HashMap<String, Layer>();
        var layersByProjectionType = new HashMap<ProjectionTypeCode, Layer>();

        var polygon = new Polygon.Builder() //
                .featureId(1) //
                .polygonNumber(2L) //
                .district("D") //
                .mapSheet("M") //
                .mapQuad("Q") //
                .mapSubQuad("SQ") //
                .inventoryStandard(InventoryStandard.FIP) //
                .referenceYear(2000) //
                .yearOfDeath(2020) //
                .isCoastal(true) //
                .forestInventoryZone("A") //
                .becZone("AT") //
                .cfsEcoZone(CfsEcoZoneCode.ArcticCordillera) //
                .nonProductiveDescriptor("NPD") //
                .percentStockable(20.0) //
                .percentStockableDead(20.0) //
                .yieldFactor(20.0) //
                .wereLayerAdjustmentsSupplied(false) //
                .layers(layers) //
                .layerByProjectionType(layersByProjectionType) //
                .history(history) //
                .reportingInfo(polygonReportingInfo) //
                .otherVegetationTypes(otherVegetationTypes) //
                .nonVegetationTypes(nonVegetationTypes) //
                .layerSummarizationMode(LayerSummarizationModeCode.TwoLayer) //
                .doAllowProjection(true) //
                .doAllowProjectionOfType(doAllowProjectionOfType) //
                .build();

        assertThat(
                polygon, allOf(
                        hasProperty("featureId", is(1L)), //
                        hasProperty("polygonNumber", is(2L)), //
                        hasProperty("district", is("D")), //
                        hasProperty("mapSheet", is("M")), //
                        hasProperty("mapQuad", is("Q")), //
                        hasProperty("mapSubQuad", is("SQ")), //
                        hasProperty("inventoryStandard", is(InventoryStandard.FIP)), //
                        hasProperty("referenceYear", is(2000)), //
                        hasProperty("yearOfDeath", is(2020)), //
                        hasProperty("isCoastal", is(true)), //
                        hasProperty("forestInventoryZone", is("A")), //
                        hasProperty("becZone", is("AT")), //
                        hasProperty("cfsEcoZone", is(CfsEcoZoneCode.ArcticCordillera)), //
                        hasProperty("nonProductiveDescriptor", is("NPD")), //
                        hasProperty("percentStockable", is(20.0)), //
                        hasProperty("percentStockableDead", is(20.0)), //
                        hasProperty("yieldFactor", is(20.0)), //
                        hasProperty("wereLayerAdjustmentsSupplied", is(false)), //
                        hasProperty("layers", is(layers)), //
                        hasProperty("history", is(history)), //
                        hasProperty("reportingInfo", is(polygonReportingInfo)), //
                        hasProperty("otherVegetationTypes", is(otherVegetationTypes)), //
                        hasProperty("nonVegetationTypes", is(nonVegetationTypes)), //
                        hasProperty("layerSummarizationMode", is(LayerSummarizationModeCode.TwoLayer)), //
                        hasProperty("doAllowProjection", is(true))
                )
        );

        var layer = new Layer.Builder() //
                .layerId("1") //
                .assignedProjectionType(ProjectionTypeCode.PRIMARY) //
                .polygon(polygon).build();

        layers.put(layer.getLayerId(), layer);
        layersByProjectionType.put(ProjectionTypeCode.PRIMARY, layer);

        assertThat(polygon.getLayerByProjectionType(ProjectionTypeCode.PRIMARY), is(layer));
    }

    @Nested
	class TweakSpeciesPercentages {
		Polygon polygon;

		Layer layer;

		@BeforeEach
		void setup() {
			polygon = new Polygon.Builder().build();

			layer = new Layer.Builder().polygon(polygon).layerId("Test").build();
		}

		@Test
		void noSpecies() {
			var message = polygon.tweakPercentages(layer);

			// No error

			assertThat(message, notPresent());

			// No change

			assertThat(layer.getSp0sAsSupplied(), iterableWithSize(0));
		}

		@Test
		void oneStandExact() {
			var sp64Code = "PL";
			var sp0Code = SiteTool.getSpeciesVDYP7Code(sp64Code);

			var stand = new Stand.Builder() //
					.sp0Code("P") //
					.layer(layer) //
					.build();

			var sp64 = new Species.Builder() //
					.stand(stand) //
					.speciesCode(sp64Code) //
					.speciesPercent(100d) //
					.totalAge(40d) //
					.dominantHeight(20d) //
					.build();

			Species sp0 = new Species.Builder().stand(stand) //
					.speciesCode(sp64Code) //
					.speciesPercent(0) //
					.build();
			stand.addSpeciesGroup(sp0, layer.getSp0sAsSupplied().size());
			layer.addStand(stand);

			sp64.calculateUndefinedFieldValues();

			stand.addSp64(sp64);
			layer.addSp64(sp64);

			var message = polygon.tweakPercentages(layer);

			// No error

			assertThat(message, notPresent());

			// No change

			assertThat(layer.getSp0sAsSupplied(), iterableWithSize(1));
			assertThat(layer.getSp0sAsSupplied().get(0), hasProperty("speciesByPercent", iterableWithSize(1)));
			assertThat(
					layer.getSp0sAsSupplied().get(0).getSpeciesByPercent().get(0), hasProperty(
							"speciesPercent", closeTo(100d, 0.1d)
					)
			);

		}

		@Test
		void oneStandSlightlyOver() {
			var sp64Code = "PL";
			var sp0Code = SiteTool.getSpeciesVDYP7Code(sp64Code);

			var stand = new Stand.Builder() //
					.sp0Code("P") //
					.layer(layer) //
					.build();

			var sp64 = new Species.Builder() //
					.stand(stand) //
					.speciesCode(sp64Code) //
					.speciesPercent(100.5d) //
					.totalAge(40d) //
					.dominantHeight(20d) //
					.build();

			Species sp0 = new Species.Builder().stand(stand) //
					.speciesCode(sp64Code) //
					.speciesPercent(0) //
					.build();
			stand.addSpeciesGroup(sp0, layer.getSp0sAsSupplied().size());
			layer.addStand(stand);

			sp64.calculateUndefinedFieldValues();

			stand.addSp64(sp64);
			layer.addSp64(sp64);

			var message = polygon.tweakPercentages(layer);

			// No error

			assertThat(message, notPresent());

			// Corrected

			assertThat(layer.getSp0sAsSupplied(), iterableWithSize(1));
			assertThat(layer.getSp0sAsSupplied().get(0), hasProperty("speciesByPercent", iterableWithSize(1)));
			assertThat(
					layer.getSp0sAsSupplied().get(0).getSpeciesByPercent().get(0), hasProperty(
							"speciesPercent", closeTo(100d, 0.1d)
					)
			);

		}

		@Test
		void oneStandSlightlyUnder() {
			var sp64Code = "PL";
			var sp0Code = SiteTool.getSpeciesVDYP7Code(sp64Code);

			var stand = new Stand.Builder() //
					.sp0Code("P") //
					.layer(layer) //
					.build();

			var sp64 = new Species.Builder() //
					.stand(stand) //
					.speciesCode(sp64Code) //
					.speciesPercent(99.75d) //
					.totalAge(40d) //
					.dominantHeight(20d) //
					.build();

			Species sp0 = new Species.Builder().stand(stand) //
					.speciesCode(sp64Code) //
					.speciesPercent(0) //
					.build();
			stand.addSpeciesGroup(sp0, layer.getSp0sAsSupplied().size());
			layer.addStand(stand);

			sp64.calculateUndefinedFieldValues();

			stand.addSp64(sp64);
			layer.addSp64(sp64);

			var message = polygon.tweakPercentages(layer);

			// No error

			assertThat(message, notPresent());

			// Corrected

			assertThat(layer.getSp0sAsSupplied(), iterableWithSize(1));
			assertThat(layer.getSp0sAsSupplied().get(0), hasProperty("speciesByPercent", iterableWithSize(1)));
			assertThat(
					layer.getSp0sAsSupplied().get(0).getSpeciesByPercent().get(0), hasProperty(
							"speciesPercent", closeTo(100d, 0.1d)
					)
			);

		}

		@Test
		void oneStandWayOver() {
			var sp64Code = "PL";
			var sp0Code = SiteTool.getSpeciesVDYP7Code(sp64Code);

			var stand = new Stand.Builder() //
					.sp0Code("P") //
					.layer(layer) //
					.build();

			var sp64 = new Species.Builder() //
					.stand(stand) //
					.speciesCode(sp64Code) //
					.speciesPercent(102d) //
					.totalAge(40d) //
					.dominantHeight(20d) //
					.build();

			Species sp0 = new Species.Builder().stand(stand) //
					.speciesCode(sp64Code) //
					.speciesPercent(0) //
					.build();
			stand.addSpeciesGroup(sp0, layer.getSp0sAsSupplied().size());
			layer.addStand(stand);

			sp64.calculateUndefinedFieldValues();

			stand.addSp64(sp64);
			layer.addSp64(sp64);

			var message = polygon.tweakPercentages(layer);

			// Error

			assertThat(message, present(hasProperty("kind", is(PolygonMessageKind.LAYER_PERCENTAGES_TOO_INACCURATE))));

			// No change

			assertThat(layer.getSp0sAsSupplied(), iterableWithSize(1));
			assertThat(layer.getSp0sAsSupplied().get(0), hasProperty("speciesByPercent", iterableWithSize(1)));
			assertThat(
					layer.getSp0sAsSupplied().get(0).getSpeciesByPercent().get(0), hasProperty(
							"speciesPercent", closeTo(102, 0.1d)
					)
			);

		}

		@Test
		void oneStandWayUnder() {
			var sp64Code = "PL";
			var sp0Code = SiteTool.getSpeciesVDYP7Code(sp64Code);

			var stand = new Stand.Builder() //
					.sp0Code("P") //
					.layer(layer) //
					.build();

			var sp64 = new Species.Builder() //
					.stand(stand) //
					.speciesCode(sp64Code) //
					.speciesPercent(98d) //
					.totalAge(40d) //
					.dominantHeight(20d) //
					.build();

			Species sp0 = new Species.Builder().stand(stand) //
					.speciesCode(sp64Code) //
					.speciesPercent(0) //
					.build();
			stand.addSpeciesGroup(sp0, layer.getSp0sAsSupplied().size());
			layer.addStand(stand);

			sp64.calculateUndefinedFieldValues();

			stand.addSp64(sp64);
			layer.addSp64(sp64);

			var message = polygon.tweakPercentages(layer);

			// Error

			assertThat(message, present(hasProperty("kind", is(PolygonMessageKind.LAYER_PERCENTAGES_TOO_INACCURATE))));

			// No change

			assertThat(layer.getSp0sAsSupplied(), iterableWithSize(1));
			assertThat(layer.getSp0sAsSupplied().get(0), hasProperty("speciesByPercent", iterableWithSize(1)));
			assertThat(
					layer.getSp0sAsSupplied().get(0).getSpeciesByPercent().get(0), hasProperty(
							"speciesPercent", closeTo(98d, 0.1d)
					)
			);

		}
	}

    @Nested
    class CalculateMissingData {
        Parameters params;
        ProjectionContext context;
        Polygon polygon;
        Layer layer;
        History history;
        String sp64Code = "PL";
        String sp0Code = SiteTool.getSpeciesVDYP7Code(sp64Code);

        Double leadingSpeciesAge = 40d;
        Double leadingSpeciesHeight = 20d;

        //Get minimum Base Config For Polygon Calculation Testing
        Polygon.Builder baseConfig(Polygon.Builder builder) {
            return builder
                    .history(history)
                    .district(null /* not available in HCSV input */) //
                    .doAllowProjection(true) //
                    .doAllowProjectionOfType(GetAllowProjectionOfType())
                    .layers(new HashMap<>());
        }

        //Get minimum Base Config For Polygon Calculation Testing
        Layer.Builder baseConfig(Layer.Builder builder) {
            return builder
                    .assignedProjectionType(ProjectionTypeCode.UNKNOWN)
                    .layerId("Test")
                    .doSuppressPerHAYields(false)
                    .doIncludeWithProjection(true);
        }
        //Get minimum Base Config For Polygon Calculation Testing
        Stand.Builder baseConfig(Stand.Builder builder) {
            return builder //
                    .sp0Code("P") ;
        }
        Species.Builder baseConfigSp64(Species.Builder builder) {
            return builder
                    .speciesCode(sp64Code) //
                    .speciesPercent(100d) //
                    .totalAge(leadingSpeciesAge) //
                    .dominantHeight(leadingSpeciesHeight);
        }

        Species.Builder baseConfigSp0(Species.Builder builder) {
            return builder
                    .speciesCode(sp64Code) //
                    .speciesPercent(0);
        }

        static Map<ProjectionTypeCode, Boolean> allowProjectionOfType;
        static Map<ProjectionTypeCode, Boolean> GetAllowProjectionOfType(){
            if (allowProjectionOfType == null)
            {
                allowProjectionOfType= new EnumMap<ProjectionTypeCode, Boolean>(ProjectionTypeCode.class);
                for (ProjectionTypeCode t : ProjectionTypeCode.values()) {
                    allowProjectionOfType.put(t, true);
                }
            }
            return allowProjectionOfType;
        }

        record LayersCase(
                List<ProjectionTypeCode> layerTypes,
                Map<ProjectionTypeCode, Map<String,Object>> layerParams
        ){}


        void addLayerByType(ProjectionTypeCode code, Map<String, Object> params)
        {
            leadingSpeciesHeight = 10.0;
            leadingSpeciesAge = 50.0;
            var layerBuilder = baseConfig(new Layer.Builder())
                    .layerId("TEST_"+code.name)
                    .vdyp7LayerCode(code)
                    .polygon(polygon)
                    .crownClosure((short)0); // default can be overidden
            if (params != null) {
                for (var param : params.entrySet()) {
                    switch (param.getKey()) {
                        case "cc" -> layerBuilder.crownClosure((short) param.getValue());
                        case "tph" -> layerBuilder.treesPerHectare((Double) param.getValue());
                        case "rankCode" -> layerBuilder.rankCode((String) param.getValue());
                        case "ba" -> layerBuilder.basalArea((Double) param.getValue());
                        case "ps" -> layerBuilder.percentStockable((Double) param.getValue());
                        case "id" -> layerBuilder.layerId((String) param.getValue());
                        case "age" -> leadingSpeciesAge = (Double) param.getValue();
                        case "height" -> leadingSpeciesHeight = (Double) param.getValue();
                    }
                }
            }

            var layer = layerBuilder.build();

            setupBaseTestSpecies(layer);
            polygon.getLayers().put(layer.getLayerId(), layer);

            if (code == ProjectionTypeCode.VETERAN && (Boolean)params.getOrDefault("force",false))
            {
                polygon.setTargetedVeteranLayer(layer);
            }

            if ("1".equals(layer.getRankCode()))
            {
                polygon.setRank1Layer(layer);
            }
            switch(code)
            {
                case DEAD:
                    polygon.assignDeadLayer(layer,1950, layer.getPercentStockable());
                    break;
                case RESIDUAL:
                    polygon.setResidualLayer(layer);
                    break;
                case REGENERATION:
                    polygon.setRegenerationLayer(layer);
                    break;
            }
        }

        @BeforeEach
        void setup() {
            try {
                history = new History.Builder().build();
                params = new Parameters().ageStart(0).ageEnd(100);
                context = new ProjectionContext(
                        ProjectionRequestKind.HCSV,
                        "Test",
                        params, false);
            } catch (AbstractProjectionRequestException e) {
                // No action
            }
        }

        void setupBaseTestSpecies(Layer layer)
        {
            var stand = baseConfig(new Stand.Builder())//
                    .layer(layer) //
                    .build();

            var sp64 = baseConfigSp64(new Species.Builder().stand(stand)) //
                    .build();

            Species sp0 = baseConfigSp0(new Species.Builder().stand(stand))//
                    .build();
            stand.addSpeciesGroup(sp0, layer.getSp0sAsSupplied().size());
            layer.addStand(stand);

            sp64.calculateUndefinedFieldValues();

            stand.addSp64(sp64);
            layer.addSp64(sp64);
        }

        @Nested
        class EstimateYieldFactor {

            static Stream<Arguments> yieldFactors() {
                return Stream.of(
                        Arguments.of(0.5, 0.5),
                        Arguments.of(1.0, 1.0),
                        Arguments.of(0.0, 1.0),
                        Arguments.of(null, 1.0)
                );
            }

            @ParameterizedTest
            @MethodSource("yieldFactors")
            void testExplicitYieldFactor(Double providedYield, Double expectedYield) throws PolygonValidationException {

                polygon = baseConfig(new Polygon.Builder())
                        .yieldFactor(providedYield)
                        .build();

                layer = baseConfig(new Layer.Builder())
                        .polygon(polygon)
                        .crownClosure((short) 2)
                        .vdyp7LayerCode(ProjectionTypeCode.PRIMARY)
                        .build();

                setupBaseTestSpecies(layer);

                polygon.getLayers().put(layer.getLayerId(), layer);

                polygon.doCompleteDefinition(context);

                assertThat(polygon.getYieldFactor(), is(expectedYield));

            }

            static Stream<Arguments> fipYieldFactors() {
                return Stream.of(
                        Arguments.of((short) 13, 0.13),
                        Arguments.of((short) 50, 0.5),
                        Arguments.of((short) 51, 0.5),
                        Arguments.of((short) 75, 0.5)
                );
            }

            @ParameterizedTest
            @MethodSource("fipYieldFactors")
            void testUndefinedFIPYieldFactor(Short crownClosure, Double expectedYield) throws PolygonValidationException {

                polygon = baseConfig(new Polygon.Builder())
                        .yieldFactor(null)
                        .nonProductiveDescriptor("NSP")
                        .inventoryStandard(InventoryStandard.FIP)
                        .build();

                layer = baseConfig(new Layer.Builder())
                        .polygon(polygon)
                        .crownClosure(crownClosure)
                        .build();
                setupBaseTestSpecies(layer);

                polygon.getLayers().put(layer.getLayerId(), layer);

                polygon.doCompleteDefinition(context);

                assertThat(polygon.getYieldFactor(), is(expectedYield));

            }
        }

        @Nested
        class EstimateStockability {

            @Test
            void testEstimateWithExplicitValue()  throws PolygonValidationException{
                polygon = baseConfig(new Polygon.Builder())
                        .percentStockable(50d)
                        .build();

                layer = baseConfig(new Layer.Builder())
                        .polygon(polygon)
                        .crownClosure((short) 50)
                        .build();

                setupBaseTestSpecies(layer);

                polygon.getLayers().put(layer.getLayerId(), layer);

                polygon.doCompleteDefinition(context);

                assertThat(polygon.getPercentStockable(), is(50d));

            }

            @Test
            void testEstimateWithDeadLayer()  throws PolygonValidationException{
                polygon = baseConfig(new Polygon.Builder())
                        .build();

                layer = baseConfig(new Layer.Builder())
                        .polygon(polygon)
                        .crownClosure((short) 50)
                        .build();

                setupBaseTestSpecies(layer);

                var deadLayer = baseConfig(new Layer.Builder())
                        .layerId("DEAD")
                        .isDeadLayer(true)
                        .yearOfDeath(2000)
                        .polygon(polygon)
                        .build();

                polygon.getLayers().put(layer.getLayerId(), layer);
                polygon.getLayers().put(deadLayer.getLayerId(), deadLayer);

                polygon.assignDeadLayer(deadLayer, 2000, 25.0);

                polygon.doCompleteDefinition(context);

                assertThat(polygon.getPercentStockable(), is(85d));
            }

            @Test
            void testPrimaryLayerNoLeadingSpecies()  throws PolygonValidationException{
                polygon = baseConfig(new Polygon.Builder())
                        .build();

                layer = baseConfig(new Layer.Builder())
                        .polygon(polygon)
                        .crownClosure((short) 50)
                        .build();

                polygon.getLayers().put(layer.getLayerId(), layer);

                assertThrows(PolygonValidationException.class, () -> polygon.doCompleteDefinition(context));
            }

            static Stream<Arguments> disturbedValues() {
                return Stream.of(
                        Arguments.of(2000, 2000, 95.71), // equal years no years value for calculation
                        Arguments.of(2000, 2010, 90.22), // measurement year < disturbance use age
                        Arguments.of(2000, 1990, 95.71), // 10 years
                        Arguments.of(2000, 1980, 95.71), // 20 years
                        Arguments.of(2000, 1970, 92.82), // 30 years
                        Arguments.of(2000, 1960, 90.22), // 40 years
                        Arguments.of(2000, 1950, 90.22) // 50 years capped to 40 years (age)
                );
            }
            @ParameterizedTest
            @MethodSource("disturbedValues")
            void testSilvicultureDisturbanceHistory(int measurementYear, int disturbanceStartYear, Double expected)  throws PolygonValidationException{
                history = new History.Builder()
                        .disturbanceStartYear(disturbanceStartYear)
                        .silvicultureBase(SilviculturalBaseCode.DISTURBED)
                        .build();

                polygon = baseConfig(new Polygon.Builder())
                        .history(history)
                        .referenceYear(measurementYear)
                        .build();

                layer = baseConfig(new Layer.Builder())
                        .polygon(polygon)
                        .crownClosure((short) 50)
                        .build();

                setupBaseTestSpecies(layer);

                polygon.getLayers().put(layer.getLayerId(), layer);

                polygon.doCompleteDefinition(context);

                assertThat(polygon.getPercentStockable(), closeTo(expected,0.01d));
            }

            static Stream<Arguments> p1AgeValues() {
                return Stream.of(
                        Arguments.of(Vdyp7Constants.YEAR_LIMIT / 2, 95.71),// less than YEAR_LIMIT
                        Arguments.of(Vdyp7Constants.YEAR_MAX * 2, 71.42),// greater than YEAR_MAX
                        Arguments.of(Vdyp7Constants.YEAR_LIMIT , 95.71),//precisely year limit
                        Arguments.of(Vdyp7Constants.YEAR_LIMIT + Vdyp7Constants.YEAR_HALF, 81.42),// exponent of exactly 1
                        Arguments.of(199, 71.42)// designed to illicit neg lp1
                );
            }
            @ParameterizedTest
            @MethodSource("p1AgeValues")
            void testP1Calculations(double age, double expected)  throws PolygonValidationException{
                polygon = baseConfig(new Polygon.Builder()).build();

                layer = baseConfig(new Layer.Builder())
                        .polygon(polygon)
                        .crownClosure((short) 50)
                        .build();

                leadingSpeciesAge = age;
                setupBaseTestSpecies(layer);

                polygon.getLayers().put(layer.getLayerId(), layer);

                polygon.doCompleteDefinition(context);

                assertThat(polygon.getPercentStockable(), closeTo(expected,0.01d));
            }

            @Test
            void testInvalidCCCauseNoPercentUnaccounted()  throws PolygonValidationException{
                polygon = baseConfig(new Polygon.Builder()).build();

                layer = baseConfig(new Layer.Builder())
                        .polygon(polygon)
                        .crownClosure((short) 100)
                        .build();

                setupBaseTestSpecies(layer);

                polygon.getLayers().put(layer.getLayerId(), layer);

                polygon.doCompleteDefinition(context);

                assertThat(polygon.getPercentStockable(), closeTo(100,0.01d));
            }

            @Test
            void testFIPNonProductiveNominalStockability()  throws PolygonValidationException{
                polygon = baseConfig(new Polygon.Builder())
                        .yieldFactor(0.001) // force non productive FIP calculation under 1
                        .nonProductiveDescriptor("NSP")
                        .inventoryStandard(InventoryStandard.FIP)
                        .build();

                layer = baseConfig(new Layer.Builder())
                        .polygon(polygon)
                        .crownClosure((short)1)
                        .build();
                setupBaseTestSpecies(layer);

                polygon.getLayers().put(layer.getLayerId(), layer);

                polygon.doCompleteDefinition(context);

                assertThat(polygon.getPercentStockable(), is(1.0));
            }
        }

        @Nested
        class DetermineStockabilityForProjectionType{
            @BeforeEach
            void setup()
            {
                try {
                    history = new History.Builder().build();
                    params = new Parameters().ageStart(0).ageEnd(100);
                    context = new ProjectionContext(
                            ProjectionRequestKind.HCSV,
                            "Test",
                            params, false);
                } catch (AbstractProjectionRequestException e) {
                    // No action
                }
                polygon = baseConfig(new Polygon.Builder())
                        .percentStockable(85.0)
                        .percentStockableDead(0.0)
                        .build();
            }

            static Stream<Arguments> determinePolygonStockabilityCases(){
                return Stream.of(
                        Arguments.of(
                                new LayersCase( // Happy Path
                                        ProjectionTypeCode.ACTUAL_PROJECTION_TYPES_LIST,
                                        Map.of(
                                                ProjectionTypeCode.VETERAN,Map.of("id","1","cc",(short)5,"ba",20.0,"tph",20.0, "age",141.0,"height",11.0),
                                                ProjectionTypeCode.PRIMARY, Map.of("rankCode","1","ba",40.0),
                                                ProjectionTypeCode.RESIDUAL,Map.of("ba",20.0),
                                                ProjectionTypeCode.DEAD, Map.of("ps", 10.0)
                                        )
                                ),
                                Map.of(
                                        ProjectionTypeCode.PRIMARY,  42.0,
                                        ProjectionTypeCode.VETERAN, 5.0,
                                        ProjectionTypeCode.RESIDUAL, 21.0,
                                        ProjectionTypeCode.DEAD, 8.5,
                                        ProjectionTypeCode.REGENERATION, 8.5
                                )
                        ),
                        Arguments.of(
                                new LayersCase( // Nominal Dead
                                        List.of(ProjectionTypeCode.PRIMARY,ProjectionTypeCode.DEAD),
                                        Map.of(
                                                ProjectionTypeCode.PRIMARY,Map.of("ps",10.0, "cc",(short)11),
                                                ProjectionTypeCode.DEAD,Map.of("ps",0.0, "cc",(short)11)
                                        )
                                ),
                                Map.of(
                                        ProjectionTypeCode.PRIMARY, 84.0,
                                        ProjectionTypeCode.DEAD, 1.0
                                )
                        ),
                        Arguments.of(
                                new LayersCase( // Nominal Regeneration
                                        List.of(ProjectionTypeCode.PRIMARY,ProjectionTypeCode.REGENERATION),
                                        Map.of(
                                                ProjectionTypeCode.PRIMARY,Map.of("ps",10.0, "cc",(short)11),
                                                ProjectionTypeCode.REGENERATION,Map.of("ps",0.0, "cc",(short)11)
                                        )
                                ),
                                Map.of(
                                        ProjectionTypeCode.PRIMARY, 84.0,
                                        ProjectionTypeCode.REGENERATION, 1.0
                                )
                        ),
                        Arguments.of(
                                new LayersCase( // Nominal Veteran
                                        List.of(ProjectionTypeCode.PRIMARY,ProjectionTypeCode.VETERAN),
                                        Map.of(
                                                ProjectionTypeCode.PRIMARY,Map.of("ps",10.0, "cc",(short)11),
                                                ProjectionTypeCode.VETERAN,Map.of("force",true,"cc",(short)0,"ba",20.0,"tph",20.0, "age",141.0,"height",11.0, "rankCode","1")
                                        )
                                ),
                                Map.of(
                                        ProjectionTypeCode.PRIMARY, 84.0,
                                        ProjectionTypeCode.VETERAN, 1.0
                                )
                        ),
                        Arguments.of(
                                new LayersCase( // Nominal Residual
                                        List.of(ProjectionTypeCode.PRIMARY,ProjectionTypeCode.RESIDUAL),
                                        Map.of(
                                                ProjectionTypeCode.PRIMARY,Map.of("ps",10.0, "cc",(short)11, "ba",100.0),
                                                ProjectionTypeCode.RESIDUAL,Map.of("cc",(short)11,"ba",1.0)
                                        )
                                ),
                                Map.of(
                                        ProjectionTypeCode.PRIMARY, 84.15,
                                        ProjectionTypeCode.RESIDUAL, 1.0
                                )
                        ),
                        Arguments.of(
                                new LayersCase( // Nominal Primary
                                        List.of(ProjectionTypeCode.PRIMARY,ProjectionTypeCode.RESIDUAL),
                                        Map.of(
                                                ProjectionTypeCode.RESIDUAL,Map.of("ps",10.0, "cc",(short)11, "ba",100.0),
                                                ProjectionTypeCode.PRIMARY,Map.of("cc",(short)11,"ba",1.0)
                                        )
                                ),
                                Map.of(
                                        ProjectionTypeCode.RESIDUAL, 84.15,
                                        ProjectionTypeCode.PRIMARY, 1.0
                                )
                        ),
                        Arguments.of(
                                new LayersCase( // non basal area primary without dead
                                        List.of(ProjectionTypeCode.PRIMARY,ProjectionTypeCode.RESIDUAL),
                                        Map.of(
                                                ProjectionTypeCode.PRIMARY,Map.of("ps",10.0, "cc",(short)11, "ba",100.0),
                                                ProjectionTypeCode.RESIDUAL,Map.of("cc",(short)11)
                                        )
                                ),
                                Map.of(
                                        ProjectionTypeCode.PRIMARY, 84.0,
                                        ProjectionTypeCode.RESIDUAL, 1.0
                                )
                        ),
                        Arguments.of(
                                new LayersCase( // non basal area primary with dead
                                        List.of(ProjectionTypeCode.PRIMARY,ProjectionTypeCode.RESIDUAL,ProjectionTypeCode.DEAD),
                                        Map.of(
                                                ProjectionTypeCode.PRIMARY,Map.of("ps",10.0, "cc",(short)11, "ba",100.0),
                                                ProjectionTypeCode.RESIDUAL,Map.of("cc",(short)11),
                                                ProjectionTypeCode.DEAD,Map.of("ps",10.0)
                                        )
                                ),
                                Map.of(
                                        ProjectionTypeCode.PRIMARY, 68.0,
                                        ProjectionTypeCode.RESIDUAL, 8.5,
                                        ProjectionTypeCode.DEAD, 8.5
                                )
                        ),
                        Arguments.of(
                                new LayersCase( // nominal no residual Primary
                                        List.of(ProjectionTypeCode.PRIMARY, ProjectionTypeCode.DEAD),
                                        Map.of(
                                                ProjectionTypeCode.PRIMARY,Map.of("ps",10.0, "cc",(short)11, "ba",100.0),
                                                ProjectionTypeCode.DEAD,Map.of("ps",100.0) // invalid but potentially accepted value
                                        )
                                ),
                                Map.of(
                                        ProjectionTypeCode.PRIMARY, 1.0,
                                        ProjectionTypeCode.DEAD, 85.0
                                )
                        ),
                        Arguments.of(
                                new LayersCase( // no residual Primary
                                        List.of(ProjectionTypeCode.PRIMARY, ProjectionTypeCode.VETERAN),
                                        Map.of(
                                                ProjectionTypeCode.PRIMARY,Map.of("cc",(short)11),
                                                ProjectionTypeCode.VETERAN,Map.of("cc",(short)10,"ba",20.0,"tph",20.0, "age",141.0,"height",11.0, "id","1") // invalid but potentially accepted value
                                        )
                                ),
                                Map.of(
                                        ProjectionTypeCode.PRIMARY, 75.0,
                                        ProjectionTypeCode.VETERAN, 10.0
                                )
                        )
                );
            }

            @ParameterizedTest
            @MethodSource("determinePolygonStockabilityCases")
            void testDetermineStockability(LayersCase testCase, Map<ProjectionTypeCode, Double> expectedValues) throws PolygonValidationException{
                for (ProjectionTypeCode code : testCase.layerTypes) {
                    addLayerByType(code, testCase.layerParams.getOrDefault(code, null));
                }
                polygon.doCompleteDefinition(context);
                assertThat(polygon.determineStockabilityByProjectionType(ProjectionTypeCode.PRIMARY), closeTo(expectedValues.getOrDefault(ProjectionTypeCode.PRIMARY,0.0),0.01d));
                assertThat(polygon.determineStockabilityByProjectionType(ProjectionTypeCode.VETERAN), closeTo(expectedValues.getOrDefault(ProjectionTypeCode.VETERAN,0.0),0.01d));
                assertThat(polygon.determineStockabilityByProjectionType(ProjectionTypeCode.RESIDUAL), closeTo(expectedValues.getOrDefault(ProjectionTypeCode.RESIDUAL,0.0),0.01d));
                assertThat(polygon.determineStockabilityByProjectionType(ProjectionTypeCode.DEAD), closeTo(expectedValues.getOrDefault(ProjectionTypeCode.DEAD,0.0),0.01d));
                assertThat(polygon.determineStockabilityByProjectionType(ProjectionTypeCode.REGENERATION), closeTo(expectedValues.getOrDefault(ProjectionTypeCode.REGENERATION,0.0),0.01d));
                assertThat(polygon.determineStockabilityByProjectionType(ProjectionTypeCode.UNKNOWN), is(0.0));
            }
        }

        @Nested
        class SelectPrimaryAdnVeteranLayers{
            @BeforeEach
            void setup() {
                try {
                    history = new History.Builder().build();
                    params = new Parameters().ageStart(0).ageEnd(100);
                    context = new ProjectionContext(
                            ProjectionRequestKind.HCSV,
                            "Test",
                            params, false);
                } catch (AbstractProjectionRequestException e) {
                    // No action
                }
            }

            static Stream<Arguments> selectImportantLayersCases(){
                return Stream.of(
                        Arguments.of( // FIP Veteran layer CC too high
                                InventoryStandard.FIP,
                                new LayersCase(
                                        List.of(ProjectionTypeCode.PRIMARY,ProjectionTypeCode.VETERAN),
                                        Map.of(
                                                ProjectionTypeCode.PRIMARY,Map.of("ps",10.0, "cc",(short)11,"rankcode","1"),
                                                ProjectionTypeCode.VETERAN,Map.of("cc",(short)10,"ba",20.0,"tph",20.0, "age",141.0,"height",11.0)
                                        )
                                ),
                                Map.of(
                                        ProjectionTypeCode.PRIMARY, true
                                )
                        ),
                        Arguments.of( // rank code disqualify veeran
                                InventoryStandard.FIP,
                                new LayersCase(
                                        List.of(ProjectionTypeCode.PRIMARY,ProjectionTypeCode.VETERAN),
                                        Map.of(
                                                ProjectionTypeCode.PRIMARY,Map.of("ps",10.0, "cc",(short)11),
                                                ProjectionTypeCode.VETERAN,Map.of("cc",(short)10,"ba",20.0,"tph",20.0, "age",141.0,"height",11.0, "id", "1", "rankcode","1")
                                        )
                                ),
                                Map.of(
                                        ProjectionTypeCode.PRIMARY, true
                                )
                        ),
                        Arguments.of( // Valid setup
                                InventoryStandard.FIP,
                                new LayersCase(
                                        List.of(ProjectionTypeCode.PRIMARY,ProjectionTypeCode.VETERAN),
                                        Map.of(
                                                ProjectionTypeCode.PRIMARY,Map.of("ps",10.0, "cc",(short)11),
                                                ProjectionTypeCode.VETERAN,Map.of("cc",(short)5,"ba",20.0,"tph",20.0, "age",141.0,"height",11.0, "id","1")
                                        )
                                ),
                                Map.of(
                                        ProjectionTypeCode.PRIMARY, true,
                                        ProjectionTypeCode.VETERAN, true
                                )
                        ),
                        Arguments.of( // Targetted veteran
                                InventoryStandard.FIP,
                                new LayersCase(
                                        List.of(ProjectionTypeCode.PRIMARY,ProjectionTypeCode.VETERAN),
                                        Map.of(
                                                ProjectionTypeCode.PRIMARY,Map.of("ps",10.0, "cc",(short)11),
                                                ProjectionTypeCode.VETERAN,Map.of("force",true,"cc",(short)5,"height",11.0, "id","1")
                                        )
                                ),
                                Map.of(
                                        ProjectionTypeCode.PRIMARY, true,
                                        ProjectionTypeCode.VETERAN, true
                                )
                        )

                );
            }

            @ParameterizedTest
            @MethodSource("selectImportantLayersCases")
            void testSelectImportantLayers(InventoryStandard inventory, LayersCase testCase, Map<ProjectionTypeCode, Boolean> expectedExists) throws PolygonValidationException{
                polygon = baseConfig(new Polygon.Builder())
                        .inventoryStandard(inventory)
                        .percentStockable(85.0)
                        .percentStockableDead(0.0)
                        .build();

                for (ProjectionTypeCode code : testCase.layerTypes) {
                    addLayerByType(code, testCase.layerParams.getOrDefault(code, null));
                }
                polygon.doCompleteDefinition(context);
                assertThat(polygon.getPrimaryLayer() != null, is(expectedExists.getOrDefault(ProjectionTypeCode.PRIMARY, false)));
                assertThat(polygon.getVeteranLayer() != null, is(expectedExists.getOrDefault(ProjectionTypeCode.VETERAN, false)));
            }
        }
    }
}
