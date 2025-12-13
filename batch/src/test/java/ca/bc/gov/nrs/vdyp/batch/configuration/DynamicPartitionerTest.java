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
	}
}