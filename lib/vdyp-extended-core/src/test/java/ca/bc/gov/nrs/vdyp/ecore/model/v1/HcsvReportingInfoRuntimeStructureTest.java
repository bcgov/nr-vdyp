package ca.bc.gov.nrs.vdyp.ecore.model.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.LongStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.api.helpers.TestHelper;
import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.AbstractProjectionRequestException;
import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.PolygonValidationException;
import ca.bc.gov.nrs.vdyp.ecore.projection.ProjectionContext;
import ca.bc.gov.nrs.vdyp.ecore.projection.input.AbstractPolygonStream;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.Layer;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.LayerReportingInfo;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.Species;
import ca.bc.gov.nrs.vdyp.ecore.utils.FileHelper;
import ca.bc.gov.nrs.vdyp.ecore.utils.ParameterNames;

class HcsvReportingInfoRuntimeStructureTest {

	private static final Logger logger = LoggerFactory.getLogger(HcsvReportingInfoRuntimeStructureTest.class);

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
				.of(FileHelper.TEST_DATA_FILES, FileHelper.HCSV, "single-layer-multiple-sp0s-fip");

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

		String projectionId = "TEST";// TODO ProjectionService.buildProjectionId(ProjectionRequestKind.HCSV);

		var parameters = new Parameters().ageStart(100).ageEnd(400);
		var context = new ProjectionContext(ProjectionRequestKind.HCSV, projectionId, parameters, false);

		AbstractPolygonStream polygonStream = AbstractPolygonStream.build(context, inputStreams);

		assertTrue(polygonStream.hasNextPolygon());

		var polygon = polygonStream.getNextPolygon();
		var polygonReportingInfo = polygon.getReportingInfo();

		assertEquals(polygon.getFeatureId(), polygonReportingInfo.getFeatureId());
		assertTrue(polygon.getPolygonNumber().equals(polygonReportingInfo.getPolygonNumber()));
		assertEquals(polygon.getDistrict(), polygonReportingInfo.getDistrict());
		assertEquals(polygon.getMapQuad(), polygonReportingInfo.getMapQuad());
		assertEquals(polygon.getMapSheet(), polygonReportingInfo.getMapSheet());
		assertEquals(polygon.getMapSubQuad(), polygonReportingInfo.getMapSubQuad());
		assertEquals(polygon.getNonProductiveDescriptor(), polygonReportingInfo.getNonProdDescriptor());
		assertEquals(polygon.getReferenceYear(), polygonReportingInfo.getReferenceYear());

		Set<Long> sourceLayerIdsNotSeen = new HashSet<>();
		LongStream.range(0, polygonReportingInfo.getLayerReportingInfos().size())
				.forEach(l -> sourceLayerIdsNotSeen.add(l));
		for (LayerReportingInfo layerReportingInfo : polygonReportingInfo.getLayerReportingInfos().values()) {
			Layer layer = polygon.getLayers().get(layerReportingInfo.getLayerID());

			assertEquals(layer.getLayerId(), layerReportingInfo.getLayerID());
			assertEquals(layer.getNonForestDescriptor(), layerReportingInfo.getNonForestDesc());
			assertEquals(layer.getRankCode(), layerReportingInfo.getRank());
			assertEquals(layer.getVdyp7LayerCode(), layerReportingInfo.getProcessedAsVDYP7Layer());

			sourceLayerIdsNotSeen.remove(Long.valueOf(layerReportingInfo.getSourceLayerID()));

			Map<String, Species> layerSpeciesByNameMap = new HashMap<>();
			layer.getSp64sAsSupplied().stream().forEach(s -> layerSpeciesByNameMap.put(s.getSpeciesCode(), s));
			for (var speciesReportingInfo : layerReportingInfo.getOrderedSpecies()) {
				Species species = layerSpeciesByNameMap.get(speciesReportingInfo.getSp64Name());

				assertNotNull(species);
				assertEquals(species.getSpeciesCode(), speciesReportingInfo.getSp64Name());
				assertTrue(species.getSpeciesPercent() == speciesReportingInfo.getSp64Percent());
			}
		}
		assertTrue(sourceLayerIdsNotSeen.size() == 0);

		assertFalse(polygonStream.hasNextPolygon());
	}
}
