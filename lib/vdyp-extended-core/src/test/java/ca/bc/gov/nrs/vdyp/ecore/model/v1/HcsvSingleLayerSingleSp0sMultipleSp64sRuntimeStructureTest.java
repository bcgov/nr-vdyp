package ca.bc.gov.nrs.vdyp.ecore.model.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.api.helpers.TestHelper;
import ca.bc.gov.nrs.vdyp.common_calculators.enumerations.SiteIndexEquation;
import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.AbstractProjectionRequestException;
import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.PolygonValidationException;
import ca.bc.gov.nrs.vdyp.ecore.projection.ProjectionContext;
import ca.bc.gov.nrs.vdyp.ecore.projection.input.AbstractPolygonStream;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.Layer;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.SiteSpecies;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.Species;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.Stand;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.ProjectionTypeCode;
import ca.bc.gov.nrs.vdyp.ecore.utils.FileHelper;
import ca.bc.gov.nrs.vdyp.ecore.utils.ParameterNames;

class HcsvSingleLayerSingleSp0sMultipleSp64sRuntimeStructureTest {

	private static final Logger logger = LoggerFactory
			.getLogger(HcsvSingleLayerSingleSp0sMultipleSp64sRuntimeStructureTest.class);

	private TestHelper testHelper;

	@BeforeEach
	void setup() {
		testHelper = new TestHelper();
	}

