package ca.bc.gov.nrs.vdyp.backend.projection.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.ProjectionTypeCode;

public class LayerTest {

	@Test
	void TestLayerBuilder() {

		var layerAdjustments = new LayerAdjustments();
		var history = new History.Builder().build();
		var polygon = new Polygon.Builder().build();

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
}
