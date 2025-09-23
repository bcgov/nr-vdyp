package ca.bc.gov.nrs.vdyp.batch.service;

import ca.bc.gov.nrs.vdyp.batch.model.BatchRecord;
import ca.bc.gov.nrs.vdyp.batch.model.Layer;
import ca.bc.gov.nrs.vdyp.batch.model.Polygon;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters.ExecutionOption;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters.OutputFormat;
import ca.bc.gov.nrs.vdyp.ecore.projection.ProjectionRunner;
import ca.bc.gov.nrs.vdyp.ecore.projection.ProjectionContext;
import ca.bc.gov.nrs.vdyp.ecore.projection.ValidatedParameters;
import ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable.YieldTable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VdypProjectionServiceTest {

	@Mock
	private ProjectionRunner projectionRunner;

	@Mock
	private ProjectionContext projectionContext;

	@Mock
	private ValidatedParameters validatedParameters;

	@Mock
	private YieldTable yieldTable;

	@Mock
	private OutputFormat outputFormat;

	@Mock
	private InputStream yieldTableStream;

	@Mock
	private InputStream progressStream;

	@Mock
	private InputStream errorStream;

	private VdypProjectionService vdypProjectionService;

	@TempDir
	Path tempDir;

	private static final String FEATURE_ID = "123456789";
	private static final String PARTITION_NAME = "partition-0";

	@BeforeEach
	void setUp() {
		vdypProjectionService = new VdypProjectionService();
		ReflectionTestUtils.setField(vdypProjectionService, "outputBasePath", tempDir.toString());
	}

	@Test
	void testCreateInputStreamsFromBatchRecord() {
		BatchRecord batchRecord = createTestBatchRecordWithLayers();

		@SuppressWarnings("unchecked")
		Map<String, InputStream> streams = (Map<String, InputStream>) ReflectionTestUtils
				.invokeMethod(vdypProjectionService, "createInputStreamsFromBatchRecord", batchRecord);

		assertNotNull(streams);
		assertEquals(2, streams.size());
		assertTrue(streams.containsKey("HCSV-Polygon"));
		assertTrue(streams.containsKey("HCSV-Layers"));

		streams.values().forEach(stream -> {
			try {
				stream.close();
			} catch (Exception e) {
				/* ignore */ }
		});
	}

	@Test
	void testPolygonDataToCsvLine() {
		Polygon polygon = createTestPolygon();

		String csvLine = (String) ReflectionTestUtils
				.invokeMethod(vdypProjectionService, "polygonDataToCsvLine", polygon);

		assertNotNull(csvLine);
		assertTrue(csvLine.contains(FEATURE_ID));
		assertTrue(csvLine.contains("MAP123"));
		assertTrue(csvLine.contains("IDF"));
	}

	@Test
	void testLayerDataToCsvLine() {
		Layer layer = createTestLayer();

		String csvLine = (String) ReflectionTestUtils.invokeMethod(vdypProjectionService, "layerDataToCsvLine", layer);

		assertNotNull(csvLine);
		assertTrue(csvLine.contains(FEATURE_ID));
		assertTrue(csvLine.contains("P"));
		assertTrue(csvLine.contains("FD"));
	}

	@Test
	void testNvl_WithNullValue() {
		String result = (String) ReflectionTestUtils.invokeMethod(vdypProjectionService, "nvl", (Object) null);

		assertEquals("", result);
	}

	@Test
	void testNvl_WithStringValue() {
		String result = (String) ReflectionTestUtils.invokeMethod(vdypProjectionService, "nvl", "test");

		assertEquals("test", result);
	}

	@Test
	void testNvl_WithNumericValue() {
		String result = (String) ReflectionTestUtils.invokeMethod(vdypProjectionService, "nvl", 42);

		assertEquals("42", result);
	}

	@Test
	void testBuildProjectionId() {
		String projectionId = (String) ReflectionTestUtils
				.invokeMethod(vdypProjectionService, "buildProjectionId", PARTITION_NAME, FEATURE_ID);

		assertNotNull(projectionId);
		assertTrue(projectionId.contains("batch-projection"));
		assertTrue(projectionId.contains(PARTITION_NAME));
		assertTrue(projectionId.contains(FEATURE_ID));
	}

	@Test
	void testCreatePartitionOutputDir() {
		Path outputDir = (Path) ReflectionTestUtils
				.invokeMethod(vdypProjectionService, "createPartitionOutputDir", PARTITION_NAME);

		assertNotNull(outputDir);
		assertTrue(Files.exists(outputDir));
		assertTrue(outputDir.toString().contains(PARTITION_NAME));
	}

	@Test
	void testGetPolygonCsvHeader() {
		String header = (String) ReflectionTestUtils.invokeMethod(vdypProjectionService, "getPolygonCsvHeader");

		assertNotNull(header);
		assertTrue(header.contains("FEATURE_ID"));
		assertTrue(header.contains("MAP_ID"));
		assertTrue(header.contains("BEC_ZONE_CODE"));
	}

	@Test
	void testGetLayerCsvHeader() {
		String header = (String) ReflectionTestUtils.invokeMethod(vdypProjectionService, "getLayerCsvHeader");

		assertNotNull(header);
		assertTrue(header.contains("FEATURE_ID"));
		assertTrue(header.contains("LAYER_LEVEL_CODE"));
		assertTrue(header.contains("SPECIES_CD_1"));
	}

	@Test
	void testCreateInputStreamsFromBatchRecord_WithNullLayers() {
		BatchRecord batchRecord = createTestBatchRecord();
		batchRecord.setLayers(null);

		@SuppressWarnings("unchecked")
		Map<String, InputStream> streams = (Map<String, InputStream>) ReflectionTestUtils
				.invokeMethod(vdypProjectionService, "createInputStreamsFromBatchRecord", batchRecord);

		assertNotNull(streams);
		assertEquals(2, streams.size());
		assertTrue(streams.containsKey("HCSV-Polygon"));
		assertTrue(streams.containsKey("HCSV-Layers"));

		streams.values().forEach(stream -> {
			try {
				stream.close();
			} catch (Exception e) {
				/* ignore */ }
		});
	}

	@Test
	void testCreateInputStreamsFromBatchRecord_WithEmptyLayers() {
		BatchRecord batchRecord = createTestBatchRecord();
		batchRecord.setLayers(new ArrayList<>());

		@SuppressWarnings("unchecked")
		Map<String, InputStream> streams = (Map<String, InputStream>) ReflectionTestUtils
				.invokeMethod(vdypProjectionService, "createInputStreamsFromBatchRecord", batchRecord);

		assertNotNull(streams);
		assertEquals(2, streams.size());

		streams.values().forEach(stream -> {
			try {
				stream.close();
			} catch (Exception e) {
				/* ignore */ }
		});
	}

	@Test
	void testStoreIntermediateResults() throws Exception {
		setupMocksForStoreTests();
		Path partitionDir = tempDir.resolve(PARTITION_NAME);
		Files.createDirectories(partitionDir);

		ReflectionTestUtils.invokeMethod(
				vdypProjectionService, "storeIntermediateResults", projectionRunner, partitionDir, "test-projection",
				FEATURE_ID);

		verify(projectionRunner, atLeastOnce()).getContext();
	}

	@Test
	void testStoreYieldTables() throws Exception {
		setupMocksForStoreTests();
		Path partitionDir = tempDir.resolve(PARTITION_NAME);
		Files.createDirectories(partitionDir);

		ReflectionTestUtils
				.invokeMethod(vdypProjectionService, "storeYieldTables", projectionRunner, partitionDir, FEATURE_ID);

		verify(yieldTable).getAsStream();
		verify(yieldTable).getOutputFormat();
	}

	@Test
	void testStoreLogs_WithProgressLogging() throws Exception {
		setupMocksForStoreTests();
		when(validatedParameters.containsOption(ExecutionOption.DO_ENABLE_PROGRESS_LOGGING)).thenReturn(true);
		when(projectionRunner.getProgressStream()).thenReturn(progressStream);

		Path partitionDir = tempDir.resolve(PARTITION_NAME);
		Files.createDirectories(partitionDir);

		ReflectionTestUtils
				.invokeMethod(vdypProjectionService, "storeLogs", projectionRunner, partitionDir, FEATURE_ID);

		verify(projectionRunner).getProgressStream();
	}

	@Test
	void testStoreLogs_WithErrorLogging() throws Exception {
		setupMocksForStoreTests();
		when(validatedParameters.containsOption(ExecutionOption.DO_ENABLE_ERROR_LOGGING)).thenReturn(true);
		when(projectionRunner.getErrorStream()).thenReturn(errorStream);

		Path partitionDir = tempDir.resolve(PARTITION_NAME);
		Files.createDirectories(partitionDir);

		ReflectionTestUtils
				.invokeMethod(vdypProjectionService, "storeLogs", projectionRunner, partitionDir, FEATURE_ID);

		verify(projectionRunner).getErrorStream();
	}

	@Test
	void testStoreLogs_WithDebugLogging() throws Exception {
		setupMocksForStoreTests();
		when(validatedParameters.containsOption(ExecutionOption.DO_ENABLE_DEBUG_LOGGING)).thenReturn(true);

		Path partitionDir = tempDir.resolve(PARTITION_NAME);
		Files.createDirectories(partitionDir);

		ReflectionTestUtils
				.invokeMethod(vdypProjectionService, "storeLogs", projectionRunner, partitionDir, FEATURE_ID);

		Path debugLogPath = partitionDir.resolve("YieldTables_FEATURE_" + FEATURE_ID + "_DebugLog.txt");
		assertTrue(Files.exists(debugLogPath));
	}

	@Test
	void testStoreLogs_NoLoggingEnabled() throws Exception {
		setupMocksForStoreTests();
		when(validatedParameters.containsOption(any(ExecutionOption.class))).thenReturn(false);

		Path partitionDir = tempDir.resolve(PARTITION_NAME);
		Files.createDirectories(partitionDir);

		ReflectionTestUtils
				.invokeMethod(vdypProjectionService, "storeLogs", projectionRunner, partitionDir, FEATURE_ID);

		verify(projectionRunner, never()).getProgressStream();
		verify(projectionRunner, never()).getErrorStream();
	}

	@Test
	void testPolygonDataToCsvLine_WithNullValues() {
		Polygon polygon = new Polygon();
		polygon.setFeatureId(FEATURE_ID);

		String csvLine = (String) ReflectionTestUtils
				.invokeMethod(vdypProjectionService, "polygonDataToCsvLine", polygon);

		assertNotNull(csvLine);
		assertTrue(csvLine.contains(FEATURE_ID));
		assertTrue(csvLine.contains(",,"));
	}

	@Test
	void testLayerDataToCsvLine_WithNullValues() {
		Layer layer = new Layer();
		layer.setFeatureId(FEATURE_ID);

		String csvLine = (String) ReflectionTestUtils.invokeMethod(vdypProjectionService, "layerDataToCsvLine", layer);

		assertNotNull(csvLine);
		assertTrue(csvLine.contains(FEATURE_ID));
		assertTrue(csvLine.contains(",,"));
	}

	@Test
	void testNvl_WithDoubleValue() {
		String result = (String) ReflectionTestUtils.invokeMethod(vdypProjectionService, "nvl", 3.14159);
		assertEquals("3.14159", result);
	}

	@Test
	void testNvl_WithBooleanValue() {
		String result = (String) ReflectionTestUtils.invokeMethod(vdypProjectionService, "nvl", true);
		assertEquals("true", result);
	}

	@Test
	void testCreatePartitionOutputDir_CreatesNestedDirectories() {
		String nestedPartitionName = "nested-partition-test";

		Path outputDir = (Path) ReflectionTestUtils
				.invokeMethod(vdypProjectionService, "createPartitionOutputDir", nestedPartitionName);

		assertNotNull(outputDir);
		assertTrue(Files.exists(outputDir));
		assertTrue(outputDir.toString().contains(nestedPartitionName));
	}

	@Test
	void testPolygonDataToCsvLine_WithCompleteData() {
		Polygon polygon = createCompletePolygon();

		String csvLine = (String) ReflectionTestUtils
				.invokeMethod(vdypProjectionService, "polygonDataToCsvLine", polygon);

		assertNotNull(csvLine);
		assertTrue(csvLine.contains(FEATURE_ID));
		assertTrue(csvLine.contains("MAP123"));
		assertTrue(csvLine.contains("2020"));
		assertTrue(csvLine.contains("IDF"));
		assertTrue(csvLine.contains("25.5"));
		assertTrue(csvLine.contains("75"));
	}

	@Test
	void testLayerDataToCsvLine_WithCompleteData() {
		Layer layer = createCompleteLayer();

		String csvLine = (String) ReflectionTestUtils.invokeMethod(vdypProjectionService, "layerDataToCsvLine", layer);

		assertNotNull(csvLine);
		assertTrue(csvLine.contains(FEATURE_ID));
		assertTrue(csvLine.contains("P"));
		assertTrue(csvLine.contains("FD"));
		assertTrue(csvLine.contains("100.0"));
		assertTrue(csvLine.contains("50"));
		assertTrue(csvLine.contains("25.5"));
	}

	@Test
	void testCreateInputStreamsFromSinglePolygonTestData() {
		BatchRecord batchRecord = createBatchRecordFromSinglePolygonTestData();

		@SuppressWarnings("unchecked")
		Map<String, InputStream> streams = (Map<String, InputStream>) ReflectionTestUtils
				.invokeMethod(vdypProjectionService, "createInputStreamsFromBatchRecord", batchRecord);

		assertNotNull(streams);
		assertEquals(2, streams.size());
		assertTrue(streams.containsKey("HCSV-Polygon"));
		assertTrue(streams.containsKey("HCSV-Layers"));

		streams.values().forEach(stream -> {
			try {
				stream.close();
			} catch (Exception e) {
				/* ignore */ }
		});
	}

	@Test
	void testCreateInputStreamsFromMultiplePolygonTestData() {
		BatchRecord batchRecord = createBatchRecordFromMultiplePolygonTestData();

		@SuppressWarnings("unchecked")
		Map<String, InputStream> streams = (Map<String, InputStream>) ReflectionTestUtils
				.invokeMethod(vdypProjectionService, "createInputStreamsFromBatchRecord", batchRecord);

		assertNotNull(streams);
		assertEquals(2, streams.size());
		assertTrue(streams.containsKey("HCSV-Polygon"));
		assertTrue(streams.containsKey("HCSV-Layers"));

		streams.values().forEach(stream -> {
			try {
				stream.close();
			} catch (Exception e) {
				/* ignore */ }
		});
	}

	@Test
	void testPolygonDataToCsvLineWithTestDataValues() {
		Polygon polygon = createPolygonFromSinglePolygonTestData();

		String csvLine = (String) ReflectionTestUtils
				.invokeMethod(vdypProjectionService, "polygonDataToCsvLine", polygon);

		assertNotNull(csvLine);
		assertTrue(csvLine.contains("13919428"));
		assertTrue(csvLine.contains("093C090"));
		assertTrue(csvLine.contains("94833422"));
		assertTrue(csvLine.contains("DQU"));
		assertTrue(csvLine.contains("MS"));
		assertTrue(csvLine.contains("2013"));
	}

	@Test
	void testLayerDataToCsvLineWithTestDataValues() {
		Layer layer = createLayerFromSinglePolygonTestData();

		String csvLine = (String) ReflectionTestUtils.invokeMethod(vdypProjectionService, "layerDataToCsvLine", layer);

		assertNotNull(csvLine);
		assertTrue(csvLine.contains("13919428"));
		assertTrue(csvLine.contains("14321066"));
		assertTrue(csvLine.contains("093C090"));
		assertTrue(csvLine.contains("1"));
		assertTrue(csvLine.contains("P"));
		assertTrue(csvLine.contains("PLI"));
		assertTrue(csvLine.contains("60"));
		assertTrue(csvLine.contains("SX"));
		assertTrue(csvLine.contains("40"));
	}

	private void setupMocksForStoreTests() {
		when(projectionRunner.getContext()).thenReturn(projectionContext);
		when(projectionContext.getYieldTables()).thenReturn(List.of(yieldTable));
		when(projectionContext.getParams()).thenReturn(validatedParameters);

		when(yieldTable.getOutputFormat()).thenReturn(outputFormat);
		when(outputFormat.getYieldTableFileName()).thenReturn("test-yield-table.csv");
		when(yieldTable.getAsStream()).thenReturn(yieldTableStream);

		when(validatedParameters.containsOption(any(ExecutionOption.class))).thenReturn(false);
	}

	private Polygon createCompletePolygon() {
		Polygon polygon = new Polygon();
		polygon.setFeatureId(FEATURE_ID);
		polygon.setMapId("MAP123");
		polygon.setPolygonNumber(1L);
		polygon.setOrgUnit("ORG001");
		polygon.setTsaName("TSA_TEST");
		polygon.setTflName("TFL_TEST");
		polygon.setBecZoneCode("IDF");
		polygon.setTsaNumber("24");
		polygon.setInventoryStandardCode("V");
		polygon.setReferenceYear(2020);
		polygon.setShrubHeight(25.5);
		polygon.setShrubCrownClosure(75);
		polygon.setShrubCoverPattern("PATTERN");
		polygon.setHerbCoverTypeCode("HERB");
		polygon.setHerbCoverPct(30);
		polygon.setHerbCoverPatternCode("HERB_PATTERN");
		polygon.setBryoidCoverPct(10);
		polygon.setCfsEcozone(5);
		polygon.setPreDisturbanceStockability(0.8);
		polygon.setYieldFactor(1.2);
		polygon.setNonProductiveDescriptorCd("NP001");
		polygon.setBclcsLevel1Code("L1");
		polygon.setBclcsLevel2Code("L2");
		polygon.setBclcsLevel3Code("L3");
		polygon.setBclcsLevel4Code("L4");
		polygon.setBclcsLevel5Code("L5");
		polygon.setPhotoEstimationBaseYear(2015);
		polygon.setPctDead(5.0);
		polygon.setNonVegCoverType1("TYPE1");
		polygon.setNonVegCoverPct1(15);
		polygon.setNonVegCoverPattern1("PAT1");
		polygon.setNonVegCoverType2("TYPE2");
		polygon.setNonVegCoverPct2(20);
		polygon.setNonVegCoverPattern2("PAT2");
		polygon.setNonVegCoverType3("TYPE3");
		polygon.setNonVegCoverPct3(25);
		polygon.setNonVegCoverPattern3("PAT3");
		polygon.setLandCoverClassCd1("LC1");
		polygon.setLandCoverPct1(35);
		polygon.setLandCoverClassCd2("LC2");
		polygon.setLandCoverPct2(40);
		polygon.setLandCoverClassCd3("LC3");
		polygon.setLandCoverPct3(45);
		return polygon;
	}

	private Layer createCompleteLayer() {
		Layer layer = new Layer();
		layer.setFeatureId(FEATURE_ID);
		layer.setTreeCoverLayerEstimatedId(12345L);
		layer.setMapId("MAP123");
		layer.setPolygonNumber(1L);
		layer.setLayerLevelCode("P");
		layer.setVdyp7LayerCd("V7");
		layer.setLayerStockability("STOCK");
		layer.setForestCoverRankCode("RANK");
		layer.setNonForestDescriptorCode("NON_FOREST");
		layer.setEstSiteIndexSpeciesCd("FD");
		layer.setEstimatedSiteIndex(25.5);
		layer.setCrownClosure(75);
		layer.setBasalArea75(30.0);
		layer.setStemsPerHa75(1000);
		layer.setSpeciesCd1("FD");
		layer.setSpeciesPct1(100.0);
		layer.setSpeciesCd2("HW");
		layer.setSpeciesPct2(0.0);
		layer.setSpeciesCd3("CW");
		layer.setSpeciesPct3(0.0);
		layer.setSpeciesCd4("BA");
		layer.setSpeciesPct4(0.0);
		layer.setSpeciesCd5("PL");
		layer.setSpeciesPct5(0.0);
		layer.setSpeciesCd6("SX");
		layer.setSpeciesPct6(0.0);
		layer.setEstAgeSpp1(50);
		layer.setEstHeightSpp1(25.5);
		layer.setEstAgeSpp2(0);
		layer.setEstHeightSpp2(0.0);
		layer.setAdjInd("ADJ");
		layer.setLoreyHeight75(24.0);
		layer.setBasalArea125(35.0);
		layer.setWsVolPerHa75(300.0);
		layer.setWsVolPerHa125(350.0);
		layer.setCuVolPerHa125(320.0);
		layer.setDVolPerHa125(310.0);
		layer.setDwVolPerHa125(305.0);
		return layer;
	}

	private BatchRecord createTestBatchRecord() {
		BatchRecord batchRecord = new BatchRecord();
		batchRecord.setFeatureId(FEATURE_ID);
		batchRecord.setPolygon(createTestPolygon());
		return batchRecord;
	}

	private BatchRecord createTestBatchRecordWithLayers() {
		BatchRecord batchRecord = createTestBatchRecord();
		List<Layer> layers = new ArrayList<>();
		layers.add(createTestLayer());
		layers.add(createTestLayer());
		batchRecord.setLayers(layers);
		return batchRecord;
	}

	private Polygon createTestPolygon() {
		Polygon polygon = new Polygon();
		polygon.setFeatureId(FEATURE_ID);
		polygon.setMapId("MAP123");
		polygon.setPolygonNumber(1L);
		polygon.setBecZoneCode("IDF");
		polygon.setTsaNumber("24");
		polygon.setInventoryStandardCode("V");
		polygon.setReferenceYear(2020);
		return polygon;
	}

	private Layer createTestLayer() {
		Layer layer = new Layer();
		layer.setFeatureId(FEATURE_ID);
		layer.setLayerLevelCode("P");
		layer.setSpeciesCd1("FD");
		layer.setSpeciesPct1(100.0);
		layer.setEstAgeSpp1(50);
		layer.setEstHeightSpp1(25.5);
		layer.setCrownClosure(75);
		return layer;
	}

	private BatchRecord createBatchRecordFromSinglePolygonTestData() {
		BatchRecord batchRecord = new BatchRecord();
		batchRecord.setFeatureId("13919428");
		batchRecord.setPolygon(createPolygonFromSinglePolygonTestData());

		List<Layer> layers = new ArrayList<>();
		layers.add(createLayerFromSinglePolygonTestData());
		layers.add(createSecondaryLayerFromSinglePolygonTestData());
		layers.add(createDeadLayerFromSinglePolygonTestData());
		batchRecord.setLayers(layers);

		return batchRecord;
	}

	private BatchRecord createBatchRecordFromMultiplePolygonTestData() {
		BatchRecord batchRecord = new BatchRecord();
		batchRecord.setFeatureId("17811434");
		batchRecord.setPolygon(createPolygonFromMultiplePolygonTestData());

		List<Layer> layers = new ArrayList<>();
		layers.add(createLayerFromMultiplePolygonTestData());
		batchRecord.setLayers(layers);

		return batchRecord;
	}

	private Polygon createPolygonFromSinglePolygonTestData() {
		Polygon polygon = new Polygon();
		polygon.setFeatureId("13919428");
		polygon.setMapId("093C090");
		polygon.setPolygonNumber(94833422L);
		polygon.setOrgUnit("DQU");
		polygon.setTsaName("UNK");
		polygon.setTflName("UNK");
		polygon.setInventoryStandardCode("V");
		polygon.setTsaNumber("UNK");
		polygon.setShrubHeight(0.6);
		polygon.setShrubCrownClosure(10);
		polygon.setShrubCoverPattern("3");
		polygon.setHerbCoverTypeCode("HE");
		polygon.setHerbCoverPct(35);
		polygon.setHerbCoverPatternCode("8");
		polygon.setBecZoneCode("MS");
		polygon.setCfsEcozone(14);
		polygon.setPreDisturbanceStockability(50.0);
		polygon.setYieldFactor(1.000);
		polygon.setBclcsLevel1Code("V");
		polygon.setBclcsLevel2Code("T");
		polygon.setBclcsLevel3Code("U");
		polygon.setBclcsLevel4Code("TC");
		polygon.setBclcsLevel5Code("SP");
		polygon.setPhotoEstimationBaseYear(2013);
		polygon.setReferenceYear(2013);
		polygon.setPctDead(60.0);
		polygon.setLandCoverClassCd1("TC");
		polygon.setLandCoverPct1(100);
		return polygon;
	}

	private Polygon createPolygonFromMultiplePolygonTestData() {
		Polygon polygon = new Polygon();
		polygon.setFeatureId("17811434");
		polygon.setMapId("093G045");
		polygon.setPolygonNumber(42603189L);
		polygon.setOrgUnit("DPG");
		polygon.setTsaName("UNK");
		polygon.setTflName("UNK");
		polygon.setInventoryStandardCode("V");
		polygon.setTsaNumber("UNK");
		polygon.setShrubHeight(0.5);
		polygon.setShrubCrownClosure(15);
		polygon.setShrubCoverPattern("3");
		polygon.setHerbCoverTypeCode("HE");
		polygon.setHerbCoverPct(20);
		polygon.setHerbCoverPatternCode("6");
		polygon.setBecZoneCode("SBS");
		polygon.setCfsEcozone(14);
		polygon.setPreDisturbanceStockability(45.0);
		polygon.setYieldFactor(1.0);
		polygon.setBclcsLevel1Code("V");
		polygon.setBclcsLevel2Code("T");
		polygon.setBclcsLevel3Code("U");
		polygon.setBclcsLevel4Code("TC");
		polygon.setBclcsLevel5Code("SP");
		polygon.setPhotoEstimationBaseYear(2013);
		polygon.setReferenceYear(2013);
		polygon.setNonVegCoverType1("DW");
		polygon.setNonVegCoverPct1(10);
		polygon.setNonVegCoverPattern1("3");
		polygon.setLandCoverClassCd1("TC");
		polygon.setLandCoverPct1(100);
		return polygon;
	}

	private Layer createLayerFromSinglePolygonTestData() {
		Layer layer = new Layer();
		layer.setFeatureId("13919428");
		layer.setTreeCoverLayerEstimatedId(14321066L);
		layer.setMapId("093C090");
		layer.setPolygonNumber(94833422L);
		layer.setLayerLevelCode("1");
		layer.setVdyp7LayerCd("P");
		layer.setForestCoverRankCode("1");
		layer.setCrownClosure(20);
		layer.setBasalArea75(10.000010);
		layer.setStemsPerHa75(300);
		layer.setSpeciesCd1("PLI");
		layer.setSpeciesPct1(60.00);
		layer.setSpeciesCd2("SX");
		layer.setSpeciesPct2(40.00);
		layer.setEstAgeSpp1(180);
		layer.setEstHeightSpp1(18.00);
		layer.setEstAgeSpp2(180);
		layer.setEstHeightSpp2(23.00);
		return layer;
	}

	private Layer createSecondaryLayerFromSinglePolygonTestData() {
		Layer layer = new Layer();
		layer.setFeatureId("13919428");
		layer.setTreeCoverLayerEstimatedId(14321067L);
		layer.setMapId("093C090");
		layer.setPolygonNumber(94833422L);
		layer.setLayerLevelCode("2");
		layer.setVdyp7LayerCd("Y");
		layer.setCrownClosure(5);
		layer.setBasalArea75(1.000050);
		layer.setStemsPerHa75(150);
		layer.setSpeciesCd1("PLI");
		layer.setSpeciesPct1(100.00);
		layer.setEstAgeSpp1(60);
		layer.setEstHeightSpp1(9.00);
		return layer;
	}

	private Layer createDeadLayerFromSinglePolygonTestData() {
		Layer layer = new Layer();
		layer.setFeatureId("13919428");
		layer.setTreeCoverLayerEstimatedId(14321068L);
		layer.setMapId("093C090");
		layer.setPolygonNumber(94833422L);
		layer.setLayerLevelCode("D");
		layer.setVdyp7LayerCd("D");
		layer.setBasalArea75(14.999990);
		layer.setStemsPerHa75(500);
		layer.setSpeciesCd1("PLI");
		layer.setSpeciesPct1(100.00);
		layer.setEstAgeSpp1(170);
		layer.setEstHeightSpp1(18.00);
		return layer;
	}

	private Layer createLayerFromMultiplePolygonTestData() {
		Layer layer = new Layer();
		layer.setFeatureId("17811434");
		layer.setTreeCoverLayerEstimatedId(18584953L);
		layer.setMapId("093G045");
		layer.setPolygonNumber(42603189L);
		layer.setLayerLevelCode("1");
		layer.setVdyp7LayerCd("P");
		layer.setForestCoverRankCode("1");
		layer.setCrownClosure(25);
		layer.setBasalArea75(14.99999);
		layer.setStemsPerHa75(249);
		layer.setSpeciesCd1("SX");
		layer.setSpeciesPct1(95.0);
		layer.setSpeciesCd2("AT");
		layer.setSpeciesPct2(5.0);
		layer.setEstAgeSpp1(130);
		layer.setEstHeightSpp1(25.0);
		layer.setEstAgeSpp2(140);
		layer.setEstHeightSpp2(24.0);
		return layer;
	}
}
