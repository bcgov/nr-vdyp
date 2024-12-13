package ca.bc.gov.nrs.vdyp.backend.endpoints.projection.input;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencsv.bean.CsvToBeanBuilder;

import ca.bc.gov.nrs.api.helpers.TestHelper;
import ca.bc.gov.nrs.vdyp.backend.projection.input.HcsvLayerRecordBean;

public class HscvLayerReaderTest {

	private static Logger logger = LoggerFactory.getLogger(HscvLayerReaderTest.class);

	private TestHelper testHelper = new TestHelper();
	
	@Test
	void testHscvLayerReader() throws IOException {

		logger.info("Starting testHscvLayerReader");

		Path resourceFolderPath = Path.of("VDYP7Console-sample-files", "hcsv", "vdyp-240");

		byte[] csvBytes = Files.readAllBytes(testHelper.getResourceFile(resourceFolderPath, "VDYP7_INPUT_LAYER.csv"));
		
		var layerCsvStream = new CsvToBeanBuilder<HcsvLayerRecordBean>(
				new BufferedReader(
						new InputStreamReader(
								new ByteArrayInputStream(
										csvBytes))))
				.withSeparator(',')
                .withType(HcsvLayerRecordBean.class)
                .build();
		
		Iterator<HcsvLayerRecordBean> li = layerCsvStream.iterator();
		Assert.assertTrue(li.hasNext());
		
		var l1 = li.next();
		logger.info("Read layer {}", l1.getLayerFeatureId() + ' ' + l1.getLayerLevelCode());

		Assert.assertEquals(null, l1.getAdjustmentIndicatorInd());
		Assert.assertEquals(null, l1.getBasalArea125());
		Assert.assertEquals(null, l1.getCloseUtilizationVolumeLessDecayAndWastagePerHectare125());
		Assert.assertEquals(null, l1.getCloseUtilizationVolumeLessDecayPerHectare125());
		Assert.assertEquals(null, l1.getCloseUtilizationVolumePerHectare125());
		Assert.assertEquals(Short.valueOf((short)5), l1.getCrownClosure());
		Assert.assertEquals(Short.valueOf((short)60), l1.getEstimatedAgeSpp1());
		Assert.assertEquals(null, l1.getEstimatedAgeSpp2());
		Assert.assertEquals(null, l1.getEstimatedHeightSpp1());
		Assert.assertEquals(null, l1.getEstimatedAgeSpp2());
		Assert.assertEquals(null, l1.getEstimatedHeightSpp1());
		Assert.assertEquals(null, l1.getEstimatedHeightSpp2());
		Assert.assertEquals(null, l1.getEstimatedSiteIndex());
		Assert.assertEquals(null, l1.getEstimatedSiteIndexSpeciesCode());
		Assert.assertEquals(null, l1.getForestCoverRankCode());
		Assert.assertEquals("13919428", l1.getLayerFeatureId());
		Assert.assertEquals("2", l1.getLayerLevelCode());
		Assert.assertEquals("093C090", l1.getLayerMapId());
		Assert.assertEquals(null, l1.getLayerStockability());
		Assert.assertEquals(null, l1.getLoreyHeight75());
		Assert.assertEquals(null, l1.getNonForestDescriptorCode());
		Assert.assertEquals(Long.valueOf(94833422), l1.getPolygonNumber());
		Assert.assertEquals("PLI", l1.getSpeciesCode1());
		Assert.assertEquals(null, l1.getSpeciesCode2());
		Assert.assertEquals(null, l1.getSpeciesCode3());
		Assert.assertEquals(null, l1.getSpeciesCode4());
		Assert.assertEquals(null, l1.getSpeciesCode5());
		Assert.assertEquals(null, l1.getSpeciesCode6());
		Assert.assertEquals(Double.valueOf(100.0d), l1.getSpeciesPercent1());
		Assert.assertEquals(null, l1.getSpeciesPercent2());
		Assert.assertEquals(null, l1.getSpeciesPercent3());
		Assert.assertEquals(null, l1.getSpeciesPercent4());
		Assert.assertEquals(null, l1.getSpeciesPercent5());
		Assert.assertEquals(null, l1.getSpeciesPercent6());
		Assert.assertEquals(Double.valueOf(150.0d), l1.getStemsPerHectare());
		Assert.assertEquals("14321067", l1.getTreeCoverId());
		Assert.assertEquals("Y", l1.getVdyp7LayerLevelCode());
		Assert.assertEquals(null, l1.getWholeStemVolumePerHectare125());
		Assert.assertEquals(null, l1.getWholeStemVolumePerHectare75());

		var l2 = li.next();
		logger.info("Read layer {}", l2.getLayerFeatureId() + ' ' + l2.getLayerLevelCode());
		var l3 = li.next();
		logger.info("Read layer {}", l3.getLayerFeatureId() + ' ' + l3.getLayerLevelCode());
		Assert.assertFalse(li.hasNext());
	}
}
