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
import com.opencsv.exceptions.CsvConstraintViolationException;

import ca.bc.gov.nrs.api.helpers.TestHelper;
import ca.bc.gov.nrs.api.helpers.TestHelper.ValueOverride;
import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.PolygonValidationException;
import ca.bc.gov.nrs.vdyp.backend.projection.input.HcsvLineFilter;
import ca.bc.gov.nrs.vdyp.backend.projection.input.HcsvPolygonRecordBean;
import ca.bc.gov.nrs.vdyp.backend.projection.input.InventoryStandardCode;

public class HscvPolygonReaderTest {

	private static Logger logger = LoggerFactory.getLogger(HscvPolygonReaderTest.class);

	private final TestHelper testHelper = new TestHelper();

	@Test
	void testHscvPolygonReader() throws IOException {

		logger.info("Starting testHscvPolygonReader");

		Path resourceFolderPath = Path.of("VDYP7Console-sample-files", "hcsv", "vdyp-240");

		byte[] csvBytes = Files.readAllBytes(testHelper.getResourceFile(resourceFolderPath, "VDYP7_INPUT_POLY.csv"));

		var polygonCsvStream = HcsvPolygonRecordBean.createHcsvPolygonStream(new ByteArrayInputStream(csvBytes));

		Iterator<HcsvPolygonRecordBean> pi = polygonCsvStream.iterator();

		Assert.assertTrue(pi.hasNext());
		var polygon = pi.next();

		logger.info("Read polygon {}", polygon.getPolygonNumber());
		Assert.assertEquals(Long.valueOf(13919428), polygon.getPolyFeatureId());
		Assert.assertEquals("093C090", polygon.getMapId());
		Assert.assertEquals(Long.valueOf(94833422L), polygon.getPolygonNumber());
		Assert.assertEquals("DQU", polygon.getOrgUnit());
		Assert.assertEquals("UNK", polygon.getTsaName());
		Assert.assertEquals("UNK", polygon.getTflName());
		Assert.assertEquals(InventoryStandardCode.V, polygon.getInventoryStandardCode());
		Assert.assertEquals("UNK", polygon.getTsaNumber());
		Assert.assertEquals(Double.valueOf(0.6), polygon.getShrubHeight());
		Assert.assertEquals(Integer.valueOf(10), polygon.getShrubCrownClosure());
		Assert.assertEquals("3", polygon.getShrubCoverPattern());
		Assert.assertEquals("HE", polygon.getHerbCoverTypeCode());
		Assert.assertEquals(Integer.valueOf(35), polygon.getHerbCoverPercent());
		Assert.assertEquals("8", polygon.getHerbCoverPatternCode());
		Assert.assertEquals(null, polygon.getBryoidCoverPercent());
		Assert.assertEquals("MS", polygon.getBecZoneCode());
		Assert.assertEquals(Short.valueOf((short) 14), polygon.getCfsEcoZoneCode());
		Assert.assertEquals(Double.valueOf(50.0), polygon.getPercentStockable());
		Assert.assertEquals(Double.valueOf(1.0), polygon.getYieldFactor());
		Assert.assertEquals(null, polygon.getNonProductiveDescriptorCode());
		Assert.assertEquals("V", polygon.getBclcsLevel1Code());
		Assert.assertEquals("T", polygon.getBclcsLevel2Code());
		Assert.assertEquals("U", polygon.getBclcsLevel3Code());
		Assert.assertEquals("TC", polygon.getBclcsLevel4Code());
		Assert.assertEquals("SP", polygon.getBclcsLevel5Code());
		Assert.assertEquals(Integer.valueOf(2013), polygon.getReferenceYear());
		Assert.assertEquals(Integer.valueOf(2013), polygon.getYearOfDeath());
		Assert.assertEquals(Double.valueOf(60.0), polygon.getPercentDead());
		Assert.assertEquals(null, polygon.getNonVegCoverType1());
		Assert.assertEquals(null, polygon.getNonVegCoverPercent1());
		Assert.assertEquals(null, polygon.getNonVegCoverPattern1());
		Assert.assertEquals(null, polygon.getNonVegCoverType2());
		Assert.assertEquals(null, polygon.getNonVegCoverPercent2());
		Assert.assertEquals(null, polygon.getNonVegCoverPattern2());
		Assert.assertEquals(null, polygon.getNonVegCoverType3());
		Assert.assertEquals(null, polygon.getNonVegCoverPercent3());
		Assert.assertEquals(null, polygon.getNonVegCoverPattern3());
		Assert.assertEquals("TC", polygon.getLandCoverClassCode1());
		Assert.assertEquals(Integer.valueOf(100), polygon.getLandCoverPercent1());
		Assert.assertEquals(null, polygon.getLandCoverClassCode2());
		Assert.assertEquals(null, polygon.getLandCoverPercent2());
		Assert.assertEquals(null, polygon.getLandCoverClassCode3());
		Assert.assertEquals(null, polygon.getLandCoverPercent3());

		Assert.assertFalse(pi.hasNext());
	}

	@Test
	void testEmptyFile() {
		byte[] csvBytes = new String("").getBytes();

		var polygonCsvStream = HcsvPolygonRecordBean.createHcsvPolygonStream(new ByteArrayInputStream(csvBytes));

		Iterator<HcsvPolygonRecordBean> pi = polygonCsvStream.iterator();
		Assert.assertFalse(pi.hasNext());
	}

