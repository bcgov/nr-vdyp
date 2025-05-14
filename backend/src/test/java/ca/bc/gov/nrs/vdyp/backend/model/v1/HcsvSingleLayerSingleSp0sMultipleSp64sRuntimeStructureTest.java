package ca.bc.gov.nrs.vdyp.backend.model.v1;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.api.helpers.TestHelper;
import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.PolygonValidationException;
import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.ProjectionRequestException;
import ca.bc.gov.nrs.vdyp.backend.endpoints.v1.ParameterNames;
import ca.bc.gov.nrs.vdyp.backend.projection.ProjectionContext;
import ca.bc.gov.nrs.vdyp.backend.projection.input.AbstractPolygonStream;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Layer;
import ca.bc.gov.nrs.vdyp.backend.projection.model.SiteSpecies;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Species;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Stand;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.ProjectionTypeCode;
import ca.bc.gov.nrs.vdyp.backend.services.ProjectionService;
import ca.bc.gov.nrs.vdyp.common_calculators.enumerations.SiteIndexEquation;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class HcsvSingleLayerSingleSp0sMultipleSp64sRuntimeStructureTest {

	private static final Logger logger = LoggerFactory
			.getLogger(HcsvSingleLayerSingleSp0sMultipleSp64sRuntimeStructureTest.class);

	private final TestHelper testHelper;

	@Inject
	HcsvSingleLayerSingleSp0sMultipleSp64sRuntimeStructureTest(TestHelper testHelper) {
		this.testHelper = testHelper;
	}

	@BeforeEach
	void setup() {
	}

	@Test
	void testHcsvSingleLayerMultipleSp0s() throws IOException, ProjectionRequestException, PolygonValidationException {

		logger.info("Starting {}", this.getClass().getSimpleName());

		Path resourceFolderPath = Path
				.of("VDYP7Console-sample-files", "hcsv", "single-layer-single-sp0-multiple-sp64s-fip");

		Map<String, InputStream> inputStreams = new HashMap<>();

		{
			var polygonStream = new ByteArrayInputStream(
					Files.readAllBytes(testHelper.getResourceFile(resourceFolderPath, "VDYP7_INPUT_POLY_FIP.csv"))
			);
			var layersStream = new ByteArrayInputStream(
					Files.readAllBytes(testHelper.getResourceFile(resourceFolderPath, "VDYP7_INPUT_LAYER_FIP.csv"))
			);
			inputStreams.put(ParameterNames.HCSV_POLYGON_INPUT_DATA, polygonStream);
			inputStreams.put(ParameterNames.HCSV_LAYERS_INPUT_DATA, layersStream);
		}

		String projectionId = ProjectionService.buildId(ProjectionRequestKind.HCSV);

		var parameters = new Parameters().ageStart(100).ageEnd(400);
		var context = new ProjectionContext(ProjectionRequestKind.HCSV, projectionId, parameters, false);

		AbstractPolygonStream polygonStream = AbstractPolygonStream.build(context, inputStreams);

		Assert.assertTrue(polygonStream.hasNextPolygon());

		var polygon = polygonStream.getNextPolygon();

		Assert.assertTrue(6993168 == polygon.getFeatureId());
		Assert.assertTrue(1 == polygon.getLayers().size());
		Layer layer = polygon.getLayers().get("1");
		Assert.assertNull(layer.getAdjustments());
		Assert.assertEquals(null, layer.getAgeAtDeath());
		Assert.assertEquals(ProjectionTypeCode.PRIMARY, layer.getAssignedProjectionType());
		Assert.assertNull(layer.getBasalArea());
		Assert.assertEquals(Short.valueOf((short) 50), layer.getCrownClosure());
		Assert.assertTrue(1 == layer.getSp0sAsSupplied().size());
		Assert.assertFalse(layer.getDoSuppressPerHAYields());
		Assert.assertNull(layer.getEstimatedSiteIndex());
		Assert.assertNull(layer.getEstimatedSiteIndexSpecies());
		Assert.assertEquals("1", layer.getLayerId());
		Assert.assertEquals(Double.valueOf(7.5), layer.getMeasuredUtilizationLevel());
		Assert.assertNull(layer.getNonForestDescriptor());
		Assert.assertNull(layer.getPercentStockable());
		Assert.assertEquals("1", layer.getRankCode());
		Assert.assertEquals(Double.valueOf(740), layer.getTreesPerHectare());
		Assert.assertNull(layer.getYearOfDeath());
		Assert.assertEquals(ProjectionTypeCode.PRIMARY, layer.getVdyp7LayerCode());

		Assert.assertEquals(1, layer.getSiteSpecies().size());

		Stand stand0 = layer.getSp0sAsSupplied().get(0);
		SiteSpecies siteSpecies0 = layer.getSiteSpecies().get(0);
		Assert.assertEquals(stand0.getSp0Code(), siteSpecies0.getStand().getSp0Code());
		Assert.assertTrue(0 == stand0.getStandIndex());

		Assert.assertTrue(100 == siteSpecies0.getTotalSpeciesPercent());
		Assert.assertTrue(siteSpecies0.getHasSiteInfo());
		Assert.assertFalse(siteSpecies0.getHasBeenCombined());
		Assert.assertEquals(stand0, siteSpecies0.getStand());

		Species sp0_0 = stand0.getSpeciesGroup();
		Assert.assertEquals(stand0, sp0_0.getStand());
		Assert.assertTrue(10.4 == sp0_0.getAgeAtBreastHeight());
		Assert.assertTrue(21.0 == sp0_0.getDominantHeight());
		Assert.assertTrue(0 == sp0_0.getNDuplicates());
		Assert.assertTrue(1 == sp0_0.getPercentsPerDuplicate().size());
		Assert.assertTrue(100 == sp0_0.getPercentsPerDuplicate().get(0));
		Assert.assertTrue(SiteIndexEquation.SI_FDI_THROWERAC == sp0_0.getSiteCurve());
		Assert.assertTrue(15.52 == sp0_0.getSiteIndex());
		Assert.assertEquals("F", sp0_0.getSpeciesCode());
		Assert.assertTrue(100 == sp0_0.getSpeciesPercent());
		Assert.assertTrue(21.0 == sp0_0.getSuppliedDominantHeight());
		Assert.assertTrue(15.52 == sp0_0.getSuppliedSiteIndex());
		Assert.assertTrue(90.0 == sp0_0.getSuppliedTotalAge());
		Assert.assertTrue(90.0 == sp0_0.getTotalAge());
		Assert.assertTrue(10.4 == sp0_0.getYearsToBreastHeight());

		Assert.assertEquals(2, stand0.getSpecies().size());
		Species sp64_0_0 = stand0.getSpecies().get(0);
		Assert.assertEquals(stand0, sp64_0_0.getStand());
		Assert.assertTrue(10.4 == sp64_0_0.getAgeAtBreastHeight());
		Assert.assertTrue(21.0 == sp64_0_0.getDominantHeight());
		Assert.assertTrue(0 == sp64_0_0.getNDuplicates());
		Assert.assertTrue(1 == sp64_0_0.getPercentsPerDuplicate().size());
		Assert.assertTrue(60 == sp64_0_0.getPercentsPerDuplicate().get(0));
		Assert.assertTrue(SiteIndexEquation.SI_FDI_THROWERAC == sp64_0_0.getSiteCurve());
		Assert.assertTrue(15.52 == sp64_0_0.getSiteIndex());
		Assert.assertEquals("FD", sp64_0_0.getSpeciesCode());
		Assert.assertTrue(60 == sp64_0_0.getSpeciesPercent());
		Assert.assertTrue(21.0 == sp64_0_0.getSuppliedDominantHeight());
		Assert.assertTrue(15.52 == sp64_0_0.getSuppliedSiteIndex());
		Assert.assertTrue(90.0 == sp64_0_0.getSuppliedTotalAge());
		Assert.assertTrue(90.0 == sp64_0_0.getTotalAge());
		Assert.assertTrue(10.4 == sp64_0_0.getYearsToBreastHeight());

		Species sp64_0_1 = stand0.getSpecies().get(1);
		Assert.assertEquals(stand0, sp64_0_1.getStand());
		Assert.assertNull(sp64_0_1.getAgeAtBreastHeight());
		Assert.assertNull(sp64_0_1.getDominantHeight());
		Assert.assertTrue(0 == sp64_0_1.getNDuplicates());
		Assert.assertTrue(1 == sp64_0_1.getPercentsPerDuplicate().size());
		Assert.assertTrue(40 == sp64_0_1.getPercentsPerDuplicate().get(0));
		Assert.assertTrue(SiteIndexEquation.SI_FDI_THROWERAC == sp64_0_1.getSiteCurve());
		Assert.assertNull(sp64_0_1.getSiteIndex());
		Assert.assertEquals("XC", sp64_0_1.getSpeciesCode());
		Assert.assertTrue(40 == sp64_0_1.getSpeciesPercent());
		Assert.assertNull(sp64_0_1.getSuppliedDominantHeight());
		Assert.assertNull(sp64_0_1.getSuppliedSiteIndex());
		Assert.assertNull(sp64_0_1.getSuppliedTotalAge());
		Assert.assertNull(sp64_0_1.getTotalAge());
		Assert.assertNull(sp64_0_1.getYearsToBreastHeight());

		Assert.assertFalse(polygonStream.hasNextPolygon());
	}
}
