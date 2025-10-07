package ca.bc.gov.nrs.vdyp.batch.configuration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.ExecutionContext;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DynamicPartitionerTest {

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
	void testSetJobBaseDir() {
		String baseDir = "/test/path";
		assertDoesNotThrow(() -> dynamicPartitioner.setJobBaseDir(baseDir));
	}

	@Test
	void testSetJobBaseDir_WithNull() {
		assertDoesNotThrow(() -> dynamicPartitioner.setJobBaseDir(null));
	}

	@Test
	void testPartition_SinglePartition() {
		Map<String, ExecutionContext> partitions = dynamicPartitioner.partition(1);

		assertEquals(1, partitions.size());
		assertTrue(partitions.containsKey("partition0"));

		ExecutionContext context = partitions.get("partition0");
		assertEquals("partition0", context.getString("partitionName"));
		assertEquals("", context.getString("assignedFeatureIds"));
		assertFalse(context.containsKey("jobBaseDir"));
	}

	@Test
	void testPartition_MultiplePartitions() {
		Map<String, ExecutionContext> partitions = dynamicPartitioner.partition(4);

		assertEquals(4, partitions.size());

		for (int i = 0; i < 4; i++) {
			String partitionKey = "partition" + i;
			assertTrue(partitions.containsKey(partitionKey));

			ExecutionContext context = partitions.get(partitionKey);
			assertEquals(partitionKey, context.getString("partitionName"));
			assertEquals("", context.getString("assignedFeatureIds"));
		}
	}

	@Test
	void testPartition_WithJobBaseDir() {
		String baseDir = "/test/partition/path";
		dynamicPartitioner.setJobBaseDir(baseDir);

		Map<String, ExecutionContext> partitions = dynamicPartitioner.partition(2);

		assertEquals(2, partitions.size());

		ExecutionContext context0 = partitions.get("partition0");
		assertEquals("partition0", context0.getString("partitionName"));
		assertEquals(baseDir, context0.getString("jobBaseDir"));
		assertEquals("", context0.getString("assignedFeatureIds"));

		ExecutionContext context1 = partitions.get("partition1");
		assertEquals("partition1", context1.getString("partitionName"));
		assertEquals(baseDir, context1.getString("jobBaseDir"));
		assertEquals("", context1.getString("assignedFeatureIds"));
	}

	@Test
	void testPartition_ZeroPartitions() {
		Map<String, ExecutionContext> partitions = dynamicPartitioner.partition(0);

		assertEquals(0, partitions.size());
		assertTrue(partitions.isEmpty());
	}

	@Test
	void testPartition_LargeNumberOfPartitions() {
		int gridSize = 10;
		Map<String, ExecutionContext> partitions = dynamicPartitioner.partition(gridSize);

		assertEquals(gridSize, partitions.size());

		// Verify all partitions are created with correct naming
		for (int i = 0; i < gridSize; i++) {
			String partitionKey = "partition" + i;
			assertTrue(partitions.containsKey(partitionKey));

			ExecutionContext context = partitions.get(partitionKey);
			assertEquals(partitionKey, context.getString("partitionName"));
			assertEquals("", context.getString("assignedFeatureIds"));
		}
	}

	@Test
	void testPartition_ConsistentResults() {
		// Test that multiple calls produce consistent results
		Map<String, ExecutionContext> partitions1 = dynamicPartitioner.partition(3);
		Map<String, ExecutionContext> partitions2 = dynamicPartitioner.partition(3);

		assertEquals(partitions1.size(), partitions2.size());

		for (String key : partitions1.keySet()) {
			assertTrue(partitions2.containsKey(key));
			
			ExecutionContext context1 = partitions1.get(key);
			ExecutionContext context2 = partitions2.get(key);
			
			assertEquals(context1.getString("partitionName"), context2.getString("partitionName"));
			assertEquals(context1.getString("assignedFeatureIds"), context2.getString("assignedFeatureIds"));
		}
	}

	@Test
	void testPartition_WithBaseDirAndMultiplePartitions() {
		String baseDir = "/upload/test";
		dynamicPartitioner.setJobBaseDir(baseDir);

		Map<String, ExecutionContext> partitions = dynamicPartitioner.partition(3);

		assertEquals(3, partitions.size());

		// Verify all partitions have the base directory set
		for (int i = 0; i < 3; i++) {
			ExecutionContext context = partitions.get("partition" + i);
			assertEquals(baseDir, context.getString("jobBaseDir"));
			assertEquals("partition" + i, context.getString("partitionName"));
			assertEquals("", context.getString("assignedFeatureIds"));
		}
	}
}