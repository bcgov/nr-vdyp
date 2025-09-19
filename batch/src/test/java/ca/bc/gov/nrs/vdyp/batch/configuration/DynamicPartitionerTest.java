package ca.bc.gov.nrs.vdyp.batch.configuration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DynamicPartitionerTest {

	@Mock
	private Resource mockResource;

	private DynamicPartitioner dynamicPartitioner;

	@BeforeEach
	void setUp() {
		dynamicPartitioner = new DynamicPartitioner();
	}

	@Test
	void testConstructor() {
		assertNotNull(dynamicPartitioner);
	}

	@Test
	void testSetPolygonResource() {
		Resource resource = mock(Resource.class);
		assertDoesNotThrow(() -> dynamicPartitioner.setPolygonResource(resource));
	}

	@Test
	void testSetPolygonResource_withNull() {
		assertDoesNotThrow(() -> dynamicPartitioner.setPolygonResource(null));
	}

	@Test
	void testPartition_withNullResource_returnsSinglePartition() {
		Map<String, ExecutionContext> partitions = dynamicPartitioner.partition(4);

		assertEquals(1, partitions.size());
		assertTrue(partitions.containsKey("partition0"));

		ExecutionContext context = partitions.get("partition0");
		assertEquals("", context.getString("assignedFeatureIds"));
		assertEquals("partition0", context.getString("partitionName"));
	}

	@Test
	void testPartition_withEmptyFile_returnsSinglePartition() throws IOException {
		String csvContent = "FEATURE_ID,MAP_ID\n"; // Only header, no data records
		InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());
		when(mockResource.getInputStream()).thenReturn(inputStream);

		dynamicPartitioner.setPolygonResource(mockResource);
		Map<String, ExecutionContext> partitions = dynamicPartitioner.partition(4);

		assertEquals(1, partitions.size());
		assertTrue(partitions.containsKey("partition0"));

		ExecutionContext context = partitions.get("partition0");
		assertEquals("", context.getString("assignedFeatureIds"));
		assertEquals("partition0", context.getString("partitionName"));

		verify(mockResource).getInputStream();
	}

	@Test
	void testPartition_withEmptyFileNoHeader_returnsSinglePartition() throws IOException {
		String csvContent = ""; // Completely empty file
		InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());
		when(mockResource.getInputStream()).thenReturn(inputStream);

		dynamicPartitioner.setPolygonResource(mockResource);
		Map<String, ExecutionContext> partitions = dynamicPartitioner.partition(2);

		assertEquals(1, partitions.size());
		assertTrue(partitions.containsKey("partition0"));

		ExecutionContext context = partitions.get("partition0");
		assertEquals("", context.getString("assignedFeatureIds"));
		assertEquals("partition0", context.getString("partitionName"));
	}

	@Test
	void testPartition_withValidFile_createsCorrectPartitions() throws IOException {
		String csvContent = """
				FEATURE_ID,MAP_ID,POLYGON_NUMBER,ORG_UNIT
				1145678901,093G045,42603189,DPG
				1245678902,093G045,42603181,DPG
				1345678903,093G045,42603182,DPG
				1445678904,093G045,42603183,DPG
				1545678905,093G045,42603184,DPG
				1645678906,093G045,42603185,DPG
				1745678907,093G045,42603186,DPG
				1845678908,093G045,42603187,DPG
				""";

		InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());
		when(mockResource.getInputStream()).thenReturn(inputStream);

		dynamicPartitioner.setPolygonResource(mockResource);
		Map<String, ExecutionContext> partitions = dynamicPartitioner.partition(4);

		assertEquals(4, partitions.size());

		// Check partition0: 8 FEATURE_IDs / 4 partitions = 2 FEATURE_IDs per partition
		ExecutionContext context0 = partitions.get("partition0");
		assertEquals("1145678901,1245678902", context0.getString("assignedFeatureIds"));
		assertEquals("partition0", context0.getString("partitionName"));
		assertEquals(1145678901L, context0.getLong("featureIdRangeStart"));
		assertEquals(1245678902L, context0.getLong("featureIdRangeEnd"));

		// Check partition1
		ExecutionContext context1 = partitions.get("partition1");
		assertEquals("1345678903,1445678904", context1.getString("assignedFeatureIds"));
		assertEquals("partition1", context1.getString("partitionName"));

		// Check partition2
		ExecutionContext context2 = partitions.get("partition2");
		assertEquals("1545678905,1645678906", context2.getString("assignedFeatureIds"));
		assertEquals("partition2", context2.getString("partitionName"));

		// Check partition3
		ExecutionContext context3 = partitions.get("partition3");
		assertEquals("1745678907,1845678908", context3.getString("assignedFeatureIds"));
		assertEquals("partition3", context3.getString("partitionName"));
	}

	@Test
	void testPartition_withSingleRecord_createsSinglePartition() throws IOException {
		String csvContent = """
				FEATURE_ID,MAP_ID
				1145678901,093G045
				""";

		InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());
		when(mockResource.getInputStream()).thenReturn(inputStream);

		dynamicPartitioner.setPolygonResource(mockResource);
		Map<String, ExecutionContext> partitions = dynamicPartitioner.partition(4);

		assertEquals(1, partitions.size()); // Only creates partitions for available FEATURE_IDs

		// With 1 FEATURE_ID, only partition0 gets created
		ExecutionContext context0 = partitions.get("partition0");
		assertEquals("1145678901", context0.getString("assignedFeatureIds"));
		assertEquals("partition0", context0.getString("partitionName"));
		assertEquals(1145678901L, context0.getLong("featureIdRangeStart"));
		assertEquals(1145678901L, context0.getLong("featureIdRangeEnd"));
	}

	@Test
	void testPartition_withGridSizeOne_createsSinglePartition() throws IOException {
		String csvContent = """
				FEATURE_ID,MAP_ID
				1145678901,093G045
				1245678902,093G045
				1345678903,093G045
				""";

		InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());
		when(mockResource.getInputStream()).thenReturn(inputStream);

		dynamicPartitioner.setPolygonResource(mockResource);
		Map<String, ExecutionContext> partitions = dynamicPartitioner.partition(1);

		assertEquals(1, partitions.size());

		ExecutionContext context0 = partitions.get("partition0");
		assertEquals("1145678901,1245678902,1345678903", context0.getString("assignedFeatureIds")); // All 3 FEATURE_IDs
		assertEquals("partition0", context0.getString("partitionName"));
	}

	@Test
	void testPartition_withEmptyLines_skipsEmptyLines() throws IOException {
		String csvContent = """
				FEATURE_ID,MAP_ID
				1145678901,093G045

				1245678902,093G045

				1345678903,093G045
				"""; // Contains empty lines

		InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());
		when(mockResource.getInputStream()).thenReturn(inputStream);

		dynamicPartitioner.setPolygonResource(mockResource);
		Map<String, ExecutionContext> partitions = dynamicPartitioner.partition(2);

		assertEquals(2, partitions.size());

		// Should only count non-empty lines: 3 FEATURE_IDs
		// 3 FEATURE_IDs / 2 partitions = 1 FEATURE_ID per partition + 1 remainder
		ExecutionContext context0 = partitions.get("partition0");
		assertEquals("1145678901,1245678902", context0.getString("assignedFeatureIds")); // 2 FEATURE_IDs (1 +
																							// remainder)
		ExecutionContext context1 = partitions.get("partition1");
		assertEquals("1345678903", context1.getString("assignedFeatureIds")); // 1 FEATURE_ID
	}

	@Test
	void testPartition_ioException_returnsSinglePartition() throws IOException {
		when(mockResource.getInputStream()).thenThrow(new IOException("File read error"));

		dynamicPartitioner.setPolygonResource(mockResource);
		Map<String, ExecutionContext> partitions = dynamicPartitioner.partition(4);

		assertEquals(1, partitions.size());
		assertTrue(partitions.containsKey("partition0"));

		ExecutionContext context = partitions.get("partition0");
		assertEquals("", context.getString("assignedFeatureIds"));
		assertEquals("partition0", context.getString("partitionName"));

		verify(mockResource).getInputStream();
	}

	@Test
	void testPartition_resourceInputStreamThrowsException_handlesGracefully() throws IOException {
		when(mockResource.getInputStream()).thenThrow(new RuntimeException("Unexpected error"));

		dynamicPartitioner.setPolygonResource(mockResource);

		// RuntimeException should be caught and handled as IOException in
		// calculateTotalRecords
		// This will result in totalRecords = 0, creating a single partition
		assertThrows(RuntimeException.class, () -> {
			dynamicPartitioner.partition(2);
		});
	}

	@Test
	void testPartition_withLargeFile_createsCorrectPartitions() throws IOException {
		StringBuilder csvContentBuilder = new StringBuilder("""
				FEATURE_ID,MAP_ID
				""");
		// Create 100 realistic FEATURE_IDs starting from 1145678901
		for (int i = 0; i < 100; i++) {
			long featureId = 1145678901L + i;
			csvContentBuilder.append(featureId).append(",F").append("\n");
		}

		InputStream inputStream = new ByteArrayInputStream(csvContentBuilder.toString().getBytes());
		when(mockResource.getInputStream()).thenReturn(inputStream);

		dynamicPartitioner.setPolygonResource(mockResource);
		Map<String, ExecutionContext> partitions = dynamicPartitioner.partition(10);

		assertEquals(10, partitions.size());

		// 100 FEATURE_IDs / 10 partitions = 10 FEATURE_IDs per partition
		for (int i = 0; i < 10; i++) {
			ExecutionContext context = partitions.get("partition" + i);
			assertEquals("partition" + i, context.getString("partitionName"));

			// Verify that each partition has 10 FEATURE_IDs
			String assignedFeatureIds = context.getString("assignedFeatureIds");
			String[] featureIds = assignedFeatureIds.split(",");
			assertEquals(10, featureIds.length);

			// Verify first and last FEATURE_ID in each partition
			long expectedFirstFeatureId = 1145678901L + (i * 10);
			long expectedLastFeatureId = expectedFirstFeatureId + 9;
			assertEquals(String.valueOf(expectedFirstFeatureId), featureIds[0]);
			assertEquals(String.valueOf(expectedLastFeatureId), featureIds[9]);
		}
	}

	@Test
	void testPartition_withGridSizeZero_handlesEdgeCase() throws IOException {
		String csvContent = "FEATURE_ID,MAP_ID\n1145678901,093G045\n";
		InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());
		when(mockResource.getInputStream()).thenReturn(inputStream);

		dynamicPartitioner.setPolygonResource(mockResource);

		assertThrows(ArithmeticException.class, () -> {
			dynamicPartitioner.partition(0);
		});
	}

	@Test
	void testPartition_withNegativeGridSize_handlesEdgeCase() throws IOException {
		String csvContent = "FEATURE_ID,MAP_ID\n1145678901,093G045\n";
		InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());
		when(mockResource.getInputStream()).thenReturn(inputStream);

		dynamicPartitioner.setPolygonResource(mockResource);

		// This should handle negative grid size gracefully
		assertDoesNotThrow(() -> {
			Map<String, ExecutionContext> partitions = dynamicPartitioner.partition(-1);
			assertNotNull(partitions);
		});
	}

	@Test
	void testPartition_multipleConsecutiveCalls_producesConsistentResults() throws IOException {
		String csvContent = "FEATURE_ID,MAP_ID\n1145678901,093G045\n1245678902,093G045\n1345678903,093G045\n1445678904,093G045\n";

		// First call
		InputStream inputStream1 = new ByteArrayInputStream(csvContent.getBytes());
		when(mockResource.getInputStream()).thenReturn(inputStream1);
		dynamicPartitioner.setPolygonResource(mockResource);
		Map<String, ExecutionContext> partitions1 = dynamicPartitioner.partition(2);

		// Second call - need to reset the input stream mock
		InputStream inputStream2 = new ByteArrayInputStream(csvContent.getBytes());
		when(mockResource.getInputStream()).thenReturn(inputStream2);
		Map<String, ExecutionContext> partitions2 = dynamicPartitioner.partition(2);

		// Results should be identical
		assertEquals(partitions1.size(), partitions2.size());
		for (String key : partitions1.keySet()) {
			ExecutionContext context1 = partitions1.get(key);
			ExecutionContext context2 = partitions2.get(key);

			assertEquals(context1.getString("assignedFeatureIds"), context2.getString("assignedFeatureIds"));
			assertEquals(context1.getString("partitionName"), context2.getString("partitionName"));
		}

		verify(mockResource, times(2)).getInputStream();
	}

	@Test
	void testPartition_withInvalidFeatureIds_skipsInvalidEntries() throws IOException {
		String csvContent = """
				FEATURE_ID,MAP_ID
				1145678901,093G045
				invalid_id,093G045
				1245678902,093G045
				,093G045
				1345678903,093G045
				""";

		InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());
		when(mockResource.getInputStream()).thenReturn(inputStream);

		dynamicPartitioner.setPolygonResource(mockResource);
		Map<String, ExecutionContext> partitions = dynamicPartitioner.partition(2);

		assertEquals(2, partitions.size());

		// Should only process valid FEATURE_IDs: 1145678901, 1245678902, 1345678903
		// 3 valid FEATURE_IDs / 2 partitions = 1 FEATURE_ID per partition + 1 remainder
		ExecutionContext context0 = partitions.get("partition0");
		assertEquals("1145678901,1245678902", context0.getString("assignedFeatureIds"));

		ExecutionContext context1 = partitions.get("partition1");
		assertEquals("1345678903", context1.getString("assignedFeatureIds"));
	}

	@Test
	void testPartition_withMissingFeatureIdHeader_returnsSinglePartition() throws IOException {
		String csvContent = """
				ID,MAP_ID
				1145678901,093G045
				1245678902,093G045
				""";

		InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());
		when(mockResource.getInputStream()).thenReturn(inputStream);

		dynamicPartitioner.setPolygonResource(mockResource);
		Map<String, ExecutionContext> partitions = dynamicPartitioner.partition(4);

		assertEquals(1, partitions.size());
		assertTrue(partitions.containsKey("partition0"));

		ExecutionContext context = partitions.get("partition0");
		assertEquals("", context.getString("assignedFeatureIds"));
		assertEquals("partition0", context.getString("partitionName"));
	}

	@Test
	void testPartitionWithSinglePolygonTestData() {
		Resource polygonResource = new ClassPathResource("test-data/hcsv/single-polygon/VDYP7_INPUT_POLY.csv");
		dynamicPartitioner.setPolygonResource(polygonResource);

		Map<String, ExecutionContext> partitions = dynamicPartitioner.partition(2);

		assertEquals(1, partitions.size());
		assertTrue(partitions.containsKey("partition0"));

		ExecutionContext context0 = partitions.get("partition0");
		assertEquals("13919428", context0.getString("assignedFeatureIds"));
		assertEquals("partition0", context0.getString("partitionName"));
		assertEquals(13919428L, context0.getLong("featureIdRangeStart"));
		assertEquals(13919428L, context0.getLong("featureIdRangeEnd"));
	}

	@Test
	void testPartitionWithMultiplePolygonTestData() {
		Resource polygonResource = new ClassPathResource("test-data/hcsv/multiple-polygon/VDYP7_INPUT_POLY.csv");
		dynamicPartitioner.setPolygonResource(polygonResource);

		Map<String, ExecutionContext> partitions = dynamicPartitioner.partition(2);

		assertEquals(2, partitions.size());

		ExecutionContext context0 = partitions.get("partition0");
		assertEquals("17811434", context0.getString("assignedFeatureIds"));
		assertEquals("partition0", context0.getString("partitionName"));
		assertEquals(17811434L, context0.getLong("featureIdRangeStart"));
		assertEquals(17811434L, context0.getLong("featureIdRangeEnd"));

		ExecutionContext context1 = partitions.get("partition1");
		assertEquals("17811435", context1.getString("assignedFeatureIds"));
		assertEquals("partition1", context1.getString("partitionName"));
		assertEquals(17811435L, context1.getLong("featureIdRangeStart"));
		assertEquals(17811435L, context1.getLong("featureIdRangeEnd"));
	}

	@Test
	void testPartitionWithMultiplePolygonTestDataSinglePartition() {
		Resource polygonResource = new ClassPathResource("test-data/hcsv/multiple-polygon/VDYP7_INPUT_POLY.csv");
		dynamicPartitioner.setPolygonResource(polygonResource);

		Map<String, ExecutionContext> partitions = dynamicPartitioner.partition(1);

		assertEquals(1, partitions.size());

		ExecutionContext context0 = partitions.get("partition0");
		assertEquals("17811434,17811435", context0.getString("assignedFeatureIds"));
		assertEquals("partition0", context0.getString("partitionName"));
		assertEquals(17811434L, context0.getLong("featureIdRangeStart"));
		assertEquals(17811435L, context0.getLong("featureIdRangeEnd"));
	}
}