	@Test
	void testHcsvSingleLayerMultipleSp0s()
			throws IOException, AbstractProjectionRequestException, PolygonValidationException {

		logger.info("Starting {}", this.getClass().getSimpleName());

		Path resourceFolderPath = Path
				.of(FileHelper.TEST_DATA_FILES, FileHelper.HCSV, "single-layer-single-sp0-multiple-sp64s-fip");

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

		String projectionId = "TEST";// ProjectionService.buildProjectionId(ProjectionRequestKind.HCSV);

		var parameters = new Parameters().ageStart(100).ageEnd(400);
		var context = new ProjectionContext(ProjectionRequestKind.HCSV, projectionId, parameters, false);

		AbstractPolygonStream polygonStream = AbstractPolygonStream.build(context, inputStreams);

		assertTrue(polygonStream.hasNextPolygon());

		var polygon = polygonStream.getNextPolygon();

		assertTrue(6993168 == polygon.getFeatureId());
		assertTrue(1 == polygon.getLayers().size());
		Layer layer = polygon.getLayers().get("1");
		assertNull(layer.getAdjustments());
		assertEquals(null, layer.getAgeAtDeath());
		assertEquals(ProjectionTypeCode.PRIMARY, layer.getAssignedProjectionType());
		assertNull(layer.getBasalArea());
		assertEquals(Short.valueOf((short) 50), layer.getCrownClosure());
		assertTrue(1 == layer.getSp0sAsSupplied().size());
		assertFalse(layer.getDoSuppressPerHAYields());
		assertNull(layer.getEstimatedSiteIndex());
		assertNull(layer.getEstimatedSiteIndexSpecies());
		assertEquals("1", layer.getLayerId());
		assertEquals(Double.valueOf(7.5), layer.getMeasuredUtilizationLevel());
		assertNull(layer.getNonForestDescriptor());
		assertNull(layer.getPercentStockable());
		assertEquals("1", layer.getRankCode());
		assertEquals(Double.valueOf(740), layer.getTreesPerHectare());
		assertNull(layer.getYearOfDeath());
		assertEquals(ProjectionTypeCode.PRIMARY, layer.getVdyp7LayerCode());

		assertEquals(1, layer.getSiteSpecies().size());

		Stand stand0 = layer.getSp0sAsSupplied().get(0);
		SiteSpecies siteSpecies0 = layer.getSiteSpecies().get(0);
		assertEquals(stand0.getSp0Code(), siteSpecies0.getStand().getSp0Code());
		assertTrue(0 == stand0.getStandIndex());

		assertTrue(100 == siteSpecies0.getTotalSpeciesPercent());
		assertTrue(siteSpecies0.getHasSiteInfo());
		assertFalse(siteSpecies0.getHasBeenCombined());
		assertEquals(stand0, siteSpecies0.getStand());

		Species sp0_0 = stand0.getSpeciesGroup();
		assertEquals(stand0, sp0_0.getStand());
		assertTrue(10.4 == sp0_0.getAgeAtBreastHeight());
		assertTrue(21.0 == sp0_0.getDominantHeight());
		assertTrue(0 == sp0_0.getNDuplicates());
		assertTrue(1 == sp0_0.getPercentsPerDuplicate().size());
		assertTrue(100 == sp0_0.getPercentsPerDuplicate().get(0));
		assertTrue(SiteIndexEquation.SI_FDI_THROWERAC == sp0_0.getSiteCurve());
		assertTrue(15.52 == sp0_0.getSiteIndex());
		assertEquals("F", sp0_0.getSpeciesCode());
		assertTrue(100 == sp0_0.getSpeciesPercent());
		assertTrue(21.0 == sp0_0.getSuppliedDominantHeight());
		assertTrue(15.52 == sp0_0.getSuppliedSiteIndex());
		assertTrue(90.0 == sp0_0.getSuppliedTotalAge());
		assertTrue(90.0 == sp0_0.getTotalAge());
		assertTrue(10.4 == sp0_0.getYearsToBreastHeight());

		assertEquals(2, stand0.getSpeciesByPercent().size());
		Species sp64_0_0 = stand0.getSpeciesByPercent().get(0);
		assertEquals(stand0, sp64_0_0.getStand());
		assertTrue(10.4 == sp64_0_0.getAgeAtBreastHeight());
		assertTrue(21.0 == sp64_0_0.getDominantHeight());
		assertTrue(0 == sp64_0_0.getNDuplicates());
		assertTrue(1 == sp64_0_0.getPercentsPerDuplicate().size());
		assertTrue(60 == sp64_0_0.getPercentsPerDuplicate().get(0));
		assertTrue(SiteIndexEquation.SI_FDI_THROWERAC == sp64_0_0.getSiteCurve());
		assertTrue(15.52 == sp64_0_0.getSiteIndex());
		assertEquals("FD", sp64_0_0.getSpeciesCode());
		assertTrue(60 == sp64_0_0.getSpeciesPercent());
		assertTrue(21.0 == sp64_0_0.getSuppliedDominantHeight());
		assertTrue(15.52 == sp64_0_0.getSuppliedSiteIndex());
		assertTrue(90.0 == sp64_0_0.getSuppliedTotalAge());
		assertTrue(90.0 == sp64_0_0.getTotalAge());
		assertTrue(10.4 == sp64_0_0.getYearsToBreastHeight());

		Species sp64_0_1 = stand0.getSpeciesByPercent().get(1);
		assertEquals(stand0, sp64_0_1.getStand());
		assertNull(sp64_0_1.getAgeAtBreastHeight());
		assertNull(sp64_0_1.getDominantHeight());
		assertTrue(0 == sp64_0_1.getNDuplicates());
		assertTrue(1 == sp64_0_1.getPercentsPerDuplicate().size());
		assertTrue(40 == sp64_0_1.getPercentsPerDuplicate().get(0));
		assertTrue(SiteIndexEquation.SI_FDI_THROWERAC == sp64_0_1.getSiteCurve());
		assertNull(sp64_0_1.getSiteIndex());
		assertEquals("XC", sp64_0_1.getSpeciesCode());
		assertTrue(40 == sp64_0_1.getSpeciesPercent());
		assertNull(sp64_0_1.getSuppliedDominantHeight());
		assertNull(sp64_0_1.getSuppliedSiteIndex());
		assertNull(sp64_0_1.getSuppliedTotalAge());
		assertNull(sp64_0_1.getTotalAge());
		assertNull(sp64_0_1.getYearsToBreastHeight());

		assertFalse(polygonStream.hasNextPolygon());
	}
}
