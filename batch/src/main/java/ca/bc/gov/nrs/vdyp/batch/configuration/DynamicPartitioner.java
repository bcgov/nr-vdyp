package ca.bc.gov.nrs.vdyp.batch.configuration;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.lang.NonNull;

import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;

public class DynamicPartitioner implements Partitioner {

	private static final Logger logger = LoggerFactory.getLogger(DynamicPartitioner.class);

	private String jobBaseDir;

	public void setJobBaseDir(String jobBaseDir) {
		this.jobBaseDir = jobBaseDir;
	}

	@Override
	@NonNull
	public Map<String, ExecutionContext> partition(int partitionSize) {
		Map<String, ExecutionContext> partitions = new HashMap<>();

		logger.info("Creating {} execution contexts for VDYP partitions", partitionSize);

		// Create execution contexts for existing partition directories
		for (int i = 0; i < partitionSize; i++) {
			ExecutionContext context = new ExecutionContext();

			String partitionName = BatchConstants.Partition.PREFIX + i;

			context.putString(BatchConstants.Partition.NAME, partitionName);
			if (jobBaseDir != null) {
				context.putString(BatchConstants.Job.BASE_DIR, jobBaseDir);
			}

			// Set empty FEATURE_IDs since they're already distributed in partition files
			context.putString(BatchConstants.Partition.ASSIGNED_FEATURE_IDS, "");

			partitions.put(partitionName, context);

			logger.debug("Created execution context for partition {}", i);
		}

		return partitions;
	}
}
