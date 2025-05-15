package ca.bc.gov.nrs.vdyp.backend.model.v1;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.HashMap;

import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.backend.projection.input.HcsvPolygonRecordBean.NonVegCoverDetails;
import ca.bc.gov.nrs.vdyp.backend.projection.input.HcsvPolygonRecordBean.OtherVegCoverDetails;
import ca.bc.gov.nrs.vdyp.backend.projection.model.History;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Layer;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Polygon;
import ca.bc.gov.nrs.vdyp.backend.projection.model.PolygonReportingInfo;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.CfsEcoZoneCode;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.InventoryStandard;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.LayerSummarizationModeCode;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.NonVegetationTypeCode;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.OtherVegetationTypeCode;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.ProjectionTypeCode;

public class PolygonTest {

	@Test
	void TestPolygonBuilder() {

		var history = new History.Builder().build();
		var polygonReportingInfo = new PolygonReportingInfo.Builder().build();
		var otherVegetationTypes = new HashMap<OtherVegetationTypeCode, OtherVegCoverDetails>();
		var nonVegetationTypes = new HashMap<NonVegetationTypeCode, NonVegCoverDetails>();
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
}
