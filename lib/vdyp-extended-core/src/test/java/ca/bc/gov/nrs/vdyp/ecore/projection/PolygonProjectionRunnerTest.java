package ca.bc.gov.nrs.vdyp.ecore.projection;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.AbstractProjectionRequestException;
import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.PolygonExecutionException;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.ProgressFrequency;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.ProjectionRequestKind;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.History;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.Layer;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.Polygon;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.Species;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.Stand;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.InventoryStandard;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.ProjectionTypeCode;
import ca.bc.gov.nrs.vdyp.si32.vdyp.VdypMethods;

public class PolygonProjectionRunnerTest {

	@Test
	void testUnsupportedSpeciesFIPStart() throws AbstractProjectionRequestException, IOException {
		var history = new History.Builder().build();
		var polygon = new Polygon.Builder().featureId(13919428).polygonNumber(13919428L).referenceYear(2024)
				.inventoryStandard(InventoryStandard.FIP).history(history).becZone("MS").build();
		var layer = new Layer.Builder().layerId("1").polygon(polygon).doSuppressPerHAYields(false)
				.crownClosure((short) 20).build();
		polygon.getLayers().put(layer.getLayerId(), layer);
		var sp64code = "PL";
		var stand = new Stand.Builder().layer(layer).sp0Code(VdypMethods.getVDYP7Species(sp64code)).build();
		var sp0 = new Species.Builder().stand(stand).speciesCode(VdypMethods.getVDYP7Species(sp64code))
				.speciesPercent(0).build();
		stand.addSpeciesGroup(sp0, layer.getSp0sAsSupplied().size());

		layer.addStand(stand);
		var sp64 = new Species.Builder().stand(stand).speciesCode(sp64code).speciesPercent(78.0).totalAge(8.0)
				.dominantHeight(10.0).build();
		stand.addSp64(sp64);
		layer.addSp64(sp64);

		var params = new Parameters().ageStart(0).ageEnd(190).progressFrequency(ProgressFrequency.FrequencyKind.POLYGON)
				.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.DO_ENABLE_PROGRESS_LOGGING);
		var context = new ProjectionContext(ProjectionRequestKind.HCSV, "TEST", params, false);
		layer.setAssignedProjectionType(ProjectionTypeCode.PRIMARY);
		polygon.doCompleteDefinition(context);
		var componentRunner = new RealComponentRunner();
		var unit = PolygonProjectionRunner.of(polygon, context, componentRunner);

		assertThrows(PolygonExecutionException.class, unit::project);

	}

	@Test
	void testTooYoungFIPStartFallthroughVRI() throws AbstractProjectionRequestException, IOException {
		var history = new History.Builder().build();
		var polygon = new Polygon.Builder().featureId(13919428).polygonNumber(13919428L).referenceYear(2024)
				.inventoryStandard(InventoryStandard.FIP).history(history).becZone("MS").build();
		var layer = new Layer.Builder().layerId("1").polygon(polygon).doSuppressPerHAYields(false)
				.crownClosure((short) 20).build();
		polygon.getLayers().put(layer.getLayerId(), layer);
		var sp64code = "PL";
		var stand = new Stand.Builder().layer(layer).sp0Code(VdypMethods.getVDYP7Species(sp64code)).build();
		var sp0 = new Species.Builder().stand(stand).speciesCode(VdypMethods.getVDYP7Species(sp64code))
				.speciesPercent(0).build();
		stand.addSpeciesGroup(sp0, layer.getSp0sAsSupplied().size());

		layer.addStand(stand);
		var sp64 = new Species.Builder().stand(stand).speciesCode(sp64code).speciesPercent(100.0).totalAge(8.0)
				.dominantHeight(1.0).build();
		stand.addSp64(sp64);
		layer.addSp64(sp64);

		var params = new Parameters().ageStart(0).ageEnd(190).progressFrequency(ProgressFrequency.FrequencyKind.POLYGON)
				.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.DO_ENABLE_PROGRESS_LOGGING);
		var context = new ProjectionContext(ProjectionRequestKind.HCSV, "TEST", params, false);
		layer.setAssignedProjectionType(ProjectionTypeCode.PRIMARY);
		polygon.doCompleteDefinition(context);
		var componentRunner = new RealComponentRunner();
		var unit = PolygonProjectionRunner.of(polygon, context, componentRunner);

	}

	@Test
	void testValidFIPStart() throws AbstractProjectionRequestException, IOException {
		var history = new History.Builder().build();
		var polygon = new Polygon.Builder().featureId(13919428).polygonNumber(13919428L).referenceYear(2024)
				.inventoryStandard(InventoryStandard.FIP).history(history).becZone("MS").build();
		var layer = new Layer.Builder().layerId("1").polygon(polygon).doSuppressPerHAYields(false)
				.crownClosure((short) 20).build();
		polygon.getLayers().put(layer.getLayerId(), layer);
		var sp64code = "PL";
		var stand = new Stand.Builder().layer(layer).sp0Code(VdypMethods.getVDYP7Species(sp64code)).build();
		var sp0 = new Species.Builder().stand(stand).speciesCode(VdypMethods.getVDYP7Species(sp64code))
				.speciesPercent(0).build();
		stand.addSpeciesGroup(sp0, layer.getSp0sAsSupplied().size());

		layer.addStand(stand);
		var sp64 = new Species.Builder().stand(stand).speciesCode(sp64code).speciesPercent(100.0).totalAge(8.0)
				.dominantHeight(10.0).build();
		stand.addSp64(sp64);
		layer.addSp64(sp64);

		var params = new Parameters().ageStart(0).ageEnd(190).progressFrequency(ProgressFrequency.FrequencyKind.POLYGON)
				.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.DO_ENABLE_PROGRESS_LOGGING);
		var context = new ProjectionContext(ProjectionRequestKind.HCSV, "TEST", params, false);
		layer.setAssignedProjectionType(ProjectionTypeCode.PRIMARY);
		polygon.doCompleteDefinition(context);
		var componentRunner = new RealComponentRunner();
		var unit = PolygonProjectionRunner.of(polygon, context, componentRunner);

	}
}
