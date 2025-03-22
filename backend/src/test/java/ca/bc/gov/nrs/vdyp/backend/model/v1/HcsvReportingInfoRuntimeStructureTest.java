package ca.bc.gov.nrs.vdyp.backend.model.v1;

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

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.api.helpers.TestHelper;
import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.PolygonValidationException;
import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.AbstractProjectionRequestException;
import ca.bc.gov.nrs.vdyp.backend.endpoints.v1.ParameterNames;
import ca.bc.gov.nrs.vdyp.backend.projection.ProjectionContext;
import ca.bc.gov.nrs.vdyp.backend.projection.input.AbstractPolygonStream;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Layer;
import ca.bc.gov.nrs.vdyp.backend.projection.model.LayerReportingInfo;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Species;
import ca.bc.gov.nrs.vdyp.backend.services.ProjectionService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class HcsvReportingInfoRuntimeStructureTest {

	private static final Logger logger = LoggerFactory.getLogger(HcsvReportingInfoRuntimeStructureTest.class);

	private final TestHelper testHelper;

	@Inject
	HcsvReportingInfoRuntimeStructureTest(TestHelper testHelper) {
		this.testHelper = testHelper;
	}

	@BeforeEach
	void setup() {
	}

	@Test
	void testHcsvSingleLayerMultipleSp0s()
			throws IOException, AbstractProjectionRequestException, PolygonValidationException {

		logger.info("Starting {}", this.getClass().getSimpleName());

		Path resourceFolderPath = Path.of("test-data-files", "hcsv", "single-layer-multiple-sp0s-fip");

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
		var polygonReportingInfo = polygon.getReportingInfo();

		Assert.assertEquals(polygon.getFeatureId(), polygonReportingInfo.getFeatureId());
		Assert.assertTrue(polygon.getPolygonNumber().equals(polygonReportingInfo.getPolygonNumber()));
		Assert.assertEquals(polygon.getDistrict(), polygonReportingInfo.getDistrict());
		Assert.assertEquals(polygon.getMapQuad(), polygonReportingInfo.getMapQuad());
		Assert.assertEquals(polygon.getMapSheet(), polygonReportingInfo.getMapSheet());
		Assert.assertEquals(polygon.getMapSubQuad(), polygonReportingInfo.getMapSubQuad());
		Assert.assertEquals(polygon.getNonProductiveDescriptor(), polygonReportingInfo.getNonProdDescriptor());
		Assert.assertEquals(polygon.getReferenceYear(), polygonReportingInfo.getReferenceYear());

		Set<Long> sourceLayerIdsNotSeen = new HashSet<>();
		LongStream.range(0, polygonReportingInfo.getLayerReportingInfos().size())
				.forEach(l -> sourceLayerIdsNotSeen.add(l));
		for (LayerReportingInfo layerReportingInfo : polygonReportingInfo.getLayerReportingInfos().values()) {
			Layer layer = polygon.getLayers().get(layerReportingInfo.getLayerID());

			Assert.assertEquals(layer.getLayerId(), layerReportingInfo.getLayerID());
			Assert.assertEquals(layer.getNonForestDescriptor(), layerReportingInfo.getNonForestDesc());
			Assert.assertEquals(layer.getRankCode(), layerReportingInfo.getRank());
			Assert.assertEquals(layer.getVdyp7LayerCode(), layerReportingInfo.getProcessedAsVDYP7Layer());

			sourceLayerIdsNotSeen.remove(Long.valueOf(layerReportingInfo.getSourceLayerID()));

			Map<String, Species> layerSpeciesByNameMap = new HashMap<>();
			layer.getSp64sAsSupplied().stream().forEach(s -> layerSpeciesByNameMap.put(s.getSpeciesCode(), s));
			for (var speciesReportingInfo : layerReportingInfo.getOrderedSpecies()) {
				Species species = layerSpeciesByNameMap.get(speciesReportingInfo.getSp64Name());

				Assert.assertNotNull(species);
				Assert.assertEquals(species.getSpeciesCode(), speciesReportingInfo.getSp64Name());
				Assert.assertTrue(species.getSpeciesPercent() == speciesReportingInfo.getSp64Percent());
			}
		}
		Assert.assertTrue(sourceLayerIdsNotSeen.size() == 0);

		Assert.assertFalse(polygonStream.hasNextPolygon());
	}
}
