package ca.bc.gov.nrs.vdyp.backend.projection.output.yieldtable;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.ArrayList;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.AbstractProjectionRequestException;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.backend.model.v1.ProjectionRequestKind;
import ca.bc.gov.nrs.vdyp.backend.projection.PolygonProjectionState;
import ca.bc.gov.nrs.vdyp.backend.projection.ProjectionContext;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Layer;
import ca.bc.gov.nrs.vdyp.backend.projection.model.LayerReportingInfo;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Polygon;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Species;
import ca.bc.gov.nrs.vdyp.backend.projection.model.SpeciesReportingInfo;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Stand;
import ca.bc.gov.nrs.vdyp.si32.site.SiteTool;

public class YieldTableRowContextTest {
    Parameters params;
    ProjectionContext context;
    Polygon polygon;
    PolygonProjectionState state;
    Layer layer;
    LayerReportingInfo layerInfo;
    @BeforeEach
    void setup() throws AbstractProjectionRequestException {
        params = new Parameters().ageStart(0).ageEnd(100);
        polygon = new Polygon.Builder().build();
        context = new ProjectionContext(ProjectionRequestKind.HCSV, "TEST", params, false);
        state = new PolygonProjectionState();
        layer = new Layer.Builder().layerId("1").polygon(polygon).build();
        Stand stand = new Stand.Builder().layer(layer).sp0Code("PL").build();
        layer.addStand(stand);
        Species species1 = new Species.Builder().stand(stand).speciesCode("PL").speciesPercent(60.0).build();
        layer.addSp64(species1);
        layerInfo = new LayerReportingInfo.Builder().layer(layer).build();
    }

    @Test
    void testSortedSpeciesFromLayerReportingInfo() {
        YieldTableRowContext.of(context, polygon, state, layerInfo);
    }

    static class AgeYearCombo{
        private AgeYearCombo(Parameters.AgeYearRangeCombinationKind k, Integer as, Integer ae, Integer ys, Integer ye){
            ageStart = as;
            ageEnd = ae;
            yearStart = ys;
            yearEnd = ye;
            kind = k;
        }
        static AgeYearCombo of(Parameters.AgeYearRangeCombinationKind kind, Integer ageStart, Integer ageEnd, Integer yearStart, Integer yearEnd){
            return new AgeYearCombo(kind, ageStart, ageEnd, yearStart, yearEnd);
        }
        Parameters.AgeYearRangeCombinationKind kind;
        final Integer ageStart;
        final Integer ageEnd;
        final Integer yearStart;
        final Integer yearEnd;
    }
    static class AgeYearComboResults{
        private AgeYearComboResults(Integer ays, Integer aye, Integer ags, Integer age){
            ageAtStartYear = ays;
            ageAtEndYear = aye;
            ageAtGapStart = ags;
            ageAtGapEnd = age;
        }
        static AgeYearComboResults of(Integer ageAtStartYear, Integer ageAtEndYear, Integer ageAtGapStart, Integer ageAtGapEnd){
            return new AgeYearComboResults(ageAtStartYear, ageAtEndYear, ageAtGapStart, ageAtGapEnd);
        }
        final Integer ageAtStartYear;
        final Integer ageAtEndYear;
        final Integer ageAtGapStart;
        final Integer ageAtGapEnd;
    }

    static Stream<Arguments> ageYearCombinations(){
        return Stream.of(
                /*
                Arguments.of(AgeYearCombo.of(null, 0,100,null,null),
                        AgeYearComboResults.of(0,100,null,null)),
                Arguments.of(AgeYearCombo.of(null, null,null,2000,2100),
                        AgeYearComboResults.of(50,150,null,null)),
                Arguments.of(AgeYearCombo.of(Parameters.AgeYearRangeCombinationKind.INTERSECT, 0,100,2000,2100),// Overlapping Intersection
                        AgeYearComboResults.of(50,100,null,null)),
                Arguments.of(AgeYearCombo.of(Parameters.AgeYearRangeCombinationKind.INTERSECT, 0,40,2000,2100), // Disjoint Intersection
                        AgeYearComboResults.of(null,null,null,null)),*/
                Arguments.of(AgeYearCombo.of(Parameters.AgeYearRangeCombinationKind.UNION, 0,100,2000,2100), // Overlapping Union
                        AgeYearComboResults.of(0,150,null,null)),
                Arguments.of(AgeYearCombo.of(Parameters.AgeYearRangeCombinationKind.UNION, 0,40,2000,2100), // Disjoint Union
                        AgeYearComboResults.of(0,150,40,50))
        );
    }


    @ParameterizedTest
    @MethodSource("ageYearCombinations")
    void testAgeYearCombinations(AgeYearCombo ageYearParams, AgeYearComboResults results) throws AbstractProjectionRequestException {
        params = new Parameters().ageStart(ageYearParams.ageStart).ageEnd(ageYearParams.ageEnd).yearStart(ageYearParams.yearStart).yearEnd(ageYearParams.yearEnd).combineAgeYearRange(ageYearParams.kind);
        polygon = new Polygon.Builder().referenceYear(1950).build();
        context = new ProjectionContext(ProjectionRequestKind.HCSV, "TEST", params, false);
        state = new PolygonProjectionState();
        layer = new Layer.Builder().layerId("1").polygon(polygon).build();
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

        var speciesReportingInfo = new SpeciesReportingInfo.Builder() //
                .sp64Name(sp64Code) //
                .sp64Percent(100d) //
                .asSuppliedIndex(0) //
                .build();
        layerInfo = new LayerReportingInfo.Builder().layer(layer).build();
        ArrayList<SpeciesReportingInfo> list = new ArrayList<SpeciesReportingInfo>();
        list.add(speciesReportingInfo);
        layerInfo.setSpeciesReportingInfos(list);
        var yieldTableRowContext = YieldTableRowContext.of(context, polygon, state, layerInfo);
        assertThat(yieldTableRowContext.getAgeAtStartYear(), is(results.ageAtStartYear));
        assertThat(yieldTableRowContext.getAgeAtEndYear(), is(results.ageAtEndYear));
        assertThat(yieldTableRowContext.getAgeAtGapStart(), is(results.ageAtGapStart));
        assertThat(yieldTableRowContext.getAgeAtGapEnd(), is(results.ageAtGapEnd));
    }
}
