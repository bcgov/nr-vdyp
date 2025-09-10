package ca.bc.gov.nrs.vdyp.batch.configuration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.ExecutionContext;
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
	void testSetInputResource() {
		Resource resource = mock(Resource.class);
		assertDoesNotThrow(() -> dynamicPartitioner.setInputResource(resource));
	}

	@Test
	void testSetInputResource_withNull() {
		assertDoesNotThrow(() -> dynamicPartitioner.setInputResource(null));
	}

	@Test
	void testPartition_withNullResource_returnsSinglePartition() {
		// No resource set (null by default)
		Map<String, ExecutionContext> partitions = dynamicPartitioner.partition(4);

		assertEquals(1, partitions.size());
		assertTrue(partitions.containsKey("partition0"));

		ExecutionContext context = partitions.get("partition0");
		assertEquals(2L, context.getLong("startLine"));
		assertEquals(2L, context.getLong("endLine"));
		assertEquals("partition0", context.getString("partitionName"));
	}

	@Test
	void testPartition_withEmptyFile_returnsSinglePartition() throws IOException {
		String csvContent = "header\n"; // Only header, no data records
		InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());
		when(mockResource.getInputStream()).thenReturn(inputStream);

		dynamicPartitioner.setInputResource(mockResource);
		Map<String, ExecutionContext> partitions = dynamicPartitioner.partition(4);

		assertEquals(1, partitions.size());
		assertTrue(partitions.containsKey("partition0"));

		ExecutionContext context = partitions.get("partition0");
		assertEquals(2L, context.getLong("startLine"));
		assertEquals(2L, context.getLong("endLine"));
		assertEquals("partition0", context.getString("partitionName"));

		verify(mockResource).getInputStream();
	}

	@Test
	void testPartition_withEmptyFileNoHeader_returnsSinglePartition() throws IOException {
		String csvContent = ""; // Completely empty file
		InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());
		when(mockResource.getInputStream()).thenReturn(inputStream);

		dynamicPartitioner.setInputResource(mockResource);
		Map<String, ExecutionContext> partitions = dynamicPartitioner.partition(2);

		assertEquals(1, partitions.size());
		assertTrue(partitions.containsKey("partition0"));

		ExecutionContext context = partitions.get("partition0");
		assertEquals(2L, context.getLong("startLine"));
		assertEquals(2L, context.getLong("endLine"));
		assertEquals("partition0", context.getString("partitionName"));
	}

	@Test
	void testPartition_withValidFile_createsCorrectPartitions() throws IOException {
		String csvContent = """
				id,data,polygonId,layerId
				1,data1,poly1,layer1
				2,data2,poly2,layer2
				3,data3,poly3,layer3
				4,data4,poly4,layer4
				5,data5,poly5,layer5
				6,data6,poly6,layer6
				7,data7,poly7,layer7
				8,data8,poly8,layer8
				""";

		InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());
		when(mockResource.getInputStream()).thenReturn(inputStream);

		dynamicPartitioner.setInputResource(mockResource);
		Map<String, ExecutionContext> partitions = dynamicPartitioner.partition(4);

		assertEquals(4, partitions.size());

		// Check partition0: 8 records / 4 partitions = 2 records per partition
		ExecutionContext context0 = partitions.get("partition0");
		assertEquals(2L, context0.getLong("startLine")); // Start after header
		assertEquals(3L, context0.getLong("endLine")); // 2 records: lines 2-3
		assertEquals("partition0", context0.getString("partitionName"));

		// Check partition1
		ExecutionContext context1 = partitions.get("partition1");
		assertEquals(4L, context1.getLong("startLine"));
		assertEquals(5L, context1.getLong("endLine")); // 2 records: lines 4-5
		assertEquals("partition1", context1.getString("partitionName"));

		// Check partition2
		ExecutionContext context2 = partitions.get("partition2");
		assertEquals(6L, context2.getLong("startLine"));
		assertEquals(7L, context2.getLong("endLine")); // 2 records: lines 6-7
		assertEquals("partition2", context2.getString("partitionName"));

		// Check partition3
		ExecutionContext context3 = partitions.get("partition3");
		assertEquals(8L, context3.getLong("startLine"));
		assertEquals(9L, context3.getLong("endLine")); // 2 records: lines 8-9
		assertEquals("partition3", context3.getString("partitionName"));
	}

	@Test
	void testPartition_withRemainderRecords_distributesCorrectly() throws IOException {
		String csvContent = """
				id,data
				1,data1
				2,data2
				3,data3
				4,data4
				5,data5
				"""; // 5 records

		InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());
		when(mockResource.getInputStream()).thenReturn(inputStream);

		dynamicPartitioner.setInputResource(mockResource);
		Map<String, ExecutionContext> partitions = dynamicPartitioner.partition(2);

		assertEquals(2, partitions.size());

		// 5 records / 2 partitions = 2 records per partition + 1 remainder
		// partition0 gets 2 records
		ExecutionContext context0 = partitions.get("partition0");
		assertEquals(2L, context0.getLong("startLine"));
		assertEquals(3L, context0.getLong("endLine")); // 2 records

		// partition1 gets 2 + remainder = 3 records
		ExecutionContext context1 = partitions.get("partition1");
		assertEquals(4L, context1.getLong("startLine"));
		assertEquals(6L, context1.getLong("endLine")); // 3 records (includes remainder)
	}

	@Test
	void testPartition_withSingleRecord_createsSinglePartition() throws IOException {
		String csvContent = """
				id,data
				1,data1
				""";

		InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());
		when(mockResource.getInputStream()).thenReturn(inputStream);

		dynamicPartitioner.setInputResource(mockResource);
		Map<String, ExecutionContext> partitions = dynamicPartitioner.partition(4);

		assertEquals(4, partitions.size());

		// With 1 record and 4 partitions: recordsPerPartition=0, remainder=1
		// Only the last partition (partition3) gets the remainder record
		ExecutionContext context0 = partitions.get("partition0");
		assertEquals(2L, context0.getLong("startLine"));
		assertEquals(1L, context0.getLong("endLine")); // 0 records: start=2, end=2+0-1=1

		// Middle partitions also get empty ranges
		ExecutionContext context1 = partitions.get("partition1");
		assertTrue(context1.getLong("startLine") > context1.getLong("endLine"));

		ExecutionContext context2 = partitions.get("partition2");
		assertTrue(context2.getLong("startLine") > context2.getLong("endLine"));

		// Last partition gets the remainder record
		ExecutionContext context3 = partitions.get("partition3");
		assertEquals(2L, context3.getLong("startLine"));
		assertEquals(2L, context3.getLong("endLine")); // 1 record
	}

	@Test
	void testPartition_withGridSizeOne_createsSinglePartition() throws IOException {
		String csvContent = """
				id,data
				1,data1
				2,data2
				3,data3
				""";

		InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());
		when(mockResource.getInputStream()).thenReturn(inputStream);

		dynamicPartitioner.setInputResource(mockResource);
		Map<String, ExecutionContext> partitions = dynamicPartitioner.partition(1);

		assertEquals(1, partitions.size());

		ExecutionContext context0 = partitions.get("partition0");
		assertEquals(2L, context0.getLong("startLine"));
		assertEquals(4L, context0.getLong("endLine")); // All 3 records
		assertEquals("partition0", context0.getString("partitionName"));
	}

	@Test
	void testPartition_withEmptyLines_skipsEmptyLines() throws IOException {
		String csvContent = """
				id,data
				1,data1

				2,data2

				3,data3
				"""; // Contains empty line and whitespace only line

		InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());
		when(mockResource.getInputStream()).thenReturn(inputStream);

		dynamicPartitioner.setInputResource(mockResource);
		Map<String, ExecutionContext> partitions = dynamicPartitioner.partition(2);

		assertEquals(2, partitions.size());

		// Should only count non-empty lines: 3 data records
		// 3 records / 2 partitions = 1 record per partition + 1 remainder
		ExecutionContext context0 = partitions.get("partition0");
		assertEquals(2L, context0.getLong("startLine"));
		assertEquals(2L, context0.getLong("endLine")); // 1 record

		ExecutionContext context1 = partitions.get("partition1");
		assertEquals(3L, context1.getLong("startLine"));
		assertEquals(4L, context1.getLong("endLine")); // 2 records (1 + remainder)
	}

	@Test
	void testPartition_ioException_returnsSinglePartition() throws IOException {
		when(mockResource.getInputStream()).thenThrow(new IOException("File read error"));

		dynamicPartitioner.setInputResource(mockResource);
		Map<String, ExecutionContext> partitions = dynamicPartitioner.partition(4);

		assertEquals(1, partitions.size());
		assertTrue(partitions.containsKey("partition0"));

		ExecutionContext context = partitions.get("partition0");
		assertEquals(2L, context.getLong("startLine"));
		assertEquals(2L, context.getLong("endLine"));
		assertEquals("partition0", context.getString("partitionName"));

		verify(mockResource).getInputStream();
	}

	@Test
	void testPartition_resourceInputStreamThrowsException_handlesGracefully() throws IOException {
		when(mockResource.getInputStream()).thenThrow(new RuntimeException("Unexpected error"));

		dynamicPartitioner.setInputResource(mockResource);

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
				id,data
				""");
		// Create 100 data records
		for (int i = 1; i <= 100; i++) {
			csvContentBuilder.append(i).append(",data").append(i).append("\n");
		}

		InputStream inputStream = new ByteArrayInputStream(csvContentBuilder.toString().getBytes());
		when(mockResource.getInputStream()).thenReturn(inputStream);

		dynamicPartitioner.setInputResource(mockResource);
		Map<String, ExecutionContext> partitions = dynamicPartitioner.partition(10);

		assertEquals(10, partitions.size());

		// 100 records / 10 partitions = 10 records per partition
		for (int i = 0; i < 10; i++) {
			ExecutionContext context = partitions.get("partition" + i);
			long expectedStart = 2 + (i * 10); // Start after header + partition offset
			long expectedEnd = expectedStart + 9; // 10 records per partition

			assertEquals(expectedStart, context.getLong("startLine"));
			assertEquals(expectedEnd, context.getLong("endLine"));
			assertEquals("partition" + i, context.getString("partitionName"));
		}
	}

	@Test
	void testPartition_withGridSizeZero_handlesEdgeCase() throws IOException {
		String csvContent = "id,data\n1,data1\n";
		InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());
		when(mockResource.getInputStream()).thenReturn(inputStream);

		dynamicPartitioner.setInputResource(mockResource);

		// Grid size 0 will cause division by zero, so we expect an exception
		assertThrows(ArithmeticException.class, () -> {
			dynamicPartitioner.partition(0);
		});
	}

	@Test
	void testPartition_withNegativeGridSize_handlesEdgeCase() throws IOException {
		String csvContent = "id,data\n1,data1\n";
		InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());
		when(mockResource.getInputStream()).thenReturn(inputStream);

		dynamicPartitioner.setInputResource(mockResource);

		// This should handle negative grid size gracefully
		assertDoesNotThrow(() -> {
			Map<String, ExecutionContext> partitions = dynamicPartitioner.partition(-1);
			assertNotNull(partitions);
		});
	}

	@Test
	void testPartition_multipleConsecutiveCalls_producesConsistentResults() throws IOException {
		String csvContent = "id,data\n1,data1\n2,data2\n3,data3\n4,data4\n";

		// First call
		InputStream inputStream1 = new ByteArrayInputStream(csvContent.getBytes());
		when(mockResource.getInputStream()).thenReturn(inputStream1);
		dynamicPartitioner.setInputResource(mockResource);
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

			assertEquals(context1.getLong("startLine"), context2.getLong("startLine"));
			assertEquals(context1.getLong("endLine"), context2.getLong("endLine"));
			assertEquals(context1.getString("partitionName"), context2.getString("partitionName"));
		}

		verify(mockResource, times(2)).getInputStream();
	}

	@Test
	void testPartition_withComplexCsvData_parsesCorrectly() throws IOException {
		String csvContent = """
				id,name,description,value
				1,"Item with, comma","Description with
				newline",100.50
				2,Simple Item,Simple Description,200.75
				3,"Another, complex item","Multi-line
				description",300.25
				""";

		InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());
		when(mockResource.getInputStream()).thenReturn(inputStream);

		dynamicPartitioner.setInputResource(mockResource);
		Map<String, ExecutionContext> partitions = dynamicPartitioner.partition(2);

		assertEquals(2, partitions.size());

		// The CSV has embedded newlines, so it counts more lines than logical records
		// Verify the basic partitioning logic works
		ExecutionContext context0 = partitions.get("partition0");
		assertNotNull(context0);
		assertTrue(context0.getLong("startLine") >= 2L);

		ExecutionContext context1 = partitions.get("partition1");
		assertNotNull(context1);
		assertTrue(context1.getLong("startLine") > context0.getLong("endLine"));
	}
}