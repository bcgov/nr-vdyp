package ca.bc.gov.nrs.vdyp.batch.configuration;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.lang.NonNull;

public class DynamicPartitioner implements Partitioner {

	private static final Logger logger = LoggerFactory.getLogger(DynamicPartitioner.class);

	private static final String PARTITION_NAME = "partitionName";
	private static final String PARTITION_BASE_DIR = "partitionBaseDir";
	private static final String ASSIGNED_FEATURE_IDS = "assignedFeatureIds";

	private String partitionBaseDir;

	public void setPartitionBaseDir(String partitionBaseDir) {
		this.partitionBaseDir = partitionBaseDir;
	}

	/**
	 * @param gridSize Number of partitions to create
	 * @return Map of partition execution contexts for existing partitions
	 */
	@Override
	@NonNull
	public Map<String, ExecutionContext> partition(int gridSize) {
		Map<String, ExecutionContext> partitions = new HashMap<>();

		logger.info(
				"[VDYP Uploaded File Partitioner] Creating execution contexts for {} uploaded file partitions",
				gridSize);

		// Create execution contexts for existing partition directories
		for (int i = 0; i < gridSize; i++) {
			ExecutionContext context = new ExecutionContext();

			// Set partition parameters
			context.putString(PARTITION_NAME, "partition" + i);
			if (partitionBaseDir != null) {
				context.putString(PARTITION_BASE_DIR, partitionBaseDir);
			}

			// Set empty FEATURE_IDs since they're already distributed in partition files
			context.putString(ASSIGNED_FEATURE_IDS, "");

			partitions.put("partition" + i, context);

			logger.info(
					"VDYP partition{} execution context created for uploaded partition directory", i);
		}

		logger.info(
				"Uploaded file partitioner created {} execution contexts for uploaded partitions", partitions.size());

		return partitions;
	}
}
