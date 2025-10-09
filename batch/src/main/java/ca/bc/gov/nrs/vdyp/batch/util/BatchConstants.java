package ca.bc.gov.nrs.vdyp.batch.util;

public final class BatchConstants {

	public static final class Job {
		public static final String EXECUTION_ID = "jobExecutionId";
		public static final String NAME = "jobName";
		public static final String STATUS = "status";
		public static final String MESSAGE = "message";
		public static final String ERROR = "error";
		public static final String START_TIME = "startTime";
		public static final String TIMESTAMP = "jobTimestamp";
		public static final String BASE_DIR = "jobBaseDir";
		public static final String BASE_FOLDER_PREFIX = "vdyp-batch";

		private Job() {
		}
	}

	public static final class Partition {
		public static final String INPUT_PREFIX = "input";
		public static final String OUTPUT_PREFIX = "output";
		public static final String PREFIX = "partition";
		public static final String INPUT_FOLDER_NAME_PREFIX = INPUT_PREFIX + "-" + PREFIX;
		public static final String OUTPUT_FOLDER_NAME_PREFIX = OUTPUT_PREFIX + "-" + PREFIX;
		public static final String SIZE = "partitionSize";
		public static final String NAME = "partitionName";
		public static final String INPUT_POLYGON_FILE_NAME = "polygons.csv";
		public static final String INPUT_LAYER_FILE_NAME = "layers.csv";
		public static final String ASSIGNED_FEATURE_IDS = "assignedFeatureIds";

		private Partition() {
		}
	}

	public static final class File {
		public static final String YIELD_TABLE_TYPE = "YieldTable";
		public static final String YIELD_TABLE_FILENAME = "YieldTable.csv";

		private File() {
		}
	}

	public static final class Common {
		public static final String UNKNOWN = "unknown";
		public static final String TIMESTAMP = "timestamp";

		private Common() {
		}
	}

	public static final class ErrorMessage {
		public static final String NO_ERROR_MESSAGE = "No error message available";

		private ErrorMessage() {
		}
	}

	public static final class Projection {
		public static final String PARAMETERS_JSON = "projectionParametersJson";

		private Projection() {
		}
	}

	private BatchConstants() {
	}
}
