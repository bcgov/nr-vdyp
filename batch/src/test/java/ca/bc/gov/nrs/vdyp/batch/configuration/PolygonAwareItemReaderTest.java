package ca.bc.gov.nrs.vdyp.batch.configuration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import ca.bc.gov.nrs.vdyp.batch.model.BatchRecord;
import ca.bc.gov.nrs.vdyp.batch.service.BatchMetricsCollector;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PolygonAwareItemReaderTest {

	@Mock
	private Resource polygonResource;

	@Mock
	private Resource layerResource;

	@Mock
	private BatchMetricsCollector metricsCollector;

	@Mock
	private StepExecution stepExecution;

	private PolygonAwareItemReader reader;

	@BeforeEach
	void setUp() {
		reader = new PolygonAwareItemReader(polygonResource, layerResource, metricsCollector);
	}

	@Test
	void testConstructor() {
		assertNotNull(reader);
	}

	@Test
	void testConstructorWithNullMetricsCollector() {
		assertDoesNotThrow(() -> {
			new PolygonAwareItemReader(polygonResource, layerResource, null);
		});
	}

	@Test
	void testBeforeStep() {
		Long jobExecutionId = 123L;
		ExecutionContext executionContext = new ExecutionContext();
		executionContext.putString("partitionName", "partition1");
		executionContext.putString("assignedFeatureIds", "1001,1002,1003");

		when(stepExecution.getJobExecutionId()).thenReturn(jobExecutionId);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);

		assertDoesNotThrow(() -> reader.beforeStep(stepExecution));
	}

	@Test
	void testBeforeStepWithEmptyFeatureIds() {
		Long jobExecutionId = 123L;
		ExecutionContext executionContext = new ExecutionContext();
		executionContext.putString("partitionName", "partition0");
		executionContext.putString("assignedFeatureIds", "");

		when(stepExecution.getJobExecutionId()).thenReturn(jobExecutionId);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);

		assertDoesNotThrow(() -> reader.beforeStep(stepExecution));
	}

	@Test
	void testBeforeStepWithDefaultValues() {
		Long jobExecutionId = 123L;
		ExecutionContext executionContext = new ExecutionContext();

		when(stepExecution.getJobExecutionId()).thenReturn(jobExecutionId);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);

		assertDoesNotThrow(() -> reader.beforeStep(stepExecution));
	}

	@Test
	void testReadWithoutOpen() {
		Exception exception = assertThrows(IllegalStateException.class, () -> reader.read());
		assertEquals("ItemReader not opened. Call open() first.", exception.getMessage());
	}

	@Test
	void testOpenAndReadSuccessfully() throws Exception {
		String polygonCsv = """
				featureId,mapId,polygonNumber,orgUnit,tsaName,tflName,inventoryStandardCode,tsaNumber,shrubHeight,shrubCrownClosure,shrubCoverPattern,herbCoverTypeCode,herbCoverPct,herbCoverPatternCode,bryoidCoverPct,becZoneCode,cfsEcozone,preDisturbanceStockability,yieldFactor,nonProductiveDescriptorCd,bclcsLevel1Code,bclcsLevel2Code,bclcsLevel3Code,bclcsLevel4Code,bclcsLevel5Code,photoEstimationBaseYear,referenceYear,pctDead,nonVegCoverType1,nonVegCoverPct1,nonVegCoverPattern1,nonVegCoverType2,nonVegCoverPct2,nonVegCoverPattern2,nonVegCoverType3,nonVegCoverPct3,nonVegCoverPattern3,landCoverClassCd1,landCoverPct1,landCoverClassCd2,landCoverPct2,landCoverClassCd3,landCoverPct3
				1001,MAP1,123,ORG1,TSA1,TFL1,I01,T01,1.5,50,P1,H01,25,HP1,10,BEC1,1,0.85,1.2,NPD1,BC1,BC2,BC3,BC4,BC5,2000,2020,5.0,NV1,15,NP1,NV2,20,NP2,NV3,25,NP3,LC1,30,LC2,35,LC3,40
				1002,MAP2,124,ORG2,TSA2,TFL2,I02,T02,2.0,60,P2,H02,30,HP2,15,BEC2,2,0.90,1.1,NPD2,BC1,BC2,BC3,BC4,BC5,2001,2021,6.0,NV1,18,NP1,NV2,22,NP2,NV3,28,NP3,LC1,32,LC2,37,LC3,42
				""";

		String layerCsv = """
				featureId,treeCoverLayerEstimatedId,mapId,polygonNumber,layerLevelCode,vdyp7LayerCd,layerStockability,forestCoverRankCode,nonForestDescriptorCode,estSiteIndexSpeciesCd,estimatedSiteIndex,crownClosure,basalArea75,stemsPerHa75,speciesCd1,speciesPct1,speciesCd2,speciesPct2,speciesCd3,speciesPct3,speciesCd4,speciesPct4,speciesCd5,speciesPct5,speciesCd6,speciesPct6,estAgeSpp1,estHeightSpp1,estAgeSpp2,estHeightSpp2,adjInd,loreyHeight75,basalArea125,wsVolPerHa75,wsVolPerHa125,cuVolPerHa125,dVolPerHa125,dwVolPerHa125
				1001,1,MAP1,123,P,VL1,Y,1,NF1,SP1,15.5,70,25.5,1200,FIR,60.0,HEM,30.0,BAL,10.0,SP4,0.0,SP5,0.0,SP6,0.0,50,20.5,45,18.2,ADJ1,22.1,30.2,350.5,400.8,380.2,50.3,25.1
				1001,2,MAP1,123,S,VL2,Y,2,NF2,SP2,12.8,45,18.3,800,HEM,80.0,FIR,20.0,SP3,0.0,SP4,0.0,SP5,0.0,SP6,0.0,40,15.2,35,12.8,ADJ2,16.8,22.5,250.3,280.5,260.1,35.8,18.2
				1002,3,MAP2,124,P,VL3,Y,1,NF3,SP3,18.2,75,28.7,1500,BAL,50.0,FIR,40.0,HEM,10.0,SP4,0.0,SP5,0.0,SP6,0.0,60,25.8,55,22.5,ADJ3,27.2,35.8,420.8,480.2,450.5,65.2,32.8
				""";

		setupStepExecution("1001,1002");
		setupMockResources(polygonCsv, layerCsv);

		ExecutionContext executionContext = new ExecutionContext();
		reader.open(executionContext);

		BatchRecord record1 = reader.read();
		assertNotNull(record1);
		assertEquals("1001", record1.getFeatureId());
		assertNotNull(record1.getPolygon());
		assertEquals("1001", record1.getPolygon().getFeatureId());
		assertEquals(2, record1.getLayers().size());

		BatchRecord record2 = reader.read();
		assertNotNull(record2);
		assertEquals("1002", record2.getFeatureId());
		assertNotNull(record2.getPolygon());
		assertEquals("1002", record2.getPolygon().getFeatureId());
		assertEquals(1, record2.getLayers().size());

		BatchRecord record3 = reader.read();
		assertNull(record3);

		reader.close();
	}

	@Test
	void testUpdate() throws Exception {
		setupStepExecution("1001");

		String polygonCsv = """
				featureId,mapId,polygonNumber,orgUnit,tsaName,tflName,inventoryStandardCode,tsaNumber,shrubHeight,shrubCrownClosure,shrubCoverPattern,herbCoverTypeCode,herbCoverPct,herbCoverPatternCode,bryoidCoverPct,becZoneCode,cfsEcozone,preDisturbanceStockability,yieldFactor,nonProductiveDescriptorCd,bclcsLevel1Code,bclcsLevel2Code,bclcsLevel3Code,bclcsLevel4Code,bclcsLevel5Code,photoEstimationBaseYear,referenceYear,pctDead,nonVegCoverType1,nonVegCoverPct1,nonVegCoverPattern1,nonVegCoverType2,nonVegCoverPct2,nonVegCoverPattern2,nonVegCoverType3,nonVegCoverPct3,nonVegCoverPattern3,landCoverClassCd1,landCoverPct1,landCoverClassCd2,landCoverPct2,landCoverClassCd3,landCoverPct3
				1001,MAP1,123,ORG1,TSA1,TFL1,I01,T01,1.5,50,P1,H01,25,HP1,10,BEC1,1,0.85,1.2,NPD1,BC1,BC2,BC3,BC4,BC5,2000,2020,5.0,NV1,15,NP1,NV2,20,NP2,NV3,25,NP3,LC1,30,LC2,35,LC3,40
				""";

		String layerCsv = """
				featureId,treeCoverLayerEstimatedId,mapId,polygonNumber,layerLevelCode,vdyp7LayerCd,layerStockability,forestCoverRankCode,nonForestDescriptorCode,estSiteIndexSpeciesCd,estimatedSiteIndex,crownClosure,basalArea75,stemsPerHa75,speciesCd1,speciesPct1,speciesCd2,speciesPct2,speciesCd3,speciesPct3,speciesCd4,speciesPct4,speciesCd5,speciesPct5,speciesCd6,speciesPct6,estAgeSpp1,estHeightSpp1,estAgeSpp2,estHeightSpp2,adjInd,loreyHeight75,basalArea125,wsVolPerHa75,wsVolPerHa125,cuVolPerHa125,dVolPerHa125,dwVolPerHa125
				1001,1,MAP1,123,P,VL1,Y,1,NF1,SP1,15.5,70,25.5,1200,FIR,60.0,HEM,30.0,BAL,10.0,SP4,0.0,SP5,0.0,SP6,0.0,50,20.5,45,18.2,ADJ1,22.1,30.2,350.5,400.8,380.2,50.3,25.1
				""";

		setupMockResources(polygonCsv, layerCsv);

		ExecutionContext executionContext = new ExecutionContext();
		reader.open(executionContext);
		reader.read();

		ExecutionContext updateContext = new ExecutionContext();
		reader.update(updateContext);

		assertTrue(updateContext.containsKey("partition1.processedCount"));
		assertTrue(updateContext.containsKey("partition1.skippedCount"));

		reader.close();
	}

	@Test
	void testReadWithEmptyData() throws Exception {
		String polygonCsv = """
				featureId,mapId,polygonNumber,orgUnit,tsaName,tflName,inventoryStandardCode,tsaNumber,shrubHeight,shrubCrownClosure,shrubCoverPattern,herbCoverTypeCode,herbCoverPct,herbCoverPatternCode,bryoidCoverPct,becZoneCode,cfsEcozone,preDisturbanceStockability,yieldFactor,nonProductiveDescriptorCd,bclcsLevel1Code,bclcsLevel2Code,bclcsLevel3Code,bclcsLevel4Code,bclcsLevel5Code,photoEstimationBaseYear,referenceYear,pctDead,nonVegCoverType1,nonVegCoverPct1,nonVegCoverPattern1,nonVegCoverType2,nonVegCoverPct2,nonVegCoverPattern2,nonVegCoverType3,nonVegCoverPct3,nonVegCoverPattern3,landCoverClassCd1,landCoverPct1,landCoverClassCd2,landCoverPct2,landCoverClassCd3,landCoverPct3
				""";

		String layerCsv = """
				featureId,treeCoverLayerEstimatedId,mapId,polygonNumber,layerLevelCode,vdyp7LayerCd,layerStockability,forestCoverRankCode,nonForestDescriptorCode,estSiteIndexSpeciesCd,estimatedSiteIndex,crownClosure,basalArea75,stemsPerHa75,speciesCd1,speciesPct1,speciesCd2,speciesPct2,speciesCd3,speciesPct3,speciesCd4,speciesPct4,speciesCd5,speciesPct5,speciesCd6,speciesPct6,estAgeSpp1,estHeightSpp1,estAgeSpp2,estHeightSpp2,adjInd,loreyHeight75,basalArea125,wsVolPerHa75,wsVolPerHa125,cuVolPerHa125,dVolPerHa125,dwVolPerHa125
				""";

		setupStepExecution("1001");
		setupMockResources(polygonCsv, layerCsv);

		ExecutionContext executionContext = new ExecutionContext();
		reader.open(executionContext);

		BatchRecord batchRecord = reader.read();
		assertNull(batchRecord);

		reader.close();
	}

	@Test
	void testOpenWithIOException() throws Exception {
		setupStepExecution("1001");
		lenient().when(polygonResource.getInputStream()).thenThrow(new IOException("File read error"));
		lenient().when(polygonResource.getDescription()).thenReturn("polygonResource");
		lenient().when(polygonResource.exists()).thenReturn(true);
		lenient().when(polygonResource.isReadable()).thenReturn(true);

		ExecutionContext executionContext = new ExecutionContext();

		ItemStreamException exception = assertThrows(ItemStreamException.class, () -> {
			reader.open(executionContext);
		});

		assertTrue(exception.getMessage().contains("PolygonAwareItemReader initialization"));
	}

	@Test
	void testOpenWithRuntimeException() throws Exception {
		setupStepExecution("1001");
		lenient().when(polygonResource.getInputStream()).thenThrow(new RuntimeException("Unexpected error"));
		lenient().when(polygonResource.getDescription()).thenReturn("polygonResource");
		lenient().when(polygonResource.exists()).thenReturn(true);
		lenient().when(polygonResource.isReadable()).thenReturn(true);

		ExecutionContext executionContext = new ExecutionContext();

		ItemStreamException exception = assertThrows(ItemStreamException.class, () -> {
			reader.open(executionContext);
		});

		assertTrue(exception.getMessage().contains("PolygonAwareItemReader initialization"));
	}

	@Test
	void testCloseWithNullReaders() {
		setupStepExecution("1001");

		assertDoesNotThrow(() -> reader.close());
	}

	@Test
	void testReadWithInvalidFeatureIds() throws Exception {
		String polygonCsv = """
				featureId,mapId,polygonNumber,orgUnit,tsaName,tflName,inventoryStandardCode,tsaNumber,shrubHeight,shrubCrownClosure,shrubCoverPattern,herbCoverTypeCode,herbCoverPct,herbCoverPatternCode,bryoidCoverPct,becZoneCode,cfsEcozone,preDisturbanceStockability,yieldFactor,nonProductiveDescriptorCd,bclcsLevel1Code,bclcsLevel2Code,bclcsLevel3Code,bclcsLevel4Code,bclcsLevel5Code,photoEstimationBaseYear,referenceYear,pctDead,nonVegCoverType1,nonVegCoverPct1,nonVegCoverPattern1,nonVegCoverType2,nonVegCoverPct2,nonVegCoverPattern2,nonVegCoverType3,nonVegCoverPct3,nonVegCoverPattern3,landCoverClassCd1,landCoverPct1,landCoverClassCd2,landCoverPct2,landCoverClassCd3,landCoverPct3
				invalid,MAP1,123,ORG1,TSA1,TFL1,I01,T01,1.5,50,P1,H01,25,HP1,10,BEC1,1,0.85,1.2,NPD1,BC1,BC2,BC3,BC4,BC5,2000,2020,5.0,NV1,15,NP1,NV2,20,NP2,NV3,25,NP3,LC1,30,LC2,35,LC3,40
				,MAP2,124,ORG2,TSA2,TFL2,I02,T02,2.0,60,P2,H02,30,HP2,15,BEC2,2,0.90,1.1,NPD2,BC1,BC2,BC3,BC4,BC5,2001,2021,6.0,NV1,18,NP1,NV2,22,NP2,NV3,28,NP3,LC1,32,LC2,37,LC3,42
				1001,MAP3,125,ORG3,TSA3,TFL3,I03,T03,1.8,55,P3,H03,28,HP3,12,BEC3,3,0.88,1.15,NPD3,BC1,BC2,BC3,BC4,BC5,2002,2022,7.0,NV1,16,NP1,NV2,21,NP2,NV3,26,NP3,LC1,31,LC2,36,LC3,41
				""";

		String layerCsv = """
				featureId,treeCoverLayerEstimatedId,mapId,polygonNumber,layerLevelCode,vdyp7LayerCd,layerStockability,forestCoverRankCode,nonForestDescriptorCode,estSiteIndexSpeciesCd,estimatedSiteIndex,crownClosure,basalArea75,stemsPerHa75,speciesCd1,speciesPct1,speciesCd2,speciesPct2,speciesCd3,speciesPct3,speciesCd4,speciesPct4,speciesCd5,speciesPct5,speciesCd6,speciesPct6,estAgeSpp1,estHeightSpp1,estAgeSpp2,estHeightSpp2,adjInd,loreyHeight75,basalArea125,wsVolPerHa75,wsVolPerHa125,cuVolPerHa125,dVolPerHa125,dwVolPerHa125
				invalid,1,MAP1,123,P,VL1,Y,1,NF1,SP1,15.5,70,25.5,1200,FIR,60.0,HEM,30.0,BAL,10.0,SP4,0.0,SP5,0.0,SP6,0.0,50,20.5,45,18.2,ADJ1,22.1,30.2,350.5,400.8,380.2,50.3,25.1
				,2,MAP2,124,S,VL2,Y,2,NF2,SP2,12.8,45,18.3,800,HEM,80.0,FIR,20.0,SP3,0.0,SP4,0.0,SP5,0.0,SP6,0.0,40,15.2,35,12.8,ADJ2,16.8,22.5,250.3,280.5,260.1,35.8,18.2
				1001,3,MAP3,125,P,VL3,Y,1,NF3,SP3,18.2,75,28.7,1500,BAL,50.0,FIR,40.0,HEM,10.0,SP4,0.0,SP5,0.0,SP6,0.0,60,25.8,55,22.5,ADJ3,27.2,35.8,420.8,480.2,450.5,65.2,32.8
				""";

		setupStepExecution("1001");
		setupMockResources(polygonCsv, layerCsv);

		ExecutionContext executionContext = new ExecutionContext();
		reader.open(executionContext);

		BatchRecord batchRecord = reader.read();
		assertNotNull(batchRecord);
		assertEquals("1001", batchRecord.getFeatureId());
		assertEquals(1, batchRecord.getLayers().size());

		BatchRecord record2 = reader.read();
		assertNull(record2);

		reader.close();
	}

	@Test
	void testParseAssignedFeatureIdsWithValidInput() {
		setupStepExecution("1001,1002,1003");

		ExecutionContext executionContext = new ExecutionContext();
		executionContext.putString("partitionName", "partition1");
		executionContext.putString("assignedFeatureIds", "1001,1002,1003");

		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(stepExecution.getJobExecutionId()).thenReturn(123L);

		assertDoesNotThrow(() -> reader.beforeStep(stepExecution));
	}

	@Test
	void testParseAssignedFeatureIdsWithWhitespace() {
		setupStepExecution(" 1001 , 1002 , 1003 ");

		ExecutionContext executionContext = new ExecutionContext();
		executionContext.putString("partitionName", "partition1");
		executionContext.putString("assignedFeatureIds", " 1001 , 1002 , 1003 ");

		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(stepExecution.getJobExecutionId()).thenReturn(123L);

		assertDoesNotThrow(() -> reader.beforeStep(stepExecution));
	}

	@Test
	void testUseClassPathResourcesForIntegration() throws Exception {
		Resource polygonResourceCP = new ClassPathResource("VDYP7_INPUT_POLY.csv");
		Resource layerResourceCP = new ClassPathResource("VDYP7_INPUT_LAYER.csv");

		PolygonAwareItemReader integrationReader = new PolygonAwareItemReader(
				polygonResourceCP, layerResourceCP, metricsCollector
		);

		Long jobExecutionId = 123L;
		ExecutionContext executionContext = new ExecutionContext();
		executionContext.putString("partitionName", "partition0");
		executionContext.putString("assignedFeatureIds", "1145678901");

		when(stepExecution.getJobExecutionId()).thenReturn(jobExecutionId);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);

		integrationReader.beforeStep(stepExecution);

		ExecutionContext openContext = new ExecutionContext();
		integrationReader.open(openContext);

		BatchRecord batchRecord = integrationReader.read();
		if (batchRecord != null) {
			assertNotNull(batchRecord.getFeatureId());
			assertNotNull(batchRecord.getPolygon());
		}

		integrationReader.close();
	}

	@Test
	void testReadWithSinglePolygonTestData() throws Exception {
		Resource polygonResourceCP = new ClassPathResource("test-data/hcsv/single-polygon/VDYP7_INPUT_POLY.csv");
		Resource layerResourceCP = new ClassPathResource("test-data/hcsv/single-polygon/VDYP7_INPUT_LAYER.csv");

		PolygonAwareItemReader integrationReader = new PolygonAwareItemReader(
				polygonResourceCP, layerResourceCP, metricsCollector
		);

		Long jobExecutionId = 123L;
		ExecutionContext executionContext = new ExecutionContext();
		executionContext.putString("partitionName", "partition0");
		executionContext.putString("assignedFeatureIds", "13919428");

		when(stepExecution.getJobExecutionId()).thenReturn(jobExecutionId);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);

		integrationReader.beforeStep(stepExecution);

		ExecutionContext openContext = new ExecutionContext();
		integrationReader.open(openContext);

		BatchRecord batchRecord = integrationReader.read();
		assertNotNull(batchRecord);
		assertEquals("13919428", batchRecord.getFeatureId());
		assertNotNull(batchRecord.getPolygon());
		assertEquals("13919428", batchRecord.getPolygon().getFeatureId());
		assertEquals("093C090", batchRecord.getPolygon().getMapId());
		assertEquals("MS", batchRecord.getPolygon().getBecZoneCode());
		assertEquals(3, batchRecord.getLayers().size());

		BatchRecord secondRecord = integrationReader.read();
		assertNull(secondRecord);

		integrationReader.close();
	}

	@Test
	void testReadWithMultiplePolygonTestData() throws Exception {
		Resource polygonResourceCP = new ClassPathResource("test-data/hcsv/multiple-polygon/VDYP7_INPUT_POLY.csv");
		Resource layerResourceCP = new ClassPathResource("test-data/hcsv/multiple-polygon/VDYP7_INPUT_LAYER.csv");

		PolygonAwareItemReader integrationReader = new PolygonAwareItemReader(
				polygonResourceCP, layerResourceCP, metricsCollector
		);

		Long jobExecutionId = 123L;
		ExecutionContext executionContext = new ExecutionContext();
		executionContext.putString("partitionName", "partition0");
		executionContext.putString("assignedFeatureIds", "17811434,17811435");

		when(stepExecution.getJobExecutionId()).thenReturn(jobExecutionId);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);

		integrationReader.beforeStep(stepExecution);

		ExecutionContext openContext = new ExecutionContext();
		integrationReader.open(openContext);

		BatchRecord firstRecord = integrationReader.read();
		assertNotNull(firstRecord);
		assertEquals("17811435", firstRecord.getFeatureId());
		assertEquals("093G045", firstRecord.getPolygon().getMapId());
		assertEquals("SBS", firstRecord.getPolygon().getBecZoneCode());
		assertEquals(2, firstRecord.getLayers().size());

		BatchRecord secondRecord = integrationReader.read();
		assertNotNull(secondRecord);
		assertEquals("17811434", secondRecord.getFeatureId());
		assertEquals("093G045", secondRecord.getPolygon().getMapId());
		assertEquals("SBS", secondRecord.getPolygon().getBecZoneCode());
		assertEquals(1, secondRecord.getLayers().size());

		BatchRecord thirdRecord = integrationReader.read();
		assertNull(thirdRecord);

		integrationReader.close();
	}

	@Test
	void testReadWithSinglePolygonPartialFeatureIds() throws Exception {
		Resource polygonResourceCP = new ClassPathResource("test-data/hcsv/multiple-polygon/VDYP7_INPUT_POLY.csv");
		Resource layerResourceCP = new ClassPathResource("test-data/hcsv/multiple-polygon/VDYP7_INPUT_LAYER.csv");

		PolygonAwareItemReader integrationReader = new PolygonAwareItemReader(
				polygonResourceCP, layerResourceCP, metricsCollector
		);

		Long jobExecutionId = 123L;
		ExecutionContext executionContext = new ExecutionContext();
		executionContext.putString("partitionName", "partition0");
		executionContext.putString("assignedFeatureIds", "17811434");

		when(stepExecution.getJobExecutionId()).thenReturn(jobExecutionId);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);

		integrationReader.beforeStep(stepExecution);

		ExecutionContext openContext = new ExecutionContext();
		integrationReader.open(openContext);

		BatchRecord firstRecord = integrationReader.read();
		assertNotNull(firstRecord);
		assertEquals("17811434", firstRecord.getFeatureId());
		assertEquals(1, firstRecord.getLayers().size());

		BatchRecord secondRecord = integrationReader.read();
		assertNull(secondRecord);

		integrationReader.close();
	}

	private void setupStepExecution(String assignedFeatureIds) {
		ExecutionContext executionContext = new ExecutionContext();
		executionContext.putString("partitionName", "partition1");
		executionContext.putString("assignedFeatureIds", assignedFeatureIds);

		when(stepExecution.getJobExecutionId()).thenReturn(123L);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);

		reader.beforeStep(stepExecution);
	}

	private void setupMockResources(String polygonCsv, String layerCsv) throws IOException {
		InputStream polygonStream = new ByteArrayInputStream(polygonCsv.getBytes());
		InputStream layerStream = new ByteArrayInputStream(layerCsv.getBytes());

		when(polygonResource.getInputStream()).thenReturn(polygonStream);
		when(layerResource.getInputStream()).thenReturn(layerStream);
		lenient().when(polygonResource.getDescription()).thenReturn("polygonResource");
		lenient().when(layerResource.getDescription()).thenReturn("layerResource");
		when(polygonResource.exists()).thenReturn(true);
		when(layerResource.exists()).thenReturn(true);
		lenient().when(polygonResource.isReadable()).thenReturn(true);
		lenient().when(layerResource.isReadable()).thenReturn(true);
	}
}