	@Test
	void testFileWithBlankLines() throws IOException {
		byte[] csvBytes = new String(" \n \n\n").getBytes();

		var polygonCsvStreamWithBlankLineFilter = new CsvToBeanBuilder<HcsvPolygonRecordBean>(
				new BufferedReader(new InputStreamReader(new ByteArrayInputStream(csvBytes)))
		) //
				.withSeparator(',') //
				.withType(HcsvPolygonRecordBean.class) //
				.withFilter(new HcsvLineFilter(true, false)) //
				.build();

		Iterator<HcsvPolygonRecordBean> pi1 = polygonCsvStreamWithBlankLineFilter.iterator();
		Assert.assertFalse(pi1.hasNext());
	}

	@Test
	void testFileWithBlankLinesAndNoBlankLineFilter() throws IOException {
		byte[] csvBytes = new String(" \n \n\n").getBytes();

		var polygonCsvStreamWithoutBlankLineFilter = new CsvToBeanBuilder<HcsvPolygonRecordBean>(
				new BufferedReader(new InputStreamReader(new ByteArrayInputStream(csvBytes)))
		).withSeparator(',').withType(HcsvPolygonRecordBean.class).build();

		Iterator<HcsvPolygonRecordBean> pi = polygonCsvStreamWithoutBlankLineFilter.iterator();

		Assert.assertTrue(pi.hasNext());
		pi.next();
		Assert.assertTrue(pi.hasNext());
		pi.next();
		Assert.assertTrue(pi.hasNext());
		pi.next();
		Assert.assertFalse(pi.hasNext());
	}

	@Test
	void testNumberFormatting() {
		var csvText = testHelper.buildPolygonCsvStream(new ValueOverride("percentStockable", Double.valueOf(20.32)));

		var polygonCsvStream = new CsvToBeanBuilder<HcsvPolygonRecordBean>(
				new BufferedReader(new InputStreamReader(new ByteArrayInputStream(csvText.getBytes())))
		) //
				.withSeparator(',') //
				.withType(HcsvPolygonRecordBean.class) //
				.withFilter(new HcsvLineFilter(false, true)) //
				.build();

		HcsvPolygonRecordBean p = polygonCsvStream.iterator().next();
		Assert.assertTrue(p.getPercentStockable() == 20.32d);
	}

	@Test
	void testEnumerations() {
		var csvText = testHelper.buildPolygonCsvStream(new ValueOverride("inventoryStandardCode", ""));

		var polygonCsvStream = new CsvToBeanBuilder<HcsvPolygonRecordBean>(
				new BufferedReader(new InputStreamReader(new ByteArrayInputStream(csvText.getBytes())))
		) //
				.withSeparator(',') //
				.withType(HcsvPolygonRecordBean.class) //
				.withFilter(new HcsvLineFilter(true, true)) //
				.build();

		HcsvPolygonRecordBean p = polygonCsvStream.iterator().next();
		Assert.assertTrue(p.getInventoryStandardCode() == null);
	}

	@Test
	void testEnumerationOutOfRange() {
		var csvText = testHelper.buildPolygonCsvStream(new ValueOverride("inventoryStandardCode", "Z"));

		var polygonCsvStream = HcsvPolygonRecordBean
				.createHcsvPolygonStream(new ByteArrayInputStream(csvText.getBytes()));

		try {
			polygonCsvStream.iterator().next();
			Assert.fail();
		} catch (RuntimeException e) {
			if (e.getCause() instanceof CsvConstraintViolationException cve) {
				if (cve.getSourceObject() instanceof PolygonValidationException pve) {
					var messages = pve.getValidationMessages();
					Assert.assertEquals(1, messages.size());
					var message = messages.get(0);
					Assert.assertEquals("Polygon 13919428: field Inventory Standard Code value \"Z\" is not a recognized value for this code", message.toString());
				} else {
					Assert.fail();
				}
			} else {
				Assert.fail();
			}
		}
	}

	@Test
	void testNotANumber() {
		var csvText = testHelper.buildPolygonCsvStream(new ValueOverride("shrubHeight", "not a number"));

		var polygonCsvStream = HcsvPolygonRecordBean
				.createHcsvPolygonStream(new ByteArrayInputStream(csvText.getBytes()));

		try {
			polygonCsvStream.iterator().next();
			Assert.fail();
		} catch (RuntimeException e) {
			if (e.getCause() instanceof CsvConstraintViolationException cve) {
				if (cve.getSourceObject() instanceof PolygonValidationException pve) {
					var messages = pve.getValidationMessages();
					Assert.assertEquals(1, messages.size());
					var message = messages.get(0);
					Assert.assertEquals("Polygon 13919428: field Shrub Height value \"not a number\" is not a number", message.toString());
				} else {
					Assert.fail();
				}
			} else {
				Assert.fail();
			}
		}
	}

	@Test
	void testNumberOutOfRange() {
		var csvText = testHelper.buildPolygonCsvStream(new ValueOverride("yieldFactor", "11.0"));

		var polygonCsvStream = HcsvPolygonRecordBean
				.createHcsvPolygonStream(new ByteArrayInputStream(csvText.getBytes()));

		try {
			polygonCsvStream.iterator().next();
			Assert.fail();
		} catch (RuntimeException e) {
			if (e.getCause() instanceof CsvConstraintViolationException cve) {
				if (cve.getSourceObject() instanceof PolygonValidationException pve) {
					var messages = pve.getValidationMessages();
					Assert.assertEquals(1, messages.size());
					var message = messages.get(0);
					Assert.assertEquals("Polygon 13919428: field Yield Factor value \"11.0\" is either not a number or out of range. Must be between 0 and 10, inclusive", message.toString());
				} else {
					Assert.fail();
				}
			} else {
				Assert.fail();
			}
		}
	}
}
